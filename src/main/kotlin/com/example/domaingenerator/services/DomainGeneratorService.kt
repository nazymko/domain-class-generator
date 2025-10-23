package com.example.domaingenerator.services

import com.example.domaingenerator.generator.DomainClassGenerator
import com.example.domaingenerator.models.GeneratorConfig
import com.example.domaingenerator.utils.NotificationHelper
import com.example.domaingenerator.utils.PsiHelper
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

/**
 * Service for managing domain class generation.
 * Orchestrates the entire generation process.
 */
@Service(Service.Level.PROJECT)
class DomainGeneratorService(private val project: Project) {

    /**
     * Generate domain classes based on configuration.
     */
    fun generateDomainClasses(config: GeneratorConfig) {
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Generating Domain Classes", true) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        performGeneration(config, indicator)
                    } catch (e: Exception) {
                        NotificationHelper.showError(
                            project,
                            "Failed to generate domain classes: ${e.message}"
                        )
                    }
                }
            }
        )
    }

    /**
     * Perform the actual generation.
     */
    private fun performGeneration(config: GeneratorConfig, indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        indicator.text = "Scanning source package..."

        // Find all classes in source package
        val sourceClasses = PsiHelper.findClassesInPackageRecursive(project, config.sourcePackage)

        if (sourceClasses.isEmpty()) {
            NotificationHelper.showWarning(
                project,
                "No classes found in package: ${config.sourcePackage}"
            )
            return
        }

        // Filter out interfaces, enums, and abstract classes if desired
        val classesToGenerate = sourceClasses.filter { psiClass ->
            !PsiHelper.isInterface(psiClass) && !PsiHelper.isEnum(psiClass)
        }

        if (classesToGenerate.isEmpty()) {
            NotificationHelper.showWarning(
                project,
                "No suitable classes found to generate from (excluded interfaces and enums)"
            )
            return
        }

        indicator.text = "Found ${classesToGenerate.size} classes to generate"

        // Sort classes by inheritance hierarchy (superclasses first)
        val sortedClasses = if (config.followInheritance) {
            sortByInheritanceHierarchy(classesToGenerate)
        } else {
            classesToGenerate
        }

        // Find or create target package directory
        val targetDirectory = findOrCreatePackageDirectory(config.targetPackage)
        if (targetDirectory == null) {
            NotificationHelper.showError(
                project,
                "Failed to create target package directory: ${config.targetPackage}"
            )
            return
        }

        // Generate classes
        val generator = DomainClassGenerator(project, config)
        val generatedFiles = mutableListOf<PsiFile>()

        sortedClasses.forEachIndexed { index, sourceClass ->
            if (indicator.isCanceled) return

            indicator.fraction = (index + 1).toDouble() / sortedClasses.size
            indicator.text = "Generating ${sourceClass.name}..."

            try {
                val generatedClass = generator.generateDomainClass(sourceClass)
                val psiFile = createJavaFile(
                    targetDirectory,
                    generatedClass.className,
                    generatedClass.sourceCode
                )
                if (psiFile != null) {
                    generatedFiles.add(psiFile)
                }
            } catch (e: Exception) {
                NotificationHelper.showWarning(
                    project,
                    "Failed to generate ${sourceClass.name}: ${e.message}"
                )
            }
        }

        // Show success notification
        NotificationHelper.showInfo(
            project,
            "Successfully generated ${generatedFiles.size} domain classes in ${config.targetPackage}"
        )

        // Open first generated file
        if (generatedFiles.isNotEmpty()) {
            generatedFiles.first().virtualFile?.let { virtualFile ->
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        }
    }

    /**
     * Sort classes by inheritance hierarchy (superclasses first).
     */
    private fun sortByInheritanceHierarchy(classes: List<PsiClass>): List<PsiClass> {
        val sorted = mutableListOf<PsiClass>()
        val remaining = classes.toMutableList()

        // Add classes without superclasses (or with external superclasses) first
        val withoutLocalSuper = remaining.filter { psiClass ->
            val superClass = PsiHelper.getNonObjectSuperclass(psiClass)
            superClass == null || !remaining.contains(superClass)
        }
        sorted.addAll(withoutLocalSuper)
        remaining.removeAll(withoutLocalSuper)

        // Keep adding classes whose superclasses are already in sorted list
        var previousSize = sorted.size
        while (remaining.isNotEmpty()) {
            val canAdd = remaining.filter { psiClass ->
                val superClass = PsiHelper.getNonObjectSuperclass(psiClass)
                superClass == null || sorted.contains(superClass)
            }
            sorted.addAll(canAdd)
            remaining.removeAll(canAdd)

            // Break if no progress (circular dependency or other issue)
            if (sorted.size == previousSize) {
                sorted.addAll(remaining)
                break
            }
            previousSize = sorted.size
        }

        return sorted
    }

    /**
     * Find or create a package directory in the project.
     */
    private fun findOrCreatePackageDirectory(packageName: String): PsiDirectory? {
        val projectDir = project.guessProjectDir() ?: return null

        // Try to find src/main/java directory
        val srcMainJava = projectDir.findFileByRelativePath("src/main/java")
            ?: projectDir.findFileByRelativePath("src")
            ?: return null

        val psiManager = PsiManager.getInstance(project)
        var currentDir = psiManager.findDirectory(srcMainJava) ?: return null

        // Create package directories
        val segments = packageName.split(".")
        for (segment in segments) {
            currentDir = WriteCommandAction.runWriteCommandAction<PsiDirectory>(project) {
                currentDir.findSubdirectory(segment) ?: currentDir.createSubdirectory(segment)
            }
        }

        return currentDir
    }

    /**
     * Create a Java file with content.
     */
    private fun createJavaFile(
        directory: PsiDirectory,
        className: String,
        content: String
    ): PsiFile? {
        return WriteCommandAction.runWriteCommandAction<PsiFile>(project) {
            val fileName = "$className.java"

            // Check if file already exists
            val existingFile = directory.findFile(fileName)
            if (existingFile != null) {
                // Overwrite existing file
                val document = PsiDocumentManager.getInstance(project).getDocument(existingFile)
                document?.setText(content)
                existingFile
            } else {
                // Create new file
                val psiFile = PsiFileFactory.getInstance(project)
                    .createFileFromText(fileName, JavaFileType.INSTANCE, content)
                directory.add(psiFile) as PsiFile
            }
        }
    }

    companion object {
        fun getInstance(project: Project): DomainGeneratorService = project.service()
    }
}
