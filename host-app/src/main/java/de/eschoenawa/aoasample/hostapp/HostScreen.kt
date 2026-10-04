package de.eschoenawa.aoasample.hostapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun HostScreen(
    uiState: HostUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSend: (String) -> Unit,
) {
    var message by rememberSaveable { mutableStateOf("") }
    val logState = rememberLazyListState()
    LaunchedEffect(uiState.log.size) {
        if (uiState.log.isNotEmpty()) logState.animateScrollToItem(uiState.log.lastIndex)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Host", style = MaterialTheme.typography.titleLarge)
            Text(uiState.connectionState.toString(), style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnect) { Text("Connect") }
                Button(onClick = onDisconnect) { Text("Disconnect") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { onSend(message) }, enabled = uiState.canSend) { Text("Send") }
            }
            LazyColumn(state = logState, modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(uiState.log) { line ->
                    Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
