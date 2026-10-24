package com.dooderge.bmw.motorrad.cluster.tpms

import android.os.ParcelUuid
import java.util.UUID

/**
 * LYTPMS Advertise Service UUID.
 * 공식 앱은 raw AD `03 03 B0 FB` (= `0xFBB0`) 프리픽스로 식별한다.
 * GATT Connect 없음.
 */
object TpmsBleUuids {
    val SERVICE_FBB0: ParcelUuid =
        ParcelUuid.fromString("0000fbb0-0000-1000-8000-00805f9b34fb")

    val SCAN_SERVICE_UUIDS: List<ParcelUuid> = listOf(SERVICE_FBB0)

    fun uuidEquals(a: UUID?, b: ParcelUuid): Boolean =
        a != null && a == b.uuid
}
