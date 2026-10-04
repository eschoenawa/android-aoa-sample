package de.eschoenawa.aoasample.hostapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.eschoenawa.aoasample.host.data.AndroidHostConnection
import de.eschoenawa.aoasample.host.domain.HostIdentity

private val SAMPLE_HOST_IDENTITY = HostIdentity(
    manufacturer = "Android AoA Sample",
    model = "Host",
    description = "Android Open Accessory host sample",
    version = "1",
    uri = "https://source.android.com/docs/core/interaction/accessories/protocol",
    serial = "host-sample",
)

class HostActivity : ComponentActivity() {

    private val viewModel: HostViewModel by viewModels {
        viewModelFactory {
            initializer { HostViewModel(AndroidHostConnection(applicationContext, SAMPLE_HOST_IDENTITY)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) viewModel.onIntent("onCreate", intent)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HostScreen(
                    uiState = uiState,
                    onConnect = viewModel::connect,
                    onDisconnect = viewModel::disconnect,
                    onSend = viewModel::send,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onIntent("onNewIntent", intent)
    }
}
