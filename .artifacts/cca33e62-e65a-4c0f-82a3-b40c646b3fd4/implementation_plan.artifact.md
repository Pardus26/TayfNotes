# TayfNotes: Master-Detail Tablet Layout ve Profesyonel Çizim (Sketch) Planı

Bu plan, uygulamayı profesyonel tablet standartlarına (Samsung A73, Lenovo Idea Tab) taşımayı, "Master-Detail" (Yan yana görünüm) yapısını kurmayı ve kalem destekli çizim arayüzünü entegre etmeyi hedefler.

## User Review Required

> [!IMPORTANT]
> **Master-Detail Görünümü:** Geniş ekranlarda (Tablet) sol tarafta not listesi, sağ tarafta not içeriği görünecektir. Telefonlarda ise mevcut navigasyon devam edecektir.
> **Çizim (Canvas):** Elle çizimler yüksek çözünürlüklü vektör verisi olarak saklanacak, böylece 2.5K ekranlarda bozulma olmayacaktır.
> **Veritabanı Analizi:** Şu anki Room yapısı optimize edilmiştir. Isar'a geçiş, KMP (iOS/Masaüstü) fazında daha stabil olacaktır; şu anki zırhlı yapıyı bozmamak adına Room ile devam edip "Sketch" desteği eklenecektir.

## Proposed Changes

### 1. Master-Detail (Adaptive) Arayüzü
- [MODIFY] `MainActivity.kt`: Ekran genişliğine göre `MasterDetailScaffold` mantığının kurulması.
- [MODIFY] `ui/MainScreen.kt`: Seçili notun (active note) state yönetimi.
- [NEW] `ui/DetailPane.kt`: Sağ tarafta görünecek zengin içerik paneli.

### 2. Profesyonel Çizim (Sketch) Arayüzü
- [NEW] `ui/components/DrawingCanvas.kt`: Kalem, fırça, renk ve silgi desteği sunan çizim alanı.
- [MODIFY] `shared/src/.../Note.kt`: Not modeline `sketchData` (çizim yolları/koordinatları) alanının eklenmesi.
- [MODIFY] `ui/NoteEditorScreen.kt`: Çizim moduna geçiş butonu ve canvas entegrasyonu.

### 3. Veri Güvenliği ve Kalıcılık
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`: Tüm state'lerin (tema, seçimler, ayarlar) uygulama kapansa dahi korunması için `DataStore` ve `Room` senkronizasyonunun sıkılaştırılması.
- [MODIFY] `NoteEntity.kt`: Çizim verilerini saklayacak Blob/Text alanının eklenmesi.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile derleme kontrolü.
- APK `TayfNotes_v01.23.apk` üretilecek.

### Manual Verification
- Lenovo Idea Tab (Landscape) modunda sol-sağ bölünmüş ekranın çalıştığı görülecek.
- Kalem (Stylus) ile çizim yapılıp, nottan çıkıp tekrar girildiğinde çizimin korunduğu test edilecek.
- Tema değişiminin her iki panelde de anlık yansıdığı teyit edilecek.
