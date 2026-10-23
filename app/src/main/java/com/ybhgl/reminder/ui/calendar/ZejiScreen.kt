package com.ybhgl.reminder.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.CircularRevealShape
import com.ybhgl.reminder.ui.common.AnchorBubble
import com.ybhgl.reminder.ui.common.CategoryScrollAnchor
import com.ybhgl.reminder.ui.common.CategoryScrollBar
import com.ybhgl.reminder.ui.common.ExplanationBubbleBody
import com.ybhgl.reminder.ui.common.ImmersiveTopBar
import com.ybhgl.reminder.ui.common.contentTopPadding
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import com.ybhgl.reminder.ui.common.smoothImePadding

/**
 * 择日页：宜/忌两区展示全部黄历事件（按分类分组、chip 流式排布），点 chip 进择日详情页。
 * 顶栏为通用沉浸式顶栏，可收起；右上角进入搜索，按名称/白话解释过滤并高亮关键词；
 * 右侧分类滚动条可按类别快速定位并弹出「宜·婚嫁喜事」气泡。
 */
@Composable
fun ZejiScreen(
    onNavigateBack: () -> Unit,
    onOpenDetail: (term: String, recommended: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val recommendedTerms = remember { zejiTerms(recommended = true) }
    val avoidedTerms = remember { zejiTerms(recommended = false) }

    val rows = remember(recommendedTerms, avoidedTerms) {
        buildList {
            add(ZejiRow.Hint)
            add(ZejiRow.Section(recommended = true))
            recommendedTerms.groupBy { tabooCategory(it) }.forEach { (category, list) ->
                add(ZejiRow.Group(recommended = true, category = category, terms = list))
            }
            add(ZejiRow.Section(recommended = false))
            avoidedTerms.groupBy { tabooCategory(it) }.forEach { (category, list) ->
                add(ZejiRow.Group(recommended = false, category = category, terms = list))
            }
        }
    }
    val anchors = remember(rows) {
        rows.mapIndexedNotNull { index, row ->
            (row as? ZejiRow.Group)?.let {
                CategoryScrollAnchor(
                    label = "${if (it.recommended) "宜" else "忌"}·${it.category.label}",
                    itemIndex = index
                )
            }
        }
    }
    val searchPairs = remember(recommendedTerms, avoidedTerms) {
        buildList {
            recommendedTerms.forEach { add(SearchEntry(it, recommended = true)) }
            avoidedTerms.forEach { add(SearchEntry(it, recommended = false)) }
        }
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // 圆形揭示的圆心：右上角搜索按钮实测中心点（与回收站/首页搜索同一口径）
    var searchIconOffset by remember { mutableStateOf(Offset.Zero) }
    val searchAnimationProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ZejiSearchCircularReveal"
    )
    // 按住（长按）中的 chip：气泡跟手显示，松手即收起；key 区分宜/忌两区
    var heldChip by remember { mutableStateOf<String?>(null) }
    val normalizedQuery = searchQuery.trim()
    val results = remember(searchPairs, normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            emptyList()
        } else {
            searchPairs.filter { entry ->
                entry.term.contains(normalizedQuery, ignoreCase = true) ||
                    tabooExplanation(entry.term)?.contains(normalizedQuery, ignoreCase = true) == true
            }.sortedWith(
                compareBy(
                    { !it.term.contains(normalizedQuery, ignoreCase = true) },
                    { tabooCategory(it.term).ordinal },
                    { it.term }
                )
            )
        }
    }

    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val topBarState = rememberCollapsingTopBarState(listState)

    LaunchedEffect(normalizedQuery) {
        if (searchListState.firstVisibleItemIndex > 0) searchListState.scrollToItem(0)
    }
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(topBarState.nestedScrollConnection)
    ) {
        val horizontalPadding = when {
            maxWidth >= 840.dp -> 48.dp
            maxWidth >= 600.dp -> 32.dp
            else -> 20.dp
        }
        val chipGap = when {
            maxWidth >= 840.dp -> 10.dp
            maxWidth >= 600.dp -> 9.dp
            else -> 7.dp
        }
        val topPadding = topBarState.contentTopPadding(extra = 8.dp)
        // 滚动条只在非搜索态出现；顶部按展开高度固定，避免随顶栏收起而移动
        val topBarHeightDp = with(LocalDensity.current) { topBarState.topBarHeightPx.toDp() }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding + 8.dp,
                top = topPadding,
                bottom = navBottom + 24.dp
            )
        ) {
            itemsIndexed(rows, key = { _, row -> row.key }) { _, row ->
                when (row) {
                    is ZejiRow.Hint -> Text(
                        text = "共 ${recommendedTerms.size + avoidedTerms.size} 项宜忌事件，点按事件查看可选日子，长按查看释义",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    is ZejiRow.Section -> SectionHeader(
                        label = if (row.recommended) "宜" else "忌",
                        accent = if (row.recommended) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                        summary = if (row.recommended) "适合操办的事情" else "不宜操办的事情",
                        modifier = if (row.recommended) Modifier else Modifier.padding(top = 16.dp)
                    )

                    is ZejiRow.Group -> CategoryGroup(
                        row = row,
                        chipGap = chipGap,
                        heldChip = heldChip,
                        onHeldChipChange = { heldChip = it },
                        onOpenDetail = onOpenDetail
                    )
                }
            }
        }
        CategoryScrollBar(
            listState = listState,
            anchors = anchors,
            liveContentTopPaddingPx = with(LocalDensity.current) { topPadding.roundToPx() },
            restingContentTopPaddingPx = with(LocalDensity.current) { (topBarHeightDp + 8.dp).roundToPx() },
            contentBottomPaddingPx = with(LocalDensity.current) { (navBottom + 24.dp).roundToPx() },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxSize()
                .padding(top = topBarHeightDp + 8.dp, bottom = navBottom + 12.dp)
        )

        ImmersiveTopBar(
            state = topBarState,
            onNavigateBack = onNavigateBack,
            title = { Text("择日") },
            actions = {
                IconButton(
                    onClick = {
                        heldChip = null
                        isSearchActive = true
                    },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        val position = coordinates.localToRoot(Offset.Zero)
                        val size = coordinates.size
                        searchIconOffset = Offset(
                            position.x + size.width / 2f,
                            position.y + size.height / 2f
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "搜索"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        )

        // 搜索遮罩层（半透明，点击关闭）—— 与回收站/首页搜索同一交互
        if (searchAnimationProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * searchAnimationProgress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }
                    )
            )
        }

        // 搜索面板：圆形揭示展开
        if (searchAnimationProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircularRevealShape(searchAnimationProgress, searchIconOffset))
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 阻止点击穿透
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "搜索名称或白话解释...",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "清空",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                    }

                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .smoothImePadding()
                                .padding(bottom = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (normalizedQuery.isEmpty()) {
                                    "输入关键词，搜索事项名称或白话解释"
                                } else {
                                    "未找到匹配「$normalizedQuery」的事项"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = searchListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .smoothImePadding(),
                            contentPadding = PaddingValues(
                                start = horizontalPadding,
                                end = horizontalPadding,
                                top = 8.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(results, key = { "${it.term}_${it.recommended}" }) { entry ->
                                SearchResultCard(entry = entry, query = normalizedQuery, onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                    onOpenDetail(entry.term, entry.recommended)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface ZejiRow {
    val key: String

    data object Hint : ZejiRow {
        override val key: String = "hint"
    }

    data class Section(val recommended: Boolean) : ZejiRow {
        override val key: String = "section_$recommended"
    }

    data class Group(
        val recommended: Boolean,
        val category: TermCategory,
        val terms: List<String>
    ) : ZejiRow {
        override val key: String = "group_${if (recommended) "r" else "a"}_${category.name}"
    }
}

private data class SearchEntry(val term: String, val recommended: Boolean)

@Composable
private fun SectionHeader(
    label: String,
    accent: Color,
    summary: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.12f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryGroup(
    row: ZejiRow.Group,
    chipGap: Dp,
    heldChip: String?,
    onHeldChipChange: (String?) -> Unit,
    onOpenDetail: (String, Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (row.recommended) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.secondary
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = row.category.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${row.terms.size} 项",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(chipGap),
            verticalArrangement = Arrangement.spacedBy(chipGap)
        ) {
            row.terms.forEach { term ->
                val chipKey = "${if (row.recommended) "r" else "a"}_$term"
                TermChip(
                    term = term,
                    expanded = heldChip == chipKey,
                    onClick = { onOpenDetail(term, row.recommended) },
                    onHoldStart = { onHeldChipChange(chipKey) },
                    onHoldEnd = { onHeldChipChange(null) }
                )
            }
        }
    }
}

/** 术语 chip：点击进入详情；长按浮出白话释义气泡并跟手 —— 按住期间显示，松手立即收起 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TermChip(
    term: String,
    expanded: Boolean,
    onClick: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val explanation = tabooExplanation(term)
    Surface(
        shape = shape,
        color = if (expanded) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .clip(shape)
            // 只做"抬手"观察：Final 通道取事件、全程不消费，点击/长按仍归 combinedClickable，
            // 页面滚动也不受影响；combinedClickable 没有"松开"回调，靠它把跟手气泡收掉。
            // 判退出只认这根手指自己的状态（抬起/从事件里消失），不能拿"被消费"当退出条件——
            // 普通点击的抬手就是被 combinedClickable 消费的，那样会永远等不到下一个事件。
            .pointerInput(term) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                    }
                    onHoldEnd()
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (explanation != null) onHoldStart() },
                onLongClickLabel = "查看释义"
            )
    ) {
        Box {
            // 定高 Box 居中：Text 实测高小于容器高，直接给 Text 设 height 会顶在上沿
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = term, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            AnchorBubble(
                expanded = expanded && explanation != null,
                onDismissRequest = onHoldEnd,
                // 按住场景下弹窗不能抢焦点/拦外部点击，否则手指未抬就被判"点了外面"
                dismissOnOutsideTouch = false
            ) {
                if (explanation != null) ExplanationBubbleBody(term, explanation)
            }
        }
    }
}

@Composable
private fun SearchResultCard(entry: SearchEntry, query: String, onClick: () -> Unit) {
    val accent = if (entry.recommended) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    val explanation = tabooExplanation(entry.term)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.12f)) {
                    Text(
                        text = if (entry.recommended) "宜" else "忌",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = highlight(entry.term, query),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (explanation != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = highlight(explanation, query),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun highlight(text: String, query: String): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    val background = primary.copy(alpha = 0.16f)
    return remember(text, query, primary, background) {
        if (query.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                var start = 0
                while (start < text.length) {
                    val hit = text.indexOf(query, start, ignoreCase = true)
                    if (hit < 0) {
                        append(text.substring(start))
                        break
                    }
                    append(text.substring(start, hit))
                    withStyle(
                        SpanStyle(
                            color = primary,
                            background = background,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(text.substring(hit, hit + query.length))
                    }
                    start = hit + query.length
                }
            }
        }
    }
}
