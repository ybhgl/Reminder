package com.ybhgl.reminder.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ybhgl.reminder.data.ReminderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

/**
 * 用户导入字体管理器：
 * - 将用户通过系统文件选择器选择的字体文件原样转存到应用私有目录 fonts/ 子目录（不做转码）
 * - 数据库仅存储 "custom:<文件名>" 标识（ReminderItem.customFont / PersonalizationConfig.customFont）
 * - 备份策略：仅被提醒项使用的字体随应用备份 zip 打包（collectFontFiles，未使用的导入字体不打包）；
 *   原始字体目录通过 backup_rules.xml / data_extraction_rules.xml 排除出系统云备份
 */
object FontManager {

    private const val DIR_NAME = "fonts"

    /** 用户字体在 customFont 字段中的标识前缀（与内置字体名天然不冲突） */
    const val PREFIX = "custom:"

    /** 用户字体条目：落盘文件名 + 去扩展名的展示名 */
    data class UserFont(val fileName: String, val displayName: String)

    /** FontFamily 内存缓存：避免同一字体反复读盘解析 */
    private val familyCache = ConcurrentHashMap<String, FontFamily>()

    /**
     * 字重感知的 FontFamily 缓存（key = "文件名#字重"）：
     * 可变字体每个目标字重各持有一个经 FontVariation 实例化的 Font
     */
    private val weightedFamilyCache = ConcurrentHashMap<String, FontFamily>()

    /** wght 轴检测结果缓存（含"无轴"的 null 结果，用 HashMap 手动同步避免 ConcurrentHashMap 不存 null） */
    private val weightAxisCache = HashMap<String, FontWeightAxis?>()

    /**
     * 字体的 wght 可变轴信息（来自 fvar 表），静态字体为 null。
     * 可变字体可在 [min, max] 区间内任意实例化字重，默认实例字重为 [default]
     */
    data class FontWeightAxis(val min: Int, val default: Int, val max: Int) {
        /** fvar 存在且 wght 轴范围有效（max > min）才视为支持字重调整 */
        val isVariable: Boolean get() = max > min
    }

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
            // 顺带预热 wght 轴检测，避免首次渲染/打开设置面板时再读盘
            weightAxis(context, target.name)
            UserFont(target.name, displayName(target.name))
        } catch (_: Throwable) {
            null
        }
    }

    /** 删除字体文件并使缓存失效；文件本就不存在视为成功，删除失败返回 false */
    suspend fun deleteFont(context: Context, fileName: String): Boolean = withContext(Dispatchers.IO) {
        if (fileName.isEmpty()) return@withContext false
        familyCache.remove(fileName)
        weightedFamilyCache.keys.removeAll { it.startsWith("$fileName#") }
        synchronized(weightAxisCache) { weightAxisCache.remove(fileName) }
        try {
            val file = File(fontsDir(context), fileName)
            !file.exists() || file.delete()
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 备份收集：返回所有被提醒项引用（custom: 前缀）且实际存在的字体文件，
     * 仅包含实际存在的；未被任何卡片使用的导入字体不打包
     */
    suspend fun collectFontFiles(context: Context, reminders: List<ReminderItem>): Map<String, File> =
        withContext(Dispatchers.IO) {
            val referenced = reminders.map { fileNameOf(it.customFont) }.filterNotNull().distinct()
            val result = mutableMapOf<String, File>()
            for (name in referenced) {
                try {
                    val file = File(fontsDir(context), name)
                    if (file.exists()) result[name] = file
                } catch (_: Throwable) {
                }
            }
            result
        }

    /**
     * 备份恢复：将压缩包内 fonts/ 目录的字体字节写回应用私有目录。
     * 已存在的同名文件跳过（重复恢复幂等、不覆盖本地）；只取文件名部分防止路径逃逸。
     */
    suspend fun restoreFonts(context: Context, fonts: Map<String, ByteArray>) = withContext(Dispatchers.IO) {
        for ((name, bytes) in fonts) {
            if (name.isEmpty()) continue
            try {
                val safeName = name.substringAfterLast('/')
                val target = File(fontsDir(context), safeName)
                if (target.exists()) continue
                target.writeBytes(bytes)
                // 清掉恢复前可能残留的"文件缺失→Default"缓存，让下次解析走真实文件
                familyCache.remove(safeName)
                weightedFamilyCache.keys.removeAll { it.startsWith("$safeName#") }
                synchronized(weightAxisCache) { weightAxisCache.remove(safeName) }
            } catch (_: Throwable) {
            }
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

    /**
     * 按文件名 + 目标字重解析 FontFamily（带缓存，同步）：
     * - 可变字体（含 wght 轴）：把字重钳制到轴范围内，用 `FontVariation` 在 wght 轴上实例化对应字重，
     *   使 Compose 的 `FontWeight` 请求真正生效（单实例包装只会渲染默认字重）
     * - 静态字体：忽略字重，回落默认实例（Compose 无伪粗体/伪细体，档位请求不改变渲染）
     */
    fun resolveFontFamily(context: Context, fileName: String, weight: Int): FontFamily {
        if (fileName.isEmpty()) return FontFamily.Default
        val axis = weightAxis(context, fileName)
        if (axis == null || !axis.isVariable) return resolveFontFamily(context, fileName)
        val clamped = weight.coerceIn(axis.min, axis.max)
        return weightedFamilyCache.computeIfAbsent("$fileName#$clamped") {
            try {
                val file = File(fontsDir(context), fileName)
                if (!file.exists()) return@computeIfAbsent FontFamily.Default
                val fontWeight = FontWeight(clamped)
                FontFamily(
                    Font(
                        file = file,
                        weight = fontWeight,
                        style = FontStyle.Normal,
                        variationSettings = FontVariation.Settings(FontVariation.weight(clamped))
                    )
                )
            } catch (_: Throwable) {
                FontFamily.Default
            }
        }
    }

    /** 检测字体的 wght 可变轴（fvar 表解析，结果缓存）；静态字体或文件缺失返回 null */
    fun weightAxis(context: Context, fileName: String): FontWeightAxis? {
        if (fileName.isEmpty()) return null
        synchronized(weightAxisCache) {
            if (weightAxisCache.containsKey(fileName)) return weightAxisCache[fileName]
            val axis = try {
                parseWeightAxis(File(fontsDir(context), fileName))
            } catch (_: Throwable) {
                null
            }
            weightAxisCache[fileName] = axis
            return axis
        }
    }

    /** 启动预热：把目录内全部字体提前解析进缓存，避免首次渲染时主线程读盘解析 */
    suspend fun preload(context: Context) = withContext(Dispatchers.IO) {
        try {
            fontsDir(context).listFiles()?.forEach { file ->
                if (file.isFile) {
                    resolveFontFamily(context, file.name)
                    weightAxis(context, file.name)
                }
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

    /**
     * 解析 sfnt 字体（ttf/otf/ttc）的 fvar 表，提取 wght 可变轴范围；无 fvar 表或无 wght 轴返回 null。
     * 只做表头级 seek 读取（表目录 + 轴记录），不加载字体数据体。
     * 参考格式：fvar 头 16 字节（axesArrayOffset@4、axisCount@8、axisSize@10），
     * 每条轴记录 20 字节（tag@0、minValue@4、defaultValue@8、maxValue@12，Fixed 16.16）
     */
    private fun parseWeightAxis(file: File): FontWeightAxis? {
        if (!file.exists()) return null
        RandomAccessFile(file, "r").use { raf ->
            val head = ByteArray(12)
            raf.readFully(head)
            var numTables = readUShort(head, 4)
            var dirOffset = 12L
            // TTC（字体集合）：取第一个 face 的偏移，再解析该 face 自己的表目录
            if (String(head, 0, 4, Charsets.US_ASCII) == "ttcf") {
                raf.seek(12) // 跳过 ttcf 版本(4) + numFonts(4)
                val firstFace = ByteArray(4)
                raf.readFully(firstFace)
                val faceOffset = readUInt(firstFace, 0)
                raf.seek(faceOffset + 4) // 跳过 face sfnt 版本
                val nt = ByteArray(2)
                raf.readFully(nt)
                numTables = readUShort(nt, 0)
                dirOffset = faceOffset + 12L
            }
            val dir = ByteArray(numTables * 16)
            raf.seek(dirOffset)
            raf.readFully(dir)
            var fvarOffset = -1L
            for (i in 0 until numTables) {
                val base = i * 16
                if (String(dir, base, 4, Charsets.US_ASCII) == "fvar") {
                    fvarOffset = readUInt(dir, base + 8).toLong()
                    break
                }
            }
            if (fvarOffset < 0) return null
            val fvarHead = ByteArray(16)
            raf.seek(fvarOffset)
            raf.readFully(fvarHead)
            // fvar 头布局：major@0 minor@2 axesArrayOffset@4 reserved@6 axisCount@8 axisSize@10
            val axesArrayOffset = readUShort(fvarHead, 4)
            val axisCount = readUShort(fvarHead, 8)
            val axisSize = readUShort(fvarHead, 10)
            if (axisCount <= 0 || axisSize < 20) return null
            val axes = ByteArray(axisCount * axisSize)
            raf.seek(fvarOffset + axesArrayOffset)
            raf.readFully(axes)
            for (i in 0 until axisCount) {
                val base = i * axisSize
                if (String(axes, base, 4, Charsets.US_ASCII) == "wght") {
                    fun fixed(off: Int): Int = readInt(axes, base + off) / 65536
                    return FontWeightAxis(fixed(4), fixed(8), fixed(12))
                }
            }
            return null
        }
    }

    /** 大端 uint16 */
    private fun readUShort(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    /** 大端 int32（Fixed 16.16 的高低位读取共用） */
    private fun readInt(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 24) or ((b[off + 1].toInt() and 0xFF) shl 16) or
            ((b[off + 2].toInt() and 0xFF) shl 8) or (b[off + 3].toInt() and 0xFF)

    /** 大端 uint32 */
    private fun readUInt(b: ByteArray, off: Int): Long = readInt(b, off).toLong() and 0xFFFFFFFFL
}
