package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BrandedSplashActivity
import com.example.engine.ApkParser
import com.example.engine.BrandedApkBuilder
import com.example.model.AppItem
import com.example.model.BrandingConfig
import com.example.model.GeneratedApkItem
import com.example.model.LogoPreset
import com.example.model.SplashStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class LauncherTab(val label: String) {
    APPS("Apps"),
    APK_STUDIO("APK Studio"),
    VAULT("Branded Vault"),
    SETTINGS("Settings")
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _importedApks = MutableStateFlow<List<AppItem>>(emptyList())
    val importedApks: StateFlow<List<AppItem>> = _importedApks.asStateFlow()

    private val _selectedApk = MutableStateFlow<AppItem?>(null)
    val selectedApk: StateFlow<AppItem?> = _selectedApk.asStateFlow()

    private val _brandingConfig = MutableStateFlow(BrandingConfig())
    val brandingConfig: StateFlow<BrandingConfig> = _brandingConfig.asStateFlow()

    private val _generatedApks = MutableStateFlow<List<GeneratedApkItem>>(emptyList())
    val generatedApks: StateFlow<List<GeneratedApkItem>> = _generatedApks.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationStep = MutableStateFlow<BrandedApkBuilder.BuildStep?>(null)
    val generationStep: StateFlow<BrandedApkBuilder.BuildStep?> = _generationStep.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(LauncherTab.APK_STUDIO)
    val selectedTab: StateFlow<LauncherTab> = _selectedTab.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadInstalledApps()
        loadSampleApks()
        loadGeneratedVault()
    }

    fun setSelectedTab(tab: LauncherTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val apps = resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val pkgName = resolveInfo.activityInfo.packageName
                    if (pkgName == context.packageName) return@mapNotNull null // Don't show launcher in itself

                    val label = resolveInfo.loadLabel(pm).toString()
                    val iconDrawable = resolveInfo.loadIcon(pm)
                    val bitmap = ApkParser.let {
                        val d = iconDrawable
                        val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
                        val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
                        val b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                        val c = android.graphics.Canvas(b)
                        d.setBounds(0, 0, c.width, c.height)
                        d.draw(c)
                        b
                    }
                    val pkgInfo = pm.getPackageInfo(pkgName, 0)
                    val vName = pkgInfo.versionName ?: "1.0"
                    val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkgInfo.longVersionCode else pkgInfo.versionCode.toLong()

                    AppItem(
                        name = label,
                        packageName = pkgName,
                        iconBitmap = bitmap,
                        versionName = vName,
                        versionCode = vCode,
                        isImportedApk = false,
                        launchActivity = resolveInfo.activityInfo.name
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.name.lowercase() }

            _installedApps.value = apps
        }
    }

    private fun loadSampleApks() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val samplesDir = File(context.filesDir, "sample_apks").apply { if (!exists()) mkdirs() }

            val sampleFiles = listOf(
                "sample_retro_game.apk" to "Retro Space Runner",
                "sample_quick_notes.apk" to "Nova Quick Notes"
            )

            val parsedSamples = mutableListOf<AppItem>()

            sampleFiles.forEach { (assetName, title) ->
                val targetFile = File(samplesDir, assetName)
                if (!targetFile.exists()) {
                    try {
                        context.assets.open(assetName).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (targetFile.exists()) {
                    val parsed = ApkParser.parseApkFile(context, targetFile)
                    if (parsed != null) {
                        val adjusted = parsed.copy(
                            name = title,
                            packageName = "com.sample.${assetName.substringBefore(".apk")}"
                        )
                        parsedSamples.add(adjusted)
                    }
                }
            }

            _importedApks.value = parsedSamples

            // Select first sample by default so APK Studio is instantly interactive
            if (_selectedApk.value == null && parsedSamples.isNotEmpty()) {
                selectApkForBranding(parsedSamples.first())
            }
        }
    }

    fun importApkFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val copiedFile = ApkParser.copyUriToTempFile(context, uri)
            if (copiedFile != null) {
                val parsed = ApkParser.parseApkFile(context, copiedFile)
                if (parsed != null) {
                    _importedApks.value = listOf(parsed) + _importedApks.value
                    selectApkForBranding(parsed)
                    _statusMessage.value = "Successfully imported APK: ${parsed.name}"
                    _selectedTab.value = LauncherTab.APK_STUDIO
                } else {
                    _statusMessage.value = "Failed to parse APK manifest from file."
                }
            } else {
                _statusMessage.value = "Could not read APK file from storage."
            }
        }
    }

    fun selectApkForBranding(appItem: AppItem) {
        _selectedApk.value = appItem
        _brandingConfig.value = _brandingConfig.value.copy(
            targetPackage = appItem.packageName,
            appTitle = appItem.name,
            tagline = "Powered by Aura Launcher"
        )
    }

    fun updateDuration(durationMs: Int) {
        _brandingConfig.value = _brandingConfig.value.copy(durationMs = durationMs)
    }

    fun updateTagline(tagline: String) {
        _brandingConfig.value = _brandingConfig.value.copy(tagline = tagline)
    }

    fun updateLogoPreset(preset: LogoPreset) {
        _brandingConfig.value = _brandingConfig.value.copy(logoPreset = preset)
    }

    fun updateSplashStyle(style: SplashStyle) {
        _brandingConfig.value = _brandingConfig.value.copy(splashStyle = style)
    }

    fun generateBrandedApk() {
        val currentSelected = _selectedApk.value ?: return
        if (_isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            val context = getApplication<Application>()
            val config = _brandingConfig.value.copy(
                targetPackage = currentSelected.packageName,
                appTitle = currentSelected.name
            )

            val result = BrandedApkBuilder.generateBrandedApk(
                context = context,
                config = config,
                onProgress = { step ->
                    _generationStep.value = step
                }
            )

            _isGenerating.value = false
            if (result != null) {
                _generatedApks.value = listOf(result) + _generatedApks.value
                _statusMessage.value = "Generated Branded APK: ${result.fileName}"
            }
        }
    }

    fun dismissGenerationDialog() {
        _generationStep.value = null
    }

    fun launchAppWithAuraLogo(context: Context, app: AppItem) {
        val config = _brandingConfig.value
        BrandedSplashActivity.start(
            context = context,
            targetPackage = app.packageName,
            appTitle = app.name,
            tagline = config.tagline,
            durationMs = config.durationMs,
            isPreview = false
        )
    }

    fun previewSplash(context: Context) {
        val app = _selectedApk.value
        val config = _brandingConfig.value
        BrandedSplashActivity.start(
            context = context,
            targetPackage = app?.packageName ?: "",
            appTitle = app?.name ?: "Preview App",
            tagline = config.tagline,
            durationMs = config.durationMs,
            isPreview = true
        )
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "APK file not found on disk.", Toast.LENGTH_SHORT).show()
                return
            }
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error opening package installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Branded APK"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share APK: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGeneratedVault() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val outputDir = File(context.filesDir, "generated_apks")
            if (outputDir.exists()) {
                val files = outputDir.listFiles { f -> f.extension == "apk" } ?: emptyArray()
                val list = files.map { file ->
                    val sizeMb = file.length() / (1024.0 * 1024.0)
                    val rawName = file.nameWithoutExtension.replace("_aura_branded", "").replace("_", " ")
                    val displayAppName = rawName.replaceFirstChar { char -> char.uppercase() }
                    GeneratedApkItem(
                        id = file.name,
                        appName = displayAppName,
                        targetPackage = "com.aura.branded",
                        filePath = file.absolutePath,
                        fileName = file.name,
                        timestamp = file.lastModified(),
                        fileSizeFormatted = String.format("%.2f MB", sizeMb),
                        logoPreset = LogoPreset.AURA_ROCKET.title,
                        durationMs = 2200,
                        tagline = "Powered by Aura Launcher"
                    )
                }.sortedByDescending { it.timestamp }
                _generatedApks.value = list
            }
        }
    }
}
