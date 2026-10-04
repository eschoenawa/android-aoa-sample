package de.eschoenawa.aoasample.accessory.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal data class UsbAccessoryBroadcast(val action: String, val externalHost: UsbAccessory?)

internal fun Intent.externalHost(): UsbAccessory? =
    IntentCompat.getParcelableExtra(this, UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)

internal fun Context.usbAccessoryBroadcasts(): Flow<UsbAccessoryBroadcast> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(UsbAccessoryBroadcast(intent.action.orEmpty(), intent.externalHost()))
        }
    }
    val filter = IntentFilter().apply {
        addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
    }
    ContextCompat.registerReceiver(this@usbAccessoryBroadcasts, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    awaitClose { unregisterReceiver(receiver) }
}

internal suspend fun ReceiveChannel<UsbAccessoryBroadcast>.awaitDetachOf(externalHost: UsbAccessory) {
    for (broadcast in this) {
        if (broadcast.action == UsbManager.ACTION_USB_ACCESSORY_DETACHED && broadcast.externalHost == externalHost) {
            return
        }
    }
}
