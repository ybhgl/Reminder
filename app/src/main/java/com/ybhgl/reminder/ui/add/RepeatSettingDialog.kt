package com.ybhgl.reminder.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ybhgl.reminder.data.RepeatInfo
import com.ybhgl.reminder.data.RepeatUnit
import com.ybhgl.reminder.ui.theme.ReminderTheme
import com.seo4d696b75.compose.material3.picker.Picker
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun RepeatSettingDialog(
    repeatInfo: RepeatInfo?,
    availableUnits: List<RepeatUnit>,
    onDismissRequest: () -> Unit,
    onConfirm: (RepeatInfo?) -> Unit
) {
    val intervalOptions: PersistentList<PickerOption<Int>> = remember {
        buildList<PickerOption<Int>> {
            add(PickerOption(label = "不重复", value = null))
            for (value in 1..100) {
                add(PickerOption(label = value.toString(), value = value))
            }
        }.toPersistentList()
    }
    var intervalIndex by remember(repeatInfo) {
        val initialIndex = repeatInfo?.interval?.let { interval ->
            intervalOptions.indexOfFirst { it.value == interval }
        } ?: 0
        mutableIntStateOf(initialIndex.takeIf { it >= 0 } ?: 0)
    }

    val unitOptions: PersistentList<PickerOption<RepeatUnit>> = remember(availableUnits) {
        buildList<PickerOption<RepeatUnit>> {
            add(PickerOption(label = "不重复", value = null))
            availableUnits.forEach { unit ->
                val label = when (unit) {
                    RepeatUnit.DAY -> "天"
                    RepeatUnit.WEEK -> "周"
                    RepeatUnit.MONTH -> "月"
                    RepeatUnit.YEAR -> "年"
                }
                add(PickerOption(label = label, value = unit))
            }
        }.toPersistentList()
    }
    var unitIndex by remember(repeatInfo, availableUnits) {
        val initialIndex = repeatInfo?.let { info ->
            unitOptions.indexOfFirst { it.value == info.unit }
        } ?: 0
        mutableIntStateOf(initialIndex.takeIf { it >= 0 } ?: 0)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("设置重复周期") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Picker(
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp),
                        index = intervalIndex,
                        values = intervalOptions,
                        onIndexChange = { newIndex -> intervalIndex = newIndex },
                        labelStyle = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                    Picker(
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp),
                        index = unitIndex,
                        values = unitOptions,
                        onIndexChange = { newIndex -> unitIndex = newIndex },
                        labelStyle = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val chosenInterval = intervalOptions.getOrNull(intervalIndex)?.value
                val chosenUnit = unitOptions.getOrNull(unitIndex)?.value
                if (chosenInterval == null || chosenUnit == null) {
                    onConfirm(null)
                } else {
                    onConfirm(RepeatInfo(chosenInterval, chosenUnit))
                }
            }) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun RepeatSettingDialogPreview() {
    ReminderTheme {
        RepeatSettingDialog(
            repeatInfo = RepeatInfo(3, RepeatUnit.DAY),
            availableUnits = listOf(RepeatUnit.DAY, RepeatUnit.WEEK, RepeatUnit.MONTH, RepeatUnit.YEAR),
            onDismissRequest = {},
            onConfirm = {}
        )
    }
}

private data class PickerOption<T>(val label: String, val value: T?) {
    override fun toString(): String = label
}
