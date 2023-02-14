package com.acmesoftware.cleanframework.generators.feature.domain

import com.acmesoftware.cleanframework.generators.feature.FeatureGenerator

class EntityGenerator(featureName: String): FeatureGenerator(featureName = featureName, layer = "domain", templateName = "entity")