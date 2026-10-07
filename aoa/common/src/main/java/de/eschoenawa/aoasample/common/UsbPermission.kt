package de.eschoenawa.aoasample.common

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

suspend fun Context.requestUsbPermission(
    hasPermission: Boolean,
    requestPermission: (PendingIntent) -> Unit,
    onBroadcastReceived: (Intent) -> Unit = {},
): Boolean {
    if (hasPermission) return true
    val action = "$packageName.USB_PERMISSION"
    return suspendCancellableCoroutine { continuation ->
        val registered = AtomicBoolean(true)
        lateinit var receiver: BroadcastReceiver
        fun unregisterOnce() {
            if (registered.compareAndSet(true, false)) unregisterReceiver(receiver)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                unregisterOnce()
                onBroadcastReceived(intent)
                continuation.resume(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
            }
        }
        ContextCompat.registerReceiver(this, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
        continuation.invokeOnCancellation { unregisterOnce() }
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(action).setPackage(packageName),
            PendingIntent.FLAG_MUTABLE,
        )
        requestPermission(permissionIntent)
    }
}
