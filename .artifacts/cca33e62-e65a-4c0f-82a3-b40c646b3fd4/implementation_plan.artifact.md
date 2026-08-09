# TayfNotes: Yerel Veritabanı (Room) Entegrasyonu Planı

Bu plan, uygulamadaki notların telefon hafızasında kalıcı olarak saklanmasını sağlayacak Room Veritabanı mimarisinin kurulumunu ve UI entegrasyonunu kapsar.

## User Review Required

> [!NOTE]
> Verileri saklamak için Android'in standart ve en güvenli kütüphanesi olan **Room Persistence Library** kullanılacaktır. İleride Multi-platform (KMP) geçişi yapıldığında, bu katman kolayca SQLDelight veya Room-KMP'ye taşınabilecek şekilde soyutlanacaktır.

## Proposed Changes

### 1. Bağımlılıklar (Room)
- [MODIFY] `app/build.gradle.kts`: Room (Compiler, Runtime, KTX) bağımlılıklarının eklenmesi.

### 2. Veri Katmanı (Data Layer)
- [NEW] `data/entity/NoteEntity.kt`: Veritabanı tablo yapısı.
- [NEW] `data/dao/NoteDao.kt`: Veritabanı sorguları (Ekle, Sil, Güncelle, Listele).
- [NEW] `data/database/AppDatabase.kt`: Room veritabanı tanımı.
- [NEW] `data/repository/NoteRepository.kt`: UI ve Veritabanı arasındaki köprü.

### 3. İş Mantığı (ViewModel)
- [NEW] `ui/viewmodel/NoteViewModel.kt`: Veritabanı işlemlerini yöneten ve UI'a "State" sağlayan katman.

### 4. UI Entegrasyonu
- [MODIFY] `ui/MainScreen.kt`: Gerçek veritabanından notları çekme ve görüntüleme.
- [MODIFY] `ui/NoteEditorScreen.kt`: "Kaydet" butonu tıklandığında veriyi kalıcı olarak kaydetme.
- [MODIFY] `MainActivity.kt`: ViewModel'in başlatılması ve ekrana bağlanması.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile başarılı derleme.
- APK isminin `TayfNotes_v01.11.apk` (veya sıradaki numara) olduğu doğrulanacak.

### Manual Verification
- Bir not oluşturup "Kaydet" dedikten sonra uygulama kapatılıp açıldığında notun hala listede olduğu görülecek.
- Notun rengi değiştirilip kaydedildiğinde, ana ekranda yeni rengiyle göründüğü teyit edilecek.
- GitHub Actions üzerinde `sourcecodes_*.zip` dosyasının oluştuğu ve Room kodlarını içerdiği kontrol edilecek.
