# TayfNotes: Çevrimiçi Yedekleme ve Markdown Desteği Planı

Bu plan, TayfNotes uygulamasına Evernote benzeri zengin içerik desteği (Markdown) ve ColorNote benzeri güvenli bulut yedekleme altyapısının entegrasyonunu kapsar.

## User Review Required

> [!IMPORTANT]
> **Çevrimiçi Yedekleme:** İlk aşamada Firebase Authentication ve Firestore kullanarak "Tayfun Yamak" adına bir hesap yapısı ve veri senkronizasyonu kuracağız.
> **Markdown Desteği:** Not editörüne, yazılan metni anında zengin metne dönüştüren bir önizleme modu ekleyeceğiz.

## Proposed Changes

### 1. `:shared` Modülü (KMP Hazırlığı)
- [MODIFY] `shared/build.gradle.kts`: Multiplatform Ktor (Network) ve Firebase (açık kaynak alternatifleri) için zemin hazırlığı.

### 2. Markdown Entegrasyonu
- [MODIFY] `app/build.gradle.kts`: Markdown rendering kütüphanesi (Örn: `multiplatform-markdown` veya Android-specific rendering) eklenmesi.
- [MODIFY] `ui/NoteEditorScreen.kt`: Markdown önizleme butonu ve render motorunun entegrasyonu.

### 3. Çevrimiçi Yedekleme Arka Planı
- [NEW] `data/sync/SyncManager.kt`: Yerel veritabanı ile bulut arasındaki farkları yöneten senkronizasyon mantığı.
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`: "Sync" (Senkronizasyon) tetikleyicisi eklenmesi.

### 4. UI Güncellemesi
- [MODIFY] `ui/SettingsScreen.kt`: İmaj 1'deki "Çevrimiçi yedekleme" butonunun işlevsel hale getirilmesi.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile başarılı derleme.
- APK isminin `TayfNotes_v01.16.apk` olduğu doğrulanacak.

### Manual Verification
- Not içerisine `# Başlık` veya `**kalın**` yazıldığında Markdown önizlemesinin doğruluğu.
- Ayarlar menüsünden "Senkronizasyon" butonuna basıldığında (şimdilik log bazlı) yedekleme sürecinin başlaması.
- GitHub Actions üzerinde kaynak kod yedeği (`sourcecodes_*.zip`) kontrol edilecek.
