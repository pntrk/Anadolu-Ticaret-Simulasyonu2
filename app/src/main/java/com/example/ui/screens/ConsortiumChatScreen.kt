package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ConsortiumChatMessage
import com.example.data.MegaProject
import com.example.ui.components.CurrencyText
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.viewmodel.GameIntent
import com.example.viewmodel.GameViewModel

/**
 * Konsorsiyum Ortaklar Canlı Sohbet Ekranı (ConsortiumChatScreen).
 * Supabase Free Plan'daki 200 eşzamanlı Realtime soket kotasını korumak için
 * "Just-in-Time" (JIT) yaşam döngüsü ile çalışır:
 * - DisposableEffect(projectId):
 *     - onEnter: startListeningToConsortiumChat (Soket açılır ve kanala phx_join atılır)
 *     - onDispose: stopListeningToConsortiumChat (Kanal anında unsubscribe edilir ve Job sonlandırılır)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsortiumChatScreen(
    projectId: String,
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var messageInput by remember { mutableStateOf("") }

    val chatMessagesMap by viewModel.consortiumChatMessages.collectAsStateWithLifecycle()
    val messages = chatMessagesMap[projectId] ?: emptyList()
    val megaProjects by viewModel.megaProjects.collectAsStateWithLifecycle()
    val currentProject = megaProjects.find { it.id == projectId }

    // JUST-IN-TIME (JIT) SOCKET YAŞAM DÖNGÜSÜ
    DisposableEffect(projectId) {
        viewModel.handleIntent(GameIntent.ListenToConsortiumChat(projectId))
        onDispose {
            viewModel.handleIntent(GameIntent.StopListeningToConsortiumChat(projectId))
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentProject?.consortiumName ?: "Konsorsiyum Sohbeti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Text(
                                    text = "🔴 CANLI JIT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF34D399),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Ortaklar Koordinasyon & Anlık İletişim",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0B111E)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Mesaj Listesi
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Henüz mesaj bulunmuyor 💬",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ortaklarınızla ilk iletişimi başlatın!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(msg = msg)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mesaj Gönderme Çubuğu
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = {
                            Text(
                                "Ortaklara mesaj yazın...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("consortium_chat_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val trimmed = messageInput.trim()
                                if (trimmed.isNotBlank()) {
                                    viewModel.handleIntent(GameIntent.SendConsortiumChatMessage(projectId, trimmed))
                                    messageInput = ""
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            val trimmed = messageInput.trim()
                            if (trimmed.isNotBlank()) {
                                viewModel.handleIntent(GameIntent.SendConsortiumChatMessage(projectId, trimmed))
                                messageInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = messageInput.isNotBlank(),
                        modifier = Modifier.testTag("consortium_chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = if (messageInput.isNotBlank()) ThemeNeonCyan else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}
