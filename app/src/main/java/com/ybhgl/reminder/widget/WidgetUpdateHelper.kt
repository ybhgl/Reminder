package com.ybhgl.reminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.ybhgl.reminder.MainActivity
import com.ybhgl.reminder.R
import com.ybhgl.reminder.ReminderApplication
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.common.CardBackgroundSpec
import com.ybhgl.reminder.ui.common.CardBackgroundType
import com.ybhgl.reminder.ui.common.bitmapAverageColor
import com.ybhgl.reminder.ui.common.cardBackgroundSpec
import com.ybhgl.reminder.ui.common.numberEffectSpec
import com.ybhgl.reminder.ui.common.resolveCardBackgroundForeground
import com.ybhgl.reminder.ui.common.resolveEffectiveFontEffect
import com.ybhgl.reminder.util.CalendarUtil
import com.ybhgl.reminder.util.CardBackgroundImageManager
import com.ybhgl.reminder.util.FontManager
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min

data class WidgetDisplayInfo(
    val title: String,
    val label: String,
    val days: String,
    val unit: String,
    val dateString: String,
    val accentColorResId: Int
)

object WidgetUpdateHelper {

    fun getFeaturedReminder(items: List<ReminderItem>): ReminderItem? {
        if (items.isEmpty()) return null
        val pinned = items.filter { it.isPinned }
        if (pinned.isNotEmpty()) {
            return pinned.first()
        }
        val today = LocalDate.now()
        val upcoming = items.filter { it.type != ReminderType.COUNT_UP }
            .mapNotNull { item ->
                // 区间事件（endDate != null）按阶段取关键日：进行中=结束日，避免精选漏掉进行中的事件
                val keyDate = CalendarUtil.calculateNextKeyDate(item, today)
                if (keyDate != null) {
                    item to ChronoUnit.DAYS.between(today, keyDate)
                } else {
                    null
                }
            }
            .sortedBy { it.second }

        if (upcoming.isNotEmpty()) {
            return upcoming.first().first
        }
        return items.firstOrNull()
    }

    fun getDisplayInfo(context: Context, reminder: ReminderItem): WidgetDisplayInfo {
        val today = LocalDate.now()
        val title = reminder.title
        var label = "还有"
        var days = "0"
        var unit = "天"
        var dateString = ""

        val accentColorResId = when (reminder.type) {
            ReminderType.ANNUAL -> R.color.widget_accent_annual
            ReminderType.COUNT_UP -> R.color.widget_accent_count_up
            ReminderType.BIRTHDAY -> R.color.widget_accent_birthday
        }

        when (reminder.type) {
            ReminderType.ANNUAL -> {
                val stage = CalendarUtil.resolveIntervalStage(reminder, today)
                if (stage != null) {
                    // 区间事件：还有x天 → 就是今天 → 第x天 → 已过x天；有重复则周期结束后进入下一周期
                    val (stageLabel, stageDays, keyDate) = stage
                    label = stageLabel
                    if (stageLabel == "就是") {
                        days = "今"
                        unit = ""
                    } else {
                        days = stageDays.toString()
                    }
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(keyDate) else keyDate.toString()
                } else {
                val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
                if (nextDate == null) {
                    val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                    label = "已过"
                    days = daysPassed.toString()
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(reminder.date) else reminder.date.toString()
                } else {
                    val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
                    if (daysRemaining == 0) {
                        label = "就是"
                        days = "今"
                        unit = ""
                    } else {
                        label = "还有"
                        days = daysRemaining.toString()
                    }
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(nextDate) else nextDate.toString()
                }
                }
            }

            ReminderType.COUNT_UP -> {
                val isIncludeStartDay = reminder.notificationConfig.includeStartDay
                val daysElapsed = if (isIncludeStartDay) {
                    ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0) + 1
                } else {
                    ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                }
                label = "第"
                days = daysElapsed.toString()
                dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(reminder.date) else reminder.date.toString()
            }

            ReminderType.BIRTHDAY -> {
                val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
                if (nextDate == null) {
                    val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                    label = "生日已过"
                    days = daysPassed.toString()
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(reminder.date) else reminder.date.toString()
                } else {
                    val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
                    if (daysRemaining == 0) {
                        label = "生日就是"
                        days = "今"
                        unit = ""
                    } else {
                        label = "生日还有"
                        days = daysRemaining.toString()
                    }
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(nextDate) else nextDate.toString()
                }
            }
        }

        return WidgetDisplayInfo(
            title = title,
            label = label,
            days = days,
            unit = unit,
            dateString = dateString,
            accentColorResId = accentColorResId
        )
    }

    fun applyWidgetOpacity(context: Context, views: RemoteViews, bgViewId: Int, appWidgetId: Int) {
        val opacity = WidgetConfigStore.getWidgetOpacity(context, appWidgetId)
        val alpha = (opacity * 255) / 100
        views.setInt(bgViewId, "setImageAlpha", alpha)
    }

    /**
     * 根据小组件可用空间和内容长度计算响应式字号（2x2 专用）
     */
    fun getResponsiveDaysTextSize(
        context: Context,
        days: String,
        unit: String,
        widgetWidthDp: Float,
        widgetHeightDp: Float
    ): Float {
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        
        val widgetWidthPx = widgetWidthDp * density
        val widgetHeightPx = widgetHeightDp * density
        
        val headerHeightPx = 15f * scaledDensity + 8f * density * 2
        val footerHeightPx = 13f * scaledDensity + 8f * density * 2
        val dividerHeightPx = 0.6f * density
        val bodyPaddingVerticalPx = 4f * density * 2
        val bodyPaddingHorizontalPx = 8f * density * 2
        
        val availableHeightPx = widgetHeightPx - headerHeightPx - footerHeightPx - dividerHeightPx - bodyPaddingVerticalPx
        val availableWidthPx = widgetWidthPx - bodyPaddingHorizontalPx
        
        if (availableHeightPx <= 0 || availableWidthPx <= 0) {
            return 24f
        }
        
        val daysLen = days.length.coerceAtLeast(1)
        val unitLen = unit.length.coerceAtLeast(0)
        val charWidthFactor = daysLen * 0.6f + unitLen * 1.0f
        val spacingPx = 4f * density
        
        val maxByHeightPx = availableHeightPx / 1.2f
        val maxByHeightSp = maxByHeightPx / scaledDensity
        
        val maxByWidthPx = (availableWidthPx - spacingPx) / charWidthFactor
        val maxByWidthSp = maxByWidthPx / scaledDensity
        
        // 上限 140sp：2x2 时高/宽约束本身已把字号压在 ~80sp 内，
        // 放宽仅为用户手动拉大（如 4x4）后数字能继续放大填充
        return (min(maxByHeightSp, maxByWidthSp) * 0.9f).coerceAtMost(140f)
    }

    /**
     * 根据小组件可用空间和内容长度计算响应式字号（1x2 专用）
     */
    fun getResponsiveDaysTextSize1x2(
        context: Context,
        days: String,
        unit: String,
        widgetWidthDp: Float,
        widgetHeightDp: Float
    ): Float {
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        
        val widgetWidthPx = widgetWidthDp * density
        val widgetHeightPx = widgetHeightDp * density
        
        // 1x2 padding: 6dp*2 垂直, 12dp*2 水平, 右侧 margin 8dp
        val paddingVerticalPx = 6f * density * 2
        val paddingHorizontalPx = 12f * density * 2
        val rightMarginPx = 8f * density
        
        val availableHeightPx = widgetHeightPx - paddingVerticalPx
        val availableWidthPx = (widgetWidthPx - paddingHorizontalPx - rightMarginPx) * 0.5f
        
        if (availableHeightPx <= 0 || availableWidthPx <= 0) {
            return 24f
        }
        
        val daysLen = days.length.coerceAtLeast(1)
        val unitLen = unit.length.coerceAtLeast(0)
        val charWidthFactor = daysLen * 0.6f + unitLen * 1.0f
        val spacingPx = 2f * density
        
        val maxByHeightSp = availableHeightPx / (1.2f * scaledDensity)
        val maxByWidthSp = (availableWidthPx - spacingPx) / (charWidthFactor * scaledDensity)
        
        return (min(maxByHeightSp, maxByWidthSp) * 0.85f).coerceAtMost(48f)
    }

    /**
     * 获取小组件的尺寸（dp）
     */
    fun getWidgetCellSize(appWidgetManager: AppWidgetManager, appWidgetId: Int): Pair<Float, Float> {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220).toFloat()
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 220).toFloat()
        return Pair(widthDp, heightDp)
    }

    /**
     * 将 2x2 卡片容器钳制为正方形（2x2 专用）。
     *
     * 部分 OEM 桌面（MIUI/EMUI/ColorOS 等）网格单元格高>宽，2x2 区域天然呈长方形，
     * App 无法改变桌面给定的边界，只能把卡片钳成 side=min(宽,高) 的正方形并居中透出壁纸。
     * - API 31+：setViewLayoutWidth/Height 精确钳制；
     * - API 28~30：反射 setMinimumWidth/Height 保证下限，高度由响应式字号撑到接近方形（近似方形）。
     *
     * @return 正方形边长（dp）；0 = 拿不到有效尺寸（保持铺满的旧行为）
     */
    fun apply2x2SquareCard(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): Float {
        val (widthDp, heightDp) = getWidgetCellSize(appWidgetManager, appWidgetId)
        val sideDp = min(widthDp, heightDp)
        if (sideDp <= 0f) return 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewLayoutWidth(R.id.widget_2x2_card, sideDp, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(R.id.widget_2x2_card, sideDp, TypedValue.COMPLEX_UNIT_DIP)
        }
        val sidePx = (sideDp * context.resources.displayMetrics.density).toInt()
        views.setInt(R.id.widget_2x2_card, "setMinimumWidth", sidePx)
        views.setInt(R.id.widget_2x2_card, "setMinimumHeight", sidePx)
        return sideDp
    }

    /**
     * 2x2 小组件"跟随个性化设置"渲染（开关开启且事件 isCustomized 时调用，覆盖默认配色）。
     *
     * RemoteViews 限制：自定义字体文件/字重与 BLUR/GLASS 数字特效无法还原；
     * 可还原项：标头色与标题自适应文字色、卡片背景色/背景图（圆角位图）、
     * 正文文字色（自动反色/手动 WHITE/BLACK）、SOLID/MIXED 字体效果的颜色层。
     * 有自定义背景时同步首页卡片口径：隐藏标头色块与分割线，标题改用正文前景色。
     */
    private fun apply2x2Personalization(
        context: Context,
        views: RemoteViews,
        reminder: ReminderItem,
        sideDp: Float,
        daysText: String,
        daysTextSizeSp: Float,
        unitTextSizeSp: Float,
        accentColor: Int
    ) {
        // 1. 卡片背景：色块走底层 shape 的 setColorFilter（每轮必发默认值，无残留问题）；
        //    图片走独立 widget_2x2_bg_image（VISIBLE 覆盖默认 GONE），避免位图 src 残留。
        //    同时得到亮度与平均色供文字反色；加载失败回退默认白底。
        val bgSpec = reminder.cardBackgroundSpec
        val density = context.resources.displayMetrics.density
        val sidePx = (sideDp * density).toInt().coerceAtLeast(1)
        var effectiveSpec: CardBackgroundSpec? = null
        var bgAverage = Color(0xFF808080)
        var bgLuminance = 0.5f
        when (bgSpec.type) {
            CardBackgroundType.COLOR -> {
                effectiveSpec = bgSpec
                bgAverage = bgSpec.color
                bgLuminance = bgSpec.color.luminance()
                views.setInt(R.id.widget_2x2_bg, "setColorFilter", bgSpec.color.toArgb())
            }

            CardBackgroundType.IMAGE -> {
                val src = CardBackgroundImageManager.loadBitmapSync(context, bgSpec.imagePath)
                if (src != null) {
                    effectiveSpec = bgSpec
                    bgAverage = bitmapAverageColor(src)
                    bgLuminance = bgAverage.luminance()
                    // 背景图模糊（cardBackgroundBlurRadius）：先降采样近似高斯，再 center-crop+圆角，
                    // 与首页"模糊图→裁切"层级一致
                    val blurred = blurBitmapApprox(src, bgSpec.blurRadius * density)
                    views.setImageViewBitmap(R.id.widget_2x2_bg_image, roundedWidgetBitmap(blurred, sidePx, 16f * density))
                    if (blurred !== src) blurred.recycle()
                    setViewVisibilityCompat(views, R.id.widget_2x2_bg_image, true)
                }
            }

            else -> Unit
        }

        // 2. 字体效果颜色：与首页/详情同一纯函数链路（BLUR/GLASS 仅取颜色层，特效本身不可还原）
        val resolved = resolveEffectiveFontEffect(
            spec = reminder.numberEffectSpec,
            backgroundSpec = effectiveSpec,
            bgLuminance = bgLuminance,
            bgAverageColor = bgAverage
        )
        val foreground = resolved.cardTextColor
            ?: effectiveSpec?.let { resolveCardBackgroundForeground(it, bgLuminance) }
        val numberColor = resolved.numberColor ?: foreground
        val secondaryColor = resolved.secondaryTextColor ?: foreground?.copy(alpha = 0.92f)

        // 3. 标头与分割线：有自定义背景时隐藏标头色块与分割线、标题用正文前景色
        //    （对齐首页卡片 hasCustomBackground 口径）；无自定义背景时个性化标头色优先，
        //    标题按标头色明暗自适应白/深（阈值与首页 headerContentColorFor 一致）
        if (effectiveSpec != null) {
            setViewVisibilityCompat(views, R.id.widget_2x2_header_bg, false)
            setViewVisibilityCompat(views, R.id.widget_2x2_divider, false)
            foreground?.let { views.setTextColor(R.id.widget_2x2_header_title, it.toArgb()) }
        } else if (reminder.customHeaderColor.isNotEmpty()) {
            try {
                val header = android.graphics.Color.parseColor(reminder.customHeaderColor)
                views.setInt(R.id.widget_2x2_header_bg, "setColorFilter", header)
                val headerLum = Color(header).luminance()
                views.setTextColor(
                    R.id.widget_2x2_header_title,
                    if (headerLum > 0.55f) android.graphics.Color.parseColor("#1D1B20")
                    else android.graphics.Color.WHITE
                )
            } catch (_: Exception) {
            }
        }

        // 4. 数字/副文字颜色
        numberColor?.let { views.setTextColor(R.id.widget_2x2_days, it.toArgb()) }
        if (secondaryColor != null) {
            views.setTextColor(R.id.widget_2x2_unit, secondaryColor.toArgb())
            views.setTextColor(R.id.widget_2x2_date, secondaryColor.toArgb())
        }

        // 5. 自定义字体/字重：RemoteViews 无法设 Typeface，数字自绘位图覆盖层
        //    （与背景图同一显式切换模式；渲染失败自动回落 TextView 路径。
        //    BLUR/GLASS 特效已按用户决定回落普通填色，不再走位图触发）
        val needsCustomNumber = reminder.customFont.isNotEmpty() ||
            reminder.customFontWeight.roundToInt() != 700
        if (needsCustomNumber && daysText.isNotEmpty()) {
            val numberArgb = (numberColor ?: Color(accentColor)).toArgb()
            render2x2NumberBitmap(context, reminder, daysText, daysTextSizeSp, numberArgb, unitTextSizeSp)?.let { bmp ->
                views.setImageViewBitmap(R.id.widget_2x2_days_image, bmp)
                setViewVisibilityCompat(views, R.id.widget_2x2_days_image, true)
                setViewVisibilityCompat(views, R.id.widget_2x2_days, false)
            }
        }
    }

    /** RemoteViews.setViewVisibility 是 API 31 方法（28~30 直接崩溃），统一走 View.setVisibility(int) 反射 */
    private fun setViewVisibilityCompat(views: RemoteViews, viewId: Int, visible: Boolean) {
        views.setInt(viewId, "setVisibility", if (visible) View.VISIBLE else View.GONE)
    }

    /**
     * 文字尺寸下发：API 33+ 用专用 action `setTextViewTextSize`——
     * 部分 OEM 桌面（实测 HyperOS）会静默丢弃通用反射 setFloat("setTextSize")，
     * 导致 resize 后位图数字跟随尺寸、TextView 文字却纹丝不动；低版本无此问题保持反射兜底。
     */
    private fun setTextSizeCompat(views: RemoteViews, viewId: Int, sizeSp: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        } else {
            views.setFloat(viewId, "setTextSize", sizeSp)
        }
    }

    /**
     * 解析数字的 android.graphics.Typeface（与 FontManager 的 Compose 解析同口径）：
     * - 导入字体：可变字体按 wght 轴实例化目标字重（钳制到轴范围）；静态字体回落默认实例；
     * - 内置字体（含 "Default"）：用静态 Typeface 常量做基字族（Typeface.Builder(family) 在部分
     *   OEM 上解析不到裸族名，实测 HyperOS 不生效），字重走 Typeface.create(base, weight, italic)
     *   的真实变体匹配（Condensed 固定字重自然回落）。null = 渲染层用 DEFAULT_BOLD。
     */
    private fun resolveWidgetNumberTypeface(context: Context, reminder: ReminderItem): Typeface? {
        val font = reminder.customFont
        val weight = reminder.customFontWeight.coerceIn(100f, 900f).roundToInt()
        return try {
            val fileName = FontManager.fileNameOf(font)
            if (fileName != null) {
                val file = File(context.filesDir, "fonts/$fileName")
                if (!file.exists()) return null
                val axis = FontManager.weightAxis(context, fileName)
                Typeface.Builder(file).apply {
                    if (axis != null && axis.isVariable) {
                        val w = weight.coerceIn(axis.min, axis.max)
                        setWeight(w)
                        setFontVariationSettings("'wght' $w")
                    }
                }.build()
            } else {
                val base = when (font) {
                    "Serif" -> Typeface.SERIF
                    "Monospace" -> Typeface.MONOSPACE
                    "Cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                    "SansSerif-Condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                    // "Default"/"SansSerif"/"" 一律走系统无衬线基字族
                    else -> Typeface.DEFAULT
                }
                Typeface.create(base, weight, false)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 自绘 2x2 大数字位图（自定义字体/字重落地通道）。
     * 基线对齐口径（对齐首页 Row 的 alignByBaseline）：行容器 gravity=bottom 按视图底边贴线，
     * 因此位图在基线以下预留的高度必须等于同行"天"TextView 基线以下的高度（=天字号行盒的 fm.bottom），
     * 位图在基线以上的高度用数字自身行盒的 -fm.top——两个字号不同，若各用各的行盒底边必然错位。
     * BLUR/GLASS 字体特效在小组件观感差，已按用户决定自动回落为普通填色（2026-11-03）。
     */
    private fun render2x2NumberBitmap(
        context: Context,
        reminder: ReminderItem,
        text: String,
        textSizeSp: Float,
        colorArgb: Int,
        unitTextSizeSp: Float
    ): Bitmap? {
        return try {
            val density = context.resources.displayMetrics.density
            val scaledDensity = context.resources.displayMetrics.scaledDensity
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = textSizeSp * scaledDensity
                typeface = resolveWidgetNumberTypeface(context, reminder) ?: Typeface.DEFAULT_BOLD
                color = colorArgb
            }
            // "天"TextView 同款度量：默认字族常规字重、unitTextSizeSp 字号
            val unitFm = Paint().apply { textSize = unitTextSizeSp * scaledDensity }.fontMetrics
            val numberFm = fill.fontMetrics

            val baselineY = -numberFm.top
            val bottomPad = unitFm.bottom.coerceAtLeast(0f)
            val padH = 2f * density
            val w = (ceil(fill.measureText(text)) + padH * 2).toInt().coerceAtLeast(1)
            val h = (ceil(baselineY + bottomPad)).toInt().coerceAtLeast(1)
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(out).drawText(text, w / 2f, baselineY, fill)
            out
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 近似高斯模糊（背景图 cardBackgroundBlurRadius 落地）：双线性降采样再还原原尺寸，
     * 半径越大降得越狠。软件 Canvas 无 RenderEffect，两级缩放对小组件背景观感足够。
     */
    private fun blurBitmapApprox(src: Bitmap, radiusPx: Float): Bitmap {
        if (radiusPx <= 0.5f) return src
        val factor = (1f / (1f + radiusPx / 8f)).coerceAtLeast(0.08f)
        val sw = (src.width * factor).toInt().coerceAtLeast(1)
        val sh = (src.height * factor).toInt().coerceAtLeast(1)
        val down = Bitmap.createScaledBitmap(src, sw, sh, true)
        val blurred = Bitmap.createScaledBitmap(down, src.width, src.height, true)
        if (down !== src) down.recycle()
        return blurred
    }

    /** 将位图 center-crop 到 sidePx 方形并裁 16dp 圆角（小组件个性化背景图自绘） */
    private fun roundedWidgetBitmap(src: Bitmap, sidePx: Int, radiusPx: Float): Bitmap {
        val out = Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val path = Path().apply {
            addRoundRect(0f, 0f, sidePx.toFloat(), sidePx.toFloat(), radiusPx, radiusPx, Path.Direction.CW)
        }
        canvas.clipPath(path)
        val scale = max(sidePx.toFloat() / src.width, sidePx.toFloat() / src.height)
        val w = src.width * scale
        val h = src.height * scale
        canvas.drawBitmap(
            src, null,
            RectF((sidePx - w) / 2f, (sidePx - h) / 2f, (sidePx + w) / 2f, (sidePx + h) / 2f),
            paint
        )
        return out
    }

    suspend fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val repository = (context.applicationContext as ReminderApplication).container.reminderRepository

        withContext(Dispatchers.IO) {
            try {
                val reminders = repository.getAllRemindersList()

                val ids1x2 = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget1x2::class.java))
                for (appWidgetId in ids1x2) {
                    val opacity = WidgetConfigStore.getWidgetOpacity(context, appWidgetId)
                    val configuredId = WidgetConfigStore.get1x2Or2x2Config(context, appWidgetId)
                    update1x2WidgetWithData(context, appWidgetManager, appWidgetId, opacity, configuredId, reminders)
                }

                val ids2x2 = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget2x2::class.java))
                for (appWidgetId in ids2x2) {
                    val opacity = WidgetConfigStore.getWidgetOpacity(context, appWidgetId)
                    val configuredId = WidgetConfigStore.get1x2Or2x2Config(context, appWidgetId)
                    update2x2WidgetWithData(context, appWidgetManager, appWidgetId, opacity, configuredId, reminders)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val ids4x2 = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget4x2::class.java))
        if (ids4x2.isNotEmpty()) {
            val intent = Intent(context, ReminderWidget4x2::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids4x2)
            }
            context.sendBroadcast(intent)
            // 按双列开关分组，分别通知对应列表视图刷新数据
            val (dualIds, singleIds) = ids4x2.partition { WidgetConfigStore.get4x2DualColumn(context, it) }
            if (singleIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(singleIds.toIntArray(), R.id.widget_list_view)
            }
            if (dualIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(dualIds.toIntArray(), R.id.widget_grid_view)
            }
        }
    }

    fun update1x2WidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        opacity: Int,
        selectedId: Int,
        items: List<ReminderItem>
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout_1x2)
        
        val alpha = (opacity * 255) / 100
        views.setInt(R.id.widget_1x2_bg, "setImageAlpha", alpha)

        val featured = if (selectedId != -1) {
            items.find { it.id == selectedId } ?: getFeaturedReminder(items)
        } else {
            getFeaturedReminder(items)
        }

        if (featured != null) {
            val displayInfo = getDisplayInfo(context, featured)
            views.setTextViewText(R.id.widget_1x2_title, displayInfo.title)
            views.setTextViewText(R.id.widget_1x2_label, displayInfo.label)
            views.setTextViewText(R.id.widget_1x2_days, displayInfo.days)
            views.setTextViewText(R.id.widget_1x2_unit, displayInfo.unit)

            // 应用响应式字号（1x2 专用）
            val (cellWidth, cellHeight) = getWidgetCellSize(appWidgetManager, appWidgetId)
            val responsiveTextSize = getResponsiveDaysTextSize1x2(context, displayInfo.days, displayInfo.unit, cellWidth, cellHeight)
            views.setFloat(R.id.widget_1x2_days, "setTextSize", responsiveTextSize)
            val unitTextSize = responsiveTextSize * 0.32f
            views.setFloat(R.id.widget_1x2_unit, "setTextSize", unitTextSize)

            views.setTextColor(R.id.widget_1x2_days, context.getColor(displayInfo.accentColorResId))

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminderId", featured.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                featured.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_1x2_container, pendingIntent)
        } else {
            views.setTextViewText(R.id.widget_1x2_title, "暂无日程")
            views.setTextViewText(R.id.widget_1x2_label, "点击添加")
            views.setTextViewText(R.id.widget_1x2_days, "0")
            views.setTextViewText(R.id.widget_1x2_unit, "天")

            views.setTextColor(R.id.widget_1x2_days, context.getColor(R.color.widget_accent_annual))

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_1x2_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun update2x2WidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        opacity: Int,
        selectedId: Int,
        items: List<ReminderItem>
    ) {
        // API 31+ 可精确钳方形、内容列用 weight 撑满；低版本降级为无 weight 的近似方形布局
        val layoutResId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            R.layout.widget_layout_2x2
        } else {
            R.layout.widget_layout_2x2_compat
        }
        val views = RemoteViews(context.packageName, layoutResId)

        // 钳制卡片为正方形（部分 OEM 桌面单元格高>宽）
        val sideDp = apply2x2SquareCard(context, views, appWidgetManager, appWidgetId)

        val featured = if (selectedId != -1) {
            items.find { it.id == selectedId } ?: getFeaturedReminder(items)
        } else {
            getFeaturedReminder(items)
        }
        val followPersonalization = featured != null && featured.isCustomized &&
            WidgetConfigStore.get2x2FollowPersonalization(context, appWidgetId)

        // 桌面复用小组件视图、只增量应用本轮 action：凡可被个性化覆盖的属性，
        // 每轮必须显式重置默认值，否则上一轮的位图/颜色会永久残留（如关闭开关后背景图不消失）。
        // 背景图放在独立的 widget_2x2_bg_image 上，靠显式 GONE/VISIBLE 切换；
        // 底层 shape 的 setColorFilter 每轮必发（顺带覆盖旧版本残留的 tint）。
        val alpha = (opacity * 255) / 100
        views.setImageViewResource(R.id.widget_2x2_bg, R.drawable.widget_bg_shape)
        views.setInt(R.id.widget_2x2_bg, "setColorFilter", context.getColor(R.color.widget_bg))
        views.setInt(R.id.widget_2x2_bg, "setImageAlpha", alpha)
        views.setInt(R.id.widget_2x2_bg_image, "setImageAlpha", alpha)
        setViewVisibilityCompat(views, R.id.widget_2x2_bg_image, false)
        setViewVisibilityCompat(views, R.id.widget_2x2_header_bg, true)
        setViewVisibilityCompat(views, R.id.widget_2x2_divider, true)
        setViewVisibilityCompat(views, R.id.widget_2x2_days_image, false)
        setViewVisibilityCompat(views, R.id.widget_2x2_days, true)
        views.setTextColor(R.id.widget_2x2_header_title, android.graphics.Color.WHITE)
        views.setTextColor(R.id.widget_2x2_unit, context.getColor(R.color.widget_text_secondary))
        views.setTextColor(R.id.widget_2x2_date, context.getColor(R.color.widget_text_secondary))

        // 标头/日期字号与上下栏内边距随方形边长等比缩放。
        // 基准 150dp=首页卡片宽度（标头 15sp Medium / 日期 13sp 与首页一致），
        // 2x2 时 scale≈1 保持首页观感，仅手动拉大后才放大，上限 1.6 防过大
        val sizeScale = if (sideDp > 0f) (sideDp / 150f).coerceIn(1f, 1.6f) else 1f
        setTextSizeCompat(views, R.id.widget_2x2_header_title, 15f * sizeScale)
        setTextSizeCompat(views, R.id.widget_2x2_date, 13f * sizeScale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 顶栏/底栏容器高度按比例钉死（34dp=15sp 行高+16dp 边距；32dp=13sp 行高+16dp 边距，
            // scale=1 与 XML 观感一致）；栏内文字靠 center gravity 在钉出的空间里居中
            views.setViewLayoutHeight(R.id.widget_2x2_header, 34f * sizeScale, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(R.id.widget_2x2_footer, 32f * sizeScale, TypedValue.COMPLEX_UNIT_DIP)
            // setViewPadding 已被实机证伪（HyperOS 丢弃，XML 固定 12dp 不随缩放）→
            // 用同族已生效的 setViewLayoutMargin 补差值：总留白 = XML 12dp + margin (12·scale−12)
            val extraPadH = (12f * (sizeScale - 1f)).coerceAtLeast(0f)
            views.setViewLayoutMargin(R.id.widget_2x2_header_title, RemoteViews.MARGIN_START, extraPadH, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutMargin(R.id.widget_2x2_header_title, RemoteViews.MARGIN_END, extraPadH, TypedValue.COMPLEX_UNIT_DIP)
        }

        if (featured != null) {
            val displayInfo = getDisplayInfo(context, featured)
            
            val headerText = "${displayInfo.title} ${displayInfo.label}"
            views.setTextViewText(R.id.widget_2x2_header_title, headerText)
            views.setTextViewText(R.id.widget_2x2_days, displayInfo.days)
            views.setTextViewText(R.id.widget_2x2_unit, displayInfo.unit)
            views.setTextViewText(R.id.widget_2x2_date, displayInfo.dateString)

            // 应用响应式字号（同时考虑数字和单位）；按钳制后的方形尺寸计算
            val (cellWidth, cellHeight) = if (sideDp > 0f) Pair(sideDp, sideDp)
                else getWidgetCellSize(appWidgetManager, appWidgetId)
            val responsiveTextSize = getResponsiveDaysTextSize(context, displayInfo.days, displayInfo.unit, cellWidth, cellHeight)
            setTextSizeCompat(views, R.id.widget_2x2_days, responsiveTextSize)
            // 单位字号按比例缩放（原比例 14sp / 44sp ≈ 0.32）
            val unitTextSize = responsiveTextSize * 0.32f
            setTextSizeCompat(views, R.id.widget_2x2_unit, unitTextSize)

            views.setInt(R.id.widget_2x2_header_bg, "setColorFilter", context.getColor(displayInfo.accentColorResId))
            views.setTextColor(R.id.widget_2x2_days, context.getColor(displayInfo.accentColorResId))

            // 跟随个性化设置：开关开启且事件已个性化时，覆盖默认配色/背景
            if (followPersonalization) {
                apply2x2Personalization(
                    context, views, featured, sideDp,
                    displayInfo.days, responsiveTextSize, unitTextSize,
                    context.getColor(displayInfo.accentColorResId)
                )
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminderId", featured.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                featured.id + 10000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_2x2_container, pendingIntent)
        } else {
            views.setTextViewText(R.id.widget_2x2_header_title, "暂无日程")
            views.setTextViewText(R.id.widget_2x2_days, "0")
            views.setTextViewText(R.id.widget_2x2_unit, "天")
            views.setTextViewText(R.id.widget_2x2_date, "——")
            // 空状态同样重置字号，避免上一轮响应式字号残留
            setTextSizeCompat(views, R.id.widget_2x2_days, 44f)
            setTextSizeCompat(views, R.id.widget_2x2_unit, 14f)

            views.setInt(R.id.widget_2x2_header_bg, "setColorFilter", context.getColor(R.color.widget_accent_annual))
            views.setTextColor(R.id.widget_2x2_days, context.getColor(R.color.widget_accent_annual))

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_2x2_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

object WidgetConfigStore {
    private const val PREFS_NAME = "com.ybhgl.reminder.widget_prefs"

    fun save1x2Or2x2Config(context: Context, appWidgetId: Int, reminderId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("widget_${appWidgetId}_reminder_id", reminderId)
            .commit()
    }

    fun get1x2Or2x2Config(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("widget_${appWidgetId}_reminder_id", -1)
    }

    fun save4x2Config(context: Context, appWidgetId: Int, filterType: String, customIds: Set<Int>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("widget_${appWidgetId}_filter_type", filterType)
            .putString("widget_${appWidgetId}_custom_ids", customIds.joinToString(","))
            .commit()
    }

    fun get4x2FilterType(context: Context, appWidgetId: Int): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("widget_${appWidgetId}_filter_type", "all") ?: "all"
    }

    fun get4x2CustomIds(context: Context, appWidgetId: Int): Set<Int> {
        val str = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("widget_${appWidgetId}_custom_ids", "") ?: ""
        if (str.isEmpty()) return emptySet()
        return str.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun save4x2Tags(context: Context, appWidgetId: Int, tags: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("widget_${appWidgetId}_filter_tags", tags.joinToString(","))
            .commit()
    }

    fun get4x2Tags(context: Context, appWidgetId: Int): Set<String> {
        val str = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("widget_${appWidgetId}_filter_tags", "") ?: ""
        if (str.isEmpty()) return emptySet()
        return str.split(",").filter { it.isNotEmpty() }.toSet()
    }

    fun save4x2DualColumn(context: Context, appWidgetId: Int, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("widget_${appWidgetId}_dual_column", enabled)
            .commit()
    }

    fun get4x2DualColumn(context: Context, appWidgetId: Int): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("widget_${appWidgetId}_dual_column", false)
    }

    /** 2x2 小组件"跟随个性化设置"开关（默认关） */
    fun save2x2FollowPersonalization(context: Context, appWidgetId: Int, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("widget_${appWidgetId}_follow_personalization", enabled)
            .commit()
    }

    fun get2x2FollowPersonalization(context: Context, appWidgetId: Int): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("widget_${appWidgetId}_follow_personalization", false)
    }

    fun deleteConfig(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove("widget_${appWidgetId}_reminder_id")
            .remove("widget_${appWidgetId}_filter_type")
            .remove("widget_${appWidgetId}_custom_ids")
            .remove("widget_${appWidgetId}_filter_tags")
            .remove("widget_${appWidgetId}_dual_column")
            .remove("widget_${appWidgetId}_follow_personalization")
            .remove("widget_${appWidgetId}_opacity")
            .commit()
    }

    fun saveWidgetOpacity(context: Context, appWidgetId: Int, opacity: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("widget_${appWidgetId}_opacity", opacity)
            .commit()
    }

    fun getWidgetOpacity(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("widget_${appWidgetId}_opacity", 100)
    }
}
