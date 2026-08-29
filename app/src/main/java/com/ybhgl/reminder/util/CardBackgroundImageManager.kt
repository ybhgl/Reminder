package com.ybhgl.reminder.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ybhgl.reminder.data.ReminderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.Locale

/**
 * 卡片背景图片管理器：
 * - 将用户选择的图片中心裁剪为卡片比例（1:1 正方形）
 * - 降采样 + JPEG 压缩后转存到应用私有目录，避免占用外部存储且不受原图删除影响
 * - 提供按文件名加载与删除能力（数据库仅存储文件名）
 */
object CardBackgroundImageManager {

    private const val DIR_NAME = "card_backgrounds"
    private const val JPEG_QUALITY = 85

    /** 解码结果内存缓存（按位图字节计，上限约 8MB）：避免同一图片反复解码，消除首帧 null 闪烁 */
    private val bitmapCache = object : android.util.LruCache<String, Bitmap>((8 * 1024 * 1024)) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private fun backgroundDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** 同步读取缓存中的位图（未命中返回 null，供 Composable 首帧直接使用） */
    fun cachedBitmap(fileName: String): Bitmap? =
        if (fileName.isEmpty()) null else bitmapCache.get(fileName)

    /** 将位图放入缓存 */
    private fun putCache(fileName: String, bitmap: Bitmap) {
        bitmapCache.put(fileName, bitmap)
    }

    /**
     * 解码图片 URI 为位图（降采样到 [maxSide] 以内），供裁剪对话框展示。
     */
    suspend fun decodeScaledBitmap(context: Context, uri: Uri, maxSide: Int = 1600): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

                val options = BitmapFactory.Options().apply {
                    inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
                }
                val decoded = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: return@withContext null

                val longest = maxOf(decoded.width, decoded.height)
                if (longest <= maxSide) return@withContext decoded
                val ratio = maxSide.toFloat() / longest
                val scaled = Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
                if (scaled != decoded) decoded.recycle()
                scaled
            } catch (_: Throwable) {
                null
            }
        }

    /**
     * 导入用户裁剪好的正方形位图：压缩后转存到应用私有目录。
     * @return 成功时返回文件名（用于持久化到 ReminderItem），失败返回 null
     */
    suspend fun importBitmap(context: Context, source: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = String.format(Locale.US, "bg_%d.jpg", System.currentTimeMillis())
            val target = File(backgroundDir(context), fileName)
            FileOutputStream(target).use { out ->
                source.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            fileName
        } catch (_: Throwable) {
            null
        }
    }

    /** 按文件名加载背景位图（带内存缓存），加载失败返回 null */
    suspend fun loadBitmap(context: Context, fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        if (fileName.isEmpty()) return@withContext null
        bitmapCache.get(fileName)?.let { return@withContext it }
        try {
            val file = File(backgroundDir(context), fileName)
            if (!file.exists()) return@withContext null
            BitmapFactory.decodeFile(file.absolutePath)?.also { putCache(fileName, it) }
        } catch (_: Throwable) {
            null
        }
    }

    /** 删除已转存的背景图片（旧图替换或恢复默认时调用） */
    suspend fun deleteImage(context: Context, fileName: String) = withContext(Dispatchers.IO) {
        if (fileName.isEmpty()) return@withContext
        try {
            bitmapCache.remove(fileName)
            File(backgroundDir(context), fileName).delete()
        } catch (_: Throwable) {
        }
    }

    /**
     * 备份收集：将所有被提醒项引用的背景图片编码为 Base64，
     * 随 JSON 备份文件一同导出，保证恢复时图片不丢失。
     * @return 文件名 -> Base64 内容（仅包含实际存在且被引用的图片）
     */
    suspend fun collectForBackup(context: Context, reminders: List<ReminderItem>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val referenced = reminders.map { it.cardBackgroundImagePath }.filter { it.isNotEmpty() }.distinct()
            val result = mutableMapOf<String, String>()
            for (name in referenced) {
                try {
                    val file = File(backgroundDir(context), name)
                    if (file.exists()) {
                        result[name] = Base64.getEncoder().encodeToString(file.readBytes())
                    }
                } catch (_: Throwable) {
                }
            }
            result
        }

    /**
     * 备份恢复：将 Base64 图片写回应用私有目录。
     * 已存在的同名文件跳过（智能合并时避免重复写盘）。
     */
    suspend fun restoreFromBackup(context: Context, images: Map<String, String>) = withContext(Dispatchers.IO) {
        for ((name, base64) in images) {
            try {
                val target = File(backgroundDir(context), name)
                if (target.exists()) continue
                target.writeBytes(Base64.getDecoder().decode(base64))
            } catch (_: Throwable) {
            }
        }
    }

    /** 清理孤儿图片：删除目录中未被任何提醒项引用的残留文件 */
    suspend fun pruneOrphans(context: Context, reminders: List<ReminderItem>) = withContext(Dispatchers.IO) {
        try {
            val referenced = reminders.map { it.cardBackgroundImagePath }.filter { it.isNotEmpty() }.toSet()
            backgroundDir(context).listFiles()?.forEach { file ->
                if (file.name !in referenced) file.delete()
            }
        } catch (_: Throwable) {
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (width / sample > maxSide * 2 || height / sample > maxSide * 2) {
            sample *= 2
        }
        return maxOf(1, sample)
    }
}
