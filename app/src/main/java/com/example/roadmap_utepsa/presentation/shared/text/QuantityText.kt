package com.example.roadmap_utepsa.presentation.shared.text

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource

@Composable
fun quantityText(@PluralsRes id: Int, count: Int): String = pluralStringResource(id, count, count)
