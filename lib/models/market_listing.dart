// =============================================================================
// MarketListing Model for Dart / Flutter
// Peer-to-peer marketplace listing with quality_level (1-5) support
// =============================================================================

import 'item_quality.dart';

class MarketListing {
  final String id;
  final String sellerId;
  final String sellerName;
  final String itemId;
  final int qualityLevel;
  final ItemQuality quality;
  final int quantity;
  final int pricePerUnit;
  final String originCityId;
  final DateTime createdAt;

  MarketListing({
    required this.id,
    required this.sellerId,
    required this.sellerName,
    required this.itemId,
    int? qualityLevel,
    ItemQuality? quality,
    required this.quantity,
    required this.pricePerUnit,
    required this.originCityId,
    DateTime? createdAt,
  })  : quality = quality ?? (qualityLevel != null ? ItemQuality.fromStars(qualityLevel) : ItemQuality.extractQuality(itemId)),
        qualityLevel = qualityLevel ?? (quality?.stars ?? ItemQuality.extractQuality(itemId).stars),
        createdAt = createdAt ?? DateTime.now();

  int get totalPrice => quantity * pricePerUnit;

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'seller_id': sellerId,
      'seller_name': sellerName,
      'item_id': itemId,
      'quality_level': qualityLevel,
      'quality_tier': quality.name,
      'quantity': quantity,
      'price_per_unit': pricePerUnit,
      'total_price': totalPrice,
      'city': originCityId,
      'created_at': createdAt.toIso8601String(),
    };
  }

  factory MarketListing.fromJson(Map<String, dynamic> json) {
    final rawItemId = json['item_id'] as String? ?? '';
    final rawQuality = json['quality_level'] ?? json['quality'];
    final quality = ItemQuality.fromJson(rawQuality != null ? rawQuality : ItemQuality.extractQuality(rawItemId).stars);

    DateTime parsedDate;
    try {
      parsedDate = json['created_at'] != null
          ? DateTime.parse(json['created_at'].toString())
          : DateTime.now();
    } catch (_) {
      parsedDate = DateTime.now();
    }

    return MarketListing(
      id: json['id'] as String? ?? '',
      sellerId: json['seller_id'] as String? ?? '',
      sellerName: json['seller_name'] as String? ?? '',
      itemId: rawItemId,
      qualityLevel: quality.stars,
      quality: quality,
      quantity: (json['quantity'] as num?)?.toInt() ?? 0,
      pricePerUnit: (json['price_per_unit'] as num?)?.toInt() ?? 0,
      originCityId: json['city'] as String? ?? json['origin_city_id'] as String? ?? 'istanbul',
      createdAt: parsedDate,
    );
  }
}
