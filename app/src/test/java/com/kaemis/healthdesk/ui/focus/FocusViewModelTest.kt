package com.kaemis.healthdesk.ui.focus

import com.kaemis.healthdesk.data.dao.FocusSessionDao
import com.kaemis.healthdesk.data.dao.WorkingHourRuleDao
import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.defaults.DefaultData
import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import com.kaemis.healthdesk.data.repository.FocusSessionRepository
import com.kaemis.healthdesk.data.repository.WorkingHoursRepository
import com.kaemis.healthdesk.domain.focus.CustomFocusMode
import com.kaemis.healthdesk.platform.alarm.FocusAlarmScheduler
import com.kaemis.healthdesk.platform.audio.AlarmFeedbackController
import com.kaemis.healthdesk.platform.service.FocusActionCommand
import com.kaemis.healthdesk.platform.service.FocusServiceController
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startAndStopRecordsStoppedSession() = runFocusTest(dispatcher) {
        val viewModel = focusViewModel()
        runCurrent()

        viewModel.start()
        runCurrent()

        assertEquals(FocusPhase.FocusRunning, viewModel.state.value.phase)
        assertEquals("active", focusDao.latest()?.status)

        now += 10_000L
        viewModel.stop()
        runCurrent()

        val session = focusDao.latest()
        assertEquals(FocusPhase.Stopped, viewModel.state.value.phase)
        assertEquals("stopped", session?.status)
        assertEquals("userStopped", session?.endReason)
        assertEquals(10L, session?.actualFocusSeconds)
    }

    @Test
    fun normalTimerWithRestCompletesAfterFocusAndRestAlarms() = runFocusTest(dispatcher) {
        val viewModel = focusViewModel(settings = SettingsSnapshot(workSessionMinutes = 1, restMinutes = 1))
        runCurrent()

        viewModel.start()
        runCurrent()
        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(FocusPhase.FocusAlarm, viewModel.state.value.phase)
        assertEquals(60L, focusDao.latest()?.actualFocusSeconds)

        viewModel.stopAlarm()
        runCurrent()
        assertEquals(FocusPhase.RestRunning, viewModel.state.value.phase)

        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(FocusPhase.RestAlarm, viewModel.state.value.phase)

        viewModel.stopAlarm()
        runCurrent()

        val session = focusDao.latest()
        assertEquals(FocusPhase.Completed, viewModel.state.value.phase)
        assertEquals("completed", session?.status)
        assertEquals("restElapsed", session?.endReason)
        assertEquals(60L, session?.actualRestSeconds)
    }

    @Test
    fun restZeroCompletesFromFocusAlarm() = runFocusTest(dispatcher) {
        val viewModel = focusViewModel(settings = SettingsSnapshot(workSessionMinutes = 1, restMinutes = 0))
        runCurrent()

        viewModel.start()
        runCurrent()
        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.stopAlarm()
        runCurrent()

        val session = focusDao.latest()
        assertEquals(FocusPhase.Completed, viewModel.state.value.phase)
        assertEquals("completed", session?.status)
        assertEquals("timerElapsed", session?.endReason)
        assertEquals(0L, session?.actualRestSeconds)
    }

    @Test
    fun snoozeAddsFocusSecondsAndCount() = runFocusTest(dispatcher) {
        val viewModel = focusViewModel(
            settings = SettingsSnapshot(workSessionMinutes = 1, restMinutes = 0, snoozeMinutes = 10),
        )
        runCurrent()

        viewModel.start()
        runCurrent()
        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.snooze()
        runCurrent()

        assertEquals(FocusPhase.Snoozed, viewModel.state.value.phase)
        assertEquals(1, focusDao.latest()?.snoozeCount)

        now += 600_000L
        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(FocusPhase.FocusAlarm, viewModel.state.value.phase)
        assertEquals(660L, focusDao.latest()?.actualFocusSeconds)
    }

    @Test
    fun startOutsideWorkingHoursRequiresConfirmation() = runFocusTest(dispatcher) {
        now = epochMillis(LocalDate.of(2026, 7, 13), LocalTime.of(7, 0))
        workingHoursDao.replace(DefaultData.workingHourRules(now))
        val viewModel = focusViewModel(settings = SettingsSnapshot(workingHoursEnabled = true))
        runCurrent()

        viewModel.start()
        runCurrent()

        assertTrue(viewModel.state.value.pendingOutsideWorkingHoursConfirmation)
        assertFalse(focusDao.hasSessions())

        viewModel.startConfirmed()
        runCurrent()

        assertEquals(FocusPhase.FocusRunning, viewModel.state.value.phase)
        assertTrue(focusDao.hasSessions())

        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(FocusPhase.FocusRunning, viewModel.state.value.phase)
    }

    @Test
    fun pomodoroAdvancesConfiguredCyclesAfterAlarms() = runFocusTest(dispatcher) {
        val viewModel = focusViewModel(
            settings = SettingsSnapshot(
                timerMode = "pomodoro",
                workSessionMinutes = 1,
                pomodoroCycles = 2,
                pomodoroShortRestMinutes = 0,
                pomodoroLongRestMinutes = 0,
                workingHoursEnabled = false,
            ),
        )
        runCurrent()

        viewModel.start()
        runCurrent()
        assertEquals(2, viewModel.state.value.totalCycles)

        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.stopAlarm()
        runCurrent()

        assertEquals(FocusPhase.FocusRunning, viewModel.state.value.phase)
        assertEquals(2, viewModel.state.value.currentCycle)

        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.stopAlarm()
        runCurrent()

        assertEquals(FocusPhase.Completed, viewModel.state.value.phase)
        assertEquals(120L, focusDao.latest()?.actualFocusSeconds)
    }

    @Test
    fun multiCycleAdvancesConfiguredRounds() = runFocusTest(dispatcher) {
        val viewModel = focusViewModel(
            settings = SettingsSnapshot(
                timerMode = "multiCycle",
                workSessionMinutes = 1,
                restMinutes = 0,
                multiCycleCycles = 2,
                workingHoursEnabled = false,
            ),
        )
        runCurrent()

        viewModel.start()
        runCurrent()
        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.stopAlarm()
        runCurrent()

        assertEquals(FocusPhase.FocusRunning, viewModel.state.value.phase)
        assertEquals(2, viewModel.state.value.currentCycle)

        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.stopAlarm()
        runCurrent()

        assertEquals(FocusPhase.Completed, viewModel.state.value.phase)
        assertEquals(120L, focusDao.latest()?.actualFocusSeconds)
    }

    @Test
    fun selectedCustomPomodoroUsesCustomShortAndLongRests() = runFocusTest(dispatcher) {
        val viewModel = focusViewModel(
            settings = SettingsSnapshot(
                timerMode = "custom-deep-work",
                customFocusModes = listOf(
                    CustomFocusMode(
                        id = "custom-deep-work",
                        name = "Deep work",
                        type = "pomodoro",
                        workMinutes = 1,
                        restMinutes = 0,
                        snoozeMinutes = 5,
                        pomodoroCycles = 2,
                        pomodoroShortRestMinutes = 1,
                        pomodoroLongRestMinutes = 2,
                        multiCycleCycles = 2,
                    ),
                ),
                workingHoursEnabled = false,
            ),
        )
        runCurrent()

        viewModel.start()
        runCurrent()
        assertEquals(2, viewModel.state.value.totalCycles)
        assertEquals(1, viewModel.state.value.workSessionMinutes)

        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.stopAlarm()
        runCurrent()

        assertEquals(FocusPhase.RestRunning, viewModel.state.value.phase)
        assertEquals(60L, viewModel.state.value.remainingSeconds)

        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.stopAlarm()
        runCurrent()

        assertEquals(FocusPhase.FocusRunning, viewModel.state.value.phase)
        assertEquals(2, viewModel.state.value.currentCycle)

        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()
        viewModel.stopAlarm()
        runCurrent()

        assertEquals(FocusPhase.RestRunning, viewModel.state.value.phase)
        assertEquals(120L, viewModel.state.value.remainingSeconds)
    }

    @Test
    fun notificationPauseResumeAndStopSynchronizeStateSessionAndPlatformControllers() = runFocusTest(dispatcher) {
        val commands = MutableSharedFlow<FocusActionCommand>(extraBufferCapacity = 16)
        val serviceController = RecordingFocusServiceController()
        val alarmScheduler = RecordingFocusAlarmScheduler()
        val alarmFeedback = RecordingAlarmFeedbackController()
        val viewModel = focusViewModel(
            commandsFlow = commands,
            focusServiceController = serviceController,
            focusAlarmScheduler = alarmScheduler,
            alarmFeedbackController = alarmFeedback,
        )
        runCurrent()

        viewModel.start()
        runCurrent()
        assertEquals(FocusPhase.FocusRunning, serviceController.lastState?.phase)
        assertEquals(1, alarmScheduler.phaseSchedules)

        commands.tryEmit(FocusActionCommand.Pause)
        runCurrent()
        assertEquals(FocusPhase.FocusPaused, viewModel.state.value.phase)
        assertEquals(FocusPhase.FocusPaused, serviceController.lastState?.phase)
        assertEquals(1, alarmScheduler.phaseCancels)

        commands.tryEmit(FocusActionCommand.Resume)
        runCurrent()
        assertEquals(FocusPhase.FocusRunning, viewModel.state.value.phase)
        assertEquals(2, alarmScheduler.phaseSchedules)

        now += 10_000L
        commands.tryEmit(FocusActionCommand.Stop)
        runCurrent()

        val session = focusDao.latest()
        assertEquals(FocusPhase.Stopped, viewModel.state.value.phase)
        assertEquals("stopped", session?.status)
        assertEquals("userStopped", session?.endReason)
        assertEquals(FocusPhase.Stopped, serviceController.lastState?.phase)
        assertTrue(alarmFeedback.stopCount > 0)
    }

    @Test
    fun notificationStopAlarmStopsFeedbackAndStartsRest() = runFocusTest(dispatcher) {
        val commands = MutableSharedFlow<FocusActionCommand>(extraBufferCapacity = 16)
        val serviceController = RecordingFocusServiceController()
        val alarmFeedback = RecordingAlarmFeedbackController()
        val viewModel = focusViewModel(
            settings = SettingsSnapshot(workSessionMinutes = 1, restMinutes = 1, workingHoursEnabled = false),
            commandsFlow = commands,
            focusServiceController = serviceController,
            alarmFeedbackController = alarmFeedback,
        )
        runCurrent()

        viewModel.start()
        runCurrent()
        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(FocusPhase.FocusAlarm, viewModel.state.value.phase)
        assertEquals(1, alarmFeedback.startCount)

        commands.tryEmit(FocusActionCommand.StopAlarm)
        runCurrent()

        assertEquals(FocusPhase.RestRunning, viewModel.state.value.phase)
        assertEquals(FocusPhase.RestRunning, serviceController.lastState?.phase)
        assertTrue(alarmFeedback.stopCount > 0)
    }

    @Test
    fun notificationSnoozeStopsFeedbackUpdatesSessionAndUiState() = runFocusTest(dispatcher) {
        val commands = MutableSharedFlow<FocusActionCommand>(extraBufferCapacity = 16)
        val alarmFeedback = RecordingAlarmFeedbackController()
        val viewModel = focusViewModel(
            settings = SettingsSnapshot(workSessionMinutes = 1, restMinutes = 0, snoozeMinutes = 5, workingHoursEnabled = false),
            commandsFlow = commands,
            alarmFeedbackController = alarmFeedback,
        )
        runCurrent()

        viewModel.start()
        runCurrent()
        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()

        commands.tryEmit(FocusActionCommand.Snooze)
        runCurrent()

        assertEquals(FocusPhase.Snoozed, viewModel.state.value.phase)
        assertEquals(1, focusDao.latest()?.snoozeCount)
        assertEquals(5 * 60L, viewModel.state.value.remainingSeconds)
        assertTrue(alarmFeedback.stopCount > 0)
    }

    @Test
    fun alarmFeedbackUsesConfiguredSoundAndHaptics() = runFocusTest(dispatcher) {
        val alarmFeedback = RecordingAlarmFeedbackController()
        val viewModel = focusViewModel(
            settings = SettingsSnapshot(
                workSessionMinutes = 1,
                restMinutes = 0,
                alarmSoundKey = "tone4",
                hapticsEnabled = false,
                workingHoursEnabled = false,
            ),
            alarmFeedbackController = alarmFeedback,
        )
        runCurrent()

        viewModel.start()
        runCurrent()
        now += 60_000L
        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(FocusPhase.FocusAlarm, viewModel.state.value.phase)
        assertEquals("tone4", alarmFeedback.lastSoundKey)
        assertEquals(false, alarmFeedback.lastHapticsEnabled)
    }

    private fun FocusTestScope.focusViewModel(
        settings: SettingsSnapshot = SettingsSnapshot(workingHoursEnabled = false),
        commandsFlow: Flow<FocusActionCommand> = MutableSharedFlow<FocusActionCommand>(),
        focusServiceController: FocusServiceController = FocusServiceController.NoOp,
        focusAlarmScheduler: FocusAlarmScheduler = FocusAlarmScheduler.NoOp,
        alarmFeedbackController: AlarmFeedbackController = AlarmFeedbackController.NoOp,
    ): FocusViewModel = track(
        FocusViewModel(
            settingsFlow = MutableStateFlow(settings),
            focusSessionRepository = FocusSessionRepository(focusDao),
            workingHoursRepository = WorkingHoursRepository(workingHoursDao),
            nowProvider = { now },
            commandsFlow = commandsFlow,
            focusServiceController = focusServiceController,
            focusAlarmScheduler = focusAlarmScheduler,
            alarmFeedbackController = alarmFeedbackController,
        ),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runFocusTest(
    dispatcher: TestDispatcher,
    block: suspend FocusTestScope.() -> Unit,
) = runTest(dispatcher) {
    val scope = FocusTestScope(this)
    try {
        scope.block()
    } finally {
        scope.cancelViewModels()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class FocusTestScope(
    private val testScope: TestScope,
) {
    var now: Long = epochMillis(LocalDate.of(2026, 7, 13), LocalTime.of(9, 0))
    val focusDao = FakeFocusSessionDao()
    val workingHoursDao = FakeWorkingHourRuleDao()
    val viewModels = mutableListOf<FocusViewModel>()

    fun runCurrent() {
        testScope.runCurrent()
    }

    fun advanceTimeBy(delayTimeMillis: Long) {
        testScope.advanceTimeBy(delayTimeMillis)
    }

    fun track(viewModel: FocusViewModel): FocusViewModel {
        viewModels += viewModel
        return viewModel
    }

    fun cancelViewModels() {
        viewModels.forEach { it.cancelJobsForTest() }
    }
}

private class RecordingFocusServiceController : FocusServiceController {
    var lastState: FocusUiState? = null

    override fun update(state: FocusUiState) {
        lastState = state
    }
}

private class RecordingFocusAlarmScheduler : FocusAlarmScheduler {
    var phaseSchedules = 0
    var phaseCancels = 0
    var workdaySchedules = 0
    var workdayCancels = 0

    override fun schedulePhaseEnd(triggerAtMillis: Long) {
        phaseSchedules += 1
    }

    override fun cancelPhaseEnd() {
        phaseCancels += 1
    }

    override fun scheduleWorkdayEnd(triggerAtMillis: Long) {
        workdaySchedules += 1
    }

    override fun cancelWorkdayEnd() {
        workdayCancels += 1
    }
}

private class RecordingAlarmFeedbackController : AlarmFeedbackController {
    var startCount = 0
    var stopCount = 0
    var lastSoundKey: String? = null
    var lastHapticsEnabled: Boolean? = null

    override fun startAlarm(soundKey: String, hapticsEnabled: Boolean) {
        startCount += 1
        lastSoundKey = soundKey
        lastHapticsEnabled = hapticsEnabled
    }

    override fun stopAlarm() {
        stopCount += 1
    }
}

private class FakeFocusSessionDao : FocusSessionDao {
    private val sessions = MutableStateFlow<List<FocusSessionEntity>>(emptyList())

    override fun observeActiveSession(): Flow<FocusSessionEntity?> = sessions.map { list ->
        list.filter { it.status == "active" }.maxByOrNull { it.updatedAt }
    }

    override fun observeSessionsFrom(fromEpochMillis: Long): Flow<List<FocusSessionEntity>> = sessions.map { list ->
        list.filter { it.startedAt >= fromEpochMillis }.sortedBy { it.startedAt }
    }

    override suspend fun getAll(): List<FocusSessionEntity> = sessions.value

    override suspend fun upsert(session: FocusSessionEntity) {
        sessions.value = sessions.value.filterNot { it.id == session.id } + session
    }

    override suspend fun upsertAll(sessions: List<FocusSessionEntity>) {
        sessions.forEach { upsert(it) }
    }

    override suspend fun clear() {
        sessions.value = emptyList()
    }

    fun latest(): FocusSessionEntity? = sessions.value.maxByOrNull { it.updatedAt }

    fun hasSessions(): Boolean = sessions.value.isNotEmpty()
}

private class FakeWorkingHourRuleDao : WorkingHourRuleDao {
    private val rules = MutableStateFlow<List<WorkingHourRuleEntity>>(emptyList())

    override fun observeRules(): Flow<List<WorkingHourRuleEntity>> = rules

    override suspend fun getAll(): List<WorkingHourRuleEntity> = rules.value

    override suspend fun upsert(rule: WorkingHourRuleEntity) {
        rules.value = rules.value.filterNot { it.id == rule.id } + rule
    }

    override suspend fun upsertAll(rules: List<WorkingHourRuleEntity>) {
        this.rules.value = rules
    }

    override suspend fun clear() {
        rules.value = emptyList()
    }

    fun replace(rules: List<WorkingHourRuleEntity>) {
        this.rules.value = rules
    }
}

private fun epochMillis(date: LocalDate, time: LocalTime): Long =
    date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
