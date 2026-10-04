package de.eschoenawa.aoasample.accessoryapp

import de.eschoenawa.aoasample.accessory.domain.AccessoryConnectionState

data class AccessoryUiState(
    val connectionState: AccessoryConnectionState = AccessoryConnectionState.Disconnected,
    val log: List<String> = emptyList(),
) {
    val canSend: Boolean get() = connectionState is AccessoryConnectionState.Connected
}
