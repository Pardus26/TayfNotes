# TayfNotes: Not Ekleme ve Düzenleme Ekranı Planı

Bu plan, uygulamanın ana ekranından erişilebilen, hem Evernote'un detaycılığını hem de ColorNote'un renk odaklı yapısını birleştiren "Not Editörü" ekranının geliştirilmesini kapsar.

## User Review Required

> [!TIP]
> Not editöründe, seçilen rengin tüm ekranın arka planına (veya üst barına) hafifçe yansıdığı "Premium" bir geçiş efekti kullanacağız. Bu, ColorNote deneyimini modern bir düzeye taşır.

## Proposed Changes

### 1. `:app` Modülü (UI Geliştirme)
- [NEW] `ui/NoteEditorScreen.kt`: Not başlığı, içeriği, renk seçici ve etiket girişini barındıran ekran.
- [NEW] `ui/components/ColorSelector.kt`: ColorNote stilinde, notun rengini hızlıca değiştirmeyi sağlayan yatay kaydırmalı renk paleti.
- [MODIFY] `ui/MainScreen.kt`:
    - FAB (Artı butonu) tıklandığında "Ekleme" moduna geçiş.
    - Not kartına tıklandığında "Düzenleme" moduna geçiş.
- [MODIFY] `MainActivity.kt`: Basit bir "Screen Navigation" mantığı ekleyerek ekranlar arası geçişi sağlama.

### 2. İzolasyon ve Otomasyonun Korunması
- `version.properties` üzerinden otomatik versiyonlama devam edecek.
- GitHub Actions yedekleme ve APK isimlendirme kuralları (`TayfNotes_v01.x.apk`) aynen korunacak.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile APK üretiminin başarısı.
- Yeni APK isminin (Örn: `TayfNotes_v01.9.apk`) kontrolü.

### Manual Verification
- Ana ekrandaki "+" butonuna basıldığında editörün açılması.
- Editörde renk seçildiğinde kartın renginin değiştiğinin görülmesi.
- "Kaydet" (veya geri) butonuna basıldığında ana ekrana dönülmesi.
