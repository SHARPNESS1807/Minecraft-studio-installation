package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GenerationDialog
import com.example.ui.components.LauncherTopBar
import com.example.ui.screens.ApkSignerScreen
import com.example.ui.screens.ApkStudioScreen
import com.example.ui.screens.AppsDrawerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LauncherTab
import com.example.viewmodel.LauncherViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: LauncherViewModel = viewModel()
                LauncherApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LauncherApp(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()
    val importedApks by viewModel.importedApks.collectAsState()
    val selectedApk by viewModel.selectedApk.collectAsState()
    val brandingConfig by viewModel.brandingConfig.collectAsState()
    val generatedApks by viewModel.generatedApks.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationStep by viewModel.generationStep.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isSigning by viewModel.isSigning.collectAsState()
    val signingResult by viewModel.signingResult.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importApkFromUri(it) }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LauncherTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onAddApkClick = {
                    filePicker.launch("application/vnd.android.package-archive")
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("launcher_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = selectedTab == LauncherTab.APPS,
                    onClick = { viewModel.setSelectedTab(LauncherTab.APPS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Apps",
                            modifier = Modifier.testTag("nav_item_apps")
                        )
                    },
                    label = {
                        Text(
                            text = "Apps",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == LauncherTab.APPS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001E2B),
                        selectedTextColor = AuraPrimary,
                        indicatorColor = AuraPrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == LauncherTab.APK_STUDIO,
                    onClick = { viewModel.setSelectedTab(LauncherTab.APK_STUDIO) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Studio",
                            modifier = Modifier.testTag("nav_item_studio")
                        )
                    },
                    label = {
                        Text(
                            text = "Studio",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == LauncherTab.APK_STUDIO) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001E2B),
                        selectedTextColor = AuraPrimary,
                        indicatorColor = AuraPrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == LauncherTab.SIGNER,
                    onClick = { viewModel.setSelectedTab(LauncherTab.SIGNER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Sign APK",
                            modifier = Modifier.testTag("nav_item_signer")
                        )
                    },
                    label = {
                        Text(
                            text = "Sign APK",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == LauncherTab.SIGNER) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001E2B),
                        selectedTextColor = AuraPrimary,
                        indicatorColor = AuraPrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == LauncherTab.VAULT,
                    onClick = { viewModel.setSelectedTab(LauncherTab.VAULT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Vault",
                            modifier = Modifier.testTag("nav_item_vault")
                        )
                    },
                    label = {
                        Text(
                            text = "Vault",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == LauncherTab.VAULT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001E2B),
                        selectedTextColor = AuraPrimary,
                        indicatorColor = AuraPrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == LauncherTab.SETTINGS,
                    onClick = { viewModel.setSelectedTab(LauncherTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == LauncherTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001E2B),
                        selectedTextColor = AuraPrimary,
                        indicatorColor = AuraPrimary,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AuraBackground)
        ) {
            when (selectedTab) {
                LauncherTab.APPS -> {
                    AppsDrawerScreen(
                        installedApps = installedApps,
                        importedApks = importedApks,
                        searchQuery = searchQuery,
                        onLaunchWithLogo = { app ->
                            viewModel.launchAppWithAuraLogo(context, app)
                        },
                        onBrandApk = { app ->
                            viewModel.selectApkForBranding(app)
                            viewModel.setSelectedTab(LauncherTab.APK_STUDIO)
                        }
                    )
                }

                LauncherTab.APK_STUDIO -> {
                    ApkStudioScreen(
                        selectedApk = selectedApk,
                        importedApks = importedApks,
                        brandingConfig = brandingConfig,
                        isGenerating = isGenerating,
                        onPickApkFromStorage = { uri ->
                            viewModel.importApkFromUri(uri)
                        },
                        onSelectSampleApk = { sample ->
                            viewModel.selectApkForBranding(sample)
                        },
                        onAppTitleChange = { newTitle ->
                            viewModel.updateAppTitle(newTitle)
                        },
                        onDurationChange = { dur ->
                            viewModel.updateDuration(dur)
                        },
                        onTaglineChange = { tag ->
                            viewModel.updateTagline(tag)
                        },
                        onLogoPresetChange = { preset ->
                            viewModel.updateLogoPreset(preset)
                        },
                        onPickCustomLogo = { uri ->
                            viewModel.setCustomLogoFromUri(uri)
                        },
                        onSplashStyleChange = { style ->
                            viewModel.updateSplashStyle(style)
                        },
                        onPreviewSplash = {
                            viewModel.previewSplash(context)
                        },
                        onGenerateApk = {
                            viewModel.generateBrandedApk()
                        }
                    )
                }

                LauncherTab.SIGNER -> {
                    ApkSignerScreen(
                        importedApks = importedApks,
                        isSigning = isSigning,
                        signingResult = signingResult,
                        onPickApkToSign = { uri ->
                            viewModel.importApkFromUri(uri)
                        },
                        onSignApk = { file ->
                            viewModel.signAnyApk(file)
                        },
                        onInstallSignedApk = { file ->
                            viewModel.installApk(context, file)
                        },
                        onShareSignedApk = { file ->
                            viewModel.shareApk(context, file)
                        },
                        onClearResult = {
                            viewModel.clearSigningResult()
                        }
                    )
                }

                LauncherTab.VAULT -> {
                    VaultScreen(
                        generatedApks = generatedApks,
                        onInstallApk = { item ->
                            viewModel.installApk(context, File(item.filePath))
                        },
                        onLaunchApk = { item ->
                            BrandedSplashActivity.start(
                                context = context,
                                targetPackage = item.targetPackage,
                                appTitle = item.appName,
                                tagline = item.tagline,
                                durationMs = item.durationMs,
                                isPreview = false
                            )
                        },
                        onShareApk = { item ->
                            viewModel.shareApk(context, File(item.filePath))
                        },
                        onNavigateToStudio = {
                            viewModel.setSelectedTab(LauncherTab.APK_STUDIO)
                        }
                    )
                }

                LauncherTab.SETTINGS -> {
                    SettingsScreen()
                }
            }

            // Generation Progress Modal
            generationStep?.let { step ->
                GenerationDialog(
                    step = step,
                    onDismiss = { viewModel.dismissGenerationDialog() },
                    onInstall = { item ->
                        viewModel.installApk(context, File(item.filePath))
                    },
                    onLaunch = { item ->
                        BrandedSplashActivity.start(
                            context = context,
                            targetPackage = item.targetPackage,
                            appTitle = item.appName,
                            tagline = item.tagline,
                            durationMs = item.durationMs,
                            isPreview = false
                        )
                    },
                    onShare = { item ->
                        viewModel.shareApk(context, File(item.filePath))
                    }
                )
            }
        }
    }
}
