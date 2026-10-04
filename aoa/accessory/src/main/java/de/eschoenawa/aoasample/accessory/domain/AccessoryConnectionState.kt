package de.eschoenawa.aoasample.accessory.domain

sealed interface AccessoryConnectionState {
    data object Disconnected : AccessoryConnectionState
    data object FindingHost : AccessoryConnectionState
    data class RequestingPermission(val host: HostIdentity) : AccessoryConnectionState
    data class OpeningHost(val host: HostIdentity) : AccessoryConnectionState
    data class Connected(val host: HostIdentity) : AccessoryConnectionState
    data class Failed(val reason: String) : AccessoryConnectionState
}
