package com.example.roadmap_utepsa.presentation.navigation
import androidx.compose.runtime.Composable

@Composable
internal fun AcademicNavigation(destination: AcademicDestination, recommendationsAvailable: Boolean, offerAvailable: Boolean,
    recommendations: @Composable () -> Unit, offer: @Composable () -> Unit,
    summary: @Composable () -> Unit, curriculum: @Composable () -> Unit) {
    when {
        destination == AcademicDestination.RECOMMENDATIONS && recommendationsAvailable -> recommendations()
        destination == AcademicDestination.OFFER && offerAvailable -> offer()
        destination == AcademicDestination.SUMMARY -> summary()
        else -> curriculum()
    }
}
