// =============================================================================
// Transaction / MarketTransaction Model for Dart / Flutter
// Supports quality_level (1-5), dynamic pricing, and JSON serialization
// =============================================================================

import 'item_quality.dart';

enum TransactionType {
  marketBuy,
  marketSell,
  futuresSettle,
  consortiumSupply,
  directTrade,
}

class Transaction {
  final String id;
  final TransactionType transactionType;
  final String? buyerId;
  final String? buyerName;
  final String? sellerId;
  final String? sellerName;
  final String itemId;
  final String itemName;
  final int qualityLevel;
  final ItemQuality quality;
  final int quantity;
  final int unitPrice;
  final int totalAmount;
  final String cityId;
  final int synergyBonusPercent;
  final String status;
  final DateTime timestamp;

  Transaction({
    required this.id,
    required this.transactionType,
    this.buyerId,
    this.buyerName,
    this.sellerId,
    this.sellerName,
    required this.itemId,
    this.itemName = '',
    int? qualityLevel,
    ItemQuality? quality,
    required this.quantity,
    required this.unitPrice,
    int? totalAmount,
    this.cityId = 'istanbul',
    this.synergyBonusPercent = 0,
    this.status = 'SETTLED',
    DateTime? timestamp,
  })  : quality = quality ?? (qualityLevel != null ? ItemQuality.fromStars(qualityLevel) : ItemQuality.extractQuality(itemId)),
        qualityLevel = qualityLevel ?? (quality?.stars ?? ItemQuality.extractQuality(itemId).stars),
        totalAmount = totalAmount ?? (quantity * unitPrice),
        timestamp = timestamp ?? DateTime.now();

  /// Dinamik birim hesaplanan fiyat kontrolü
  int get calculatedUnitPrice => (unitPrice * quality.priceMultiplier).round();

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'transaction_type': transactionType.name,
      'buyer_id': buyerId,
      'buyer_name': buyerName,
      'seller_id': sellerId,
      'seller_name': sellerName,
      'item_id': itemId,
      'item_name': itemName,
      'quality_level': qualityLevel,
      'quality_tier': quality.name,
      'quantity': quantity,
      'unit_price': unitPrice,
      'total_amount': totalAmount,
      'city_id': cityId,
      'synergy_bonus_percent': synergyBonusPercent,
      'status': status,
      'created_at': timestamp.toIso8601String(),
    };
  }

  factory Transaction.fromJson(Map<String, dynamic> json) {
    final rawItemId = json['item_id'] as String? ?? json['itemId'] as String? ?? '';
    final rawQuality = json['quality_level'] ?? json['quality'];
    final quality = ItemQuality.fromJson(rawQuality != null ? rawQuality : ItemQuality.extractQuality(rawItemId).stars);

    final typeStr = json['transaction_type'] as String? ?? json['transactionType'] as String? ?? 'marketBuy';
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

    return Transaction(
      id: json['id'] as String? ?? '',
      transactionType: type,
      buyerId: json['buyer_id'] as String? ?? json['buyerId'] as String?,
      buyerName: json['buyer_name'] as String? ?? json['buyerName'] as String?,
      sellerId: json['seller_id'] as String? ?? json['sellerId'] as String?,
      sellerName: json['seller_name'] as String? ?? json['sellerName'] as String?,
      itemId: rawItemId,
      itemName: json['item_name'] as String? ?? json['itemName'] as String? ?? '',
      qualityLevel: quality.stars,
      quality: quality,
      quantity: (json['quantity'] as num?)?.toInt() ?? 0,
      unitPrice: (json['unit_price'] as num?)?.toInt() ?? (json['unitPrice'] as num?)?.toInt() ?? 0,
      totalAmount: (json['total_amount'] as num?)?.toInt() ?? (json['totalAmount'] as num?)?.toInt(),
      cityId: json['city_id'] as String? ?? json['cityId'] as String? ?? 'istanbul',
      synergyBonusPercent: (json['synergy_bonus_percent'] as num?)?.toInt() ?? 0,
      status: json['status'] as String? ?? 'SETTLED',
      timestamp: parsedDate,
    );
  }
}

/// Type alias for MarketTransaction to guarantee seamless compatibility
typedef MarketTransaction = Transaction;
