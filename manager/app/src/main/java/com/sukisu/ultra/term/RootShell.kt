package com.sukisu.ultra.term

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader

/**
 * 一个可以输入命令的 shell：手机允许时以 root 运行，不允许时退回应用自己的用户。
 *
 * shell 是一个进程，只要页面开着就一直活着，所以 `cd`、管道和变量能一行接一行地传下去，
 * 这也是这里“交互式”的意思。它没有伪终端：`top`、`vim` 这种要画整个屏幕的程序没法显示，
 * 会自己报错；其他都是按行工作的程序，包括等输入的那些。
 */
class RootShell {

    /** shell 启动以来打印的全部内容，最新的在最后。 */
    private val _output = MutableStateFlow("$ ")
    val output: StateFlow<String> = _output

    /** 确认是 root 时为 true，跑在应用身份下为 false，还在确认时为 null。 */
    private val _root = MutableStateFlow<Boolean?>(null)
    val root: StateFlow<Boolean?> = _root

    /** shell 是否还在等下一行输入。 */
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private val printed = StringBuilder()
    @Volatile private var alive = false

    /** 还没启动就启动；已经启动了就直接返回，所以每次显示页面调用都安全。 */
    fun start() {
        if (process != null) {
            return
        }
        printed.setLength(0)
        printed.append("$ ")
        _output.value = printed.toString()
        _root.value = null
        val started = spawn()
        if (started == null) {
            append("\n没有可用的 shell（su 和 sh 都启动失败）。\n")
            _root.value = false
            return
        }
        try {
            writer = started.outputStream.bufferedWriter()
        } catch (error: Throwable) {
            // 没法往 shell 里打字的话它就没用；说清楚原因，而不是直接崩。
            append("\n无法写入 shell 输入：${error.message ?: error.javaClass.simpleName}\n")
            runCatching { started.destroy() }
            _root.value = false
            return
        }
        process = started
        alive = true
        _running.value = true
        runCatching {
            Thread({ readLoop(started) }, "root-shell-reader").apply { isDaemon = true }.start()
        }.onFailure {
            append("\n无法读取 shell 输出：${it.message ?: it.javaClass.simpleName}\n")
        }
        // 返回 0 的 shell 就是 root，别的数字说明手机没给。
        send("id -u")
    }

    /** 往 shell 里敲一行。 */
    fun send(line: String) {
        val out = writer ?: return
        runCatching {
            out.write(line)
            out.write("\n")
            out.flush()
        }
    }

    /** 停掉 shell，连同里面正在跑的程序。 */
    fun stop() {
        alive = false
        runCatching { writer?.close() }
        runCatching { process?.destroy() }
        process = null
        writer = null
        _running.value = false
    }

    /** 由页面自己写一行，让用户看得见马上要跑的是哪条命令。 */
    fun note(text: String) {
        append("\n$ " + text + "\n")
    }

    /** 忘掉已经打印的内容，shell 本身留着。 */
    fun clear() {
        printed.setLength(0)
        printed.append("$ ")
        _output.value = printed.toString()
    }

    private fun readLoop(shell: Process) {
        val reader = runCatching { BufferedReader(InputStreamReader(shell.inputStream)) }.getOrNull()
        if (reader == null) {
            _running.value = false
            _root.value = false
            return
        }
        val chunk = CharArray(1024)
        try {
            while (alive) {
                val read = reader.read(chunk)
                if (read <= 0) {
                    continue
                }
                val text = String(chunk, 0, read)
                append(text)
                if (_root.value == null) {
                    val trimmed = printed.toString()
                    if (trimmed.contains("uid=0") || Regex("(^|\\n)0(\\r?\\n)").containsMatchIn(trimmed)) {
                        _root.value = true
                    }
                }
            }
        } catch (_: Exception) {
            if (alive) {
                append("\n[shell 已结束]\n")
            }
        } finally {
            _running.value = false
            // 进程结束了还没认出 root，就是没拿到（多半是权限被拒）。
            if (_root.value == null) {
                _root.value = false
            }
        }
    }

    /** 依次尝试几个 su 路径：KernelSU 只认 `/system/bin/su`，普通 su 走 PATH，最后退回 sh。 */
    private fun spawn(): Process? {
        for (command in SU_CANDIDATES) {
            val started = runCatching {
                ProcessBuilder(command).redirectErrorStream(true).start()
            }.getOrNull()
            if (started != null) {
                return started
            }
        }
        return null
    }

    /** 往已经打印的内容后面追加，同时把缓冲区压到手机画得动的大小。 */
    private fun append(text: String) {
        synchronized(printed) {
            printed.append(text)
            if (printed.length > LIMIT) {
                printed.delete(0, printed.length - LIMIT)
            }
            _output.value = printed.toString()
        }
        if (_root.value == null && text.isNotBlank()) {
            // `id -u` 会单独一行打印用户 id；root shell 打印的是 0。
            val line = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            if (line == "0") {
                _root.value = true
            } else if (line != null && line.all { it.isDigit() }) {
                _root.value = false
            }
        }
    }

    private companion object {
        /** 保留多少字符的 shell 输出：大概一屏的历史。 */
        const val LIMIT = 12_000

        /** 先试 KernelSU 的 sucompat 路径，再走 PATH 里的 su，最后退回普通 sh。 */
        val SU_CANDIDATES = listOf(
            "/system/bin/su",
            "su",
            "/system/xbin/su",
            "/data/adb/ksu/bin/su",
            "/system/bin/sh",
        )
    }
}
