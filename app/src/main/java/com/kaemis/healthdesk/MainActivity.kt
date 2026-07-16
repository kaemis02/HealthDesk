package com.kaemis.healthdesk

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.backup.BackupImportSummary
import com.kaemis.healthdesk.data.backup.BackupParseResult
import com.kaemis.healthdesk.data.backup.BackupService
import com.kaemis.healthdesk.data.backup.NativeBackupPayload
import com.kaemis.healthdesk.data.entity.CategoryEntity
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.TaskEntity
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import com.kaemis.healthdesk.domain.focus.MULTI_CYCLE_MODE_ID
import com.kaemis.healthdesk.domain.focus.NORMAL_TIMER_MODE_ID
import com.kaemis.healthdesk.domain.focus.POMODORO_MODE_ID
import com.kaemis.healthdesk.domain.focus.ResolvedFocusMode
import com.kaemis.healthdesk.domain.focus.focusModeOptions
import com.kaemis.healthdesk.domain.focus.resolveFocusMode
import com.kaemis.healthdesk.domain.stats.StatsSnapshot as LocalStatsSnapshot
import com.kaemis.healthdesk.ui.focus.FocusPhase
import com.kaemis.healthdesk.ui.focus.FocusUiState
import com.kaemis.healthdesk.ui.focus.FocusViewModel
import com.kaemis.healthdesk.ui.focus.WorkingHoursEvaluator
import com.kaemis.healthdesk.ui.profile.ProfileViewModel
import com.kaemis.healthdesk.ui.reminders.ReminderDraft
import com.kaemis.healthdesk.ui.reminders.RemindersViewModel
import com.kaemis.healthdesk.ui.reminders.toDraft
import com.kaemis.healthdesk.ui.settings.SettingsViewModel
import com.kaemis.healthdesk.ui.stats.StatsViewModel
import com.kaemis.healthdesk.ui.strings.AppStrings
import com.kaemis.healthdesk.ui.strings.stringsFor
import com.kaemis.healthdesk.ui.tasks.TasksUiState
import com.kaemis.healthdesk.ui.tasks.TasksViewModel
import com.kaemis.healthdesk.ui.theme.HealthDeskTheme
import com.kaemis.healthdesk.widgets.HealthDeskWidgetUpdater
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as HealthDeskApplication).appContainer

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(
                    settingsDataStore = appContainer.settingsDataStore,
                    workingHoursRepository = appContainer.workingHoursRepository,
                    resetLocalData = appContainer::resetLocalData,
                ),
            )
            val settings by settingsViewModel.settings.collectAsState()
            val useDarkTheme = when (settings.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val view = LocalView.current

            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }

            HealthDeskTheme(
                darkTheme = useDarkTheme,
                accentKey = settings.accentKey,
            ) {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.factory(appContainer.profileRepository),
                )
                val focusViewModel: FocusViewModel = viewModel(
                    factory = FocusViewModel.factory(
                        settingsFlow = appContainer.settingsDataStore.settings,
                        focusSessionRepository = appContainer.focusSessionRepository,
                        workingHoursRepository = appContainer.workingHoursRepository,
                        focusServiceController = appContainer.focusServiceController,
                        commandsFlow = appContainer.focusActionBus.commands,
                        pendingCommands = appContainer.focusActionBus::pendingCommands,
                        acknowledgeCommand = appContainer.focusActionBus::acknowledge,
                        focusAlarmScheduler = appContainer.focusAlarmScheduler,
                        alarmFeedbackController = appContainer.alarmFeedbackController,
                    ),
                )
                val remindersViewModel: RemindersViewModel = viewModel(
                    factory = RemindersViewModel.factory(
                        reminderRepository = appContainer.reminderRepository,
                        reminderAlarmScheduler = appContainer.reminderAlarmScheduler,
                    ),
                )
                val tasksViewModel: TasksViewModel = viewModel(
                    factory = TasksViewModel.factory(
                        taskRepository = appContainer.taskRepository,
                        settingsFlow = appContainer.settingsDataStore.settings,
                        taskFeedbackController = appContainer.taskFeedbackController,
                    ),
                )
                val statsViewModel: StatsViewModel = viewModel(
                    factory = StatsViewModel.factory(appContainer.statsRepository),
                )
                val workingHourRules by appContainer.workingHoursRepository.observeRules().collectAsState(initial = emptyList())
                HealthDeskApp(
                    profileViewModel = profileViewModel,
                    settingsViewModel = settingsViewModel,
                    focusViewModel = focusViewModel,
                    remindersViewModel = remindersViewModel,
                    tasksViewModel = tasksViewModel,
                    statsViewModel = statsViewModel,
                    workingHourRules = workingHourRules,
                    backupService = appContainer.backupService,
                    importNativeBackup = appContainer::importNativeBackup,
                )
            }
        }
    }
}

private enum class AppDestination(
    val icon: ImageVector,
) {
    Dashboard(Icons.Outlined.Home),
    Reminders(Icons.Outlined.Notifications),
    Tasks(Icons.Outlined.TaskAlt),
    Stats(Icons.Outlined.Insights),
    Settings(Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthDeskApp(
    profileViewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel,
    focusViewModel: FocusViewModel,
    remindersViewModel: RemindersViewModel,
    tasksViewModel: TasksViewModel,
    statsViewModel: StatsViewModel,
    workingHourRules: List<WorkingHourRuleEntity>,
    backupService: BackupService,
    importNativeBackup: suspend (NativeBackupPayload) -> BackupImportSummary,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val requestNotificationsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    var selectedDestination by remember { mutableStateOf(AppDestination.Dashboard) }
    val profile by profileViewModel.profile.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val focusState by focusViewModel.state.collectAsState()
    val reminders by remindersViewModel.reminders.collectAsState()
    val tasksState by tasksViewModel.state.collectAsState()
    val stats by statsViewModel.stats.collectAsState()
    val strings = stringsFor(settings.languageCode)

    LaunchedEffect(
        profile.displayName,
        profile.updatedAt,
        settings.timerMode,
        settings.customFocusModes,
        settings.languageCode,
    ) {
        HealthDeskWidgetUpdater.updateAll(context)
    }

    LaunchedEffect(settings.notificationsEnabled) {
        if (settings.notificationsEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HealthDeskDrawer(
                profile = profile,
                profileViewModel = profileViewModel,
                strings = strings,
                selectedDestination = selectedDestination,
                onDestinationSelected = { destination ->
                    selectedDestination = destination
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedDestination.label(strings)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = strings.openNavigationDrawer,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background,
            ) {
                DestinationContent(
                    destination = selectedDestination,
                    profile = profile,
                    settings = settings,
                    focusState = focusState,
                    workingHourRules = workingHourRules,
                    reminders = reminders,
                    tasksState = tasksState,
                    stats = stats,
                    strings = strings,
                    settingsViewModel = settingsViewModel,
                    focusViewModel = focusViewModel,
                    remindersViewModel = remindersViewModel,
                    tasksViewModel = tasksViewModel,
                    profileViewModel = profileViewModel,
                    backupService = backupService,
                    importNativeBackup = importNativeBackup,
                    onDestinationSelected = { selectedDestination = it },
                )
            }
        }
    }
}

@Composable
private fun HealthDeskDrawer(
    profile: UserProfileEntity,
    profileViewModel: ProfileViewModel,
    strings: AppStrings,
    selectedDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    val context = LocalContext.current
    var showProfileEditor by remember { mutableStateOf(false) }

    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        DrawerHeader(profile, strings, onEditProfile = { showProfileEditor = true })
        AppDestination.entries.forEach { destination ->
            NavigationDrawerItem(
                label = { Text(destination.label(strings)) },
                selected = destination == selectedDestination,
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                onClick = { onDestinationSelected(destination) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f, fill = true))
        TextButton(
            onClick = { openExternalLink(context, "https://buymeacoffee.com/kaemis") },
            modifier = Modifier.padding(16.dp),
        ) {
            Text(strings.buyMeACoffee)
        }
    }
    if (showProfileEditor) {
        ProfileEditDialog(
            profile = profile,
            profileViewModel = profileViewModel,
            strings = strings,
            onDismiss = { showProfileEditor = false },
        )
    }
}

@Composable
private fun DrawerHeader(
    profile: UserProfileEntity,
    strings: AppStrings,
    onEditProfile: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(profile = profile, strings = strings, size = 56.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = strings.storedOnThisDevice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onEditProfile) {
            Text(strings.edit)
        }
    }
}

@Composable
private fun DestinationContent(
    destination: AppDestination,
    profile: UserProfileEntity,
    settings: SettingsSnapshot,
    focusState: FocusUiState,
    workingHourRules: List<WorkingHourRuleEntity>,
    reminders: List<ReminderEntity>,
    tasksState: TasksUiState,
    stats: LocalStatsSnapshot,
    strings: AppStrings,
    settingsViewModel: SettingsViewModel,
    focusViewModel: FocusViewModel,
    remindersViewModel: RemindersViewModel,
    tasksViewModel: TasksViewModel,
    profileViewModel: ProfileViewModel,
    backupService: BackupService,
    importNativeBackup: suspend (NativeBackupPayload) -> BackupImportSummary,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    val screenScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (destination == AppDestination.Dashboard || destination == AppDestination.Tasks) {
                    Modifier
                } else {
                    Modifier.verticalScroll(screenScrollState)
                },
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(if (destination == AppDestination.Dashboard) 10.dp else 16.dp),
    ) {
        when (destination) {
            AppDestination.Dashboard -> DashboardScreen(
                profile = profile,
                settings = settings,
                focusState = focusState,
                workingHourRules = workingHourRules,
                reminders = reminders,
                tasksState = tasksState,
                stats = stats,
                strings = strings,
                focusViewModel = focusViewModel,
                settingsViewModel = settingsViewModel,
                onOpenReminders = { onDestinationSelected(AppDestination.Reminders) },
                onOpenTasks = { onDestinationSelected(AppDestination.Tasks) },
            )
            AppDestination.Reminders -> RemindersScreen(reminders, settings, strings, remindersViewModel)
            AppDestination.Tasks -> TasksScreen(tasksState, strings, tasksViewModel)
            AppDestination.Stats -> StatsScreen(stats, strings)
            AppDestination.Settings -> SettingsScreen(
                profile,
                profileViewModel,
                settings,
                workingHourRules,
                settingsViewModel,
                backupService,
                importNativeBackup,
                strings,
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    profile: UserProfileEntity,
    settings: SettingsSnapshot,
    focusState: FocusUiState,
    workingHourRules: List<WorkingHourRuleEntity>,
    reminders: List<ReminderEntity>,
    tasksState: TasksUiState,
    stats: LocalStatsSnapshot,
    strings: AppStrings,
    focusViewModel: FocusViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenReminders: () -> Unit,
    onOpenTasks: () -> Unit,
) {
    FocusCard(settings, focusState, workingHourRules, stats, strings, focusViewModel, settingsViewModel)
    DashboardReminderSummary(reminders, strings, onOpenReminders)
    DashboardTaskSummary(tasksState.pendingTasks, strings, onOpenTasks)
}

@Composable
private fun FocusCard(
    settings: SettingsSnapshot,
    state: FocusUiState,
    workingHourRules: List<WorkingHourRuleEntity>,
    stats: LocalStatsSnapshot,
    strings: AppStrings,
    focusViewModel: FocusViewModel,
    settingsViewModel: SettingsViewModel,
) {
    var showEditor by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showNewCustomModeDialog by remember { mutableStateOf(false) }
    var newCustomModeName by remember { mutableStateOf("") }
    val selectedMode = settings.resolveFocusMode()
    val alarmActive = state.phase == FocusPhase.FocusAlarm || state.phase == FocusPhase.RestAlarm
    val idleLike = state.phase == FocusPhase.Idle || state.phase == FocusPhase.Completed || state.phase == FocusPhase.Stopped
    val displaySeconds = if (idleLike) selectedMode.workMinutes.toLong() * 60L else state.remainingSeconds
    val insideWorkingHours = !settings.workingHoursEnabled || workingHourRules.isEmpty() || WorkingHoursEvaluator.isWithinWorkingHours(workingHourRules, System.currentTimeMillis())

    if (state.pendingOutsideWorkingHoursConfirmation) {
        AlertDialog(
            onDismissRequest = focusViewModel::cancelOutsideWorkingHoursStart,
            title = { Text(strings.startOutsideHoursTitle) },
            text = { Text(strings.startOutsideHoursMessage) },
            confirmButton = {
                TextButton(onClick = focusViewModel::startConfirmed) {
                    Text(strings.startAnyway)
                }
            },
            dismissButton = {
                TextButton(onClick = focusViewModel::cancelOutsideWorkingHoursStart) {
                    Text(strings.cancel)
                }
            },
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = strings.focusSession,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = strings.modeSubtitle(focusModeTitle(selectedMode, strings)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showEditor = true }) {
                    Icon(Icons.Outlined.Settings, contentDescription = strings.edit)
                }
            }

            if (shouldShowStatusPill(state.phase)) {
                StatusPill(focusStatusLabel(state.phase, strings))
            }

            if (settings.workingHoursEnabled && insideWorkingHours) {
                CompactChip(workingHoursStatusText(settings.languageCode, insideWorkingHours))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .semantics {
                        contentDescription = "${focusStatusLabel(state.phase, strings)}, ${formatDuration(displaySeconds)}"
                        role = Role.Button
                    }
                    .clickable {
                        if (alarmActive) {
                            focusViewModel.stopAlarm()
                        } else {
                            showStats = true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { if (idleLike) 0f else progressFor(state) },
                    modifier = Modifier.size(250.dp),
                    strokeWidth = 14.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = formatDuration(displaySeconds),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = focusCaption(state.phase, alarmActive, settings.languageCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (alarmActive) {
                    Button(onClick = focusViewModel::stopAlarm, modifier = Modifier.weight(1f)) {
                        Text(strings.stopAlarm)
                    }
                    OutlinedButton(onClick = focusViewModel::snooze, modifier = Modifier.weight(1f)) {
                        Text(strings.snooze)
                    }
                } else {
                    FocusPrimaryButton(state, strings, focusViewModel, Modifier.weight(1f))
                }
                if (showPauseResumeButton(state.phase) && !alarmActive) {
                    OutlinedButton(
                        onClick = if (state.phase == FocusPhase.FocusPaused || state.phase == FocusPhase.RestPaused) {
                            focusViewModel::resume
                        } else {
                            focusViewModel::pause
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.phase == FocusPhase.FocusPaused || state.phase == FocusPhase.RestPaused) strings.resume else strings.pause)
                    }
                }
            }
        }
    }

    if (showStats) {
        AlertDialog(
            onDismissRequest = { showStats = false },
            title = { Text(strings.sessionStats) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${strings.focusMinutesToday}: ${stats.focusMinutesToday}")
                    Text("${strings.completedTasks}: ${stats.completedTasksToday}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showStats = false }) {
                    Text(strings.done)
                }
            },
        )
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(strings.focusSession) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(strings.timerModeLabel, style = MaterialTheme.typography.labelLarge)
                    settings.focusModeOptions().forEach { mode ->
                        FocusModeChoiceCard(
                            mode = mode,
                            selected = settings.timerMode == mode.id,
                            strings = strings,
                            languageCode = settings.languageCode,
                            onSelect = { settingsViewModel.updateTimerMode(mode.id) },
                        )
                    }
                    OutlinedButton(onClick = {
                        newCustomModeName = ""
                        showNewCustomModeDialog = true
                    }) {
                        Text(strings.newCustomMode)
                    }
                    if (selectedMode.isCustom) {
                        OutlinedButton(
                            onClick = { settingsViewModel.deleteCustomFocusMode(selectedMode.id) },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(strings.delete)
                        }
                    }

                    if (selectedMode.isCustom) {
                        OutlinedTextField(
                            value = selectedMode.name,
                            onValueChange = { settingsViewModel.updateFocusModeName(selectedMode.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(strings.focusModeName) },
                        )
                    }

                    if (selectedMode.isCustom) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                NORMAL_TIMER_MODE_ID to strings.normal,
                                POMODORO_MODE_ID to strings.pomodoro,
                                MULTI_CYCLE_MODE_ID to strings.multiCycle,
                            ).forEach { (type, label) ->
                                SegmentedActionButton(
                                    selected = selectedMode.type == type,
                                    onClick = { settingsViewModel.updateFocusModeType(selectedMode.id, type) },
                                    text = label,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    NumberFieldRow(strings.workMinutes, selectedMode.workMinutes, 1, 120) {
                        settingsViewModel.updateFocusWorkSessionMinutes(selectedMode.id, it)
                    }
                    NumberFieldRow(strings.restMinutes, selectedMode.restMinutes, 0, 60) {
                        settingsViewModel.updateFocusRestMinutes(selectedMode.id, it)
                    }
                    NumberFieldRow(strings.snoozeMinutes, selectedMode.snoozeMinutes, 1, 30) {
                        settingsViewModel.updateFocusSnoozeMinutes(selectedMode.id, it)
                    }
                    if (selectedMode.type == POMODORO_MODE_ID) {
                        NumberFieldRow(strings.pomodoroCycles, selectedMode.pomodoroCycles, 1, 12) {
                            settingsViewModel.updateFocusPomodoroCycles(selectedMode.id, it)
                        }
                        NumberFieldRow(strings.pomodoroShortRest, selectedMode.pomodoroShortRestMinutes, 0, 60) {
                            settingsViewModel.updateFocusPomodoroShortRestMinutes(selectedMode.id, it)
                        }
                        NumberFieldRow(strings.pomodoroLongRest, selectedMode.pomodoroLongRestMinutes, 0, 60) {
                            settingsViewModel.updateFocusPomodoroLongRestMinutes(selectedMode.id, it)
                        }
                    }
                    if (selectedMode.type == MULTI_CYCLE_MODE_ID) {
                        NumberFieldRow(strings.multiCycleCycles, selectedMode.multiCycleCycles, 1, 12) {
                            settingsViewModel.updateFocusMultiCycleCycles(selectedMode.id, it)
                        }
                    }

                }
            },
            confirmButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text(strings.done)
                }
            },
        )
    }

    if (showNewCustomModeDialog) {
        AlertDialog(
            onDismissRequest = { showNewCustomModeDialog = false },
            title = { Text(strings.newCustomMode) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCustomModeName,
                        onValueChange = { newCustomModeName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.focusModeName) },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.createCustomFocusMode(newCustomModeName, selectedMode.type)
                    showNewCustomModeDialog = false
                }) {
                    Text(strings.done)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCustomModeDialog = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FocusModeChoiceCard(
    mode: ResolvedFocusMode,
    selected: Boolean,
    strings: AppStrings,
    languageCode: String,
    onSelect: () -> Unit,
) {
    var showInfo by remember(mode.id) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = focusModeTitle(mode, strings),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = focusModeSummary(mode, strings, languageCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Outlined.Info, contentDescription = strings.edit)
            }
            if (selected) {
                Text(
                    text = strings.enabled,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(focusModeTitle(mode, strings)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(focusModeDescription(mode, strings, languageCode))
                    Text(cyclePreviewText(mode, strings), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text(strings.done)
                }
            },
        )
    }
}

@Composable
private fun WorkingHourRuleEditorCard(
    rule: WorkingHourRuleEntity,
    strings: AppStrings,
    languageCode: String,
    onRuleChange: (WorkingHourRuleEntity) -> Unit,
) {
    val locale = localeForLanguage(languageCode)
    val dayLabel = java.time.DayOfWeek.of(rule.dayOfWeek).getDisplayName(TextStyle.SHORT, locale)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dayLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Checkbox(
                    checked = rule.isEnabled,
                    onCheckedChange = { enabled -> onRuleChange(rule.copy(isEnabled = enabled, updatedAt = System.currentTimeMillis())) },
                )
            }
            TimeStepperRow(
                label = strings.startTime,
                value = rule.startLocalTime,
                enabled = rule.isEnabled,
            ) { updatedTime ->
                onRuleChange(rule.copy(startLocalTime = updatedTime, updatedAt = System.currentTimeMillis()))
            }
            TimeStepperRow(
                label = strings.endTime,
                value = rule.endLocalTime,
                enabled = rule.isEnabled,
            ) { updatedTime ->
                onRuleChange(rule.copy(endLocalTime = updatedTime, updatedAt = System.currentTimeMillis()))
            }
            if (rule.startLocalTime == rule.endLocalTime && rule.isEnabled) {
                Text(
                    text = strings.allDay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TimeStepperRow(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(enabled = enabled, onClick = { onValueChange(adjustTime(value, -30)) }) {
                Text("-30")
            }
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedButton(enabled = enabled, onClick = { onValueChange(adjustTime(value, 30)) }) {
                Text("+30")
            }
        }
    }
}

@Composable
private fun DashboardReminderSummary(
    reminders: List<ReminderEntity>,
    strings: AppStrings,
    onClick: () -> Unit,
) {
    SectionCard(
        title = strings.reminders,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        val upcoming = reminders.filter { it.isEnabled }.sortedBy { it.nextScheduledAt ?: Long.MAX_VALUE }.take(4)
        if (upcoming.isEmpty()) {
            Text(strings.noRemindersDashboard)
        } else {
            upcoming.forEach { reminder ->
                Text("${reminder.title}  ${reminder.nextScheduledAt?.let(::formatReminderTime).orEmpty()}")
            }
        }
    }
}

@Composable
private fun DashboardTaskSummary(
    tasks: List<TaskEntity>,
    strings: AppStrings,
    onClick: () -> Unit,
) {
    SectionCard(
        title = strings.tasks,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        if (tasks.isEmpty()) {
            Text(strings.noTasksDashboard)
        } else {
            tasks.take(3).forEach { task -> Text(task.title) }
        }
    }
}

@Composable
private fun FocusScreen(
    state: FocusUiState,
    strings: AppStrings,
    focusViewModel: FocusViewModel,
) {
    if (state.pendingOutsideWorkingHoursConfirmation) {
        AlertDialog(
            onDismissRequest = focusViewModel::cancelOutsideWorkingHoursStart,
            title = { Text(strings.startOutsideHoursTitle) },
            text = { Text(strings.startOutsideHoursMessage) },
            confirmButton = {
                TextButton(onClick = focusViewModel::startConfirmed) {
                    Text(strings.startAnyway)
                }
            },
            dismissButton = {
                TextButton(onClick = focusViewModel::cancelOutsideWorkingHoursStart) {
                    Text(strings.cancel)
                }
            },
        )
    }

    SectionCard(title = strings.focusToday) {
        val alarmActive = state.phase == FocusPhase.FocusAlarm || state.phase == FocusPhase.RestAlarm
        StatusPill(focusStatusLabel(state.phase, strings))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .semantics {
                    contentDescription = "${focusStatusLabel(state.phase, strings)}, ${formatDuration(state.remainingSeconds)}"
                    if (alarmActive) role = Role.Button
                }
                .clickable(enabled = alarmActive, onClick = focusViewModel::stopAlarm),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { progressFor(state) },
                modifier = Modifier.size(220.dp),
                strokeWidth = 12.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDuration(state.remainingSeconds),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = focusStatusLabel(state.phase, strings),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FocusPrimaryButton(state, strings, focusViewModel, Modifier.weight(1f))
            if (state.phase != FocusPhase.Idle && state.phase != FocusPhase.Completed && state.phase != FocusPhase.Stopped) {
                OutlinedButton(onClick = focusViewModel::stop, modifier = Modifier.weight(1f)) {
                    Text(strings.stop)
                }
            }
        }

        if (state.phase == FocusPhase.FocusAlarm || state.phase == FocusPhase.RestAlarm) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = focusViewModel::stopAlarm) {
                    Text(strings.stopAlarm)
                }
                OutlinedButton(onClick = focusViewModel::snooze) {
                    Text(strings.snooze)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FocusPrimaryButton(
    state: FocusUiState,
    strings: AppStrings,
    focusViewModel: FocusViewModel,
    modifier: Modifier = Modifier,
) {
    val (label, action) = when (state.phase) {
        FocusPhase.FocusRunning,
        FocusPhase.RestRunning,
        FocusPhase.Snoozed,
        -> strings.stop to focusViewModel::stop
        FocusPhase.FocusPaused,
        FocusPhase.RestPaused,
        -> strings.start to focusViewModel::start
        FocusPhase.FocusAlarm,
        FocusPhase.RestAlarm,
        -> strings.stopAlarm to focusViewModel::stopAlarm
        else -> strings.start to focusViewModel::start
    }
    Button(onClick = action, modifier = modifier) {
        Text(label)
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ListCard(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun IconBubble(text: String, active: Boolean = true) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, active: Boolean = true) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null)
        }
    }
}

@Composable
private fun CompactChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .defaultMinSize(minHeight = 32.dp)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun CategoryChips(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onSelected: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onSelected(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

@Composable
private fun RemindersScreen(
    reminders: List<ReminderEntity>,
    settings: SettingsSnapshot,
    strings: AppStrings,
    remindersViewModel: RemindersViewModel,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    ScreenHeader(title = strings.reminders, actionLabel = strings.newItem, onAction = { showCreateDialog = true })

    if (reminders.isEmpty()) {
        SectionCard(title = strings.reminders) {
            Text(strings.noReminders)
        }
    } else {
        reminders.forEach { reminder ->
            ReminderCard(reminder, settings, strings, remindersViewModel)
        }
    }

    if (showCreateDialog) {
        ReminderEditorDialog(
            initialDraft = ReminderDraft(soundKey = "ring2", iconKey = "notifications"),
            settings = settings,
            strings = strings,
            onDismiss = { showCreateDialog = false },
            onSave = { draft ->
                remindersViewModel.createReminder(draft)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    settings: SettingsSnapshot,
    strings: AppStrings,
    remindersViewModel: RemindersViewModel,
) {
    var showEditor by remember(reminder.id) { mutableStateOf(false) }

    ListCard {
        IconBubble(icon = reminderIcon(reminder.iconKey), active = reminder.isEnabled)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactChip(if (reminder.isEnabled) strings.enabled else strings.disabled)
            }
            reminder.nextScheduledAt?.let { next ->
                Text(
                    strings.nextReminder(formatReminderTime(next)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = reminder.isEnabled, onCheckedChange = { remindersViewModel.toggleReminder(reminder) })
        OutlinedButton(onClick = { showEditor = true }) {
            Text(strings.edit)
        }
    }

    if (showEditor) {
        ReminderEditorDialog(
            initialDraft = reminder.toDraft(),
            settings = settings,
            strings = strings,
            onDismiss = { showEditor = false },
            onDelete = { remindersViewModel.deleteReminder(reminder) },
            onSave = { draft ->
                remindersViewModel.saveReminder(reminder, draft)
                showEditor = false
            },
        )
    }
}

@Composable
private fun ReminderEditorDialog(
    initialDraft: ReminderDraft,
    settings: SettingsSnapshot,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (ReminderDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var draft by remember(initialDraft) { mutableStateOf(initialDraft) }
    var showIconPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addReminder) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    label = { Text(strings.reminderTitle) },
                    leadingIcon = {
                        IconButton(onClick = { showIconPicker = true }) {
                            Icon(reminderIcon(draft.iconKey), contentDescription = draft.iconKey)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "interval" to strings.intervalLabel,
                        "fixedTimeOnce" to strings.fixedTimeLabel,
                        "recurring" to strings.recurringLabel,
                    ).forEach { (mode, label) ->
                        SegmentedActionButton(
                            selected = draft.scheduleMode == mode,
                            onClick = { draft = draft.copy(scheduleMode = mode) },
                            text = label,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (draft.scheduleMode == "interval") {
                    OutlinedTextField(
                        value = draft.intervalMinutes.toString(),
                        onValueChange = { value ->
                            val parsed = value.toIntOrNull()
                            if (parsed != null || value.isBlank()) {
                                draft = draft.copy(intervalMinutes = parsed?.coerceAtLeast(1) ?: 1)
                            }
                        },
                        label = { Text(strings.everyMinutesLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    TimePickerFieldRow(
                        label = strings.fixedTimeLabel,
                        value = draft.fixedLocalTime,
                        enabled = true,
                        strings = strings,
                    ) { updatedTime ->
                        draft = draft.copy(fixedLocalTime = updatedTime)
                    }
                }

                if (draft.scheduleMode == "recurring") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "daily" to recurrenceUnitLabel("daily", settings.languageCode),
                            "weekly" to recurrenceUnitLabel("weekly", settings.languageCode),
                            "monthly" to recurrenceUnitLabel("monthly", settings.languageCode),
                        ).forEach { (unit, label) ->
                            SegmentedActionButton(
                                selected = draft.recurrenceUnit == unit,
                                onClick = { draft = draft.copy(recurrenceUnit = unit) },
                                text = label,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (draft.recurrenceUnit != "daily") {
                        OutlinedTextField(
                            value = draft.recurrenceInterval.toString(),
                            onValueChange = { value ->
                                val parsed = value.toIntOrNull()
                                if (parsed != null || value.isBlank()) {
                                    draft = draft.copy(recurrenceInterval = parsed?.coerceAtLeast(1) ?: 1)
                                }
                            },
                            label = { Text(strings.recurringLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (draft.recurrenceUnit == "weekly") {
                        WeekdaySelector(
                            selected = draft.recurrenceWeekdays,
                            days = listOf(1, 2, 3, 4, 5),
                            onSelected = { selected -> draft = draft.copy(recurrenceWeekdays = selected) },
                        )
                    }
                    if (draft.recurrenceUnit == "monthly") {
                        MonthDaySelector(
                            selected = draft.monthlyDays,
                            languageCode = settings.languageCode,
                            onSelected = { selected -> draft = draft.copy(monthlyDays = selected) },
                        )
                    }
                }

            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(strings.done)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(strings.delete)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.cancel)
                }
            }
        },
    )

    if (showIconPicker) {
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            title = { Text(strings.iconLabel) },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reminderIconOptions().forEach { (key, icon) ->
                        Surface(
                            modifier = Modifier.clickable {
                                draft = draft.copy(iconKey = key)
                                showIconPicker = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (draft.iconKey == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = key)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPicker = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

@Composable
private fun WeekdaySelector(
    selected: Set<Int>,
    days: List<Int> = (1..7).toList(),
    singleSelection: Boolean = false,
    onSelected: (Set<Int>) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            val label = java.time.DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            FilterChip(
                selected = selected.contains(day),
                onClick = {
                    val updated = if (singleSelection) {
                        setOf(day)
                    } else if (selected.contains(day)) {
                        (selected - day).ifEmpty { setOf(day) }
                    } else {
                        selected + day
                    }
                    onSelected(updated)
                },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun MonthDaySelector(
    selected: Set<Int>,
    languageCode: String,
    onSelected: (Set<Int>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (languageCode == "es") "Dias del mes" else "Days of the month",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (languageCode == "es") {
                "En meses cortos, 29-31 se programan el ultimo dia valido."
            } else {
                "In shorter months, days 29-31 run on the last valid day."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..31).forEach { day ->
                val isSelected = selected.contains(day)
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            val updated = if (isSelected) (selected - day).ifEmpty { setOf(day) } else selected + day
                            onSelected(updated)
                        }
                        .semantics { contentDescription = if (languageCode == "es") "Dia $day" else "Day $day" },
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(day.toString(), style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

private fun reminderIconOptions(): List<Pair<String, ImageVector>> = listOf(
    "notifications" to Icons.Outlined.Notifications,
    "water_drop" to Icons.Outlined.WaterDrop,
    "visibility" to Icons.Outlined.Visibility,
    "directions_walk" to Icons.AutoMirrored.Outlined.DirectionsWalk,
    "air" to Icons.Outlined.Air,
    "local_cafe" to Icons.Outlined.LocalCafe,
    "schedule" to Icons.Outlined.Schedule,
)

private fun reminderIcon(iconKey: String): ImageVector = reminderIconOptions().firstOrNull { it.first == iconKey }?.second
    ?: Icons.Outlined.Notifications

@Composable
private fun ColumnScope.TasksScreen(
    state: TasksUiState,
    strings: AppStrings,
    tasksViewModel: TasksViewModel,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val orderedPendingTasks = remember { mutableStateListOf<TaskEntity>() }
    LaunchedEffect(state.pendingTasks) {
        val incomingById = state.pendingTasks.associateBy { it.id }
        if (orderedPendingTasks.map { it.id }.toSet() != incomingById.keys) {
            orderedPendingTasks.clear()
            orderedPendingTasks.addAll(state.pendingTasks)
        } else {
            orderedPendingTasks.indices.forEach { index ->
                incomingById[orderedPendingTasks[index].id]?.let { orderedPendingTasks[index] = it }
            }
        }
    }

    ScreenHeader(title = strings.tasks, actionLabel = strings.newItem, onAction = { showCreateDialog = true })

    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.weight(1f),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (orderedPendingTasks.isEmpty()) {
            item(key = "pending-empty") {
                SectionCard(title = strings.tasks) { Text(strings.noTasks) }
            }
        } else {
            items(orderedPendingTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    strings = strings,
                    tasksViewModel = tasksViewModel,
                    modifier = Modifier.animateItem(),
                    onMove = { direction ->
                        val from = orderedPendingTasks.indexOfFirst { it.id == task.id }
                        val to = (from + direction).coerceIn(orderedPendingTasks.indices)
                        if (from != -1 && from != to) {
                            orderedPendingTasks.removeAt(from)
                            orderedPendingTasks.add(to, task)
                        }
                    },
                    onDragFinished = { tasksViewModel.reorderTasks(orderedPendingTasks.toList()) },
                )
            }
        }
        item(key = "completed-header") {
            Text(strings.completedTasks, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }
        if (state.completedTasks.isEmpty()) {
            item(key = "completed-empty") { Text(strings.noTasks) }
        } else {
            items(state.completedTasks.take(12), key = { "completed-${it.id}" }) { task ->
                CompletedTaskRow(task, strings, tasksViewModel)
            }
        }
    }

    if (showCreateDialog) {
        TaskEditorDialog(
            dialogTitle = strings.addTask,
            title = "",
            strings = strings,
            onDismiss = { showCreateDialog = false },
            onSave = {
                tasksViewModel.createTask(it)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    strings: AppStrings,
    tasksViewModel: TasksViewModel,
    modifier: Modifier = Modifier,
    onMove: (Int) -> Unit = { direction -> tasksViewModel.moveTask(task, direction) },
    onDragFinished: () -> Unit = {},
) {
    var isEditing by remember(task.id, task.title) { mutableStateOf(false) }
    var dragAmount by remember(task.id) { mutableStateOf(0f) }
    val dragOffset by animateFloatAsState(targetValue = dragAmount, label = "taskDrag")

    ListCard(
        modifier = modifier.graphicsLayer {
            translationY = dragOffset
            alpha = if (dragAmount == 0f) 1f else 0.96f
        },
    ) {
        Checkbox(
            checked = false,
            onCheckedChange = { tasksViewModel.completeTask(task) },
            modifier = Modifier.semantics { contentDescription = "${strings.complete}: ${task.title}" },
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(onClick = { isEditing = true }) {
            Text(strings.edit)
        }
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "${strings.moveUp} / ${strings.moveDown}",
            modifier = Modifier
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(strings.moveUp) {
                            onMove(-1)
                            onDragFinished()
                            true
                        },
                        CustomAccessibilityAction(strings.moveDown) {
                            onMove(1)
                            onDragFinished()
                            true
                        },
                    )
                }
                .pointerInput(task.id) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, drag ->
                        dragAmount += drag
                        when {
                            dragAmount > 72f -> {
                                onMove(1)
                                dragAmount -= 72f
                            }
                            dragAmount < -72f -> {
                                onMove(-1)
                                dragAmount += 72f
                            }
                        }
                    },
                    onDragEnd = {
                        dragAmount = 0f
                        onDragFinished()
                    },
                    onDragCancel = { dragAmount = 0f },
                )
            },
        )
    }

    if (isEditing) {
        TaskEditorDialog(
            dialogTitle = strings.edit,
            title = task.title,
            strings = strings,
            onDismiss = { isEditing = false },
            onDelete = { tasksViewModel.deleteTask(task) },
            onSave = {
                tasksViewModel.editTask(task, it)
                isEditing = false
            },
        )
    }
}

@Composable
private fun CompletedTaskRow(
    task: TaskEntity,
    strings: AppStrings,
    tasksViewModel: TasksViewModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title, color = MaterialTheme.colorScheme.onSurface)
            Text(
                formatCompletedAt(task.completedAt ?: task.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = { tasksViewModel.restoreTask(task) }) {
            Text(strings.resume)
        }
    }
}

@Composable
private fun TaskEditorDialog(
    dialogTitle: String,
    title: String,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
) {
    var draftTitle by remember(title) { mutableStateOf(title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    label = { Text(strings.taskTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                footer?.invoke()
            }
        },
        confirmButton = {
            Button(onClick = { onSave(draftTitle) }) {
                Text(strings.done)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(strings.delete)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.cancel)
                }
            }
        },
    )
}

@Composable
private fun StatsScreen(
    stats: LocalStatsSnapshot,
    strings: AppStrings,
) {
    val allDays = stats.dailyStats
    val today = allDays.lastOrNull()
    var showCalendar by remember { mutableStateOf(false) }
    var chartRange by remember { mutableStateOf("week") }
    var visibleSeries by remember { mutableStateOf(setOf("focus", "tasks", "reminders")) }
    val selectedDate = remember(allDays) { mutableStateOf(allDays.lastOrNull()?.date ?: LocalDate.now().toString()) }
    val selectedDay = allDays.firstOrNull { it.date == selectedDate.value } ?: allDays.lastOrNull()
    val chartDays = remember(allDays, chartRange) {
        if (chartRange == "week") allDays.takeLast(7) else allDays
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(strings.stats, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = { showCalendar = true }) {
            Icon(Icons.Outlined.Schedule, contentDescription = strings.stats)
        }
    }

    today?.let { day ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(strings.focusMinutesToday, day.focusMinutes.toString(), Modifier.weight(1f))
            SummaryTile(strings.focusSessionsToday, day.focusSessions.toString(), Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(strings.completedTasks, day.completedTasks.toString(), Modifier.weight(1f))
            SummaryTile(strings.remindersToday, day.reminders.toString(), Modifier.weight(1f))
        }
    }

    SectionCard(title = strings.stats) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedActionButton(selected = chartRange == "week", text = "Week", onClick = { chartRange = "week" }, modifier = Modifier.weight(1f))
            SegmentedActionButton(selected = chartRange == "month", text = "Month", onClick = { chartRange = "month" }, modifier = Modifier.weight(1f))
        }
        StatsLegend(
            visibleSeries = visibleSeries,
            onToggle = { key ->
                visibleSeries = if (visibleSeries.contains(key)) visibleSeries - key else visibleSeries + key
            },
        )
        ActivityLineChart(days = chartDays, visibleSeries = visibleSeries)
    }

    SectionCard(title = strings.recentCompletedTasks) {
        if (stats.recentCompletedTasks.isEmpty()) {
            Text(strings.noTasks)
        } else {
            stats.recentCompletedTasks.forEach { task ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(task.title)
                    Text(
                        formatCompletedAt(task.completedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showCalendar) {
        AlertDialog(
            onDismissRequest = { showCalendar = false },
            title = { Text(strings.stats) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatsCalendar(
                        days = allDays,
                        selectedDate = selectedDate.value,
                        onSelectDate = { selectedDate.value = it },
                    )
                    selectedDay?.let { day ->
                        Text("${strings.focusMinutesToday}: ${day.focusMinutes}")
                        Text("${strings.focusSessionsToday}: ${day.focusSessions}")
                        Text("${strings.completedTasks}: ${day.completedTasks}")
                        Text("${strings.remindersToday}: ${day.reminders}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalendar = false }) {
                    Text(strings.done)
                }
            },
        )
    }
}

@Composable
private fun StatsLegend(
    visibleSeries: Set<String>,
    onToggle: (String) -> Unit,
) {
    val series = listOf(
        "focus" to Pair("Focus", MaterialTheme.colorScheme.primary),
        "tasks" to Pair("Tasks", MaterialTheme.colorScheme.secondary),
        "reminders" to Pair("Reminders", MaterialTheme.colorScheme.tertiary),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        series.forEach { (key, labelAndColor) ->
            val (label, color) = labelAndColor
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (visibleSeries.contains(key)) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (visibleSeries.contains(key)) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(1.dp, if (visibleSeries.contains(key)) color else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .clickable { onToggle(key) }
                    .semantics {
                        contentDescription = "$label chart series"
                        role = Role.Checkbox
                    },
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).then(Modifier)) {
                        Surface(color = color, shape = CircleShape, modifier = Modifier.fillMaxSize()) {}
                    }
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ActivityLineChart(
    days: List<com.kaemis.healthdesk.domain.stats.StatsDaySnapshot>,
    visibleSeries: Set<String>,
) {
    if (days.isEmpty()) return
    val maxValue = days.maxOf { maxOf(it.focusMinutes.toFloat(), it.completedTasks.toFloat(), it.reminders.toFloat()) }.coerceAtLeast(1f)
    val focusColor = MaterialTheme.colorScheme.primary
    val taskColor = MaterialTheme.colorScheme.secondary
    val reminderColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        val stepX = if (days.size > 1) size.width / (days.size - 1) else size.width
        fun point(index: Int, value: Float) = Offset(
            x = index * stepX,
            y = size.height - (value / maxValue) * size.height,
        )

        repeat(5) { index ->
            val y = size.height / 4f * index
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val series = listOf(
            "focus" to (days.mapIndexed { index, day -> point(index, day.focusMinutes.toFloat()) } to focusColor),
            "tasks" to (days.mapIndexed { index, day -> point(index, day.completedTasks.toFloat()) } to taskColor),
            "reminders" to (days.mapIndexed { index, day -> point(index, day.reminders.toFloat()) } to reminderColor),
        )
        series.filter { it.first in visibleSeries }.forEach { (_, pointsAndColor) ->
            val (points, color) = pointsAndColor
            for (index in 0 until points.lastIndex) {
                drawLine(color, points[index], points[index + 1], strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            }
            points.forEach { drawCircle(color, radius = 3.dp.toPx(), center = it) }
        }
    }
    Text("0 - ${maxValue.roundToInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            Text(
                LocalDate.parse(day.date).let { if (days.size > 7) it.dayOfMonth.toString() else it.dayOfWeek.name.take(3) },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsCalendar(
    days: List<com.kaemis.healthdesk.domain.stats.StatsDaySnapshot>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
) {
    if (days.isEmpty()) return
    val parsedDays = days.map { LocalDate.parse(it.date) }
    val firstDay = parsedDays.first()
    val leadingEmpty = (firstDay.dayOfWeek.value - 1).coerceAtLeast(0)
    val cells = List(leadingEmpty) { null } + days
    cells.chunked(7).forEach { week ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            week.forEach { day ->
                if (day == null) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    val isSelected = day.date == selectedDate
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectDate(day.date) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(LocalDate.parse(day.date).dayOfMonth.toString(), fontWeight = FontWeight.SemiBold)
                            Text(
                                day.focusMinutes.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            repeat(7 - week.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChartRow(
    label: String,
    value: Long,
    suffix: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(24.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(10.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(value.coerceIn(0, 60).toFloat() / 60f)
                        .height(10.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {}
            }
        }
        Text(
            text = if (suffix.isBlank()) value.toString() else "$value $suffix",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )
    }
}

private fun bar(value: Long): String = "#".repeat(value.coerceIn(0, 20).toInt())

@Composable
private fun ProfileScreen(
    profile: UserProfileEntity,
    profileViewModel: ProfileViewModel,
    strings: AppStrings,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    val pickAvatar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                copyAvatarToPrivateStorage(context, uri)?.let(profileViewModel::updateAvatarPath)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(profile = profile, strings = strings, size = 96.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = strings.localProfileOnly,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    SectionCard(title = strings.profile) {
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(strings.displayName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { profileViewModel.updateDisplayName(displayName) }) {
                Text(strings.saveName)
            }
            OutlinedButton(
                onClick = {
                    pickAvatar.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Text(strings.chooseAvatar)
            }
        }
        OutlinedButton(onClick = profileViewModel::useInitialsAvatar) {
            Text(strings.useInitialsAvatar)
        }
    }

}

@Composable
private fun ProfileEditDialog(
    profile: UserProfileEntity,
    profileViewModel: ProfileViewModel,
    strings: AppStrings,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    val pickAvatar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                copyAvatarToPrivateStorage(context, uri)?.let(profileViewModel::updateAvatarPath)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.profile) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(profile = profile, strings = strings, size = 72.dp)
                    OutlinedButton(
                        onClick = {
                            pickAvatar.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    ) {
                        Text(strings.chooseAvatar)
                    }
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(strings.displayName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = profileViewModel::useInitialsAvatar) {
                    Text(strings.useInitialsAvatar)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    profileViewModel.updateDisplayName(displayName)
                    onDismiss()
                },
            ) {
                Text(strings.done)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}

@Composable
private fun SettingsScreen(
    profile: UserProfileEntity,
    profileViewModel: ProfileViewModel,
    settings: SettingsSnapshot,
    workingHourRules: List<WorkingHourRuleEntity>,
    settingsViewModel: SettingsViewModel,
    backupService: BackupService,
    importNativeBackup: suspend (NativeBackupPayload) -> BackupImportSummary,
    strings: AppStrings,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResetConfirmation by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<NativeBackupPayload?>(null) }
    var showBackupError by remember { mutableStateOf(false) }
    val createBackupDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val backupJson = backupService.exportNativeBackup(
                    appVersionName = "1.0.0",
                    appVersionCode = 1,
                )
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(backupJson.toByteArray())
                }
            }
        }
    }
    val openBackupDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                when (val result = jsonText?.let(backupService::parse)) {
                    is BackupParseResult.Valid -> pendingImport = result.payload
                    else -> showBackupError = true
                }
            }
        }
    }

    val startRule = workingHourRules.firstOrNull { it.isEnabled } ?: workingHourRules.firstOrNull()
    val endRule = workingHourRules.firstOrNull { it.isEnabled } ?: workingHourRules.lastOrNull()

    SectionCard(title = strings.workingHours) {
        SwitchRow(strings.useWorkingHours, settings.workingHoursEnabled, settingsViewModel::updateWorkingHoursEnabled)
        Text(
            text = if (settings.workingHoursEnabled) strings.workingHoursEnabled else strings.workingHoursOff,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TimePickerFieldRow(
            label = strings.startTime,
            value = startRule?.startLocalTime ?: "08:00",
            enabled = settings.workingHoursEnabled,
            strings = strings,
            onValueChange = settingsViewModel::updateWorkingHoursStartTime,
        )
        TimePickerFieldRow(
            label = strings.endTime,
            value = endRule?.endLocalTime ?: "18:00",
            enabled = settings.workingHoursEnabled,
            strings = strings,
            onValueChange = settingsViewModel::updateWorkingHoursEndTime,
        )
    }

    SectionCard(title = strings.notifications) {
        SwitchRow(strings.appNotifications, settings.notificationsEnabled, settingsViewModel::updateNotificationsEnabled)
    }

    SectionCard(title = strings.soundAndVibration) {
        SwitchRow(strings.haptics, settings.hapticsEnabled, settingsViewModel::updateHapticsEnabled)
        SoundDropdownRow(
            label = strings.alarmSound,
            selected = settings.alarmSoundKey,
            options = soundOptions("tone", strings),
            onPreview = { previewSound(context, it) },
            onSelected = settingsViewModel::updateAlarmSoundKey,
        )
        SoundDropdownRow(
            label = strings.reminderSound,
            selected = settings.reminderSoundKey,
            options = soundOptions("ring", strings),
            onPreview = { previewSound(context, it) },
            onSelected = settingsViewModel::updateReminderSoundKey,
        )
        SoundDropdownRow(
            label = strings.taskSound,
            selected = settings.taskSoundKey,
            options = soundOptions("ring", strings),
            onPreview = { previewSound(context, it) },
            onSelected = settingsViewModel::updateTaskSoundKey,
        )
    }

    SectionCard(title = strings.appearance) {
        ChoiceRow(
            label = strings.theme,
            selected = settings.themeMode,
            options = listOf("system" to strings.system, "light" to strings.light, "dark" to strings.dark),
            onSelected = settingsViewModel::updateThemeMode,
        )
        AccentSelector(
            selected = settings.accentKey,
            strings = strings,
            onSelected = settingsViewModel::updateAccentKey,
        )
    }

    SectionCard(title = strings.language) {
        ChoiceRow(
            label = strings.appLanguage,
            selected = settings.languageCode,
            options = listOf("en" to strings.english, "es" to strings.spanish),
            onSelected = settingsViewModel::updateLanguageCode,
        )
    }

    SectionCard(title = strings.localData) {
        Text(strings.localDataPending)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { createBackupDocument.launch("healthdesk-backup.json") }) {
                Text(strings.exportJson)
            }
        }
        OutlinedButton(onClick = { openBackupDocument.launch(arrayOf("application/json", "text/*", "*/*")) }) {
            Text(strings.importJson)
        }
        OutlinedButton(onClick = { showResetConfirmation = true }) {
            Text(strings.resetLocalData)
        }
    }

    SectionCard(title = strings.aboutAndSupport) {
        Text(strings.versionText)
        Text(strings.licenseText)
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(strings.resetLocalDataTitle) },
            text = { Text(strings.resetLocalDataMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        settingsViewModel.resetLocalData()
                    },
                ) {
                    Text(strings.reset)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    pendingImport?.let { payload ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(strings.importBackupTitle) },
            text = { Text(strings.importBackupMessage(payload.tasks.size, payload.reminders.size, payload.categories.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            importNativeBackup(payload)
                            pendingImport = null
                        }
                    },
                ) {
                    Text(strings.importBackup)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    if (showBackupError) {
        AlertDialog(
            onDismissRequest = { showBackupError = false },
            title = { Text(strings.importBackupTitle) },
            text = { Text(strings.backupError) },
            confirmButton = {
                TextButton(onClick = { showBackupError = false }) {
                    Text(strings.done)
                }
            },
        )
    }
}

@Composable
private fun PlaceholderScreen(
    destination: AppDestination,
    strings: AppStrings,
) {
    Text(
        text = destination.label(strings),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    SectionCard(title = placeholderTitle(destination, strings)) {
        Text(
            text = placeholderBody(destination, strings),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, text) ->
                FilterChip(
                    selected = selected == key,
                    onClick = { onSelected(key) },
                    label = { Text(text) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundDropdownRow(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onPreview: (String) -> Unit = {},
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (key, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onPreview(key)
                            onSelected(key)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSelector(
    selected: String,
    strings: AppStrings,
    onSelected: (String) -> Unit,
) {
    var hue by remember(selected) { mutableStateOf(hueFromAccent(selected)) }
    val previewColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.62f)))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.accent, style = MaterialTheme.typography.labelLarge)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.Transparent,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                listOf(0f, 35f, 70f, 140f, 205f, 270f, 320f).forEach { swatchHue ->
                    val swatch = Color(android.graphics.Color.HSVToColor(floatArrayOf(swatchHue, 0.55f, 0.72f)))
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        color = swatch,
                        content = {},
                    )
                }
            }
        }
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                onSelected(hexFromHue(it))
            },
            valueRange = 0f..360f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = strings.accent
                        role = Role.Button
                    },
                    shape = CircleShape,
                    color = previewColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    content = {},
                )
            Text(hexFromHue(hue), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    minimum: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onValueChange((value - 1).coerceAtLeast(minimum)) },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp),
            ) {
                Text("-")
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(36.dp),
            )
            OutlinedButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp),
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerFieldRow(
    label: String,
    value: String,
    enabled: Boolean,
    strings: AppStrings,
    onValueChange: (String) -> Unit,
) {
    var showPicker by remember(value) { mutableStateOf(false) }
    val initial = remember(value) { runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.of(8, 0)) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            enabled = enabled,
            onClick = { showPicker = true },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null)
                Text(value)
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(LocalTime.of(pickerState.hour, pickerState.minute).format(DateTimeFormatter.ofPattern("HH:mm")))
                    showPicker = false
                }) {
                    Text(strings.done)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

@Composable
private fun SegmentedActionButton(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 40.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(text, maxLines = 1, overflow = TextOverflow.Clip, fontSize = 12.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 40.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(text, maxLines = 1, overflow = TextOverflow.Clip, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NumberFieldRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = textValue,
            onValueChange = { next ->
                if (next.all { it.isDigit() } || next.isBlank()) {
                    textValue = next
                    next.toIntOrNull()?.coerceIn(min, max)?.let(onValueChange)
                }
            },
            singleLine = true,
            modifier = Modifier.width(96.dp),
        )
    }
}

@Composable
private fun ProfileAvatar(
    profile: UserProfileEntity,
    strings: AppStrings,
    size: Dp,
) {
    val image = remember(profile.avatarLocalPath, profile.avatarMode, profile.updatedAt) {
        if (profile.avatarMode == "localImage" && profile.avatarLocalPath != null) {
            BitmapFactory.decodeFile(profile.avatarLocalPath)?.asImageBitmap()
        } else {
            null
        }
    }

    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = strings.profileAvatarDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Surface(
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = initialsFor(profile.displayName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun progressFor(state: FocusUiState): Float {
    if (state.totalSeconds <= 0) return 0f
    val elapsed = (state.totalSeconds - state.remainingSeconds).coerceAtLeast(0)
    return (elapsed.toFloat() / state.totalSeconds.toFloat()).coerceIn(0f, 1f)
}

private fun formatDuration(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutesPart = safeSeconds / 60
    val secondsPart = safeSeconds % 60
    return "%02d:%02d".format(minutesPart, secondsPart)
}

private fun focusStatusLabel(phase: FocusPhase, strings: AppStrings): String = when (phase) {
    FocusPhase.Idle -> strings.focusIdle
    FocusPhase.FocusRunning -> strings.focusActive
    FocusPhase.FocusPaused -> strings.focusPaused
    FocusPhase.FocusAlarm -> strings.focusAlarm
    FocusPhase.RestRunning -> strings.restActive
    FocusPhase.RestPaused -> strings.restPaused
    FocusPhase.RestAlarm -> strings.restAlarm
    FocusPhase.Snoozed -> strings.snoozed
    FocusPhase.Completed -> strings.focusIdle
    FocusPhase.Stopped -> strings.stopped
}

private fun focusModeTitle(mode: ResolvedFocusMode, strings: AppStrings): String = when {
    mode.isCustom -> mode.name
    mode.type == POMODORO_MODE_ID -> strings.pomodoro
    mode.type == MULTI_CYCLE_MODE_ID -> strings.multiCycle
    else -> strings.normal
}

private fun focusModeSummary(mode: ResolvedFocusMode, strings: AppStrings, languageCode: String): String = when {
    mode.isCustom && mode.type == POMODORO_MODE_ID -> if (languageCode == "es") "Modo personalizado con ciclos y pausas largas" else "Custom mode with cycles and long breaks"
    mode.isCustom && mode.type == MULTI_CYCLE_MODE_ID -> if (languageCode == "es") "Modo personalizado por rondas de trabajo y descanso" else "Custom rounds of work and rest"
    mode.isCustom -> if (languageCode == "es") "Modo personalizado de enfoque" else "Custom focus method"
    mode.type == POMODORO_MODE_ID -> if (languageCode == "es") "Ciclos de enfoque con pausas cortas y largas" else "Focus cycles with short and long breaks"
    mode.type == MULTI_CYCLE_MODE_ID -> if (languageCode == "es") "Rondas repetidas de trabajo y descanso" else "Repeated work and rest rounds"
    else -> if (languageCode == "es") "Una sesion de trabajo con descanso opcional" else "One work session with an optional break"
}

private fun focusModeDescription(mode: ResolvedFocusMode, strings: AppStrings, languageCode: String): String = when {
    mode.type == POMODORO_MODE_ID -> if (languageCode == "es") {
        "Pomodoro encadena varios bloques de enfoque, usando pausas cortas entre ciclos y una pausa larga al final."
    } else {
        "Pomodoro chains several focus blocks, using short breaks between cycles and a long break at the end."
    }
    mode.type == MULTI_CYCLE_MODE_ID -> if (languageCode == "es") {
        "Multiciclo repite una misma combinacion de trabajo y descanso durante varias rondas."
    } else {
        "Multi-cycle repeats the same work and rest combination across several rounds."
    }
    else -> if (languageCode == "es") {
        "Normal usa un solo bloque de trabajo y, si se configura, un descanso posterior."
    } else {
        "Normal uses one work block and, if configured, a follow-up break."
    }
}

private fun focusCaption(phase: FocusPhase, alarmActive: Boolean, languageCode: String): String = when {
    alarmActive -> if (languageCode == "es") "La sesion ha terminado. Toca el reloj o usa los botones." else "The session has finished. Tap the clock or use the buttons."
    phase == FocusPhase.FocusRunning -> ""
    phase == FocusPhase.FocusPaused -> ""
    phase == FocusPhase.RestRunning -> ""
    phase == FocusPhase.RestPaused -> ""
    phase == FocusPhase.RestAlarm -> ""
    phase == FocusPhase.Snoozed -> if (languageCode == "es") "Aplazamiento activo" else "Snooze in progress"
    else -> if (languageCode == "es") "Empieza cuando quieras tu siguiente bloque" else "Start when you are ready for the next block"
}

private fun shouldShowStatusPill(phase: FocusPhase): Boolean = when (phase) {
    FocusPhase.FocusRunning,
    FocusPhase.FocusPaused,
    FocusPhase.RestRunning,
    FocusPhase.RestPaused,
    FocusPhase.RestAlarm,
    -> false
    FocusPhase.Idle,
    FocusPhase.Completed,
    FocusPhase.Stopped,
    -> false
    else -> true
}

private fun showPauseResumeButton(phase: FocusPhase): Boolean = when (phase) {
    FocusPhase.FocusRunning,
    FocusPhase.FocusPaused,
    FocusPhase.RestRunning,
    FocusPhase.RestPaused,
    FocusPhase.Snoozed,
    -> true
    else -> false
}

private fun cyclePreviewText(mode: ResolvedFocusMode, strings: AppStrings): String {
    val steps = mutableListOf<String>()
    val totalCycles = mode.totalCycles()
    repeat(totalCycles) { index ->
        val cycle = index + 1
        steps += "${mode.workMinutes}m ${strings.focus}"
        val restMinutes = mode.restMinutesForCycle(cycle)
        if (restMinutes > 0) {
            steps += "${restMinutes}m ${strings.restMinutes}"
        }
    }
    return steps.joinToString(" -> ")
}

private fun adjustTime(time: String, deltaMinutes: Int): String {
    val parsed = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(8, 0))
    return parsed.plusMinutes(deltaMinutes.toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun localeForLanguage(languageCode: String): Locale = if (languageCode == "es") Locale.forLanguageTag("es") else Locale.ENGLISH

private fun workingHoursStatusText(languageCode: String, inside: Boolean): String = if (inside) {
    if (languageCode == "es") "Dentro de horario laboral" else "Inside working hours"
} else {
    if (languageCode == "es") "Fuera del horario laboral" else "Outside working hours"
}

private fun recurrenceUnitLabel(unit: String, languageCode: String): String = when (unit) {
    "daily" -> if (languageCode == "es") "Diario" else "Daily"
    "weekly" -> if (languageCode == "es") "Semanal" else "Weekly"
    "monthly" -> if (languageCode == "es") "Mensual" else "Monthly"
    "monthlyDay" -> if (languageCode == "es") "Mensual por dia" else "Monthly day"
    "monthlyWeekday" -> if (languageCode == "es") "Mensual por semana" else "Monthly weekday"
    "yearly" -> if (languageCode == "es") "Anual" else "Yearly"
    else -> unit
}

private fun openExternalLink(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private object SoundPreviewPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(context: android.content.Context, soundKey: String) {
        stop()
        val resource = when (soundKey) {
            "tone2" -> R.raw.tone2
            "tone3" -> R.raw.tone3
            "tone4" -> R.raw.tone4
            "ring2" -> R.raw.ring2
            "ring3" -> R.raw.ring3
            "ring4" -> R.raw.ring4
            "silent" -> null
            "tone1" -> R.raw.tone1
            else -> R.raw.ring1
        } ?: return
        runCatching {
            mediaPlayer = MediaPlayer.create(context, resource)?.apply {
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer === it) mediaPlayer = null
                }
                start()
            }
        }
    }

    private fun stop() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }
}

private fun previewSound(context: android.content.Context, soundKey: String) {
    SoundPreviewPlayer.play(context, soundKey)
}

private fun focusModeLabel(mode: String, strings: AppStrings): String = when (mode) {
    "pomodoro" -> strings.pomodoro
    "multiCycle" -> strings.multiCycle
    else -> strings.normal
}

private fun reminderScheduleLabel(reminder: ReminderEntity): String = when (reminder.scheduleMode) {
    "interval" -> "${reminder.intervalMinutes ?: 0} min"
    "fixedTimeOnce" -> reminder.fixedLocalTime ?: reminder.scheduleMode
    "recurring" -> when (reminder.recurrenceUnit) {
        "weekly" -> "Weekly"
        "monthly" -> "Monthly"
        "daily" -> "Daily"
        else -> reminder.recurrenceUnit ?: "Recurring"
    }
    else -> reminder.scheduleMode
}

private fun formatReminderTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))

private fun formatCompletedAt(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))

private fun soundOptions(prefix: String, strings: AppStrings): List<Pair<String, String>> = listOf(
    "${prefix}1" to "${prefix}1",
    "${prefix}2" to "${prefix}2",
    "${prefix}3" to "${prefix}3",
    "${prefix}4" to "${prefix}4",
    "silent" to strings.silent,
)

private fun hueFromAccent(accent: String): Float {
    val preset = when (accent) {
        "mint" -> 150f
        "amber" -> 42f
        "clay" -> 18f
        "sky" -> 205f
        "lavender" -> 250f
        else -> null
    }
    if (preset != null) return preset
    if (!accent.startsWith("#")) return 130f
    return runCatching {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(accent), hsv)
        hsv[0]
    }.getOrDefault(130f)
}

private fun hexFromHue(hue: Float): String {
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.62f))
    return "#%06X".format(0xFFFFFF and color)
}

private fun initialsFor(name: String): String {
    val initials = name
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
    return initials.ifBlank { "U" }
}

private suspend fun copyAvatarToPrivateStorage(
    context: android.content.Context,
    uri: Uri,
): String? = withContext(Dispatchers.IO) {
    val avatarDirectory = File(context.filesDir, "avatars")
    if (!avatarDirectory.exists()) avatarDirectory.mkdirs()
    val avatarFile = File(avatarDirectory, "profile-avatar")

    context.contentResolver.openInputStream(uri)?.use { input ->
        avatarFile.outputStream().use { output -> input.copyTo(output) }
        avatarFile.absolutePath
    }
}

private fun AppDestination.label(strings: AppStrings): String = when (this) {
    AppDestination.Dashboard -> strings.dashboard
    AppDestination.Reminders -> strings.reminders
    AppDestination.Tasks -> strings.tasks
    AppDestination.Stats -> strings.stats
    AppDestination.Settings -> strings.settings
}

private fun placeholderTitle(destination: AppDestination, strings: AppStrings): String =
    when (destination) {
        AppDestination.Dashboard -> strings.placeholderDailyOverview
        AppDestination.Reminders -> strings.placeholderRemindersTitle
        AppDestination.Tasks -> strings.placeholderTasksTitle
        AppDestination.Stats -> strings.placeholderStatsTitle
        AppDestination.Settings -> strings.placeholderSettingsTitle
    }

private fun placeholderBody(destination: AppDestination, strings: AppStrings): String =
    when (destination) {
        AppDestination.Dashboard -> strings.placeholderDashboardBody
        AppDestination.Reminders -> strings.placeholderRemindersBody
        AppDestination.Tasks -> strings.placeholderTasksBody
        AppDestination.Stats -> strings.placeholderStatsBody
        AppDestination.Settings -> strings.placeholderSettingsBody
    }
