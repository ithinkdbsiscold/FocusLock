package com.focuslock.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val ONBOARDING_PAGE_COUNT = 8

// Brown palette constants for onboarding
private val Caramel = Color(0xFFD4A574)
private val DarkBrown = Color(0xFF3D2E1F)
private val WarmBlack = Color(0xFF0A0908)
private val WarmGray = Color(0xFF9C9490)
private val CardBg = Color(0xFF1E1B18)

/** The first-launch experience. All setup status is read directly from Android on resume. */
@Composable
fun FocusLockOnboarding(viewModel: FocusViewModel) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val isFinal = page == ONBOARDING_PAGE_COUNT - 1

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { index ->
                when (index) {
                    0 -> DeveloperIntroScreen()
                    1 -> AppIntroductionScreen()
                    2 -> UsageGuideScreen()
                    3 -> LimitsGuideScreen()
                    4 -> BlockingGuideScreen()
                    5 -> StrictModeGuideScreen()
                    6 -> PermissionSetupScreen(viewModel)
                    else -> CompletionScreen(viewModel)
                }
            }
            OnboardingControls(
                page = page,
                isFinal = isFinal,
                onBack = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                onNext = {
                    if (isFinal) viewModel.completeOnboarding()
                    else scope.launch { pagerState.animateScrollToPage(page + 1) }
                }
            )
        }
    }
}

@Composable
private fun OnboardingControls(page: Int, isFinal: Boolean, onBack: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Previous page", tint = WarmGray)
            }
            else Spacer(Modifier.size(48.dp))
            PageIndicator(page)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Caramel,
                    contentColor = WarmBlack
                )
            ) {
                Text(
                    if (isFinal) "Enter FocusLock" else "Next",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(ONBOARDING_PAGE_COUNT) { index ->
            val width by animateFloatAsState(if (index == current) 22f else 6f, animationSpec = spring(stiffness = 700f), label = "indicator")
            Box(
                Modifier
                    .width(width.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (index == current) Caramel else DarkBrown)
            )
        }
    }
}

@Composable
private fun DeveloperIntroScreen() {
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    val titleScale = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        titleVisible = true
        titleScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        delay(200)
        subtitleVisible = true
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0x55D4A574), Color(0x22704020), Color.Transparent),
                    center = Offset(0.5f, 0.4f),
                    radius = 900f
                )
            )
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(titleVisible, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 5 }) {
                Text(
                    "Created by Divy.",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.graphicsLayer(scaleX = titleScale.value, scaleY = titleScale.value)
                )
            }
            Spacer(Modifier.height(16.dp))
            AnimatedVisibility(subtitleVisible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 6 }) {
                Text(
                    "Of course I'm flexing.",
                    style = MaterialTheme.typography.titleLarge,
                    fontStyle = FontStyle.Italic,
                    color = Caramel,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AppIntroductionScreen() = OnboardingPage(
    eyebrow = "INTRODUCING FOCUSLOCK",
    title = "Take back control of your time.",
    description = "Your phone should work for you, not the other way around."
) {
    val entrance by animateFloatAsState(1f, spring(dampingRatio = 0.65f, stiffness = 300f), label = "logo entrance")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        FocusLogo(Modifier.graphicsLayer(alpha = entrance, scaleX = 0.74f + entrance * .26f, scaleY = 0.74f + entrance * .26f))
        Spacer(Modifier.height(18.dp))
        PhoneRestrictionMock()
        Spacer(Modifier.height(18.dp))
        Text(
            "Your time. Your rules.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Caramel
        )
    }
}

@Composable
private fun FocusLogo(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(76.dp),
        shape = RoundedCornerShape(25.dp),
        color = Caramel
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Shield, null, modifier = Modifier.size(42.dp), tint = WarmBlack)
        }
    }
}

@Composable
private fun PhoneRestrictionMock() {
    Card(
        Modifier.width(200.dp).height(160.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, DarkBrown)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, tint = Caramel, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Protected apps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            MiniAppLine("Instagram", "30m limit", .72f)
            MiniAppLine("YouTube", "45m limit", .46f)
            MiniAppLine("Reddit", "20m limit", .92f)
        }
    }
}

@Composable
private fun UsageGuideScreen() = OnboardingPage(
    eyebrow = "YOUR DIGITAL HABITS",
    title = "Know where your time goes.",
    description = "Get a clear picture of your digital habits."
) { SampleDashboard() }

@Composable
private fun SampleDashboard() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val animation by animateFloatAsState(if (entered) 1f else 0f, tween(850, easing = FastOutSlowInEasing), label = "dashboard chart")
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text("Today", style = MaterialTheme.typography.labelMedium, color = WarmGray)
            Text("3h 42m", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Row(
                Modifier.fillMaxWidth().height(70.dp).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(.28f, .56f, .38f, .78f, .48f, .94f, .64f).forEach { target ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(target * animation)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(Brush.verticalGradient(listOf(Caramel, DarkBrown)))
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Most used", style = MaterialTheme.typography.labelMedium, color = WarmGray)
            MiniAppLine("Instagram", "1h 18m", .78f * animation)
            MiniAppLine("YouTube", "56m", .54f * animation)
            MiniAppLine("Messages", "38m", .37f * animation)
        }
    }
}

@Composable
private fun LimitsGuideScreen() = OnboardingPage(
    eyebrow = "PERSONAL LIMITS",
    title = "Set boundaries that work.",
    description = "Choose exactly how much time you want to spend on each application."
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GuideLimitCard("Instagram", "30 minutes", "6 min remaining", .80f, Color(0xFFD4A574))
        GuideLimitCard("YouTube", "45 minutes", "17 min remaining", .62f, Color(0xFFC49060))
        GuideLimitCard("Reddit", "20 minutes", "3 min remaining", .85f, Color(0xFFB07040))
        Text(
            "Every app gets its own daily allowance.",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Caramel,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GuideLimitCard(name: String, limit: String, remaining: String, progress: Float, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(38.dp), shape = RoundedCornerShape(12.dp), color = color) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1), fontWeight = FontWeight.Black, color = WarmBlack)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(limit, style = MaterialTheme.typography.labelMedium, color = WarmGray)
                }
                Text(remaining, style = MaterialTheme.typography.bodySmall, color = Caramel)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(DarkBrown)
                ) {
                    Box(
                        Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(DarkBrown, color)))
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockingGuideScreen() = OnboardingPage(
    eyebrow = "AUTOMATIC APP BLOCKING",
    title = "Your limit means your limit.",
    description = "When your daily allowance runs out, FocusLock blocks access to that application until the next reset."
) {
    BlockingDemo()
    Spacer(Modifier.height(14.dp))
    Text(
        "No snooze. No endless scrolling. Just focus.",
        style = MaterialTheme.typography.titleMedium,
        color = Caramel,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BlockingDemo() {
    val transition = rememberInfiniteTransition(label = "block demo")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "block phase"
    )
    val progress = (phase * 1.25f).coerceAtMost(1f)
    val locked = phase > .77f
    Card(
        Modifier.fillMaxWidth().height(210.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, DarkBrown)
    ) {
        AnimatedContent(
            targetState = locked,
            transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn(tween(260))) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut(tween(180))) },
            label = "blocking result"
        ) { isLocked ->
            if (!isLocked) Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(52.dp), shape = RoundedCornerShape(16.dp), color = Caramel) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("I", color = WarmBlack, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Instagram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Daily limit: 30 minutes", color = WarmGray, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Box(
                    Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(DarkBrown)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                if (progress > .94f) Brush.horizontalGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF4444)))
                                else Brush.horizontalGradient(listOf(DarkBrown, Caramel))
                            )
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("${(progress * 30).roundToInt()} of 30 minutes used", color = WarmGray)
            } else Column(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(Modifier.size(56.dp), shape = CircleShape, color = DarkBrown) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Lock, null, tint = Caramel, modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Time's up", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "Instagram is blocked until your next reset.",
                    textAlign = TextAlign.Center,
                    color = WarmGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StrictModeGuideScreen() = OnboardingPage(
    eyebrow = "STRICT MODE",
    title = "Make your decisions stick.",
    description = "Strict Mode makes it harder to change your limits when temptation strikes."
) {
    val transition = rememberInfiniteTransition(label = "strict lock")
    val pulse by transition.animateFloat(0.93f, 1.05f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "lock pulse")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(
            Modifier.size(76.dp).scale(pulse),
            shape = RoundedCornerShape(26.dp),
            color = DarkBrown
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(40.dp), tint = Caramel)
            }
        }
        Spacer(Modifier.height(18.dp))
        StrictPoint("Protects your configured restrictions")
        StrictPoint("Prevents casual changes to blocking settings")
        StrictPoint("Reduces accidental overrides and keeps limits persistent")
        Spacer(Modifier.height(10.dp))
        Text(
            "FocusLock cannot guarantee protection from force-stop, uninstall, Safe Mode, permission revocation, or other system-level bypasses.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = WarmGray
        )
    }
}

@Composable
private fun StrictPoint(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Check, null, tint = Caramel, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PermissionSetupScreen(viewModel: FocusViewModel) {
    val context = LocalContext.current
    val usageAccess by observePermission({ PermissionStatus.hasUsageAccess(context) }, viewModel::refresh)
    val accessibility by observePermission({ PermissionStatus.isAccessibilityServiceEnabled(context) }, viewModel::refresh)
    var notificationRefresh by remember { mutableStateOf(0) }
    val notifications by observePermission({
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }, viewModel::refresh, notificationRefresh)
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationRefresh += 1 }
    val enabled = listOf(usageAccess, accessibility, notifications).count { it }

    OnboardingPage(
        eyebrow = "SETUP",
        title = "Almost there.",
        description = "Give FocusLock the permissions it needs to work correctly."
    ) {
        Text(
            "$enabled of 3 permissions enabled.",
            color = Caramel,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(DarkBrown)
        ) {
            val animFrac by animateFloatAsState(enabled / 3f, tween(600), label = "perm progress")
            Box(
                Modifier.fillMaxWidth(animFrac).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(DarkBrown, Caramel)))
            )
        }
        Spacer(Modifier.height(14.dp))
        SetupPermissionCard("1", "Usage Access", "Allows FocusLock to understand how much time you spend in each application.", usageAccess, "Enable Usage Access") {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        SetupPermissionCard("2", "Accessibility Permission", "Allows FocusLock to detect when you open a restricted application and redirect you to its blocking screen.", accessibility, "Enable Accessibility") {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        SetupPermissionCard("3", "Notifications", "Allows FocusLock to notify you about usage limits and blocking events.", notifications, "Enable Notifications") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Text(
            "For Accessibility, choose FocusLock in Android Settings and turn on \"FocusLock blocker.\" You can revisit this setup in Settings at any time.",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray
        )
    }
}

@Composable
private fun SetupPermissionCard(number: String, title: String, detail: String, enabled: Boolean, button: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, if (enabled) Caramel.copy(.4f) else DarkBrown)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                Modifier.size(36.dp),
                shape = CircleShape,
                color = if (enabled) Caramel else DarkBrown
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (enabled) Icons.Outlined.Check else Icons.Outlined.Shield,
                        null,
                        tint = if (enabled) WarmBlack else Caramel,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Crossfade(enabled, label = "permission state") { allowed ->
                    Text(
                        if (allowed) "Enabled" else detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (allowed) Caramel else WarmGray
                    )
                }
                if (!enabled) OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.padding(top = 8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Caramel)
                ) { Text(button, style = MaterialTheme.typography.labelMedium, color = Caramel) }
            }
        }
    }
}

@Composable
private fun CompletionScreen(viewModel: FocusViewModel) {
    val context = LocalContext.current
    val usage by observePermission({ PermissionStatus.hasUsageAccess(context) }, viewModel::refresh)
    val accessibility by observePermission({ PermissionStatus.isAccessibilityServiceEnabled(context) }, viewModel::refresh)
    val notifications by observePermission({
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }, viewModel::refresh)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    OnboardingPage(
        eyebrow = "WELCOME",
        title = "Your time belongs to you.",
        description = "Make every minute count."
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(shown, enter = scaleIn(spring(dampingRatio = .55f)) + fadeIn()) { FocusLogo() }
            Spacer(Modifier.height(20.dp))
            FinalPermissionState("Usage Access", usage)
            FinalPermissionState("Accessibility", accessibility)
            FinalPermissionState("Notifications", notifications)
            Spacer(Modifier.height(16.dp))
            Text(
                "Built with focus by Divy.",
                fontStyle = FontStyle.Italic,
                color = Caramel.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun FinalPermissionState(label: String, enabled: Boolean) {
    Row(Modifier.width(240.dp).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (enabled) Icons.Outlined.Check else Icons.Outlined.LockOpen,
            null,
            tint = if (enabled) Caramel else WarmGray,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            if (enabled) "Enabled" else "Not enabled",
            color = if (enabled) Caramel else WarmGray,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun OnboardingPage(eyebrow: String, title: String, description: String, content: @Composable () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(entered, enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 8 }) {
            Column {
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Caramel,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(10.dp))
                Text(description, style = MaterialTheme.typography.bodyLarge, color = WarmGray)
                Spacer(Modifier.height(28.dp))
                content()
            }
        }
    }
}

@Composable
private fun MiniAppLine(name: String, value: String, progress: Float) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.labelSmall, color = WarmGray)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(DarkBrown)
        ) {
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(DarkBrown, Caramel)))
            )
        }
    }
}

/** Observes return from Android Settings instead of assuming a settings intent succeeded. */
@Composable
private fun observePermission(check: () -> Boolean, onRefresh: () -> Unit, refreshKey: Int = 0): State<Boolean> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(initialValue = false, lifecycle, refreshKey) {
        fun refresh() { value = check(); onRefresh() }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer)
        refresh()
        awaitDispose { lifecycle.removeObserver(observer) }
    }
}
