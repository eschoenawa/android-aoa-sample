package de.eschoenawa.aoasample.host.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HostConnection {
    val state: StateFlow<HostConnectionState>
    val events: Flow<HostEvent>
    val messages: Flow<ByteArray>
    val attachedDevices: Flow<UsbDeviceIdentity>

    suspend fun connectUntilDetachedOrCancelled()
    suspend fun send(message: ByteArray)
}
