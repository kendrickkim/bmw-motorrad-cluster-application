# BMW Motorrad Cluster — Android 애플리케이션

[English](README.md)

ESP32에서 BLE로 받은 차량 정보를 표시하는 **스마트폰 클러스터** 앱입니다. 속도 / RPM / 기어 / 전압 등을 보여 주며, 가로(reverseLandscape) 홈·런처로도 쓸 수 있고 Android Auto 리시버(예: Headunit Reloaded)와 함께 쓰는 구성을 염두에 둡니다.

| 저장소 | 역할 |
|---|---|
| [bmw-motorrad-cluster-linbus](https://github.com/kendrickkim/bmw-motorrad-cluster-linbus) | LIN 디코드 문서 |
| [bmw-motorrad-cluster-esp32](https://github.com/kendrickkim/bmw-motorrad-cluster-esp32) | ESP32 LIN → BLE / HID 펌웨어 |
| **이 저장소** (`bmw-motorrad-cluster-application`) | Android 클러스터 UI + BLE 클라이언트 |

## 기능

- Compose 기반 **가로 클러스터 UI**
- 포그라운드 **`BleService`**: `BMW-WWC-*` 연결, car-status 파싱, UI 브로드캐스트
- 위치 저장되는 **오버레이** (속도/기어 등)
- **휠 RPM → 속도** 환산 계수 설정 (SharedPreferences)
- Wonder Wheel 경로용 **`VirtualKeyService`** (접근성)
- **HOME / 런처**로 등록 가능 (`SplashActivity`)
- 페어링 BLE 장치명·주소 설정
- Headunit Reloaded (`com.andrerinas.headunitrevived`) 연동 대상 포함

## 구조

```text
ESP32 BLE notify (0x180A / 0x2A01)
        │
        ▼
   BleService (포그라운드)
        │  CAR_STATUS_CHANGED 브로드캐스트
        ▼
   MainActivity / MainScreen (Compose)
        │
        ├─ 속도, RPM, 기어, 전압, 이그니션, …
        └─ 오버레이 (선택)

Wonder Wheel / HID / 가상 키
        │
        ▼
   VirtualKeyService (접근성)
```

## 요구 사항

| 항목 | 설명 |
|---|---|
| Android | **minSdk 29**, target / compileSdk **35** |
| 언어 | Kotlin, Java 17 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| BLE | ESP32 `BMW-WWC-…` 광고 |
| 권한 | Bluetooth, 위치(레거시 스캔), 알림, 오버레이, 접근성 등 |

## 빌드

Gradle 루트는 저장소 최상위가 아니라 **`src/`** 입니다.

```bash
cd bmw-motorrad-cluster-application/src
./gradlew :app:assembleDebug
# Windows:
gradlew.bat :app:assembleDebug
```

- Application ID: `com.dooderge.bmw.motorrad.cluster`
- Android Studio에서는 `src/` 폴더를 열어 주세요.

## 최초 설정

1. ESP32를 켜 `BMW-WWC-…`가 광고되게 합니다.
2. 앱 설치 후 Bluetooth / 알림 / 오버레이 권한을 허용합니다.
3. 설정에서 ESP32 장치명·주소를 저장합니다.
4. (선택) **접근성 → BMW Motorrad Cluster** 활성화.
5. (선택) 기본 홈 앱으로 지정.
6. 표시 속도가 다르면 **휠 RPM → 속도** 계수를 조정합니다 (코드 기본값 약 `0.884`).

## BLE car-status 파싱

- Service: `0000180a-0000-1000-8000-00805f9b34fb`
- Characteristic: `00002a01-0000-1000-8000-00805f9b34fb`

ESP32 `buildData()` 기준 주요 오프셋:

| Offset | 필드 |
|:------:|------|
| 0 | 버튼 |
| 2 | Wonder Wheel |
| 3 | 기어 |
| 4 | 전압 raw |
| 5–6 | 휠 RPM (LE) |
| 7–8 | 엔진 RPM (LE) |
| 9 | 이그니션 |

```text
speed_kmh ≈ wheel_rpm × WHEEL_RPM_TO_SPEED
```

전압 UI는 보통 `raw / 10`으로 표시합니다. 패킷을 바꾸면 펌웨어와 앱 오프셋을 함께 맞추세요.

## 저장소 구성

```text
bmw-motorrad-cluster-application/
├── README.md
├── README-kr.md
└── src/                          # Gradle 루트
    └── app/src/main/java/.../
        ├── MainActivity.kt
        ├── BleService.kt
        ├── CarStatus.kt
        ├── VirtualKeyService.kt
        ├── launcher/
        └── ui/
```

## 관련 문서

- LIN 분석: [LIN_ANALYSIS.md](https://github.com/kendrickkim/bmw-motorrad-cluster-linbus/blob/main/lin_analysis/LIN_ANALYSIS.md)
- 펌웨어: [bmw-motorrad-cluster-esp32](https://github.com/kendrickkim/bmw-motorrad-cluster-esp32)

## 참고

- 주행 중에는 안전이 우선입니다. 현지 법규를 지키세요.
- 오버레이·접근성 서비스는 사용자가 명시적으로 허용해야 합니다.
