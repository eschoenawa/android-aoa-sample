package de.eschoenawa.aoasample.host.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal data class UsbDeviceBroadcast(val action: String, val device: UsbDevice)

internal fun Context.usbDeviceBroadcasts(): Flow<UsbDeviceBroadcast> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            if (device != null) trySend(UsbDeviceBroadcast(intent.action.orEmpty(), device))
        }
    }
    val filter = IntentFilter().apply {
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
    }
    ContextCompat.registerReceiver(this@usbDeviceBroadcasts, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    awaitClose { unregisterReceiver(receiver) }
}

internal suspend fun ReceiveChannel<UsbDeviceBroadcast>.awaitAttachedAccessoryModeDevice(): UsbDevice {
    for (broadcast in this) {
        if (broadcast.action == UsbManager.ACTION_USB_DEVICE_ATTACHED && broadcast.device.isInAccessoryMode) {
            return broadcast.device
        }
    }
    error("USB broadcasts stopped before re-enumeration")
}

internal suspend fun ReceiveChannel<UsbDeviceBroadcast>.awaitDetachOf(device: UsbDevice) {
    for (broadcast in this) {
        if (broadcast.action == UsbManager.ACTION_USB_DEVICE_DETACHED && broadcast.device.deviceName == device.deviceName) {
            return
        }
    }
}
