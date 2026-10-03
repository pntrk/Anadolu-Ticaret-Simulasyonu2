# Anadolu Ekonomi Bülteni: Gerçekçi Şehir-Tesis Eşleşmesi & Hızlı Üretim Arayüzü

Bu plan, **Anadolu Ekonomi Bülteni**'ndeki kâr fırsatı ve görevlerin oyun veritabanındaki (`CityFacilityRegistry.productAllowedCities`) kurallarla %100 tutarlı olmasını, bülten fırsat havuzunun hem Türkiye hem de küresel ticaret merkezlerini kapsamasını, bülten arayüzüne **şehir/bölge filtresi** ve **tek tıkla doğrudan üretim emri verme** yeteneklerinin kazandırılmasını sağlar.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Kullanıcı ile yapılan netleştirme görüşmesinde alınan kararlar:
> - **Kapsam**: Bülten fırsat havuzu sadece Türkiye değil, dünya geneli ticaret şehirlerindeki (Rotterdam, Essen, Houston, Santiago, Tokyo vb.) izinli tesis ve madenleri de kapsayacak.
> - **Arayüz & Kolaylık**: Bültende şehir/bölge filtresi sunulacak ve oyuncunun sahip olduğu tesisler için bültenden çıkmadan veya tek dokunuşla üretim emri verebileceği hızlı aksiyon butonu eklenecektir.
> - **Veritabanı Tutarlılığı**: Rize gibi tesis kurulamayan şehirlerde fırsat çıkmayacak; Artvin'de kereste veya bakır, Zonguldak'ta kömür veya çelik gibi `CityFacilityRegistry` kuralları kesin olarak uygulanacaktır.

---

## 1. Mimari ve Eşleşme Matrisi (Database-Grounded Opportunities)

Oyun veritabanındaki `CityFacilityRegistry` tablosuna tam uyumlu fırsat havuzu kurgulanır:

```
┌────────────────────────────────────────────────────────────────────────┐
│               DOĞRULANMIŞ ŞEHİR - TESİS EŞLEŞME MOTORU                 │
├────────────────────────────────────────────────────────────────────────┤
│  🌲 Kereste (timber)        ➔ Artvin, K.Maraş, Muğla, Bursa, K.Lumpur │
│  ⛏️ Bakır (copper)          ➔ Artvin, Elazığ, Santiago, Johannesburg  │
│  🧵 Pamuk (cotton)          ➔ Kahramanmaraş, Gaziantep, Mersin, İzmir │
│  ⛏️ Taş Kömürü (coal)       ➔ Zonguldak, Sivas, Essen, Kırıkkale      │
│  🛢️ Ham Petrol (crude_oil)  ➔ Batman, Mersin, Kocaeli, Basra, Houston │
│  🏗️ Demir (iron)            ➔ Sivas, Elazığ, Zonguldak, Essen         │
│  ⚙️ Alüminyum (aluminum)    ➔ Konya, Eskişehir, Santiago, Essen       │
│  ⚡ Silisyum (silicon)      ➔ Muğla, İzmir, New York, Santiago, Tokyo │
│  🧶 Kumaş (fabric)          ➔ Gaziantep, Bursa, K.Maraş, İstanbul     │
│  🔩 Çelik (steel)           ➔ Zonguldak, Sivas, Kocaeli, Essen        │
│  ⛽ Rafine Yakıt (fuel)     ➔ Kocaeli, Batman, Mersin, Rotterdam      │
│  ☀️ Güneş Paneli (solar)    ➔ Konya, Ankara, Rotterdam, Shanghai      │
└────────────────────────────────────────────────────────────────────────┘
```

Herhangi bir görev oluşturulurken veya dinamik üretilirken `product.canBeBuiltIn(cityId)` doğrulaması zorunlu kılınarak veritabanı dışı şehirlerin seçilmesi imkansız hale getirilir.

---

## 2. Kullanıcı Deneyimi ve Arayüz Optimizasyonu

```
┌────────────────────────────────────────────────────────────────────────┐
│  📢 ANADOLU EKONOMİ BÜLTENİ & BÖLGESEL FIRSATLAR             [ ✕ ]     │
│  Bölgesel Kâr Fırsatları, Makro Trendler & Görevler                    │
├────────────────────────────────────────────────────────────────────────┤
│  [ 🎯 CANLI FIRSATLAR (3) ]  [ 🏛️ MAKRO & FAİZ ]  [ 📰 ŞEHİR & PİYASA ]│
├────────────────────────────────────────────────────────────────────────┤
│  🔍 Bölge Filtresi: [ Tümü (3) ] [ 🇹🇷 Türkiye ] [ 🌍 Küresel ] [ Artvin ]│
├────────────────────────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │ 🌲 ARTVİN: Karadeniz Dağları Kereste İhracatı Seferberliği        │ │
│ │ 📍 Artvin  │  🌲 Kereste Kampı  │  💰 +%40 Prim  │  💎 8 ELMAS    │ │
│ │ ────────────────────────────────────────────────────────────────── │ │
│ │ 📊 Görev: Artvin'de 60 Ton Kereste Üret ve Sat                     │ │
│ │ [██████████████░░░░░░░░░░] %60 (Üretim: 60/60 ✓ | Satış: 15/60)   │ │
│ │ ────────────────────────────────────────────────────────────────── │ │
│ │ [ ⚡ Artvin'de Hızlı Üret (1 Tık) ]   [ 🛒 Pazarda / Borsada Sat ] │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │ ⛏️ ESSEN: Ağır Sanayi Taş Kömürü İkmal Çağrısı                    │ │
│ │ 📍 Essen (Almanya)  │  ⛏️ Kömür Madeni  │  💰 +%35 Prim │ 💎 7 ELMAS│ │
│ │ ────────────────────────────────────────────────────────────────── │ │
│ │ 📊 Görev: Essen'de Kömür Madeni Kur & 80 Ton Üret/Sat              │ │
│ │ [░░░░░░░░░░░░░░░░░░░░░░░░] %0 (Tesis Henüz Kurulmadı)              │ │
│ │ ────────────────────────────────────────────────────────────────── │ │
│ │ [ 🏗️ Essen'de Kömür Madeni Kur (Önseçili Yönlendir) ]              │ │
│ └────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Yenilikler & Arayüz Geliştirmeleri:
1. **Şehir & Bölge Filtre Çubuğu (Chips)**:
   - `Tümü`, `🇹🇷 Türkiye`, `🌍 Küresel` ve aktif görev şehirlerine göre anında filtreleme.
2. **Tek Tıkla Üretim Emri (Direct Quick-Produce)**:
   - Oyuncunun ilgili şehirde tesisi varsa, bültenden çıkmadan veya tek dokunuşla tesiste mevcut hammaddeyle tam parti üretim başlatabilmesi (veya tesis üretim panelini anında açması).
3. **Akıllı Kurulum Butonu**:
   - Tesisi yoksa, doğrudan o şehrin seçili olduğu `Tesis Kur` penceresine yönlendirme.
4. **Kompakt & Okunaklı Kart Yerleşimi**:
   - Fazla boşluklar optimize edilmiş, metinler netleşmiş, ilerleme çubukları ve onay rozetleri (✓) ile kusursuz bilgi hiyerarşisi.

---

## 3. Teknik Değişiklikler ve Planlanan Adımlar

1. **`BulletinOpportunityManager.kt`**:
   - `OPPORTUNITY_TEMPLATES` havuzunu `CityFacilityRegistry.productAllowedCities` haritasıyla birebir eşleşecek şekilde genişletme:
     - Artvin Kereste & Bakır, Kahramanmaraş Pamuk & Kereste, Zonguldak Kömür & Çelik, Batman Petrol, Sivas Demir & Çimento, Konya Alüminyum & Güneş Paneli, Muğla Silisyum, Essen Kömür & Çelik, Rotterdam Petrol & Kimya vb.
   - `validateCityFacilityMatching()` fonksiyonu ekleyerek her fırsatın `product.canBeBuiltIn(cityId)` kuralını sağladığını garanti altına alma.
   - Tamamlanan görevin yerine gelecek yeni fırsatın rastgele seçilirken geçerli şehirlerden türetilmesi.

2. **`CityNewsBulletinDialog.kt`**:
   - Yatay kaydırılabilir `FilterChip` bölge/şehir filtresi ekleme.
   - Tesisi olan görevler için **"⚡ [Şehir] Tesisinde Üretim Başlat"** hızlı aksiyonu ekleme.
   - Kartların padding ve tipografi optimizasyonu, ülke bayrakları (`🇹🇷`, `🇩🇪`, `🇳🇱` vb.) ile zenginleştirilmesi.

3. **`GameViewModel.kt` & `GameViewModelExtensions.kt`**:
   - Bültenden tek tıkla doğrudan üretim başlatma fonksiyonu (`quickProduceForBulletinOpportunity(opportunityId)`).
   - Tesis hammadde kontrolü yapılarak üretimin başlatılması veya üretim penceresinin açılması.

4. **Doğrulama & APK**:
   - `compile_applet` ile hatasız derleme.
   - `./export_apk.sh` ile güncel APK dosyasının üretilmesi.

---

## 4. Doğrulama ve Test Senaryoları

1. **Veritabanı Kuralları Testi**:
   - Artvin'de kereste görevi çıkabildiği, Rize gibi izin verilmeyen şehirlerde kereste veya maden görevinin asla çıkmadığı teyit edilecek.
2. **Hızlı Üretim Testi**:
   - Oyuncunun Artvin'de kereste kampı veya Maraş'ta pamuk tarlası varken bülten kartındaki tek tık butonuna basıldığında üretimin başladığı ve ilerleme çubuğunun güncellendiği test edilecek.
3. **Filtre Testi**:
   - `Türkiye` ve `Küresel` sekmelerine tıklandığında ilgili fırsatların filtrelendiği doğrulanacak.
