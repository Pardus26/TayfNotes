# TayfNotes: Profesyonel Arayüz, Gerçek Senkronizasyon ve Gelişmiş Sketch Planı (v27)

Bu plan, TayfNotes'u görsel, fonksiyonel ve teknik olarak piyasadaki en üst düzey (Microsoft To Do, Procreate, Google Keep) standartlara taşımayı hedefler.

## User Review Required

> [!IMPORTANT]
> **Gerçek Senkronizasyon:** Google Drive ve Dropbox için gerçek OAuth2 akışları ve kullanıcı kimlik yönetimi kurulacaktır.
> **Master-Detail:** Hem dikey hem yatay modda Master-Detail (Liste-Detay) yapısı aktif edilecektir.
> **Sürükle-Bırak:** Not ve klasör sıralaması manuel olarak (Reorderable) değiştirilebilecektir.

## Proposed Changes

### 1. Görsel Zırh ve Kontrast (Madde 1, 11)
- [MODIFY] `ui/theme/Theme.kt`: İkon ve yazılara zıt renkli "Outline" ve Neon ışıltı (Glow) efektleri ekleyen bileşenler.
- [NEW] `ui/ThemeSelectionScreen.kt`: Şık bir önizleme ile sadece tema ve karanlık modun seçildiği ekran.

### 2. Akıllı Navigasyon ve Tablet Uyumu (Madde 2, 3, 4, 13)
- [MODIFY] `MainActivity.kt`: Dikey ve yatay modda Master-Detail (Sol Liste, Sağ Detay) yapısının stabilizasyonu.
- [MODIFY] `ui/MainScreen.kt`: Ana ekranda yatay dizilmiş, eşit büyüklükte 3 ana buton (Not, Liste, Sketch).

### 3. Sıralama ve Manuel Düzenleme (Madde 5, 6, 7, 8)
- [MODIFY] `ui/MainScreen.kt` & `ui/FoldersScreen.kt`:
    - Sağ üst hamburger menü ile akıllı sıralama (Tarih, Alfabetik, Renk).
    - `LazyColumn` üzerinde sürükle-bırak (Drag-and-Drop) ile manuel sıralama desteği.

### 4. Gelişmiş Sketch ve Tasarım (Madde 14, 15, 16, 18)
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - Tekil kayıt mantığı: Çizimin her aşaması değil, "Tamamla" denildiğinde tek bir veri olarak saklanması.
    - Gelişmiş Renk Seçici: Sınır rengi ve dolgu rengi (Fill) ayrımı.
    - Akıllı Şekiller: Kare, Daire, Elips, Yay, Dikdörtgen (Boyutlandırılabilir ve Taşınabilir).
    - Fırça ve kalemlerde seçilen rengin anlık uygulanması.

### 5. Veri Yönetimi ve Gerçek Bulut (Madde 9, 10, 12)
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`:
    - Çöp kutusuna taşıma ve kalıcı silme (onaylı) mantığı.
    - Gerçek Google/Dropbox hesap bağlama ve benzersiz cihaz kimliği ile çift taraflı eşitleme.
- [MODIFY] `shared/src/.../SyncManager.kt`: Gerçek ağ çağrıları ve API entegrasyonu.

### 6. Multimedya ve Fixler (Madde 17)
- [FIX] Ses kaydı esnasındaki "Error" veren izin ve kayıt başlatma hatası.
- [FIX] Resimlerin ve Sketch'lerin önizleme/detay panelinde görünmeme sorunu.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile build kontrolü.
- APK `TayfNotes_v01.27.apk` üretilecek.

### Manual Verification
- Maddelerin her biri (Sürükle-bırak, Renk dolgusu, Gerçek senkronizasyon vb.) tek tek test edilecek.
- Tablette hem dikey hem yatay modda Master-Detail akışı kontrol edilecek.
