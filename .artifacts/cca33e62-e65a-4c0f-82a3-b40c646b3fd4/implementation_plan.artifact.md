# TayfNotes: APK Güncelleme Çözümü, Görsel ve Sesli Not Desteği Planı

Bu plan, uygulamanın sürüm güncelleme sorunlarını kökten çözmeyi, notlara resim ve sesli mesaj ekleme özelliklerini entegre etmeyi hedefler.

## User Review Required

> [!IMPORTANT]
> **APK Güncelleme Sorunu:** Sürüm çakışmasını önlemek için sabit bir imzalama anahtarı (keystore) kullanılacak ve `versionCode` her build'de otomatik artırılacaktır. GitHub'dan indirilen her yeni sürüm, eskisini silmeden üzerine yüklenebilecektir.
> **Medya İzinleri:** Kamera, Galeri ve Mikrofon kullanımı için kullanıcıdan çalışma zamanı (runtime) izinleri istenecektir.

## Proposed Changes

### 1. APK Güncelleme ve İmzalama Çözümü
- [MODIFY] `app/build.gradle.kts`: Sabit bir `signingConfigs` bloğu eklenmesi.
- [MODIFY] `version.properties`: `versionCode` artırım mantığının GitHub Actions ile tam senkronize edilmesi.

### 2. Görsel/Resim Ekleme Desteği
- [MODIFY] `shared/src/commonMain/.../Note.kt`: Not modeline `imageUris: List<String>` alanının eklenmesi.
- [MODIFY] `ui/NoteEditorScreen.kt`: Galeri/Kamera ikonları ve resimlerin not içerisinde listelenmesi.

### 3. Sesli Not Alma (Audio Recording)
- [NEW] `util/AudioRecorder.kt`: Ses kaydetme ve oynatma yardımcı sınıfı.
- [MODIFY] `ui/NoteEditorScreen.kt`: Mikrofon ikonu ve kaydedilen seslerin (voice notes) not içerisinde oynatılabilir şekilde sunulması.

### 4. Veri Katmanı
- [MODIFY] `NoteEntity.kt`: Resim URI'ları ve Ses dosyası yollarını saklayacak yeni sütunların eklenmesi.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile başarılı derleme.
- APK isminin `TayfNotes_v01.20.apk` olduğu doğrulanacak.

### Manual Verification
- Cihaza `v01.19` yüklüyken `v01.20`'nin üzerine sorunsuzca kurulduğu test edilecek.
- Not içerisine resim ekleme ve resmin görünürlüğü kontrol edilecek.
- Ses kaydı başlatma, durdurma ve notu açınca tekrar dinleme testi yapılacak.
- GitHub Actions yedekleme otomasyonu kontrol edilecek.
