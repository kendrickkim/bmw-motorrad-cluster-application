package com.dooderge.bmw.motorrad.cluster.tpms

import android.os.ParcelUuid
import java.util.UUID

/**
 * TPMS Advanced (Sysgration / Pecham / Wicarlink) — Advertise Service UUID.
 * GATT Connect 없음.
 */
object TpmsBleUuids {
    /** Sysgration + Wicarlink */
    val SERVICE_FBB0: ParcelUuid =
        ParcelUuid.fromString("0000fbb0-0000-1000-8000-00805f9b34fb")

    /** Pecham (외장, 이름 "BR") */
    val SERVICE_27A5: ParcelUuid =
        ParcelUuid.fromString("000027a5-0000-1000-8000-00805f9b34fb")

    val SCAN_SERVICE_UUIDS: List<ParcelUuid> = listOf(SERVICE_FBB0, SERVICE_27A5)

    fun uuidEquals(a: UUID?, b: ParcelUuid): Boolean =
        a != null && a == b.uuid
}
