package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraSecondary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class BrandedSplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        val appTitle = intent.getStringExtra(EXTRA_APP_TITLE) ?: "Target Application"
        val tagline = intent.getStringExtra(EXTRA_TAGLINE) ?: "Powered by Aura Launcher"
        val durationMs = intent.getIntExtra(EXTRA_DURATION_MS, 2200)
        val isPreviewMode = intent.getBooleanExtra(EXTRA_IS_PREVIEW, false)

        setContent {
            MyApplicationTheme {
                BrandedSplashScreen(
                    targetPackage = targetPackage,
                    appTitle = appTitle,
                    tagline = tagline,
                    durationMs = durationMs,
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
            Toast.makeText(this, "Splash preview finished! Ready for launch.", Toast.LENGTH_SHORT).show()
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
            "Target App [$appTitle] is starting or not directly installed.",
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

        fun start(
            context: Context,
            targetPackage: String,
            appTitle: String,
            tagline: String = "Powered by Aura Launcher",
            durationMs: Int = 2200,
            isPreview: Boolean = false
        ) {
            val intent = Intent(context, BrandedSplashActivity::class.java).apply {
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                putExtra(EXTRA_APP_TITLE, appTitle)
                putExtra(EXTRA_TAGLINE, tagline)
                putExtra(EXTRA_DURATION_MS, durationMs)
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
fun BrandedSplashScreen(
    targetPackage: String,
    appTitle: String,
    tagline: String,
    durationMs: Int,
    isPreviewMode: Boolean,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
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
        // Light haptic feedback
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF070B14),
                        Color(0xFF020408)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic background ring aura
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AuraCyanGlow.copy(alpha = glowAlpha * 0.4f),
                            AuraSecondary.copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Launcher Logo Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.aura_launcher_logo),
                    contentDescription = "Aura Launcher Official Logo",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(AuraPrimary, AuraSecondary)
                            ),
                            RoundedCornerShape(28.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Launcher Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraPrimary.copy(alpha = 0.6f)),
                modifier = Modifier.padding(bottom = 12.dp)
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Target Application Name
            Text(
                text = appTitle,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Branded Tagline
            Text(
                text = tagline,
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Progress bar and loading status
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(46.dp),
                    color = AuraPrimary,
                    trackColor = Color(0xFF1E293B),
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (progress >= 0.95f) "Launching Application..." else "Initializing via Aura Engine...",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Tap to skip button at the bottom
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .clickable { onFinish() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isPreviewMode) "Close Preview" else "Tap to Skip Intro",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
