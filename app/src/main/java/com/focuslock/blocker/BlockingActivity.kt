package com.focuslock.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslock.ui.FocusLockTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.atomic.AtomicBoolean

private val Caramel = Color(0xFFD4A574)
private val DarkBrown = Color(0xFF3D2E1F)
private val WarmBlack = Color(0xFF0A0908)
private val WarmGray = Color(0xFF9C9490)

class BlockingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val name = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val resetAt = intent.getLongExtra(EXTRA_NEXT_RESET, 0L)

        // Handle back press: go to launcher instead of returning to blocked app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        setContent { FocusLockTheme { BlockedScreen(name, resetAt) { goHome() } } }
    }

    override fun onResume() {
        super.onResume()
        showing.set(true)
    }

    override fun onPause() {
        showing.set(false)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        showing.set(false)
        super.onDestroy()
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_NEXT_RESET = "next_reset"
        private val showing = AtomicBoolean(false)
        fun isShowing() = showing.get()
    }
}

@Composable
private fun BlockedScreen(appName: String, resetAt: Long, onDone: () -> Unit) {
    val resetText = if (resetAt > 0) DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .format(Instant.ofEpochMilli(resetAt).atZone(java.time.ZoneId.systemDefault())) else "the next reset"

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    // Pulsing lock icon
    val infiniteTransition = rememberInfiniteTransition(label = "lock pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = WarmBlack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF2A1F14),
                            Color(0xFF150F0A),
                            WarmBlack,
                            WarmBlack
                        )
                    )
                )
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "FOCUSLOCK",
                        color = Caramel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(32.dp))

                    // Pulsing lock icon
                    Surface(
                        modifier = Modifier.size(80.dp).scale(pulse),
                        shape = CircleShape,
                        color = DarkBrown
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Lock,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = Caramel
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                    Text(
                        "Time's up",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "$appName has reached its daily limit.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Available again $resetText.",
                        textAlign = TextAlign.Center,
                        color = WarmGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(40.dp))
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Caramel,
                            contentColor = WarmBlack
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Return to home",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
