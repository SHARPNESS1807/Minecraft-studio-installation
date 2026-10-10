package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BrandingConfig
import com.example.model.LogoPreset
import com.example.model.SplashStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Launcher", appName)
    }

    @Test
    fun `branding config default values are valid`() {
        val config = BrandingConfig(
            targetPackage = "com.sample.retro_game",
            appTitle = "Retro Game",
            tagline = "Powered by Aura Launcher",
            logoPreset = LogoPreset.AURA_ROCKET,
            splashStyle = SplashStyle.PULSE_GLOW,
            durationMs = 2200
        )
        assertEquals("Retro Game", config.appTitle)
        assertEquals(2200, config.durationMs)
        assertEquals("Powered by Aura Launcher", config.tagline)
        assertEquals(LogoPreset.AURA_ROCKET, config.logoPreset)
    }

    @Test
    fun `logo presets have badges and titles`() {
        for (preset in LogoPreset.entries) {
            assertNotNull(preset.title)
            assertNotNull(preset.badge)
            assertNotNull(preset.description)
        }
    }
}
