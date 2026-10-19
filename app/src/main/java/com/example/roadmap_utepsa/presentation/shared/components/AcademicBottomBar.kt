package com.example.roadmap_utepsa.presentation.shared.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.R

@Composable
internal fun AcademicBottomBar(selectedIndex: Int, enabled: Boolean = true, onSummary: (() -> Unit)?, onCurriculum: () -> Unit,
    onOffer: (() -> Unit)? = null, onRecommendations: (() -> Unit)? = null, compact: Boolean = false) {
    NavigationBar(modifier = if (compact) Modifier else Modifier.height(84.dp), containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = if (compact) NavigationBarDefaults.windowInsets else WindowInsets(0, 0, 0, 0)) {
        val labels = listOf(R.string.academic_summary, R.string.academic_curriculum, R.string.academic_offer, R.string.academic_recommendations)
        val icons = listOf(R.drawable.academic_summary, R.drawable.academic_curriculum, R.drawable.academic_offer, R.drawable.academic_recommendations)
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            NavigationBarItem(selected = selected,
                onClick = { when (index) {
                    0 -> onSummary?.invoke()
                    1 -> onCurriculum()
                    2 -> onOffer?.invoke()
                    3 -> onRecommendations?.invoke()
                } },
                enabled = enabled && when (index) {
                    0 -> onSummary != null
                    1 -> true
                    2 -> onOffer != null
                    else -> onRecommendations != null
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary),
                icon = { Icon(painterResource(icons[index]), null, Modifier.size(21.dp)) },
                label = {
                    if (compact) Text(stringResource(label), fontSize = 11.sp)
                    else Text(stringResource(label), fontSize = 11.sp, lineHeight = 15.sp,
                        letterSpacing = 0.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                })
        }
    }
}
