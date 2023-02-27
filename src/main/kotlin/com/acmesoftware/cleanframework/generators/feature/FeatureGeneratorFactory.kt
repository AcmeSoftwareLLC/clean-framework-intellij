package com.acmesoftware.cleanframework.generators.feature

import com.acmesoftware.cleanframework.generators.feature.domain.EntityGenerator
import com.acmesoftware.cleanframework.generators.feature.domain.UIOutputGenerator
import com.acmesoftware.cleanframework.generators.feature.domain.UseCaseGenerator
import com.acmesoftware.cleanframework.generators.feature.domain.UseCaseTestGenerator
import com.acmesoftware.cleanframework.generators.feature.presentation.*

object FeatureGeneratorFactory {
    fun getGenerators(packageName: String, featureName: String): List<FeatureGenerator> {
        return listOf(
            EntityGenerator(featureName),
            UIOutputGenerator(featureName),
            UseCaseGenerator(packageName, featureName),
            ViewModelGenerator(featureName),
            PresenterGenerator(packageName, featureName),
            UIGenerator(packageName, featureName),
        )
    }

    fun getTestGenerators(packageName: String, featureName: String): List<FeatureGenerator> {
        return listOf(
            UseCaseTestGenerator(packageName, featureName),
            PresenterTestGenerator(packageName, featureName),
            UITestGenerator(packageName, featureName),
        )
    }
}