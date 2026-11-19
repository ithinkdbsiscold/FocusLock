package com.focuslock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focuslock.data.AppUsageRow
import com.focuslock.data.AppUsageStat
import com.focuslock.data.ChartBarData
import com.focuslock.data.DayTotal
import com.focuslock.data.ManagedApp
import com.focuslock.data.ScreenTimeReport
import com.focuslock.data.ScreenTimeTab
import com.focuslock.ui.FocusLockOnboarding
import com.focuslock.ui.FocusLockTheme
import com.focuslock.ui.FocusViewModel
import com.focuslock.ui.PermissionStatus
import com.focuslock.ui.formatClockMinutes
import com.focuslock.ui.formatDuration
import com.focuslock.ui.formatDurationDetailed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

// Warm visual accents for FocusLock dark theme
private val Caramel = Color(0xFFD4A574)
private val DarkBrown = Color(0xFF3D2E1F)
private val WarmBlack = Color(0xFF0A0908)
private val WarmGray = Color(0xFF9C9490)
private val CardBg = Color(0xFF1E1B18)
private val SurfaceDark = Color(0xFF141210)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FocusLockRoot() }
    }
}

private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("dashboard", "Today", Icons.Outlined.Dashboard),
    Destination("screentime", "Screen Time", Icons.Outlined.BarChart),
    Destination("apps", "Apps", Icons.Outlined.Apps),
    Destination("settings", "Settings", Icons.Outlined.Settings)
)

@Composable
private fun FocusLockRoot(viewModel: FocusViewModel = viewModel()) {
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val onboarded by viewModel.onboardingComplete.collectAsStateWithLifecycle()
    FocusLockTheme(darkTheme) {
        when (onboarded) {
            null -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { }
            true -> FocusLockApp(viewModel)
            false -> FocusLockOnboarding(viewModel)
        }
    }
}

@Composable
private fun FocusLockApp(viewModel: FocusViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collectLatest { snackbar.showSnackbar(it) } }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!route.startsWith("detail/")) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp
                ) {
                    destinations.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, item.label, modifier = Modifier.size(22.dp)) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Caramel,
                                selectedTextColor = Caramel,
                                unselectedIconColor = WarmGray,
                                unselectedTextColor = WarmGray,
                                indicatorColor = DarkBrown.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(viewModel, onOpen = { nav.navigate("detail/$it") }) }
            composable("screentime") { ScreenTimeScreen(viewModel, onOpen = { nav.navigate("detail/$it") }) }
            composable("apps") { AppsScreen(viewModel, onOpen = { nav.navigate("detail/$it") }) }
            composable("settings") { SettingsScreen(viewModel) }
            composable("detail/{packageName}") { backStack ->
                DetailScreen(
                    viewModel = viewModel,
                    packageName = backStack.arguments?.getString("packageName").orEmpty(),
                    onBack = nav::popBackStack
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageHeader(title: String, onRefresh: (() -> Unit)? = null) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        actions = {
            if (onRefresh != null) IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, "Refresh usage", tint = Caramel)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

// ─── DASHBOARD (TODAY TAB) ───

@Composable
private fun DashboardScreen(viewModel: FocusViewModel, onOpen: (String) -> Unit) {
    val snapshot by viewModel.dashboard.collectAsStateWithLifecycle()
    val rows = snapshot.rows
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
        item { PageHeader("Today", viewModel::refresh) }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenTimeRing(
                        usedMillis = snapshot.totalMillis,
                        modifier = Modifier.size(90.dp)
                    )
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            "SCREEN TIME",
                            style = MaterialTheme.typography.labelLarge,
                            color = Caramel,
                            letterSpacing = 2.sp
                        )
                        AnimatedDurationText(snapshot.totalMillis)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Launches", snapshot.totalLaunches.toString(), Icons.Outlined.RocketLaunch, Modifier.weight(1f))
                    MetricCard("Restricted", snapshot.restrictedCount.toString(), Icons.Outlined.Shield, Modifier.weight(1f))
                    MetricCard("Blocked", snapshot.blockedCount.toString(), Icons.Outlined.Block, Modifier.weight(1f))
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    "DAILY OVERVIEW",
                    style = MaterialTheme.typography.labelLarge,
                    color = Caramel,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        if (rows.isEmpty()) item {
            EmptyState("No limits yet", "Open Apps to set a daily allowance for the apps that pull your attention.")
        }
        itemsIndexed(rows.take(5), key = { _, it -> it.packageName }) { index, row ->
            StaggeredFadeIn(index) {
                UsageCard(row, Modifier
                    .padding(horizontal = 20.dp, vertical = 5.dp)
                    .clickable { onOpen(row.packageName) })
            }
        }
        if (rows.isNotEmpty()) item {
            Text(
                "REMAINING ALLOWANCE",
                Modifier.padding(start = 20.dp, top = 26.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Caramel,
                letterSpacing = 2.sp
            )
        }
    }
}

// ─── DEDICATED SCREEN TIME DASHBOARD ───

@Composable
private fun ScreenTimeScreen(viewModel: FocusViewModel, onOpen: (String) -> Unit) {
    val currentTab by viewModel.screenTimeTab.collectAsStateWithLifecycle()
    val report by viewModel.screenTimeReport.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    var selectedBar by remember { mutableStateOf<ChartBarData?>(null) }

    // Reset selected bar when tab or period changes
    LaunchedEffect(report.periodSubtitle, currentTab) {
        selectedBar = null
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            PageHeader("Screen Time", viewModel::refresh)
        }

        // Tab Selector: Day vs Week
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(4.dp)) {
                        TabButton(
                            title = "Day",
                            isSelected = currentTab == ScreenTimeTab.DAY,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setScreenTimeTab(ScreenTimeTab.DAY) }
                        )
                        TabButton(
                            title = "Week",
                            isSelected = currentTab == ScreenTimeTab.WEEK,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setScreenTimeTab(ScreenTimeTab.WEEK) }
                        )
                    }
                }
            }
        }

        // Date Range Selector with < > arrows
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = viewModel::previousScreenTimePeriod,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous period", tint = Caramel)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        report.periodTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        report.periodSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray
                    )
                }

                IconButton(
                    onClick = viewModel::nextScreenTimePeriod,
                    enabled = canGoForward,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        "Next period",
                        tint = if (canGoForward) Caramel else Color(0xFF4A423C)
                    )
                }
            }
        }

        // Prominent Total Screen Time Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        if (currentTab == ScreenTimeTab.DAY) "DAILY SCREEN TIME" else "WEEKLY SCREEN TIME",
                        style = MaterialTheme.typography.labelLarge,
                        color = Caramel,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatDuration(report.totalMillis),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (currentTab == ScreenTimeTab.WEEK && report.totalMillis > 0) {
                        Text(
                            "Daily average: ${formatDuration(report.totalMillis / 7)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Selected Bar Info or Prompt
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedBar != null) {
                                Icon(Icons.Outlined.Timer, null, tint = Caramel, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${selectedBar!!.label} · ${formatDuration(selectedBar!!.millis)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Caramel,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "Tap any bar to inspect duration",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmGray
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Interactive Bar Chart
                    InteractiveBarChart(
                        bars = report.chartBars,
                        selectedBarId = selectedBar?.id,
                        onBarClick = { clicked ->
                            selectedBar = if (selectedBar?.id == clicked.id) null else clicked
                        }
                    )
                }
            }
        }

        // App Usage Breakdown Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "APP USAGE BREAKDOWN",
                    style = MaterialTheme.typography.labelLarge,
                    color = Caramel,
                    letterSpacing = 2.sp
                )
                Text(
                    "${report.appUsages.size} apps",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGray
                )
            }
        }

        if (report.appUsages.isEmpty()) {
            item {
                EmptyState("No usage detected", "No foreground activity recorded for this period.")
            }
        } else {
            items(report.appUsages, key = { it.packageName }) { appStat ->
                AppUsageBreakdownRow(
                    stat = appStat,
                    onClick = { onOpen(appStat.packageName) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TabButton(title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Caramel else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) WarmBlack else WarmGray,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun InteractiveBarChart(
    bars: List<ChartBarData>,
    selectedBarId: Int?,
    onBarClick: (ChartBarData) -> Unit
) {
    val maxMillis = bars.maxOfOrNull { it.millis }?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            val isSelected = selectedBarId == bar.id
            val heightFraction = (bar.millis.toFloat() / maxMillis).coerceIn(0.04f, 1f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onBarClick(bar) },
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightFraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(listOf(Color(0xFFFFF0DC), Caramel))
                            } else {
                                Brush.verticalGradient(listOf(Caramel.copy(alpha = 0.85f), DarkBrown))
                            }
                        )
                )
                if (bar.shortLabel.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        bar.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Caramel else WarmGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUsageBreakdownRow(
    stat: AppUsageStat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(stat.packageName, stat.displayName)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stat.displayName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${(stat.percentage * 100).toInt()}% of total screen time",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray
                    )
                }
                Text(
                    formatDuration(stat.usageMillis),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Caramel
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(DarkBrown)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(stat.percentage.coerceIn(0.01f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFFC49060), Caramel)))
                )
            }
        }
    }
}

// ─── APPS SCREEN (EDITABLE LIMITS) ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsScreen(viewModel: FocusViewModel, onOpen: (String) -> Unit) {
    val apps by viewModel.managedApps.collectAsStateWithLifecycle()
    val strict by viewModel.configuration.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editingApp by remember { mutableStateOf<ManagedApp?>(null) }
    var removingApp by remember { mutableStateOf<ManagedApp?>(null) }
    val filtered = apps.filter { it.displayName.contains(query, true) || it.packageName.contains(query, true) }

    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
        item { PageHeader("Applications", viewModel::refresh) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = WarmGray) },
                    label = { Text("Search installed apps") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Caramel,
                        unfocusedBorderColor = Color(0xFF4A423C),
                        cursorColor = Caramel,
                        focusedLabelColor = Caramel
                    )
                )
                Spacer(Modifier.height(12.dp))
                if (strict.strictModeEnabled) AssistChip(
                    onClick = {},
                    label = { Text("Strict mode locks changes", color = Caramel) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = Caramel, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkBrown)
                )
                Text(
                    "Set daily limits for applications. Tap Edit to change hours and minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
        itemsIndexed(filtered, key = { _, it -> it.packageName }) { index, app ->
            StaggeredFadeIn(index) {
                ManagedAppCard(
                    app = app,
                    strict = strict.strictModeEnabled,
                    onOpen = { onOpen(app.packageName) },
                    onEdit = { editingApp = app },
                    onRemove = { removingApp = app },
                    onToggle = { viewModel.setEnabled(app.packageName, it) }
                )
            }
        }
        if (filtered.isEmpty()) item {
            EmptyState("No matching apps", "Installed launcher apps appear here after FocusLock scans your device.")
        }
    }

    // Edit Limit Dialog
    editingApp?.let { app ->
        EditLimitDialog(
            app = app,
            existingLimitMillis = app.limitMillis,
            onDismiss = { editingApp = null },
            onSave = { durationMillis ->
                viewModel.saveLimit(app.packageName, app.displayName, durationMillis, true)
                editingApp = null
            },
            onRemove = if (app.limitMillis != null) {
                {
                    viewModel.removeLimit(app.packageName)
                    editingApp = null
                }
            } else null
        )
    }

    // Confirm Removal Dialog
    removingApp?.let { app ->
        AlertDialog(
            onDismissRequest = { removingApp = null },
            containerColor = SurfaceDark,
            title = { Text("Remove Limit?", fontWeight = FontWeight.Bold) },
            text = { Text("Remove the daily time limit for ${app.displayName}? Accumulated usage history will be preserved.", color = WarmGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeLimit(app.packageName)
                        removingApp = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { removingApp = null },
                    border = BorderStroke(1.dp, Color(0xFF4A423C))
                ) { Text("Cancel", color = WarmGray) }
            }
        )
    }
}

@Composable
private fun ManagedAppCard(
    app: ManagedApp,
    strict: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName, app.displayName)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.displayName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (app.limitMillis != null) "Daily limit: ${formatDurationDetailed(app.limitMillis)}" else "No daily limit",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (app.limitMillis != null) Caramel else WarmGray
                    )
                }

                if (app.limitMillis == null) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !strict,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Caramel.copy(alpha = if (strict) 0.3f else 1f))
                    ) {
                        Text("Set limit", color = Caramel.copy(alpha = if (strict) 0.3f else 1f))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = app.enabled,
                            onCheckedChange = onToggle,
                            enabled = !strict,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WarmBlack,
                                checkedTrackColor = Caramel,
                                uncheckedThumbColor = WarmGray,
                                uncheckedTrackColor = DarkBrown
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onEdit, enabled = !strict, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Outlined.Edit, "Edit limit", tint = if (!strict) Caramel else WarmGray, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onRemove, enabled = !strict, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Outlined.Delete, "Remove limit", tint = if (!strict) MaterialTheme.colorScheme.error else WarmGray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── EDIT LIMIT DIALOG (HOURS & MINUTES) ───

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditLimitDialog(
    app: ManagedApp,
    existingLimitMillis: Long?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val initialTotalMinutes = (existingLimitMillis ?: (30 * 60_000L)) / 60_000L
    val initialHours = (initialTotalMinutes / 60).toInt().coerceIn(0, 23)
    val initialMinutes = (initialTotalMinutes % 60).toInt().coerceIn(0, 59)

    var hoursText by remember { mutableStateOf(initialHours.toString()) }
    var minutesText by remember { mutableStateOf(initialMinutes.toString()) }

    val hours = hoursText.toIntOrNull() ?: 0
    val minutes = minutesText.toIntOrNull() ?: 0

    val totalDurationMillis = (hours * 60L + minutes) * 60_000L
    val isValid = totalDurationMillis >= 60_000L

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName, app.displayName)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (existingLimitMillis != null) "Edit Daily Limit" else "Set Daily Limit",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(app.displayName, style = MaterialTheme.typography.bodySmall, color = WarmGray)
                }
            }
        },
        text = {
            Column {
                Text(
                    "Set an exact daily time limit for ${app.displayName}. Enter custom hours and minutes or choose a quick preset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray
                )
                Spacer(Modifier.height(16.dp))

                // Current Total Preview Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, if (isValid) Caramel.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "DAILY ALLOWANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Caramel,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isValid) formatDurationDetailed(totalDurationMillis) else "Enter at least 1 minute",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isValid) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Hours and Minutes editable inputs side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hours Column
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "HOURS",
                            style = MaterialTheme.typography.labelMedium,
                            color = Caramel,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    val current = hoursText.toIntOrNull() ?: 0
                                    val newH = (current - 1).coerceAtLeast(0)
                                    hoursText = newH.toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Remove, "-", tint = Caramel)
                            }
                            OutlinedTextField(
                                value = hoursText,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(2)
                                    val num = filtered.toIntOrNull()
                                    if (filtered.isEmpty()) {
                                        hoursText = ""
                                    } else if (num != null && num in 0..23) {
                                        hoursText = num.toString()
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                singleLine = true,
                                modifier = Modifier.width(68.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Caramel,
                                    unfocusedBorderColor = DarkBrown,
                                    focusedContainerColor = CardBg,
                                    unfocusedContainerColor = CardBg
                                )
                            )
                            IconButton(
                                onClick = {
                                    val current = hoursText.toIntOrNull() ?: 0
                                    val newH = (current + 1).coerceAtMost(23)
                                    hoursText = newH.toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Add, "+", tint = Caramel)
                            }
                        }
                    }

                    // Minutes Column
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "MINUTES",
                            style = MaterialTheme.typography.labelMedium,
                            color = Caramel,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    val current = minutesText.toIntOrNull() ?: 0
                                    val newM = (current - 1).coerceAtLeast(0)
                                    minutesText = newM.toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Remove, "-", tint = Caramel)
                            }
                            OutlinedTextField(
                                value = minutesText,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(2)
                                    val num = filtered.toIntOrNull()
                                    if (filtered.isEmpty()) {
                                        minutesText = ""
                                    } else if (num != null && num in 0..59) {
                                        minutesText = num.toString()
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                singleLine = true,
                                modifier = Modifier.width(68.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Caramel,
                                    unfocusedBorderColor = DarkBrown,
                                    focusedContainerColor = CardBg,
                                    unfocusedContainerColor = CardBg
                                )
                            )
                            IconButton(
                                onClick = {
                                    val current = minutesText.toIntOrNull() ?: 0
                                    val newM = (current + 1).coerceAtMost(59)
                                    minutesText = newM.toString()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Add, "+", tint = Caramel)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Quick Presets
                Text(
                    "QUICK PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGray,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "5m" to (0 to 5),
                        "15m" to (0 to 15),
                        "30m" to (0 to 30),
                        "45m" to (0 to 45),
                        "1h" to (1 to 0),
                        "1h 30m" to (1 to 30),
                        "2h" to (2 to 0)
                    )
                    presets.forEach { (label, pair) ->
                        val (pHours, pMins) = pair
                        val isSelected = hours == pHours && minutes == pMins
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                hoursText = pHours.toString()
                                minutesText = pMins.toString()
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkBrown,
                                selectedLabelColor = Caramel,
                                containerColor = CardBg,
                                labelColor = WarmGray
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onSave(totalDurationMillis) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Caramel, contentColor = WarmBlack)
            ) {
                Text(if (existingLimitMillis != null) "Update Limit" else "Save Limit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onRemove != null && existingLimitMillis != null) {
                    OutlinedButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Text("Remove")
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color(0xFF4A423C))
                ) {
                    Text("Cancel", color = WarmGray)
                }
            }
        }
    )
}

// ─── DETAIL SCREEN (WITH TREND & EDIT/REMOVE LIMIT) ───

@Composable
private fun DetailScreen(viewModel: FocusViewModel, packageName: String, onBack: () -> Unit) {
    val apps by viewModel.limitedApps.collectAsStateWithLifecycle()
    val allApps by viewModel.managedApps.collectAsStateWithLifecycle()
    val history by viewModel.appHistory(packageName).collectAsStateWithLifecycle()
    val trend by viewModel.appTrend(packageName).collectAsStateWithLifecycle()
    val strict by viewModel.configuration.collectAsStateWithLifecycle()

    val row = apps.firstOrNull { it.packageName == packageName }
    val managedApp = allApps.firstOrNull { it.packageName == packageName } ?: ManagedApp(packageName, packageName, null, false)
    val displayName = row?.displayName ?: managedApp.displayName

    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, Color(0xFF4A423C)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("‹ Back", color = WarmGray) }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName, displayName)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(packageName, style = MaterialTheme.typography.bodySmall, color = WarmGray)
                }
            }
        }

        // Metrics Grid (Used Today + Remaining)
        item {
            val usedToday = row?.usageMillis ?: 0L
            val limitMillis = managedApp.limitMillis ?: row?.dailyLimitMillis
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Used Today", formatDuration(usedToday), Icons.Outlined.Timer, Modifier.weight(1f))
                MetricCard(
                    label = "Remaining",
                    value = if (limitMillis != null) formatDuration((limitMillis - usedToday).coerceAtLeast(0L)) else "No limit",
                    icon = Icons.Outlined.Shield,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Usage Trend Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, tint = Caramel, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Usage Trend", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(trend, style = MaterialTheme.typography.bodyMedium, color = WarmGray)
                    }
                }
            }
        }

        // Configured Daily Limit Card (Editable & Removable)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Daily Allowance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (managedApp.limitMillis != null) {
                        Text(
                            "Configured limit: ${formatDurationDetailed(managedApp.limitMillis)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Caramel,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enforce limit blocking", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = managedApp.enabled,
                                onCheckedChange = { viewModel.setEnabled(packageName, it) },
                                enabled = !strict.strictModeEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = WarmBlack,
                                    checkedTrackColor = Caramel,
                                    uncheckedThumbColor = WarmGray,
                                    uncheckedTrackColor = DarkBrown
                                )
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showEditDialog = true },
                                enabled = !strict.strictModeEnabled,
                                colors = ButtonDefaults.buttonColors(containerColor = Caramel, contentColor = WarmBlack),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit Limit", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.removeLimit(packageName) },
                                enabled = !strict.strictModeEnabled,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Remove Limit")
                            }
                        }
                    } else {
                        Text("No time limit configured for this application.", style = MaterialTheme.typography.bodyMedium, color = WarmGray)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { showEditDialog = true },
                            enabled = !strict.strictModeEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = Caramel, contentColor = WarmBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Set Daily Limit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Seven-day History Chart
        item {
            UsageHistoryCard("Seven-day usage", history)
        }

        // Blocking Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Blocking status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (row?.isBlocked == true) "Blocked until your next reset"
                        else if (managedApp.enabled && managedApp.limitMillis != null) "Active — FocusLock will redirect to Time's Up when the limit expires"
                        else "Inactive",
                        color = if (row?.isBlocked == true) MaterialTheme.colorScheme.error else WarmGray
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditLimitDialog(
            app = managedApp,
            existingLimitMillis = managedApp.limitMillis,
            onDismiss = { showEditDialog = false },
            onSave = { durationMillis ->
                viewModel.saveLimit(packageName, displayName, durationMillis, true)
                showEditDialog = false
            },
            onRemove = if (managedApp.limitMillis != null) {
                {
                    viewModel.removeLimit(packageName)
                    showEditDialog = false
                }
            } else null
        )
    }
}

// ─── REUSABLE UI WIDGETS ───

@Composable
private fun ScreenTimeRing(usedMillis: Long, modifier: Modifier = Modifier) {
    val hours = usedMillis / 3_600_000f
    val fraction = (hours / 8f).coerceIn(0f, 1f)
    val animatedFraction = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        animatedFraction.animateTo(fraction, tween(1200, easing = FastOutSlowInEasing))
    }
    val caramel = Caramel
    val trackColor = DarkBrown
    Canvas(modifier = modifier) {
        val strokeWidth = 10.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = caramel,
            startAngle = -90f,
            sweepAngle = animatedFraction.value * 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AnimatedDurationText(millis: Long) {
    var displayedMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(millis) {
        val start = displayedMillis
        val steps = 30
        for (i in 1..steps) {
            displayedMillis = start + ((millis - start) * i / steps)
            delay(20)
        }
        displayedMillis = millis
    }
    Text(
        formatDuration(displayedMillis),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun StaggeredFadeIn(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        content()
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val gradientBrush = Brush.verticalGradient(listOf(CardBg, SurfaceDark))
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier
                .background(gradientBrush)
                .padding(14.dp)
        ) {
            Column {
                Icon(icon, null, tint = Caramel, modifier = Modifier.size(18.dp))
                Spacer(Modifier.height(8.dp))
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text(label, style = MaterialTheme.typography.labelSmall, color = WarmGray)
            }
        }
    }
}

@Composable
private fun UsageCard(row: AppUsageRow, modifier: Modifier = Modifier) {
    val progressColor = if (row.isBlocked) MaterialTheme.colorScheme.error else Caramel
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(row.progress) {
        animatedProgress.animateTo(row.progress, tween(800, easing = FastOutSlowInEasing))
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(row.packageName, row.displayName)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        row.displayName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (row.isBlocked) "Blocked until reset"
                        else "${formatDuration(row.remainingMillis)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.isBlocked) MaterialTheme.colorScheme.error else WarmGray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatDuration(row.usageMillis),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        "of ${formatDuration(row.dailyLimitMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGray
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(DarkBrown)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animatedProgress.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    if (row.isBlocked) Color(0xFFFF6B6B) else Color(0xFFC49060),
                                    if (row.isBlocked) Color(0xFFFF4444) else Caramel
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, label: String) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkBrown,
        modifier = Modifier.size(44.dp)
    ) {
        if (bitmap != null)
            androidx.compose.foundation.Image(bitmap, null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
        else
            Box(contentAlignment = Alignment.Center) {
                Text(label.take(1).uppercase(), fontWeight = FontWeight.Black, color = Caramel)
            }
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Shield, null, tint = Caramel, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            textAlign = TextAlign.Center,
            color = WarmGray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun UsageHistoryCard(title: String, days: List<DayTotal>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (days.isEmpty()) Text("Usage history builds locally as FocusLock reconciles your device activity.", color = WarmGray)
            else UsageBars(days)
        }
    }
}

@Composable
private fun UsageBars(days: List<DayTotal>) {
    val max = days.maxOfOrNull { it.foregroundMillis }?.coerceAtLeast(1L) ?: 1L
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val animFraction by animateFloatAsState(if (entered) 1f else 0f, tween(900, easing = FastOutSlowInEasing), label = "bars")
    Row(
        Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val fraction = (day.foregroundMillis.toFloat() / max).coerceIn(0.04f, 1f)
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction * animFraction)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(
                            Brush.verticalGradient(listOf(Caramel, DarkBrown))
                        )
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    day.usageDate.takeLast(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGray
                )
            }
        }
    }
}

// ─── SETTINGS SCREEN ───

@Composable
private fun SettingsScreen(viewModel: FocusViewModel) {
    val context = LocalContext.current
    val config by viewModel.configuration.collectAsStateWithLifecycle()
    val dark by viewModel.darkTheme.collectAsStateWithLifecycle()
    val usageAccess = PermissionStatus.hasUsageAccess(context)
    val accessibility = PermissionStatus.isAccessibilityServiceEnabled(context)
    var showResetTimes by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Settings") }
        item {
            SettingsSection("Protection") {
                SettingsToggle("Strict mode", "Locks normal limit changes and data resets until turned off.", config.strictModeEnabled) { viewModel.setStrictMode(it) }
                HorizontalDivider(color = Color(0xFF332D28))
                SettingsAction("Daily reset", formatClockMinutes(config.resetMinutesAfterMidnight), enabled = !config.strictModeEnabled) { showResetTimes = true }
            }
        }
        item {
            SettingsSection("Appearance") {
                SettingsToggle("Dark theme", "Use FocusLock's dark-first color theme.", dark, viewModel::setDarkTheme)
            }
        }
        item {
            SettingsSection("Permissions") {
                SettingsAction("Usage access", if (usageAccess) "Granted ✓" else "Required") {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                HorizontalDivider(color = Color(0xFF332D28))
                SettingsAction("Accessibility service", if (accessibility) "Enabled ✓" else "Required") {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                HorizontalDivider(color = Color(0xFF332D28))
                SettingsAction("Battery optimization", "Open Android guidance") {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
        item {
            SettingsSection("Data") {
                SettingsAction("Reset FocusLock data", "Limits and local history", enabled = !config.strictModeEnabled, destructive = true) { confirmClear = true }
                HorizontalDivider(color = Color(0xFF332D28))
                SettingsAction("Replay welcome", "Show the FocusLock introduction again") { viewModel.resetOnboarding() }
            }
        }
        item {
            Text(
                "FocusLock works offline. Standard Android permissions cannot prevent force-stop, uninstall, Safe Mode, or permission revocation.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    if (showResetTimes) ResetTimeDialog(config.resetMinutesAfterMidnight, { showResetTimes = false }, viewModel::setResetMinutes)
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        containerColor = SurfaceDark,
        title = { Text("Reset local data?", fontWeight = FontWeight.Bold) },
        text = { Text("This removes local usage history and all limits. This cannot be undone.", color = WarmGray) },
        confirmButton = {
            Button(
                onClick = { viewModel.clearData(); confirmClear = false },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Reset", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { confirmClear = false },
                border = BorderStroke(1.dp, Color(0xFF4A423C))
            ) { Text("Cancel", color = WarmGray) }
        }
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                title.uppercase(),
                Modifier.padding(start = 18.dp, top = 16.dp, bottom = 6.dp),
                color = Caramel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WarmGray)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmBlack,
                checkedTrackColor = Caramel,
                uncheckedThumbColor = WarmGray,
                uncheckedTrackColor = DarkBrown
            )
        )
    }
}

@Composable
private fun SettingsAction(title: String, subtitle: String, enabled: Boolean = true, destructive: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WarmGray)
        }
        Text(
            "›",
            style = MaterialTheme.typography.headlineSmall,
            color = if (enabled) Caramel else Color(0xFF4A423C)
        )
    }
}

@Composable
private fun ResetTimeDialog(initial: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    val options = listOf(0, 60, 180, 360)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Daily reset time", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("A new FocusLock day starts at this local time.", color = WarmGray)
                Spacer(Modifier.height(14.dp))
                options.forEach { minute ->
                    FilterChip(
                        selected = selected == minute,
                        onClick = { selected = minute },
                        label = { Text(formatClockMinutes(minute)) },
                        modifier = Modifier.padding(end = 6.dp, bottom = 6.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkBrown,
                            selectedLabelColor = Caramel
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Caramel, contentColor = WarmBlack)
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Color(0xFF4A423C))
            ) { Text("Cancel", color = WarmGray) }
        }
    )
}
