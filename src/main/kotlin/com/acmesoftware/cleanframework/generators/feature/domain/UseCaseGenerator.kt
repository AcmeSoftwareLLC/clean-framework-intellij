package com.acmesoftware.cleanframework.generators.feature.domain

import com.acmesoftware.cleanframework.generators.feature.FeatureGenerator

class UseCaseGenerator(featureName: String): FeatureGenerator(featureName = featureName, layer = "domain", templateName = "use_case")