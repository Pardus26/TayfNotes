# TayfNotes: Gelişmiş Klasörleme, Akıllı Başlık ve İleri Düzey Özelleştirme Planı

Bu plan, uygulamanın marka kimliğini güçlendirmeyi, klasörleme ve not yönetimi mantığını daha profesyonel bir seviyeye taşımayı hedefler.

## User Review Required

> [!IMPORTANT]
> **İsimlendirme:** Ana ekran başlığı "TayfNotes buildv01.x Tayfun YAMAK©" olarak güncellenecektir.
> **Dışa Aktarma:** Notlar metin dosyası (.txt) olarak cihaza kaydedilebilecek şekilde "Export" özelliği eklenecektir.

## Proposed Changes

### 1. Akıllı Not Mantığı ve Klasörleme
- [MODIFY] `ui/NoteEditorScreen.kt`:
    - Başlık boş bırakılırsa, içeriğin ilk 5 kelimesinden otomatik başlık üretme.
    - Klasör seçimi için bir açılır menü (Dropdown) eklenmesi.
- [MODIFY] `ui/FoldersScreen.kt`:
    - Klasör oluştururken isim soran bir diyalog (Dialog) eklenmesi.
    - Mevcut klasör adını düzenleme (Rename) özelliği.

### 2. Görsel Özelleştirme (Premium Palet)
- [MODIFY] `ui/components/ColorSelector.kt`:
    - Renk sayısının 20+ adet profesyonel seçeneğe çıkarılması.
    - Bir hamburger/ızgara menüsü üzerinden seçim yapılması.
    - Son seçilen rengin varsayılan (Default) olarak hatırlanması.
- [MODIFY] `ui/theme/Theme.kt`: Karanlık mod desteği ve 10 farklı renk paleti teması taslağının oluşturulması.

### 3. Marka Kimliği ve Dışa Aktarma
- [MODIFY] `ui/MainScreen.kt`: Başlık alanına versiyon ve telif bilgisinin eklenmesi.
- [MODIFY] `ui/SettingsScreen.kt`: Versiyon ve yazar bilgisinin güncellenmesi.
- [NEW] `util/FileExportHelper.kt`: Seçilen notu `.txt` olarak dışarı aktarma mantığı.

### 4. Güvenlik ve İleri Seviye (Faz 8.5)
- [NEW] `util/BiometricHelper.kt`: Parmak izi ve yüz tanıma entegrasyonu hazırlığı.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile derleme kontrolü.
- APK isminin `TayfNotes_v01.18.apk` olduğu doğrulanacak.

### Manual Verification
- Başlıksız not yazıldığında ilk kelimelerin başlık olup olmadığı kontrol edilecek.
- Klasör ismi değiştirme ve notu klasöre taşıma işlemleri test edilecek.
- Ana ekranda "Tayfun YAMAK©" imzasının göründüğü teyit edilecek.
- GitHub Actions yedekleme kontrolü yapılacak.
