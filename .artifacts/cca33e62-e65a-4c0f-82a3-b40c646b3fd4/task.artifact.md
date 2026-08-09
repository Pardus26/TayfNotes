# TayfNotes Hibrit Geliştirme Yol Haritası (Android Öncelikli)

Bu yol haritası, uygulamanın Android APK üretimini bozmadan, gelecekteki çoklu platform desteğine zemin hazırlayarak ilerlemeyi hedefler.

## [/] Faz 2: Veri ve Mimari Temelleri (Gelecek Odaklı)
- [x] `shared` modülünün (KMP) taslağını oluşturma (İş mantığı ve Veri modelleri buraya taşınacak).
- [x] GitHub Actions: Kaynak kod yedekleme sisteminin kurulması (dosyaadı.uzantisi-buildno_commitno).
- [x] Premium İkon Tasarımı ve Uygulanması.
    - [x] `colors.xml` premium renklerin eklenmesi.
    - [x] `ic_launcher_background.xml` modern ızgara tasarımı.
    - [x] `ic_launcher_foreground.xml` Tayf/Spectrum logolu "T" tasarımı.
- [ ] Hibrit Not veri modelinin tanımlanması (Renk, Metin, Liste, Etiketler).
- [ ] Yerel veritabanı (Room veya SQLDelight) kurulumu.
- [ ] Repository katmanının oluşturulması.

## [/] Faz 3: Hibrit Arayüz (Android/Compose)
- [x] `shared` modülünde örnek veri sağlayıcı (`MockNoteProvider.kt`) oluşturulması.
- [x] Material 3 ve Premium Renklerin temaya entegrasyonu.
- [x] Not Kartı (`NoteGridItem.kt`) bileşeninin kodlanması.
- [x] Ana Ekran (`MainScreen.kt`) ızgara görünümünün oluşturulması.
- [x] Not Editörü (`NoteEditorScreen.kt`) ve Renk Seçici (`ColorSelector.kt`) kodlanması.
- [x] Navigasyon mantığının (Ana Ekran <-> Editör) kurulması.
- [x] Faz 2: Veri Katmanı (Room) Entegrasyonu
    - [x] Room bağımlılıklarının eklenmesi.
    - [x] `NoteEntity`, `NoteDao` ve `AppDatabase` oluşturulması.
    - [x] `NoteRepository` ve `NoteViewModel` kodlanması.
    - [x] UI'ın ViewModel'e bağlanması.
- [x] Faz 4: Arama ve Silme Özellikleri
    - [x] `NoteDao` ve `NoteRepository` arama fonksiyonlarının eklenmesi.
    - [x] `NoteViewModel` üzerinde arama state'inin yönetilmesi.
    - [x] `MainScreen` arama çubuğu entegrasyonu.
    - [x] `NoteEditorScreen` silme butonu entegrasyonu.

## [ ] Faz 4: Evernote Yetenekleri
- [ ] Markdown desteği (Zengin metin için).
- [ ] Notlara etiket ve kategori (Notebook) ekleme.
- [ ] Arama motoru (Metin içi ve etiket bazlı).

## [/] Faz 5: Premium UI ve Hibrit Özellikler (İmajlara Göre)
- [/] Veri modellerinin klasör ve otomatik kayıt için güncellenmesi.
- [ ] `BottomNavigationBar` ve `AddNoteDialog` bileşenlerinin kodlanması.
- [ ] `FoldersScreen`, `CalendarScreen`, `MoreScreen` ve `SettingsScreen` arayüzlerinin oluşturulması.
- [ ] `NoteEditorScreen` üzerinde "Otomatik Kayıt" mantığının kurulması.
- [ ] Projenin "zırhlı" derleme başarısının ve APK güncelleme uyumunun doğrulanması.

## [ ] Faz 6: Çoklu Platform Genişlemesi
- [ ] iOS hedefinin eklenmesi.
- [ ] Masaüstü (Windows/Linux) hedefinin eklenmesi.
- [ ] Web (Compose Wasm) hedefinin eklenmesi.
