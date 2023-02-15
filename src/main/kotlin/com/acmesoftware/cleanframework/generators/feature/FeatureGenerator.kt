package com.acmesoftware.cleanframework.generators.feature

import com.acmesoftware.cleanframework.utils.TemplateUtils
import com.google.common.base.CaseFormat
import org.apache.commons.lang.text.StrSubstitutor

abstract class FeatureGenerator(packageName: String, val featureName: String, val layer: String, private val templateName: String) {
    private val template: String = TemplateUtils.read("feature/$layer/$templateName.dart.cf")

    private val values: HashMap<String, String> = hashMapOf(
        "package_name" to packageName,
        "feature_name" to featureName,
        "feature_name_snake" to CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, featureName),
        "feature_name_camel" to CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, featureName),
    )

    val fileName get(): String = "${values["feature_name_snake"]}_$templateName.dart"

    fun generate(): String {
        val substitutor = StrSubstitutor(values, "{{", "}}", '\\')
        return substitutor.replace(template)
    }
}