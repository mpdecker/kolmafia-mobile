package net.sourceforge.kolmafia.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.chat.ChatManager
import net.sourceforge.kolmafia.chat.ChatPoller
import net.sourceforge.kolmafia.chat.ChatSender
import org.koin.compose.koinInject

@Composable
fun ChatScreen(
    chatManager: ChatManager = koinInject(),
    chatPoller: ChatPoller = koinInject(),
    chatSender: ChatSender = koinInject()
) {
    val channels by chatManager.knownChannels.collectAsState()
    var activeChannel by rememberSaveable { mutableStateOf("clan") }
    val messages by chatManager.channelFlow(activeChannel).collectAsState()
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Start polling when this screen is active; stop when removed
    DisposableEffect(Unit) {
        chatPoller.setListener { chatManager.dispatch(it) }
        chatPoller.start()
        onDispose {
            chatPoller.clearListener()
            chatPoller.stop()
        }
    }

    // Auto-scroll to newest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Row(Modifier.fillMaxSize()) {
        // Channel sidebar
        LazyColumn(
            Modifier
                .width(120.dp)
                .fillMaxHeight()
                .padding(4.dp)
        ) {
            val channelList = (setOf("clan") + channels).toList().sorted()
            items(channelList) { channel ->
                Text(
                    text = "#$channel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (channel == activeChannel)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeChannel = channel }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        // Vertical divider (use Box since VerticalDivider may not be available)
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Messages + input
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(messages) { msg ->
                    val prefix = if (msg.isAction) "* ${msg.sender}" else "<${msg.sender}>"
                    Text(
                        text = "$prefix ${msg.content}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            HorizontalDivider()

            Row(
                Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("#$activeChannel") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotEmpty()) {
                            scope.launch {
                                chatSender.send(channel = activeChannel, message = text)
                            }
                            input = ""
                        }
                    }
                ) { Text("Send") }
            }
        }
    }
}
