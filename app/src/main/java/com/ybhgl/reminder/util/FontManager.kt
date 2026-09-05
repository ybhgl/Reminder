package com.ybhgl.reminder.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 用户导入字体管理器：
 * - 将用户通过系统文件选择器选择的字体文件原样转存到应用私有目录 fonts/ 子目录（不做转码）
 * - 数据库仅存储 "custom:<文件名>" 标识（ReminderItem.customFont / PersonalizationConfig.customFont）
 * - 导入的字体文件不进入应用内备份，也通过 backup_rules.xml / data_extraction_rules.xml 排除出系统备份
 */
object FontManager {

    private const val DIR_NAME = "fonts"

    /** 用户字体在 customFont 字段中的标识前缀（与内置字体名天然不冲突） */
    const val PREFIX = "custom:"

    /** 用户字体条目：落盘文件名 + 去扩展名的展示名 */
    data class UserFont(val fileName: String, val displayName: String)

    /** FontFamily 内存缓存：避免同一字体反复读盘解析 */
    private val familyCache = ConcurrentHashMap<String, FontFamily>()

    private fun fontsDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** 拼接 customFont 字段存储用的字体标识 */
    fun fontId(fileName: String): String = PREFIX + fileName

    /** 从 customFont 标识中提取落盘文件名（非用户字体返回 null） */
    fun fileNameOf(fontId: String): String? =
        if (fontId.startsWith(PREFIX)) fontId.removePrefix(PREFIX).takeIf { it.isNotEmpty() } else null

    /** 文件名去掉扩展名后的展示名 */
    fun displayName(fileName: String): String =
        fileName.substringBeforeLast('.').ifEmpty { fileName }

    /** 列出已导入的字体（按导入时间升序），供字体选择器渲染用户字体卡片 */
    suspend fun listFonts(context: Context): List<UserFont> = withContext(Dispatchers.IO) {
        try {
            fontsDir(context).listFiles()
                ?.filter { it.isFile }
                ?.sortedBy { it.lastModified() }
                ?.map { UserFont(it.name, displayName(it.name)) }
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /** 导入字体：从 Uri 原样复制到私有目录并校验魔数，成功返回 [UserFont]，失败返回 null */
    suspend fun importFont(context: Context, uri: Uri): UserFont? = withContext(Dispatchers.IO) {
        try {
            val sourceName = queryDisplayName(context, uri) ?: return@withContext null
            val fileName = sanitizeFileName(sourceName) ?: return@withContext null
            val target = uniqueTarget(fontsDir(context), fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null

            // 魔数校验或字体解析失败都回滚已落盘文件，不留下无效残留
            val family = createFamilySafely(target)
            if (family == null) {
                target.delete()
                return@withContext null
            }
            familyCache[target.name] = family
            UserFont(target.name, displayName(target.name))
        } catch (_: Throwable) {
            null
        }
    }

    /** 删除字体文件并使缓存失效；文件本就不存在视为成功，删除失败返回 false */
    suspend fun deleteFont(context: Context, fileName: String): Boolean = withContext(Dispatchers.IO) {
        if (fileName.isEmpty()) return@withContext false
        familyCache.remove(fileName)
        try {
            val file = File(fontsDir(context), fileName)
            !file.exists() || file.delete()
        } catch (_: Throwable) {
            false
        }
    }

    /** 按文件名解析 FontFamily（带缓存，同步），文件缺失或解析失败回落系统默认 */
    fun resolveFontFamily(context: Context, fileName: String): FontFamily {
        if (fileName.isEmpty()) return FontFamily.Default
        return familyCache.computeIfAbsent(fileName) { name ->
            try {
                val file = File(fontsDir(context), name)
                if (!file.exists()) return@computeIfAbsent FontFamily.Default
                createFamilySafely(file) ?: FontFamily.Default
            } catch (_: Throwable) {
                FontFamily.Default
            }
        }
    }

    /** 启动预热：把目录内全部字体提前解析进缓存，避免首次渲染时主线程读盘解析 */
    suspend fun preload(context: Context) = withContext(Dispatchers.IO) {
        try {
            fontsDir(context).listFiles()?.forEach { file ->
                if (file.isFile) resolveFontFamily(context, file.name)
            }
        } catch (_: Throwable) {
        }
    }

    /** 读取选择器返回文件的原始显示名 */
    private fun queryDisplayName(context: Context, uri: Uri): String? =
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (_: Throwable) {
            null
        }

    /** 清洗文件名：替换路径非法字符、去首尾空格与点、限制长度；清洗后无法命名返回 null */
    private fun sanitizeFileName(raw: String): String? {
        val cleaned = raw.replace(Regex("[\\\\/:*?\"<>|\n\r\t]"), "_").trim().trim('.')
        if (cleaned.isEmpty() || cleaned == "_") return null
        val dot = cleaned.lastIndexOf('.')
        val base = (if (dot > 0) cleaned.substring(0, dot) else cleaned).ifEmpty { return null }
        val ext = if (dot > 0) cleaned.substring(dot) else ""
        return base.take(60) + ext
    }

    /** 同名冲突时在扩展名前插入时间戳保证唯一 */
    private fun uniqueTarget(dir: File, fileName: String): File {
        val target = File(dir, fileName)
        if (!target.exists()) return target
        val dot = fileName.lastIndexOf('.')
        val unique = if (dot > 0) {
            "${fileName.substring(0, dot)}_${System.currentTimeMillis()}${fileName.substring(dot)}"
        } else {
            "${fileName}_${System.currentTimeMillis()}"
        }
        return File(dir, unique)
    }

    /** 解析字体文件为 FontFamily：先校验 ttf/ttc/otf 魔数，解析异常返回 null */
    private fun createFamilySafely(file: File): FontFamily? {
        return try {
            if (!isSupportedFontMagic(file)) return null
            FontFamily(Typeface.createFromFile(file))
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 校验字体文件头魔数（Typeface.createFromFile 仅支持 ttf/ttc/otf，woff/woff2 一并拒绝）：
     * 00 01 00 00 / "true" / "ttcf" / "OTTO"
     */
    private fun isSupportedFontMagic(file: File): Boolean = try {
        file.inputStream().use { input ->
            val header = ByteArray(4)
            var read = 0
            while (read < 4) {
                val n = input.read(header, read, 4 - read)
                if (n < 0) break
                read += n
            }
            if (read < 4) false else {
                val tag = String(header, Charsets.US_ASCII)
                tag == "OTTO" || tag == "true" || tag == "ttcf" ||
                    (header[0] == 0x00.toByte() && header[1] == 0x01.toByte() &&
                        header[2] == 0x00.toByte() && header[3] == 0x00.toByte())
            }
        }
    } catch (_: Throwable) {
        false
    }
}
