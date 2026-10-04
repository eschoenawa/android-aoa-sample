package de.eschoenawa.aoasample.accessoryapp

import android.content.Intent
import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.eschoenawa.aoasample.accessory.domain.AccessoryConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccessoryViewModel(private val connection: AccessoryConnection) : ViewModel() {

    private val mutableUiState = MutableStateFlow(AccessoryUiState())
    val uiState: StateFlow<AccessoryUiState> = mutableUiState.asStateFlow()

    private var sessionJob: Job? = null

    init {
        connection.state
            .onEach { state ->
                mutableUiState.update { it.copy(connectionState = state) }
                appendLog("State: $state")
            }
            .launchIn(viewModelScope)
        connection.events.onEach { appendLog("Event: $it") }.launchIn(viewModelScope)
        connection.messages.onEach { appendLog("Received: ${it.decodeToString()}") }.launchIn(viewModelScope)
    }

    fun onIntent(source: String, intent: Intent) {
        appendLog("Intent ($source): ${intent.action}")
        if (intent.action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED) connect()
    }

    fun connect() {
        if (sessionJob?.isActive == true) return
        sessionJob = viewModelScope.launch { connection.connectUntilDetachedOrCancelled() }
    }

    fun disconnect() {
        sessionJob?.cancel()
    }

    fun send(text: String) {
        viewModelScope.launch {
            runCatching { connection.send(text.encodeToByteArray()) }
                .onSuccess { appendLog("Sent: $text") }
                .onFailure { appendLog("Send failed: ${it.message}") }
        }
    }

    private fun appendLog(line: String) = mutableUiState.update { it.copy(log = it.log + line) }
}
