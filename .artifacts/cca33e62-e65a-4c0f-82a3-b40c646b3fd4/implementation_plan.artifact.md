# TayfNotes: Tablet Uyumluluğu ve Gelişmiş Kontrol Listesi (Microsoft To Do Stili) Planı

Bu plan, Lenovo Idea Tab (TB336FU) ve benzeri yüksek çözünürlüklü tabletlerdeki görsel hataları gidermeyi, veri kaybolma sorunlarını çözmeyi ve Kontrol Listesi özelliğini profesyonel bir seviyeye taşımayı hedefler.

## User Review Required

> [!IMPORTANT]
> **Tablet Uyumluluğu:** Ekran yoğunluğu ve yüksek çözünürlük (2.5K) dikkate alınarak tüm UI bileşenleri `Adaptive` (uyarlanabilir) hale getirilecektir.
> **Kontrol Listesi:** Microsoft To Do benzeri; tamamlananların üzerinin çizilmesi, alta taşınması ve alt görev desteği eklenecektir.

## Proposed Changes

### 1. Tablet ve Yüksek Çözünürlük Düzeltmeleri
- [MODIFY] `ui/MainScreen.kt`: Izgara (Grid) yapısının tabletlerde 3 veya 4 sütunlu olacak şekilde dinamikleştirilmesi.
- [MODIFY] `ui/NoteEditorScreen.kt`: Yüksek çözünürlükte görünmeyen renk paleti ve seçim butonlarının görünürlük/padding ayarlarının düzeltilmesi.
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`:
    - Klasör filtreleme mantığının düzeltilmesi (Klasör seçili değilse "Tüm Notlar"ın doğru gösterilmesi).
    - Veritabanı değişimlerinin anlık olarak UI'a yansıması için `StateFlow` akışının optimize edilmesi.

### 2. Gelişmiş Kontrol Listesi (Microsoft To Do Esintili)
- [MODIFY] `shared/src/.../Note.kt`: Not içeriğinin `CHECKLIST` tipinde JSON olarak saklanması (Görev adı, Durum, Alt adımlar).
- [NEW] `ui/components/ChecklistEditor.kt`: İnteraktif, sürüklenebilir ve tamamlandığında üzeri çizilen liste bileşeni.
- [MODIFY] `ui/NoteEditorScreen.kt`: Kontrol listesi modunda gelişmiş düzenleyicinin aktif edilmesi.

### 3. Veri Kaybolma ve Senkronizasyon Hataları
- [MODIFY] `data/dao/NoteDao.kt`: Klasör filtresi `null` olduğunda tüm notları getiren `COALESCE` veya opsiyonel sorgu mantığının iyileştirilmesi.
- [MODIFY] `MainActivity.kt`: Tablet yatay/dikey mod geçişlerinde state kaybının önlenmesi.

### 4. Görselleştirme ve Temalar
- [MODIFY] `ui/theme/Theme.kt`: Tabletlerdeki tema değişim hatalarının (recomposition) giderilmesi.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile başarılı derleme.
- APK `TayfNotes_v01.22.apk` üretilecek.

### Manual Verification
- Tablet simülatöründe (veya Lenovo cihazda) notların anlık güncellendiği test edilecek.
- Klasörler arası geçiş yapıldığında "Klasörsüz" notların kaybolmadığı teyit edilecek.
- Kontrol listesinde bir eleman işaretlendiğinde animasyonlu şekilde alta geçtiği ve üzerinin çizildiği görülecek.
