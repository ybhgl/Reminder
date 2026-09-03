package com.ybhgl.reminder.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份压缩包管理器：
 * - 打包格式：zip（metadata.json + images/ 目录下被引用的图片）
 * - 整包 AES 加密（BackupEncryptor.encryptBytes），落盘/上传前整体加密
 * - 与旧版"加密 JSON + Base64 图片"方案解耦，图片不再内联进 JSON
 */
object BackupArchiveManager {

    private const val METADATA_ENTRY = "metadata.json"
    private const val IMAGES_DIR = "images/"

    const val ZIP_MIME = "application/zip"
    const val BACKUP_EXTENSION = ".zip"

    /** 解析后的压缩包内容：metadata.json 文本 + 图片名 -> 字节 */
    data class ArchiveContent(
        val metadataJson: String,
        val images: Map<String, ByteArray>
    )

    /**
     * 打包：metadata.json + 图片文件，可选整包 AES 加密。
     * @param imageFiles 文件名 -> 图片文件（仅包含实际存在且被引用的图片）
     * @param encrypt true = 整包加密；false = 明文 zip（用户关闭"备份数据加密"时）
     * @return 备份包字节；失败返回 null
     */
    fun encode(metadataJson: String, imageFiles: Map<String, File>, encrypt: Boolean = true): ByteArray? {
        val zipBytes = try {
            ByteArrayOutputStream().use { byteOut ->
                ZipOutputStream(byteOut).use { zipOut ->
                    zipOut.putNextEntry(ZipEntry(METADATA_ENTRY))
                    zipOut.write(metadataJson.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()

                    for ((name, file) in imageFiles) {
                        if (name.isEmpty()) continue
                        // 统一放入 images/ 目录，防止路径分隔符异常
                        val entryName = IMAGES_DIR + name.substringAfterLast('/')
                        zipOut.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
                byteOut.toByteArray()
            }
        } catch (_: Throwable) {
            return null
        }
        return if (encrypt) BackupEncryptor.encryptBytes(zipBytes) else zipBytes
    }

    /**
     * 解析备份包：优先按加密包解密，失败则按明文 zip 处理（用户关闭加密时生成）。
     * @return 解析成功返回内容；解密/解包失败返回 null
     */
    fun decode(data: ByteArray): ArchiveContent? {
        val zipBytes = BackupEncryptor.decryptBytes(data) ?: data
        return parseZip(zipBytes)
    }

    private fun parseZip(zipBytes: ByteArray): ArchiveContent? {
        return try {
            var metadataJson: String? = null
            val images = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zipIn.readBytes()
                        when {
                            entry.name == METADATA_ENTRY -> metadataJson = bytes.toString(Charsets.UTF_8)
                            entry.name.startsWith(IMAGES_DIR) ->
                                images[entry.name.removePrefix(IMAGES_DIR).substringAfterLast('/')] = bytes
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            metadataJson?.let { ArchiveContent(it, images) }
        } catch (_: Throwable) {
            null
        }
    }
}
