package com.example

import com.example.model.BrandingConfig
import com.example.model.LogoPreset
import com.example.model.SplashStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun `branding config default values are valid and app renaming works`() {
        val config = BrandingConfig(
            targetPackage = "com.sample.retro_game",
            appTitle = "Renamed Awesome App",
            tagline = "Powered by Aura Launcher",
            logoPreset = LogoPreset.NEON_PHOENIX,
            splashStyle = SplashStyle.PULSE_GLOW,
            durationMs = 2500
        )
        assertEquals("Renamed Awesome App", config.appTitle)
        assertEquals(2500, config.durationMs)
        assertEquals("Powered by Aura Launcher", config.tagline)
        assertEquals(LogoPreset.NEON_PHOENIX, config.logoPreset)
    }

    @Test
    fun `all logo presets have valid titles, badges and drawables`() {
        for (preset in LogoPreset.entries) {
            assertNotNull(preset.title)
            assertNotNull(preset.badge)
            assertNotNull(preset.description)
            assertTrue(preset.drawableRes != 0)
        }
    }
}
