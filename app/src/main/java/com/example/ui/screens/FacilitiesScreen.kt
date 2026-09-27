package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.viewmodel.GameIntent
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel

/**
 * FacilitiesScreen - Tesisler Ekranı.
 *
 * Üst kısımdaki SegmentedButton / Tab ile:
 * - [ 🏭 Tesislerim (Liste) ]: Mevcut LazyColumn / 2.5D OSB tesis kartları, üretim, bakım ve yükseltme yönetimi.
 * - [ 🔄 Tedarik Zinciri (Akış) ]: SupplyChainFlowScreen ile uçtan uca sanayi ve lojistik entegrasyon akışı.
 */
@Composable
fun FacilitiesScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    viewModel: GameViewModel,
    initialProductId: String? = null,
    onNavigateToRd: (String?) -> Unit = {},
    onNavigateToConsortium: () -> Unit = {}
) {
    AssetsScreen(
        uiState = uiState,
        onIntent = onIntent,
        viewModel = viewModel,
        initialProductId = initialProductId,
        onNavigateToRd = onNavigateToRd,
        onNavigateToConsortium = onNavigateToConsortium
    )
}
