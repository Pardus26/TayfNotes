# TayfNotes Hibrit Geliştirme Yol Haritası (Android Öncelikli)

Bu yol haritası, uygulamanın Android APK üretimini bozmadan, gelecekteki çoklu platform desteğine zemin hazırlayarak ilerlemeyi hedefler.

## [x] Faz 2: Veri ve Mimari Temelleri (Gelecek Odaklı)
- [x] `shared` modülünün (KMP) taslağını oluşturma.
- [x] GitHub Actions: Kaynak kod yedekleme sisteminin kurulması.
- [x] Premium İkon Tasarımı ve Uygulanması.
- [x] Hibrit Not veri modelinin tanımlanması.
- [x] Yerel veritabanı (Room) kurulumu.
- [x] Repository katmanının oluşturulması.

## [x] Faz 3: Hibrit Arayüz (Android/Compose)
- [x] Material 3 ve "Dinamik Renk" teması kurulumu.
- [x] Ana Ekran: ColorNote stili renkli ızgara (Grid).
- [x] Not Düzenleyici: Basit metin ve liste geçişi.

## [x] Faz 4: Arama ve Silme Özellikleri
- [x] `NoteDao` ve `NoteRepository` arama fonksiyonları.
- [x] `NoteViewModel` arama state yönetimi.
- [x] `MainScreen` arama çubuğu ve `NoteEditorScreen` silme butonu.

## [x] Faz 5: Premium UI ve Hibrit Özellikler (İmajlara Göre)
- [x] Veri modellerinin klasör ve otomatik kayıt için güncellenmesi.
- [x] `BottomNavigationBar` ve `AddNoteDialog` bileşenleri.
- [x] `FoldersScreen`, `CalendarScreen`, `MoreScreen` ve `SettingsScreen` arayüzleri.
- [x] `NoteEditorScreen` üzerinde "Otomatik Kayıt" mantığı.

## [x] Faz 6: Markdown ve Çevrimiçi Yedekleme
- [x] Markdown (rendering) ve Bulut Senkronizasyon (Ktor) bağımlılıkları.
- [x] `NoteEditorScreen` Markdown önizleme.
- [x] `SyncManager` temel altyapısı.

## [x] Faz 7: Takvim ve Hatırlatıcı Sistemi
- [x] `reminderTimestamp` alanının entegrasyonu.
- [x] `NotificationHelper` ve `ReminderReceiver` kodlanması.
- [x] `CalendarScreen` aylık listeleme.

## [/] Faz 8: Gelişmiş Özelleştirme ve Akıllı Mantık
- [/] Klasör ismi sorma ve düzenleme mantığı.
- [ ] Akıllı başlık üretme (ilk 5 kelime).
- [ ] 20+ Not rengi ve Hamburger menü seçici.
- [ ] 10 Farklı Tema ve Karanlık Mod altyapısı.
- [ ] Notu .txt olarak dışa aktarma.
- [ ] Ana ekran ve Ayarlar başlıklarının (Tayfun YAMAK©) güncellenmesi.
- [ ] Biyometrik Kilit (Parmak İzi/Yüz Tanıma) entegrasyonu.

## [ ] Faz 9: Çoklu Platform Genişlemesi
- [ ] iOS, Masaüstü ve Web hedeflerinin eklenmesi.
