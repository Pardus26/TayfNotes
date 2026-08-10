# TayfNotes: Teknik Mükemmellik ve Profesyonel Arayüz Planı (v28)

Bu plan, uygulamanın görsel zırhını güçlendirmeyi, tablet ve telefonlarda Master-Detail yapısını kusursuzlaştırmayı ve multimedya/sketch özelliklerini profesyonel seviyeye taşımayı hedefler.

## User Review Required

> [!IMPORTANT]
> **Sürükle-Bırak:** Not ve klasör sıralaması artık manuel olarak (Drag-and-Drop) yapılabilecektir.
> **Gerçek Senkronizasyon:** Dropbox ve Drive için gerçek kimlik doğrulama akışları (Auth) entegre edilecektir.
> **Sketch Devrimi:** Çizimlerde her işlem değil, sadece "Bitti" denildiğinde tekil kayıt yapılacaktır.

## Proposed Changes

### 1. Görsel Zırh ve Neon İkonlar (Madde 1, 8)
- [MODIFY] `ui/theme/Theme.kt`: Tüm üst menü ikonlarını zıt renkli kapsül ve **Neon Işıltısı** ile saran `NeonIcon` bileşeni.
- [MODIFY] `ui/ThemeSelectionScreen.kt`: Her temanın renklerini kutucuklar halinde gösteren şık bir önizleme ızgarası.

### 2. Gelişmiş Navigasyon ve Master-Detail (Madde 2)
- [MODIFY] `MainActivity.kt`: Dikey modda dahi 0.4f/0.6f oranında sol liste ve sağ boş/detay panel ayrımı. Not başlıklarının dar alana dinamik adaptasyonu.

### 3. Sürükle-Bırak ve Akıllı Sıralama (Madde 3, 4, 5)
- [MODIFY] `ui/MainScreen.kt` & `ui/FoldersScreen.kt`:
    - `reorderable` liste desteği ile manuel sıralama.
    - Klasörler için sağ üst hamburger menü ve 4'lü sıralama (Tarih, Ad, Renk).

### 4. Gelişmiş Sketch ve Tasarım (Madde 10, 11, 12, 14)
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - Tekil kayıt: Çizim bitmeden veritabanına kayıt yapılmayacak.
    - Gerçek Renk: Kalem/Fırça seçiliyken renk değiştirme anlık ve kalıcı.
    - Şekil Pro: Duvar (Stroke) ve İç (Fill) renklerinin bağımsız ayarlanması.
    - Önizleme: Detay panelinde sketch çizimlerinin tam görünürlüğü.

### 5. Veri ve Senkronizasyon (Madde 6, 7, 9, 13)
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`:
    - Çöp kutusu ve Arşiv mantığının veri silme yerine ID taşıma ile stabilizasyonu.
    - Gerçek Bulut Senkronizasyonu için OAuth2 altyapısının aktif edilmesi.
- [FIX] `AudioRecorder`: Ses kaydı hata kodlarının (Permission/Path) çözülmesi.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile build kontrolü.
- APK `TayfNotes_v01.28.apk` üretilecek.

### Manual Verification
- Dikey modda sağ panelin boş kalıp not seçilince dolduğu görülecek.
- Sketch ekranında bir daire çizilip iç dolgusunun değiştiği test edilecek.
- Ses kaydı yapılıp hata almadan kaydedildiği görülecek.
- Sürükle-bırak ile notların yerinin değiştiği doğrulanacak.
