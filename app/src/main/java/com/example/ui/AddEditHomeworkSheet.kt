package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Homework
import com.example.model.HomeworkPriority
import com.example.model.Subject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHomeworkSheet(
    homework: Homework?,
    subjects: List<Subject>,
    defaultDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        subjectName: String,
        subjectColor: Long,
        description: String,
        dueTimestamp: Long,
        alarmTimestamp: Long,
        hasPreReminder: Boolean,
        estimatedMinutes: Int,
        priority: HomeworkPriority
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultSubject = subjects.firstOrNull() ?: Subject(name = "General", colorHex = 0xFF4F46E5)

    var title by remember { mutableStateOf(homework?.title ?: "") }
    var selectedSubjectName by remember { mutableStateOf(homework?.subjectName ?: defaultSubject.name) }
    var selectedSubjectColor by remember { mutableLongStateOf(homework?.subjectColor ?: defaultSubject.colorHex) }
    var description by remember { mutableStateOf(homework?.description ?: "") }

    val now = System.currentTimeMillis()
    val baseAlarm = defaultDateMillis ?: (now + 2 * 60 * 60 * 1000L)
    val baseDue = defaultDateMillis ?: (now + 24 * 60 * 60 * 1000L)

    var alarmTimestamp by remember { mutableLongStateOf(homework?.alarmTimestamp ?: baseAlarm) }
    var dueTimestamp by remember { mutableLongStateOf(homework?.dueTimestamp ?: baseDue) }
    var hasPreReminder by remember { mutableStateOf(homework?.hasPreReminder ?: true) }
    var estimatedMinutes by remember { mutableIntStateOf(homework?.estimatedMinutes ?: 30) }
    var selectedPriority by remember { mutableStateOf(homework?.priorityEnum ?: HomeworkPriority.MEDIUM) }

    var isTitleError by remember { mutableStateOf(false) }

    // Pickers visibility
    var showAlarmDatePicker by remember { mutableStateOf(false) }
    var showAlarmTimePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }

    val dateTimeFormatter = remember { SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_edit_homework_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (homework == null) "New Homework" else "Edit Homework",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Homework Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) isTitleError = false
                },
                label = { Text("Homework Title / Assignment") },
                placeholder = { Text("e.g., Math Ch. 4 Exercises 1-20") },
                isError = isTitleError,
                supportingText = if (isTitleError) { { Text("Title cannot be empty") } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("homework_title_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subject Selector
            Text(
                text = "Subject",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { subject ->
                    val isSelected = selectedSubjectName.equals(subject.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSubjectName = subject.name
                            selectedSubjectColor = subject.colorHex
                        },
                        label = { Text(subject.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(subject.colorHex))
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(subject.colorHex).copy(alpha = 0.2f),
                            selectedLabelColor = Color(subject.colorHex)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Desired "Do Homework" Alarm Time Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Desired Do-Homework Alarm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "The exact time phone rings with default system alarm to sit down and study.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Current Selected Alarm Time Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAlarmDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = dateTimeFormatter.format(Date(alarmTimestamp)),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Tap to change date & time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Edit Alarm",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Quick presets for Alarm
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val preset1 = "In 1 Hour" to (now + 60 * 60 * 1000L)
                        val preset2 = "In 2 Hours" to (now + 2 * 60 * 60 * 1000L)
                        val preset3 = "Tonight 7 PM" to Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 19); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
                        }.timeInMillis
                        val preset4 = "Tomorrow 4 PM" to Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 16); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                        }.timeInMillis

                        listOf(preset1, preset2, preset3, preset4).forEach { (label, ts) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { alarmTimestamp = ts }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2-minute Pre-reminder Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "2-Minute Pre-Reminder",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Sends heads-up notification 2 min before",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = hasPreReminder,
                            onCheckedChange = { hasPreReminder = it },
                            modifier = Modifier.testTag("pre_reminder_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Due Date & Time Section
            Text(
                text = "Due Date (Submission Deadline)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDueDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = dateTimeFormatter.format(Date(dueTimestamp)),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Edit Due Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estimated Duration & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Estimated Duration
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Est. Time",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90).forEach { mins ->
                            FilterChip(
                                selected = estimatedMinutes == mins,
                                onClick = { estimatedMinutes = mins },
                                label = { Text("${mins}m") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Priority
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HomeworkPriority.entries.forEach { p ->
                            FilterChip(
                                selected = selectedPriority == p,
                                onClick = { selectedPriority = p },
                                label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(p.colorHex).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(p.colorHex)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes / Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Instructions & Notes (Optional)") },
                placeholder = { Text("e.g., Pages 42-45, questions 1, 3, 5. Bring protractor.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("homework_description_input"),
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isTitleError = true
                    } else {
                        onSave(
                            title.trim(),
                            selectedSubjectName,
                            selectedSubjectColor,
                            description.trim(),
                            dueTimestamp,
                            alarmTimestamp,
                            hasPreReminder,
                            estimatedMinutes,
                            selectedPriority
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_homework_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (homework == null) "Set Homework & Alarm" else "Update Homework",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // --- Date/Time Pickers for Alarm ---
    if (showAlarmDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = alarmTimestamp)
        DatePickerDialog(
            onDismissRequest = { showAlarmDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val cal = Calendar.getInstance().apply { timeInMillis = alarmTimestamp }
                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                            val min = cal.get(Calendar.MINUTE)

                            val updated = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, min)
                            }.timeInMillis
                            alarmTimestamp = updated
                        }
                        showAlarmDatePicker = false
                        showAlarmTimePicker = true
                    }
                ) {
                    Text("Next: Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAlarmTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = alarmTimestamp }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )
        Dialog(onDismissRequest = { showAlarmTimePicker = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Alarm Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAlarmTimePicker = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                val updated = Calendar.getInstance().apply {
                                    timeInMillis = alarmTimestamp
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                    set(Calendar.SECOND, 0)
                                }.timeInMillis
                                alarmTimestamp = updated
                                showAlarmTimePicker = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }

    // --- Date/Time Pickers for Due Date ---
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val cal = Calendar.getInstance().apply { timeInMillis = dueTimestamp }
                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                            val min = cal.get(Calendar.MINUTE)

                            val updated = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, min)
                            }.timeInMillis
                            dueTimestamp = updated
                        }
                        showDueDatePicker = false
                        showDueTimePicker = true
                    }
                ) {
                    Text("Next: Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDueTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = dueTimestamp }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )
        Dialog(onDismissRequest = { showDueTimePicker = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Due Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDueTimePicker = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                val updated = Calendar.getInstance().apply {
                                    timeInMillis = dueTimestamp
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                    set(Calendar.SECOND, 0)
                                }.timeInMillis
                                dueTimestamp = updated
                                showDueTimePicker = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}
