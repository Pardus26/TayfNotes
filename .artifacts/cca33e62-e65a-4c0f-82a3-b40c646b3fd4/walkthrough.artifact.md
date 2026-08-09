# TayfNotes: Zırhlı Yapı ve Yedekleme Sistemi Kurulumu

Bu doküman, TayfNotes projesi için kurulan izole geliştirme ortamını ve GitHub Actions üzerindeki özel yedekleme mekanizmasını özetler.

## Yapılan Kurulumlar

### 1. Zırhlı Proje İzolasyonu
- **İzole Namespace:** Proje `com.eldora25.tayfnotes` olarak diğer projelerden ayrıştırıldı.
- **AndroidX & Stabilite:** `gradle.properties` içerisine `useAndroidX=true` ve `enableJetifier=true` eklenerek modern kütüphane uyumluluğu sağlandı.
- **Hatasız Build:** Yerel ve GitHub build süreçleri Java 11 ve AGP 8.7.3 standartlarına sabitlendi.

### 2. Akıllı APK İsimlendirme
- Her build işleminde `version.properties` dosyasındaki build numarası kontrol edilir.
- APK dosyası otomatik olarak `TayfNotes_v01.[build_no].apk` şeklinde isimlendirilir.

### 3. GitHub Actions Kaynak Kod Yedeği
GitHub Actions workflow dosyası (`android.yml`) şu karmaşık mantığı işleyecek şekilde güncellendi:
- **Dosya Bazlı Adlandırma:** Yedekleme sırasında her dosya `dosyaadı.uzantısı-buildno_commitno` (Örn: `MainActivity.kt-5_1e9e676`) şeklinde yeniden adlandırılır.
- **Dinamik Zip:** Tüm yedeklenen dosyalar `sourcecodes_buildno_commitno.zip` adıyla paketlenir.
- **Artifact Yükleme:** Oluşan zip dosyası GitHub Actions "Summary" sayfasından indirilebilir.

## Doğrulama Sonuçları
- [x] Yerel build başarılı: `TayfNotes_v01.6.apk` üretildi.
- [x] GitHub Push başarılı: Tüm kodlar `main` branch'ine gönderildi.
- [x] Workflow güncellendi ve test edildi.

![GitHub Actions Örneği](https://via.placeholder.com/600x200?text=APK+ve+SourceCode+Backup+Artifacts+Hazir)
