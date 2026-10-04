package de.eschoenawa.aoasample.accessory.data

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnection
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnectionState
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnectionState.Connected
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnectionState.Disconnected
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnectionState.Failed
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnectionState.FindingHost
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnectionState.OpeningHost
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnectionState.RequestingPermission
import de.eschoenawa.aoasample.accessory.domain.AccessoryEvent
import de.eschoenawa.aoasample.accessory.domain.HostIdentity
import de.eschoenawa.aoasample.common.AoaChannel
import de.eschoenawa.aoasample.common.requestUsbPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.produceIn

class AndroidAccessoryConnection(
    context: Context,
    private val expectedHost: HostIdentity,
) : AccessoryConnection {

    private val context = context.applicationContext
    private val usbManager = context.getSystemService(UsbManager::class.java)

    private val mutableState = MutableStateFlow<AccessoryConnectionState>(Disconnected)
    override val state: StateFlow<AccessoryConnectionState> = mutableState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<AccessoryEvent>(extraBufferCapacity = 64)
    override val events: Flow<AccessoryEvent> = mutableEvents.asSharedFlow()

    private val mutableMessages = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val messages: Flow<ByteArray> = mutableMessages.asSharedFlow()

    @Volatile
    private var activeChannel: AoaChannel? = null

    override suspend fun connectUntilDetachedOrCancelled() {
        try {
            coroutineScope {
                val broadcasts = context.usbAccessoryBroadcasts()
                    .onEach { mutableEvents.emit(AccessoryEvent.BroadcastReceived(it.action, it.externalHost?.identity.toString())) }
                    .produceIn(this)
                try {
                    runSession(broadcasts)
                } finally {
                    broadcasts.cancel()
                }
            }
            mutableState.value = Disconnected
        } catch (cancellation: CancellationException) {
            mutableState.value = Disconnected
            throw cancellation
        } catch (failure: Exception) {
            mutableState.value = Failed(failure.message ?: failure.toString())
        }
    }

    override suspend fun send(message: ByteArray) {
        val channel = checkNotNull(activeChannel) { "Not connected" }
        channel.send(message)
    }

    private suspend fun runSession(broadcasts: ReceiveChannel<UsbAccessoryBroadcast>) {
        mutableState.value = FindingHost
        val externalHost = findExternalHost()

        mutableState.value = RequestingPermission(externalHost.identity)
        val granted = context.requestUsbPermission(
            hasPermission = usbManager.hasPermission(externalHost),
            requestPermission = { usbManager.requestPermission(externalHost, it) },
            onBroadcastReceived = ::emitPermissionBroadcast,
        )
        check(granted) { "USB permission denied for ${externalHost.identity}" }

        mutableState.value = OpeningHost(externalHost.identity)
        val descriptor = checkNotNull(usbManager.openAccessory(externalHost)) { "openAccessory returned null" }
        val channel = AoaChannel(FileDescriptorTransport(descriptor))
        activeChannel = channel
        mutableState.value = Connected(externalHost.identity)
        try {
            channel.runUntilDetached(
                awaitDetach = { broadcasts.awaitDetachOf(externalHost) },
                onMessage = mutableMessages::emit,
            )
        } finally {
            activeChannel = null
        }
    }

    private fun findExternalHost(): UsbAccessory {
        // AOSP naming is confusing here: a UsbAccessory is the external Host that switched this device into Accessory mode.
        val externalHosts = usbManager.accessoryList.orEmpty()
        return checkNotNull(externalHosts.singleOrNull { it.identity == expectedHost }) {
            "No Host matching $expectedHost in ${externalHosts.map { it.identity }}"
        }
    }

    private fun emitPermissionBroadcast(intent: Intent) {
        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
        mutableEvents.tryEmit(AccessoryEvent.BroadcastReceived(intent.action.orEmpty(), "${intent.externalHost()?.identity} granted=$granted"))
    }

    private val UsbAccessory.identity: HostIdentity
        get() = HostIdentity(manufacturer, model, version.orEmpty())
}
