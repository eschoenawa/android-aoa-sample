package de.eschoenawa.aoasample.accessoryapp

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
import de.eschoenawa.aoasample.accessory.data.AndroidAccessoryConnection
import de.eschoenawa.aoasample.accessory.domain.HostIdentity

private val EXPECTED_HOST = HostIdentity(
    manufacturer = "Android AoA Sample",
    model = "Host",
    version = "1",
)

class AccessoryActivity : ComponentActivity() {

    private val viewModel: AccessoryViewModel by viewModels {
        viewModelFactory {
            initializer { AccessoryViewModel(AndroidAccessoryConnection(applicationContext, EXPECTED_HOST)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) viewModel.onIntent("onCreate", intent)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AccessoryScreen(
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
