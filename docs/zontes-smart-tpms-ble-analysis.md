# Zontes Smart TPMS BLE GATT 분석

- **앱**: Zontes Smart `1.12` (`tayo.com.ZontesIntelligence`, versionCode `3034`)
- **소스**: `app/Zontes+Smart_1.12_APKPure.xapk`
- **관련 패키지**: `net.easyconn.carman.bluetooth.ble.tpms.*`
- **사용 하드웨어 (TPMS)**: [AliExpress 상품 — item `1005013156406545`](https://ko.aliexpress.com/item/1005013156406545.html)  
  본 분석·클러스터 연동은 위 링크에서 판매하는 TPMS를 기준으로 한다. 동 제품은 Zontes Smart 앱과 연동되는 BLE 프로토콜을 사용한다.

타이어 공기압(TPMS)은 BLE GATT Notify로 수신하고, 설정/조회 명령은 Write Characteristic으로 전송한다.

---

## 1. 개요

| 항목 | 내용 |
|------|------|
| 스캔 식별 UUID | `0000ffb0-0000-1000-8000-00805f9b34fb` |
| 압력 데이터 수신 | Service `ffe0` / Characteristic **`ffe4`** (Notify) |
| 명령 전송 | Service `ffe5` / Characteristic **`ffe9`** (Write) |
| 프로토콜 | 커스텀 바이너리 프레임 (`0xFC` 헤더 + XOR 체크섬) |
| 구현 클래스 | `TPMSConstants`, `TPMSPresenter`, `TPMSParser`, `Instruct` |

---

## 2. GATT Service / Characteristic

정의 위치: `TPMSConstants.java`

### 2.1 TPMS 데이터 / 제어 (핵심)

| 용도 | Service UUID | Characteristic UUID | 속성 |
|------|--------------|---------------------|------|
| **압력·온도 Notify** | `0000ffe0-0000-1000-8000-00805f9b34fb` | **`0000ffe4-0000-1000-8000-00805f9b34fb`** | Notify |
| **명령 Write** | `0000ffe5-0000-1000-8000-00805f9b34fb` | **`0000ffe9-0000-1000-8000-00805f9b34fb`** | Write |
| Notify 활성화 (CCCD) | — | `00002902-0000-1000-8000-00805f9b34fb` | Write |

### 2.2 표준 보조 서비스

| 용도 | Service UUID | Characteristic UUID |
|------|--------------|---------------------|
| Battery | `0000180f-0000-1000-8000-00805f9b34fb` | `00002a19-0000-1000-8000-00805f9b34fb` |
| Device Information | `0000180a-0000-1000-8000-00805f9b34fb` | `00002a23` (System ID로 사용), `00002a26` (Firmware), `00002a29` (Manufacturer) |

> `LIST_DEVICE_CHARACTERISTIC_INFORMATION`에는 Battery(`2a19`)와 Firmware Revision(`2a26`)만 포함되어 연결 후 Read 큐에 들어간다.

### 2.3 스캔 필터

`ScanParser` / `ScanConstants`:

| 장치 유형 | Advertise Service UUID |
|-----------|------------------------|
| **TPMS** | `0000ffb0-0000-1000-8000-00805f9b34fb` |
| HUD | `0000fff0-0000-1000-8000-00805f9b34fb` |
| WRC (리모컨 등) | `00001c00-...`, `0000474d-...`, `0000474e-...` 등 |

---

## 3. 연결·데이터 수신 흐름

`TPMSPresenter` 기준:

1. BLE 스캔 → advertise에 `0000ffb0` 포함 시 TPMS로 인식
2. GATT 연결 → `discoverServices()`
3. Service 순회
   - `ffe0`에서 `ffe4` (Notify) 확보
   - `ffe5`에서 `ffe9` (Write) 확보
4. `ffe4`에 CCCD(`2902`) write → Notification ON
5. 연결 직후 명령 큐에 `QUERY_ID` + `QUERY_SETTING` 적재 후 Write
6. `onCharacteristicChanged`에서 UUID가 `ffe4`인 페이로드만 `TPMSParser.parse()`로 처리

```text
[Phone App]
    |  Write (명령)
    v
Service ffe5 / Char ffe9  ---->  TPMS 장치
Service ffe0 / Char ffe4  <----  Notify (타이어 데이터·응답)
    |
    v
TPMSParser.parse() → ITyre / ITPMSDevice 갱신
```

---

## 4. 프레임 프로토콜

공통 포맷:

```text
[ HEAD=0xFC ][ LEN ][ FRAME_TYPE ][ PAYLOAD... ][ XOR_CHECKSUM ]
```

- `HEAD`: `0xFC` (`TPMSParseConstants.HEAD = -4`)
- `LEN`: 뒤따르는 바이트 수(프레임 타입 + 페이로드 + 체크섬 쪽 길이 규칙에 맞춤)
- `FRAME_TYPE`: 명령/응답 종류
- `CHECKSUM`: 앞 바이트들의 XOR

고정 명령 예시:

| 명령 | 바이트 |
|------|--------|
| QUERY_ID | `FC 02 D5 2B` |
| QUERY_SETTING | `FC 02 C5 3B` |
| VOICE_ALARM_OPEN | `FC 02 B5 4B` |
| VOICE_ALARM_CLOSE | `FC 02 A5 …` |

---

## 5. Write 명령 종류 (`Instruct.Action`)

모두 Characteristic **`ffe9`** 로 전송.

| Action | 의미 | Frame | 페이로드 |
|--------|------|-------|----------|
| `QUERY_ID` | 센서 ID 조회 | `0xD5` (-43) | 없음 |
| `QUERY_SETTING` | 설정값 조회 | `0xC5` (-59) | 없음 |
| `CHANGE_LOW_PRESSURE_THRESHOLD` | 저압 임계값 설정 | `0x02` | `1, value, unit(BAR)` |
| `CHANGE_HIGH_PRESSURE_THRESHOLD` | 고압 임계값 설정 | `0x03` | `1, value, unit(BAR)` |
| `CHANGE_TEMPERATURE_THRESHOLD` | 온도 임계값 설정 | `0xF5` (-11) | `value` |
| `STUDY_PAIR` | 학습 페어링 (QR/수동 센서 ID) | `0x04` | `position, sensorId[]` — ID는 `TPMSUtil.hexStringToBytes`로 hex 문자열→바이트 (보통 4바이트/`XXXXXXXX`) |
| `AUTO_STUDY` | 감압(洩压) 자동학습 | `0x06` | `position` |
| `CANCEL_STUDY` | 자동학습 취소 | `0x07` | `position` |
| `CHANGE_TYRE` | 타이어 위치 교환 | `0xE5` (-27) | `fromPos, toPos` |
| `VOICE_ALARM_OPEN` | 음성 알람 ON | `0xB5` (-75) | 없음 |
| `VOICE_ALARM_CLOSE` | 음성 알람 OFF | `0xA5` (-91) | 없음 |

### 임계값 범위 (`TPMSParseConstants`)

| 항목 | Min | Max |
|------|-----|-----|
| 저압 (Low) | 15 | 22 |
| 고압 (High) | 28 | 35 |
| 온도 | 70 | 90 |

기본값 예시 (`ITPMSDevice.Settings`): 고압 `32`, 저압 `18`, 온도 `90`, 단위 `BAR`.

### 타이어 위치 ID

| ID | 위치 |
|----|------|
| `1` | 좌전 / Left Front |
| `2` | 우전 / Right Front |
| `3` | 좌후 / Left Behind |
| `4` | 우후 / Right Behind |

---

## 6. Notify 응답 / 푸시 Frame Type

Characteristic **`ffe4`** 로 수신. `bArr[2]`가 프레임 타입.

| Frame | Hex | 의미 | 처리 |
|-------|-----|------|------|
| `FRAME_DATA` | `0x01` | **타이어 실시간 데이터** | `setTyreData()` |
| `FRAME_LOW_PRESSURE_THRESHOLD` | `0x02` | 저압 임계값 설정 성공 | `setPressureMin()` |
| `FRAME_HIGH_PRESSURE_THRESHOLD` | `0x03` | 고압 임계값 설정 성공 | `setPressureMax()` |
| `FRAME_STUDY_PAIR` | `0x04` | 학습 페어링 성공 | 로그 |
| `FRAME_ERROR` | `0x05` | 오류 | `onDeviceActionError(5)` |
| `FRAME_AUTO_STUDY` | `0x06` | 자동학습 성공 | `setOneTyreData()` |
| `FRAME_CANCEL_AUTO_STUDY` | `0x07` | 자동학습 취소 | 로그 |
| `FRAME_TEMPERATURE_THRESHOLD` | `0xF5` | 온도 임계값 조정 OK | `setTemperatureMax()` |
| `FRAME_CHANGE_TYRE` | `0xE5` | 타이어 순서 교환 성공 | `setChangeTyreData()` |
| `FRAME_ERROR_CHANGE_ORDER` | `0xE6` | 타이어 교환 실패 | `onTyreChange(false)` |
| `FRAME_QUERY_ID` | `0xD5` | ID 조회 성공 | `setTyreId()` |
| `FRAME_QUERY_SETTING` | `0xC5` | 설정 조회 성공 | `setObdSetting()` |
| `FRAME_VOICE_ALARM_OPEN` | `0xB5` | 음성알람 ON 성공 | |
| `FRAME_ERROR_VOICE_ALARM_OPEN` | `0xB6` | 음성알람 ON 실패 | |
| `FRAME_VOICE_ALARM_CLOSE` | `0xA5` | 음성알람 OFF 성공 | |
| `FRAME_ERROR_VOICE_ALARM_CLOSE` | `0xA6` | 음성알람 OFF 실패 | |

---

## 7. 타이어 데이터 페이로드 (`FRAME_DATA = 0x01`)

`ITPMSDevice.setTyreData()` → `ITyre.setData()`:

| Offset | 내용 |
|--------|------|
| `[2]` | Frame type (`0x01`) |
| `[3]` | 타이어 위치 (1~4) |
| `[5]` | **Pressure** (raw) |
| `[6]` | Temperature (°C) |
| `[7]` | Battery |
| `[8]` | Unit (`0` = BAR, 그 외 KPA 등) |

표시 변환 (`ITyre`):

- **BAR**: `pressure / 10.0` (예: raw `25` → `2.5 Bar`)
- **KPA**: `pressure * 10`

---

## 8. 주요 소스 파일

| 파일 | 역할 |
|------|------|
| `.../ble/tpms/TPMSConstants.java` | GATT UUID 정의 |
| `.../ble/tpms/TPMSPresenter.java` | 연결, Notify 등록, Write 큐 |
| `.../ble/tpms/TPMSParser.java` | 명령 생성·응답 파싱 |
| `.../ble/tpms/TPMSParseConstants.java` | Frame 상수·고정 Instruct |
| `.../ble/tpms/Instruct.java` | 명령 Action enum |
| `.../bean/ITyre.java` | 압력/온도/배터리 파싱 |
| `.../bean/ITPMSDevice.java` | 4개 타이어·설정 모델 |
| `.../ble/scan/ScanConstants.java` | 스캔 UUID (`ffb0`) |

디컴파일 경로 예:

```text
app/decompiled_c7/sources/net/easyconn/carman/bluetooth/ble/tpms/
```

---

## 9. 센서 QR / ID 등록 (학습 페어링)

앱 내 `HWQrFragment` 등 QR UI는 주로 **Wi‑Fi P2P / EasyConn 연결**용이다.  
타이어 센서 등록에 해당하는 BLE 경로는 다음과 같다.

| 단계 | 구현 |
|------|------|
| ID 문자열 → 바이트 | `TPMSUtil.hexStringToBytes` (예: `"A1B2C3D4"` → 4 bytes) |
| 허브에 학습 | `TPMSPresenter.studyPair` → `Instruct.STUDY_PAIR` (`0x04`, position + sensorId) |
| ID 길이 | `ITPMSDevice.setTyreId` 기준 **타이어당 4바이트** (표시 `"00000000"`이면 미등록) |
| 대체 학습 | `AUTO_STUDY` (`0x06`, 감압/洩压 학습) — ID 입력 없이 위치만 지정 |

클러스터 앱은 동일하게 QR 스캔·수동 hex 입력 → MAC이면 직접 연결, hex ID면 스캔 목록 매칭 후 연결 + `STUDY_PAIR` 전송.

---

## 10. 요약

1. **공기압 값은 GATT Notify `0000ffe4` (Service `0000ffe0`)에서 읽는다.**
2. **명령은 Write `0000ffe9` (Service `0000ffe5`)로 보낸다.**
3. 장치는 스캔 시 Service UUID **`0000ffb0`** 으로 구분한다.
4. Write 명령은 조회·임계값·학습/페어링·위치교환·음성알람 등 **11종**이다.
5. 실시간 압력은 Notify frame type **`0x01`** 페이로드 offset `[5]`이다.
6. 센서 QR/ID 등록은 **`hexStringToBytes` + `STUDY_PAIR`** 경로다.
7. 검증·연동에 사용한 TPMS 하드웨어는 [AliExpress item 1005013156406545](https://ko.aliexpress.com/item/1005013156406545.html)이다.
