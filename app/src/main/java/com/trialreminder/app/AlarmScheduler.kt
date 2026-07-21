package com.trialreminder.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.trialreminder.app.data.TrialDatabase
import com.trialreminder.app.model.Trial

object AlarmScheduler {

    /**
     * @return true if the alarm was scheduled, false if exact-alarm permission is missing
     * or the trial id is invalid.
     */
    fun schedule(context: Context, trial: Trial): Boolean {
        if (trial.id <= 0) {
            return false
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("trial_id", trial.id)
            putExtra("trial_name", trial.name)
            putExtra("trial_description", trial.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            trial.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return false
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trial.reminderTime,
            pendingIntent
        )
        return true
    }

    fun cancel(context: Context, trialId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            trialId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    suspend fun rescheduleFutureAlarms(context: Context) {
        if (!canScheduleExactAlarms(context)) {
            return
        }
        val database = TrialDatabase.getDatabase(context)
        val trials = database.trialDao().getAllTrialsList()
        val now = System.currentTimeMillis()
        for (trial in trials) {
            if (trial.reminderTime > now) {
                schedule(context, trial)
            }
        }
    }
}
