package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Homework
import com.example.ui.AddEditHomeworkSheet
import com.example.ui.AutoCleanupInfoDialog
import com.example.ui.BagPackingOverlay
import com.example.ui.CalendarScreen
import com.example.ui.FocusTimerScreen
import com.example.ui.HomeScreen
import com.example.ui.HomeworkViewModel
import com.example.ui.MainNavigationItem
import com.example.ui.RescheduleDialog
import com.example.ui.SubjectsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeworkViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Notification permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for Notification permission on Android 13+
        checkAndRequestNotificationPermission()

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Check if opened from Bag Reminder Notification
        val bagOverlayId = intent.getLongExtra("SHOW_BAG_OVERLAY_ID", 0)
        if (bagOverlayId > 0) {
            // Find homework and show bag overlay
            val hw = viewModel.allHomeworkList.value.find { it.id == bagOverlayId }
            if (hw != null) {
                viewModel.onToggleDone(hw, true)
            }
        }

        // 2. Check if opened to start focus mode from alarm ring screen
        val focusId = intent.getLongExtra("ACTION_START_FOCUS_ID", 0)
        if (focusId > 0) {
            val hw = viewModel.allHomeworkList.value.find { it.id == focusId }
            if (hw != null) {
                viewModel.startFocusTimerForHomework(hw)
            }
        }

        // 3. Check if opened from change time on alarm ring screen
        val changeTimeId = intent.getLongExtra("ACTION_CHANGE_TIME_ID", 0)
        val newTimeMs = intent.getLongExtra("ACTION_NEW_TIME_MS", 0)
        if (changeTimeId > 0 && newTimeMs > 0) {
            viewModel.rescheduleAlarm(changeTimeId, newTimeMs)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: HomeworkViewModel) {
    val currentNav by viewModel.currentNav.collectAsStateWithLifecycle()
    val allHomework by viewModel.allHomeworkList.collectAsStateWithLifecycle()
    val filteredHomework by viewModel.filteredHomework.collectAsStateWithLifecycle()
    val subjectSummaries by viewModel.subjectSummaries.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedSubjectFilter by viewModel.selectedSubjectFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()
    val calendarSelectedDayHomeworks by viewModel.calendarSelectedDayHomeworks.collectAsStateWithLifecycle()

    val bagOverlayHomework by viewModel.bagOverlayHomework.collectAsStateWithLifecycle()
    val reschedulingHomework by viewModel.reschedulingHomework.collectAsStateWithLifecycle()
    val isAddEditSheetOpen by viewModel.isAddEditSheetOpen.collectAsStateWithLifecycle()
    val editingHomework by viewModel.editingHomework.collectAsStateWithLifecycle()
    val showCleanupInfo by viewModel.showCleanupInfo.collectAsStateWithLifecycle()
    val focusTimerState by viewModel.focusTimerState.collectAsStateWithLifecycle()
    val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentNav == MainNavigationItem.DASHBOARD,
                    onClick = { viewModel.setNav(MainNavigationItem.DASHBOARD) },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Dashboard") },
                    label = { Text("Tasks", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = currentNav == MainNavigationItem.CALENDAR,
                    onClick = { viewModel.setNav(MainNavigationItem.CALENDAR) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                    label = { Text("Calendar", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = currentNav == MainNavigationItem.SUBJECTS,
                    onClick = { viewModel.setNav(MainNavigationItem.SUBJECTS) },
                    icon = { Icon(Icons.Default.School, contentDescription = "Subjects") },
                    label = { Text("Subjects", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = currentNav == MainNavigationItem.FOCUS_TIMER,
                    onClick = { viewModel.setNav(MainNavigationItem.FOCUS_TIMER) },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Focus Timer") },
                    label = { Text("Focus", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentNav,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "screen_navigation"
        ) { nav ->
            when (nav) {
                MainNavigationItem.DASHBOARD -> {
                    HomeScreen(
                        homeworkList = filteredHomework,
                        subjectSummaries = subjectSummaries,
                        selectedTab = selectedTab,
                        selectedSubjectFilter = selectedSubjectFilter,
                        searchQuery = searchQuery,
                        onTabSelected = { viewModel.setSelectedTab(it) },
                        onSubjectFilterSelected = { viewModel.setSelectedSubjectFilter(it) },
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onToggleDone = { hw, isDone -> viewModel.onToggleDone(hw, isDone) },
                        onOpenAddHomework = { viewModel.openAddEditSheet() },
                        onEditHomework = { viewModel.openAddEditSheet(it) },
                        onDeleteHomework = { viewModel.deleteHomework(it) },
                        onRescheduleAlarm = { viewModel.openRescheduleDialog(it) },
                        onStartFocus = { viewModel.startFocusTimerForHomework(it) },
                        onShowCleanupInfo = { viewModel.setShowCleanupInfo(true) }
                    )
                }

                MainNavigationItem.CALENDAR -> {
                    CalendarScreen(
                        allHomework = allHomework,
                        selectedDateMillis = selectedCalendarDate,
                        selectedDayHomeworks = calendarSelectedDayHomeworks,
                        onDateSelected = { viewModel.setSelectedCalendarDate(it) },
                        onAddHomeworkForDate = { dateMillis ->
                            viewModel.setSelectedCalendarDate(dateMillis)
                            viewModel.openAddEditSheet()
                        },
                        onToggleDone = { hw, isDone -> viewModel.onToggleDone(hw, isDone) },
                        onEditHomework = { viewModel.openAddEditSheet(it) },
                        onDeleteHomework = { viewModel.deleteHomework(it) },
                        onRescheduleAlarm = { viewModel.openRescheduleDialog(it) },
                        onStartFocus = { viewModel.startFocusTimerForHomework(it) }
                    )
                }

                MainNavigationItem.SUBJECTS -> {
                    SubjectsScreen(
                        summaries = subjectSummaries,
                        onAddSubject = { name, colorHex, iconName, teacher, classroom ->
                            viewModel.addSubject(name, colorHex, iconName, teacher, classroom)
                        },
                        onDeleteSubject = { viewModel.deleteSubject(it) }
                    )
                }

                MainNavigationItem.FOCUS_TIMER -> {
                    FocusTimerScreen(
                        timerState = focusTimerState,
                        pendingHomeworks = allHomework.filter { !it.isDone },
                        onToggleTimer = { viewModel.toggleFocusTimer() },
                        onResetTimer = { viewModel.resetFocusTimer() },
                        onSetDuration = { viewModel.setFocusTimerDuration(it) },
                        onSelectHomework = { viewModel.startFocusTimerForHomework(it) },
                        onFinishAndPackBag = { viewModel.finishFocusAndPackBag() }
                    )
                }
            }
        }

        // --- Big "Put in Bag" Animated Overlay Dialog ---
        BagPackingOverlay(
            homework = bagOverlayHomework,
            onPutInBag = { hw -> viewModel.onConfirmPutInBag(hw) },
            onRemindLater = { hw -> viewModel.onRemindBagLater(hw) },
            onDismiss = { viewModel.dismissBagOverlay() }
        )

        // --- Reschedule Dialog ---
        if (reschedulingHomework != null) {
            RescheduleDialog(
                homework = reschedulingHomework,
                onReschedule = { newTime ->
                    reschedulingHomework?.let { hw ->
                        viewModel.rescheduleAlarm(hw.id, newTime)
                    }
                },
                onDismiss = { viewModel.closeRescheduleDialog() }
            )
        }

        // --- Auto-Cleanup Rules Info Dialog ---
        if (showCleanupInfo) {
            AutoCleanupInfoDialog(
                onDismiss = { viewModel.setShowCleanupInfo(false) }
            )
        }

        // --- Add / Edit Homework Bottom Sheet ---
        if (isAddEditSheetOpen) {
            AddEditHomeworkSheet(
                homework = editingHomework,
                subjects = allSubjects,
                defaultDateMillis = selectedCalendarDate,
                onDismiss = { viewModel.closeAddEditSheet() },
                onSave = { title, subjectName, subjectColor, desc, dueTs, alarmTs, hasPre, estMins, priority ->
                    viewModel.saveHomework(
                        title = title,
                        subjectName = subjectName,
                        subjectColor = subjectColor,
                        description = desc,
                        dueTimestamp = dueTs,
                        alarmTimestamp = alarmTs,
                        hasPreReminder = hasPre,
                        estimatedMinutes = estMins,
                        priority = priority
                    )
                }
            )
        }
    }
}
