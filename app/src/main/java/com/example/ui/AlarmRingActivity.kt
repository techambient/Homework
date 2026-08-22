package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.MainActivity
import com.example.alarm.AlarmRingtonePlayer
import com.example.alarm.HomeworkAlarmScheduler
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmRingActivity : ComponentActivity() {

    private var homeworkId: Long = 0
    private var homeworkTitle: String = "Homework"
    private var subjectName: String = "Study"
    private var estimatedMinutes: Int = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnScreenOnAndShowOverLockScreen()

        homeworkId = intent.getLongExtra(HomeworkAlarmScheduler.EXTRA_HOMEWORK_ID, 0)
        homeworkTitle = intent.getStringExtra(HomeworkAlarmScheduler.EXTRA_HOMEWORK_TITLE) ?: "Homework"
        subjectName = intent.getStringExtra(HomeworkAlarmScheduler.EXTRA_SUBJECT_NAME) ?: "General"
        estimatedMinutes = intent.getIntExtra(HomeworkAlarmScheduler.EXTRA_ESTIMATED_MINUTES, 30)

        // Start ringing sound
        AlarmRingtonePlayer.playAlarm(this)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                AlarmRingContent(
                    homeworkTitle = homeworkTitle,
                    subjectName = subjectName,
                    estimatedMinutes = estimatedMinutes,
                    onSnooze = { minutes ->
                        AlarmRingtonePlayer.stopAlarm()
                        HomeworkAlarmScheduler.scheduleSnooze(this, homeworkId, homeworkTitle, subjectName, minutes)
                        finish()
                    },
                    onChangeTime = { newTimeMs ->
                        AlarmRingtonePlayer.stopAlarm()
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("ACTION_CHANGE_TIME_ID", homeworkId)
                            putExtra("ACTION_NEW_TIME_MS", newTimeMs)
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onStartNow = {
                        AlarmRingtonePlayer.stopAlarm()
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("ACTION_START_FOCUS_ID", homeworkId)
                            putExtra("ACTION_START_FOCUS_TITLE", homeworkTitle)
                            putExtra("ACTION_START_FOCUS_SUBJECT", subjectName)
                            putExtra("ACTION_START_FOCUS_MINUTES", estimatedMinutes)
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onDismiss = {
                        AlarmRingtonePlayer.stopAlarm()
                        finish()
                    }
                )
            }
        }
    }

    private fun turnScreenOnAndShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmRingtonePlayer.stopAlarm()
    }
}

@Composable
fun AlarmRingContent(
    homeworkTitle: String,
    subjectName: String,
    estimatedMinutes: Int,
    onSnooze: (Int) -> Unit,
    onChangeTime: (Long) -> Unit,
    onStartNow: () -> Unit,
    onDismiss: () -> Unit
) {
    var showSnoozePicker by remember { mutableStateOf(false) }
    var showChangeTimePicker by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val currentTimeString = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF311042)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Current Time & Dismiss
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTimeString,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("alarm_dismiss_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Alarm",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Middle: Animated Bell & Homework Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                // Glowing pulsating alarm icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Alarm ringing",
                            modifier = Modifier.size(52.dp),
                            tint = Color(0xFF1E1B4B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Subject tag
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.35f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subjectName,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = "Time to do your homework!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = homeworkTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Estimated time: ~$estimatedMinutes mins",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Bottom Actions: Start Now, Snooze, Change Time
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Action: Start Doing Homework Now (Focus Mode)
                Button(
                    onClick = onStartNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("alarm_start_now_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Homework Now",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Row with Snooze and Change Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showSnoozePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("alarm_snooze_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF334155),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Snooze",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = { showChangeTimePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("alarm_change_time_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFDE68A)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Change Time",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Stop / Dismiss Text Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_dismiss_bottom_button")
                ) {
                    Text(
                        text = "Dismiss Alarm",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Snooze Options Dialog
        if (showSnoozePicker) {
            Dialog(onDismissRequest = { showSnoozePicker = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Snooze Homework Alarm",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "When should we remind you again?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilledTonalButton(
                                onClick = {
                                    showSnoozePicker = false
                                    onSnooze(mins)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF334155),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = "$mins Minutes", fontWeight = FontWeight.Medium)
                            }
                        }

                        TextButton(
                            onClick = { showSnoozePicker = false },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        // Change Time Picker Dialog (Quick presets: +1 hour, +2 hours, Tonight 8 PM, Tomorrow 9 AM)
        if (showChangeTimePicker) {
            Dialog(onDismissRequest = { showChangeTimePicker = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Change Alarm Time",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a new time to do this homework:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        val now = System.currentTimeMillis()

                        val options = listOf(
                            "In 1 Hour" to (now + 60 * 60 * 1000L),
                            "In 2 Hours" to (now + 2 * 60 * 60 * 1000L),
                            "Tonight at 8:00 PM" to Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 20)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
                            }.timeInMillis,
                            "Tomorrow Morning (9:00 AM)" to Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }.timeInMillis
                        )

                        options.forEach { (label, timestamp) ->
                            FilledTonalButton(
                                onClick = {
                                    showChangeTimePicker = false
                                    onChangeTime(timestamp)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF334155),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = label, fontWeight = FontWeight.Medium)
                            }
                        }

                        TextButton(
                            onClick = { showChangeTimePicker = false },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}
