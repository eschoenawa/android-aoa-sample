package de.eschoenawa.aoasample.host.domain

sealed interface HostConnectionState {
    data object Disconnected : HostConnectionState
    data object FindingAccessory : HostConnectionState
    data class RequestingPermission(val accessory: UsbDeviceIdentity) : HostConnectionState
    data class SwitchingToAccessoryMode(val accessory: UsbDeviceIdentity) : HostConnectionState
    data class WaitingForReenumeration(val previousIdentity: UsbDeviceIdentity) : HostConnectionState
    data class OpeningAccessory(val accessory: UsbDeviceIdentity) : HostConnectionState
    data class Connected(val accessory: UsbDeviceIdentity) : HostConnectionState
    data class Failed(val reason: String) : HostConnectionState
}
