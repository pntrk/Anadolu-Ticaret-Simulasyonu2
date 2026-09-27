// =============================================================================
// PlayerSaveData Model for Dart / Flutter
// Full Supabase Cloud Save & Backup Schema with quality_level persistence
// =============================================================================

import 'item_quality.dart';
import 'inventory_item.dart';

class BusinessSaveData {
  final int id;
  final String type;
  final int level;
  final String cityId;
  final double wearLevel;
  final int storageCapacity;

  BusinessSaveData({
    required this.id,
    required this.type,
    required this.level,
    required this.cityId,
    this.wearLevel = 0.0,
    this.storageCapacity = 500,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'type': type,
    'level': level,
    'cityId': cityId,
    'wearLevel': wearLevel,
    'storageCapacity': storageCapacity,
  };

  factory BusinessSaveData.fromJson(Map<String, dynamic> json) => BusinessSaveData(
    id: (json['id'] as num?)?.toInt() ?? 0,
    type: json['type'] as String? ?? '',
    level: (json['level'] as num?)?.toInt() ?? 1,
    cityId: json['cityId'] as String? ?? json['city_id'] as String? ?? 'istanbul',
    wearLevel: (json['wearLevel'] as num?)?.toDouble() ?? 0.0,
    storageCapacity: (json['storageCapacity'] as num?)?.toInt() ?? 500,
  );
}

class PlayerSaveData {
  final String id;
  final String name;
  final String companyName;
  final int money;
  final int loanAmount;
  final int depositBalance;
  final int dailyIncome;
  final int dailyExpense;
  final int totalProfit;
  final int xp;
  final int level;
  final int inventoryCapacity;
  final String currentCity;
  final bool isVip;
  final int gems;
  final List<BusinessSaveData> businesses;
  final List<InventoryItem> inventory;
  final int lastSavedTime;

  PlayerSaveData({
    required this.id,
    this.name = 'Tüccar',
    this.companyName = 'Tüccar Holding',
    this.money = 100000,
    this.loanAmount = 0,
    this.depositBalance = 0,
    this.dailyIncome = 0,
    this.dailyExpense = 0,
    this.totalProfit = 0,
    this.xp = 0,
    this.level = 1,
    this.inventoryCapacity = 5000,
    this.currentCity = 'istanbul',
    this.isVip = false,
    this.gems = 0,
    this.businesses = const [],
    this.inventory = const [],
    int? lastSavedTime,
  }) : lastSavedTime = lastSavedTime ?? DateTime.now().millisecondsSinceEpoch;

  /// Oyuncunun envanterindeki ürünlerin kalite seviyelerine göre toplam tahmini piyasa değeri
  int calculateTotalInventoryValue(Map<String, int> productBasePrices) {
    int total = 0;
    for (final item in inventory) {
      final basePrice = productBasePrices[item.baseProductId] ?? 1000;
      total += item.getTotalCalculatedValue(basePrice);
    }
    return total;
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'company_name': companyName,
      'money': money,
      'loan_amount': loanAmount,
      'deposit_balance': depositBalance,
      'daily_income': dailyIncome,
      'daily_expense': dailyExpense,
      'total_profit': totalProfit,
      'xp': xp,
      'level': level,
      'inventory_capacity': inventoryCapacity,
      'current_city': currentCity,
      'is_vip': isVip,
      'gems': gems,
      'businesses': businesses.map((b) => b.toJson()).toList(),
      'inventory': inventory.map((i) => i.toJson()).toList(),
      'last_saved_time': lastSavedTime,
    };
  }

  factory PlayerSaveData.fromJson(Map<String, dynamic> json) {
    final rawInventory = json['inventory'] as List? ?? [];
    final parsedInventory = rawInventory.map((item) {
      if (item is Map<String, dynamic>) {
        return InventoryItem.fromJson(item);
      }
      return InventoryItem(itemId: item.toString(), quantity: 1);
    }).toList();

    final rawBiz = json['businesses'] as List? ?? [];
    final parsedBiz = rawBiz.map((b) {
      if (b is Map<String, dynamic>) {
        return BusinessSaveData.fromJson(b);
      }
      return BusinessSaveData(id: 0, type: 'unknown', level: 1, cityId: 'istanbul');
    }).toList();

    return PlayerSaveData(
      id: json['id'] as String? ?? 'local_player',
      name: json['name'] as String? ?? 'Tüccar',
      companyName: json['company_name'] as String? ?? 'Tüccar Holding',
      money: (json['money'] as num?)?.toInt() ?? 100000,
      loanAmount: (json['loan_amount'] as num?)?.toInt() ?? 0,
      depositBalance: (json['deposit_balance'] as num?)?.toInt() ?? 0,
      dailyIncome: (json['daily_income'] as num?)?.toInt() ?? 0,
      dailyExpense: (json['daily_expense'] as num?)?.toInt() ?? 0,
      totalProfit: (json['total_profit'] as num?)?.toInt() ?? 0,
      xp: (json['xp'] as num?)?.toInt() ?? 0,
      level: (json['level'] as num?)?.toInt() ?? 1,
      inventoryCapacity: (json['inventory_capacity'] as num?)?.toInt() ?? 5000,
      currentCity: json['current_city'] as String? ?? 'istanbul',
      isVip: json['is_vip'] as bool? ?? false,
      gems: (json['gems'] as num?)?.toInt() ?? 0,
      businesses: parsedBiz,
      inventory: parsedInventory,
      lastSavedTime: (json['last_saved_time'] as num?)?.toInt(),
    );
  }
}
