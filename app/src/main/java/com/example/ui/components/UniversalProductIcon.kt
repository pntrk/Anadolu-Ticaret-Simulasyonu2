package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Product

object ProductDrawables {
    fun getProductDrawableResId(productId: String): Int {
        return when (productId.lowercase().trim()) {
            // --- TIER 1 ---
            "iron" -> R.drawable.iron
            "copper" -> R.drawable.copper
            "silicon" -> R.drawable.silicon
            "aluminum" -> R.drawable.aluminum
            "coal" -> R.drawable.coal
            "limestone" -> R.drawable.limestone
            "lithium" -> R.drawable.lithium
            "timber" -> R.drawable.timber
            "chemicals" -> R.drawable.chemicals
            "crude_oil" -> R.drawable.crude_oil
            "natural_gas" -> R.drawable.natural_gas
            "titanium" -> R.drawable.titanium
            "graphite_ore" -> R.drawable.graphite_ore
            "rubber_latex" -> R.drawable.rubber_latex
            "cotton" -> R.drawable.cotton

            // --- TIER 2 ---
            "steel" -> R.drawable.steel
            "wire" -> R.drawable.wire
            "glass" -> R.drawable.glass
            "fabric" -> R.drawable.fabric
            "battery" -> R.drawable.battery
            "plastic" -> R.drawable.plastic
            "wood_plank" -> R.drawable.wood_plank
            "cement" -> R.drawable.cement
            "refined_fuel" -> R.drawable.refined_fuel
            "tire" -> R.drawable.tire
            "packaging" -> R.drawable.packaging
            "aluminum_ingot" -> R.drawable.aluminum_ingot
            "liquefied_gas" -> R.drawable.liquefied_gas
            "titanium_ingot" -> R.drawable.titanium_ingot
            "pcb_substrate" -> R.drawable.pcb_substrate
            "synthetic_textile" -> R.drawable.synthetic_textile
            "titanium_alloy" -> R.drawable.titanium_alloy
            "carbon_fiber" -> R.drawable.carbon_fiber
            "semiconductor_wafer" -> R.drawable.semiconductor_wafer
            "optical_fiber" -> R.drawable.optical_fiber
            "graphene_sheet" -> R.drawable.graphene_sheet

            // --- TIER 3 ---
            "chip" -> R.drawable.chip
            "smartphone" -> R.drawable.smartphone
            "clothing" -> R.drawable.clothing
            "auto_part" -> R.drawable.auto_part
            "appliance" -> R.drawable.appliance
            "furniture" -> R.drawable.furniture
            "building_block" -> R.drawable.building_block
            "pharma" -> R.drawable.pharma
            "machinery" -> R.drawable.machinery
            "solar_panel" -> R.drawable.solar_panel
            "petrochem" -> R.drawable.petrochem
            "biotech_med" -> R.drawable.biotech_med
            "turbine_engine" -> R.drawable.turbine_engine
            "industrial_container" -> R.drawable.industrial_container
            "composite_structure" -> R.drawable.composite_structure
            "robotics_arm" -> R.drawable.robotics_arm
            "telecom_station" -> R.drawable.telecom_station
            "nano_battery" -> R.drawable.nano_battery

            // --- TIER 4 (MEGA PROJELER) ---
            "ev" -> R.drawable.ev
            "uav" -> R.drawable.uav
            "satellite" -> R.drawable.satellite
            "cargo_ship" -> R.drawable.cargo_ship
            "bullet_train" -> R.drawable.bullet_train
            "ai_datacenter" -> R.drawable.ai_datacenter
            "defense_frigate" -> R.drawable.defense_frigate
            "hydrogen_plant" -> R.drawable.hydrogen_plant
            "smart_grid" -> R.drawable.smart_grid
            "super_yacht" -> R.drawable.super_yacht
            "space_rocket" -> R.drawable.space_rocket
            "smart_skyscraper" -> R.drawable.smart_skyscraper
            "fusion_reactor_core" -> R.drawable.fusion_reactor_core
            "quantum_supercomputer" -> R.drawable.quantum_supercomputer
            "autonomous_drone_swarm" -> R.drawable.autonomous_drone_swarm
            "hyperloop_capsule" -> R.drawable.hyperloop_capsule
            "luxury_aircraft_interior" -> R.drawable.luxury_aircraft_interior

            else -> 0
        }
    }

    fun getProjectBannerDrawableResId(productId: String): Int {
        return when (productId.lowercase().trim()) {
            "ev" -> R.drawable.ev_factory_1788253026864
            "uav" -> R.drawable.aerospace_factory_1788253039000
            "satellite" -> R.drawable.space_defense_factory
            "cargo_ship" -> R.drawable.shipyard
            "bullet_train" -> R.drawable.rail_works_1788253076648
            "ai_datacenter" -> R.drawable.datacenter_complex_1788253088719
            "defense_frigate" -> R.drawable.naval_defense_shipyard_1788253101893
            "hydrogen_plant" -> R.drawable.hydrogen_refinery_1788253113250
            "smart_grid" -> R.drawable.smart_city_hub
            "super_yacht" -> R.drawable.luxury_shipyard_1788253148959
            "space_rocket" -> R.drawable.aerospace_launch_complex_1788253163511
            "smart_skyscraper" -> R.drawable.mega_construction_hub_1788253174080
            "fusion_reactor_core" -> R.drawable.fusion_energy_complex_1788253184990
            "quantum_supercomputer" -> R.drawable.img_quantum_tech_complex_v2_1788257750120
            "autonomous_drone_swarm" -> R.drawable.drone_factory_v3_1788258155505
            "hyperloop_capsule" -> R.drawable.img_hyperloop_mega_factory
            "luxury_aircraft_interior" -> R.drawable.img_vip_interiors_complex_v2_1788257771036
            else -> getProductDrawableResId(productId)
        }
    }
}

@Composable
fun UniversalProductIcon(
    product: Product,
    size: Dp = 24.dp,
    tint: Color = Color(product.colorTint)
) {
    UniversalProductIcon(
        productId = product.id,
        displayName = product.displayName,
        fallbackVector = product.icon,
        size = size,
        tint = tint
    )
}

@Composable
fun UniversalProductIcon(
    productId: String,
    displayName: String = productId,
    fallbackVector: ImageVector = Icons.Rounded.Inventory,
    size: Dp = 24.dp,
    tint: Color = Color(0xFF00E5FF)
) {
    val context = LocalContext.current
    val drawableId = remember(productId) {
        val staticRes = ProductDrawables.getProductDrawableResId(productId)
        if (staticRes != 0) {
            staticRes
        } else {
            val candidates = listOf(
                productId.lowercase(),
                "product_${productId.lowercase()}",
                "img_${productId.lowercase()}",
                productId.lowercase().replace("-", "_")
            )
            var foundId = 0
            for (name in candidates) {
                val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
                if (resId != 0) {
                    foundId = resId
                    break
                }
            }
            foundId
        }
    }

    if (drawableId != 0) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
        )
    } else {
        val tier4Ids = setOf(
            "hydrogen_plant", "ev", "bullet_train", "cargo_ship", 
            "defense_frigate", "uav", "satellite", "ai_datacenter", 
            "smart_grid", "super_yacht", "space_rocket", "smart_skyscraper", 
            "fusion_reactor_core", "quantum_supercomputer", 
            "autonomous_drone_swarm", "hyperloop_capsule", "luxury_aircraft_interior"
        )

        if (productId in tier4Ids) {
            Tier4PremiumIcon(
                productId = productId,
                size = size,
                primaryColor = tint
            )
        } else {
            Icon(
                imageVector = fallbackVector,
                contentDescription = displayName,
                tint = tint,
                modifier = Modifier.size(size)
            )
        }
    }
}


