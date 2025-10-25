package com.example.domaingenerator.ui

import com.example.domaingenerator.models.GeneratorConfig
import com.example.domaingenerator.models.LombokAnnotations
import com.example.domaingenerator.utils.PackageHelper
import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiClass
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

/**
 * Configuration dialog for domain class generation.
 * Allows users to select source/target packages and Lombok annotations.
 */
class GeneratorConfigDialog(
    private val project: Project,
    private val detectedClass: PsiClass? = null,
    private val detectedPackage: String? = null
) : DialogWrapper(project) {

    // Package configuration
    private var sourcePackage: String = detectedPackage ?: ""
    private var targetPackage: String = ""
    private lateinit var targetPackageField: TextFieldWithBrowseButton

    // Detected class info
    private val detectedClassName = detectedClass?.name
    private val detectedClassFqn = detectedClass?.qualifiedName

    // Lombok annotation options
    private var useData: Boolean = false
    private var useBuilder: Boolean = false
    private var useGetter: Boolean = false
    private var useSetter: Boolean = false
    private var useNoArgsConstructor: Boolean = false
    private var useAllArgsConstructor: Boolean = false
    private var useToString: Boolean = false
    private var useEqualsAndHashCode: Boolean = false

    // Additional options
    private var generateGettersSetters: Boolean = false
    private var followInheritance: Boolean = true
    private var generateJavaDocs: Boolean = true

    // Generation scope
    private var generateSingleClass: Boolean = detectedClass != null
    private var generateMode: String = if (detectedClass != null) "single" else "package"

    init {
        title = "Generate Domain Classes"

        // Pre-fill target package with simple heuristic (no slow operations)
        if (sourcePackage.isNotEmpty()) {
            targetPackage = getSuggestedTargetPackage(sourcePackage)
        }

        init()
    }

    /**
     * Generate a simple target package suggestion from source package.
     * This is a fast, string-based heuristic that doesn't require scanning the project.
     */
    private fun getSuggestedTargetPackage(sourcePackage: String): String {
        // If source contains "library" or "external", replace with "domain"
        when {
            sourcePackage.contains("library") -> {
                return sourcePackage.replace("library", "domain")
            }
            sourcePackage.contains("external") -> {
                return sourcePackage.replace("external", "domain")
            }
            else -> {
                // Otherwise, suggest replacing the last segment with "domain"
                val basePkg = sourcePackage.substringBeforeLast(".", sourcePackage)
                return if (basePkg.isNotEmpty()) "$basePkg.domain" else "domain"
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            // Show detected class info if available
            if (detectedClassName != null) {
                group("Detected Context") {
                    row("Class:") {
                        label("<html><b>$detectedClassName</b></html>")
                    }
                    if (detectedClassFqn != null) {
                        row("Full Name:") {
                            label(detectedClassFqn)
                        }
                    }
                }
            }

            group("Package Configuration") {
                row("Source Package:") {
                    textField()
                        .bindText(::sourcePackage.toMutableProperty())
                        .columns(40)
                        .validationOnInput {
                            when {
                                it.text.isBlank() -> error("Source package cannot be empty")
                                !PackageHelper.isValidPackageName(it.text) -> error("Invalid package name format")
                                it.text == targetPackageField.text -> error("Source and target must be different")
                                else -> null
                            }
                        }
                        .apply {
                            if (detectedPackage.isNullOrEmpty()) {
                                focused()
                            }
                        }
                }

                row("Target Package:") {
                    targetPackageField = TextFieldWithBrowseButton().apply {
                        text = targetPackage
                        addActionListener {
                            // Open native JetBrains package chooser dialog
                            val chooser = PackageChooserDialog(
                                "Choose Target Package",
                                project
                            )
                            chooser.selectPackage(text)
                            chooser.show()

                            val selectedPackage = chooser.selectedPackage
                            if (selectedPackage != null) {
                                text = selectedPackage.qualifiedName
                                targetPackage = selectedPackage.qualifiedName
                            }
                        }

                        // Add document listener to update targetPackage variable
                        textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
                            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) {
                                targetPackage = textField.text.trim()
                            }
                            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) {
                                targetPackage = textField.text.trim()
                            }
                            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) {
                                targetPackage = textField.text.trim()
                            }
                        })
                    }

                    cell(targetPackageField)
                        .columns(40)
                        .validationOnInput {
                            val text = targetPackageField.text.trim()
                            when {
                                text.isBlank() -> error("Target package cannot be empty")
                                !PackageHelper.isValidPackageName(text) -> error("Invalid package name format")
                                text == sourcePackage -> error("Source and target must be different")
                                else -> null
                            }
                        }
                        .apply {
                            focused()
                        }
                }
            }

            // Generation scope (only show if class is detected)
            if (detectedClass != null) {
                group("Generation Scope") {
                    buttonsGroup {
                        row {
                            radioButton("Generate only: ${detectedClassName}", "single")
                        }
                        row {
                            radioButton("Generate entire package: $sourcePackage", "package")
                        }
                    }.bind(::generateMode.toMutableProperty())
                }
            }

            group("Lombok Annotations") {
                row {
                    checkBox("@Data")
                        .bindSelected(::useData.toMutableProperty())
                    checkBox("@Builder")
                        .bindSelected(::useBuilder.toMutableProperty())
                    checkBox("@Getter")
                        .bindSelected(::useGetter.toMutableProperty())
                    checkBox("@Setter")
                        .bindSelected(::useSetter.toMutableProperty())
                }

                row {
                    checkBox("@NoArgsConstructor")
                        .bindSelected(::useNoArgsConstructor.toMutableProperty())
                    checkBox("@AllArgsConstructor")
                        .bindSelected(::useAllArgsConstructor.toMutableProperty())
                }

                row {
                    checkBox("@ToString")
                        .bindSelected(::useToString.toMutableProperty())
                    checkBox("@EqualsAndHashCode")
                        .bindSelected(::useEqualsAndHashCode.toMutableProperty())
                }
            }

            group("Additional Options") {
                row {
                    checkBox("Follow inheritance structure")
                        .bindSelected(::followInheritance.toMutableProperty())
                    checkBox("Generate JavaDocs")
                        .bindSelected(::generateJavaDocs.toMutableProperty())
                }

                row {
                    checkBox("Generate manual getters/setters (if no Lombok)")
                        .bindSelected(::generateGettersSetters.toMutableProperty())
                }
            }

        }
    }

    /**
     * Validate user input before closing dialog.
     */
    override fun doValidate(): ValidationInfo? {
        // Update targetPackage from text field
        targetPackage = targetPackageField.text.trim()

        if (sourcePackage.isBlank()) {
            return ValidationInfo("Source package cannot be empty")
        }
        if (targetPackage.isBlank()) {
            return ValidationInfo("Target package cannot be empty")
        }
        if (sourcePackage == targetPackage) {
            return ValidationInfo("Source and target packages must be different")
        }

        // Package name validation
        if (!PackageHelper.isValidPackageName(sourcePackage)) {
            return ValidationInfo("Invalid source package name format. Use lowercase letters and dots (e.g., com.example.models)")
        }
        if (!PackageHelper.isValidPackageName(targetPackage)) {
            return ValidationInfo("Invalid target package name format. Use lowercase letters and dots (e.g., com.example.domain)")
        }

        return null
    }

    /**
     * Get the configuration from dialog inputs.
     */
    fun getConfiguration(): GeneratorConfig {
        val singleClassMode = generateMode == "single" && detectedClass != null

        return GeneratorConfig(
            sourcePackage = sourcePackage.trim(),
            targetPackage = targetPackage.trim(),
            lombokAnnotations = LombokAnnotations(
                useData = useData,
                useBuilder = useBuilder,
                useGetter = useGetter,
                useSetter = useSetter,
                useNoArgsConstructor = useNoArgsConstructor,
                useAllArgsConstructor = useAllArgsConstructor,
                useToString = useToString,
                useEqualsAndHashCode = useEqualsAndHashCode
            ),
            generateGettersSetters = generateGettersSetters,
            followInheritance = followInheritance,
            generateJavaDocs = generateJavaDocs,
            singleClassMode = singleClassMode,
            singleClass = if (singleClassMode) detectedClass else null
        )
    }
}
