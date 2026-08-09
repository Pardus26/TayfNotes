# TayfNotes: Zırhlı Yapı ve GitHub Kaynak Kod Yedekleme Planı

Bu plan, TayfNotes projesinin APK isimlendirme kurallarını sağlamlaştırırken, GitHub Actions üzerinde her dosyanın ismini değiştirerek yedekleyen bir mekanizma kurmayı hedefler.

## User Review Required

> [!IMPORTANT]
> GitHub Actions üzerinde her dosyanın ismini `dosyaadı.uzantısı-buildno_commitno` şeklinde değiştirmek, zipleme işlemi öncesinde dosyaların geçici bir klasöre kopyalanmasını ve yeniden adlandırılmasını gerektirir. Bu işlem orijinal dosya yollarını (klasör yapısını) korumak için klasör hiyerarşisini de içeren bir script ile yapılacaktır.

## Proposed Changes

### 1. GitHub CI/CD Geliştirmesi
- [MODIFY] `.github/workflows/android.yml`:
    - Build numarası (`GITHUB_RUN_NUMBER`) ve Commit ID (`GITHUB_SHA_SHORT`) alınacak.
    - Kaynak kodları (src/, build.gradle, vb.) tarayan bir bash script eklenecek.
    - Her dosya `backup/` klasörüne istediğiniz formatta taşınacak.
    - `sourcecodes_buildno_commitno.zip` oluşturulup artifact olarak yüklenecek.

### 2. Proje İzolasyonu (Shielded Structure)
- [MODIFY] `gradle.properties`: Projenin bağımsız çalışmasını garanti altına alan (`android.useAndroidX=true`, vb.) ayarların korunması.
- [MODIFY] `build.gradle.kts`: APK isimlendirme mantığının `version.properties` ile tam uyumu.

## Verification Plan

### Automated Verification
- GitHub Actions üzerinde `build` job'unun tamamlanması.
- "Actions" sekmesinde iki adet artifact'in görünmesi:
    1. `TayfNotes-APKs` (APK dosyası)
    2. `TayfNotes-SourceCode-Backup` (Zipli kaynak kodları)

### Manual Verification
- Zipli dosyayı indirip içindeki bir dosyanın ismini kontrol etme: Örn: `MainActivity.kt-5_a1b2c3d`.
