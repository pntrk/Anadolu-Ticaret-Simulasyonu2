// =============================================================================
// BuyOrder Model for Dart / Flutter
// Procurement orders with minimum quality_level requirement
// =============================================================================

import 'item_quality.dart';

class BuyOrder {
  final String id;
  final String buyerId;
  final String buyerName;
  final String itemId;
  final int qualityLevel;
  final int minQualityLevel;
  final ItemQuality quality;
  final int quantity;
  final int pricePerUnit;
  final String destinationCityId;
  final DateTime createdAt;

  BuyOrder({
    required this.id,
    required this.buyerId,
    required this.buyerName,
    required this.itemId,
    int? qualityLevel,
    int? minQualityLevel,
    ItemQuality? quality,
    required this.quantity,
    required this.pricePerUnit,
    required this.destinationCityId,
    DateTime? createdAt,
  })  : quality = quality ?? (qualityLevel != null ? ItemQuality.fromStars(qualityLevel) : ItemQuality.extractQuality(itemId)),
        qualityLevel = qualityLevel ?? (quality?.stars ?? 1),
        minQualityLevel = minQualityLevel ?? qualityLevel ?? 1,
        createdAt = createdAt ?? DateTime.now();

  int get totalBudget => quantity * pricePerUnit;

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'buyer_id': buyerId,
      'buyer_name': buyerName,
      'item_id': itemId,
      'quality_level': qualityLevel,
      'min_quality_level': minQualityLevel,
      'quality_tier': quality.name,
      'quantity': quantity,
      'price_per_unit': pricePerUnit,
      'total_budget': totalBudget,
      'destination_city_id': destinationCityId,
      'created_at': createdAt.toIso8601String(),
    };
  }

  factory BuyOrder.fromJson(Map<String, dynamic> json) {
    final rawItemId = json['item_id'] as String? ?? '';
    final qLvl = (json['quality_level'] as num?)?.toInt() ?? 1;
    final minQLvl = (json['min_quality_level'] as num?)?.toInt() ?? qLvl;

    DateTime parsedDate;
    try {
      parsedDate = json['created_at'] != null
          ? DateTime.parse(json['created_at'].toString())
          : DateTime.now();
    } catch (_) {
      parsedDate = DateTime.now();
    }

    return BuyOrder(
      id: json['id'] as String? ?? '',
      buyerId: json['buyer_id'] as String? ?? '',
      buyerName: json['buyer_name'] as String? ?? '',
      itemId: rawItemId,
      qualityLevel: qLvl,
      minQualityLevel: minQLvl,
      quality: ItemQuality.fromStars(qLvl),
      quantity: (json['quantity'] as num?)?.toInt() ?? 0,
      pricePerUnit: (json['price_per_unit'] as num?)?.toInt() ?? 0,
      destinationCityId: json['destination_city_id'] as String? ?? json['city'] as String? ?? 'istanbul',
      createdAt: parsedDate,
    );
  }
}
