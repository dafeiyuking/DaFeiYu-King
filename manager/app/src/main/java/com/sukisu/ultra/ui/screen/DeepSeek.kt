package com.sukisu.ultra.ui.screen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.sukisu.ultra.R
import com.sukisu.ultra.deepseek.DeepSeekClient
import com.sukisu.ultra.ui.component.ScreenTopBar
import com.sukisu.ultra.ui.theme.getCardColors
import com.sukisu.ultra.ui.theme.getCardElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 用户的 DeepSeek key 放在这个应用自己的存储里。 */
private const val PREFS = "deepseek"
private const val KEY_API = "api_key"

/**
 * 大肥鱼 DeepSeek 页：用自己的 key 登录、看账户还能花多少，然后和模型聊天。
 *
 * key 是用户自己的，输入一次就留在这台手机上。除了发给 DeepSeek 的 API，不往别处送；
 * 余额也来自这个 key 所属的账户。手机没有联网时，失败原因会直接显示出来。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DeepSeekScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(preferences.getString(KEY_API, "").orEmpty()) }
    var typedKey by rememberSaveable { mutableStateOf("") }
    var balance by remember { mutableStateOf<DeepSeekClient.Balance?>(null) }
    var balanceProblem by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    var messages by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var question by rememberSaveable { mutableStateOf("") }
    var asking by remember { mutableStateOf(false) }
    var chatProblem by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val pageScroll = rememberScrollState()

    fun loadBalance(key: String) {
        if (key.isBlank()) {
            return
        }
        checking = true
        balanceProblem = null
        scope.launch {
            try {
                balance = withContext(Dispatchers.IO) { DeepSeekClient.balance(key) }
            } catch (error: Exception) {
                balance = null
                balanceProblem = error.message ?: "?"
            } finally {
                checking = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (apiKey.isNotBlank() && balance == null) {
            loadBalance(apiKey)
        }
    }

    fun send() {
        val text = question.trim()
        if (text.isEmpty() || asking) {
            return
        }
        question = ""
        chatProblem = null
        val next = messages + ("user" to text)
        messages = next
        asking = true
        scope.launch {
            try {
                val answer = withContext(Dispatchers.IO) { DeepSeekClient.chat(apiKey, next) }
                messages = next + ("assistant" to answer)
            } catch (error: Exception) {
                chatProblem = error.message ?: "?"
                messages = next
            } finally {
                asking = false
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.deepseek_title),
                onBack = { navigator.popBackStack() },
                actions = {
                    if (messages.isNotEmpty()) {
                        TextButton(onClick = {
                            messages = emptyList()
                            chatProblem = null
                        }) {
                            Text(stringResource(R.string.deepseek_clear))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(pageScroll)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            if (apiKey.isBlank()) {
                LoginCard(
                    typedKey = typedKey,
                    checking = checking,
                    problem = balanceProblem,
                    onKey = { typedKey = it },
                    onSave = {
                        val key = typedKey.trim()
                        if (key.isNotBlank()) {
                            preferences.edit().putString(KEY_API, key).apply()
                            apiKey = key
                            loadBalance(key)
                        }
                    },
                )
            } else {
                AccountCard(
                    apiKey = apiKey,
                    balance = balance,
                    problem = balanceProblem,
                    checking = checking,
                    onRefresh = { loadBalance(apiKey) },
                    onLogout = {
                        preferences.edit().remove(KEY_API).apply()
                        apiKey = ""
                        balance = null
                        messages = emptyList()
                        chatProblem = null
                    },
                )
                ChatCard(
                    messages = messages,
                    question = question,
                    asking = asking,
                    problem = chatProblem,
                    onQuestion = { question = it },
                    onSend = { send() },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 还没有 key 时：一张登录卡，说清楚 key 存哪儿。 */
@Composable
private fun LoginCard(
    typedKey: String,
    checking: Boolean,
    problem: String?,
    onKey: (String) -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = getCardElevation(),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.deepseek_avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = stringResource(R.string.deepseek_signed_out),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.deepseek_login_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = typedKey,
                onValueChange = onKey,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.deepseek_key_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.deepseek_key_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (problem != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = problem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSave, enabled = typedKey.isNotBlank() && !checking) {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.deepseek_login))
            }
        }
    }
}

/** 登进去之后：账户、余额和退出。 */
@Composable
private fun AccountCard(
    apiKey: String,
    balance: DeepSeekClient.Balance?,
    problem: String?,
    checking: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = getCardElevation(),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.deepseek_avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.deepseek_account),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = mask(apiKey),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onLogout) {
                    Text(
                        text = stringResource(R.string.deepseek_logout),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.deepseek_balance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = balance?.let { "${it.currency} ${it.total}" } ?: "-",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = when {
                            problem != null -> problem
                            balance?.available == true -> stringResource(R.string.deepseek_available)
                            balance != null -> stringResource(R.string.deepseek_unavailable)
                            checking -> stringResource(R.string.deepseek_loading)
                            else -> stringResource(R.string.deepseek_loading)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (problem != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = onRefresh, enabled = !checking) {
                    if (checking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.deepseek_refresh))
                }
            }
        }
    }
}

/** 对话：最新的一条在下面，输入框在最下面。 */
@Composable
private fun ChatCard(
    messages: List<Pair<String, String>>,
    question: String,
    asking: Boolean,
    problem: String?,
    onQuestion: (String) -> Unit,
    onSend: () -> Unit,
) {
    val chatScroll = rememberScrollState()

    LaunchedEffect(messages.size, asking) {
        chatScroll.animateScrollTo(chatScroll.maxValue)
    }

    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = getCardElevation(),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.deepseek_chat_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = MODEL,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (messages.isEmpty()) {
                Text(
                    text = stringResource(R.string.deepseek_chat_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(chatScroll),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        messages.forEach { (role, text) -> Bubble(role, text) }
                    }
                }
            }
            if (asking) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.deepseek_thinking),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (problem != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = problem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = question,
                onValueChange = onQuestion,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.deepseek_chat_hint)) },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onSend, enabled = question.isNotBlank() && !asking) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.deepseek_send))
                }
            }
        }
    }
}

/** 一句话：用户说的在右边，模型说的在左边。 */
@Composable
private fun Bubble(role: String, text: String) {
    val mine = role == "user"
    val container = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(container)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** `sk-…abcd`：只露出足够认出是哪个 key 的几位。 */
private fun mask(key: String): String = when {
    key.length <= 10 -> key
    else -> key.take(6) + "…" + key.takeLast(4)
}

private const val MODEL = "deepseek-chat"
