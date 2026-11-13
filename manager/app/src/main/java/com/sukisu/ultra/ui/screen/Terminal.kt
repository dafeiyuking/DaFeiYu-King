package com.sukisu.ultra.ui.screen

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.sukisu.ultra.R
import com.sukisu.ultra.term.Ansi
import com.sukisu.ultra.term.RootShell
import com.sukisu.ultra.term.Scripts
import com.sukisu.ultra.ui.component.ScreenTopBar
import com.sukisu.ultra.ui.theme.getCardColors
import com.sukisu.ultra.ui.theme.getCardElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 大肥鱼 ROOT 终端：手机给了 root 就以 root 跑，没给就退回应用自己的用户。
 *
 * shell 是点按钮才开始启动的，不是一进页面就申请 root——打开一个标签页不应该弹权限框。
 * 后面没有伪终端，所以要画整个屏幕的程序（`top`、`vim`）没地方画；按行工作的都能用，
 * 包括等输入的程序。
 *
 * 上面那些脚本只导入一次就留在应用里，再运行点一下就行，不用重新挑文件。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun TerminalScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shell = remember { RootShell() }
    val output by shell.output.collectAsStateWithLifecycle()
    val root by shell.root.collectAsStateWithLifecycle()

    var started by rememberSaveable { mutableStateOf(false) }
    var line by rememberSaveable { mutableStateOf("") }
    var scripts by remember { mutableStateOf(Scripts.list(context)) }
    val pageScroll = rememberScrollState()
    val consoleScroll = rememberScrollState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
            Scripts.import(context, uri, name)
            scripts = Scripts.list(context)
        }
    }

    fun startShell() {
        started = true
        scope.launch { withContext(Dispatchers.IO) { shell.start() } }
    }

    fun run(command: String) {
        val text = command.trim()
        if (text.isEmpty()) {
            return
        }
        val needsStart = !started
        started = true
        scope.launch {
            // shell 还没起来时先把进程拉起来，再把这一行敲进去；直接 send 会丢。
            if (needsStart) {
                withContext(Dispatchers.IO) { shell.start() }
            }
            shell.note(text)
            shell.send(text)
        }
    }

    DisposableEffect(Unit) { onDispose { shell.stop() } }
    LaunchedEffect(output) {
        if (started) {
            consoleScroll.animateScrollTo(consoleScroll.maxValue)
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.terminal_title),
                onBack = { navigator.popBackStack() },
                actions = {
                    IconButton(onClick = { shell.clear() }) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = stringResource(R.string.terminal_clear),
                        )
                    }
                    IconButton(onClick = {
                        shell.stop()
                        startShell()
                    }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.terminal_restart),
                        )
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
            TerminalStatusCard(root = root, started = started)
            ScriptsCard(
                scripts = scripts,
                onImport = { picker.launch(arrayOf("*/*")) },
                onRun = { file -> run(Scripts.commandFor(file)) },
                onDelete = { file ->
                    Scripts.remove(file)
                    scripts = Scripts.list(context)
                },
            )
            if (started) {
                ConsoleCard(text = Ansi.render(output), scroll = consoleScroll)
                InputCard(
                    line = line,
                    onLine = { line = it },
                    onSend = {
                        run(line)
                        line = ""
                    },
                    onQuick = { command -> run(command) },
                )
            } else {
                StartCard(onStart = { startShell() })
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 顶部的状态卡：现在是不是 root，shell 起没起来。 */
@Composable
private fun TerminalStatusCard(root: Boolean?, started: Boolean) {
    val isRoot = root == true
    ElevatedCard(
        colors = getCardColors(
            if (isRoot) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = getCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isRoot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Icon(
                    Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = if (isRoot) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.terminal_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        !started -> stringResource(R.string.terminal_status_idle)
                        root == null -> stringResource(R.string.terminal_status_checking)
                        isRoot -> stringResource(R.string.terminal_status_root)
                        else -> stringResource(R.string.terminal_status_shell)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            val badge = when {
                !started -> null
                root == null -> "…"
                isRoot -> "ROOT"
                else -> "SHELL"
            }
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRoot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isRoot) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** 已导入的脚本：点“运行”就在下面的 shell 里跑起来。 */
@Composable
private fun ScriptsCard(
    scripts: List<File>,
    onImport: () -> Unit,
    onRun: (File) -> Unit,
    onDelete: (File) -> Unit,
) {
    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = getCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.terminal_scripts),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onImport) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.terminal_import))
            }
        }
        if (scripts.isEmpty()) {
            Text(
                text = stringResource(R.string.terminal_no_scripts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
        } else {
            scripts.forEachIndexed { index, file ->
                if (index > 0) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(file.name, style = MaterialTheme.typography.bodyMedium)
                    },
                    supportingContent = {
                        Text("${file.length() / 1024} KB", style = MaterialTheme.typography.bodySmall)
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onRun(file) }) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.terminal_run),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { onDelete(file) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.terminal_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 还没启动时显示的那张卡：说清楚为什么要手动启动。 */
@Composable
private fun StartCard(onStart: () -> Unit) {
    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = getCardElevation(),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.terminal_start_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStart) {
                Icon(Icons.Filled.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.terminal_start))
            }
        }
    }
}

/** shell 的输出：深色底、等宽字体，带 ANSI 颜色。 */
@Composable
private fun ConsoleCard(text: AnnotatedString, scroll: ScrollState) {
    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = getCardElevation(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.terminal_output),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0B1220))
                    .heightIn(min = 220.dp, max = 320.dp)
                    .padding(14.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll),
                )
            }
        }
    }
}

/** 一行命令的输入框，外加几个常用命令的快捷方式。 */
@Composable
private fun InputCard(
    line: String,
    onLine: (String) -> Unit,
    onSend: () -> Unit,
    onQuick: (String) -> Unit,
) {
    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = getCardElevation(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = line,
                onValueChange = onLine,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.terminal_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                trailingIcon = {
                    IconButton(onClick = onSend, enabled = line.isNotBlank()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.terminal_send),
                        )
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("id", "ls /data/adb", "df -h").forEach { quick ->
                    AssistChip(
                        onClick = { onQuick(quick) },
                        label = { Text(quick, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }
    }
}
