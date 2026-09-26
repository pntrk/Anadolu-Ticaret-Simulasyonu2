package com.example.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeNeonCyan
import com.example.viewmodel.GameViewModel

data class TechItemData(
    val techKey: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
    @StringRes val tierLabelRes: Int,
    val tierPriority: Int, // 4 for Tier 4 Mega, 3 for Tier 3 Final, 2 for Support
    val baseCost: Long,
    val baseDurationMs: Long,
    val unlockedProductIds: List<String>
)

enum class TechSortOption {
    COST_LOW_TO_HIGH,
    COST_HIGH_TO_LOW,
    TIER_PRIORITY,
    LEVEL_PROGRESS
}

enum class TechFilterOption {
    ALL,
    TIER_4_MEGA,
    TIER_3_FINAL,
    SUPPORT
}

@Composable
fun RdCenterSection(
    uiState: com.example.viewmodel.GameUiState,
    viewModel: GameViewModel,
    highlightedTechKey: String? = null
) {
    val allTechs = remember {
        listOf(
            TechItemData(
                techKey = "aerospace",
                titleRes = R.string.tech_aerospace_title,
                subtitleRes = R.string.tech_aerospace_subtitle,
                icon = Icons.Rounded.RocketLaunch,
                accentColor = Color(0xFF6366F1),
                tierLabelRes = R.string.tech_tier_4_mega,
                tierPriority = 4,
                baseCost = 1_000_000_000L,
                baseDurationMs = 14_400_000L,
                unlockedProductIds = listOf("uav", "satellite", "turbine_engine", "space_rocket", "autonomous_drone_swarm")
            ),
            TechItemData(
                techKey = "quantum_ai",
                titleRes = R.string.tech_quantum_ai_title,
                subtitleRes = R.string.tech_quantum_ai_subtitle,
                icon = Icons.Rounded.Psychology,
                accentColor = Color(0xFF38BDF8),
                tierLabelRes = R.string.tech_tier_4_mega,
                tierPriority = 4,
                baseCost = 2_000_000_000L,
                baseDurationMs = 28_800_000L,
                unlockedProductIds = listOf("chip", "smartphone", "ai_datacenter", "quantum_supercomputer", "semiconductor_wafer", "telecom_station")
            ),
            TechItemData(
                techKey = "heavy_industry",
                titleRes = R.string.tech_heavy_industry_title,
                subtitleRes = R.string.tech_heavy_industry_subtitle,
                icon = Icons.Rounded.PrecisionManufacturing,
                accentColor = Color(0xFFFB8C00),
                tierLabelRes = R.string.tech_tier_4_mega,
                tierPriority = 4,
                baseCost = 800_000_000L,
                baseDurationMs = 10_800_000L,
                unlockedProductIds = listOf("machinery", "auto_part", "cargo_ship", "bullet_train", "defense_frigate", "industrial_container", "robotics_arm", "composite_structure", "super_yacht", "hyperloop_capsule")
            ),
            TechItemData(
                techKey = "nanotech",
                titleRes = R.string.tech_nanotech_title,
                subtitleRes = R.string.tech_nanotech_subtitle,
                icon = Icons.Rounded.Biotech,
                accentColor = Color(0xFFF43F5E),
                tierLabelRes = R.string.tech_tier_4_mega,
                tierPriority = 4,
                baseCost = 1_200_000_000L,
                baseDurationMs = 14_400_000L,
                unlockedProductIds = listOf("nano_battery")
            ),
            TechItemData(
                techKey = "green_energy",
                titleRes = R.string.tech_green_energy_title,
                subtitleRes = R.string.tech_green_energy_subtitle,
                icon = Icons.Rounded.EnergySavingsLeaf,
                accentColor = Color(0xFF4ADE80),
                tierLabelRes = R.string.tech_tier_3_final,
                tierPriority = 3,
                baseCost = 500_000_000L,
                baseDurationMs = 7_200_000L,
                unlockedProductIds = listOf("solar_panel", "ev", "hydrogen_plant", "smart_grid", "fusion_reactor_core")
            ),
            TechItemData(
                techKey = "biotech_cloning",
                titleRes = R.string.tech_biotech_cloning_title,
                subtitleRes = R.string.tech_biotech_cloning_subtitle,
                icon = Icons.Rounded.Coronavirus,
                accentColor = Color(0xFFEC4899),
                tierLabelRes = R.string.tech_tier_3_final,
                tierPriority = 3,
                baseCost = 600_000_000L,
                baseDurationMs = 7_200_000L,
                unlockedProductIds = listOf("pharma", "biotech_med", "canned_food", "gourmet_food", "confectionery_luxury", "chocolate_confectionery", "organic_beverage")
            ),
            TechItemData(
                techKey = "consumer_goods",
                titleRes = R.string.tech_consumer_goods_title,
                subtitleRes = R.string.tech_consumer_goods_subtitle,
                icon = Icons.Rounded.Devices,
                accentColor = Color(0xFF26A69A),
                tierLabelRes = R.string.tech_tier_3_final,
                tierPriority = 3,
                baseCost = 400_000_000L,
                baseDurationMs = 3_600_000L,
                unlockedProductIds = listOf("appliance", "furniture", "clothing", "building_block", "jewelry", "smart_skyscraper", "marble_architecture", "luxury_footwear", "luxury_aircraft_interior")
            ),
            TechItemData(
                techKey = "petrochem",
                titleRes = R.string.tech_petrochem_title,
                subtitleRes = R.string.tech_petrochem_subtitle,
                icon = Icons.Rounded.LocalGasStation,
                accentColor = Color(0xFFFF7043),
                tierLabelRes = R.string.tech_tier_3_final,
                tierPriority = 3,
                baseCost = 500_000_000L,
                baseDurationMs = 5_400_000L,
                unlockedProductIds = listOf("petrochem", "carbon_fiber", "titanium_alloy")
            ),
            TechItemData(
                techKey = "quality_control",
                titleRes = R.string.tech_quality_control_title,
                subtitleRes = R.string.tech_quality_control_subtitle,
                icon = Icons.Rounded.Verified,
                accentColor = Color(0xFFFFD700),
                tierLabelRes = R.string.tech_tier_support,
                tierPriority = 2,
                baseCost = 500_000_000L,
                baseDurationMs = 5_400_000L,
                unlockedProductIds = emptyList()
            ),
            TechItemData(
                techKey = "logistics",
                titleRes = R.string.tech_logistics_title,
                subtitleRes = R.string.tech_logistics_subtitle,
                icon = Icons.Rounded.LocalShipping,
                accentColor = ThemeNeonCyan,
                tierLabelRes = R.string.tech_tier_support,
                tierPriority = 2,
                baseCost = 600_000_000L,
                baseDurationMs = 7_200_000L,
                unlockedProductIds = emptyList()
            ),
            TechItemData(
                techKey = "automation",
                titleRes = R.string.tech_automation_title,
                subtitleRes = R.string.tech_automation_subtitle,
                icon = Icons.Rounded.PrecisionManufacturing,
                accentColor = Color(0xFFA855F7),
                tierLabelRes = R.string.tech_tier_support,
                tierPriority = 2,
                baseCost = 900_000_000L,
                baseDurationMs = 10_800_000L,
                unlockedProductIds = emptyList()
            ),
            TechItemData(
                techKey = "cyber_security",
                titleRes = R.string.tech_cyber_security_title,
                subtitleRes = R.string.tech_cyber_security_subtitle,
                icon = Icons.Rounded.Security,
                accentColor = Color(0xFF10B981),
                tierLabelRes = R.string.tech_tier_support,
                tierPriority = 2,
                baseCost = 500_000_000L,
                baseDurationMs = 5_400_000L,
                unlockedProductIds = emptyList()
            ),
            TechItemData(
                techKey = "global_finance",
                titleRes = R.string.tech_global_finance_title,
                subtitleRes = R.string.tech_global_finance_subtitle,
                icon = Icons.Rounded.CurrencyExchange,
                accentColor = Color(0xFFEAB308),
                tierLabelRes = R.string.tech_tier_support,
                tierPriority = 2,
                baseCost = 700_000_000L,
                baseDurationMs = 14_400_000L,
                unlockedProductIds = emptyList()
            ),
            TechItemData(
                techKey = "cultural_heritage",
                titleRes = R.string.tech_cultural_heritage_title,
                subtitleRes = R.string.tech_cultural_heritage_subtitle,
                icon = Icons.Rounded.AccountBalance,
                accentColor = Color(0xFFD946EF),
                tierLabelRes = R.string.tech_tier_support,
                tierPriority = 2,
                baseCost = 400_000_000L,
                baseDurationMs = 10_800_000L,
                unlockedProductIds = emptyList()
            )
        )
    }

    // Direct streamlined Sci-Fi Lab HUD & Tech Tree
    RdLabCenterVisualView(
        uiState = uiState,
        viewModel = viewModel,
        allTechs = allTechs,
        highlightedTechKey = highlightedTechKey
    )
}

@Composable
fun GameChip(selected: Boolean, text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (selected) ThemeNeonCyan.copy(alpha = 0.15f) else Color(0xFF131C2E),
        border = BorderStroke(1.dp, if (selected) ThemeNeonCyan.copy(alpha = 0.8f) else Color(0xFF334155).copy(alpha = 0.5f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        CurrencyText(
            text = text,
            color = if (selected) ThemeNeonCyan else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = RobotoMonoFontFamily,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
