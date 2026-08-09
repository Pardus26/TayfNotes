# TayfNotes: Görsel Temalar ve Biyometrik Güvenlik Planı

Bu plan, uygulamaya 10 farklı renk paleti, karanlık mod desteği ve biyometrik güvenlik (parmak izi/yüz tanıma) özelliklerinin eklenmesini kapsar.

## User Review Required

> [!IMPORTANT]
> **Biyometrik Güvenlik:** Kullanıcının cihazında parmak izi veya yüz tanıma kayıtlı olmalıdır. Uygulama açılışında veya kilitli notlara erişimde bu katman devreye girecektir.
> **Tema Sistemi:** Seçilen tema tüm uygulamaya (arka planlar, butonlar, vurgular) anında uygulanacaktır.

## Proposed Changes

### 1. Görsel Tema Sistemi (10+ Renk Paleti)
- [MODIFY] `ui/theme/Color.kt`: 10 farklı tema için (Örn: Ocean, Sunset, Forest, Lavender, Gold, vb.) renk tanımlamaları.
- [MODIFY] `ui/theme/Theme.kt`: Dinamik tema seçimini destekleyen merkezi tema motoru.
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`: Seçilen tema ve karanlık mod tercihini saklayan `DataStore` entegrasyonu (başlangıçta ViewModel state).

### 2. Biyometrik Kilit Entegrasyonu
- [MODIFY] `app/build.gradle.kts`: `androidx.biometric:biometric` kütüphanesinin eklenmesi.
- [NEW] `util/BiometricHelper.kt`: Parmak izi/yüz tanıma doğrulama mantığı.
- [MODIFY] `MainActivity.kt`: Uygulama açılışında (opsiyonel) veya kilitli notlarda biyometrik kontrol.

### 3. UI Entegrasyonu
- [MODIFY] `ui/SettingsScreen.kt`:
    - İmaj 10'daki "Tema" butonunun işlevsel hale getirilmesi.
    - Tema seçim diyaloğu (10 seçenekli görsel liste).
    - Güvenlik kategorisinde "Biyometrik Kilidi Etkinleştir" seçeneği.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile build başarısı.
- APK isminin `TayfNotes_v01.19.apk` olduğu doğrulanacak.

### Manual Verification
- Ayarlar -> Tema kısmından farklı renk paletleri seçilecek ve arayüzün değiştiği görülecek.
- Karanlık mod butonu test edilecek.
- Biyometrik kilit aktifken notların açılışında doğrulama isteneceği teyit edilecek.
- GitHub Actions yedekleme zip dosyası kontrol edilecek.
