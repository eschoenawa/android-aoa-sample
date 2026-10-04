package de.eschoenawa.aoasample.host.data

import android.hardware.usb.UsbConstants.USB_DIR_IN
import android.hardware.usb.UsbConstants.USB_DIR_OUT
import android.hardware.usb.UsbConstants.USB_TYPE_VENDOR
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import de.eschoenawa.aoasample.host.domain.AoaControlRequest
import de.eschoenawa.aoasample.host.domain.AoaControlRequest.GET_PROTOCOL
import de.eschoenawa.aoasample.host.domain.AoaControlRequest.SEND_STRING
import de.eschoenawa.aoasample.host.domain.AoaControlRequest.START_ACCESSORY_MODE
import de.eschoenawa.aoasample.host.domain.HostEvent
import de.eschoenawa.aoasample.host.domain.HostIdentity

private const val GOOGLE_VENDOR_ID = 0x18D1
private val ACCESSORY_MODE_PRODUCT_IDS = setOf(0x2D00, 0x2D01, 0x2D04, 0x2D05)
private const val TIMEOUT_MS = 1_000

internal val UsbDevice.isInAccessoryMode: Boolean
    get() = vendorId == GOOGLE_VENDOR_ID && productId in ACCESSORY_MODE_PRODUCT_IDS

internal class AoaControlRequests(
    private val connection: UsbDeviceConnection,
    private val onRequestSent: (HostEvent.ControlRequestSent) -> Unit,
) {
    fun readProtocolVersion(): Int {
        val version = ByteArray(2)
        val result = send(GET_PROTOCOL, USB_DIR_IN, index = 0, data = version)
        val protocolVersion = (version[0].toInt() and 0xFF) or ((version[1].toInt() and 0xFF) shl 8)
        onRequestSent(HostEvent.ControlRequestSent(GET_PROTOCOL, 0, "version=$protocolVersion", result))
        check(result == version.size && protocolVersion >= 1) { "Device does not support AOA" }
        return protocolVersion
    }

    fun sendIdentity(identity: HostIdentity) {
        identity.inAoaStringIndexOrder().forEachIndexed { index, value ->
            val nullTerminated = value.encodeToByteArray() + 0
            val result = send(SEND_STRING, USB_DIR_OUT, index, nullTerminated)
            onRequestSent(HostEvent.ControlRequestSent(SEND_STRING, index, value, result))
            check(result == nullTerminated.size) { "SEND_STRING $index failed with $result" }
        }
    }

    fun startAccessoryMode() {
        val result = send(START_ACCESSORY_MODE, USB_DIR_OUT, index = 0, data = ByteArray(0))
        onRequestSent(HostEvent.ControlRequestSent(START_ACCESSORY_MODE, 0, "", result))
        check(result >= 0) { "START_ACCESSORY_MODE failed with $result" }
    }

    private fun send(request: AoaControlRequest, direction: Int, index: Int, data: ByteArray): Int =
        connection.controlTransfer(
            direction or USB_TYPE_VENDOR,
            request.code,
            0,
            index,
            data,
            data.size,
            TIMEOUT_MS,
        )
}
