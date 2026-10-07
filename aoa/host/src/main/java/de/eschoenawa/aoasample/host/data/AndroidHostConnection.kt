package de.eschoenawa.aoasample.host.data

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.IntentCompat
import de.eschoenawa.aoasample.host.domain.HostConnection
import de.eschoenawa.aoasample.host.domain.HostConnectionState
import de.eschoenawa.aoasample.host.domain.HostConnectionState.Connected
import de.eschoenawa.aoasample.host.domain.HostConnectionState.Disconnected
import de.eschoenawa.aoasample.host.domain.HostConnectionState.Failed
import de.eschoenawa.aoasample.host.domain.HostConnectionState.FindingAccessory
import de.eschoenawa.aoasample.host.domain.HostConnectionState.OpeningAccessory
import de.eschoenawa.aoasample.host.domain.HostConnectionState.RequestingPermission
import de.eschoenawa.aoasample.host.domain.HostConnectionState.SwitchingToAccessoryMode
import de.eschoenawa.aoasample.host.domain.HostConnectionState.WaitingForReenumeration
import de.eschoenawa.aoasample.host.domain.HostEvent
import de.eschoenawa.aoasample.host.domain.HostIdentity
import de.eschoenawa.aoasample.host.domain.UsbDeviceIdentity
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

private const val REENUMERATION_TIMEOUT_MS = 10_000L

class AndroidHostConnection(
    context: Context,
    private val identity: HostIdentity,
) : HostConnection {

    private val context = context.applicationContext
    private val usbManager = context.getSystemService(UsbManager::class.java)

    private val mutableState = MutableStateFlow<HostConnectionState>(Disconnected)
    override val state: StateFlow<HostConnectionState> = mutableState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<HostEvent>(extraBufferCapacity = 64)
    override val events: Flow<HostEvent> = mutableEvents.asSharedFlow()

    private val mutableMessages = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val messages: Flow<ByteArray> = mutableMessages.asSharedFlow()

    override val attachedDevices: Flow<UsbDeviceIdentity> = context.usbDeviceBroadcasts()
        .filter { it.action == UsbManager.ACTION_USB_DEVICE_ATTACHED }
        .map { it.device.identity }

    @Volatile
    private var activeChannel: AoaChannel? = null

    override suspend fun connectUntilDetachedOrCancelled() {
        try {
            coroutineScope {
                val broadcasts = context.usbDeviceBroadcasts()
                    .onEach { mutableEvents.emit(HostEvent.BroadcastReceived(it.action, it.device.identity.toString())) }
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

    private suspend fun runSession(broadcasts: ReceiveChannel<UsbDeviceBroadcast>) {
        mutableState.value = FindingAccessory
        val attachedDevice = findAttachedDevice().withPermission()
        val accessory = if (attachedDevice.isInAccessoryMode) {
            attachedDevice
        } else {
            switchToAccessoryMode(attachedDevice, broadcasts).withPermission()
        }

        mutableState.value = OpeningAccessory(accessory.identity)
        val channel = AoaChannel(BulkEndpointTransport.open(usbManager, accessory))
        activeChannel = channel
        mutableState.value = Connected(accessory.identity)
        try {
            channel.runUntilDetached(
                awaitDetach = { broadcasts.awaitDetachOf(accessory) },
                onMessage = mutableMessages::emit,
            )
        } finally {
            activeChannel = null
        }
    }

    private fun findAttachedDevice(): UsbDevice {
        val devices = usbManager.deviceList.values
        return devices.singleOrNull()
            ?: devices.singleOrNull { it.isInAccessoryMode }
            ?: error(if (devices.isEmpty()) "No USB device attached" else "Multiple USB devices attached")
    }

    private suspend fun switchToAccessoryMode(
        device: UsbDevice,
        broadcasts: ReceiveChannel<UsbDeviceBroadcast>,
    ): UsbDevice {
        mutableState.value = SwitchingToAccessoryMode(device.identity)
        val connection = checkNotNull(usbManager.openDevice(device)) { "openDevice returned null" }
        try {
            val requests = AoaControlRequests(connection) { mutableEvents.tryEmit(it) }
            requests.readProtocolVersion()
            requests.sendIdentity(identity)
            requests.startAccessoryMode()
        } finally {
            connection.close()
        }

        mutableState.value = WaitingForReenumeration(device.identity)
        return withTimeoutOrNull(REENUMERATION_TIMEOUT_MS.milliseconds) { broadcasts.awaitAttachedAccessoryModeDevice() }
            ?: error("Device did not re-enumerate in Accessory mode within $REENUMERATION_TIMEOUT_MS ms")
    }

    private suspend fun UsbDevice.withPermission(): UsbDevice {
        mutableState.value = RequestingPermission(identity)
        val granted = context.requestUsbPermission(
            hasPermission = usbManager.hasPermission(this),
            requestPermission = { usbManager.requestPermission(this, it) },
            onBroadcastReceived = ::emitPermissionBroadcast,
        )
        check(granted) { "USB permission denied for $identity" }
        return this
    }

    private fun emitPermissionBroadcast(intent: Intent) {
        val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
        mutableEvents.tryEmit(HostEvent.BroadcastReceived(intent.action.orEmpty(), "${device?.identity} granted=$granted"))
    }

    private val UsbDevice.identity: UsbDeviceIdentity
        get() = UsbDeviceIdentity(deviceName, vendorId, productId)
}
