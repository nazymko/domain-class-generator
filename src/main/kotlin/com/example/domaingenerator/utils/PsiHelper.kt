package com.example.domaingenerator.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch

/**
 * PSI helper utilities for analyzing Java classes.
 */
object PsiHelper {

    /**
     * Find all classes in a given package.
     */
    fun findClassesInPackage(project: Project, packageName: String): List<PsiClass> {
        val psiFacade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)

        // Find the package
        val psiPackage = psiFacade.findPackage(packageName) ?: return emptyList()

        // Get all classes in the package (non-recursive)
        return psiPackage.classes.toList()
    }

    /**
     * Find all classes in a package and its subpackages (recursive).
     */
    fun findClassesInPackageRecursive(project: Project, packageName: String): List<PsiClass> {
        val classes = mutableListOf<PsiClass>()
        val psiFacade = JavaPsiFacade.getInstance(project)

        fun searchPackage(pkgName: String) {
            val psiPackage = psiFacade.findPackage(pkgName) ?: return
            classes.addAll(psiPackage.classes)

            // Recursively search subpackages
            psiPackage.subPackages.forEach { subPackage ->
                searchPackage(subPackage.qualifiedName)
            }
        }

        searchPackage(packageName)
        return classes
    }

    /**
     * Get all non-static fields in a class.
     */
    fun getNonStaticFields(psiClass: PsiClass): List<PsiField> {
        return psiClass.fields.filter { field ->
            !field.hasModifierProperty(PsiModifier.STATIC)
        }
    }

    /**
     * Get the fully qualified name of a class.
     */
    fun getFqn(psiClass: PsiClass): String? {
        return psiClass.qualifiedName
    }

    /**
     * Get the package name from a fully qualified class name.
     */
    fun getPackageFromFqn(fqn: String): String {
        val lastDot = fqn.lastIndexOf('.')
        return if (lastDot > 0) fqn.substring(0, lastDot) else ""
    }

    /**
     * Get the simple class name from a fully qualified class name.
     */
    fun getSimpleNameFromFqn(fqn: String): String {
        val lastDot = fqn.lastIndexOf('.')
        return if (lastDot > 0) fqn.substring(lastDot + 1) else fqn
    }

    /**
     * Check if a class has a superclass (excluding Object and Enum).
     */
    fun hasNonObjectSuperclass(psiClass: PsiClass): Boolean {
        val superClass = psiClass.superClass ?: return false
        val superFqn = superClass.qualifiedName ?: return false
        return superFqn != "java.lang.Object" && superFqn != "java.lang.Enum"
    }

    /**
     * Get the superclass if it exists and is not Object or Enum.
     */
    fun getNonObjectSuperclass(psiClass: PsiClass): PsiClass? {
        val superClass = psiClass.superClass ?: return null
        val superFqn = superClass.qualifiedName ?: return null
        return if (superFqn != "java.lang.Object" && superFqn != "java.lang.Enum") superClass else null
    }

    /**
     * Check if a class is abstract.
     */
    fun isAbstract(psiClass: PsiClass): Boolean {
        return psiClass.hasModifierProperty(PsiModifier.ABSTRACT)
    }

    /**
     * Check if a class is an interface.
     */
    fun isInterface(psiClass: PsiClass): Boolean {
        return psiClass.isInterface
    }

    /**
     * Check if a class is an enum.
     */
    fun isEnum(psiClass: PsiClass): Boolean {
        return psiClass.isEnum
    }

    /**
     * Get all methods in a class (excluding static and constructors).
     */
    fun getNonStaticMethods(psiClass: PsiClass): List<PsiMethod> {
        return psiClass.methods.filter { method ->
            !method.hasModifierProperty(PsiModifier.STATIC) && !method.isConstructor
        }
    }

    /**
     * Check if a field has a getter method.
     */
    fun hasGetter(field: PsiField, psiClass: PsiClass): Boolean {
        val fieldName = field.name
        val getterName = "get${fieldName.replaceFirstChar { it.uppercaseChar() }}"
        val booleanGetterName = "is${fieldName.replaceFirstChar { it.uppercaseChar() }}"

        return psiClass.methods.any { method ->
            (method.name == getterName || method.name == booleanGetterName) &&
            method.parameterList.parametersCount == 0
        }
    }

    /**
     * Check if a field has a setter method.
     */
    fun hasSetter(field: PsiField, psiClass: PsiClass): Boolean {
        val fieldName = field.name
        val setterName = "set${fieldName.replaceFirstChar { it.uppercaseChar() }}"

        return psiClass.methods.any { method ->
            method.name == setterName && method.parameterList.parametersCount == 1
        }
    }

    /**
     * Get the canonical text of a type (fully qualified name).
     */
    fun getTypeCanonicalText(type: PsiType): String {
        return type.canonicalText
    }

    /**
     * Check if a type is a primitive.
     */
    fun isPrimitive(type: PsiType): Boolean {
        return type is PsiPrimitiveType
    }

    /**
     * Get generic type parameters from a type.
     */
    fun getGenericParameters(type: PsiType): List<PsiType> {
        return when (type) {
            is PsiClassType -> type.parameters.toList()
            else -> emptyList()
        }
    }

    /**
     * Check if a class belongs to a specific package (or subpackage).
     */
    fun belongsToPackage(psiClass: PsiClass, packagePrefix: String): Boolean {
        val classFqn = psiClass.qualifiedName ?: return false
        return classFqn.startsWith("$packagePrefix.")
    }

    /**
     * Find a class by its fully qualified name.
     */
    fun findClassByFqn(project: Project, fqn: String): PsiClass? {
        val psiFacade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        return psiFacade.findClass(fqn, scope)
    }

    /**
     * Collect all superclasses recursively up to Object.
     * Returns list starting from most distant ancestor to immediate parent.
     */
    fun getAllSuperclasses(psiClass: PsiClass): List<PsiClass> {
        val superclasses = mutableListOf<PsiClass>()
        var currentClass = psiClass

        while (true) {
            val superClass = getNonObjectSuperclass(currentClass) ?: break
            superclasses.add(0, superClass) // Add at beginning to maintain hierarchy order
            currentClass = superClass
        }

        return superclasses
    }

    /**
     * Collect all superclasses for a list of classes, removing duplicates.
     * Returns combined list with superclasses first.
     */
    fun collectAllSuperclassesForClasses(classes: List<PsiClass>): List<PsiClass> {
        val allClasses = mutableSetOf<PsiClass>()
        val superclassesSet = mutableSetOf<PsiClass>()

        // First collect all superclasses
        classes.forEach { psiClass ->
            val superclasses = getAllSuperclasses(psiClass)
            superclassesSet.addAll(superclasses)
        }

        // Add superclasses first, then original classes
        allClasses.addAll(superclassesSet)
        allClasses.addAll(classes)

        return allClasses.toList()
    }

    /**
     * Get all custom type dependencies from a class's fields.
     * Returns classes used as field types (excluding primitives and java.* classes).
     */
    fun getFieldTypeDependencies(psiClass: PsiClass): Set<PsiClass> {
        val dependencies = mutableSetOf<PsiClass>()
        val fields = getNonStaticFields(psiClass)

        fields.forEach { field ->
            collectTypeDependencies(field.type, dependencies)
        }

        return dependencies
    }

    /**
     * Recursively collect type dependencies (including generics).
     */
    private fun collectTypeDependencies(type: PsiType, dependencies: MutableSet<PsiClass>) {
        when (type) {
            is PsiClassType -> {
                val resolvedClass = type.resolve()
                if (resolvedClass != null) {
                    val fqn = getFqn(resolvedClass)
                    // Only collect custom types (not primitives, not java.*, not kotlin.*, not interfaces)
                    if (fqn != null &&
                        !fqn.startsWith("java.") &&
                        !fqn.startsWith("kotlin.") &&
                        !isInterface(resolvedClass)) {
                        dependencies.add(resolvedClass)
                    }
                }

                // Handle generic parameters
                type.parameters.forEach { paramType ->
                    collectTypeDependencies(paramType, dependencies)
                }
            }
            is PsiArrayType -> {
                collectTypeDependencies(type.componentType, dependencies)
            }
        }
    }

    /**
     * Collect all dependencies (superclasses and field types) for a list of classes.
     * Recursively collects dependencies of dependencies.
     * Returns list with dependencies first, original classes last.
     */
    fun collectAllDependencies(classes: List<PsiClass>, maxDepth: Int = 10): List<PsiClass> {
        val allClasses = mutableSetOf<PsiClass>()
        val processed = mutableSetOf<PsiClass>()
        val toProcess = mutableListOf<PsiClass>()

        toProcess.addAll(classes)
        var depth = 0

        while (toProcess.isNotEmpty() && depth < maxDepth) {
            val current = toProcess.removeAt(0)
            if (processed.contains(current)) continue

            processed.add(current)
            allClasses.add(current)

            // Collect superclasses
            val superclasses = getAllSuperclasses(current)
            superclasses.forEach { superClass ->
                if (!processed.contains(superClass) && !toProcess.contains(superClass)) {
                    toProcess.add(superClass)
                }
            }

            // Collect field type dependencies
            val fieldDependencies = getFieldTypeDependencies(current)
            fieldDependencies.forEach { dependency ->
                if (!processed.contains(dependency) && !toProcess.contains(dependency)) {
                    toProcess.add(dependency)
                }
            }

            depth++
        }

        // Return sorted by hierarchy (dependencies first)
        return sortByDependencyOrder(allClasses.toList(), classes)
    }

    /**
     * Sort classes so dependencies come before classes that depend on them.
     */
    private fun sortByDependencyOrder(allClasses: List<PsiClass>, originalClasses: List<PsiClass>): List<PsiClass> {
        val sorted = mutableListOf<PsiClass>()
        val remaining = allClasses.toMutableList()

        // Keep adding classes whose dependencies are already in sorted list
        while (remaining.isNotEmpty()) {
            val previousSize = sorted.size

            val canAdd = remaining.filter { psiClass ->
                val superClass = getNonObjectSuperclass(psiClass)
                val fieldDeps = getFieldTypeDependencies(psiClass)

                // All dependencies must be in sorted already (or not in our generation set)
                val superOk = superClass == null || !remaining.contains(superClass) || sorted.contains(superClass)
                val fieldsOk = fieldDeps.all { dep -> !remaining.contains(dep) || sorted.contains(dep) }

                superOk && fieldsOk
            }

            sorted.addAll(canAdd)
            remaining.removeAll(canAdd)

            // Break if no progress (circular dependency)
            if (sorted.size == previousSize) {
                sorted.addAll(remaining)
                break
            }
        }

        return sorted
    }
}
