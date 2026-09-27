// =============================================================================
// TradeTransaction Model for Dart / Flutter
// Complete transaction audit logging with quality_level
// =============================================================================

import 'item_quality.dart';

enum TransactionType {
  marketBuy,
  marketSell,
  futuresSettle,
  consortiumSupply,
  directTrade,
}

class TradeTransaction {
  final String id;
  final TransactionType transactionType;
  final String? buyerId;
  final String? buyerName;
  final String? sellerId;
  final String? sellerName;
  final String itemId;
  final int qualityLevel;
  final ItemQuality quality;
  final int quantity;
  final int unitPrice;
  final int totalAmount;
  final String cityId;
  final int synergyBonusPercent;
  final DateTime timestamp;

  TradeTransaction({
    required this.id,
    required this.transactionType,
    this.buyerId,
    this.buyerName,
    this.sellerId,
    this.sellerName,
    required this.itemId,
    int? qualityLevel,
    ItemQuality? quality,
    required this.quantity,
    required this.unitPrice,
    int? totalAmount,
    this.cityId = 'istanbul',
    this.synergyBonusPercent = 0,
    DateTime? timestamp,
  })  : quality = quality ?? (qualityLevel != null ? ItemQuality.fromStars(qualityLevel) : ItemQuality.extractQuality(itemId)),
        qualityLevel = qualityLevel ?? (quality?.stars ?? 1),
        totalAmount = totalAmount ?? (quantity * unitPrice),
        timestamp = timestamp ?? DateTime.now();

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'transaction_type': transactionType.name,
      'buyer_id': buyerId,
      'buyer_name': buyerName,
      'seller_id': sellerId,
      'seller_name': sellerName,
      'item_id': itemId,
      'quality_level': qualityLevel,
      'quality_tier': quality.name,
      'quantity': quantity,
      'unit_price': unitPrice,
      'total_amount': totalAmount,
      'city_id': cityId,
      'synergy_bonus_percent': synergyBonusPercent,
      'created_at': timestamp.toIso8601String(),
    };
  }

  factory TradeTransaction.fromJson(Map<String, dynamic> json) {
    final rawItemId = json['item_id'] as String? ?? '';
    final qLvl = (json['quality_level'] as num?)?.toInt() ?? 1;

    final typeStr = json['transaction_type'] as String? ?? 'marketBuy';
    final type = TransactionType.values.firstWhere(
      (t) => t.name.toLowerCase() == typeStr.toLowerCase(),
      orElse: () => TransactionType.marketBuy,
    );

    DateTime parsedDate;
    try {
      parsedDate = json['created_at'] != null
          ? DateTime.parse(json['created_at'].toString())
          : DateTime.now();
    } catch (_) {
      parsedDate = DateTime.now();
    }

    return TradeTransaction(
      id: json['id'] as String? ?? '',
      transactionType: type,
      buyerId: json['buyer_id'] as String?,
      buyerName: json['buyer_name'] as String?,
      sellerId: json['seller_id'] as String?,
      sellerName: json['seller_name'] as String?,
      itemId: rawItemId,
      qualityLevel: qLvl,
      quality: ItemQuality.fromStars(qLvl),
      quantity: (json['quantity'] as num?)?.toInt() ?? 0,
      unitPrice: (json['unit_price'] as num?)?.toInt() ?? 0,
      totalAmount: (json['total_amount'] as num?)?.toInt(),
      cityId: json['city_id'] as String? ?? 'istanbul',
      synergyBonusPercent: (json['synergy_bonus_percent'] as num?)?.toInt() ?? 0,
      timestamp: parsedDate,
    );
  }
}
