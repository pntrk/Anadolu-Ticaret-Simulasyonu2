package com.example.data

/**
 * Tedarik zinciri ağacındaki tek bir ürün/tesis düğümü.
 *
 * @param product Temsil edilen ürün (GameRegistry.Product)
 * @param tier Ürünün kademesi (TIER_1'den TIER_4'e)
 * @param ownedBusiness Oyuncunun bu ürünü üreten kurulu bir tesisi varsa nesne, yoksa null
 * @param stockInWarehouse Merkez depodaki toplam stok miktarı (tüm kalite kademeleri dahil)
 * @param isBottleneck Hammadde eksikliği yüzünden üretim durdu mu? (Girdi malzemeleri depoda yetersizse true)
 * @param childNodes Bu ürünü üretmek için gerekli olan alt girdi düğümleri (Product.recipe hiyerarşisi)
 */
data class SupplyChainNode(
    val product: Product,
    val tier: ProductTier,
    val ownedBusiness: BusinessEntity?,
    val stockInWarehouse: Int,
    val isBottleneck: Boolean,
    val childNodes: List<SupplyChainNode>
) {
    /**
     * Tüm alt ağaçtaki darboğaz (bottleneck) düğümlerini liste olarak döner.
     */
    fun getAllBottlenecks(): List<SupplyChainNode> {
        val list = mutableListOf<SupplyChainNode>()
        if (isBottleneck) {
            list.add(this)
        }
        for (child in childNodes) {
            list.addAll(child.getAllBottlenecks())
        }
        return list
    }

    /**
     * Bu düğüm ve altındaki tüm girdiler için oyuncunun kurulu tesisi bulunup bulunmadığını kontrol eder.
     */
    fun isFullChainOwned(): Boolean {
        if (ownedBusiness == null) return false
        return childNodes.all { it.isFullChainOwned() }
    }

    /**
     * Ağacı düz bir liste haline getirir (DFS).
     */
    fun flatten(): List<SupplyChainNode> {
        val result = mutableListOf(this)
        for (child in childNodes) {
            result.addAll(child.flatten())
        }
        return result
    }
}

/**
 * Tedarik zinciri akış ve hiyerarşi eşleştirme motoru.
 * Product.recipe bağımlılıklarını özyinelemeli (recursive) tarayarak
 * Tier 4'ten Tier 1'e kadar tüm hiyerarşiyi çıkarır ve oyuncunun
 * kurulu tesisleriyle (`facilityId`) birebir eşleştirir.
 */
object SupplyChainFlowEngine {

    /**
     * Belirtilen hedef ürün ID'si için tam tedarik zinciri ağacını oluşturur.
     *
     * @param targetProductId Ürün ID'si (ör. "quantum_supercomputer", "steel", "electric_vehicle")
     * @param ownedBusinesses Oyuncunun sahip olduğu mevcut tesisler
     * @param inventory Oyuncunun merkez deposundaki envanter listesi
     * @return Hiyerarşik SupplyChainNode ağacı
     */
    fun buildTreeForProduct(
        targetProductId: String,
        ownedBusinesses: List<BusinessEntity>,
        inventory: List<InventoryEntity>
    ): SupplyChainNode {
        val product = Product.values().find {
            it.id.equals(targetProductId, ignoreCase = true) ||
            it.facilityId.equals(targetProductId, ignoreCase = true) ||
            it.name.equals(targetProductId, ignoreCase = true)
        } ?: Product.values().firstOrNull { it.id == "iron" } ?: Product.values().first()

        return buildNodeRecursive(product, ownedBusinesses, inventory, visited = emptySet())
    }

    private fun buildNodeRecursive(
        product: Product,
        ownedBusinesses: List<BusinessEntity>,
        inventory: List<InventoryEntity>,
        visited: Set<String>
    ): SupplyChainNode {
        // 1. Oyuncunun bu ürün için kurulu tesisi var mı? (facilityId veya product.id ile eşleşen)
        val matchingBusiness = ownedBusinesses
            .filter { it.type == product.facilityId || it.type == product.id }
            .maxByOrNull { it.level }

        // 2. Merkez depodaki toplam stok miktarı (ana ürün ID'si veya kalite varyantları ile)
        val currentStock = inventory
            .filter { it.baseProductId.equals(product.id, ignoreCase = true) || it.itemId.equals(product.id, ignoreCase = true) }
            .sumOf { it.quantity }

        // 3. Alt girdiler (Product.recipe) - Özyinelemeli hiyerarşi
        val childNodes = mutableListOf<SupplyChainNode>()
        var hasDirectMissingRawMaterials = false

        if (product.recipe.isNotEmpty()) {
            for (req in product.recipe) {
                // Depodaki girdi stoğunu kontrol et
                val reqStock = inventory
                    .filter { it.baseProductId.equals(req.productId, ignoreCase = true) || it.itemId.equals(req.productId, ignoreCase = true) }
                    .sumOf { it.quantity }

                // 1 birim üretim için gereken miktar karşılanamıyorsa hammadde eksikliği var
                if (reqStock < req.amountPerUnit) {
                    hasDirectMissingRawMaterials = true
                }

                // Döngüsel bağımlılığı (circular dependency) önlemek için visited kontrolü
                if (!visited.contains(req.productId)) {
                    val childProduct = Product.values().find { it.id.equals(req.productId, ignoreCase = true) }
                    if (childProduct != null) {
                        val childNode = buildNodeRecursive(
                            product = childProduct,
                            ownedBusinesses = ownedBusinesses,
                            inventory = inventory,
                            visited = visited + product.id
                        )
                        childNodes.add(childNode)
                    }
                }
            }
        }

        // 4. isBottleneck kontrolü:
        // Hammadde eksikliği yüzünden üretim durdu mu?
        // - Tier 1 ürünlerin reçetesi boştur (doğrudan ham madde), bu yüzden hammadde eksikliği darboğazı oluşturmaz.
        // - Reçeteli ürünlerde (Tier 2-4) doğrudan girdi eksikse veya alt girdi zincirinde depoda hiç stok yokken üretim durduysa darboğaz oluşur.
        val isBottleneck = if (product.recipe.isEmpty()) {
            false
        } else {
            hasDirectMissingRawMaterials || childNodes.any { it.isBottleneck && it.stockInWarehouse == 0 }
        }

        return SupplyChainNode(
            product = product,
            tier = product.tier,
            ownedBusiness = matchingBusiness,
            stockInWarehouse = currentStock,
            isBottleneck = isBottleneck,
            childNodes = childNodes
        )
    }
}
