package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.Homework

object HomeworkAlarmScheduler {
    private const val TAG = "AlarmScheduler"

    const val ACTION_PRE_REMINDER = "com.example.homework.ACTION_PRE_REMINDER"
    const val ACTION_EXACT_ALARM = "com.example.homework.ACTION_EXACT_ALARM"
    const val ACTION_BAG_REMINDER = "com.example.homework.ACTION_BAG_REMINDER"
    const val ACTION_SNOOZE = "com.example.homework.ACTION_SNOOZE"
    const val ACTION_DISMISS = "com.example.homework.ACTION_DISMISS"
    const val ACTION_MARK_DONE = "com.example.homework.ACTION_MARK_DONE"

    const val EXTRA_HOMEWORK_ID = "extra_homework_id"
    const val EXTRA_HOMEWORK_TITLE = "extra_homework_title"
    const val EXTRA_SUBJECT_NAME = "extra_subject_name"
    const val EXTRA_ESTIMATED_MINUTES = "extra_estimated_minutes"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    fun scheduleHomeworkAlarms(context: Context, homework: Homework) {
        if (homework.isDone) {
            cancelAlarms(context, homework.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        // 1. Schedule 2-minute Pre-Reminder
        if (homework.hasPreReminder) {
            val preReminderTime = homework.alarmTimestamp - (2 * 60 * 1000L)
            if (preReminderTime > now) {
                val intent = Intent(context, HomeworkAlarmReceiver::class.java).apply {
                    action = ACTION_PRE_REMINDER
                    putExtra(EXTRA_HOMEWORK_ID, homework.id)
                    putExtra(EXTRA_HOMEWORK_TITLE, homework.title)
                    putExtra(EXTRA_SUBJECT_NAME, homework.subjectName)
                    putExtra(EXTRA_ESTIMATED_MINUTES, homework.estimatedMinutes)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    getPreReminderRequestCode(homework.id),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleExact(alarmManager, preReminderTime, pendingIntent)
                Log.d(TAG, "Scheduled 2-minute pre-reminder for homework #${homework.id} at $preReminderTime")
            }
        }

        // 2. Schedule Exact "Do Homework" Alarm
        if (homework.alarmTimestamp > now) {
            val intent = Intent(context, HomeworkAlarmReceiver::class.java).apply {
                action = ACTION_EXACT_ALARM
                putExtra(EXTRA_HOMEWORK_ID, homework.id)
                putExtra(EXTRA_HOMEWORK_TITLE, homework.title)
                putExtra(EXTRA_SUBJECT_NAME, homework.subjectName)
                putExtra(EXTRA_ESTIMATED_MINUTES, homework.estimatedMinutes)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getExactAlarmRequestCode(homework.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExact(alarmManager, homework.alarmTimestamp, pendingIntent)
            Log.d(TAG, "Scheduled exact alarm for homework #${homework.id} at ${homework.alarmTimestamp}")
        }
    }

    fun scheduleBagReminder(context: Context, homeworkId: Long, homeworkTitle: String, subjectName: String, delayMinutes: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        val intent = Intent(context, HomeworkAlarmReceiver::class.java).apply {
            action = ACTION_BAG_REMINDER
            putExtra(EXTRA_HOMEWORK_ID, homeworkId)
            putExtra(EXTRA_HOMEWORK_TITLE, homeworkTitle)
            putExtra(EXTRA_SUBJECT_NAME, subjectName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getBagReminderRequestCode(homeworkId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, triggerAt, pendingIntent)
        Log.d(TAG, "Scheduled bag packing reminder for homework #$homeworkId in $delayMinutes minutes")
    }

    fun scheduleSnooze(context: Context, homeworkId: Long, homeworkTitle: String, subjectName: String, snoozeMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, HomeworkAlarmReceiver::class.java).apply {
            action = ACTION_EXACT_ALARM
            putExtra(EXTRA_HOMEWORK_ID, homeworkId)
            putExtra(EXTRA_HOMEWORK_TITLE, homeworkTitle)
            putExtra(EXTRA_SUBJECT_NAME, subjectName)
            putExtra(EXTRA_ESTIMATED_MINUTES, 30)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getExactAlarmRequestCode(homeworkId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, triggerAt, pendingIntent)
        Log.d(TAG, "Snoozed alarm for homework #$homeworkId by $snoozeMinutes minutes")
    }

    fun cancelAlarms(context: Context, homeworkId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel Pre-reminder
        val preIntent = Intent(context, HomeworkAlarmReceiver::class.java).apply { action = ACTION_PRE_REMINDER }
        val prePending = PendingIntent.getBroadcast(
            context,
            getPreReminderRequestCode(homeworkId),
            preIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (prePending != null) {
            alarmManager.cancel(prePending)
            prePending.cancel()
        }

        // Cancel Exact Alarm
        val exactIntent = Intent(context, HomeworkAlarmReceiver::class.java).apply { action = ACTION_EXACT_ALARM }
        val exactPending = PendingIntent.getBroadcast(
            context,
            getExactAlarmRequestCode(homeworkId),
            exactIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (exactPending != null) {
            alarmManager.cancel(exactPending)
            exactPending.cancel()
        }

        // Cancel Bag Reminder
        val bagIntent = Intent(context, HomeworkAlarmReceiver::class.java).apply { action = ACTION_BAG_REMINDER }
        val bagPending = PendingIntent.getBroadcast(
            context,
            getBagReminderRequestCode(homeworkId),
            bagIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (bagPending != null) {
            alarmManager.cancel(bagPending)
            bagPending.cancel()
        }
    }

    private fun scheduleExact(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun getPreReminderRequestCode(homeworkId: Long) = (homeworkId * 10 + 1).toInt()
    private fun getExactAlarmRequestCode(homeworkId: Long) = (homeworkId * 10 + 2).toInt()
    private fun getBagReminderRequestCode(homeworkId: Long) = (homeworkId * 10 + 3).toInt()
}
