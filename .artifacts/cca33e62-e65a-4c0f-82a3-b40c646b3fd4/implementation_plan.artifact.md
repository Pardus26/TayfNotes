# TayfNotes: Tam Hibrit Özellikler ve İleri Seviye UI Uygulama Planı

Bu plan, TayfNotes'u gönderilen ekran görüntülerine sadık kalarak premium bir arayüze kavuşturmayı, otomatik kayıt, klasörleme ve gelişmiş ayarlar sistemini entegre etmeyi hedefler.

## User Review Required

> [!IMPORTANT]
> **APK Çakışma Çözümü:** Paket ismini `com.eldora25.tayfnotes` olarak sabitledik. Eğer cihazınızda daha önce `com.example.tayfnotes` yüklüyse bir defaya mahsus silmeniz gerekebilir. Bundan sonraki tüm `v01.x` sürümleri birbirinin üzerine yüklenebilecektir.

## Proposed Changes

### 1. Veri Modeli ve Veritabanı Genişletmesi
- [NEW] `FolderEntity.kt`: Klasörlerin (ID, İsim, Renk, Not Sayısı) saklanacağı tablo.
- [MODIFY] `NoteEntity.kt`: Notlara `folderId` ve `lastModified` alanlarının eklenmesi.
- [MODIFY] `AppDatabase.kt`: Klasör DAO'sunun eklenmesi ve versiyon artırımı.

### 2. UI Geliştirme (İmajlara Sadık Kalınarak)
- [NEW] `ui/components/BottomNavigationBar.kt`: Notlar, Klasörler, Takvim, Arama, Diğer.
- [NEW] `ui/components/AddNoteDialog.kt`: Metin ve Kontrol Listesi seçim popup'ı.
- [NEW] `ui/FoldersScreen.kt`: İmaj 8'deki gibi klasör listesi.
- [NEW] `ui/SettingsScreen.kt`: İmaj 1-5 arası gösterilen detaylı ayarlar kategorileri.
- [NEW] `ui/MoreScreen.kt`: İmaj 10'daki profil ve hızlı erişim menüsü.
- [NEW] `ui/CalendarScreen.kt`: İmaj 9'daki takvim görünümü taslağı.

### 3. Fonksiyonel Geliştirmeler
- [MODIFY] `NoteEditorScreen.kt`:
    - **Otomatik Kayıt:** Her harf değişiminde veya `onDispose` anında veritabanına yazma.
    - **Kaydet Butonu Kaldırma:** Kullanıcı deneyimini ColorNote'a benzetme.
- [MODIFY] `NoteViewModel.kt`: Klasör bazlı filtreleme ve ayarlar state yönetimi.

### 4. GitHub ve APK Otomasyonu
- Mevcut zırhlı yedekleme ve isimlendirme (`TayfNotes_v01.x.apk`) yapısı korunacaktır.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile başarılı derleme.
- APK isminin `TayfNotes_v01.14.apk` (veya sıradaki numara) olduğu doğrulanacak.

### Manual Verification
- Alt menüdeki ikonlar arası geçiş kontrol edilecek.
- Yeni bir not yazılırken uygulamadan çıkılıp girildiğinde verinin korunduğu teyit edilecek.
- Ayarlar menüsündeki listelerin imajlarla birebir uyumu kontrol edilecek.
