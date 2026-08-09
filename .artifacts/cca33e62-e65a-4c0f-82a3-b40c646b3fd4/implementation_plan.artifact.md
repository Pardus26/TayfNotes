# TayfNotes: Takvim Görünümü ve Hatırlatıcı Sistemi Planı

Bu plan, uygulamaya notlar için tarih bazlı takip (Takvim) ve zamanlanmış uyarılar (Hatırlatıcı/Bildirim) özelliklerinin eklenmesini kapsar.

## User Review Required

> [!IMPORTANT]
> **Hatırlatıcılar:** Android 13+ cihazlarda bildirim gönderebilmek için kullanıcıdan izin istenecektir. Hatırlatıcılar `AlarmManager` kullanılarak tam zamanında tetiklenecek şekilde ayarlanacaktır.

## Proposed Changes

### 1. Veri Modeli Güncellemesi
- [MODIFY] `shared/src/commonMain/.../Note.kt`: `reminderTimestamp` alanının eklenmesi.
- [MODIFY] `app/src/main/java/.../NoteEntity.kt`: Veritabanına hatırlatıcı zamanı sütununun eklenmesi.

### 2. Hatırlatıcı ve Bildirim Sistemi
- [NEW] `util/NotificationHelper.kt`: Bildirim kanalları ve bildirim oluşturma mantığı.
- [NEW] `receiver/ReminderReceiver.kt`: Alarm vakti geldiğinde bildirimi tetikleyen BroadcastReceiver.

### 3. UI Geliştirme (Takvim ve Editör)
- [MODIFY] `ui/CalendarScreen.kt`: Aylık görünümde notların tarihlere göre listelenmesi.
- [MODIFY] `ui/NoteEditorScreen.kt`: Not içerisinden tarih ve saat seçerek hatırlatıcı kurma butonu.

### 4. Otomasyon ve Zırhlı Yapı
- APK `TayfNotes_v01.x.apk` ve GitHub kaynak kod yedekleme sistemi korunacaktır.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile başarılı derleme.
- APK isminin `TayfNotes_v01.17.apk` olduğu doğrulanacak.

### Manual Verification
- Bir nota hatırlatıcı kurulduğunda belirlenen saatte bildirim geldiği test edilecek.
- Takvim ekranında, hatırlatıcısı olan notların ilgili günlerde göründüğü teyit edilecek.
- GitHub Actions üzerinde kaynak kod yedeği kontrol edilecek.
