# TayfNotes: Zırhlı Yapı, Premium İkon ve Gelişmiş Yedekleme Planı

Bu plan, TayfNotes projesinin Windows ortamında tam izolasyonunu sağlarken, GitHub Actions üzerinde her dosyanın ismini değiştirerek yedekleyen mekanizmayı kesinleştirir ve ana ekran geliştirmesine geçer.

## User Review Required

> [!IMPORTANT]
> GitHub Actions üzerinde her dosyanın ismini `dosyaadı.uzantısı-buildno_commitno` şeklinde değiştirmek, zipleme işlemi öncesinde dosyaların geçici bir klasöre kopyalanmasını ve yeniden adlandırılmasını gerektirir. Bu işlem orijinal dosya yollarını (klasör yapısını) korumak için klasör hiyerarşisini de içeren bir script ile yapılacaktır.

## Proposed Changes

### 1. Zırhlı Yapı ve İzolasyon (Windows/AS Güvenliği)
- [MODIFY] `gradle.properties`: Diğer projelerle çakışmayı önlemek için `org.gradle.daemon=false` ve izole bir `gradle.user.home` benzeri yapılandırma önerisi (opsiyonel, şimdilik daemon kapatılacak).
- [MODIFY] `app/build.gradle.kts`: APK isimlendirme mantığının (`TayfNotes_v01.[buildno].apk`) korunması ve derleme hatalarının giderilmesi.

### 2. Gelişmiş GitHub CI/CD Otomasyonu
- [MODIFY] `.github/workflows/android.yml`:
    - Build numarası (`GITHUB_RUN_NUMBER`) ve kısa Commit ID (`GITHUB_SHA_SHORT`) kullanımı.
    - Kaynak kodları tarayan ve her dosyayı `filename.ext-build_commit` olarak adlandıran optimize edilmiş Bash scripti.
    - `sourcecodes_buildno_commitno.zip` dosyasının otomatik oluşturulması ve artifact olarak yüklenmesi.

### 3. Ana Ekran (Renkli Izgara) Başlangıcı
- [NEW] `ui/MainScreen.kt`: ColorNote tarzı renkli, ızgara yapısında not listeleme ekranı.
- [NEW] `ui/components/NoteGridItem.kt`: Her bir notun premium stil ve renkle gösterileceği bileşen.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile yerel APK üretimi.
- APK isminin `TayfNotes_v01.x.apk` olduğunun doğrulanması.
- GitHub Actions üzerinde `build` job'unun tamamlanması ve artifact'lerin (APK + Zip) oluşması.

### Manual Verification
- GitHub'dan indirilen `sourcecodes_*.zip` içindeki dosyaların isim formatının (Örn: `MainActivity.kt-8_a1b2c3d`) kontrol edilmesi.
- Uygulama ikonunun premium tarzda cihazda göründüğünün teyit edilmesi.
