# TayfNotes

TayfNotes is a powerful, multi-platform note-taking application inspired by Evernote and ColorNote.

## Features (Planned)
- Cross-platform support: Android, iOS, PC (Windows, Linux, macOS), and Web.
- Rich text editing and color-coded notes.
- Synchronization across devices.
- Advanced organization and search.

## Technology Stack
- **Kotlin Multiplatform (KMP)**
- **Compose Multiplatform**
- **GitHub Actions** for CI/CD

## Build and Versioning
The project uses an automatic versioning system:
- **Local Builds**: Versions are tracked in `version.properties` and increment automatically on each successful debug build.
- **GitHub Actions**: Versions use the GitHub Run Number for consistency.
- **APK Naming**: APKs are generated as `TayfNotes_v01.<build_no>.apk`.

To build the Android APK locally:
```bash
./gradlew :app:assembleDebug
```
