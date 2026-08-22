package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Homework
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    allHomework: List<Homework>,
    selectedDateMillis: Long,
    selectedDayHomeworks: List<Homework>,
    onDateSelected: (Long) -> Unit,
    onAddHomeworkForDate: (Long) -> Unit,
    onToggleDone: (Homework, Boolean) -> Unit,
    onEditHomework: (Homework) -> Unit,
    onDeleteHomework: (Homework) -> Unit,
    onRescheduleAlarm: (Homework) -> Unit,
    onStartFocus: (Homework) -> Unit
) {
    var calendarMonth by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDateMillis })
    }

    val monthNameFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val daySelectedFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val currentMonthCalendar = remember(calendarMonth.timeInMillis) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = calendarMonth.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal
    }

    val daysInMonth = remember(calendarMonth.timeInMillis) {
        currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(calendarMonth.timeInMillis) {
        currentMonthCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-based for Sun
    }

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Header
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("calendar_month_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month navigation row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            calendarMonth = (calendarMonth.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                calendarMonth = Calendar.getInstance()
                                onDateSelected(todayMidnight)
                            }
                        ) {
                            Text(
                                text = monthNameFormatter.format(calendarMonth.time),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            calendarMonth = (calendarMonth.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days of week header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        daysOfWeek.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(38.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Days Grid (6 rows x 7 days)
                    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

                    for (row in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - firstDayOfWeek + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val cellCal = (calendarMonth.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayNumber)
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }
                                    val cellMillis = cellCal.timeInMillis

                                    val isSelected = cellMillis == selectedDateMillis
                                    val isToday = cellMillis == todayMidnight

                                    // Find homeworks on this day to render indicator dots
                                    val dayEnd = cellMillis + 24 * 60 * 60 * 1000L
                                    val dayHomeworks = allHomework.filter {
                                        (it.alarmTimestamp in cellMillis until dayEnd) || (it.dueTimestamp in cellMillis until dayEnd)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { onDateSelected(cellMillis) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNumber",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontSize = 13.sp
                                            )

                                            // Subject Dots
                                            if (dayHomeworks.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(top = 1.dp)
                                                ) {
                                                    dayHomeworks.take(3).forEach { hw ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSelected) Color.White else Color(hw.subjectColor)
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(42.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Agenda Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Agenda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = daySelectedFormatter.format(Date(selectedDateMillis)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onAddHomeworkForDate(selectedDateMillis) }
                        .testTag("calendar_add_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add for this day",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Homeworks for selected day
        if (selectedDayHomeworks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No homework scheduled for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(selectedDayHomeworks, key = { it.id }) { homework ->
                HomeworkCard(
                    homework = homework,
                    onToggleDone = { isDone -> onToggleDone(homework, isDone) },
                    onEdit = { onEditHomework(homework) },
                    onDelete = { onDeleteHomework(homework) },
                    onReschedule = { onRescheduleAlarm(homework) },
                    onStartFocus = { onStartFocus(homework) }
                )
            }
        }
    }
}
