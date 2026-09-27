// =============================================================================
// InventoryItem Model for Dart / Flutter
// Stores items uniquely by 'productId_quality' format to prevent merging
// =============================================================================

import 'item_quality.dart';

class InventoryItem {
  final String itemId; // Format: 'wheat_star3' or 'iron'
  final int quantity;
  final ItemQuality quality;

  InventoryItem({
    required this.itemId,
    required this.quantity,
    ItemQuality? quality,
  }) : quality = quality ?? ItemQuality.extractQuality(itemId);

  /// Ham ürün kimliği (örn: 'iron')
  String get baseProductId => ItemQuality.extractBaseProductId(itemId);

  /// 1-5 arası tam sayı kalite seviyesi
  int get qualityLevel => quality.stars;

  /// Envanterdeki benzersiz anahtar ('productId_quality')
  String get uniqueKey => ItemQuality.makeKey(baseProductId, quality);

  /// Birim hesaplanan fiyat
  int getCalculatedPrice(int basePrice) {
    return (basePrice * quality.priceMultiplier).round();
  }

  /// Toplam stok değeri
  int getTotalCalculatedValue(int basePrice) {
    return getCalculatedPrice(basePrice) * quantity;
  }

  InventoryItem copyWith({
    String? itemId,
    int? quantity,
    ItemQuality? quality,
  }) {
    return InventoryItem(
      itemId: itemId ?? this.itemId,
      quantity: quantity ?? this.quantity,
      quality: quality ?? this.quality,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'itemId': uniqueKey,
      'base_product_id': baseProductId,
      'quality_level': quality.stars,
      'quality_tier': quality.name,
      'quantity': quantity,
    };
  }

  factory InventoryItem.fromJson(Map<String, dynamic> json) {
    final rawKey = json['itemId'] as String? ?? json['item_id'] as String? ?? '';
    final rawQuality = json['quality_level'] ?? json['quality'];
    final parsedQuality = rawQuality != null
        ? ItemQuality.fromJson(rawQuality)
        : ItemQuality.extractQuality(rawKey);

    return InventoryItem(
      itemId: rawKey.isNotEmpty ? rawKey : ItemQuality.makeKey('item', parsedQuality),
      quantity: (json['quantity'] as num?)?.toInt() ?? 0,
      quality: parsedQuality,
    );
  }
}
