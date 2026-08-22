package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.ui.AlarmRingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeworkAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val homeworkId = intent.getLongExtra(HomeworkAlarmScheduler.EXTRA_HOMEWORK_ID, 0)
        val homeworkTitle = intent.getStringExtra(HomeworkAlarmScheduler.EXTRA_HOMEWORK_TITLE) ?: "Homework"
        val subjectName = intent.getStringExtra(HomeworkAlarmScheduler.EXTRA_SUBJECT_NAME) ?: "Subject"
        val estimatedMinutes = intent.getIntExtra(HomeworkAlarmScheduler.EXTRA_ESTIMATED_MINUTES, 30)

        Log.d("HomeworkAlarmReceiver", "Received action $action for homework #$homeworkId: $homeworkTitle")

        ensureNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            HomeworkAlarmScheduler.ACTION_PRE_REMINDER -> {
                // 2-minute pre-reminder notification
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("HIGHLIGHT_HOMEWORK_ID", homeworkId)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    (homeworkId * 10 + 101).toInt(),
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_PRE_REMINDERS)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("⏰ In 2 mins: $homeworkTitle")
                    .setContentText("Get ready for $subjectName homework (~$estimatedMinutes mins)")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("In 2 minutes, it's time to start working on your $subjectName homework: '$homeworkTitle'. Get your materials ready!"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                notificationManager.notify((homeworkId * 10 + 1).toInt(), builder.build())
            }

            HomeworkAlarmScheduler.ACTION_EXACT_ALARM -> {
                // Exact alarm: Trigger Ring Activity & Heads-up notification
                val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(HomeworkAlarmScheduler.EXTRA_HOMEWORK_ID, homeworkId)
                    putExtra(HomeworkAlarmScheduler.EXTRA_HOMEWORK_TITLE, homeworkTitle)
                    putExtra(HomeworkAlarmScheduler.EXTRA_SUBJECT_NAME, subjectName)
                    putExtra(HomeworkAlarmScheduler.EXTRA_ESTIMATED_MINUTES, estimatedMinutes)
                }

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    (homeworkId * 10 + 102).toInt(),
                    ringIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Snooze 10m action
                val snoozeIntent = Intent(context, HomeworkAlarmReceiver::class.java).apply {
                    this.action = HomeworkAlarmScheduler.ACTION_SNOOZE
                    putExtra(HomeworkAlarmScheduler.EXTRA_HOMEWORK_ID, homeworkId)
                    putExtra(HomeworkAlarmScheduler.EXTRA_HOMEWORK_TITLE, homeworkTitle)
                    putExtra(HomeworkAlarmScheduler.EXTRA_SUBJECT_NAME, subjectName)
                    putExtra(HomeworkAlarmScheduler.EXTRA_SNOOZE_MINUTES, 10)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    (homeworkId * 10 + 103).toInt(),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val builder = NotificationCompat.Builder(context, CHANNEL_EXACT_ALARMS)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("🔔 Time to do: $homeworkTitle")
                    .setContentText("$subjectName • ~$estimatedMinutes mins")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("Time to do your $subjectName homework: '$homeworkTitle'. Tap to start studying or snooze."))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setContentIntent(fullScreenPendingIntent)
                    .setSound(alarmSoundUri)
                    .addAction(android.R.drawable.ic_media_play, "Start Now", fullScreenPendingIntent)
                    .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10m", snoozePendingIntent)

                notificationManager.notify((homeworkId * 10 + 2).toInt(), builder.build())

                try {
                    context.startActivity(ringIntent)
                } catch (e: Exception) {
                    Log.e("HomeworkAlarmReceiver", "Failed to start AlarmRingActivity directly", e)
                }
            }

            HomeworkAlarmScheduler.ACTION_BAG_REMINDER -> {
                // 5-minute bag packing reminder
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("SHOW_BAG_OVERLAY_ID", homeworkId)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    (homeworkId * 10 + 104).toInt(),
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_BAG_REMINDERS)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("🎒 Put in Bag: $homeworkTitle")
                    .setContentText("Don't forget to pack your $subjectName homework into your school bag!")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("🎒 Friendly reminder! Put your completed '$homeworkTitle' ($subjectName) notebook and worksheets into your school bag now so you don't forget them tomorrow!"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                notificationManager.notify((homeworkId * 10 + 3).toInt(), builder.build())
            }

            HomeworkAlarmScheduler.ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(HomeworkAlarmScheduler.EXTRA_SNOOZE_MINUTES, 10)
                AlarmRingtonePlayer.stopAlarm()
                notificationManager.cancel((homeworkId * 10 + 2).toInt())
                HomeworkAlarmScheduler.scheduleSnooze(context, homeworkId, homeworkTitle, subjectName, snoozeMinutes)
            }

            HomeworkAlarmScheduler.ACTION_DISMISS -> {
                AlarmRingtonePlayer.stopAlarm()
                notificationManager.cancel((homeworkId * 10 + 2).toInt())
            }

            HomeworkAlarmScheduler.ACTION_MARK_DONE -> {
                AlarmRingtonePlayer.stopAlarm()
                notificationManager.cancel((homeworkId * 10 + 2).toInt())
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    db.homeworkDao().updateDoneStatus(homeworkId, true, System.currentTimeMillis(), false)
                }
            }
        }
    }

    private fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // 1. Exact Alarms Channel
            val exactChannel = NotificationChannel(
                CHANNEL_EXACT_ALARMS,
                "Homework Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms when homework study time begins"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(alarmSoundUri, audioAttributes)
            }

            // 2. Pre-Reminders Channel
            val preChannel = NotificationChannel(
                CHANNEL_PRE_REMINDERS,
                "2-Minute Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "2-minute heads-up notifications before homework start time"
                enableVibration(true)
            }

            // 3. Bag Reminders Channel
            val bagChannel = NotificationChannel(
                CHANNEL_BAG_REMINDERS,
                "Bag Packing Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to pack completed homework into your school bag"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(exactChannel, preChannel, bagChannel))
        }
    }

    companion object {
        const val CHANNEL_EXACT_ALARMS = "homework_exact_alarms_channel"
        const val CHANNEL_PRE_REMINDERS = "homework_pre_reminders_channel"
        const val CHANNEL_BAG_REMINDERS = "homework_bag_reminders_channel"
    }
}
