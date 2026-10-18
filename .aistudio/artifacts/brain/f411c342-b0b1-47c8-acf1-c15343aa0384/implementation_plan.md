# İnsan Kaynakları Yönetici Profil Kartı (ManagerProfileCard) Milimetrik Düzen & Dinamik Grafik Optimizasyonu

Kullanıcı tarafından sağlanan örnek referans görsel (`image.png`) temel alınarak, `ManagerProfileCard.kt` ve `HrVisualComponents.kt` üzerindeki boşluklar, metin yerleşimleri, dinamik mini çizgi grafiği (sparkline) ve dairesel verimlilik halkası (%70) ConstraintLayout kılavuz çizgileriyle milimetrik olarak güncellenecektir.

## Onaylanan Kararlar (Kullanıcı Tercihleri)
- **Dinamik Grafik & Göstergeler**: Maaş kutusuna canlı çizgi grafik (Canvas sparkline) ve Pasif Etki kutusuna dairesel verimlilik yüzde halkası (% Canvas circular arc) çizilecek.
- **Düzen Hassasiyeti**: Şablon alanlarına `ConstraintLayout` kılavuz çizgileri (guidelines) ile milimetrik oranlı kilitli hizalama uygulanacak.

---

### 1. Genel Bakış ve Hedef
Referans görseldeki tüm bilgi alanlarını (İsim, Ünvan, Yıldızlar, Hiyerarşi Mührü, Maaş & Trend Grafiği, Dairesel Pasif Etki Göstergesi, Karar Günlüğü & Zaman Damgası, Sicil ve Seviye Butonları) kart şablonu üzerindeki yuvalara milimetrik olarak oturtmak.

```
┌─────────────────────────────────────────────────────────────────────────┐
│ ┌─────────┐  Ayşe Kaya, CFA                           ★★★★★  ┌───┐  │
│ │  AVATAR │  Hazine Müdürü                                    │#1 │  │
│ │         │  ┌────────────────────┐   ┌─────────────────────┐ └───┘  │
│ │ ┌─────┐ │  │ Maaş               │   │ Hiyerarşi & Pasif   │        │
│ │ │🏦 🔒│ │  │ Ⓐ 57.5 K            │   │  (70%) Nakit Rezerv │        │
│ └─┴─────┴─┘  │ 📈 ~~~▲~~~📈       │   │        Koruma %70   │        │
│              └────────────────────┘   └─────────────────────┘        │
│ ┌───────────────────────────────────────────────────────────┬──────┐ │
│ │ ● Ayşe Kaya: Fazla likidite aktarıldı (+500K)             │19:54 │ │
│ └───────────────────────────────────────────────────────────┴──────┘ │
│ ┌───────────────────────────┐    ┌─────────────────────────────────┐ │
│ │ 📜 Sicil & Loglar (10)    │    │ ★ Maks Seviye (Lvl 5)       (5) │ │
│ └───────────────────────────┘    └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 2. Görsel Tasarım & Alan Hizalama Parametreleri

1. **Üst Bilgi Katmanı**:
   - **İsim & Ünvan**: `Açık Gri / Beyaz Bold` ve `Altın Sarısı (0xFFFFD700)` hiyerarşik tipografi.
   - **Yıldızlar**: `manager.level` değerine göre 1-5 arası altın sarısı yıldızlar.
   - **Hiyerarşi Mührü**: Sağ üst köşede `#1`, `#2` hiyerarşi rozet metni.

2. **Maaş Kutusu (Orta Sol)**:
   - **Etiket**: "Maaş" küçük gri metin.
   - **Değer**: `Ⓐ 57.5 K` altın simgesi ve sarı bold sayı.
   - **Canlı Trend Grafiği**: Kutu zemininde Canvas ile çizilen sarı neon çizgi grafik (`Path` & `drawPath`).

3. **Pasif Etki Kutusu (Orta Sağ)**:
   - **Etiket**: "Hiyerarşi & Pasif Etki" üst başlığı.
   - **Dairesel Halka**: Sol tarafta Canvas ile çizilen neon sarı dairesel yüzde göstergesi (`drawArc` ile `%70` veya verimlilik oranı).
   - **Açıklama**: Halkanın sağında "Nakit Rezerv Koruma %70" iki satırlı net beyaz metin.

4. **Karar Akışı Barı (Alt İnce Bar)**:
   - **Mermi Noktası**: Sarı/cyan `●` mermi noktası ve son log metni.
   - **Zaman Damgası**: Sağ tarafta `HH:mm` formatında gri zaman damgası.

5. **Aksiyon Butonları**:
   - **Sicil & Loglar**: "📜 Sicil & Loglar (10)" sol buton.
   - **Seviye Butonu**: "★ Maks Seviye (Lvl 5)" sağ buton ve sağ tarafındaki dairesel seviye rozeti "(5)".

---

### 3. Teknik Mimari & Bileşen Yapısı

- **`ManagerProfileCard.kt`**:
  - `Box` + `Image(painterResource, ContentScale.FillBounds)` kart zemin resmi.
  - `ConstraintLayout` kılavuz çizgileri (`createGuidelineFromTop`, `createGuidelineFromStart`, vb.) ile hassas koordinat haritalaması.
  - Custom Compose `Canvas` çizim fonksiyonları:
    - `MiniSparklineChart`: Maaş kutusu içi trend çizgisi.
    - `CircularProgressGauge`: Pasif etki kutusu içi dairesel verimlilik halkası.
- **`HrVisualComponents.kt`**:
  - `ExecutiveDirectorCard` ve `ExecutiveDossierDialog` bileşenlerinin güncellenen `ManagerProfileCard` ile uyumlandırılması.

---

### 4. Uygulama Adımları

1. **`ManagerProfileCard.kt` Alan Kılavuz Çizgilerinin Hassaslaştırılması**:
   - Görseldeki kutu sınırlarına göre `ConstraintLayout` oranlarının ayarlanması.
2. **Canvas Mini Sparkline & Dairesel Halka Çizimi**:
   - Maaş kutusuna özel dinamik grafik ve Pasif Etki dairesel gösterge bileşenlerinin yazılması.
3. **Buton ve Rozet Hizalamalarının Tamamlanması**:
   - Alt butonlardaki text ve rozetlerin şablona tam oturtulması.
4. **Derleme & APK Çıktısı**:
   - `compile_applet` ile doğrulama ve `./export_apk.sh` ile güncel APK üretimi.
