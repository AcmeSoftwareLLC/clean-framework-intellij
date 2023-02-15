package com.acmesoftware.cleanframework.generators.feature

import com.google.common.base.CaseFormat
import com.google.common.io.CharStreams
import org.apache.commons.lang.text.StrSubstitutor
import java.io.InputStreamReader

abstract class FeatureGenerator(packageName: String, val featureName: String, val layer: String, private val templateName: String) {
    private val template: String
    private val values: HashMap<String, String> = hashMapOf(
        "package_name" to packageName,
        "feature_name" to featureName,
        "feature_name_snake" to CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, featureName),
        "feature_name_camel" to CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, featureName),
    )

    init {
        try {
            val resource = "/templates/feature/$layer/$templateName.dart.cf"
            val resourceStream = this::class.java.getResourceAsStream(resource)!!
            template = CharStreams.toString(InputStreamReader(resourceStream, Charsets.UTF_8))
        } catch (e: Exception) {
            throw Exception("Could not find template: $layer/$templateName")
        }
    }

    val fileName get(): String = "${values["feature_name_snake"]}_$templateName.dart"

    fun generate(): String {
        val substitutor = StrSubstitutor(values, "{{", "}}", '\\')
        return substitutor.replace(template)
    }
}