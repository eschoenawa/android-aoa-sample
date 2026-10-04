package de.eschoenawa.aoasample.hostapp

import de.eschoenawa.aoasample.host.domain.HostConnectionState

data class HostUiState(
    val connectionState: HostConnectionState = HostConnectionState.Disconnected,
    val log: List<String> = emptyList(),
) {
    val canSend: Boolean get() = connectionState is HostConnectionState.Connected
}
