package com.acmesoftware.cleanframework.generators.feature

import com.acmesoftware.cleanframework.generators.feature.domain.EntityGenerator
import com.acmesoftware.cleanframework.generators.feature.domain.UIOutputGenerator
import com.acmesoftware.cleanframework.generators.feature.domain.UseCaseGenerator
import com.acmesoftware.cleanframework.generators.feature.presentation.PresenterGenerator
import com.acmesoftware.cleanframework.generators.feature.presentation.UIGenerator
import com.acmesoftware.cleanframework.generators.feature.presentation.ViewModelGenerator
import java.util.*

object FeatureGeneratorFactory {
    fun getGenerators(featureName: String): List<FeatureGenerator> {
        return listOf(
                EntityGenerator(featureName),
                UIOutputGenerator(featureName),
                UseCaseGenerator(featureName),
                ViewModelGenerator(featureName),
                UIGenerator(featureName),
                PresenterGenerator(featureName),
        )
    }

    fun snakeCase(featureName: String): String {
        return featureName.replace("([a-z])([A-Z]+)".toRegex(), "$1_$2").lowercase(Locale.getDefault())
    }

    fun camelCase(featureName: String): String {
        return featureName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
    }
}