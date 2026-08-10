# TayfNotes: Bulut Senkronizasyon (Drive/Dropbox) ve Toplu Veri Transferi Planı

Bu plan, TayfNotes'u cihazlar arası (telefon-tablet) tam uyumlu bir ekosisteme dönüştürmeyi ve veritabanı yedeğini medya dosyalarıyla birlikte tek bir paket olarak taşımayı hedefler.

## User Review Required

> [!IMPORTANT]
> **Bulut API Anahtarları:** Google Drive ve Dropbox entegrasyonu için uygulama seviyesinde API istemci kimlikleri gereklidir. Bu aşamada teknik altyapı (Ktor tabanlı) ve kullanıcı arayüzü kurulacaktır.
> **İzolasyon Garantisi:** Proje paket ismi (`com.eldora25.tayfnotes`) ve build sistemi, Windows üzerindeki diğer projelerden tamamen izole kalarak çakışmaları önleyecek şekilde ayarlanmıştır.

## Proposed Changes

### 1. Bulut Senkronizasyon Altyapısı (Drive & Dropbox)
- [NEW] `data/sync/CloudProvider.kt`: Google Drive ve Dropbox için ortak arayüz (Interface).
- [MODIFY] `shared/src/.../SyncManager.kt`: Seçilen bulut sağlayıcısına göre otomatik veri gönderme ve çekme mantığı.
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`: Senkronizasyon durumunu ve tercihlerini yöneten yeni state'ler.

### 2. Toplu Veri Dışa Aktarma ve İçe Aktarma (Migration)
- [NEW] `util/BackupPackageHelper.kt`: Tüm veritabanını, klasörleri ve resimleri/sesleri tek bir `.tnb` (TayfNotes Backup) veya `.zip` dosyasında toplama mantığı.
- [MODIFY] `ui/SettingsScreen.kt`: "Tüm Veriyi Yedekle ve Paylaş" butonu ile senkronizasyona gerek kalmadan veri taşıma özelliği.

### 3. UI Geliştirme
- [MODIFY] `ui/SettingsScreen.kt`:
    - "Bulut Yedekleme ve Senkronizasyon" kategorisi.
    - Google Drive ve Dropbox seçim anahtarları.
    - "Şimdi Senkronize Et" butonu.

### 4. Sistem Güvenliği ve Build
- [MODIFY] `gradle.properties`: `org.gradle.daemon=false` ve `org.gradle.parallel=false` ayarlarıyla sistem kaynaklarının diğer projelerle çakışmaması sağlanacaktır.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile başarılı derleme.
- APK isminin `TayfNotes_v01.21.apk` olduğu doğrulanacak.

### Manual Verification
- Ayarlardan Google Drive seçildiğinde (simüle edilmiş) bağlantı kontrolü.
- "Tüm Veriyi Dışa Aktar" denildiğinde medya dosyaları dahil bir zip dosyası oluştuğu teyit edilecek.
- GitHub Actions yedekleme otomasyonu kontrol edilecek.
