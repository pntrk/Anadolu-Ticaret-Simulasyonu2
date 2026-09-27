// =============================================================================
// ConsortiumProduction Model for Dart / Flutter
// Serial batch manufacturing line tracking with quality_level (1-5)
// =============================================================================

import 'item_quality.dart';
import 'consortium_quality_tier.dart';

class ConsortiumProduction {
  final String id;
  final String projectId;
  final String projectName;
  final String targetProductId;
  final String targetProductName;
  final int qualityLevel;
  final ItemQuality quality;
  final String targetQualityGrade;
  final ConsortiumQualityTier qualityTier;
  final int batchIndex;
  final int producedQuantity;
  final int unitBatchPrice;
  final double craftsmanshipMultiplier;
  final double averageInputQuality;
  final bool isCompleted;
  final int startedAtMs;
  final int? completedAtMs;

  ConsortiumProduction({
    required this.id,
    required this.projectId,
    this.projectName = '',
    required this.targetProductId,
    this.targetProductName = '',
    int? qualityLevel,
    ItemQuality? quality,
    this.targetQualityGrade = 'C',
    ConsortiumQualityTier? qualityTier,
    this.batchIndex = 1,
    this.producedQuantity = 0,
    this.unitBatchPrice = 0,
    this.craftsmanshipMultiplier = 1.0,
    this.averageInputQuality = 1.0,
    this.isCompleted = false,
    int? startedAtMs,
    this.completedAtMs,
  })  : quality = quality ?? (qualityLevel != null ? ItemQuality.fromStars(qualityLevel) : ItemQuality.star1),
        qualityLevel = qualityLevel ?? (quality?.stars ?? 1),
        qualityTier = qualityTier ?? ConsortiumQualityTier.fromString(targetQualityGrade),
        startedAtMs = startedAtMs ?? DateTime.now().millisecondsSinceEpoch;

  int get totalBatchValue => producedQuantity * unitBatchPrice;

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'project_id': projectId,
      'project_name': projectName,
      'target_product_id': targetProductId,
      'target_product_name': targetProductName,
      'quality_level': qualityLevel,
      'quality_tier': quality.name,
      'target_quality_grade': targetQualityGrade,
      'batch_index': batchIndex,
      'produced_quantity': producedQuantity,
      'unit_batch_price': unitBatchPrice,
      'total_batch_value': totalBatchValue,
      'craftsmanship_multiplier': craftsmanshipMultiplier,
      'average_input_quality': averageInputQuality,
      'is_completed': isCompleted,
      'started_at_ms': startedAtMs,
      if (completedAtMs != null) 'completed_at_ms': completedAtMs,
    };
  }

  factory ConsortiumProduction.fromJson(Map<String, dynamic> json) {
    final rawQuality = json['quality_level'] ?? json['quality'];
    final qLvl = (rawQuality as num?)?.toInt() ?? 1;
    final quality = ItemQuality.fromStars(qLvl);
    final targetGrade = json['target_quality_grade'] as String? ?? 'C';

    return ConsortiumProduction(
      id: json['id'] as String? ?? '',
      projectId: json['project_id'] as String? ?? json['projectId'] as String? ?? '',
      projectName: json['project_name'] as String? ?? json['projectName'] as String? ?? '',
      targetProductId: json['target_product_id'] as String? ?? json['targetProductId'] as String? ?? '',
      targetProductName: json['target_product_name'] as String? ?? json['targetProductName'] as String? ?? '',
      qualityLevel: qLvl,
      quality: quality,
      targetQualityGrade: targetGrade,
      qualityTier: ConsortiumQualityTier.fromString(targetGrade),
      batchIndex: (json['batch_index'] as num?)?.toInt() ?? 1,
      producedQuantity: (json['produced_quantity'] as num?)?.toInt() ?? 0,
      unitBatchPrice: (json['unit_batch_price'] as num?)?.toInt() ?? 0,
      craftsmanshipMultiplier: (json['craftsmanship_multiplier'] as num?)?.toDouble() ?? 1.0,
      averageInputQuality: (json['average_input_quality'] as num?)?.toDouble() ?? 1.0,
      isCompleted: json['is_completed'] as bool? ?? false,
      startedAtMs: (json['started_at_ms'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      completedAtMs: (json['completed_at_ms'] as num?)?.toInt(),
    );
  }
}
