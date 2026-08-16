# SEG NFC - Android NFC Reader/Writer

> 🦀 Powered by Rust + Kotlin · Material 3 UI

## Features

- **📖 Read NFC Tags** - NDEF messages, MIFARE Classic/Ultralight, tag metadata
- **💳 Read Bank Cards** - EMV/TLV data extraction (Visa, Mastercard, JCB, AMEX, Discover)
- **✏️ Write Tags** - Write Text and URI NDEF records
- **🔍 Tag Analysis** - Identify tag type, UID, capacity, writable status
- **📱 NFC Check** - Auto-detect NFC hardware availability and status

## Architecture

```
┌──────────────────────────────┐
│   Kotlin + Jetpack Compose   │  ← Material 3 UI
│   (NFC lifecycle, UI)        │
├──────────────────────────────┤
│        JNI Bridge            │  ← RustBridge.kt
├──────────────────────────────┤
│     Rust Core Library        │  ← All data processing
│  • NDEF Parser               │
│  • EMV/TLV Parser            │
│  • Tag Analyzer              │
│  • NDEF Writer               │
└──────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Core Logic | Rust (JNI) |
| NFC | Android NFC API + Rust parsing |
| Build | Gradle + cargo-ndk |
| CI/CD | GitHub Actions |

## Build

### Prerequisites
- Android SDK (API 34)
- Android NDK (r26)
- Rust toolchain + cargo-ndk
- JDK 17

### Local Build
```bash
# Install Rust Android targets
rustup target add aarch64-linux-android armv7-linux-androideabi

# Install cargo-ndk
cargo install cargo-ndk

# Build
./gradlew assembleDebug
```

### CI Build
Push to `master` or `main` branch triggers automatic APK build via GitHub Actions.
Download artifacts from the Actions tab.

## EMV Support

| Card Brand | AID |
|-----------|-----|
| Visa | A0000000031010 |
| Mastercard | A0000000041010 |
| JCB | A0000000651010 |
| AMEX | A000000025010104 |
| Discover | A0000001523010 |

## License

MIT
