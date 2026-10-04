package de.eschoenawa.aoasample.host.data

import android.hardware.usb.UsbConstants.USB_CLASS_VENDOR_SPEC
import android.hardware.usb.UsbConstants.USB_DIR_IN
import android.hardware.usb.UsbConstants.USB_DIR_OUT
import android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbRequest
import de.eschoenawa.aoasample.common.UsbTransport
import java.nio.ByteBuffer

private const val WRITE_TIMEOUT_MS = 1_000
private const val IN_REQUEST_BYTES = 16 * 1024

internal class BulkEndpointTransport private constructor(
    private val connection: UsbDeviceConnection,
    private val aoaInterface: UsbInterface,
    private val bulkOut: UsbEndpoint,
    private val inRequest: UsbRequest,
) : UsbTransport {

    private val inBuffer = ByteBuffer.allocate(IN_REQUEST_BYTES)

    @Volatile
    private var closed = false

    override fun read(buffer: ByteArray): Int {
        if (closed) return -1
        inBuffer.clear()
        if (!inRequest.queue(inBuffer)) return -1
        val completed = try {
            connection.requestWait()
        } catch (failure: Exception) {
            null
        }
        if (closed || completed !== inRequest) return -1
        val read = inBuffer.position()
        inBuffer.flip()
        inBuffer.get(buffer, 0, minOf(read, buffer.size))
        return read
    }

    override fun write(bytes: ByteArray) {
        val written = connection.bulkTransfer(bulkOut, bytes, bytes.size, WRITE_TIMEOUT_MS)
        check(written == bytes.size) { "Bulk OUT wrote $written of ${bytes.size} bytes" }
    }

    override fun close() {
        closed = true
        inRequest.cancel()
        connection.releaseInterface(aoaInterface)
        connection.close()
        inRequest.close()
    }

    companion object {
        fun open(usbManager: UsbManager, accessory: UsbDevice): BulkEndpointTransport {
            val aoaInterface = (0 until accessory.interfaceCount)
                .map(accessory::getInterface)
                .first { it.interfaceClass == USB_CLASS_VENDOR_SPEC && it.bulkEndpoint(USB_DIR_IN) != null }
            val bulkIn = checkNotNull(aoaInterface.bulkEndpoint(USB_DIR_IN))
            val bulkOut = checkNotNull(aoaInterface.bulkEndpoint(USB_DIR_OUT)) { "No bulk OUT endpoint" }
            val connection = checkNotNull(usbManager.openDevice(accessory)) { "openDevice returned null" }
            check(connection.claimInterface(aoaInterface, true)) {
                connection.close()
                "claimInterface failed"
            }
            val inRequest = UsbRequest()
            check(inRequest.initialize(connection, bulkIn)) {
                inRequest.close()
                connection.releaseInterface(aoaInterface)
                connection.close()
                "UsbRequest.initialize failed"
            }
            return BulkEndpointTransport(connection, aoaInterface, bulkOut, inRequest)
        }

        private fun UsbInterface.bulkEndpoint(direction: Int): UsbEndpoint? =
            (0 until endpointCount)
                .map(::getEndpoint)
                .firstOrNull { it.type == USB_ENDPOINT_XFER_BULK && it.direction == direction }
    }
}
