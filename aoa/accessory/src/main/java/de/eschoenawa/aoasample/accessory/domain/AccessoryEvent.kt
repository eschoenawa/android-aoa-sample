package de.eschoenawa.aoasample.accessory.domain

sealed interface AccessoryEvent {
    data class BroadcastReceived(val action: String, val detail: String) : AccessoryEvent
}
