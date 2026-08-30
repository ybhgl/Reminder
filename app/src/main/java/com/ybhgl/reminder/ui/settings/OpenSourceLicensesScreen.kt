package com.ybhgl.reminder.ui.settings

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState

/**
 * 应用依赖的开源库信息（名称、坐标版本、许可证类型、项目主页）。
 */
private data class LibraryLicense(
    val name: String,
    val subtitle: String,
    val license: String,
    val url: String
)

// 与 app/build.gradle.kts 及 gradle/libs.versions.toml 中的运行期依赖保持一致
private val libraryLicenses = listOf(
    LibraryLicense(
        name = "Kotlin Standard Library",
        subtitle = "org.jetbrains.kotlin:kotlin-stdlib:2.2.21",
        license = "Apache License 2.0",
        url = "https://github.com/JetBrains/kotlin"
    ),
    LibraryLicense(
        name = "AndroidX Core KTX",
        subtitle = "androidx.core:core-ktx:1.17.0",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx"
    ),
    LibraryLicense(
        name = "AndroidX Fragment KTX",
        subtitle = "androidx.fragment:fragment-ktx:1.8.5",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx"
    ),
    LibraryLicense(
        name = "AndroidX DocumentFile",
        subtitle = "androidx.documentfile:documentfile:1.0.1",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx"
    ),
    LibraryLicense(
        name = "AndroidX Core Splashscreen",
        subtitle = "androidx.core:core-splashscreen:1.0.1",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx"
    ),
    LibraryLicense(
        name = "Activity Kotlin Extensions",
        subtitle = "androidx.activity:activity-compose:1.11.0",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx"
    ),
    LibraryLicense(
        name = "AndroidX Lifecycle",
        subtitle = "androidx.lifecycle:lifecycle-runtime-ktx:2.9.4",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx"
    ),
    LibraryLicense(
        name = "AndroidX Biometric",
        subtitle = "androidx.biometric:biometric:1.2.0-alpha05",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx"
    ),
    LibraryLicense(
        name = "Jetpack Compose BOM",
        subtitle = "androidx.compose:compose-bom:2025.10.01",
        license = "Apache License 2.0",
        url = "https://developer.android.com/develop/ui/compose/bom"
    ),
    LibraryLicense(
        name = "Compose UI",
        subtitle = "androidx.compose.ui:ui (BOM 管理)",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose"
    ),
    LibraryLicense(
        name = "Material 3",
        subtitle = "androidx.compose.material3:material3 (BOM 管理)",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose/material3"
    ),
    LibraryLicense(
        name = "Material Icons Extended",
        subtitle = "androidx.compose.material:material-icons-extended:1.7.8",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose"
    ),
    LibraryLicense(
        name = "Material 3 Adaptive Navigation Suite",
        subtitle = "androidx.compose.material3:material3-adaptive-navigation-suite (BOM 管理)",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose/material3"
    ),
    LibraryLicense(
        name = "Navigation Compose",
        subtitle = "androidx.navigation:navigation-compose:2.9.5",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose/navigation"
    ),
    LibraryLicense(
        name = "Room",
        subtitle = "androidx.room:room-ktx:2.8.3",
        license = "Apache License 2.0",
        url = "https://developer.android.com/training/data-storage/room"
    ),
    LibraryLicense(
        name = "DataStore Preferences",
        subtitle = "androidx.datastore:datastore-preferences:1.1.7",
        license = "Apache License 2.0",
        url = "https://developer.android.com/topic/libraries/architecture/datastore"
    ),
    LibraryLicense(
        name = "Kotlinx Serialization JSON",
        subtitle = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0",
        license = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.serialization"
    ),
    LibraryLicense(
        name = "Kotlinx Collections Immutable",
        subtitle = "org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0",
        license = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.collections.immutable"
    ),
    LibraryLicense(
        name = "Accompanist Permissions",
        subtitle = "com.google.accompanist:accompanist-permissions:0.36.0",
        license = "Apache License 2.0",
        url = "https://github.com/google/accompanist"
    ),
    LibraryLicense(
        name = "Accompanist Pager",
        subtitle = "com.google.accompanist:accompanist-pager:0.36.0",
        license = "Apache License 2.0",
        url = "https://github.com/google/accompanist"
    ),
    LibraryLicense(
        name = "Accompanist Pager Indicators",
        subtitle = "com.google.accompanist:accompanist-pager-indicators:0.36.0",
        license = "Apache License 2.0",
        url = "https://github.com/google/accompanist"
    ),
    LibraryLicense(
        name = "Capturable",
        subtitle = "dev.shreyaspatil:capturable:3.0.1",
        license = "Apache License 2.0",
        url = "https://github.com/PatilShreyas/Capturable"
    ),
    LibraryLicense(
        name = "Compose M3 Picker",
        subtitle = "com.seo4d696b75.compose:material3-picker:0.1.6",
        license = "Apache License 2.0",
        url = "https://github.com/Seo-4d696b75/compose-m3-picker"
    ),
    LibraryLicense(
        name = "Tyme4kt",
        subtitle = "cn.6tail:tyme4kt:1.5.0",
        license = "MIT License",
        url = "https://github.com/6tail/tyme4j"
    ),
    LibraryLicense(
        name = "Hyper Notification",
        subtitle = "com.xzakota.hyper.notification:focus-api:1.4",
        license = "Apache License 2.0",
        url = "https://github.com/xzakota/HyperNotification"
    ),
    LibraryLicense(
        name = "Shizuku API",
        subtitle = "dev.rikka.shizuku:api:13.1.5",
        license = "Apache License 2.0",
        url = "https://github.com/RikkaApps/Shizuku-API"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val topBarState = rememberCollapsingTopBarState()

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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = dynamicTopPadding,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(libraryLicenses) { library ->
                    LibraryLicenseItemCard(
                        library = library,
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, library.url.toUri()))
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

            Text(
                text = library.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
