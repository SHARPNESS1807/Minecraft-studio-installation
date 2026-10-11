package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogoPreset
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraSecondary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.io.File

class BrandedSplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        val appTitle = intent.getStringExtra(EXTRA_APP_TITLE) ?: "Target Application"
        val tagline = intent.getStringExtra(EXTRA_TAGLINE) ?: "Powered by Aura Launcher"
        val durationMs = intent.getIntExtra(EXTRA_DURATION_MS, 2200)
        val isPreviewMode = intent.getBooleanExtra(EXTRA_IS_PREVIEW, false)
        val logoPresetName = intent.getStringExtra(EXTRA_LOGO_PRESET) ?: LogoPreset.AURA_ROCKET.name
        val customLogoPath = intent.getStringExtra(EXTRA_CUSTOM_LOGO_PATH)

        val logoPreset = try {
            LogoPreset.valueOf(logoPresetName)
        } catch (_: Exception) {
            LogoPreset.AURA_ROCKET
        }

        setContent {
            MyApplicationTheme {
                LandscapeBrandedSplashScreen(
                    targetPackage = targetPackage,
                    appTitle = appTitle,
                    tagline = tagline,
                    durationMs = durationMs,
                    logoPreset = logoPreset,
                    customLogoPath = customLogoPath,
                    isPreviewMode = isPreviewMode,
                    onFinish = {
                        proceedToTarget(targetPackage, appTitle, isPreviewMode)
                    }
                )
            }
        }
    }

    private fun proceedToTarget(targetPackage: String, appTitle: String, isPreviewMode: Boolean) {
        if (isPreviewMode) {
            Toast.makeText(this, "Landscape splash preview completed!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (targetPackage.isNotEmpty()) {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                finish()
                return
            }
        }

        Toast.makeText(
            this,
            "App [$appTitle] launched. (Package not installed directly on device)",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_APP_TITLE = "extra_app_title"
        const val EXTRA_TAGLINE = "extra_tagline"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_IS_PREVIEW = "extra_is_preview"
        const val EXTRA_LOGO_PRESET = "extra_logo_preset"
        const val EXTRA_CUSTOM_LOGO_PATH = "extra_custom_logo_path"

        fun start(
            context: Context,
            targetPackage: String,
            appTitle: String,
            tagline: String = "Powered by Aura Launcher",
            durationMs: Int = 2200,
            logoPreset: LogoPreset = LogoPreset.AURA_ROCKET,
            customLogoPath: String? = null,
            isPreview: Boolean = false
        ) {
            val intent = Intent(context, BrandedSplashActivity::class.java).apply {
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                putExtra(EXTRA_APP_TITLE, appTitle)
                putExtra(EXTRA_TAGLINE, tagline)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_LOGO_PRESET, logoPreset.name)
                putExtra(EXTRA_CUSTOM_LOGO_PATH, customLogoPath)
                putExtra(EXTRA_IS_PREVIEW, isPreview)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun LandscapeBrandedSplashScreen(
    targetPackage: String,
    appTitle: String,
    tagline: String,
    durationMs: Int,
    logoPreset: LogoPreset,
    customLogoPath: String?,
    isPreviewMode: Boolean,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}

        val stepTime = 50L
        val totalSteps = (durationMs / stepTime).coerceAtLeast(1)
        for (i in 1..totalSteps) {
            delay(stepTime)
            progress = i.toFloat() / totalSteps
        }
        onFinish()
    }

    val customBitmap = remember(customLogoPath) {
        if (!customLogoPath.isNullOrEmpty()) {
            val file = File(customLogoPath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF070B14),
                        Color(0xFF020408)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Landscape Split Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // LEFT COLUMN: Logo & Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Glow background circle
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AuraCyanGlow.copy(alpha = glowAlpha * 0.45f),
                                        AuraSecondary.copy(alpha = glowAlpha * 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Logo image (Custom or Preset)
                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap.asImageBitmap(),
                            contentDescription = "Custom Logo",
                            modifier = Modifier
                                .size(130.dp)
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(26.dp))
                                .border(
                                    2.dp,
                                    Brush.linearGradient(listOf(AuraPrimary, AuraSecondary)),
                                    RoundedCornerShape(26.dp)
                                )
                        )
                    } else {
                        Image(
                            painter = painterResource(id = logoPreset.drawableRes),
                            contentDescription = logoPreset.title,
                            modifier = Modifier
                                .size(130.dp)
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(26.dp))
                                .border(
                                    2.dp,
                                    Brush.linearGradient(listOf(AuraPrimary, AuraSecondary)),
                                    RoundedCornerShape(26.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraPrimary.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AuraPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AURA LAUNCHER",
                            color = AuraPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // RIGHT COLUMN: Renamed App Title, Tagline, Loader, Skip
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AuraSecondary.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "LANDSCAPE RUNTIME",
                        color = AuraSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Renamed App Title
                Text(
                    text = appTitle,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Tagline
                Text(
                    text = tagline,
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Loader Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(36.dp),
                        color = AuraPrimary,
                        trackColor = Color(0xFF1E293B),
                        strokeWidth = 3.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (progress >= 0.95f) "Starting Application..." else "Initializing via Aura Engine...",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% • Landscape Mode",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Skip / Close Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable { onFinish() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (isPreviewMode) "Exit Preview" else "Skip to App",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
