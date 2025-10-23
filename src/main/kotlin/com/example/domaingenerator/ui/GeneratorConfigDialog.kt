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

    // Generation scope
    private var generateSingleClass: Boolean = detectedClass != null
    private var generateMode: String = if (detectedClass != null) "single" else "package"

    init {
        title = "Generate Domain Classes"

        // Get suggested packages
        val suggestedPackages = if (sourcePackage.isNotEmpty()) {
            PackageHelper.getSuggestedTargetPackages(project, sourcePackage)
        } else {
            emptyList()
        }

        // Pre-fill target package with first suggestion
        if (suggestedPackages.isNotEmpty()) {
            targetPackage = suggestedPackages.first()
        }

        init()
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
                                .comment("This is the class at your cursor or from the current file")
                        }
                    }
                }
            }

            group("Package Configuration") {
                row("Source Package:") {
                    textField()
                        .bindText(::sourcePackage.toMutableProperty())
                        .columns(40)
                        .comment("Package to scan for library classes (auto-detected from current file)")
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
                    }

                    cell(targetPackageField)
                        .columns(40)
                        .comment("Click browse to select package, or type manually")
                        .apply {
                            focused()
                        }
                }

                row {
                    comment("""
                        <b>Tip:</b> Click the folder icon to browse packages, or type a package name.
                        The generator will scan all classes in the source package.
                    """.trimIndent())
                }
            }

            // Generation scope (only show if class is detected)
            if (detectedClass != null) {
                group("Generation Scope") {
                    buttonsGroup {
                        row {
                            radioButton("Generate only: ${detectedClassName}", "single")
                                .comment("Generate domain class only for the detected class")
                        }
                        row {
                            radioButton("Generate entire package: $sourcePackage", "package")
                                .comment("Generate domain classes for all classes in the source package")
                        }
                    }.bind(::generateMode.toMutableProperty())
                }
            }

            group("Lombok Annotations") {
                row {
                    checkBox("@Data")
                        .bindSelected(::useData.toMutableProperty())
                        .comment("Generates getters, setters, toString, equals, and hashCode")
                }

                row {
                    checkBox("@Builder")
                        .bindSelected(::useBuilder.toMutableProperty())
                        .comment("Generates builder pattern for object construction")
                }

                row {
                    checkBox("@Getter")
                        .bindSelected(::useGetter.toMutableProperty())
                        .comment("Generates getter methods for all fields")
                }

                row {
                    checkBox("@Setter")
                        .bindSelected(::useSetter.toMutableProperty())
                        .comment("Generates setter methods for all fields")
                }

                row {
                    checkBox("@NoArgsConstructor")
                        .bindSelected(::useNoArgsConstructor.toMutableProperty())
                        .comment("Generates no-argument constructor")
                }

                row {
                    checkBox("@AllArgsConstructor")
                        .bindSelected(::useAllArgsConstructor.toMutableProperty())
                        .comment("Generates constructor with all arguments")
                }

                row {
                    checkBox("@ToString")
                        .bindSelected(::useToString.toMutableProperty())
                        .comment("Generates toString method")
                }

                row {
                    checkBox("@EqualsAndHashCode")
                        .bindSelected(::useEqualsAndHashCode.toMutableProperty())
                        .comment("Generates equals and hashCode methods")
                }
            }

            group("Additional Options") {
                row {
                    checkBox("Follow inheritance structure")
                        .bindSelected(::followInheritance.toMutableProperty())
                        .comment("Generated classes will extend from library superclasses if present")
                }

                row {
                    checkBox("Generate manual getters/setters (if no Lombok)")
                        .bindSelected(::generateGettersSetters.toMutableProperty())
                        .comment("Only applies if no Lombok getter/setter annotations are selected")
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
            singleClassMode = singleClassMode,
            singleClass = if (singleClassMode) detectedClass else null
        )
    }
}
