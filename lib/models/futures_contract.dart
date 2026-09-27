// =============================================================================
// FuturesContract Model for Dart / Flutter
// Locked commodities contracts with quality_level
// =============================================================================

import 'item_quality.dart';

class FuturesContract {
  final String id;
  final String creatorId;
  final String creatorName;
  final String itemId;
  final int qualityLevel;
  final ItemQuality quality;
  final int quantity;
  final int lockedPricePerUnit;
  final int durationDays;
  final String cityId;
  final bool isFulfilled;
  final DateTime createdAt;

  FuturesContract({
    required this.id,
    required this.creatorId,
    required this.creatorName,
    required this.itemId,
    int? qualityLevel,
    ItemQuality? quality,
    required this.quantity,
    required this.lockedPricePerUnit,
    this.durationDays = 30,
    this.cityId = 'istanbul',
    this.isFulfilled = false,
    DateTime? createdAt,
  })  : quality = quality ?? (qualityLevel != null ? ItemQuality.fromStars(qualityLevel) : ItemQuality.extractQuality(itemId)),
        qualityLevel = qualityLevel ?? (quality?.stars ?? 1),
        createdAt = createdAt ?? DateTime.now();

  int get totalLockedValue => quantity * lockedPricePerUnit;

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'creator_id': creatorId,
      'creator_name': creatorName,
      'item_id': itemId,
      'quality_level': qualityLevel,
      'quality_tier': quality.name,
      'quantity': quantity,
      'price_per_unit': lockedPricePerUnit,
      'total_locked_value': totalLockedValue,
      'duration_days': durationDays,
      'city': cityId,
      'is_fulfilled': isFulfilled,
      'created_at': createdAt.toIso8601String(),
    };
  }

  factory FuturesContract.fromJson(Map<String, dynamic> json) {
    final rawItemId = json['item_id'] as String? ?? '';
    final qLvl = (json['quality_level'] as num?)?.toInt() ?? 1;

    DateTime parsedDate;
    try {
      parsedDate = json['created_at'] != null
          ? DateTime.parse(json['created_at'].toString())
          : DateTime.now();
    } catch (_) {
      parsedDate = DateTime.now();
    }

    return FuturesContract(
      id: json['id'] as String? ?? '',
      creatorId: json['creator_id'] as String? ?? json['seller_id'] as String? ?? '',
      creatorName: json['creator_name'] as String? ?? json['seller_name'] as String? ?? '',
      itemId: rawItemId,
      qualityLevel: qLvl,
      quality: ItemQuality.fromStars(qLvl),
      quantity: (json['quantity'] as num?)?.toInt() ?? 0,
      lockedPricePerUnit: (json['price_per_unit'] as num?)?.toInt() ?? 0,
      durationDays: (json['duration_days'] as num?)?.toInt() ?? 30,
      cityId: json['city'] as String? ?? json['city_id'] as String? ?? 'istanbul',
      isFulfilled: json['is_fulfilled'] as bool? ?? false,
      createdAt: parsedDate,
    );
  }
}
