package com.sukisu.ultra.term

import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.charset.Charset

/**
 * 用户导入的 shell 脚本和小程序。
 *
 * 只复制一次到应用自己的存储里，之后直接运行，不用每次重新挑选。复制出来的文件会置上可执行位，
 * 而从系统选择器拿到的文件本身没有这个位。
 */
object Scripts {

    /** 文件属于哪一类，决定怎么运行它。 */
    private enum class Kind { SHELL, BINARY }

    /** 导入文件存放的目录，第一次用到时创建。 */
    fun folder(context: Context): File = File(context.filesDir, "scripts").apply { mkdirs() }

    /** 已导入的全部文件，按名字排序。 */
    fun list(context: Context): List<File> =
        folder(context).listFiles()?.filter { it.isFile }?.sortedBy { it.name.lowercase() }.orEmpty()

    /** 把选中的文件复制进来并置上可执行位。返回副本，读不到时返回 null。 */
    fun import(context: Context, uri: Uri, name: String?): File? {
        val target = uniqueTarget(context, name ?: "script.sh")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        }.isSuccess
        if (!copied) {
            target.delete()
            return null
        }
        target.setExecutable(true, false)
        return target
    }

    fun remove(file: File): Boolean = file.delete()

    /**
     * 在 root shell 里运行这个文件要敲的那一行。
     *
     * 运行之前先把文件变成 `sh` 读得懂的东西：从 Windows 存下来的脚本会带 BOM 或 Windows 换行，
     * `sh` 遇到这两种都会在第一行报语法错误。程序类的文件会先复制到 `/data/local/tmp`，
     * 因为 Android 不允许执行应用自己存储里的文件。
     *
     * 权限由 root shell 自己设（`chmod 777`），所以不管应用能设成什么，文件都跑得起来。
     */
    fun commandFor(file: File): String {
        val path = file.absolutePath.replace("\"", "\\\"")
        val quoted = "\"$path\""
        return when (kindOf(file)) {
            Kind.BINARY -> {
                val staged = "/data/local/tmp/" + file.name.replace("\"", "").replace(" ", "_")
                "cp $quoted \"$staged\" && chmod 777 \"$staged\" && \"$staged\"; echo \"[进程已结束 (error \$?)]\""
            }
            Kind.SHELL -> {
                makeReadable(file)
                "chmod 777 $quoted && sh $quoted; echo \"[进程已结束 (error \$?)]\""
            }
        }
    }

    /** 是程序、是脚本，还是 `sh` 马上要抱怨的东西。 */
    private fun kindOf(file: File): Kind {
        val head = runCatching {
            file.inputStream().use { stream ->
                val buffer = ByteArray(16)
                val read = stream.read(buffer)
                if (read <= 0) ByteArray(0) else buffer.copyOf(read)
            }
        }.getOrDefault(ByteArray(0))
        val elf = head.size >= 4 && head[0] == 0x7F.toByte() && head[1] == 'E'.code.toByte() &&
            head[2] == 'L'.code.toByte() && head[3] == 'F'.code.toByte()
        return if (elf) Kind.BINARY else Kind.SHELL
    }

    /**
     * 把文本文件重写成不带 BOM 的 UTF-8、Unix 换行，前提是它还不是这样：
     * Windows 上的脚本多半是带 BOM 的 UTF-16，`sh` 会把 UTF-16 字母的一个字节当成乱码。
     */
    private fun makeReadable(file: File) {
        if (file.length() > MAX_REWRITE) {
            return
        }
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return
        if (bytes.isEmpty()) {
            return
        }
        val text = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16LE"))
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16BE"))
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            else -> String(bytes, Charsets.UTF_8)
        }
        val fixed = text.replace("\r\n", "\n").replace('\r', '\n')
        if (fixed.toByteArray(Charsets.UTF_8).contentEquals(bytes)) {
            return
        }
        runCatching {
            file.writeBytes(fixed.toByteArray(Charsets.UTF_8))
            file.setExecutable(true, false)
        }
    }

    /** 一个还没被占用的名字，同一个文件导入两次会各留一份。 */
    private fun uniqueTarget(context: Context, name: String): File {
        val safe = name.substringAfterLast('/').substringAfterLast('\\').ifBlank { "script.sh" }
        val folder = folder(context)
        var candidate = File(folder, safe)
        var index = 1
        val stem = safe.substringBeforeLast('.', safe)
        val extension = safe.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        while (candidate.exists()) {
            candidate = File(folder, "$stem ($index)$extension")
            index++
        }
        return candidate
    }

    /** 比这还大的文件就不动了：不是文本脚本，或者不值得为了换行重写一遍。 */
    private const val MAX_REWRITE = 8L * 1024 * 1024
}
