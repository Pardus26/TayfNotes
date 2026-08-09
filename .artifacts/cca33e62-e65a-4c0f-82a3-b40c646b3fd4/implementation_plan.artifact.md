# TayfNotes Hibrit Yapı Uygulama Planı

Bu plan, TayfNotes'un hibrit özelliklerini (Evernote + ColorNote) Android üzerinde hayata geçirirken, gelecekteki çoklu platform geçişini (KMP) kolaylaştıracak mimariyi kurmayı amaçlar.

## User Review Required
> [!IMPORTANT]
> Proje yapısını **Kotlin Multiplatform (KMP)** standartlarına göre organize edeceğiz. Bu, kodun büyük kısmının `:shared` modülünde olacağı, Android'in ise sadece bir "görünüm" (entry point) olacağı anlamına gelir.

## Proposed Changes

### 1. Proje Yapılandırması (KMP Hazırlığı)
- [MODIFY] `settings.gradle.kts`: `:shared` modülünü dahil etme.
- [NEW] `shared/build.gradle.kts`: KMP yapılandırması (Android, iOS, Desktop hedefleri).
- [NEW] `shared/src/commonMain`: İş mantığının (Use Cases, Models) bulunacağı ana dizin.

### 2. Veri Modeli (Hibrit)
- [NEW] `Note` Veri Sınıfı:
    - `id`, `title`, `content` (Metin/Markdown).
    - `color` (ColorNote stili renk kodu).
    - `type` (TEXT veya CHECKLIST).
    - `tags` (Liste), `notebookId` (Organizasyon).
    - `isLocked` (Güvenlik).

### 3. UI Temeli (Compose)
- [MODIFY] `MainActivity.kt`: KMP uyumlu bir `App()` composable fonksiyonuna yönlendirme.
- [NEW] Tema Sistemi: ColorNote renk paletini içeren Material 3 teması.

## Verification Plan
### Automated Tests
- `./gradlew :shared:test` (İş mantığı testleri).
- `./gradlew :app:assembleDebug` (Android APK üretimi).

### Manual Verification
- GitHub Actions üzerinde oluşan APK'nın indirilip Android cihazda test edilmesi.
- Yeni versiyon numarasının (`TayfNotes_v01.x.apk`) doğrulanması.
