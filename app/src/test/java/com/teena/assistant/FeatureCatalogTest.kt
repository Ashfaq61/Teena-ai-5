package com.teena.assistant

import com.google.common.truth.Truth.assertThat
import com.teena.assistant.core.model.FeatureCatalog
import com.teena.assistant.core.model.FeatureStatus
import com.teena.assistant.ui.navigation.TeenaDestination
import org.junit.Test

class FeatureCatalogTest {

    @Test
    fun phaseOneCatalogHasUniqueIdsAndValidRoutes() {
        val features = FeatureCatalog.initialFeatures
        val validRoutes = TeenaDestination.entries.map { it.route }.toSet()

        assertThat(features).isNotEmpty()
        assertThat(features.map { it.id }.distinct()).hasSize(features.size)
        features.forEach { feature ->
            assertThat(validRoutes).contains(feature.sectionRoute)
            assertThat(feature.details).isNotEmpty()
        }
    }

    @Test
    fun phaseOneCatalogNeverMarksUnverifiedOrUnbuiltFeaturesAsActive() {
        FeatureCatalog.initialFeatures.forEach { feature ->
            assertThat(feature.status).isNotEqualTo(FeatureStatus.ACTIVE)
        }
    }
}
