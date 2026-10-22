# TPMS Advanced 1.6.1 BLE 분석

- **앱**: TPMS Advanced `1.6.1` (`com.masselis.tpmsadvanced`, versionCode `10601999`)
- **소스**: `app/TPMS+Advanced_1.6.1_APKPure.xapk` → base APK `com.masselis.tpmsadvanced.apk`
- **오픈소스**: [VincentMasselis/TPMS-advanced](https://github.com/VincentMasselis/TPMS-advanced) (Apache-2.0)
- **디컴파일**: `app/decompiled_tpms_advanced/`
- **대상 센서**: Sysgration / Pecham(외장) / Wicarlink(LYTPMS) 계열 BLE TPMS  
  (AliExpress 등에서 판매하는 Sysgration·Pecham 타입 센서. 공식 앱 대안으로 설계됨)

Zontes/EasyConn GATT 허브 방식과 달리, **GATT 연결 없이 BLE Advertise(스캔 결과)만으로** 압력·온도를 수신한다. Write 명령 채널이 없다.

---

## 1. 개요

| 항목 | 내용 |
|------|------|
| 통신 방식 | **Passive BLE Scan** (Advertise / Manufacturer Data) |
| GATT Connect / Notify / Write | **사용하지 않음** |
| 스캔 Service UUID | `0000fbb0-…` (Sysgration·Wicarlink), `000027a5-…` (Pecham) |
| 구현 (소스) | `BluetoothLeScannerImpl`, `RawSysgration`, `RawPecham`, `RawWicarlink` |
| 내부 압력 단위 | **kPa** (`Pressure`; 표시 시 BAR=`/100`, PSI=`/6.895`) |
| 차량 종류 | Car / Motorcycle / Trailer / 3-wheeler |

> develop 브랜치에는 `RawBekubeeKy` / `RawBekubeeTpms`(HRTPMS, deviceName `"KY"`)도 있으나, **본 APK 1.6.1 디컴파일 기준 스캔 필터·파서는 Sysgration / Pecham / Wicarlink 3종**이다.

---

## 2. 스캔 필터 / UUID

`BluetoothLeScannerImpl` / APK `ry.java`:

| 센서 | Advertise Service UUID | 비고 |
|------|------------------------|------|
| **Sysgration** | `0000fbb0-0000-1000-8000-00805f9b34fb` | Manufacturer Specific Data 파싱 |
| **Wicarlink** | 동일 `0000fbb0-…` | `advertiseFlags == 0x06` + CRC |
| **Pecham** | `000027a5-0000-1000-8000-00805f9b34fb` | deviceName `"BR"` + CRC |

스캔 설정:

- `MATCH_MODE_AGGRESSIVE`
- `MATCH_NUM_ONE_ADVERTISEMENT`
- High duty: `SCAN_MODE_LOW_LATENCY` / Normal: `SCAN_MODE_BALANCED`
- 30초 내 다수 스캔 방지용 **5초 쿨다운**

권한:

| API | 필요 권한 |
|-----|-----------|
| ≤ 28 | `ACCESS_COARSE_LOCATION` |
| 29–30 | `ACCESS_FINE_LOCATION` |
| ≥ 31 | `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` |

```text
[Sensor] --advertise--> [Phone BLE Scanner]
                              |
                              v
              RawPecham | RawWicarlink | RawSysgration
                              |
                              v
                    Tyre.Located / Tyre.Unlocated
                    (sensorId, pressure kPa, temp °C, battery, alarm)
```

---

## 3. 센서별 페이로드

### 3.1 Sysgration (`RawSysgration`) — Located

Manufacturer Specific Data (첫 entry), 주소 바이트 `EA CA` 검증.

| Offset | 내용 |
|--------|------|
| `[0]` | 휠 위치 |
| `[1..2]` | 고정 `0xEA 0xCA` (필터) |
| `[3..5]` | Sensor ID (3바이트 → LE int, 앞에 `0x00` 패딩) |
| `[6..9]` | Pressure raw LE int → **`raw / 1000` kPa** |
| `[10..13]` | Temperature raw LE int → **`raw / 100` °C** |
| `[14]` | Battery (UShort) |
| `[15]` | Alarm (`0x01` = pressure alarm) |

휠 위치 바이트:

| Hex | 위치 |
|-----|------|
| `0x80` | FRONT_LEFT |
| `0x81` | FRONT_RIGHT |
| `0x82` | REAR_LEFT |
| `0x83` | REAR_RIGHT |

### 3.2 Pecham (`RawPecham`) — Unlocated

- Service UUID `0x27a5`, 짧은 이름 `"BR"`
- 데이터는 `scanRecord.bytes[10..17)` (7바이트). `getManufacturerSpecificData()`는 앞 2바이트가 잘려 사용하지 않음
- CRC: advertise 바이트 끝 2바이트 vs 룩업 테이블(`auchCRCHi`/`auchCRCLo`)

| Offset (7바이트 data) | 내용 |
|------------------------|------|
| `[1]` | Battery (예: `27` → 2.7 V) |
| `[2]` | Temperature (°C) |
| `[3..4]` | Pressure BE uint16 → **`((raw - 145) / 10)` PSI** → 내부 kPa (`× 6.895`) |

- Sensor ID: **MAC address `hashCode()`** (페이로드에 ID 없음)
- Alarm: battery `< 26` (2.6 V)

참고 (커뮤니티/andi38): manufacturer 포맷 `SS BB TT PPPP CCCC` — 앱은 CRC·이름 필터 후 동일 계열 필드를 사용.

### 3.3 Wicarlink / LYTPMS (`RawWicarlink`) — Unlocated

- Service UUID `fbb0`, `advertiseFlags == 0x06`
- CRC16-XMODEM: bytes `[9..16)` + `[19..26)` 14바이트, expected = `((b16<<8)|b18) - b17`

| Offset (전체 advertise bytes) | 내용 |
|-------------------------------|------|
| `[11]` | Voltage → `raw*0.01 + 1.22` V; battery 표시용 `round(V*10)` |
| `[12]` (+ `[17]==1`이면 +256) | Pressure → **`raw * 3.144` kPa** |
| `[13]` | Temperature → **`raw - 55` °C** |
| `[23..25]` | Sensor ID (24-bit) |
| Alarm | voltage `≤ 2.1` V |

---

## 4. QR 코드 / 센서 바인딩

GATT STUDY_PAIR 없음. QR은 **앱 로컬 DB에 sensorId ↔ 휠 위치 바인딩**용.

`QrCodeSensorUseCase` 정규식:

| 형식 | 패턴 | 예 |
|------|------|-----|
| 4휠 | `([0-9A-Fa-f]{6})&` × 4 | `1A2B3C&2D4E5F&…` |
| 2휠 | `([0-9A-Fa-f]{6})&` × 2 | 모터사이클 등 |
| Wicarlink 단일 | `^([0-9A-Fa-f]{6})$` | **미지원** → 수동 바인딩 안내 |

ID 해석:

1. 6 hex → 3바이트 + 앞 `0x00` → **LE int** (Sysgration ID와 동일 인코딩)
2. 첫 hex 문자가 `1`–`4`이면 휠 위치 힌트 (FL/FR/RL/RR), 아니면 리스트 순서

Pecham은 advertise에 고정 ID가 없어 **MAC 기반 hash**로 매칭·수동 바인딩한다.

---

## 5. 앱 기능과의 대응

| 기능 | 구현 요지 |
|------|-----------|
| 실시간 압력/온도 | 스캔 → Raw* → `Tyre` Flow |
| 다중 차량 | SQLDelight DB (`Sensor.sq` 등) |
| 즐겨찾기/바인딩 | sensorId(또는 Pecham hash) ↔ Location |
| 알림 | 압력·온도·배터리 임계 (앱 설정) |
| 백그라운드 | `MonitorService` + foreground connected-device |
| Android Auto | `CarAppService` |
| 카메라 QR | ML Kit barcode + `QrCodeSensorUseCase` |

---

## 6. 주요 소스 위치

### 6.1 오픈소스 (권장 참조)

| 경로 | 역할 |
|------|------|
| `data/vehicle/.../BluetoothLeScannerImpl.kt` | 스캔·필터·파서 체인 |
| `.../RawSysgration.kt` | Sysgration manufacturer 파싱 |
| `.../RawPecham.kt` | Pecham `"BR"` + CRC |
| `.../RawWicarlink.kt` | Wicarlink CRC·압력 |
| `feature/qrcode/.../QrCodeSensorUseCase.kt` | QR 정규식·ID 변환 |

### 6.2 APK 디컴파일 (난독화)

| 클래스(대략) | toString / 역할 |
|--------------|-----------------|
| `ry` | UUID `fbb0` / `27a5`, 스캐너 |
| `af3` | `RawSysgration` |
| `ze3` | `RawPecham` |
| `bf3` | `RawWicarlink` |
| `st3` | 휠 enum (`0x80`–`0x83`) |
| `z83` | Pressure (kPa 내부) |

```text
app/decompiled_tpms_advanced/sources/defpackage/
```

---

## 7. 클러스터 연동 시 시사점

1. **연결(Connect) 불필요** — `BluetoothLeScanner`만으로 수신 가능.
2. Sysgration은 **위치·ID가 advertise에 포함** → 바인딩이 단순.
3. Pecham은 **ID=MAC hash**, 휠 위치는 앱 바인딩에만 존재.
4. 압력 내부 단위는 **kPa**. Pecham raw는 PSI 계열 후 `×6.895`.
5. Zontes형 `ffb0`/`ffe4`/`ffe9`/`0xFC` 프레임과는 **완전 별개 프로토콜**.
6. QR은 Sysgration식 `HHHHHH&HHHHHH&…` (6 hex × N). Wicarlink 단독 6 hex QR은 앱도 거부.

---

## 8. 요약

1. TPMS Advanced는 **Advertise-only** BLE TPMS 클라이언트다.
2. 핵심 UUID: **`fbb0`**(Sysgration/Wicarlink), **`27a5`**(Pecham).
3. Sysgration manufacturer: 위치·ID·압력(`/1000` kPa)·온도(`/100` °C)·배터리·알람.
4. Pecham: 이름 `"BR"`, 압력 `((u16-145)/10)` PSI → kPa, ID=MAC hash.
5. Wicarlink: `fbb0`+flags/`CRC`, 압력 `×3.144` kPa, ID 24-bit.
6. 명령 Write / STUDY_PAIR / GATT Notify **없음** — 스캔·로컬 바인딩만.
7. 검증용 오픈소스와 APK 1.6.1 디컴파일 결과가 일치한다.
