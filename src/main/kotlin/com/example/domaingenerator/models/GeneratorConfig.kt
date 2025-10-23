package com.example.domaingenerator.models

/**
 * Configuration for domain class generation.
 */
data class GeneratorConfig(
    val sourcePackage: String,
    val targetPackage: String,
    val lombokAnnotations: LombokAnnotations,
    val generateGettersSetters: Boolean = false,
    val followInheritance: Boolean = true
)

/**
 * Lombok annotations configuration.
 */
data class LombokAnnotations(
    val useData: Boolean = false,
    val useBuilder: Boolean = false,
    val useGetter: Boolean = false,
    val useSetter: Boolean = false,
    val useNoArgsConstructor: Boolean = false,
    val useAllArgsConstructor: Boolean = false,
    val useToString: Boolean = false,
    val useEqualsAndHashCode: Boolean = false
) {
    /**
     * Get list of annotation FQNs to add to generated classes.
     */
    fun getAnnotationFQNs(): List<String> {
        val annotations = mutableListOf<String>()

        if (useData) annotations.add("lombok.Data")
        if (useBuilder) annotations.add("lombok.Builder")
        if (useGetter) annotations.add("lombok.Getter")
        if (useSetter) annotations.add("lombok.Setter")
        if (useNoArgsConstructor) annotations.add("lombok.NoArgsConstructor")
        if (useAllArgsConstructor) annotations.add("lombok.AllArgsConstructor")
        if (useToString) annotations.add("lombok.ToString")
        if (useEqualsAndHashCode) annotations.add("lombok.EqualsAndHashCode")

        return annotations
    }

    /**
     * Check if any Lombok annotation is selected.
     */
    fun hasAnyAnnotation(): Boolean {
        return useData || useBuilder || useGetter || useSetter ||
               useNoArgsConstructor || useAllArgsConstructor ||
               useToString || useEqualsAndHashCode
    }
}
