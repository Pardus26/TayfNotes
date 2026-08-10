### 15. Bulut Senkronizasyon ve Veri Taşıma
- **Drive & Dropbox:** Ayarlar menüsüne Google Drive ve Dropbox entegrasyonu için seçim ve manuel senkronizasyon butonları eklendi.
- **Toplu Veri Yedeği:** Tüm veritabanı ve medya dosyalarını (resim/ses) tek bir ZIP dosyasında toplayıp paylaşmayı sağlayan "Veri Taşıma" özelliği eklendi.
- **Build İzolasyonu:** `gradle.properties` üzerinden paralel build ve daemon kısıtlamaları getirilerek Windows üzerindeki diğer projelerle çakışma riski sıfıra indirildi.
- **APK Güncelleme Garantisi:** Sürüm kodu ve imzalama mantığı sabitlenerek sorunsuz üzerine yükleme desteği doğrulandı.

## Doğrulama Sonuçları
- [x] Yerel build başarılı: `TayfNotes_v01.21.apk` üretildi.
- [x] Toplu veri yedeği (ZIP) oluşturma ve paylaşma fonksiyonu test edildi.
- [x] Bulut sağlayıcı seçimi ve senkronizasyon altyapısı (Ktor tabanlı) onaylandı.
- [x] GitHub Push başarılı: Tüm senkronizasyon ve taşıma kodları `main` branch'ine gönderildi.
