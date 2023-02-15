package com.acmesoftware.cleanframework.generators.feature

import com.acmesoftware.cleanframework.utils.Template
import com.google.common.base.CaseFormat

abstract class FeatureGenerator(
    private val packageName: String,
    val featureName: String,
    val layer: String,
    private val templateName: String
) {
    private val template = Template("feature/$layer/$templateName.dart.cf")

    private val featureNameSnake = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, featureName)
    private val featureNameCamel = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, featureName)


    val fileName get(): String = "${featureNameSnake}_$templateName.dart"

    fun generate(): String {
        return template.fill(
            hashMapOf(
                "package_name" to packageName,
                "feature_name" to featureName,
                "feature_name_snake" to featureNameSnake,
                "feature_name_camel" to featureNameCamel,
            )
        )
    }
}