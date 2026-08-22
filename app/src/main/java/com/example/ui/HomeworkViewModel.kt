package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HomeworkRepository
import com.example.model.AutoCleanupUtils
import com.example.model.Homework
import com.example.model.HomeworkPriority
import com.example.model.Subject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DashboardTab {
    ALL, TODAY, UPCOMING, COMPLETED
}

enum class MainNavigationItem {
    DASHBOARD, CALENDAR, SUBJECTS, FOCUS_TIMER
}

data class SubjectSummary(
    val subject: Subject,
    val pendingCount: Int,
    val totalCount: Int
)

data class FocusTimerState(
    val homework: Homework? = null,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds else 0f

    val formattedTime: String
        get() {
            val m = remainingSeconds / 60
            val s = remainingSeconds % 60
            return String.format("%02d:%02d", m, s)
        }
}

class HomeworkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HomeworkRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = HomeworkRepository(application, db.homeworkDao(), db.subjectDao())

        // Run auto-cleanup on startup
        viewModelScope.launch {
            repository.cleanupExpiredHomeworks()
        }
    }

    val allSubjects: StateFlow<List<Subject>> = repository.allSubjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHomeworkList: StateFlow<List<Homework>> = repository.allHomeworkFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current navigation tab (Bottom bar)
    private val _currentNav = MutableStateFlow(MainNavigationItem.DASHBOARD)
    val currentNav: StateFlow<MainNavigationItem> = _currentNav.asStateFlow()

    // Dashboard filters
    private val _selectedTab = MutableStateFlow(DashboardTab.ALL)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow<String?>(null)
    val selectedSubjectFilter: StateFlow<String?> = _selectedSubjectFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Calendar selected date (midnight millis)
    private val _selectedCalendarDate = MutableStateFlow(getTodayMidnightMillis())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    // Bag Packing Celebration Overlay
    private val _bagOverlayHomework = MutableStateFlow<Homework?>(null)
    val bagOverlayHomework: StateFlow<Homework?> = _bagOverlayHomework.asStateFlow()

    // Change Time / Reschedule Dialog
    private val _reschedulingHomework = MutableStateFlow<Homework?>(null)
    val reschedulingHomework: StateFlow<Homework?> = _reschedulingHomework.asStateFlow()

    // Add / Edit Sheet
    private val _editingHomework = MutableStateFlow<Homework?>(null)
    val editingHomework: StateFlow<Homework?> = _editingHomework.asStateFlow()
    private val _isAddEditSheetOpen = MutableStateFlow(false)
    val isAddEditSheetOpen: StateFlow<Boolean> = _isAddEditSheetOpen.asStateFlow()

    // Auto-Cleanup Policy Info Modal
    private val _showCleanupInfo = MutableStateFlow(false)
    val showCleanupInfo: StateFlow<Boolean> = _showCleanupInfo.asStateFlow()

    // Focus Study Timer State
    private val _focusTimerState = MutableStateFlow(FocusTimerState())
    val focusTimerState: StateFlow<FocusTimerState> = _focusTimerState.asStateFlow()
    private var timerJob: Job? = null

    // Filtered Homework List for Dashboard
    val filteredHomework: StateFlow<List<Homework>> = combine(
        allHomeworkList,
        _selectedTab,
        _selectedSubjectFilter,
        _searchQuery
    ) { list, tab, subjectFilter, query ->
        val now = System.currentTimeMillis()
        val todayStart = getTodayMidnightMillis()
        val todayEnd = todayStart + (24 * 60 * 60 * 1000L)

        list.filter { hw ->
            // Tab filter
            val tabMatch = when (tab) {
                DashboardTab.ALL -> !hw.isDone
                DashboardTab.TODAY -> !hw.isDone && ((hw.alarmTimestamp in todayStart until todayEnd) || (hw.dueTimestamp in todayStart until todayEnd))
                DashboardTab.UPCOMING -> !hw.isDone && (hw.alarmTimestamp >= todayEnd || hw.dueTimestamp >= todayEnd)
                DashboardTab.COMPLETED -> hw.isDone
            }

            // Subject filter
            val subjectMatch = subjectFilter == null || hw.subjectName.equals(subjectFilter, ignoreCase = true)

            // Search query
            val queryMatch = query.isBlank() ||
                hw.title.contains(query, ignoreCase = true) ||
                hw.subjectName.contains(query, ignoreCase = true) ||
                hw.description.contains(query, ignoreCase = true)

            tabMatch && subjectMatch && queryMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Subject Summaries (Total upcoming haven't done homework by subject)
    val subjectSummaries: StateFlow<List<SubjectSummary>> = combine(
        allSubjects,
        allHomeworkList
    ) { subjects, homeworks ->
        subjects.map { subject ->
            val pending = homeworks.count { it.subjectName.equals(subject.name, ignoreCase = true) && !it.isDone }
            val total = homeworks.count { it.subjectName.equals(subject.name, ignoreCase = true) }
            SubjectSummary(
                subject = subject,
                pendingCount = pending,
                totalCount = total
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calendar Homeworks (for the selected day)
    val calendarSelectedDayHomeworks: StateFlow<List<Homework>> = combine(
        allHomeworkList,
        _selectedCalendarDate
    ) { list, dayMillis ->
        val dayStart = dayMillis
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
        list.filter { hw ->
            (hw.alarmTimestamp in dayStart until dayEnd) || (hw.dueTimestamp in dayStart until dayEnd)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quick counts
    val pendingCount: StateFlow<Int> = allHomeworkList.combine(_selectedTab) { list, _ ->
        list.count { !it.isDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedCount: StateFlow<Int> = allHomeworkList.combine(_selectedTab) { list, _ ->
        list.count { it.isDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Navigation and Filters
    fun setNav(nav: MainNavigationItem) {
        _currentNav.value = nav
    }

    fun setSelectedTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun setSelectedSubjectFilter(subjectName: String?) {
        _selectedSubjectFilter.value = subjectName
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCalendarDate(dateMillis: Long) {
        _selectedCalendarDate.value = getMidnightMillis(dateMillis)
    }

    fun openAddEditSheet(homework: Homework? = null) {
        _editingHomework.value = homework
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _editingHomework.value = null
        _isAddEditSheetOpen.value = false
    }

    fun openRescheduleDialog(homework: Homework) {
        _reschedulingHomework.value = homework
    }

    fun closeRescheduleDialog() {
        _reschedulingHomework.value = null
    }

    fun setShowCleanupInfo(show: Boolean) {
        _showCleanupInfo.value = show
    }

    // Homework actions
    fun saveHomework(
        title: String,
        subjectName: String,
        subjectColor: Long,
        description: String,
        dueTimestamp: Long,
        alarmTimestamp: Long,
        hasPreReminder: Boolean,
        estimatedMinutes: Int,
        priority: HomeworkPriority
    ) {
        viewModelScope.launch {
            val existing = _editingHomework.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    subjectName = subjectName,
                    subjectColor = subjectColor,
                    description = description,
                    dueTimestamp = dueTimestamp,
                    alarmTimestamp = alarmTimestamp,
                    hasPreReminder = hasPreReminder,
                    estimatedMinutes = estimatedMinutes,
                    priority = priority.name
                )
                repository.updateHomework(updated)
            } else {
                val newHw = Homework(
                    title = title,
                    subjectName = subjectName,
                    subjectColor = subjectColor,
                    description = description,
                    dueTimestamp = dueTimestamp,
                    alarmTimestamp = alarmTimestamp,
                    hasPreReminder = hasPreReminder,
                    estimatedMinutes = estimatedMinutes,
                    priority = priority.name
                )
                repository.insertHomework(newHw)
            }
            closeAddEditSheet()
        }
    }

    /**
     * Checkbox toggled:
     * When user marks homework as Done -> trigger the Big "Put in Bag" Animated Overlay!
     */
    fun onToggleDone(homework: Homework, isDone: Boolean) {
        viewModelScope.launch {
            if (isDone) {
                // Trigger Bag Overlay
                _bagOverlayHomework.value = homework
                repository.markHomeworkDone(homework.id, true)
            } else {
                repository.markHomeworkDone(homework.id, false)
            }
        }
    }

    /**
     * User clicks "🎒 Put in Bag" on the celebration overlay
     */
    fun onConfirmPutInBag(homework: Homework) {
        viewModelScope.launch {
            repository.markPackedInBag(homework.id)
            _bagOverlayHomework.value = null
        }
    }

    /**
     * User clicks "⏰ Remind Me Later (5 min)" on the bag overlay
     */
    fun onRemindBagLater(homework: Homework) {
        viewModelScope.launch {
            repository.scheduleBagReminder(homework.id, delayMinutes = 5)
            _bagOverlayHomework.value = null
        }
    }

    fun dismissBagOverlay() {
        _bagOverlayHomework.value = null
    }

    fun rescheduleAlarm(homeworkId: Long, newAlarmTime: Long) {
        viewModelScope.launch {
            repository.updateAlarmTime(homeworkId, newAlarmTime)
            closeRescheduleDialog()
        }
    }

    fun deleteHomework(homework: Homework) {
        viewModelScope.launch {
            repository.deleteHomework(homework)
        }
    }

    fun addSubject(name: String, colorHex: Long, iconName: String, teacher: String, classroom: String) {
        viewModelScope.launch {
            repository.insertSubject(
                Subject(
                    name = name,
                    colorHex = colorHex,
                    iconName = iconName,
                    teacherName = teacher,
                    classroom = classroom
                )
            )
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    // Focus Timer Operations
    fun startFocusTimerForHomework(homework: Homework) {
        val durationSeconds = homework.estimatedMinutes * 60
        _focusTimerState.value = FocusTimerState(
            homework = homework,
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            isRunning = true,
            isFinished = false
        )
        _currentNav.value = MainNavigationItem.FOCUS_TIMER
        startTimerTicking()
    }

    fun toggleFocusTimer() {
        val current = _focusTimerState.value
        if (current.isRunning) {
            timerJob?.cancel()
            _focusTimerState.value = current.copy(isRunning = false)
        } else if (current.remainingSeconds > 0) {
            _focusTimerState.value = current.copy(isRunning = true)
            startTimerTicking()
        }
    }

    fun resetFocusTimer() {
        timerJob?.cancel()
        val current = _focusTimerState.value
        _focusTimerState.value = current.copy(
            remainingSeconds = current.totalSeconds,
            isRunning = false,
            isFinished = false
        )
    }

    fun setFocusTimerDuration(minutes: Int) {
        timerJob?.cancel()
        val total = minutes * 60
        _focusTimerState.value = _focusTimerState.value.copy(
            totalSeconds = total,
            remainingSeconds = total,
            isRunning = false,
            isFinished = false
        )
    }

    fun finishFocusAndPackBag() {
        val hw = _focusTimerState.value.homework
        timerJob?.cancel()
        _focusTimerState.value = _focusTimerState.value.copy(isRunning = false, isFinished = true)
        if (hw != null) {
            onToggleDone(hw, true)
        }
    }

    private fun startTimerTicking() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_focusTimerState.value.isRunning && _focusTimerState.value.remainingSeconds > 0) {
                delay(1000)
                val newSeconds = _focusTimerState.value.remainingSeconds - 1
                _focusTimerState.value = _focusTimerState.value.copy(
                    remainingSeconds = newSeconds,
                    isFinished = newSeconds <= 0,
                    isRunning = newSeconds > 0
                )
            }
        }
    }

    companion object {
        fun getTodayMidnightMillis(): Long {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        fun getMidnightMillis(timestamp: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}
