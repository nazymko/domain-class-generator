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
     * Check if a class has a superclass (excluding Object).
     */
    fun hasNonObjectSuperclass(psiClass: PsiClass): Boolean {
        val superClass = psiClass.superClass ?: return false
        val superFqn = superClass.qualifiedName ?: return false
        return superFqn != "java.lang.Object"
    }

    /**
     * Get the superclass if it exists and is not Object.
     */
    fun getNonObjectSuperclass(psiClass: PsiClass): PsiClass? {
        val superClass = psiClass.superClass ?: return null
        val superFqn = superClass.qualifiedName ?: return null
        return if (superFqn != "java.lang.Object") superClass else null
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
}
