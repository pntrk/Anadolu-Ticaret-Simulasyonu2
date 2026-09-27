// =============================================================================
// ConsortiumSlot Model for Dart / Flutter
// Consortium / Mega Project supply slot tracking craftsmanship and quality
// =============================================================================

import 'item_quality.dart';

class ConsortiumSlot {
  final String slotId;
  final String productId;
  final int requiredQuantity;
  final int quantityDelivered;
  final int qualityLevel;
  final ItemQuality deliveredQuality;
  final double deliveredQualityScore;
  final int costContributionValue;

  ConsortiumSlot({
    required this.slotId,
    required this.productId,
    required this.requiredQuantity,
    this.quantityDelivered = 0,
    int? qualityLevel,
    ItemQuality? deliveredQuality,
    this.deliveredQualityScore = 1.0,
    this.costContributionValue = 0,
  })  : deliveredQuality = deliveredQuality ?? (qualityLevel != null ? ItemQuality.fromStars(qualityLevel) : ItemQuality.star1),
        qualityLevel = qualityLevel ?? (deliveredQuality?.stars ?? 1);

  bool get isCompleted => quantityDelivered >= requiredQuantity;
  double get progressFraction => requiredQuantity > 0 ? (quantityDelivered / requiredQuantity).clamp(0.0, 1.0) : 1.0;

  Map<String, dynamic> toJson() {
    return {
      'slot_id': slotId,
      'product_id': productId,
      'required_quantity': requiredQuantity,
      'quantity_delivered': quantityDelivered,
      'quality_level': qualityLevel,
      'quality_tier': deliveredQuality.name,
      'delivered_quality_score': deliveredQualityScore,
      'cost_contribution_value': costContributionValue,
    };
  }

  factory ConsortiumSlot.fromJson(Map<String, dynamic> json) {
    final qLvl = (json['quality_level'] as num?)?.toInt() ?? 1;
    return ConsortiumSlot(
      slotId: json['slot_id'] as String? ?? json['slotId'] as String? ?? '',
      productId: json['product_id'] as String? ?? json['productId'] as String? ?? '',
      requiredQuantity: (json['required_quantity'] as num?)?.toInt() ?? (json['requiredQuantity'] as num?)?.toInt() ?? 0,
      quantityDelivered: (json['quantity_delivered'] as num?)?.toInt() ?? (json['quantityDelivered'] as num?)?.toInt() ?? 0,
      qualityLevel: qLvl,
      deliveredQuality: ItemQuality.fromStars(qLvl),
      deliveredQualityScore: (json['delivered_quality_score'] as num?)?.toDouble() ?? 1.0,
      costContributionValue: (json['cost_contribution_value'] as num?)?.toInt() ?? 0,
    );
  }
}
