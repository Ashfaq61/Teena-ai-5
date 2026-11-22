package com.teena.assistant

import com.google.common.truth.Truth.assertThat
import com.teena.assistant.ui.navigation.TeenaDestination
import org.junit.Test

class TeenaDestinationTest {

    @Test
    fun allSixteenRequiredSectionsExistWithUniqueRoutes() {
        val destinations = TeenaDestination.entries
        assertThat(destinations).hasSize(16)
        assertThat(destinations.map { it.route }.distinct()).hasSize(16)
        assertThat(destinations.map { it.title }.distinct()).hasSize(16)
    }

    @Test
    fun fromRouteFallsBackToDashboardWhenUnknown() {
        assertThat(TeenaDestination.fromRoute("unknown_route")).isEqualTo(TeenaDestination.DASHBOARD)
        assertThat(TeenaDestination.fromRoute(null)).isEqualTo(TeenaDestination.DASHBOARD)
        assertThat(TeenaDestination.fromRoute("live_voice")).isEqualTo(TeenaDestination.LIVE_VOICE)
    }
}
