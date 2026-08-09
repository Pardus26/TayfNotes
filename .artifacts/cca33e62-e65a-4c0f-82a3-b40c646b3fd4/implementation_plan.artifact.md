# TayfNotes: Premium Kimlik ve İkon Tasarımı Planı

Bu plan, TayfNotes uygulamasının marka değerini artıracak "Premium" tarzda bir uygulama ikonu tasarımını ve mevcut zırhlı yapıya entegrasyonunu kapsar.

## User Review Required

> [!IMPORTANT]
> İkon tasarımı tamamen vektörel (XML) olarak yapılacaktır. Bu, uygulamanın boyutunu artırmazken her ekran çözünürlüğünde (HD, 4K, vb.) pürüzsüz görünmesini sağlar. İkon, Evernote'un fil ikonundaki profesyonelliği ve ColorNote'un renk çeşitliliğini modern bir "Tayf" (Spektrum) tasarımıyla birleştirir.

## Proposed Changes

### 1. Görsel Kaynaklar (Premium İkon)
- [MODIFY] `app/src/main/res/drawable/ic_launcher_background.xml`: Derin gece mavisi (`#1A1C1E`) üzerine şık bir ızgara dokusu.
- [MODIFY] `app/src/main/res/drawable/ic_launcher_foreground.xml`: "T" harfi ve spektrum renklerinden oluşan modern logo tasarımı.
- [MODIFY] `app/src/main/res/values/colors.xml`: Premium palet renkleri (Midnight Blue, Spectrum Orange, Gold accent).

### 2. Yapılandırma ve İzolasyonun Korunması
- APK isimlendirme mantığı (`TayfNotes_v01.[buildno].apk`) korunacak.
- GitHub Actions üzerindeki "dosya bazlı yedekleme" sistemi (sourcecodes.zip) aynen çalışmaya devam edecek.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` komutuyla yeni ikon dosyalarının derleme hatası verip vermediği kontrol edilecek.
- APK isminin `TayfNotes_v01.7.apk` (veya sıradaki numara) olduğu doğrulanacak.

### Manual Verification
- Uygulama cihaza yüklendiğinde ana ekrandaki ikonun "Adaptive" (yuvarlak, kare veya squirclge) formlara uyumu gözlemlenecek.
- GitHub Actions "Summary" kısmından yedek zip dosyasının oluştuğu teyit edilecek.
