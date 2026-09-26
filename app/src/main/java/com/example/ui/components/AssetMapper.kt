package com.example.ui.components

import com.example.R

fun getFacility3DResource(facilityId: String): Int {
    val lower = facilityId.lowercase()
    return when {
        lower.contains("farm") || lower.contains("orchard") || lower.contains("plantation") || lower.contains("ranch") || lower.contains("livestock") -> R.drawable.icon_farm_3d
        lower.contains("mine") || lower.contains("quarry") || lower.contains("pit") || lower.contains("well") -> R.drawable.icon_mine_3d
        lower.contains("tech") || lower.contains("chip") || lower.contains("satellite") || lower.contains("aerospace") || lower.contains("quantum") || lower.contains("datacenter") || lower.contains("robotics") -> R.drawable.icon_tech_factory_3d
        lower.contains("lab") || lower.contains("research") -> R.drawable.icon_lab_3d
        lower.contains("bank") || lower.contains("finance") -> R.drawable.icon_bank_3d
        else -> R.drawable.icon_factory_3d
    }
}
