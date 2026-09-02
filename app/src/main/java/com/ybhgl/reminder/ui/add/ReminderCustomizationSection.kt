package com.ybhgl.reminder.ui.add

import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.common.SettingsLinkedVisibility
import com.ybhgl.reminder.ui.common.TonalCardRow
import com.ybhgl.reminder.ui.personalization.PersonalizationConfig
import com.ybhgl.reminder.ui.personalization.PersonalizationContract
import com.ybhgl.reminder.ui.personalization.PersonalizationInput

// 7种不占用应用体积的高表现力系统内置 FontFamily 静态声明
val SansSerifCondensed: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
val SansSerifBlack: FontFamily = FontFamily(Typeface.create("sans-serif-black", Typeface.NORMAL))
val SansSerifLight: FontFamily = FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))

fun String.toFontFamily(): FontFamily {
    return when (this) {
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        "SansSerif-Condensed" -> SansSerifCondensed
        "SansSerif-Black" -> SansSerifBlack
        "SansSerif-Light" -> SansSerifLight
        else -> FontFamily.Default
    }
}

fun String.toFontLabel(): String {
    return when (this) {
        "Serif" -> "优雅衬线 (Serif)"
        "SansSerif" -> "简约无衬线 (Sans-Serif)"
        "Monospace" -> "极客等宽 (Monospace)"
        "Cursive" -> "灵动草书 (Cursive)"
        "SansSerif-Condensed" -> "极简紧凑 (Condensed)"
        "SansSerif-Black" -> "时尚超粗 (Black)"
        "SansSerif-Light" -> "艺术轻细 (Light)"
        else -> "系统默认 (Default)"
    }
}

/**
 * 个性化区块（新建/详情/分享页共用）：
 * - 单一入口卡片，点击打开 [com.ybhgl.reminder.ui.personalization.PersonalizationActivity]
 *   （上方卡片实时预览 + 下方设置面板：卡片颜色 → 卡片背景 → 数字字体）
 * - 不再内置开关：是否个性化由页面内保存的 [PersonalizationConfig.isCustomized] 决定，
 *   在 Activity 中保存即视为开启，右上角"恢复默认"即视为关闭
 * - [loaded] 为 false 表示外部数据未就绪，不渲染也不播动画
 */
@Composable
fun ReminderCustomizationSection(
    config: PersonalizationConfig,
    reminderType: ReminderType,
    showBackgroundOption: Boolean = true,
    loaded: Boolean = true,
    onPersonalizationResult: (PersonalizationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(PersonalizationContract()) { result ->
        result?.let(onPersonalizationResult)
    }

    // loaded=false 时保持可见性占位但不出动画（数据就绪后再渲染真实状态）
    SettingsLinkedVisibility(visible = if (loaded) true else null) {
        Column(modifier = modifier.fillMaxWidth()) {
            TonalCardRow(
                icon = Icons.Filled.AutoAwesome,
                title = "个性化",
                subtitle = if (config.isCustomized) "已定制，点击编辑" else "定制卡片颜色、字体和背景",
                showChevron = true,
                trailing = {
                    // 状态指示：整卡点击进入编辑页，Switch 仅作展示避免双触发
                    Switch(checked = config.isCustomized, onCheckedChange = null)
                },
                onClick = {
                    launcher.launch(
                        PersonalizationInput(
                            config = config,
                            reminderType = reminderType.name,
                            showBackgroundOption = showBackgroundOption
                        )
                    )
                }
            )
        }
    }
}
