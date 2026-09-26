package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.RdCenterSection
import com.example.viewmodel.GameIntent
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel

@Composable
fun RdScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    viewModel: GameViewModel,
    initialHighlightedTechKey: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B14))
    ) {
        // High-Tech Laboratory Fullscreen Background (bg_ar_ge.webp)
        Image(
            painter = painterResource(id = R.drawable.bg_ar_ge),
            contentDescription = "Ar-Ge Laboratuvarı",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.88f)
        )

        // Balanced subtle sci-fi gradient scrim keeping background clearly visible while ensuring contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030712).copy(alpha = 0.25f),
                            Color(0xFF040A18).copy(alpha = 0.20f),
                            Color(0xFF030814).copy(alpha = 0.45f),
                            Color(0xFF02050C).copy(alpha = 0.70f)
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
        ) {
            item {
                RdCenterSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    highlightedTechKey = initialHighlightedTechKey
                )
            }
        }
    }
}


