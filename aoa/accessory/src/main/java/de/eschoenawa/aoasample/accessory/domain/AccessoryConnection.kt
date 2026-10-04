package de.eschoenawa.aoasample.accessory.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AccessoryConnection {
    val state: StateFlow<AccessoryConnectionState>
    val events: Flow<AccessoryEvent>
    val messages: Flow<ByteArray>

    suspend fun connectUntilDetachedOrCancelled()
    suspend fun send(message: ByteArray)
}
