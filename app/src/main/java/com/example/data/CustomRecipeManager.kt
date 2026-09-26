package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.ui.graphics.vector.ImageVector

data class CustomRecipe(
    val id: String,
    val name: String,
    val facilityName: String,
    val tier: ProductTier,
    val recipeRequirements: List<RecipeRequirement>,
    val productionCost: Long,
    val basePrice: Long,
    val facilityCost: Long,
    val colorHex: Long = 0xFF00E5FF
)

data class RecipeValidationResult(
    val isValid: Boolean,
    val message: String
)

object CustomRecipeManager {
    private val customRecipesList = mutableListOf<CustomRecipe>()

    fun getCustomRecipes(): List<CustomRecipe> = customRecipesList.toList()

    fun addCustomRecipe(recipe: CustomRecipe): Boolean {
        if (customRecipesList.any { it.id == recipe.id || it.name.equals(recipe.name, ignoreCase = true) }) {
            return false
        }
        customRecipesList.add(recipe)
        return true
    }

    /**
     * Checks if a custom recipe strictly satisfies the tier hierarchy rule:
     * Tier 2 requires Tier 1 inputs.
     * Tier 3 requires Tier 1 or 2 inputs.
     * Tier 4 requires Tier 1, 2, or 3 inputs.
     */
    fun validateRecipe(tier: ProductTier, requirements: List<RecipeRequirement>): RecipeValidationResult {
        if (requirements.isEmpty()) {
            return RecipeValidationResult(false, "Sıfır girdi seçilemez. En az 1 bileşen hammadde eklemelisiniz.")
        }
        if (requirements.size > 4) {
            return RecipeValidationResult(false, "Maksimum 4 farklı hammadde bileşeni seçebilirsiniz.")
        }

        val maxAllowedOrdinal = tier.ordinal - 1

        for (req in requirements) {
            val inputProduct = Product.values().find { it.id == req.productId }
            val inputCustom = customRecipesList.find { it.id == req.productId }

            val inputTierOrdinal = inputProduct?.tier?.ordinal ?: inputCustom?.tier?.ordinal

            if (inputTierOrdinal == null) {
                return RecipeValidationResult(false, "Seçilen bileşen '${req.productId}' kayıtlarda bulunamadı.")
            }

            if (inputTierOrdinal > maxAllowedOrdinal) {
                val inputTierName = ProductTier.values()[inputTierOrdinal].name
                val currentTierName = tier.name
                return RecipeValidationResult(
                    false,
                    "Hiyerarşi İhlali: $currentTierName kademesi $inputTierName ürünü kullanamaz! Yalnızca Tier <= ${maxAllowedOrdinal + 1} bileşen seçiniz."
                )
            }
        }

        return RecipeValidationResult(true, "✅ Reçete Hiyerarşi Kurallarına Tam Uyumlu! ($tier)")
    }
}
