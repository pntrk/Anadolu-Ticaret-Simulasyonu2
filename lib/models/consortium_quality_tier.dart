// =============================================================================
// ConsortiumQualityTier Enum for Dart / Flutter
// Defines Consortium Quality Standard (A, B, C) and Input Acceptance Rules:
// - A Grade -> Only 4★ (Superior) and 5★ (Flawless) inputs accepted [4, 5]
// - B Grade -> Only 2★ (Select), 3★ (Masterwork), and 4★ (Superior) inputs accepted [2, 3, 4]
// - C Grade -> Only 1★ (Standard) and 2★ (Select) inputs accepted [1, 2]
// =============================================================================

import 'item_quality.dart';

enum ConsortiumQualityTier {
  gradeC(
    'C Kalite (Standart)',
    'C Grade (Standard)',
    1.0,
    1.0,
    1.0,
    0xFF94A3B8,
    'Temel sanayi standardı. Yalnızca 1★ (Standart) ve 2★ (Seçme) kalitedeki hammaddeler kabul edilir.',
    'C',
    [1, 2],
    '1★ ve 2★ Girdi Kabul',
  ),
  gradeB(
    'B Kalite (Gelişmiş & Usta İşi)',
    'B Grade (Advanced & Masterwork)',
    2.0,
    2.3,
    1.3,
    0xFF38BDF8,
    'Gelişmiş mühendislik standardı. Yalnızca 2★ (Seçme), 3★ (Usta İşi) ve 4★ (Seçkin) kalitedeki hammaddeler kabul edilir.',
    'B',
    [2, 3, 4],
    '2★, 3★ ve 4★ Girdi Kabul',
  ),
  gradeA(
    'A Kalite (Premium & Kusursuz)',
    'A Grade (Premium & Flawless)',
    4.0,
    5.0,
    1.8,
    0xFFFFD700,
    'En üst düzey savunma & uzay standardı. Yalnızca 4★ (Seçkin) ve 5★ (Kusursuz) kalitedeki hammaddeler kabul edilir! 5 kat Borsa değeri!',
    'A',
    [4, 5],
    '4★ ve 5★ Girdi Kabul',
  );

  final String titleTr;
  final String titleEn;
  final double requirementMultiplier;
  final double borsaValueMultiplier;
  final double durationMultiplier;
  final int badgeColorValue;
  final String descriptionTr;
  final String gradeCode;
  final List<int> allowedStars;
  final String allowedQualityRangeText;

  const ConsortiumQualityTier(
    this.titleTr,
    this.titleEn,
    this.requirementMultiplier,
    this.borsaValueMultiplier,
    this.durationMultiplier,
    this.badgeColorValue,
    this.descriptionTr,
    this.gradeCode,
    this.allowedStars,
    this.allowedQualityRangeText,
  );

  /// Kontrol: Bu kalite seviyesindeki hammadde konsorsiyuma teslim edilebilir mi?
  bool isQualityAllowed(ItemQuality quality) => allowedStars.contains(quality.stars);

  bool isStarsAllowed(int stars) => allowedStars.contains(stars);

  String toJson() => name;

  static ConsortiumQualityTier fromString(String? key) {
    if (key == null) return ConsortiumQualityTier.gradeC;
    final clean = key.toUpperCase().trim();
    if (clean == 'A' || clean == 'GRADE_A' || clean == 'GRADEA' || clean.contains('A KALITE')) {
      return ConsortiumQualityTier.gradeA;
    }
    if (clean == 'B' || clean == 'GRADE_B' || clean == 'GRADEB' || clean.contains('B KALITE')) {
      return ConsortiumQualityTier.gradeB;
    }
    return ConsortiumQualityTier.gradeC;
  }
}
