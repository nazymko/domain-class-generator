package com.example.domaingenerator.generator

import com.example.domaingenerator.models.GeneratorConfig
import com.example.domaingenerator.utils.PsiHelper
import com.intellij.openapi.project.Project
import com.intellij.psi.*

/**
 * Generates domain class source code from library classes.
 */
class DomainClassGenerator(
    private val project: Project,
    private val config: GeneratorConfig,
    private val classesBeingGenerated: Set<PsiClass> = emptySet()
) {

    /**
     * Generate domain class code from a source class.
     */
    fun generateDomainClass(sourceClass: PsiClass): GeneratedClass {
        val className = sourceClass.name ?: throw IllegalArgumentException("Class name is null")
        val fields = PsiHelper.getNonStaticFields(sourceClass)

        val sourceCode = buildString {
            // Package declaration
            appendLine("package ${config.targetPackage};")
            appendLine()

            // Imports
            val imports = collectImports(sourceClass, fields)
            imports.forEach { appendLine(it) }
            if (imports.isNotEmpty()) appendLine()

            // Lombok annotations
            config.lombokAnnotations.getAnnotationFQNs().forEach { annotation ->
                val simpleName = annotation.substringAfterLast('.')
                appendLine("@$simpleName")
            }

            // Class declaration
            val extendsClause = generateExtendsClause(sourceClass)
            appendLine("public class $className$extendsClause {")
            appendLine()

            // Fields
            fields.forEach { field ->
                val fieldDeclaration = generateFieldDeclaration(field)
                appendLine("    $fieldDeclaration")
            }

            // Generate manual getters/setters if needed
            if (shouldGenerateManualAccessors()) {
                appendLine()
                fields.forEach { field ->
                    appendLine(generateGetter(field))
                    appendLine()
                    appendLine(generateSetter(field))
                    if (field != fields.last()) appendLine()
                }
            }

            // Close class
            appendLine("}")
        }

        return GeneratedClass(
            className = className,
            sourceCode = sourceCode,
            targetPackage = config.targetPackage
        )
    }

    /**
     * Collect necessary imports for the generated class.
     */
    private fun collectImports(sourceClass: PsiClass, fields: List<PsiField>): Set<String> {
        val imports = mutableSetOf<String>()

        // Lombok imports
        config.lombokAnnotations.getAnnotationFQNs().forEach { fqn ->
            imports.add("import $fqn;")
        }

        // Field type imports
        fields.forEach { field ->
            val type = field.type
            collectTypeImports(type, imports)
        }

        // Superclass import (if extends a non-java.lang class)
        if (config.followInheritance) {
            val superClass = PsiHelper.getNonObjectSuperclass(sourceClass)
            if (superClass != null) {
                val superFqn = PsiHelper.getFqn(superClass)
                if (superFqn != null && !superFqn.startsWith("java.lang.")) {
                    // Check if superclass is being generated (in our batch)
                    if (classesBeingGenerated.contains(superClass)) {
                        // Import from target package instead
                        val superSimpleName = PsiHelper.getSimpleNameFromFqn(superFqn)
                        imports.add("import ${config.targetPackage}.$superSimpleName;")
                    } else {
                        imports.add("import $superFqn;")
                    }
                }
            }
        }

        return imports
    }

    /**
     * Collect imports for a type (including generics).
     */
    private fun collectTypeImports(type: PsiType, imports: MutableSet<String>) {
        when (type) {
            is PsiClassType -> {
                val resolvedClass = type.resolve()
                if (resolvedClass != null) {
                    val fqn = PsiHelper.getFqn(resolvedClass)
                    if (fqn != null && !fqn.startsWith("java.lang.") && fqn.contains('.')) {
                        // Check if it's being generated (in our batch), then use target package
                        if (classesBeingGenerated.contains(resolvedClass)) {
                            val simpleName = PsiHelper.getSimpleNameFromFqn(fqn)
                            imports.add("import ${config.targetPackage}.$simpleName;")
                        } else {
                            imports.add("import $fqn;")
                        }
                    }
                }

                // Handle generic parameters
                type.parameters.forEach { paramType ->
                    collectTypeImports(paramType, imports)
                }
            }
            is PsiArrayType -> {
                collectTypeImports(type.componentType, imports)
            }
        }
    }

    /**
     * Generate extends clause if the source class has a superclass.
     */
    private fun generateExtendsClause(sourceClass: PsiClass): String {
        if (!config.followInheritance) return ""

        val superClass = PsiHelper.getNonObjectSuperclass(sourceClass) ?: return ""
        val superFqn = PsiHelper.getFqn(superClass) ?: return ""
        val superSimpleName = PsiHelper.getSimpleNameFromFqn(superFqn)

        return " extends $superSimpleName"
    }

    /**
     * Generate field declaration.
     */
    private fun generateFieldDeclaration(field: PsiField): String {
        val fieldName = field.name
        val fieldType = getSimpleTypeName(field.type)
        val modifier = "private"

        return "$modifier $fieldType $fieldName;"
    }

    /**
     * Get simple type name (with generics if present).
     */
    private fun getSimpleTypeName(type: PsiType): String {
        return when (type) {
            is PsiClassType -> {
                val resolvedClass = type.resolve()
                val className = if (resolvedClass != null) {
                    val fqn = PsiHelper.getFqn(resolvedClass)
                    // Use simple name for classes being generated (will be in target package)
                    if (fqn != null && classesBeingGenerated.contains(resolvedClass)) {
                        PsiHelper.getSimpleNameFromFqn(fqn)
                    } else {
                        resolvedClass.name ?: type.presentableText
                    }
                } else {
                    type.presentableText
                }

                // Add generic parameters if present
                val params = type.parameters
                if (params.isNotEmpty()) {
                    val paramNames = params.joinToString(", ") { getSimpleTypeName(it) }
                    "$className<$paramNames>"
                } else {
                    className
                }
            }
            is PsiArrayType -> {
                getSimpleTypeName(type.componentType) + "[]"
            }
            else -> type.presentableText
        }
    }

    /**
     * Generate getter method.
     */
    private fun generateGetter(field: PsiField): String {
        val fieldName = field.name
        val fieldType = getSimpleTypeName(field.type)
        val methodName = "get${fieldName.replaceFirstChar { it.uppercaseChar() }}"

        return buildString {
            appendLine("    public $fieldType $methodName() {")
            appendLine("        return this.$fieldName;")
            append("    }")
        }
    }

    /**
     * Generate setter method.
     */
    private fun generateSetter(field: PsiField): String {
        val fieldName = field.name
        val fieldType = getSimpleTypeName(field.type)
        val methodName = "set${fieldName.replaceFirstChar { it.uppercaseChar() }}"

        return buildString {
            appendLine("    public void $methodName($fieldType $fieldName) {")
            appendLine("        this.$fieldName = $fieldName;")
            append("    }")
        }
    }

    /**
     * Check if manual getters/setters should be generated.
     */
    private fun shouldGenerateManualAccessors(): Boolean {
        val lombok = config.lombokAnnotations
        val hasLombokAccessors = lombok.useData || lombok.useGetter || lombok.useSetter

        return config.generateGettersSetters && !hasLombokAccessors
    }
}

/**
 * Generated class data.
 */
data class GeneratedClass(
    val className: String,
    val sourceCode: String,
    val targetPackage: String
)
