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
- [/] Faz 2: Veri Katmanı (Room) Entegrasyonu
    - [/] Room bağımlılıklarının eklenmesi.
    - [ ] `NoteEntity`, `NoteDao` ve `AppDatabase` oluşturulması.
    - [ ] `NoteRepository` ve `NoteViewModel` kodlanması.
    - [ ] UI'ın ViewModel'e bağlanması.

## [ ] Faz 4: Evernote Yetenekleri
- [ ] Markdown desteği (Zengin metin için).
- [ ] Notlara etiket ve kategori (Notebook) ekleme.
- [ ] Arama motoru (Metin içi ve etiket bazlı).

## [ ] Faz 5: Güvenlik ve Modern Özellikler
- [ ] Biyometrik kilit entegrasyonu.
- [ ] Hatırlatıcılar ve Takvim görünümü.
- [ ] Widget desteği (Sticky Notes).

## [ ] Faz 6: Çoklu Platform Genişlemesi
- [ ] iOS hedefinin eklenmesi.
- [ ] Masaüstü (Windows/Linux) hedefinin eklenmesi.
- [ ] Web (Compose Wasm) hedefinin eklenmesi.
