package com.example.domaingenerator.services

import com.example.domaingenerator.generator.DomainClassGenerator
import com.example.domaingenerator.models.GeneratorConfig
import com.example.domaingenerator.utils.NotificationHelper
import com.example.domaingenerator.utils.PsiHelper
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
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

        // Determine which classes to generate (PSI read operation)
        val classesToGenerate = runReadAction {
            val initialClasses = if (config.singleClassMode && config.singleClass != null) {
                // Single class mode - generate the selected class
                indicator.text = "Generating domain class for ${config.singleClass.name}..."
                listOf(config.singleClass)
            } else {
                // Package mode - scan entire package
                indicator.text = "Scanning source package..."

                val sourceClasses = PsiHelper.findClassesInPackageRecursive(project, config.sourcePackage)

                if (sourceClasses.isEmpty()) {
                    NotificationHelper.showWarning(
                        project,
                        "No classes found in package: ${config.sourcePackage}"
                    )
                    return@runReadAction emptyList()
                }

                // Filter out interfaces only (keep enums)
                val filtered = sourceClasses.filter { psiClass ->
                    !PsiHelper.isInterface(psiClass)
                }

                if (filtered.isEmpty()) {
                    NotificationHelper.showWarning(
                        project,
                        "No suitable classes found to generate from (excluded interfaces)"
                    )
                    return@runReadAction emptyList()
                }

                indicator.text = "Found ${filtered.size} classes to generate"
                filtered
            }

            // Include all dependencies (superclasses and field types) for both single and package mode
            if (initialClasses.isNotEmpty()) {
                indicator.text = "Collecting dependencies (superclasses and field types)..."
                val withDependencies = PsiHelper.collectAllDependencies(initialClasses)
                indicator.text = "Found ${withDependencies.size} classes including all dependencies"
                withDependencies
            } else {
                initialClasses
            }
        }

        if (classesToGenerate.isEmpty()) {
            NotificationHelper.showWarning(
                project,
                "No classes selected for generation"
            )
            return
        }

        // Classes are already sorted by dependency order from collectAllDependencies
        // No need to sort again

        // Find or create target package directory (mixed read/write operations)
        val targetDirectory = findOrCreatePackageDirectory(config.targetPackage)
        if (targetDirectory == null) {
            NotificationHelper.showError(
                project,
                "Failed to create target package directory: ${config.targetPackage}"
            )
            return
        }

        // Generate classes - pass the set of classes being generated for import resolution
        val classesSet = classesToGenerate.toSet()
        val generator = DomainClassGenerator(project, config, classesSet)
        val generatedFiles = mutableListOf<PsiFile>()

        classesToGenerate.forEachIndexed { index, sourceClass ->
            if (indicator.isCanceled) return

            indicator.fraction = (index + 1).toDouble() / classesToGenerate.size

            // Read class name in read action
            val className = runReadAction { sourceClass.name ?: "UnknownClass" }
            indicator.text = "Generating $className..."

            try {
                // Generate class code (PSI read operation)
                val generatedClass = runReadAction {
                    generator.generateDomainClass(sourceClass)
                }

                // Write file (write operation)
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
                    "Failed to generate $className: ${e.message}"
                )
            }
        }

        // Show success notification
        NotificationHelper.showInfo(
            project,
            "Successfully generated ${generatedFiles.size} domain classes in ${config.targetPackage}"
        )

        // Open first generated file on EDT
        if (generatedFiles.isNotEmpty()) {
            val firstFile = generatedFiles.first().virtualFile
            if (firstFile != null) {
                invokeLater {
                    FileEditorManager.getInstance(project).openFile(firstFile, true)
                }
            }
        }
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

        // Find initial directory in read action
        var currentDir = runReadAction {
            val psiManager = PsiManager.getInstance(project)
            psiManager.findDirectory(srcMainJava)
        } ?: return null

        // Create package directories
        val segments = packageName.split(".")
        for (segment in segments) {
            currentDir = WriteCommandAction.runWriteCommandAction<PsiDirectory>(project) {
                // Read and write in same action
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
