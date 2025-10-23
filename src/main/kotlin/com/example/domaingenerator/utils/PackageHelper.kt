package com.example.domaingenerator.utils

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager

/**
 * Helper utilities for working with Java packages.
 */
object PackageHelper {

    /**
     * Get all Java packages in the user's project (source directories only, excluding libraries).
     * Safe to call from any thread - automatically wraps in read action.
     */
    fun getAllPackages(project: Project): List<String> {
        return runReadAction {
            try {
                val packages = mutableSetOf<String>()
                val psiManager = PsiManager.getInstance(project)

                // Get only source roots from the project (excludes libraries)
                val projectRootManager = ProjectRootManager.getInstance(project)
                val sourceRoots = projectRootManager.contentSourceRoots

                // For each source root, find all packages
                sourceRoots.forEach { sourceRoot ->
                    try {
                        val sourceDirectory = psiManager.findDirectory(sourceRoot)
                        if (sourceDirectory != null) {
                            // Recursively collect packages from this source root
                            collectPackagesFromDirectory(sourceDirectory, packages)
                        }
                    } catch (e: Exception) {
                        // Skip problematic source roots
                    }
                }

                packages.sorted()
            } catch (e: Exception) {
                // Return empty list if something goes wrong
                emptyList()
            }
        }
    }

    /**
     * Collect packages from a directory recursively.
     * MUST be called within a read action.
     */
    private fun collectPackagesFromDirectory(
        directory: PsiDirectory,
        packages: MutableSet<String>
    ) {
        try {
            // Check if this directory contains Java files
            directory.files.forEach { file ->
                if (file.name.endsWith(".java") && file is PsiJavaFile) {
                    try {
                        val packageName = file.packageName
                        if (packageName.isNotEmpty()) {
                            packages.add(packageName)
                        }
                    } catch (e: Exception) {
                        // Skip files that can't be processed
                    }
                }
            }

            // Recursively process subdirectories
            directory.subdirectories.forEach { subDir ->
                try {
                    collectPackagesFromDirectory(subDir, packages)
                } catch (e: Exception) {
                    // Skip problematic subdirectories
                }
            }
        } catch (e: Exception) {
            // Skip this directory if there's an issue
        }
    }

    /**
     * Get packages matching a prefix (for autocomplete).
     */
    fun getPackagesMatchingPrefix(project: Project, prefix: String): List<String> {
        if (prefix.isBlank()) return getAllPackages(project)

        return getAllPackages(project).filter { packageName ->
            packageName.startsWith(prefix, ignoreCase = true)
        }
    }

    /**
     * Validate if a package name is valid.
     */
    fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isBlank()) return false

        // Package name regex: lowercase letters, numbers, dots
        val packageRegex = Regex("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$")
        return packageName.matches(packageRegex)
    }

    /**
     * Get suggested target packages based on source package.
     * For example: com.library.models -> com.myapp.domain
     * MUST be called with read access (wrapped in runReadAction).
     */
    fun getSuggestedTargetPackages(project: Project, sourcePackage: String): List<String> {
        return runReadAction {
            val allPackages = getAllPackages(project)
            val suggestions = mutableListOf<String>()

            // Find packages that might be domain/dto/entity packages
            val domainKeywords = listOf("domain", "dto", "model", "entity", "vo", "pojo")

            allPackages.forEach { pkg ->
                val lowerPkg = pkg.lowercase()
                if (domainKeywords.any { lowerPkg.contains(it) }) {
                    suggestions.add(pkg)
                }
            }

            // If source package contains certain keywords, suggest alternatives
            if (sourcePackage.contains("library") || sourcePackage.contains("external")) {
                val basePkg = sourcePackage.substringBeforeLast(".")
                suggestions.add("$basePkg.domain")
                suggestions.add("$basePkg.dto")
            }

            suggestions.distinct().sorted()
        }
    }
}
