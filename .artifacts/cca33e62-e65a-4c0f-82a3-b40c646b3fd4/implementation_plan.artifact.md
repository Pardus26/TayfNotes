# TayfNotes: Profesyonel Gelişim, Sketch Devrimi ve Gerçek Bulut Senkronizasyonu Planı

Bu plan, uygulamayı piyasadaki en üst düzey not ve tasarım araçlarıyla (Microsoft To Do, Procreate vb.) yarışacak seviyeye getirmeyi, tablet deneyimini kusursuzlaştırmayı ve veri güvenliğini parmak izi ile zırhlamayı hedefler.

## User Review Required

> [!IMPORTANT]
> **Gerçek Bulut Senkronizasyonu:** Bu aşamada Google Drive ve Dropbox için gerçek kullanıcı girişi (OAuth2) zorunlu hale getirilecektir.
> **Sketch ve Canvas:** Çizim alanı artık sadece bir eklenti değil, başına buyruk bir tasarım aracı olacaktır. Şekiller, dolgular ve gölgeler vektörel olarak saklanacaktır.
> **İnce Ayarlar:** Uygulama açılışındaki biyometrik kilit, sadece ilk girişte çalışacak şekilde optimize edilecektir.

## Proposed Changes

### 1. Görsel ve Kontrast Devrimi
- [MODIFY] `ui/theme/Theme.kt`: İkon ve yazıların dışına zıt renkli "Premium Outline" ekleyen özel modifier'ların tanımlanması.
- [MODIFY] `ui/components/NoteGridItem.kt`: Kontrol listesi ve sketch önizlemelerinin görsellerdeki gibi profesyonel hale getirilmesi.

### 2. Gelişmiş Master-Detail ve Tablet Deneyimi
- [MODIFY] `MainActivity.kt`:
    - Tablet yatay modda sol (Liste) ve sağ (Detay) panel ayrımı.
    - Biyometrik kilidin sadece uygulama ilk açıldığında tetiklenmesi.
- [MODIFY] `ui/MainScreen.kt`:
    - Üç ayrı "Ekle" butonu (Not, Liste, Sketch).
    - Sağ üst hamburger menü ile Sıralama (Zaman, Alfabetik, Renk) seçenekleri.
    - Sürükle-Bırak (Drag and Drop) ile manuel sıralama desteği.

### 3. Sketch (Canvas) ve Tasarım Araçları
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - Kalem, Fırça, Marker ve Silgi araçları.
    - **Akıllı Şekiller:** Kare, Dikdörtgen, Daire, Elips, Yay, Üçgen (Otomatik çizim, taşıma ve boyutlandırma).
    - **Dolgu ve Gölge:** Şekil içlerini renkle doldurma ve gölgelendirme.
    - Kalınlık kontrolünün hem artırma hem azaltma (Slider) olarak düzeltilmesi.
    - Canvas üstüne ve altına metin notu ekleme bölümleri.

### 4. Gerçek Bulut ve Veri Yönetimi
- [MODIFY] `shared/src/.../SyncManager.kt`: Drive ve Dropbox için gerçek API entegrasyonu ve çift taraflı senkronizasyon.
- [MODIFY] `util/BackupImportHelper.kt`: ZIP yedeğini veritabanı ve medyayla birlikte geri yükleme.
- [NEW] `ui/ArchiveScreen.kt` & `ui/TrashScreen.kt`: Arşivleme ve Geri Dönüşüm Kutusu mantığı.

### 5. Hata Düzeltmeleri (Bug Fixes)
- [FIX] Ses kaydı esnasındaki çökme sorunu (AudioRecorder/Permission).
- [FIX] Resimlerin önizleme ve detay panelinde görünmemesi.
- [FIX] Klasör not sayılarının (Note Count) güncellenmemesi.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile build kontrolü.
- APK `TayfNotes_v01.25.apk` üretilecek.

### Manual Verification
- Tablette yan çevirince Master-Detail görünümü ve yazıların belirginliği test edilecek.
- Dropbox/Drive ile gerçek veri alışverişi denenecek.
- Sketch ekranında karmaşık şekiller çizilip kaydedilecek.
- Ses kaydı yapılıp dinlenecek.
