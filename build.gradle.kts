// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Kotlin 语言服务器导入时会读取各变体的 annotationProcessors.json 与 KSP 生成目录；
// clean 后这些文件不存在会导致导入失败，故在 IDE 导入任务前先生成。
tasks.matching { it.name == "prepareKotlinIdeaImport" }.configureEach {
    dependsOn(":app:javaPreCompileDebugAndroidTest")
    dependsOn(":app:javaPreCompileReleaseNoMinify")
    dependsOn(project(":app").tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") })
}