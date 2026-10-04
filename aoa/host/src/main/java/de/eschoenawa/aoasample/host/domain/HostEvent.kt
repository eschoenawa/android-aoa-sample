package de.eschoenawa.aoasample.host.domain

sealed interface HostEvent {
    data class BroadcastReceived(val action: String, val detail: String) : HostEvent
    data class ControlRequestSent(
        val request: AoaControlRequest,
        val index: Int,
        val data: String,
        val result: Int,
    ) : HostEvent
}

enum class AoaControlRequest(val code: Int) {
    GET_PROTOCOL(51),
    SEND_STRING(52),
    START_ACCESSORY_MODE(53),
}
