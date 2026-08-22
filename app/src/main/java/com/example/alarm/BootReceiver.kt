package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Device rebooted, rescheduling pending homework alarms...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val pendingHomework = db.homeworkDao().getAllPendingHomework()
                    val now = System.currentTimeMillis()
                    for (homework in pendingHomework) {
                        if (homework.alarmTimestamp > now) {
                            HomeworkAlarmScheduler.scheduleHomeworkAlarms(context, homework)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms on boot", e)
                }
            }
        }
    }
}
