# BMW Motorrad Cluster — Android Application

[한국어](README-kr.md)

Jetpack Compose Android app that acts as a **phone-based Motorrad cluster**: receives vehicle telemetry from the ESP32 over BLE, shows speed / RPM / gear / voltage, and can work as a landscape home/launcher alongside Android Auto (e.g. Headunit Reloaded).

| Repository | Role |
|---|---|
| [bmw-motorrad-cluster-linbus](https://github.com/kendrickkim/bmw-motorrad-cluster-linbus) | LIN decode documentation |
| [bmw-motorrad-cluster-esp32](https://github.com/kendrickkim/bmw-motorrad-cluster-esp32) | ESP32 LIN → BLE / HID firmware |
| **This repo** (`bmw-motorrad-cluster-application`) | Android cluster UI + BLE client |

## Features

- Landscape **reverseLandscape** cluster UI (Compose)
- Foreground **`BleService`**: connect to `BMW-WWC-*`, decode raw LIN frames, broadcast UI updates
- Optional floating **overlay** (speed / gear) with saved position
- Configurable **wheel-RPM → speed** factor (SharedPreferences)
- **`VirtualKeyService`** (Accessibility) for virtual key handling from Wonder Wheel paths
- Can register as **HOME / launcher** (`SplashActivity`)
- Settings for paired BLE device name / address
- Optional launch target for Headunit Reloaded (`com.andrerinas.headunitrevived`)

## Architecture

```text
ESP32 BLE notify (0x180A / 0x2A01)
        │
        ▼
   BleService (foreground)
        │  broadcasts CAR_STATUS_CHANGED
        ▼
   MainActivity / MainScreen (Compose)
        │
        ├─ speed, RPM, gear, voltage, ignition, …
        └─ overlay window (optional)

Wonder Wheel / HID / virtual keys
        │
        ▼
   VirtualKeyService (Accessibility)
```

## Requirements

| Item | Notes |
|---|---|
| Android | **minSdk 29**, target / compileSdk **35** |
| Language | Kotlin, Java 17 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| BLE device | ESP32 firmware advertising `BMW-WWC-…` |
| Permissions | Bluetooth (scan/connect), location (legacy scan), notifications, overlay, accessibility (as needed) |

## Build

Gradle project root is the **`src/`** directory (not the git repo root).

```bash
cd bmw-motorrad-cluster-application/src
./gradlew :app:assembleDebug
# Windows:
gradlew.bat :app:assembleDebug
```

- Application ID: `com.dooderge.bmw.motorrad.cluster`
- Module: `:app`

Open `src/` in Android Studio for the usual IDE workflow.

## First-run setup

1. Flash and power the ESP32 so it advertises `BMW-WWC-…`.
2. Install the debug APK; grant Bluetooth / notification / overlay permissions when prompted.
3. In settings, select or save the ESP32 device name and address.
4. (Optional) Enable **Accessibility → BMW Motorrad Cluster** for `VirtualKeyService`.
5. (Optional) Set as default home app if you want launcher behavior.
6. Tune **wheel RPM → speed** factor if indicated speed does not match the bike (default in code ~`0.884`; also stored in preferences).

## BLE LIN frame parsing

The app listens for notifications on:

- Service UUID: `0000180a-0000-1000-8000-00805f9b34fb`
- Characteristic UUID: `00002a01-0000-1000-8000-00805f9b34fb`

On the bike the ESP32 sends one raw frame per notify (`LinFrame`, 7 bytes). `LinFrame.kt` decodes it with `lin_analysis`. A 12-byte-or-longer packet is still the bench packed car-status blob.

| Offset | Field |
|:------:|-------|
| 0 | Message ID |
| 1 | Data-0 |
| 2 | Data-1 |
| 3 | Data-2 |
| 4 | Data-3 |
| 5 | Data-4 |
| 6 | Data-5 |

| ID | Fields written into `CarStatus` |
|:--:|---------------------------------|
| `0x14` | Handle bitmaps (horn, hazard, ABS, ESA, cruise, high beam, trip, info), Wonder Wheel in/out and rotation direction |
| `0x20` | Ignition, speed, fuel range, ambient light |
| `0x2B` | Odometer when Data-1 is `0x25` |
| `0xE2` | RPM, gear, throttle, coolant temperature, ASC, oil |
| `0xE9` | Average speed, secondary speed, voltage raw (`Data-3`; UI shows it as `/ 10` volts), fuel fault, general fault |

Speed from ID `0x20` uses the bike factor in `lin_analysis`:

```text
speed_kmh ≈ rear_wheel_rpm × 0.14
```

## Repository layout

```text
bmw-motorrad-cluster-application/
├── README.md
├── README-kr.md
└── src/                          # Gradle root
    ├── settings.gradle.kts
    ├── build.gradle.kts
    └── app/
        └── src/main/
            ├── AndroidManifest.xml
            └── java/com/dooderge/bmw/motorrad/cluster/
                ├── MainActivity.kt
                ├── SplashActivity.kt
                ├── SettingActivity.kt
                ├── BleService.kt
                ├── CarStatus.kt
                ├── VirtualKeyService.kt
                ├── SettingPreference.kt
                ├── launcher/
                ├── viewmodel/
                └── ui/           # Compose screens & cluster widgets
```

## Related docs

- LIN decode: [LIN_ANALYSIS.md](https://github.com/kendrickkim/bmw-motorrad-cluster-linbus/blob/main/lin_analysis/LIN_ANALYSIS.md)
- Firmware: [bmw-motorrad-cluster-esp32](https://github.com/kendrickkim/bmw-motorrad-cluster-esp32)

## Notes

- Designed for use on the bike with a phone mount / Android Auto receiver — ride safely and obey local laws.
- Overlay and accessibility services need explicit user consent on Android.
