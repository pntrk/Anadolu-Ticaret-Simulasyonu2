package com.example.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.R

enum class ProductTier(val baseDurationMs: Long) {
    TIER_1(18_000_000L * 5L), // 2.5 hours (halved)
    TIER_2(36_000_000L * 5L), // 5 hours (halved)
    TIER_3(108_000_000L * 5L), // 15 hours (halved)
    TIER_4(216_000_000L * 5L) // 30 hours (halved)
}

data class RecipeRequirement(
    val productId: String,
    val amountPerUnit: Int
)

data class TechNode(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val subtitleRes: Int = 0,
    @StringRes val tierLabelRes: Int = 0,
    val requiredLevel: Int,
    val costMoney: Long,
    val costGems: Int = 2,
    val durationMs: Long,
    val gemSkipCost: Int,
    val unlockedProductIds: List<String>
) {
    val name: String
        @Composable
        get() = stringResource(nameRes)

    val subtitle: String
        @Composable
        get() = if (subtitleRes != 0) stringResource(subtitleRes) else ""

    val tierLabel: String
        @Composable
        get() = if (tierLabelRes != 0) stringResource(tierLabelRes) else ""

    fun getName(context: Context? = null): String =
        if (context != null) context.getString(nameRes) else id.split("_").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
}

object TechTree {
    val nodes = listOf(
        TechNode(
            id = "tech_green_energy",
            nameRes = R.string.tech_green_energy_title,
            subtitleRes = R.string.tech_green_energy_subtitle,
            tierLabelRes = R.string.tech_tier_3_final,
            requiredLevel = 1,
            costMoney = 5_000_000L,
            costGems = 2,
            durationMs = 14_400_000L, // 4 hours
            gemSkipCost = 25,
            unlockedProductIds = listOf("solar_panel", "ev", "hydrogen_plant", "smart_grid", "fusion_reactor_core")
        ),
        TechNode(
            id = "tech_aerospace",
            nameRes = R.string.tech_aerospace_title,
            subtitleRes = R.string.tech_aerospace_subtitle,
            tierLabelRes = R.string.tech_tier_4_mega,
            requiredLevel = 1,
            costMoney = 10_000_000L,
            costGems = 5,
            durationMs = 28_800_000L, // 8 hours
            gemSkipCost = 50,
            unlockedProductIds = listOf("uav", "satellite", "turbine_engine", "space_rocket", "autonomous_drone_swarm")
        ),
        TechNode(
            id = "tech_quantum_ai",
            nameRes = R.string.tech_quantum_ai_title,
            subtitleRes = R.string.tech_quantum_ai_subtitle,
            tierLabelRes = R.string.tech_tier_4_mega,
            requiredLevel = 1,
            costMoney = 20_000_000L,
            costGems = 10,
            durationMs = 57_600_000L, // 16 hours
            gemSkipCost = 100,
            unlockedProductIds = listOf("chip", "smartphone", "ai_datacenter", "quantum_supercomputer", "semiconductor_wafer", "telecom_station")
        ),
        TechNode(
            id = "tech_heavy_industry",
            nameRes = R.string.tech_heavy_industry_title,
            subtitleRes = R.string.tech_heavy_industry_subtitle,
            tierLabelRes = R.string.tech_tier_4_mega,
            requiredLevel = 1,
            costMoney = 8_000_000L,
            costGems = 3,
            durationMs = 21_600_000L, // 6 hours
            gemSkipCost = 40,
            unlockedProductIds = listOf("machinery", "auto_part", "cargo_ship", "bullet_train", "defense_frigate", "industrial_container", "robotics_arm", "composite_structure", "super_yacht", "hyperloop_capsule")
        ),
        TechNode(
            id = "tech_biotech_med",
            nameRes = R.string.tech_biotech_med_title,
            subtitleRes = R.string.tech_biotech_med_subtitle,
            tierLabelRes = R.string.tech_tier_3_final,
            requiredLevel = 1,
            costMoney = 6_000_000L,
            costGems = 2,
            durationMs = 14_400_000L, // 4 hours
            gemSkipCost = 30,
            unlockedProductIds = listOf("pharma", "biotech_med")
        ),
        TechNode(
            id = "tech_biotech_cloning",
            nameRes = R.string.tech_biotech_cloning_title,
            subtitleRes = R.string.tech_biotech_cloning_subtitle,
            tierLabelRes = R.string.tech_tier_3_final,
            requiredLevel = 1,
            costMoney = 6_000_000L,
            costGems = 2,
            durationMs = 14_400_000L,
            gemSkipCost = 30,
            unlockedProductIds = listOf("pharma", "biotech_med")
        ),
        TechNode(
            id = "tech_consumer_goods",
            nameRes = R.string.tech_consumer_goods_title,
            subtitleRes = R.string.tech_consumer_goods_subtitle,
            tierLabelRes = R.string.tech_tier_3_final,
            requiredLevel = 1,
            costMoney = 4_000_000L,
            costGems = 1,
            durationMs = 7_200_000L, // 2 hours
            gemSkipCost = 15,
            unlockedProductIds = listOf("appliance", "furniture", "clothing", "building_block", "smart_skyscraper", "luxury_aircraft_interior")
        ),
        TechNode(
            id = "tech_petrochem",
            nameRes = R.string.tech_petrochem_title,
            subtitleRes = R.string.tech_petrochem_subtitle,
            tierLabelRes = R.string.tech_tier_3_final,
            requiredLevel = 1,
            costMoney = 5_000_000L,
            costGems = 2,
            durationMs = 10_800_000L, // 3 hours
            gemSkipCost = 20,
            unlockedProductIds = listOf("petrochem", "carbon_fiber", "titanium_alloy")
        ),
        TechNode(
            id = "tech_quality_control",
            nameRes = R.string.tech_quality_control_title,
            subtitleRes = R.string.tech_quality_control_subtitle,
            tierLabelRes = R.string.tech_tier_support,
            requiredLevel = 1,
            costMoney = 5_000_000L,
            costGems = 2,
            durationMs = 10_800_000L,
            gemSkipCost = 20,
            unlockedProductIds = emptyList()
        ),
        TechNode(
            id = "tech_logistics",
            nameRes = R.string.tech_logistics_title,
            subtitleRes = R.string.tech_logistics_subtitle,
            tierLabelRes = R.string.tech_tier_support,
            requiredLevel = 1,
            costMoney = 6_000_000L,
            costGems = 2,
            durationMs = 14_400_000L,
            gemSkipCost = 25,
            unlockedProductIds = emptyList()
        ),
        TechNode(
            id = "tech_automation",
            nameRes = R.string.tech_automation_title,
            subtitleRes = R.string.tech_automation_subtitle,
            tierLabelRes = R.string.tech_tier_support,
            requiredLevel = 1,
            costMoney = 9_000_000L,
            costGems = 4,
            durationMs = 21_600_000L,
            gemSkipCost = 35,
            unlockedProductIds = emptyList()
        ),
        TechNode(
            id = "tech_nanotech",
            nameRes = R.string.tech_nanotech_title,
            subtitleRes = R.string.tech_nanotech_subtitle,
            tierLabelRes = R.string.tech_tier_4_mega,
            requiredLevel = 1,
            costMoney = 12_000_000L,
            costGems = 6,
            durationMs = 28_800_000L,
            gemSkipCost = 50,
            unlockedProductIds = listOf("nano_battery")
        ),
        TechNode(
            id = "tech_cyber_security",
            nameRes = R.string.tech_cyber_security_title,
            subtitleRes = R.string.tech_cyber_security_subtitle,
            tierLabelRes = R.string.tech_tier_support,
            requiredLevel = 1,
            costMoney = 7_500_000L,
            costGems = 3,
            durationMs = 14_400_000L,
            gemSkipCost = 30,
            unlockedProductIds = emptyList()
        ),
        TechNode(
            id = "tech_global_finance",
            nameRes = R.string.tech_global_finance_title,
            subtitleRes = R.string.tech_global_finance_subtitle,
            tierLabelRes = R.string.tech_tier_support,
            requiredLevel = 1,
            costMoney = 7_000_000L,
            costGems = 3,
            durationMs = 14_400_000L,
            gemSkipCost = 30,
            unlockedProductIds = emptyList()
        ),
        TechNode(
            id = "tech_cultural_heritage",
            nameRes = R.string.tech_cultural_heritage_title,
            subtitleRes = R.string.tech_cultural_heritage_subtitle,
            tierLabelRes = R.string.tech_tier_support,
            requiredLevel = 1,
            costMoney = 4_000_000L,
            costGems = 2,
            durationMs = 10_800_000L,
            gemSkipCost = 20,
            unlockedProductIds = emptyList()
        )
    )
}

enum class Product(
    val id: String,
    @StringRes val displayNameRes: Int,
    val tier: ProductTier,
    val basePrice: Long,
    val productionCost: Long,
    val facilityId: String,
    @StringRes val facilityNameRes: Int,
    val facilityCost: Long,
    val recipe: List<RecipeRequirement> = emptyList(),
    val icon: ImageVector,
    val colorTint: Long
) {
    // --- TIER 1: HAM MADDELER ---
    IRON("iron", R.string.product_iron, ProductTier.TIER_1, 400L, 40L, "iron_mine", R.string.facility_iron_mine, 160000L, emptyList(), Icons.Rounded.Hardware, 0xFFB0BEC5),
    COPPER("copper", R.string.product_copper, ProductTier.TIER_1, 400L, 40L, "copper_mine", R.string.facility_copper_mine, 160000L, emptyList(), Icons.Rounded.Polyline, 0xFFFF8A65),
    SILICON("silicon", R.string.product_silicon, ProductTier.TIER_1, 400L, 40L, "silicon_mine", R.string.facility_silicon_mine, 160000L, emptyList(), Icons.Rounded.Hexagon, 0xFF4DD0E1),
    ALUMINUM("aluminum", R.string.product_aluminum, ProductTier.TIER_1, 380L, 38L, "aluminum_mine", R.string.facility_aluminum_mine, 150000L, emptyList(), Icons.Rounded.Layers, 0xFF90CAF9),
    COAL("coal", R.string.product_coal, ProductTier.TIER_1, 300L, 30L, "coal_mine", R.string.facility_coal_mine, 120000L, emptyList(), Icons.Rounded.Terrain, 0xFF546E7A),
    LIMESTONE("limestone", R.string.product_limestone, ProductTier.TIER_1, 150L, 15L, "limestone_quarry", R.string.facility_limestone_quarry, 60000L, emptyList(), Icons.Rounded.Terrain, 0xFFE0E0E0),
    LITHIUM("lithium", R.string.product_lithium, ProductTier.TIER_1, 600L, 60L, "lithium_mine", R.string.facility_lithium_mine, 240000L, emptyList(), Icons.Rounded.ElectricBolt, 0xFF80DEEA),
    TIMBER("timber", R.string.product_timber, ProductTier.TIER_1, 100L, 10L, "timber_camp", R.string.facility_timber_camp, 40000L, emptyList(), Icons.Rounded.Forest, 0xFF8D6E63),
    CHEMICALS("chemicals", R.string.product_chemicals, ProductTier.TIER_1, 380L, 38L, "chemical_plant", R.string.facility_chemical_plant, 150000L, emptyList(), Icons.Rounded.Science, 0xFF00E676),
    CRUDE_OIL("crude_oil", R.string.product_crude_oil, ProductTier.TIER_1, 500L, 50L, "oil_well", R.string.facility_oil_well, 200000L, emptyList(), Icons.Rounded.LocalGasStation, 0xFF37474F),
    NATURAL_GAS("natural_gas", R.string.product_natural_gas, ProductTier.TIER_1, 450L, 45L, "gas_well", R.string.facility_gas_well, 180000L, emptyList(), Icons.Rounded.Propane, 0xFF00B0FF),
    TITANIUM("titanium", R.string.product_titanium, ProductTier.TIER_1, 700L, 70L, "titanium_mine", R.string.facility_titanium_mine, 280000L, emptyList(), Icons.Rounded.Shield, 0xFF90A4AE),
    GRAPHITE_ORE("graphite_ore", R.string.product_graphite_ore, ProductTier.TIER_1, 450L, 45L, "graphite_mine", R.string.facility_graphite_mine, 180000L, emptyList(), Icons.Rounded.Terrain, 0xFF455A64),
    RUBBER_LATEX("rubber_latex", R.string.product_rubber_latex, ProductTier.TIER_1, 250L, 25L, "rubber_farm", R.string.facility_rubber_farm, 100000L, emptyList(), Icons.Rounded.Factory, 0xFF66BB6A),
    COTTON("cotton", R.string.product_cotton, ProductTier.TIER_1, 150L, 15L, "cotton_farm", R.string.facility_cotton_farm, 60000L, emptyList(), Icons.Rounded.Cloud, 0xFFF5F5F5),

    // --- TIER 2: İŞLENMİŞ ARA MALLAR ---
    STEEL("steel", R.string.product_steel, ProductTier.TIER_2, 3470L, 266L, "steel_mill", R.string.facility_steel_mill, 800000L, listOf(RecipeRequirement("iron", 2), RecipeRequirement("coal", 1)), Icons.Rounded.Hardware, 0xFF90A4AE),
    WIRE("wire", R.string.product_wire, ProductTier.TIER_2, 2400L, 200L, "wire_factory", R.string.facility_wire_factory, 600000L, listOf(RecipeRequirement("copper", 1), RecipeRequirement("chemicals", 1)), Icons.Rounded.Hub, 0xFFFFB74D),
    GLASS("glass", R.string.product_glass, ProductTier.TIER_2, 1400L, 100L, "glassworks", R.string.facility_glassworks, 300000L, listOf(RecipeRequirement("silicon", 1), RecipeRequirement("limestone", 1)), Icons.Rounded.Window, 0xFFB3E5FC),
    FABRIC("fabric", R.string.product_fabric, ProductTier.TIER_2, 1970L, 166L, "textile_workshop", R.string.facility_textile_workshop, 500000L, listOf(RecipeRequirement("cotton", 2)), Icons.Rounded.Checkroom, 0xFFE0E0E0),
    BATTERY("battery", R.string.product_battery, ProductTier.TIER_2, 3930L, 333L, "power_plant", R.string.facility_power_plant, 1000000L, listOf(RecipeRequirement("lithium", 1), RecipeRequirement("chemicals", 1), RecipeRequirement("copper", 1)), Icons.Rounded.BatteryChargingFull, 0xFF64FFDA),
    PLASTIC("plastic", R.string.product_plastic, ProductTier.TIER_2, 2710L, 233L, "plastic_factory", R.string.facility_plastic_factory, 700000L, listOf(RecipeRequirement("chemicals", 1), RecipeRequirement("crude_oil", 1)), Icons.Rounded.Category, 0xFFFF4081),
    WOOD_PLANK("wood_plank", R.string.product_wood_plank, ProductTier.TIER_2, 1200L, 100L, "sawmill", R.string.facility_sawmill, 300000L, listOf(RecipeRequirement("timber", 2)), Icons.Rounded.Carpenter, 0xFFA1887F),
    CEMENT("cement", R.string.product_cement, ProductTier.TIER_2, 1230L, 93L, "cement_factory", R.string.facility_cement_factory, 280000L, listOf(RecipeRequirement("limestone", 2), RecipeRequirement("coal", 1)), Icons.Rounded.Foundation, 0xFF9E9E9E),
    REFINED_FUEL("refined_fuel", R.string.product_refined_fuel, ProductTier.TIER_2, 4000L, 300L, "petroleum_refinery", R.string.facility_petroleum_refinery, 900000L, listOf(RecipeRequirement("crude_oil", 2)), Icons.Rounded.Propane, 0xFFFF7043),
    TIRE("tire", R.string.product_tire, ProductTier.TIER_2, 2830L, 233L, "tire_factory", R.string.facility_tire_factory, 700000L, listOf(RecipeRequirement("rubber_latex", 2), RecipeRequirement("chemicals", 1)), Icons.Rounded.TireRepair, 0xFF78909C),
    PACKAGING("packaging", R.string.product_packaging, ProductTier.TIER_2, 1300L, 120L, "paper_mill", R.string.facility_paper_mill, 360000L, listOf(RecipeRequirement("timber", 1), RecipeRequirement("chemicals", 1)), Icons.Rounded.Inventory2, 0xFFD7CCC8),
    ALUMINUM_INGOT("aluminum_ingot", R.string.product_aluminum_ingot, ProductTier.TIER_2, 2890L, 213L, "aluminum_smelter", R.string.facility_aluminum_smelter, 640000L, listOf(RecipeRequirement("aluminum", 2), RecipeRequirement("coal", 1)), Icons.Rounded.ViewInAr, 0xFF80DEEA),
    LIQUEFIED_GAS("liquefied_gas", R.string.product_liquefied_gas, ProductTier.TIER_2, 3430L, 253L, "gas_processing_plant", R.string.facility_gas_processing_plant, 760000L, listOf(RecipeRequirement("natural_gas", 2)), Icons.Rounded.PropaneTank, 0xFF00E5FF),
    TITANIUM_INGOT("titanium_ingot", R.string.product_titanium_ingot, ProductTier.TIER_2, 5070L, 366L, "titanium_smelter", R.string.facility_titanium_smelter, 1100000L, listOf(RecipeRequirement("titanium", 2), RecipeRequirement("coal", 1)), Icons.Rounded.Layers, 0xFF78909C),
    PCB_SUBSTRATE("pcb_substrate", R.string.product_pcb_substrate, ProductTier.TIER_2, 3600L, 320L, "pcb_works", R.string.facility_pcb_works, 960000L, listOf(RecipeRequirement("copper", 1), RecipeRequirement("silicon", 1), RecipeRequirement("chemicals", 1)), Icons.Rounded.Memory, 0xFF00E676),
    SYNTHETIC_TEXTILE("synthetic_textile", R.string.product_synthetic_textile, ProductTier.TIER_2, 2230L, 173L, "synthetic_mill", R.string.facility_synthetic_mill, 520000L, listOf(RecipeRequirement("crude_oil", 1), RecipeRequirement("chemicals", 1)), Icons.Rounded.Checkroom, 0xFFB39DDB),
    TITANIUM_ALLOY("titanium_alloy", R.string.product_titanium_alloy, ProductTier.TIER_2, 5400L, 400L, "titanium_alloy_plant", R.string.facility_titanium_alloy_plant, 1200000L, listOf(RecipeRequirement("titanium", 2), RecipeRequirement("aluminum", 1)), Icons.Rounded.Layers, 0xFF78909C),
    CARBON_FIBER("carbon_fiber", R.string.product_carbon_fiber, ProductTier.TIER_2, 4670L, 366L, "carbon_fiber_works", R.string.facility_carbon_fiber_works, 1100000L, listOf(RecipeRequirement("crude_oil", 2), RecipeRequirement("chemicals", 1)), Icons.Rounded.Grid3x3, 0xFF37474F),
    SEMICONDUCTOR_WAFER("semiconductor_wafer", R.string.product_semiconductor_wafer, ProductTier.TIER_2, 6130L, 533L, "semiconductor_foundry", R.string.facility_semiconductor_foundry, 1600000L, listOf(RecipeRequirement("silicon", 2), RecipeRequirement("chemicals", 2)), Icons.Rounded.Memory, 0xFF00E5FF),
    OPTICAL_FIBER("optical_fiber", R.string.product_optical_fiber, ProductTier.TIER_2, 4800L, 400L, "fiber_optics_factory", R.string.facility_fiber_optics_factory, 1200000L, listOf(RecipeRequirement("silicon", 2), RecipeRequirement("copper", 1)), Icons.Rounded.Hub, 0xFF00E5FF),
    GRAPHENE_SHEET("graphene_sheet", R.string.product_graphene_sheet, ProductTier.TIER_2, 3700L, 280L, "graphene_lab", R.string.facility_graphene_lab, 840000L, listOf(RecipeRequirement("graphite_ore", 2), RecipeRequirement("chemicals", 1)), Icons.Rounded.Grid3x3, 0xFF37474F),

    // --- TIER 3: NİHAİ ÜRÜNLER & YÜKSEK TEKNOLOJİ ---
    CHIP("chip", R.string.product_chip, ProductTier.TIER_3, 56100L, 5000L, "chip_factory", R.string.facility_chip_factory, 10000000L, listOf(RecipeRequirement("semiconductor_wafer", 1), RecipeRequirement("wire", 1), RecipeRequirement("pcb_substrate", 1)), Icons.Rounded.Memory, 0xFFE040FB),
    SMARTPHONE("smartphone", R.string.product_smartphone, ProductTier.TIER_3, 78600L, 7500L, "smartphone_factory", R.string.facility_smartphone_factory, 15000000L, listOf(RecipeRequirement("pcb_substrate", 1), RecipeRequirement("glass", 1), RecipeRequirement("battery", 1), RecipeRequirement("wire", 1)), Icons.Rounded.Smartphone, 0xFF18FFFF),
    CLOTHING("clothing", R.string.product_clothing, ProductTier.TIER_3, 13900L, 1000L, "clothing_factory", R.string.facility_clothing_factory, 2000000L, listOf(RecipeRequirement("fabric", 2), RecipeRequirement("packaging", 1)), Icons.Rounded.Checkroom, 0xFFF06292),
    AUTO_PART("auto_part", R.string.product_auto_part, ProductTier.TIER_3, 26900L, 2000L, "auto_factory", R.string.facility_auto_factory, 4000000L, listOf(RecipeRequirement("steel", 2), RecipeRequirement("battery", 1), RecipeRequirement("tire", 1)), Icons.Rounded.CarRepair, 0xFFFF5252),
    APPLIANCE("appliance", R.string.product_appliance, ProductTier.TIER_3, 28500L, 2500L, "appliance_factory", R.string.facility_appliance_factory, 5000000L, listOf(RecipeRequirement("steel", 1), RecipeRequirement("plastic", 1), RecipeRequirement("wire", 1)), Icons.Rounded.Kitchen, 0xFF84FFFF),
    FURNITURE("furniture", R.string.product_furniture, ProductTier.TIER_3, 12400L, 1000L, "furniture_factory", R.string.facility_furniture_factory, 2000000L, listOf(RecipeRequirement("wood_plank", 2), RecipeRequirement("fabric", 1)), Icons.Rounded.Chair, 0xFFBCAAA4),
    BUILDING_BLOCK("building_block", R.string.product_building_block, ProductTier.TIER_3, 12500L, 1000L, "construction_factory", R.string.facility_construction_factory, 2000000L, listOf(RecipeRequirement("cement", 2), RecipeRequirement("steel", 1), RecipeRequirement("glass", 1)), Icons.Rounded.Architecture, 0xFF78909C),
    PHARMA_COSMETICS("pharma", R.string.product_pharma, ProductTier.TIER_3, 12800L, 1200L, "pharma_lab", R.string.facility_pharma_lab, 2400000L, listOf(RecipeRequirement("chemicals", 2), RecipeRequirement("synthetic_textile", 1), RecipeRequirement("packaging", 1)), Icons.Rounded.MedicalServices, 0xFFF48FB1),
    HEAVY_MACHINERY("machinery", R.string.product_machinery, ProductTier.TIER_3, 41900L, 3500L, "machinery_factory", R.string.facility_machinery_factory, 7000000L, listOf(RecipeRequirement("steel", 2), RecipeRequirement("auto_part", 1), RecipeRequirement("tire", 1)), Icons.Rounded.Engineering, 0xFFFFB300),
    SOLAR_PANEL("solar_panel", R.string.product_solar_panel, ProductTier.TIER_3, 30400L, 3000L, "solar_factory", R.string.facility_solar_factory, 6000000L, listOf(RecipeRequirement("silicon", 1), RecipeRequirement("glass", 1), RecipeRequirement("aluminum_ingot", 1), RecipeRequirement("wire", 1)), Icons.Rounded.WbSunny, 0xFFFFD54F),
    PETROCHEM("petrochem", R.string.product_petrochem, ProductTier.TIER_3, 24000L, 1600L, "petrochem_complex", R.string.facility_petrochem_complex, 3200000L, listOf(RecipeRequirement("refined_fuel", 2), RecipeRequirement("chemicals", 1), RecipeRequirement("packaging", 1)), Icons.Rounded.Opacity, 0xFF00E5FF),
    BIOTECH_MED("biotech_med", R.string.product_biotech_med, ProductTier.TIER_3, 44800L, 3200L, "medtech_factory", R.string.facility_medtech_factory, 6400000L, listOf(RecipeRequirement("pharma", 1), RecipeRequirement("chip", 1), RecipeRequirement("plastic", 1), RecipeRequirement("aluminum_ingot", 1)), Icons.Rounded.Biotech, 0xFFE91E63),
    TURBINE_ENGINE("turbine_engine", R.string.product_turbine_engine, ProductTier.TIER_3, 48900L, 4200L, "turbine_works", R.string.facility_turbine_works, 8400000L, listOf(RecipeRequirement("steel", 2), RecipeRequirement("aluminum_ingot", 1), RecipeRequirement("wire", 1), RecipeRequirement("chip", 1)), Icons.Rounded.WindPower, 0xFF00E676),
    INDUSTRIAL_CONTAINER("industrial_container", R.string.product_industrial_container, ProductTier.TIER_3, 20900L, 1400L, "container_factory", R.string.facility_container_factory, 2800000L, listOf(RecipeRequirement("steel", 2), RecipeRequirement("aluminum_ingot", 1), RecipeRequirement("packaging", 1)), Icons.Rounded.AllInbox, 0xFF78909C),
    COMPOSITE_STRUCTURE("composite_structure", R.string.product_composite_structure, ProductTier.TIER_3, 44300L, 3500L, "composite_plant", R.string.facility_composite_plant, 7000000L, listOf(RecipeRequirement("carbon_fiber", 2), RecipeRequirement("aluminum_ingot", 1), RecipeRequirement("titanium_ingot", 1)), Icons.Rounded.Architecture, 0xFF607D8B),
    ROBOTICS_ARM("robotics_arm", R.string.product_robotics_arm, ProductTier.TIER_3, 51900L, 4500L, "robotics_factory", R.string.facility_robotics_factory, 9000000L, listOf(RecipeRequirement("steel", 2), RecipeRequirement("wire", 1), RecipeRequirement("pcb_substrate", 1), RecipeRequirement("battery", 1)), Icons.Rounded.PrecisionManufacturing, 0xFFFF9800),
    TELECOM_STATION("telecom_station", R.string.product_telecom_station, ProductTier.TIER_3, 47600L, 3800L, "telecom_factory", R.string.facility_telecom_factory, 7600000L, listOf(RecipeRequirement("optical_fiber", 2), RecipeRequirement("pcb_substrate", 1), RecipeRequirement("aluminum_ingot", 1)), Icons.Rounded.Router, 0xFF00E5FF),
    NANO_BATTERY("nano_battery", R.string.product_nano_battery, ProductTier.TIER_3, 49400L, 4200L, "nano_battery_factory", R.string.facility_nano_battery_factory, 8400000L, listOf(RecipeRequirement("graphene_sheet", 2), RecipeRequirement("battery", 1), RecipeRequirement("titanium_ingot", 1)), Icons.Rounded.ElectricBolt, 0xFF76FF03),

    // --- TIER 4: MEGA PROJELER & STRATEJİK SANAYİ ---
    EV("ev", R.string.product_ev, ProductTier.TIER_4, 354000L, 30000L, "ev_factory", R.string.facility_ev_factory, 30000000L, listOf(RecipeRequirement("auto_part", 2), RecipeRequirement("battery", 2), RecipeRequirement("chip", 1), RecipeRequirement("glass", 1)), Icons.Rounded.ElectricCar, 0xFF00E676),
    UAV("uav", R.string.product_uav, ProductTier.TIER_4, 427000L, 40000L, "aerospace_factory", R.string.facility_aerospace_factory, 40000000L, listOf(RecipeRequirement("auto_part", 1), RecipeRequirement("chip", 2), RecipeRequirement("titanium_ingot", 1), RecipeRequirement("solar_panel", 1)), Icons.Rounded.FlightTakeoff, 0xFF29B6F6),
    SATELLITE("satellite", R.string.product_satellite, ProductTier.TIER_4, 768000L, 60000L, "space_defense_factory", R.string.facility_space_defense_factory, 60000000L, listOf(RecipeRequirement("chip", 3), RecipeRequirement("solar_panel", 2), RecipeRequirement("titanium_ingot", 1), RecipeRequirement("machinery", 1)), Icons.Rounded.SatelliteAlt, 0xFFBA68C8),
    CARGO_SHIP("cargo_ship", R.string.product_cargo_ship, ProductTier.TIER_4, 514000L, 50000L, "shipyard", R.string.facility_shipyard, 50000000L, listOf(RecipeRequirement("steel", 4), RecipeRequirement("machinery", 2), RecipeRequirement("industrial_container", 2), RecipeRequirement("refined_fuel", 2)), Icons.Rounded.DirectionsBoat, 0xFF4FC3F7),
    BULLET_TRAIN("bullet_train", R.string.product_bullet_train, ProductTier.TIER_4, 450000L, 44000L, "rail_works", R.string.facility_rail_works, 44000000L, listOf(RecipeRequirement("steel", 3), RecipeRequirement("turbine_engine", 1), RecipeRequirement("chip", 2), RecipeRequirement("aluminum_ingot", 2)), Icons.Rounded.Train, 0xFF00E5FF),
    AI_DATACENTER("ai_datacenter", R.string.product_ai_datacenter, ProductTier.TIER_4, 924000L, 70000L, "datacenter_complex", R.string.facility_datacenter_complex, 70000000L, listOf(RecipeRequirement("chip", 4), RecipeRequirement("smartphone", 1), RecipeRequirement("wire", 2), RecipeRequirement("turbine_engine", 1)), Icons.Rounded.Computer, 0xFFE040FB),
    DEFENSE_FRIGATE("defense_frigate", R.string.product_defense_frigate, ProductTier.TIER_4, 814000L, 80000L, "naval_defense_shipyard", R.string.facility_naval_defense_shipyard, 80000000L, listOf(RecipeRequirement("steel", 4), RecipeRequirement("turbine_engine", 2), RecipeRequirement("chip", 2), RecipeRequirement("refined_fuel", 2)), Icons.Rounded.DirectionsBoat, 0xFF37474F),
    HYDROGEN_PLANT("hydrogen_plant", R.string.product_hydrogen_plant, ProductTier.TIER_4, 367000L, 36000L, "hydrogen_refinery", R.string.facility_hydrogen_refinery, 36000000L, listOf(RecipeRequirement("liquefied_gas", 2), RecipeRequirement("biotech_med", 1), RecipeRequirement("solar_panel", 2)), Icons.Rounded.PropaneTank, 0xFF00B0FF),
    SMART_GRID("smart_grid", R.string.product_smart_grid, ProductTier.TIER_4, 961000L, 90000L, "smart_city_hub", R.string.facility_smart_city_hub, 90000000L, listOf(RecipeRequirement("solar_panel", 2), RecipeRequirement("battery", 2), RecipeRequirement("chip", 3), RecipeRequirement("wire", 2)), Icons.Rounded.ElectricBolt, 0xFF76FF03),
    SUPER_YACHT("super_yacht", R.string.product_super_yacht, ProductTier.TIER_4, 770000L, 76000L, "luxury_shipyard", R.string.facility_luxury_shipyard, 76000000L, listOf(RecipeRequirement("steel", 3), RecipeRequirement("furniture", 2), RecipeRequirement("turbine_engine", 1), RecipeRequirement("appliance", 1)), Icons.Rounded.DirectionsBoat, 0xFFFFD700),
    SPACE_ROCKET("space_rocket", R.string.product_space_rocket, ProductTier.TIER_4, 1168000L, 100000L, "aerospace_launch_complex", R.string.facility_aerospace_launch_complex, 100000000L, listOf(RecipeRequirement("chip", 3), RecipeRequirement("turbine_engine", 2), RecipeRequirement("titanium_ingot", 2), RecipeRequirement("refined_fuel", 2)), Icons.Rounded.FlightTakeoff, 0xFF29B6F6),
    SMART_SKYSCRAPER("smart_skyscraper", R.string.product_smart_skyscraper, ProductTier.TIER_4, 1038000L, 100000L, "mega_construction_hub", R.string.facility_mega_construction_hub, 100000000L, listOf(RecipeRequirement("building_block", 3), RecipeRequirement("solar_panel", 2), RecipeRequirement("glass", 2), RecipeRequirement("chip", 2)), Icons.Rounded.Apartment, 0xFF00BCD4),
    FUSION_REACTOR_CORE("fusion_reactor_core", R.string.product_fusion_reactor_core, ProductTier.TIER_4, 1215000L, 120000L, "fusion_energy_complex", R.string.facility_fusion_energy_complex, 120000000L, listOf(RecipeRequirement("titanium_ingot", 3), RecipeRequirement("lithium", 3), RecipeRequirement("wire", 2), RecipeRequirement("chip", 3)), Icons.Rounded.Shield, 0xFFE91E63),
    QUANTUM_SUPERCOMPUTER("quantum_supercomputer", R.string.product_quantum_supercomputer, ProductTier.TIER_4, 1624000L, 140000L, "quantum_tech_complex", R.string.facility_quantum_tech_complex, 140000000L, listOf(RecipeRequirement("chip", 4), RecipeRequirement("pcb_substrate", 3), RecipeRequirement("battery", 2), RecipeRequirement("semiconductor_wafer", 2)), Icons.Rounded.Terminal, 0xFF9C27B0),
    AUTONOMOUS_DRONE_SWARM("autonomous_drone_swarm", R.string.product_autonomous_drone_swarm, ProductTier.TIER_4, 655000L, 56000L, "drone_swarm_factory", R.string.facility_drone_swarm_factory, 56000000L, listOf(RecipeRequirement("telecom_station", 2), RecipeRequirement("robotics_arm", 1), RecipeRequirement("chip", 2), RecipeRequirement("nano_battery", 1)), Icons.Rounded.FlightTakeoff, 0xFF00E5FF),
    HYPERLOOP_CAPSULE("hyperloop_capsule", R.string.product_hyperloop_capsule, ProductTier.TIER_4, 869000L, 78000L, "hyperloop_mega_factory", R.string.facility_hyperloop_mega_factory, 78000000L, listOf(RecipeRequirement("composite_structure", 2), RecipeRequirement("nano_battery", 2), RecipeRequirement("turbine_engine", 1), RecipeRequirement("machinery", 1)), Icons.Rounded.Train, 0xFFFFD700),
    LUXURY_AIRCRAFT_INTERIOR("luxury_aircraft_interior", R.string.product_luxury_aircraft_interior, ProductTier.TIER_4, 569000L, 48000L, "vip_interiors_complex", R.string.facility_vip_interiors_complex, 48000000L, listOf(RecipeRequirement("composite_structure", 2), RecipeRequirement("furniture", 2), RecipeRequirement("appliance", 1), RecipeRequirement("clothing", 1)), Icons.Rounded.AirlineSeatReclineExtra, 0xFFFFB300);

    val displayName: String
        @Composable
        get() = getDisplayName(com.example.ui.theme.isEnglishLanguage())

    val facilityName: String
        @Composable
        get() = getFacilityName(com.example.ui.theme.isEnglishLanguage())

    val baseDurationMs: Long
        get() = (when (tier) {
            ProductTier.TIER_1 -> when (id) {
                "timber", "limestone", "coal" -> 20_000L
                "cotton", "rubber_latex" -> 25_000L
                "iron", "aluminum", "silicon" -> 30_000L
                "copper", "chemicals", "crude_oil", "natural_gas" -> 35_000L
                "lithium", "graphite_ore", "titanium" -> 40_000L
                else -> 25_000L
            }
            ProductTier.TIER_2 -> when (id) {
                "wood_plank", "packaging", "cement", "glass" -> 30_000L
                "fabric", "steel", "wire", "tire", "refined_fuel", "synthetic_textile" -> 50_000L
                "battery", "plastic", "aluminum_ingot", "liquefied_gas", "titanium_ingot", "pcb_substrate" -> 60_000L
                "semiconductor_wafer", "optical_fiber", "graphene_sheet", "titanium_alloy", "carbon_fiber" -> 70_000L
                else -> 50_000L
            }
            ProductTier.TIER_3 -> when (id) {
                "furniture", "building_block", "clothing" -> 60_000L
                "pharma", "appliance", "industrial_container", "petrochem" -> 80_000L
                "chip", "auto_part", "solar_panel" -> 90_000L
                "smartphone", "machinery", "biotech_med", "turbine_engine", "composite_structure", "robotics_arm", "telecom_station", "nano_battery" -> 110_000L
                else -> 80_000L
            }
            ProductTier.TIER_4 -> when (id) {
                "hydrogen_plant", "bullet_train", "ev", "uav", "autonomous_drone_swarm", "luxury_aircraft_interior" -> 150_000L
                "cargo_ship", "super_yacht", "satellite", "ai_datacenter", "hyperloop_capsule" -> 180_000L
                "defense_frigate", "smart_grid", "smart_skyscraper" -> 210_000L
                "space_rocket", "fusion_reactor_core", "quantum_supercomputer" -> 240_000L
                else -> 150_000L
            }
        }) * 5L

    fun calculatePrice(quality: ItemQuality = ItemQuality.STAR_1): Long = (basePrice * quality.priceMultiplier).toLong()
    fun calculatePrice(quality: ProductQuality): Long = (basePrice * quality.priceMultiplier).toLong()

    fun getDisplayName(isEnglish: Boolean = false): String {
        val trName = turkishProductNames[id] ?: id.split("_").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        return if (isEnglish) (com.example.ui.theme.Dictionary[trName] ?: trName) else trName
    }

    fun getDisplayName(context: Context? = null, isEnglish: Boolean = false): String {
        if (isEnglish) return getDisplayName(isEnglish = true)
        val trName = turkishProductNames[id] ?: id.split("_").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        return trName
    }

    fun getFacilityName(isEnglish: Boolean = false): String {
        val trName = turkishFacilityNames[facilityId] ?: facilityId.split("_").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        return if (isEnglish) (com.example.ui.theme.Dictionary[trName] ?: trName) else trName
    }

    fun getFacilityName(context: Context? = null, isEnglish: Boolean = false): String {
        if (isEnglish) return getFacilityName(isEnglish = true)
        val trName = turkishFacilityNames[facilityId] ?: facilityId.split("_").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        return trName
    }

    fun getInitialConstructionDurationMs(): Long = when (tier) {
        ProductTier.TIER_1 -> 10 * 60 * 1000L // 10 minutes
        ProductTier.TIER_2 -> 45 * 60 * 1000L // 45 minutes
        ProductTier.TIER_3 -> 3 * 3600 * 1000L // 3 hours
        ProductTier.TIER_4 -> 8 * 3600 * 1000L // 8 hours
    }

    fun getInitialConstructionDiamondCost(remainingMs: Long): Int {
        val baseGems = when (tier) {
            ProductTier.TIER_1 -> 1
            ProductTier.TIER_2 -> 2
            ProductTier.TIER_3 -> 6
            ProductTier.TIER_4 -> 15
        }
        val hoursLeft = kotlin.math.ceil(remainingMs / 3600_000.0).toInt().coerceAtLeast(1)
        return minOf(baseGems, hoursLeft).coerceAtLeast(1)
    }

    fun getInitialConstructionDurationLabel(isEnglish: Boolean = false): String = when (tier) {
        ProductTier.TIER_1 -> if (isEnglish) "10 Min" else "10 Dk"
        ProductTier.TIER_2 -> if (isEnglish) "45 Min" else "45 Dk"
        ProductTier.TIER_3 -> if (isEnglish) "3 Hours" else "3 Saat"
        ProductTier.TIER_4 -> if (isEnglish) "8 Hours" else "8 Saat"
    }
}

private val turkishProductNames = mapOf(
    "iron" to "Demir Cevheri",
    "copper" to "Bakır",
    "silicon" to "Silisyum",
    "aluminum" to "Alüminyum",
    "coal" to "Kömür",
    "limestone" to "Kireçtaşı",
    "lithium" to "Lityum Cevheri",
    "timber" to "Kereste Odunu",
    "chemicals" to "Kimyasal Hammadde",
    "crude_oil" to "Ham Petrol",
    "natural_gas" to "Doğalgaz",
    "titanium" to "Titanyum Cevheri",
    "graphite_ore" to "Grafit & Nadir Element",
    "rubber_latex" to "Doğal Kauçuk",
    "cotton" to "Endüstriyel Pamuk",
    "steel" to "Çelik",
    "wire" to "Kablo",
    "glass" to "Cam & Optik",
    "fabric" to "İplik & Kumaş",
    "battery" to "Enerji Aküsü",
    "plastic" to "Plastik & Polimer",
    "wood_plank" to "İşlenmiş Kereste",
    "cement" to "Çimento",
    "refined_fuel" to "İşlenmiş Akaryakıt",
    "tire" to "Otomotiv Lastiği",
    "packaging" to "Endüstriyel Ambalaj",
    "aluminum_ingot" to "Külçe Alüminyum",
    "liquefied_gas" to "Sıvılaştırılmış Gaz (LNG)",
    "titanium_ingot" to "Külçe Titanyum",
    "pcb_substrate" to "Devre Kartı Altyapısı (PCB)",
    "synthetic_textile" to "Sentetik Kompozit Elyaf",
    "titanium_alloy" to "Alaşımlı Titanyum Levha",
    "carbon_fiber" to "Karbon Elyaf Kompozit",
    "semiconductor_wafer" to "Yarı İletken Silisyum Wafer",
    "optical_fiber" to "Fiber Optik İletişim Kablosu",
    "graphene_sheet" to "Grafen Nano Tabaka",
    "chip" to "Mikroçip",
    "smartphone" to "Akıllı Telefon & Terminal",
    "clothing" to "Endüstriyel Üniforma & Koruyucu Giyim",
    "auto_part" to "Oto Parçası & Mekaniği",
    "appliance" to "Endüstriyel Cihaz & Beyaz Eşya",
    "furniture" to "Modüler Donanım & Mobilya",
    "building_block" to "Yapı & İnşaat Elemanı",
    "pharma" to "İlaç & Biyo-Kimya",
    "machinery" to "Ağır İş Makinesi",
    "solar_panel" to "Güneş Enerjisi Paneli",
    "petrochem" to "Petrokimya & Madeni Yağ",
    "biotech_med" to "Medikal Cihaz & Biyoteknoloji",
    "turbine_engine" to "Ağır Türbin & Jet Motoru",
    "industrial_container" to "Lojistik Konteyner Sistemi",
    "composite_structure" to "Havacılık Sınıfı Kompozit Panel",
    "robotics_arm" to "Endüstriyel Robot Kolu & Otomasyon",
    "telecom_station" to "5G/6G Baz İstasyonu & Telekom Modülü",
    "nano_battery" to "Nano Grafen Güç Hücresi",
    "ev" to "Elektrikli Otomobil",
    "uav" to "Otonom İHA / SİHA Sistemleri",
    "satellite" to "Uydu & İletişim Modülü",
    "cargo_ship" to "Konteyner Gemisi & Şilep",
    "bullet_train" to "Yüksek Hızlı Tren Seti",
    "ai_datacenter" to "AI Sunucu Kompleksi",
    "defense_frigate" to "Askeri Korvet & Fırkateyn",
    "hydrogen_plant" to "Yeşil Hidrojen Üretim Santrali",
    "smart_grid" to "Akıllı Şehir Enerji Şebekesi",
    "super_yacht" to "Lüks Süper Yat",
    "space_rocket" to "Uzay Roketi & İtki Sistemi",
    "smart_skyscraper" to "Akıllı Gökdelen & Mega Kompleks",
    "fusion_reactor_core" to "Füzyon Reaktör Çekirdeği",
    "quantum_supercomputer" to "Kuantum Süper Bilgisayar",
    "autonomous_drone_swarm" to "Otonom İHA / SİHA Sürüsü",
    "hyperloop_capsule" to "Hyperloop Yolcu Kapsülü",
    "luxury_aircraft_interior" to "VIP Uçak İç Kabin Tasarımı"
)

private val turkishFacilityNames = mapOf(
    "iron_mine" to "Demir Madeni",
    "copper_mine" to "Bakır Madeni",
    "silicon_mine" to "Silisyum Madeni",
    "aluminum_mine" to "Alüminyum Madeni",
    "coal_mine" to "Kömür Madeni",
    "limestone_quarry" to "Kireçtaşı Ocağı",
    "lithium_mine" to "Lityum Madeni",
    "timber_camp" to "Kereste Kampı",
    "chemical_plant" to "Kimya Tesisi",
    "oil_well" to "Petrol Kuyusu",
    "gas_well" to "Doğalgaz Kuyusu",
    "titanium_mine" to "Titanyum Madeni",
    "graphite_mine" to "Grafit Madeni",
    "rubber_farm" to "Kauçuk Üretim Merkezi",
    "cotton_farm" to "Pamuk Tarlası",
    "steel_mill" to "Çelik Haddahanesi",
    "wire_factory" to "Kablo Fabrikası",
    "glassworks" to "Cam & Optik Fabrikası",
    "textile_workshop" to "Tekstil & Dokuma Fabrikası",
    "power_plant" to "Akü & Enerji Fabrikası",
    "plastic_factory" to "Plastik Fabrikası",
    "sawmill" to "Kereste Fabrikası",
    "cement_factory" to "Çimento Fabrikası",
    "petroleum_refinery" to "Rafineri",
    "tire_factory" to "Lastik Fabrikası",
    "paper_mill" to "Ambalaj Fabrikası",
    "aluminum_smelter" to "Alüminyum Dökümhanesi",
    "gas_processing_plant" to "Gaz Tesisleri",
    "titanium_smelter" to "Titanyum Döküm Tesisi",
    "titanium_alloy_plant" to "Titanyum Alaşım Tesisi",
    "pcb_works" to "PCB Üretim Tesisi",
    "synthetic_mill" to "Sentetik Elyaf Fabrikası",
    "carbon_fiber_works" to "Karbon Elyaf Üretim Tesisi",
    "semiconductor_foundry" to "Wafer Üretim Kompleksi",
    "fiber_optics_factory" to "Fiber Optik Fabrikası",
    "graphene_lab" to "Grafen Üretim Kompleksi",
    "chip_factory" to "Çip Fabrikası",
    "smartphone_factory" to "Telefon Fabrikası",
    "clothing_factory" to "Konfeksiyon",
    "auto_factory" to "Oto Yan Sanayi",
    "appliance_factory" to "Beyaz Eşya Fabrikası",
    "furniture_factory" to "Mobilya Fabrikası",
    "construction_factory" to "Yapı Malzemeleri Tesisleri",
    "pharma_lab" to "İlaç Fabrikası",
    "machinery_factory" to "Makine Fabrikası",
    "solar_factory" to "Güneş Paneli Fabrikası",
    "petrochem_complex" to "Petrokimya Kompleksi",
    "medtech_factory" to "Medikal Teknoloji Tesisleri",
    "turbine_works" to "Türbin & Motor Fabrikası",
    "container_factory" to "Konteyner Üretim Tesisleri",
    "composite_plant" to "Aero-Kompozit Üretim Tesisi",
    "robotics_factory" to "Robotik Otomasyon Fabrikası",
    "telecom_factory" to "Telekomünikasyon Sistemleri Fabrikası",
    "nano_battery_factory" to "Nano Pil Gigafactory",
    "ev_factory" to "Elektrikli Araç Gigafactory",
    "aerospace_factory" to "Havacılık & İHA Fabrikası",
    "space_defense_factory" to "Uzay & Uydu Teknolojileri Kompleksi",
    "shipyard" to "Tersane Kompleksi",
    "rail_works" to "Demiryolu Sanayi Kompleksi",
    "datacenter_complex" to "Yapay Zeka & Veri Merkezi Tesisi",
    "naval_defense_shipyard" to "Askeri Tersane Kompleksi",
    "hydrogen_refinery" to "Hidrojen Santrali Kompleksi",
    "smart_city_hub" to "Akıllı Şehir Şebeke Kompleksi",
    "luxury_shipyard" to "Lüks Yat Tersanesi",
    "aerospace_launch_complex" to "Uzay Fırlatma & Roket Kompleksi",
    "mega_construction_hub" to "Mega Yapı İnşaat Kompleksi",
    "fusion_energy_complex" to "Füzyon Enerji Kompleksi",
    "quantum_tech_complex" to "Kuantum Teknoloji Kompleksi",
    "drone_swarm_factory" to "İHA & Otonom Sistemler Fabrikası",
    "hyperloop_mega_factory" to "Hyperloop Mega Fabrikası",
    "vip_interiors_complex" to "VIP Kabin & İklimlendirme Kompleksi"
)

val Product.displayName: String
    @Composable
    get() = getDisplayName(com.example.ui.theme.isEnglishLanguage())

val Product.facilityName: String
    @Composable
    get() = getFacilityName(com.example.ui.theme.isEnglishLanguage())

val Product.facilityDrawableRes: Int?
    get() = when (id) {
        "iron" -> R.drawable.img_iron_mine_1788199347891
        "copper" -> R.drawable.img_copper_mine_1788200197834
        "silicon" -> R.drawable.img_silicon_mine_1788200470035
        "aluminum" -> R.drawable.img_aluminum_mine_1788200704714
        "coal" -> R.drawable.img_coal_mine_1788201118008
        "limestone" -> R.drawable.img_limestone_quarry_1788201355203
        "lithium" -> R.drawable.img_lithium_mine_1788201607570
        "timber" -> R.drawable.img_timber_camp_1788201862699
        "chemicals" -> R.drawable.img_chemical_plant_1788202090648
        "crude_oil" -> R.drawable.oil_well_1788202324127
        "natural_gas" -> R.drawable.img_natural_gas_well_1788202561570
        "titanium" -> R.drawable.img_titanium_mine_1788202787934
        "graphite_ore" -> R.drawable.img_graphite_mine_1788203017901
        "rubber_latex" -> R.drawable.img_rubber_farm_1788203242108
        "cotton" -> R.drawable.cotton_farm_1788203518981
        "steel" -> R.drawable.img_steel_mill_1788203735983
        "wire" -> R.drawable.img_wire_factory_1788203959368
        "glass" -> R.drawable.img_glassworks_1788204228951
        "fabric" -> R.drawable.textile_workshop_1788204456738
        "battery" -> R.drawable.img_power_plant_1788204698261
        "plastic" -> R.drawable.img_plastic_factory_1788204935093
        "wood_plank" -> R.drawable.sawmill_1788205241094
        "cement" -> R.drawable.img_cement_factory_1788205506295
        "refined_fuel" -> R.drawable.petroleum_refinery
        "tire" -> R.drawable.img_tire_factory_1788206068939
        "packaging" -> R.drawable.img_paper_mill_1788206300000_1788206289680
        "aluminum_ingot" -> R.drawable.img_aluminum_smelter_1788206505357
        "liquefied_gas" -> R.drawable.img_gas_processing_plant_1788206707874
        "titanium_ingot" -> R.drawable.img_titanium_smelter_1788206923892
        "titanium_alloy" -> R.drawable.img_titanium_alloy_plant_1788207151086
        "pcb_substrate" -> R.drawable.img_pcb_works_1788251219161
        "synthetic_textile" -> R.drawable.img_synthetic_mill_1788207560022
        "carbon_fiber" -> R.drawable.img_carbon_fiber_works_1788207800000
        "semiconductor_wafer" -> R.drawable.img_semiconductor_foundry_1788208100000
        "optical_fiber" -> R.drawable.img_fiber_optics_factory_1788251205829
        "graphene_sheet" -> R.drawable.img_graphene_lab_1788208500000
        "chip" -> R.drawable.img_chip_factory_1788209000000
        "smartphone" -> R.drawable.img_smartphone_factory_1788209000000
        "clothing" -> R.drawable.img_clothing_factory_1788209000000
        "auto_part" -> R.drawable.img_auto_factory_1788249034849
        "appliance" -> R.drawable.img_appliance_factory_1788249053397
        "furniture" -> R.drawable.img_furniture_factory_1788249069401
        "building_block" -> R.drawable.img_construction_factory_1788249081654
        "pharma" -> R.drawable.img_pharma_lab_1788249682750
        "machinery" -> R.drawable.img_machinery_factory_1788249709977
        "solar_panel" -> R.drawable.img_solar_factory_1788249737358
        "petrochem" -> R.drawable.img_petrochem_complex_1788250100909
        "biotech_med" -> R.drawable.img_medtech_factory_1788250120948
        "turbine_engine" -> R.drawable.img_turbine_works_1788250135274
        "industrial_container" -> R.drawable.img_container_factory_1788250149503
        "composite_structure" -> R.drawable.img_composite_plant_1788250036871
        "robotics_arm" -> R.drawable.img_robotics_factory_1788250051280
        "telecom_station" -> R.drawable.img_telecom_factory_1788250064632
        "nano_battery" -> R.drawable.img_nano_battery_factory_1788250078174
        "ev" -> R.drawable.ev_factory_1788253026864
        "uav" -> R.drawable.img_drone_swarm_factory
        "satellite" -> R.drawable.space_defense_factory
        "cargo_ship" -> R.drawable.shipyard
        "bullet_train" -> R.drawable.img_hyperloop_mega_factory
        "ai_datacenter" -> R.drawable.img_quantum_tech_complex
        "defense_frigate" -> R.drawable.naval_defense_shipyard_1788253101893
        "hydrogen_plant" -> R.drawable.hydrogen_refinery_1788253113250
        "smart_grid" -> R.drawable.smart_city_hub
        "super_yacht" -> R.drawable.img_vip_interiors_complex
        "space_rocket" -> R.drawable.aerospace_launch_complex_1788253163511
        "smart_skyscraper" -> R.drawable.mega_construction_hub_1788253174080
        "fusion_reactor_core" -> R.drawable.fusion_energy_complex_1788253184990
        "quantum_supercomputer" -> R.drawable.img_quantum_tech_complex_v2_1788257750120
        "autonomous_drone_swarm" -> R.drawable.drone_factory_v3_1788258155505
        "hyperloop_capsule" -> R.drawable.img_hyperloop_mega_factory
        "luxury_aircraft_interior" -> R.drawable.img_vip_interiors_complex_v2_1788257771036
        else -> null
    }

val TechNode.name: String
    @Composable
    get() = stringResource(nameRes)

val TechNode.subtitle: String
    @Composable
    get() = if (subtitleRes != 0) stringResource(subtitleRes) else ""

val TechNode.tierLabel: String
    @Composable
    get() = if (tierLabelRes != 0) stringResource(tierLabelRes) else ""

object SupplyChainEngine {
    /**
     * Recursively computes all raw (Tier 1) material requirements for a given product id and amount.
     */
    fun calculateRawMaterialRequirements(productId: String, amount: Long = 1L): Map<String, Long> {
        val product = Product.values().find { it.id == productId } ?: return emptyMap()
        if (product.tier == ProductTier.TIER_1) {
            return mapOf(productId to amount)
        }
        val rawMap = mutableMapOf<String, Long>()
        for (req in product.recipe) {
            val childReqs = calculateRawMaterialRequirements(req.productId, req.amountPerUnit.toLong() * amount)
            for ((matId, matQty) in childReqs) {
                rawMap[matId] = (rawMap[matId] ?: 0L) + matQty
            }
        }
        return rawMap
    }

    /**
     * Checks if a recipe strictly satisfies the tier hierarchy rule:
     * Tier 2 requires Tier 1 inputs.
     * Tier 3 requires Tier 1 or 2 inputs.
     * Tier 4 requires Tier 1, 2, or 3 inputs.
     */
    fun validateRecipeTierRule(product: Product): Boolean {
        if (product.tier == ProductTier.TIER_1) return product.recipe.isEmpty()
        val maxAllowedTier = product.tier.ordinal - 1 // T2 -> T1 (ordinal 0), T3 -> T2 max (ordinal 1), T4 -> T3 max (ordinal 2)
        return product.recipe.all { req ->
            val inputProduct = Product.values().find { it.id == req.productId }
            inputProduct != null && inputProduct.tier.ordinal <= maxAllowedTier
        }
    }
}

object CityFacilityRegistry {
    // Map of Product ID to List of Allowed City IDs based on geological, regional, and industrial profile
    val productAllowedCities: Map<String, List<String>> = mapOf(
        // --- TIER 1: HAM MADDELER & DOĞAL KAYNAKLAR ---
        "iron" to listOf("sivas", "elazig", "zonguldak", "essen", "johannesburg"),
        "copper" to listOf("artvin", "elazig", "santiago", "johannesburg"),
        "silicon" to listOf("mugla", "izmir", "new_york", "santiago", "tokyo"),
        "aluminum" to listOf("konya", "eskisehir", "santiago", "essen"),
        "coal" to listOf("zonguldak", "sivas", "essen", "kirikkale"),
        "limestone" to listOf("sivas", "artvin", "mugla", "elazig", "mersin", "kahramanmaras"),
        "lithium" to listOf("santiago", "konya", "johannesburg"),
        "timber" to listOf("kahramanmaras", "artvin", "mugla", "bursa", "kuala_lumpur"),
        "chemicals" to listOf("kirikkale", "gaziantep", "kocaeli", "new_york", "mersin", "rotterdam", "sao_paulo", "basra"),
        "crude_oil" to listOf("batman", "basra", "houston", "mersin", "rotterdam", "kocaeli"),
        "natural_gas" to listOf("batman", "basra", "houston", "new_york", "rotterdam", "mersin"),
        "titanium" to listOf("elazig", "eskisehir", "johannesburg", "houston"),
        "graphite_ore" to listOf("johannesburg", "new_york", "eskisehir", "shanghai"),
        "rubber_latex" to listOf("kuala_lumpur", "sao_paulo", "kahramanmaras"),
        "cotton" to listOf("kahramanmaras", "gaziantep", "mersin", "izmir"),

        // --- TIER 2: İŞLENMİŞ ARA MALLAR ---
        "steel" to listOf("istanbul", "essen", "zonguldak", "sivas", "london", "rotterdam", "kocaeli", "bursa"),
        "wire" to listOf("bursa", "kocaeli", "frankfurt", "london", "izmir", "artvin"),
        "glass" to listOf("bursa", "mugla", "izmir", "london", "istanbul", "mersin"),
        "fabric" to listOf("gaziantep", "bursa", "kahramanmaras", "istanbul", "izmir"),
        "battery" to listOf("shanghai", "santiago", "kocaeli", "tokyo", "new_york", "london", "konya", "ankara"),
        "plastic" to listOf("kocaeli", "sao_paulo", "gaziantep", "mersin", "london", "rotterdam", "istanbul", "batman"),
        "wood_plank" to listOf("bursa", "kahramanmaras", "artvin", "mugla", "kuala_lumpur"),
        "cement" to listOf("sivas", "istanbul", "essen", "mersin", "elazig", "zonguldak"),
        "refined_fuel" to listOf("kocaeli", "batman", "sao_paulo", "basra", "mersin", "rotterdam", "houston", "kirikkale"),
        "tire" to listOf("bursa", "kocaeli", "kuala_lumpur", "london", "frankfurt"),
        "packaging" to listOf("gaziantep", "bursa", "kocaeli", "new_york", "mersin", "istanbul", "rotterdam"),
        "aluminum_ingot" to listOf("konya", "essen", "istanbul", "london", "rotterdam", "eskisehir"),
        "liquefied_gas" to listOf("basra", "batman", "houston", "mersin", "rotterdam", "kocaeli"),
        "titanium_ingot" to listOf("istanbul", "eskisehir", "essen", "london", "elazig", "johannesburg"),
        "pcb_substrate" to listOf("izmir", "bursa", "shanghai", "tokyo", "new_york", "ankara", "london"),
        "synthetic_textile" to listOf("kocaeli", "gaziantep", "sao_paulo", "new_york", "bursa", "kahramanmaras"),
        "titanium_alloy" to listOf("istanbul", "eskisehir", "essen", "london", "ankara", "houston"),
        "carbon_fiber" to listOf("ankara", "kocaeli", "frankfurt", "london", "eskisehir", "tokyo"),
        "semiconductor_wafer" to listOf("izmir", "shanghai", "tokyo", "new_york", "london", "ankara"),
        "optical_fiber" to listOf("izmir", "ankara", "frankfurt", "new_york", "london", "tokyo"),
        "graphene_sheet" to listOf("johannesburg", "shanghai", "ankara", "eskisehir", "new_york"),

        // --- TIER 3: NİHAİ ÜRÜNLER & YÜKSEK TEKNOLOJİ ---
        "chip" to listOf("izmir", "shanghai", "tokyo", "ankara", "new_york", "london"),
        "smartphone" to listOf("izmir", "istanbul", "tokyo", "shanghai", "new_york", "london", "ankara"),
        "clothing" to listOf("gaziantep", "bursa", "istanbul", "london", "kahramanmaras", "izmir"),
        "auto_part" to listOf("bursa", "frankfurt", "tokyo", "kocaeli", "london", "eskisehir"),
        "appliance" to listOf("istanbul", "bursa", "eskisehir", "essen", "london", "kocaeli"),
        "furniture" to listOf("istanbul", "bursa", "ankara", "london", "gaziantep"),
        "building_block" to listOf("istanbul", "sivas", "mersin", "london", "new_york", "essen", "ankara"),
        "pharma" to listOf("istanbul", "kocaeli", "frankfurt", "london", "new_york", "izmir", "kirikkale"),
        "machinery" to listOf("london", "frankfurt", "eskisehir", "bursa", "essen", "ankara", "kocaeli"),
        "solar_panel" to listOf("konya", "shanghai", "ankara", "rotterdam", "mersin", "izmir"),
        "petrochem" to listOf("kocaeli", "basra", "sao_paulo", "mersin", "rotterdam", "batman", "kirikkale"),
        "biotech_med" to listOf("istanbul", "izmir", "tokyo", "frankfurt", "new_york", "london"),
        "turbine_engine" to listOf("eskisehir", "rotterdam", "london", "ankara", "frankfurt"),
        "industrial_container" to listOf("mersin", "rotterdam", "shanghai", "kocaeli", "istanbul"),
        "composite_structure" to listOf("ankara", "eskisehir", "london", "rotterdam", "tokyo", "houston"),
        "robotics_arm" to listOf("tokyo", "frankfurt", "shanghai", "izmir", "london", "ankara"),
        "telecom_station" to listOf("ankara", "izmir", "tokyo", "new_york", "london", "istanbul"),
        "nano_battery" to listOf("shanghai", "tokyo", "ankara", "london", "konya"),

        // --- TIER 4: MEGA PROJELER & STRATEJİK SANAYİ ---
        "ev" to listOf("istanbul", "bursa", "shanghai", "frankfurt", "london", "kocaeli"),
        "uav" to listOf("ankara", "eskisehir", "tokyo", "istanbul"),
        "satellite" to listOf("ankara", "istanbul", "houston", "tokyo", "new_york", "london"),
        "cargo_ship" to listOf("mersin", "rotterdam", "shanghai", "london", "istanbul"),
        "bullet_train" to listOf("london", "eskisehir", "tokyo", "ankara", "frankfurt"),
        "ai_datacenter" to listOf("istanbul", "tokyo", "london", "houston", "new_york", "ankara"),
        "defense_frigate" to listOf("mersin", "rotterdam", "istanbul", "ankara"),
        "hydrogen_plant" to listOf("rotterdam", "houston", "mersin", "kocaeli", "basra"),
        "smart_grid" to listOf("istanbul", "tokyo", "london", "rotterdam", "new_york", "ankara"),
        "super_yacht" to listOf("mersin", "rotterdam", "istanbul", "london"),
        "space_rocket" to listOf("houston", "ankara", "tokyo", "new_york"),
        "smart_skyscraper" to listOf("istanbul", "london", "shanghai", "tokyo", "new_york"),
        "fusion_reactor_core" to listOf("houston", "tokyo", "london", "ankara"),
        "quantum_supercomputer" to listOf("istanbul", "tokyo", "london", "houston", "new_york"),
        "autonomous_drone_swarm" to listOf("ankara", "tokyo", "shanghai", "eskisehir"),
        "hyperloop_capsule" to listOf("london", "ankara", "houston", "tokyo", "new_york"),
        "luxury_aircraft_interior" to listOf("london", "eskisehir", "frankfurt", "istanbul", "new_york")
    )
}

fun Product.getAllowedCityIds(): List<String> {
    return CityFacilityRegistry.productAllowedCities[id] ?: emptyList()
}

fun Product.canBeBuiltIn(cityId: String): Boolean {
    val allowed = getAllowedCityIds()
    return allowed.isEmpty() || allowed.contains(cityId.lowercase().trim())
}

