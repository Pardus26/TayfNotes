# TayfNotes: Premium Görünüm, Gerçek Senkronizasyon ve Gelişmiş Sketch Planı

Bu plan, uygulamayı profesyonel bir tasarım ve dökümantasyon aracına dönüştürmeyi, görsel kontrast sorunlarını gidermeyi ve "sanal" olan özellikleri gerçeğe dönüştürmeyi hedefler.

## User Review Required

> [!IMPORTANT]
> **Gerçek Senkronizasyon:** Google Drive ve Dropbox için OAuth2 akışları entegre edilecektir. Kullanıcıdan hesap seçimi istenecektir.
> **Sketch Devrimi:** Çizim alanı; otomatik şekil tanıma, dolgu, gölgeleme ve gelişmiş fırça kontrolleriyle donatılacaktır.
> **Tema Kontrastı:** Tüm metin ve ikon renkleri, seçilen arka plan rengine göre (aydınlık/karanlık) zıt ve belirgin hale getirilecektir.

## Proposed Changes

### 1. Görsel İyileştirmeler (Kontrast ve Kontrol)
- [MODIFY] `ui/theme/Theme.kt`: Arka plan rengine göre metin ve ikon renklerini dinamik hesaplayan mantık (Örn: luminance kontrolü).
- [MODIFY] `ui/components/ColorSelector.kt`: Tema ile çakışmayan, premium ve belirgin seçim alanı.

### 2. Veri Yönetimi (İçe Aktar ve Bulut)
- [NEW] `util/BackupImportHelper.kt`: Dışa aktarılan ZIP yedeklerini uygulamaya geri yükleme (Veritabanı + Medya).
- [MODIFY] `shared/src/.../SyncManager.kt`: Drive ve Dropbox için gerçek API çağrıları ve hesap yetkilendirme ekranları.
- [MODIFY] `ui/SettingsScreen.kt`: İçe aktar butonu ve gerçek bulut sağlayıcı yönetimi.

### 3. Not Ekleme ve Sketch Ayrımı
- [MODIFY] `ui/components/AddNoteDialog.kt`: Metin, Kontrol Listesi ve Sketch olarak 3 ana kategoriye ayrılmış seçim menüsü.
- [MODIFY] `ui/NoteEditorScreen.kt`: Sketch modunun bağımsız bir "Canvas" editörü olarak yapılandırılması.

### 4. Gelişmiş Sketch (Canvas) Özellikleri
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - **Fırça Kontrolü:** Kalınlık artırma/azaltma (Slider).
    - **Araçlar:** Kalem, İşaretleyici, Silgi.
    - **Şekiller:** Kare, Daire, Üçgen (Otomatik çizim ve boyutlandırma).
    - **Gelişmiş Renk:** Seçilen rengin anında kaydedilmesi ve şekil içi doldurma (Fill) / gölgeleme (Shadow) desteği.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile build kontrolü.
- APK `TayfNotes_v01.25.apk` üretilecek.

### Manual Verification
- Koyu temada yazıların beyaz, açık temada siyah olduğu test edilecek.
- Dropbox/Drive'da "Hesap Seç" ekranının açıldığı doğrulanacak.
- Sketch ekranında bir kare çizilip içi renkle doldurulacak.
- Alınan bir yedeğin "İçe Aktar" ile başarılı yüklendiği görülecek.
