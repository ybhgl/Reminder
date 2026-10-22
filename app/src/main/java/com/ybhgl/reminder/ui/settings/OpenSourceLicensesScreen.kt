package com.ybhgl.reminder.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ybhgl.reminder.R
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AboutLibraries Gradle 插件在构建期生成的依赖元数据（res/raw/aboutlibraries.json）。
 * 仅声明需要的字段，其余通过 ignoreUnknownKeys 忽略。
 */
@Serializable
private data class AboutLibrariesMetadata(
    val libraries: List<GeneratedLibrary> = emptyList(),
    val licenses: Map<String, GeneratedLicense> = emptyMap(),
)

@Serializable
private data class GeneratedLibrary(
    val uniqueId: String = "",
    val name: String? = null,
    val artifactVersion: String? = null,
    val website: String? = null,
    val licenses: List<String> = emptyList(),
)

@Serializable
private data class GeneratedLicense(
    val name: String? = null,
    val url: String? = null,
)

/**
 * 许可页展示用的精简模型，字段与既有卡片 UI 对齐。
 */
private data class LibraryLicense(
    val name: String,
    val subtitle: String,
    val license: String,
    val url: String
)

/**
 * 从构建期生成的 raw 资源读取全部运行期依赖（含传递依赖），解析出名称、坐标版本、许可证与主页。
 * 数据由 AboutLibraries 插件自动同步，无需在此硬编码库清单或版本号。
 */
private suspend fun loadLibraryLicenses(context: Context): List<LibraryLicense> =
    withContext(Dispatchers.IO) {
        runCatching {
            val json = context.resources.openRawResource(R.raw.aboutlibraries)
                .bufferedReader()
                .use { it.readText() }
            val metadata = Json { ignoreUnknownKeys = true }
                .decodeFromString<AboutLibrariesMetadata>(json)

            metadata.libraries.mapNotNull { lib ->
                val displayName = lib.name?.takeIf { it.isNotBlank() }
                    ?: lib.uniqueId.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val coordinate = listOf(lib.uniqueId, lib.artifactVersion.orEmpty())
                    .filter { it.isNotBlank() }
                    .joinToString(":")
                val licenseNames = lib.licenses
                    .map { id -> metadata.licenses[id]?.name ?: id }
                    .filter { it.isNotBlank() }
                    .distinct()
                val licenseUrl = lib.licenses
                    .firstNotNullOfOrNull { id -> metadata.licenses[id]?.url?.takeIf { it.isNotBlank() } }
                val website = lib.website?.takeIf { it.isNotBlank() } ?: licenseUrl.orEmpty()

                LibraryLicense(
                    name = displayName,
                    subtitle = coordinate,
                    license = licenseNames.joinToString(" / "),
                    url = website
                )
            }.sortedBy { it.name.lowercase() }
        }.getOrElse { emptyList() }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val contentListState = rememberLazyListState()
    val topBarState = rememberCollapsingTopBarState(contentListState)

    val libraries by produceState(initialValue = emptyList<LibraryLicense>(), context) {
        value = loadLibraryLicenses(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.nestedScroll(topBarState.nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val density = LocalDensity.current
            val topBarHeightDp = with(density) { topBarState.topBarHeightPx.toDp() }
            val dynamicTopPadding = (topBarHeightDp + with(density) { topBarState.titleOffsetPx.toDp() } + 16.dp)
                .coerceAtLeast(0.dp)

            LazyColumn(
                state = contentListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = dynamicTopPadding,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(libraries, key = { it.subtitle }) { library ->
                    LibraryLicenseItemCard(
                        library = library,
                        onClick = {
                            if (library.url.isNotBlank()) {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, library.url.toUri()))
                                }
                            }
                        }
                    )
                }
            }

            // 状态栏渐变遮罩
            StatusBarScrim(
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 标题栏
            val topAppBarColors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
            val topAppBarModifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged {
                        topBarState.topBarHeightPx = it.height.toFloat()
                    }
                    .graphicsLayer {
                        translationY = topBarState.titleOffsetPx
                    }
                    .then(topAppBarModifier)
            ) {
                TopAppBar(
                    title = { Text("开放源代码许可") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryLicenseItemCard(
    library: LibraryLicense,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = library.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            if (library.license.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = library.license,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = library.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
