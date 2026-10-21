package com.ybhgl.reminder.util

import android.content.Context
import com.ybhgl.reminder.BuildConfig
import com.ybhgl.reminder.data.getHolidayEmptyAttempts
import com.ybhgl.reminder.data.getHolidayTriggeredKeys
import com.ybhgl.reminder.data.holidayNetworkGrantedFlow
import com.ybhgl.reminder.data.increaseHolidayEmptyAttempts
import com.ybhgl.reminder.data.markHolidayUpdateTriggered
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 单个节假日条目（源自 NateScarlet/holiday-cn 年度 JSON 的 days 数组）。
 * isOffDay=true → 休（放假）；false → 班（调休补班）。
 */
data class HolidayDay(
    val name: String,
    val date: LocalDate,
    val isOffDay: Boolean
)

/** 一次节假日同步的结果：attempted=是否实际发起过网络请求；updatedYears=有新增条目的年份（升序）；failed=是否有源全部失败的年份 */
data class HolidaySyncResult(
    val attempted: Boolean,
    val updatedYears: List<Int>,
    val failed: Boolean
)

@Serializable
private data class HolidayYearDto(
    val year: Int = 0,
    val days: List<HolidayDayDto> = emptyList()
)

@Serializable
private data class HolidayDayDto(
    val name: String = "",
    val date: String = "",
    val isOffDay: Boolean = false
)

/**
 * 节假日数据管理器：从 NateScarlet/holiday-cn 仓库按年度下载 JSON，
 * 依次尝试 [YEAR_URL_TEMPLATES] 三个 CDN 源（gh-proxy.com → cdn.jsdelivr.net → raw.githubusercontent.com），
 * 成功后缓存到 `filesDir/holidays/{year}.json`，内存以 [holidayDays] StateFlow 供日历渲染订阅。
 *
 * 联网门禁：每次同步入口现查 `holiday_network_granted`，未授权绝不发起请求。
 */
object HolidayManager {

    /** 无本地数据时的回溯起始年 */
    private const val MIN_BACKFILL_YEAR = 2020

    /** 目标年"文件存在但数据为空"时每天最多重复下载次数（预检超限即不再发请求） */
    private const val MAX_EMPTY_ATTEMPTS_PER_DAY = 3

    /** 三个 CDN 源按优先级依次尝试（%d 为年份） */
    private val YEAR_URL_TEMPLATES = listOf(
        "https://gh-proxy.com/https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/%d.json",
        "https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/%d.json",
        "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/%d.json"
    )

    const val REPO_URL = "https://github.com/NateScarlet/holiday-cn"

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val syncMutex = Mutex()

    private val _holidayDays = MutableStateFlow<Map<LocalDate, HolidayDay>>(emptyMap())

    /** 全量节假日映射（date → 条目）；identity 变化即代表数据更新，可作网格重组 key */
    val holidayDays: StateFlow<Map<LocalDate, HolidayDay>> = _holidayDays

    @Volatile
    private var diskLoaded = false

    /** 读盘并刷新内存映射（幂等，仅首读生效；供 UI 离线渲染，不发任何网络请求） */
    suspend fun loadFromDisk(context: Context) {
        if (diskLoaded) return
        withContext(Dispatchers.IO) {
            val map = mutableMapOf<LocalDate, HolidayDay>()
            runCatching {
                holidayDir(context).listFiles { f -> f.name.endsWith(".json") }?.forEach { file ->
                    parseYearJson(file.readText())?.let { dto ->
                        dto.days.forEach { day ->
                            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@forEach
                            map[date] = HolidayDay(name = day.name, date = date, isOffDay = day.isOffDay)
                        }
                    }
                }
            }
            _holidayDays.value = map
            diskLoaded = true
        }
    }

    /**
     * 按触发时机同步节假日数据（进入日历页时调用）：
     * ① 本地完全无数据 → 回溯下载 [MIN_BACKFILL_YEAR] 至今全部年份；
     * ② 有数据但缺当年 → 补下当年；
     * ③ 每年 1月1日 / 11月15日 / 12月15日 → 拉取（下一）当年数据，成功后记录触发键防同日重复。
     */
    suspend fun syncIfNeeded(context: Context): HolidaySyncResult = syncMutex.withLock {
        // 联网门禁：未授权绝不发起任何网络请求
        val granted = holidayNetworkGrantedFlow(context).first()
        if (!granted) return@withLock HolidaySyncResult(attempted = false, updatedYears = emptyList(), failed = false)

        loadFromDisk(context)
        val today = LocalDate.now()
        val year = today.year
        val triggeredKeys = getHolidayTriggeredKeys(context)
        val current = _holidayDays.value.toMutableMap()
        val updatedYears = mutableListOf<Int>()
        var failed = false
        var changed = false

        if (current.isEmpty()) {
            // ① 完全无数据：回溯 2020 至今逐年下载（空数据年限流见 downloadYear）
            for (y in MIN_BACKFILL_YEAR..year) {
                val added = downloadYear(context, y, today, current)
                if (added == null) {
                    failed = true
                } else {
                    if (added > 0) updatedYears += y
                    changed = true
                }
            }
        } else if (current.keys.none { it.year == year }) {
            // ② 缺当年：补下当年
            val added = downloadYear(context, year, today, current)
            if (added == null) {
                failed = true
            } else {
                if (added > 0) updatedYears += year
                changed = true
            }
        }

        // ③ 日期触发：整组日期只在对应键未成功触发过时请求
        val trigger: Pair<String, Int>? = when {
            today.monthValue == 1 && today.dayOfMonth == 1 -> "$year@01-01" to year
            today.monthValue == 11 && today.dayOfMonth == 15 -> "${year + 1}@11-15" to year + 1
            today.monthValue == 12 && today.dayOfMonth == 15 -> "${year + 1}@12-15" to year + 1
            else -> null
        }
        if (trigger != null && trigger.first !in triggeredKeys) {
            val added = downloadYear(context, trigger.second, today, current)
            if (added == null) {
                failed = true
            } else {
                if (added > 0) updatedYears += trigger.second
                changed = true
                // 成功拿到有效数据才记触发键；空数据视为"该年未下载"，次日仍可再试
                markHolidayUpdateTriggered(context, trigger.first)
            }
        }

        if (changed) _holidayDays.value = current
        HolidaySyncResult(attempted = true, updatedYears = updatedYears.distinct().sorted(), failed = failed)
    }

    /**
     * 下载并合并单个年份；返回该年新增条数（0=数据相同或合法空），null=网络失败（调用方计失败）。
     * "文件存在但 days 为空"（Empty，如新一年的文件先建后填）：不落盘、不计失败、不提示，
     * 只累计当日空尝试次数，达 [MAX_EMPTY_ATTEMPTS_PER_DAY] 后预检直接跳过不再发请求。
     */
    private suspend fun downloadYear(
        context: Context,
        year: Int,
        today: LocalDate,
        target: MutableMap<LocalDate, HolidayDay>
    ): Int? {
        if (getHolidayEmptyAttempts(context, year, today) >= MAX_EMPTY_ATTEMPTS_PER_DAY) return 0
        return when (val result = fetchYear(year)) {
            is YearFetchResult.Fetched -> {
                if (result.dto.year != year && result.dto.year > 0) {
                    // 防御：代理/CDN 异常返回邻年文件时不落错缓存，按失败处理
                    null
                } else {
                    applyYearDto(context, target, result.dto, result.raw)
                }
            }
            YearFetchResult.Empty -> {
                increaseHolidayEmptyAttempts(context, year, today)
                0
            }
            YearFetchResult.Failed -> null
        }
    }

    /**
     * 年度数据落盘 + 合并进内存映射（整年替换避免旧条目残留）。
     * 返回新增条数（远端日期中本地原先不存在的部分）。
     */
    private suspend fun applyYearDto(
        context: Context,
        target: MutableMap<LocalDate, HolidayDay>,
        dto: HolidayYearDto,
        rawJson: String
    ): Int {
        val dates = dto.days.mapNotNull { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
            date to day
        }
        val newEntryCount = dates.count { (date, _) -> date != null && date !in target }
        val yearValue = dto.year.takeIf { it > 0 } ?: dates.firstNotNullOfOrNull { (date, _) -> date?.year }
        target.keys.removeAll { it.year == yearValue }
        dates.forEach { (date, day) ->
            if (date != null) target[date] = HolidayDay(name = day.name, date = date, isOffDay = day.isOffDay)
        }
        withContext(Dispatchers.IO) {
            runCatching {
                holidayDir(context).mkdirs()
                // 原样落盘源 JSON（保留 papers/$schema 等非 days 字段），解析仅用于内存映射
                File(holidayDir(context), "$yearValue.json").writeText(rawJson)
            }
        }
        return newEntryCount
    }

    private fun holidayDir(context: Context): File = File(context.filesDir, "holidays")

    /**
     * 清空全部已存节假日数据（磁盘按年缓存 + 内存映射）：
     * 撤销联网授权时由 saveHolidayNetworkGranted(false) 统一调用；
     * StateFlow 置为全新空 map，日历圆标/副标签立即回退无数据态。
     */
    suspend fun clearAll(context: Context) {
        withContext(Dispatchers.IO) {
            runCatching { holidayDir(context).deleteRecursively() }
        }
        _holidayDays.value = emptyMap()
        diskLoaded = true
    }

    /** 年度文件解析结果三态：有效数据 / 合法空文件（先建后填）/ 网络或解析失败 */
    private sealed interface YearFetchResult {
        data class Fetched(val dto: HolidayYearDto, val raw: String) : YearFetchResult
        data object Empty : YearFetchResult
        data object Failed : YearFetchResult
    }

    private fun parseYearJson(jsonText: String): HolidayYearDto? =
        runCatching { jsonParser.decodeFromString<HolidayYearDto>(jsonText) }.getOrNull()

    /**
     * 依次尝试三个 CDN 源：任一源返回可解析但 days 为空的 JSON → 继续试其余源，
     * 全部无有效数据时只要见过合法空即判 Empty（区别于 Failed，不触发失败提示）。
     */
    private suspend fun fetchYear(year: Int): YearFetchResult = withContext(Dispatchers.IO) {
        var sawEmpty = false
        for (template in YEAR_URL_TEMPLATES) {
            val text = runCatching { fetchText(String.format(template, year)) }.getOrNull() ?: continue
            val dto = parseYearJson(text) ?: continue
            if (dto.days.isEmpty()) {
                sawEmpty = true
                continue
            }
            return@withContext YearFetchResult.Fetched(dto, text)
        }
        if (sawEmpty) YearFetchResult.Empty else YearFetchResult.Failed
    }

    private fun fetchText(url: String): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "${BuildConfig.APPLICATION_ID}/holiday-update")
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP $code")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection?.disconnect()
        }
    }
}
