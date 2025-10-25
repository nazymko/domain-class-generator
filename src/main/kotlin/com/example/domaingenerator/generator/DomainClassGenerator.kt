package com.example.domaingenerator.generator

import com.example.domaingenerator.models.GeneratorConfig
import com.example.domaingenerator.utils.PsiHelper
import com.intellij.openapi.project.Project
import com.intellij.psi.*

/**
 * Field information holder for enum generation.
 */
private data class FieldInfo(
    val name: String,
    val type: PsiType
)

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

        // Check if this is an enum
        val isEnum = PsiHelper.isEnum(sourceClass)

        val sourceCode = if (isEnum) {
            generateEnumClass(sourceClass, className)
        } else {
            generateRegularClass(sourceClass, className)
        }

        return GeneratedClass(
            className = className,
            sourceCode = sourceCode,
            targetPackage = config.targetPackage
        )
    }

    /**
     * Generate regular class code.
     */
    private fun generateRegularClass(sourceClass: PsiClass, className: String): String {
        val fields = PsiHelper.getNonStaticFields(sourceClass)

        return buildString {
            // Package declaration
            appendLine("package ${config.targetPackage};")
            appendLine()

            // Imports
            val imports = collectImports(sourceClass, fields)
            imports.forEach { appendLine(it) }
            if (imports.isNotEmpty()) appendLine()

            // JavaDoc (if enabled)
            if (config.generateJavaDocs) {
                appendLine(generateClassJavaDoc(sourceClass))
            }

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
    }

    /**
     * Generate enum class code.
     */
    private fun generateEnumClass(sourceClass: PsiClass, className: String): String {
        val enumConstants = sourceClass.fields.filter {
            it is PsiEnumConstant
        }
        val constructors = sourceClass.constructors.filter { !it.hasModifierProperty(PsiModifier.PRIVATE) || enumConstants.isNotEmpty() }

        // For enums, generate fields based on constructor parameters to ensure all are included
        val fieldsToGenerate = if (constructors.isNotEmpty()) {
            // Use constructor parameters as the source of truth for fields
            val primaryConstructor = constructors.first()
            primaryConstructor.parameterList.parameters.map { param ->
                FieldInfo(name = param.name!!, type = param.type)
            }
        } else {
            // Fallback to existing fields if no constructor
            PsiHelper.getNonStaticFields(sourceClass).map { field ->
                FieldInfo(name = field.name, type = field.type)
            }
        }

        return buildString {
            // Package declaration
            appendLine("package ${config.targetPackage};")
            appendLine()

            // Imports
            val imports = collectEnumImports(sourceClass, constructors)
            imports.forEach { appendLine(it) }
            if (imports.isNotEmpty()) appendLine()

            // JavaDoc (if enabled)
            if (config.generateJavaDocs) {
                appendLine(generateClassJavaDoc(sourceClass))
            }

            // Lombok annotations (for enums)
            config.lombokAnnotations.getAnnotationFQNs().forEach { annotation ->
                val simpleName = annotation.substringAfterLast('.')
                appendLine("@$simpleName")
            }

            // Enum declaration
            appendLine("public enum $className {")

            // Enum constants with their arguments
            if (enumConstants.isNotEmpty()) {
                enumConstants.forEachIndexed { index, constant ->
                    val enumConstant = constant as PsiEnumConstant
                    val constantDeclaration = generateEnumConstantDeclaration(enumConstant)
                    val isLast = index == enumConstants.size - 1
                    if (isLast) {
                        appendLine("    $constantDeclaration;")
                    } else {
                        appendLine("    $constantDeclaration,")
                    }
                }
                appendLine()
            }

            // Fields (based on constructor parameters)
            if (fieldsToGenerate.isNotEmpty()) {
                fieldsToGenerate.forEach { fieldInfo ->
                    val fieldType = getSimpleTypeName(fieldInfo.type)
                    appendLine("    private $fieldType ${fieldInfo.name};")
                }
                appendLine()
            }

            // Constructors
            if (constructors.isNotEmpty()) {
                constructors.forEach { constructor ->
                    appendLine(generateConstructor(constructor, className))
                    appendLine()
                }
            }

            // Generate manual getters/setters if needed
            if (shouldGenerateManualAccessors() && fieldsToGenerate.isNotEmpty()) {
                fieldsToGenerate.forEach { fieldInfo ->
                    val fieldType = getSimpleTypeName(fieldInfo.type)
                    appendLine(generateGetterForField(fieldInfo.name, fieldType))
                    appendLine()
                    appendLine(generateSetterForField(fieldInfo.name, fieldType))
                    if (fieldInfo != fieldsToGenerate.last()) appendLine()
                }
            }

            // Close enum
            appendLine("}")
        }
    }

    /**
     * Generate enum constant declaration with arguments.
     */
    private fun generateEnumConstantDeclaration(enumConstant: PsiEnumConstant): String {
        val name = enumConstant.name
        val argumentList = enumConstant.argumentList

        return if (argumentList != null && argumentList.expressions.isNotEmpty()) {
            val args = argumentList.expressions.joinToString(", ") { expr ->
                expr.text
            }
            "$name($args)"
        } else {
            name
        }
    }

    /**
     * Generate constructor for enum or class.
     */
    private fun generateConstructor(constructor: PsiMethod, className: String): String {
        val parameters = constructor.parameterList.parameters
        val paramDeclarations = parameters.joinToString(", ") { param ->
            val paramType = getSimpleTypeName(param.type)
            val paramName = param.name
            "$paramType $paramName"
        }

        return buildString {
            appendLine("    $className($paramDeclarations) {")
            // Generate field assignments
            parameters.forEach { param ->
                appendLine("        this.${param.name} = ${param.name};")
            }
            append("    }")
        }
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
     * Collect necessary imports for enums (includes constructor parameter types).
     */
    private fun collectEnumImports(sourceClass: PsiClass, constructors: List<PsiMethod>): Set<String> {
        val imports = mutableSetOf<String>()

        // Lombok imports
        config.lombokAnnotations.getAnnotationFQNs().forEach { fqn ->
            imports.add("import $fqn;")
        }

        // Constructor parameter type imports (these will be the fields)
        constructors.forEach { constructor ->
            constructor.parameterList.parameters.forEach { param ->
                collectTypeImports(param.type, imports)
            }
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

    /**
     * Generate getter method from field name and type string.
     */
    private fun generateGetterForField(fieldName: String, fieldType: String): String {
        val methodName = "get${fieldName.replaceFirstChar { it.uppercaseChar() }}"

        return buildString {
            appendLine("    public $fieldType $methodName() {")
            appendLine("        return this.$fieldName;")
            append("    }")
        }
    }

    /**
     * Generate setter method from field name and type string.
     */
    private fun generateSetterForField(fieldName: String, fieldType: String): String {
        val methodName = "set${fieldName.replaceFirstChar { it.uppercaseChar() }}"

        return buildString {
            appendLine("    public void $methodName($fieldType $fieldName) {")
            appendLine("        this.$fieldName = $fieldName;")
            append("    }")
        }
    }

    /**
     * Generate JavaDoc for the class.
     */
    private fun generateClassJavaDoc(sourceClass: PsiClass): String {
        val className = sourceClass.name ?: "Unknown"
        val sourceFqn = PsiHelper.getFqn(sourceClass) ?: "Unknown"
        val superClass = PsiHelper.getNonObjectSuperclass(sourceClass)

        // Get plugin information from the plugin descriptor
        val pluginId = "io.github.nazymko.domaingenerator"
        val plugin = com.intellij.ide.plugins.PluginManagerCore.getPlugin(
            com.intellij.openapi.extensions.PluginId.getId(pluginId)
        )
        val pluginName = plugin?.name ?: "Domain Class Generator"
        val pluginVersion = plugin?.version ?: "1.0-SNAPSHOT"
        val pluginVendor = plugin?.vendor ?: "YourCompany"

        return buildString {
            appendLine("/**")
            appendLine(" * Domain class generated from library class: $className")
            appendLine(" * <p>")
            appendLine(" * Source: $sourceFqn")
            if (config.followInheritance && superClass != null) {
                val superFqn = PsiHelper.getFqn(superClass) ?: superClass.name
                appendLine(" * Extends: $superFqn")
            }
            appendLine(" * <p>")
            appendLine(" * Generated by: $pluginName v$pluginVersion")
            appendLine(" * @author $pluginVendor")
            append(" */")
        }
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
