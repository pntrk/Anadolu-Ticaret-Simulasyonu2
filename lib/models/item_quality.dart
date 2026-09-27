// =============================================================================
// ItemQuality Enum & Helper Model for Dart / Flutter
// Supports 1-5 Star Craftsmanship, Price Multipliers, Badge Colors, and Keys
// =============================================================================

enum ItemQuality {
  star1(1, 1.00, '⭐ Standart', 0xFFB08D57, 'Bronz'),
  star2(2, 1.35, '⭐⭐ Seçme', 0xFFC0C0C0, 'Gümüş'),
  star3(3, 1.90, '⭐⭐⭐ Usta İşi', 0xFFFFD700, 'Altın'),
  star4(4, 2.80, '⭐⭐⭐⭐ Seçkin', 0xFFE5E4E2, 'Platin'),
  star5(5, 4.50, '⭐⭐⭐⭐⭐ Kusursuz', 0xFF00E676, 'Parlak Zümrüt');

  final int stars;
  final double priceMultiplier;
  final String label;
  final int badgeColorValue;
  final String tierName;

  const ItemQuality(
    this.stars,
    this.priceMultiplier,
    this.label,
    this.badgeColorValue,
    this.tierName,
  );

  /// Kalite seviyesi tam sayısı (1 - 5)
  int get qualityLevel => stars;

  /// Yıldız metni (★, ★★, ★★★, vb.)
  String get starsText => '★' * stars;

  /// JSON serialization için int değere çevirir
  int toJson() => stars;

  /// Integer değerden ItemQuality üretir (1..5, varsayılan: star1)
  static ItemQuality fromStars(int? stars) {
    if (stars == null) return ItemQuality.star1;
    switch (stars) {
      case 1:
        return ItemQuality.star1;
      case 2:
        return ItemQuality.star2;
      case 3:
        return ItemQuality.star3;
      case 4:
        return ItemQuality.star4;
      case 5:
        return ItemQuality.star5;
      default:
        return stars > 5 ? ItemQuality.star5 : ItemQuality.star1;
    }
  }

  /// Supabase / JSON `quality_level` alanından üretir
  static ItemQuality fromJson(dynamic json) {
    if (json is int) return fromStars(json);
    if (json is String) {
      final parsed = int.tryParse(json);
      if (parsed != null) return fromStars(parsed);
      return fromString(json);
    }
    return ItemQuality.star1;
  }

  /// String enum / anahtardan çözümleme
  static ItemQuality fromString(String? key) {
    if (key == null) return ItemQuality.star1;
    final clean = key.toLowerCase().trim();
    if (clean.contains('star5') || clean.contains('kusursuz') || clean.endsWith('_5')) {
      return ItemQuality.star5;
    }
    if (clean.contains('star4') || clean.contains('seçkin') || clean.contains('seckin') || clean.endsWith('_4')) {
      return ItemQuality.star4;
    }
    if (clean.contains('star3') || clean.contains('usta') || clean.endsWith('_3')) {
      return ItemQuality.star3;
    }
    if (clean.contains('star2') || clean.contains('seçme') || clean.contains('secme') || clean.endsWith('_2')) {
      return ItemQuality.star2;
    }
    return ItemQuality.star1;
  }

  /// Envanterde aynı ürünün farklı kalitelerini ayırmak için benzersiz kimlik: 'productId_quality'
  /// Örnek: 'iron_star1', 'steel_star5', 'olive_star3'
  static String makeKey(String productId, [ItemQuality quality = ItemQuality.star1]) {
    final baseId = extractBaseProductId(productId);
    return '${baseId}_${quality.name}';
  }

  /// 'steel_star5' -> 'steel'
  static String extractBaseProductId(String key) {
    if (key.contains('_star')) {
      return key.substring(0, key.lastIndexOf('_star'));
    }
    return key;
  }

  /// 'steel_star5' -> ItemQuality.star5
  static ItemQuality extractQuality(String key) {
    return fromString(key);
  }
}
