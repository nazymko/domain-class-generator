package com.example.domaingenerator.ui

import com.example.domaingenerator.models.GeneratorConfig
import com.example.domaingenerator.models.LombokAnnotations
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

/**
 * Configuration dialog for domain class generation.
 * Allows users to select source/target packages and Lombok annotations.
 */
class GeneratorConfigDialog(
    private val project: Project
) : DialogWrapper(project) {

    // Package configuration
    private var sourcePackage: String = ""
    private var targetPackage: String = ""

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

    init {
        title = "Generate Domain Classes"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            group("Package Configuration") {
                row("Source Package:") {
                    textField()
                        .bindText(::sourcePackage.toMutableProperty())
                        .columns(40)
                        .comment("Full package name of library classes (e.g., com.library.models)")
                        .focused()
                }

                row("Target Package:") {
                    textField()
                        .bindText(::targetPackage.toMutableProperty())
                        .columns(40)
                        .comment("Full package name for generated domain classes (e.g., com.myapp.domain)")
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

            row {
                comment("""
                    <b>Tip:</b> The plugin will scan all classes in the source package and generate
                    corresponding domain classes in the target package with the selected annotations.
                """.trimIndent())
            }
        }
    }

    /**
     * Validate user input before closing dialog.
     */
    override fun doValidate(): ValidationInfo? {
        if (sourcePackage.isBlank()) {
            return ValidationInfo("Source package cannot be empty", null)
        }
        if (targetPackage.isBlank()) {
            return ValidationInfo("Target package cannot be empty", null)
        }
        if (sourcePackage == targetPackage) {
            return ValidationInfo("Source and target packages must be different", null)
        }
        // Package name validation (basic)
        val packageRegex = Regex("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$")
        if (!sourcePackage.matches(packageRegex)) {
            return ValidationInfo("Invalid source package name format", null)
        }
        if (!targetPackage.matches(packageRegex)) {
            return ValidationInfo("Invalid target package name format", null)
        }
        return null
    }

    /**
     * Get the configuration from dialog inputs.
     */
    fun getConfiguration(): GeneratorConfig {
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
            followInheritance = followInheritance
        )
    }
}
