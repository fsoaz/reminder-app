package com.trialreminder.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.trialreminder.app.data.TrialDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule all alarms after device reboot
            rescheduleAllAlarms(context)
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = TrialDatabase.getDatabase(context)
            val trials = database.trialDao().getAllTrialsList()

            for (trial in trials) {
                // Only reschedule if the reminder time is still in the future
                if (trial.reminderTime > System.currentTimeMillis()) {
                    AddTrialActivity.setAlarm(context, trial)
                }
            }
        }
    }
}
