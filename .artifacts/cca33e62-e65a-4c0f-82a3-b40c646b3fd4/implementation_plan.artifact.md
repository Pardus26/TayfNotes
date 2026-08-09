# TayfNotes: Not Silme ve Arama Özellikleri Uygulama Planı

Bu plan, uygulamaya Evernote ve ColorNote'un temel işlevselliklerinden olan "Not Silme" ve "Metin Bazlı Arama" özelliklerinin eklenmesini kapsar.

## User Review Required

> [!TIP]
> Arama özelliği, ana ekranda interaktif bir arama çubuğu (Search Bar) ile sunulacaktır. Silme özelliği ise hem not editörü içerisinden hem de ana ekranda uzun basma (veya silme butonu) ile erişilebilir olacaktır.

## Proposed Changes

### 1. Veri Katmanı (Data Layer)
- [MODIFY] `data/dao/NoteDao.kt`: Metin içeriğine veya başlığa göre arama yapmayı sağlayan SQL sorgusunun eklenmesi.
- [MODIFY] `data/repository/NoteRepository.kt`: Arama fonksiyonunun repository katmanına taşınması.

### 2. İş Mantığı (ViewModel)
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`:
    - `searchQuery` state'inin eklenmesi.
    - Arama sonuçlarını anlık olarak filtreleyen mantığın kurulması.
    - Not silme fonksiyonunun UI tarafından tetiklenmesi.

### 3. UI Geliştirme (Compose)
- [MODIFY] `ui/MainScreen.kt`:
    - `TopAppBar` içerisine etkileşimli bir arama çubuğu eklenmesi.
    - Not listesinin arama sorgusuna göre filtrelenmesi.
- [MODIFY] `ui/NoteEditorScreen.kt`:
    - Mevcut bir not düzenleniyorsa "Sil" butonu (Çöp kutusu ikonu) eklenmesi.
- [MODIFY] `MainActivity.kt`: Silme işlemi sonrası navigasyonun yönetilmesi.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile APK üretim başarısı.
- APK isminin `TayfNotes_v01.13.apk` (veya sıradaki numara) olduğu doğrulanacak.

### Manual Verification
- Arama çubuğuna bir kelime yazıldığında sadece o kelimeyi içeren notların listelendiği görülecek.
- Not editöründe "Sil" butonuna basıldığında onay istenip notun silindiği ve ana ekrana dönüldüğü teyit edilecek.
- GitHub Actions üzerinde `sourcecodes_*.zip` dosyasının yeni özellikleri içerdiği kontrol edilecek.
