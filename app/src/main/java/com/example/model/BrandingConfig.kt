package com.example.model

import android.graphics.Bitmap
import com.example.R

enum class LogoPreset(val title: String, val badge: String, val description: String, val drawableRes: Int) {
    AURA_ROCKET("Aura Rocket Prism", "⚡ OFFICIAL", "Dynamic electric rocket with crystalline aura glow", R.drawable.aura_launcher_logo),
    NEON_PHOENIX("Cyber Phoenix", "🔥 MYTHIC", "Futuristic neon electric gold & cyan wings crest", R.drawable.phoenix_logo),
    GALAXY_VORTEX("Galaxy Vortex", "🌀 COSMIC", "Swirling electric violet and cyan quantum shield", R.drawable.vortex_logo),
    CYBER_SHIELD("Cyber Shield", "🛡️ DEFENSE", "Neon fortified perimeter crest emblem", R.drawable.aura_launcher_logo),
    QUANTUM_PRISM("Quantum Prism", "💎 APEX", "Hexagonal refractive crystal geometry", R.drawable.aura_launcher_logo),
    MINIMAL_NOVA("Minimal Nova", "✨ CLEAN", "Sleek starlight monogram vector", R.drawable.aura_launcher_logo)
}

enum class SplashStyle(val title: String, val animType: String) {
    PULSE_GLOW("Pulse & Glow", "Smooth breathing aura glow with scale oscillation"),
    SPRING_ZOOM("Rocket Spring", "Dynamic pop-in spring bounce entrance"),
    SHIMMER("Quantum Shimmer", "Neon gradient light sweep over emblem"),
    MINIMAL("Sleek Fade", "Clean cinematic alpha reveal")
}

data class BrandingConfig(
    val targetPackage: String = "",
    val appTitle: String = "My App",
    val tagline: String = "Powered by Aura Launcher",
    val logoPreset: LogoPreset = LogoPreset.AURA_ROCKET,
    val customLogoUri: String? = null,
    val customLogoBitmap: Bitmap? = null,
    val splashStyle: SplashStyle = SplashStyle.PULSE_GLOW,
    val durationMs: Int = 2200,
    val themeBackgroundHex: String = "#0F172A",
    val themeAccentHex: String = "#38BDF8",
    val showProgressBar: Boolean = true,
    val enableHaptics: Boolean = true
)

data class GeneratedApkItem(
    val id: String,
    val appName: String,
    val targetPackage: String,
    val filePath: String,
    val fileName: String,
    val timestamp: Long,
    val fileSizeFormatted: String,
    val logoPreset: String,
    val durationMs: Int,
    val tagline: String,
    val isSigned: Boolean = true
)
