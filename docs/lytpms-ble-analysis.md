# LYTPMS 1.0.3 BLE 분석

- **앱**: LYTPMS `1.0.3` (`com.zl.dev.tire` / Wicarlink `com.wicarlink.zl`)
- **소스 APK**: `lytpms/LYTPMS 1.0.3.apk` → `base.apk`
- **디컴파일**: `lytpms/decompiled/`
- **대상**: LYTPMS / Wicarlink 계열 **Advertise-only** 타이어 센서

> TPMS Advanced 오픈소스(Sysgration / Pecham) 레이아웃은 **공식 앱과 다르며 참조하지 않는다.**

---

## 1. 개요

| 항목 | 내용 |
|------|------|
| 통신 방식 | **Passive BLE Scan** (Advertise) |
| GATT Connect / Notify / Write | **센서 수신에 사용하지 않음** |
| 식별 프리픽스 | `0201060303B0FB` (`Flags=0x06` + UUID `FBB0`) |
| 구현 클래스 | `BleServiceLY` → `BaseTireBleService.handScanResult` |
| 파서 | `TireBean.parse` + `VerifyUtil.validateCRCLYTPMS` |
| 압력 단위 | **kPa** (`raw * 3.144`), PSI = kPa / `6.89476` |
| 센서 ID | 6 hex 문자열 (예: `CCBBAA`) |

스캔 사이클 (공식):

- `SCAN_MODE_LOW_LATENCY`, 빈 `ScanFilter`
- **60초 스캔 → 정지 → 2초 후 재스캔**

---

## 2. Advertise 레이아웃

전체 `scanRecord.bytes` hex:

```text
0201060303B0FB | <19-byte payload hex = substring(14,52)> | …
└─ BLE_ID (14 hex chars = 7 bytes)
```

| AD | 의미 |
|----|------|
| `02 01 06` | Flags: LE General Discoverable |
| `03 03 B0 FB` | Complete 16-bit UUID list = `0xFBB0` |

### Payload (`TireBean.parse`, 19 bytes)

| Offset | 내용 |
|--------|------|
| `[4]` | Voltage → `raw * 0.01 + 1.22` V |
| `[5]` | Pressure raw (+256 if `[10]==1`) → **`raw * 3.144` kPa** |
| `[6]` | Temperature → **`raw - 55` °C** |
| `[7]` | State (`0`정상 `1`누기 `2`충전 …) |
| `[8]` | Version |
| `[9]` / `[11]` | CRC 관련 |
| `[10]` | Pressure high flag (`1` → raw+256) |
| `[12]` | BLE version nibble |
| `[16..18]` | Sensor ID wire bytes |
| ID 표시 | `hex([18][17][16])` → 6자리 대문자 hex |

### CRC (`ValidateUtil.validateCRCLYTPMS`)

입력: advertise hex `substring(18)` (= byte index 9부터).

```text
buf[0..6]  = input[0..6]
buf[7..13] = input[10..16]
expected   = ((u8(input[7]) << 8) | u8(input[9])) - (signed)input[8]
ok         = crc16_xmodem(buf) == expected
```

---

## 3. 바인딩

- GATT STUDY_PAIR **없음**
- `TireData.id` (6hex) ↔ `TireBean.ID` 문자열 `equals`
- 바인딩 UI: 스캔 목록 / 수동 ID 입력 / (앱에 따라) 자동 바인드 모드(state=누기·충전 시)

---

## 4. (참고) 허브용 GATT 상수

`BleUtil` 에 `00002760-08C2-11E1-9073-…` SERVICE/NOTIFY/WRITE 와 `5A A5 …` 명령이 있으나,
**LYTPMS 센서 수신 경로(`BleServiceLY`)는 Advertise 전용**이다. 클러스터 구현도 Advertise만 사용한다.

---

## 5. 클러스터 대응

| 공식 | 클러스터 |
|------|----------|
| `BleServiceLY` | `TpmsAdvParsers.parseLytpms` + `TpmsService` |
| `TireBean.parse` | 동일 오프셋·공식 |
| `validateCRCLYTPMS` | `TpmsAdvParsers.validateCrcLytpms` |
| 60s/2s 스캔 | `TpmsAdvScanner.startCycling` |
| ID 6hex | `TpmsProtocol.hex6ToSensorId` / `sensorIdToHex` |
