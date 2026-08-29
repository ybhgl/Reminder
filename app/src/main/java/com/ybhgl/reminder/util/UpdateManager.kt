package com.ybhgl.reminder.util

import android.content.Context
import com.ybhgl.reminder.BuildConfig
import com.ybhgl.reminder.data.UpdatePreferences
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * GitHub Release 解析后的更新信息。
 */
@Serializable
data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val apkUrl: String?
) {
    val version: String get() = tagName.removePrefix("v").removePrefix("V")
    val downloadUrl: String get() = apkUrl ?: htmlUrl
}

@Serializable
private data class GitHubReleaseDto(
    val tag_name: String = "",
    val name: String = "",
    val body: String = "",
    val html_url: String = "",
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
private data class GitHubAssetDto(
    val name: String = "",
    val browser_download_url: String = ""
)

enum class UpdateCheckResult {
    UPDATE_AVAILABLE,
    LATEST,
    FAILED
}

/**
 * 应用更新检测：从 GitHub 获取最新 release，与本地版本号比较。
 * 通过 [StateFlow] 暴露状态，保证 UI 线程安全订阅。
 */
object UpdateManager {

    private const val OWNER = "ybhgl"
    private const val REPO = "Reminder"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val checkMutex = Mutex()

    private val _updateInfo = MutableStateFlow<ReleaseInfo?>(null)
    /** 检测到且未被忽略的新版本；null 表示无可用更新 */
    val updateInfo: StateFlow<ReleaseInfo?> = _updateInfo

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking

    /**
     * 执行版本检查：每次都实时请求 GitHub 最新 release，不做本地缓存。
     */
    suspend fun checkForUpdates(context: Context): UpdateCheckResult =
        checkMutex.withLock {
            _isChecking.value = true
            try {
                val release = try {
                    withContext(Dispatchers.IO) { parseRelease(fetchLatestRelease()) }
                } catch (e: Exception) {
                    null
                } ?: return@withLock UpdateCheckResult.FAILED

                applyRelease(context, release)
            } finally {
                _isChecking.value = false
            }
        }

    /** 用户点击"忽略"后调用：记录忽略版本并清除更新状态 */
    suspend fun ignoreCurrentUpdate(context: Context) {
        _updateInfo.value?.let { UpdatePreferences.saveIgnoredVersion(context, it.version) }
        _updateInfo.value = null
    }

    /** 实时读取当前安装的应用版本号 */
    fun currentVersion(context: Context): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: BuildConfig.VERSION_NAME
    } catch (e: Exception) {
        BuildConfig.VERSION_NAME
    }

    private suspend fun applyRelease(context: Context, release: ReleaseInfo): UpdateCheckResult {
        val ignored = UpdatePreferences.getIgnoredVersion(context)
        val hasNewVersion = compareVersions(currentVersion(context), release.version) < 0
        // 用户忽略某版本后，仅当云端版本高于该忽略版本时才重新提示
        val notIgnored = ignored == null || compareVersions(ignored, release.version) < 0
        if (hasNewVersion && notIgnored) {
            _updateInfo.value = release
            return UpdateCheckResult.UPDATE_AVAILABLE
        }
        _updateInfo.value = null
        return UpdateCheckResult.LATEST
    }

    private fun parseRelease(json: String): ReleaseInfo? = try {
        val dto = jsonParser.decodeFromString<GitHubReleaseDto>(json)
        if (dto.tag_name.isBlank()) null
        else ReleaseInfo(
            tagName = dto.tag_name,
            name = dto.name.ifBlank { dto.tag_name },
            body = dto.body,
            htmlUrl = dto.html_url,
            apkUrl = dto.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?.browser_download_url
        )
    } catch (e: Exception) {
        null
    }

    private fun fetchLatestRelease(): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "${BuildConfig.APPLICATION_ID}/update-check")
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub API 返回 $code")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 点分版本号比较（去掉 v 前缀，逐段按数值比较）。
     * 返回：local > remote 为正，local < remote 为负，相等为 0。
     */
    private fun compareVersions(local: String, remote: String): Int {
        val localParts = normalizeVersion(local).split(".")
        val remoteParts = normalizeVersion(remote).split(".")
        val maxLen = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val l = localParts.getOrNull(i)?.toLongOrNull() ?: 0L
            val r = remoteParts.getOrNull(i)?.toLongOrNull() ?: 0L
            if (l != r) return if (l > r) 1 else -1
        }
        return 0
    }

    private fun normalizeVersion(version: String): String =
        version.trim().removePrefix("v").removePrefix("V")
}
