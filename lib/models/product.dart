// =============================================================================
// Product Model for Dart / Flutter with ItemQuality & calculatedPrice
// =============================================================================

import 'item_quality.dart';

class Product {
  final String id;
  final String name;
  final int basePrice;
  final ItemQuality quality;
  final String category;
  final String? facilityId;

  const Product({
    required this.id,
    required this.name,
    required this.basePrice,
    this.quality = ItemQuality.star1,
    this.category = 'general',
    this.facilityId,
  });

  /// 1-5 arası tam sayı kalite seviyesi
  int get qualityLevel => quality.stars;

  /// Dinamik birim fiyat: (taban fiyat * quality.priceMultiplier)
  int get calculatedPrice => (basePrice * quality.priceMultiplier).round();

  /// Envanter benzersiz anahtarı ('productId_quality')
  String get uniqueInventoryKey => ItemQuality.makeKey(id, quality);

  Product copyWith({
    String? id,
    String? name,
    int? basePrice,
    ItemQuality? quality,
    String? category,
    String? facilityId,
  }) {
    return Product(
      id: id ?? this.id,
      name: name ?? this.name,
      basePrice: basePrice ?? this.basePrice,
      quality: quality ?? this.quality,
      category: category ?? this.category,
      facilityId: facilityId ?? this.facilityId,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'base_price': basePrice,
      'quality_level': quality.stars,
      'quality_tier': quality.name,
      'calculated_price': calculatedPrice,
      'category': category,
      if (facilityId != null) 'facility_id': facilityId,
    };
  }

  factory Product.fromJson(Map<String, dynamic> json) {
    final rawQuality = json['quality_level'] ?? json['quality'];
    return Product(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      basePrice: (json['base_price'] as num?)?.toInt() ?? 0,
      quality: ItemQuality.fromJson(rawQuality),
      category: json['category'] as String? ?? 'general',
      facilityId: json['facility_id'] as String?,
    );
  }
}
