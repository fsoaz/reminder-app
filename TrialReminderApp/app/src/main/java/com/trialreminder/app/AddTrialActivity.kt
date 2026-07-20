package com.trialreminder.app

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.trialreminder.app.data.TrialDatabase
import com.trialreminder.app.model.Trial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AddTrialActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var buttonSelectDate: Button
    private lateinit var buttonSetReminder: Button
    private lateinit var spinnerReminderTime: Spinner
    private lateinit var database: TrialDatabase

    private var selectedDate: Long = 0
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trial)

        // Initialize views
        editTextName = findViewById(R.id.editTextName)
        editTextDescription = findViewById(R.id.editTextDescription)
        buttonSelectDate = findViewById(R.id.buttonSelectDate)
        buttonSetReminder = findViewById(R.id.buttonSetReminder)
        spinnerReminderTime = findViewById(R.id.spinnerReminderTime)

        // Setup spinner with reminder time options
        val reminderOptions = arrayOf("1 day before", "3 days before", "7 days before", "On the day")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reminderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReminderTime.adapter = adapter

        database = TrialDatabase.getDatabase(this)

        // Date picker
        buttonSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Save trial and set alarm
        buttonSetReminder.setOnClickListener {
            saveTrialAndSetAlarm()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                selectedDate = calendar.timeInMillis
                buttonSelectDate.text = "$dayOfMonth/${month + 1}/$year"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveTrialAndSetAlarm() {
        val name = editTextName.text.toString().trim()
        val description = editTextDescription.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a trial name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDate == 0L) {
            Toast.makeText(this, "Please select an end date", Toast.LENGTH_SHORT).show()
            return
        }

        val reminderOption = spinnerReminderTime.selectedItemPosition
        val reminderTime = calculateReminderTime(selectedDate, reminderOption)

        val trial = Trial(
            name = name,
            description = description,
            endDate = selectedDate,
            reminderTime = reminderTime
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.trialDao().insert(trial)
            
            // Set alarm for this trial
            setAlarm(this@AddTrialActivity, trial)

            launch(Dispatchers.Main) {
                Toast.makeText(this@AddTrialActivity, "Trial added and reminder set!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun calculateReminderTime(endDate: Long, option: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endDate
        
        return when (option) {
            0 -> { // 1 day before
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.timeInMillis
            }
            1 -> { // 3 days before
                calendar.add(Calendar.DAY_OF_YEAR, -3)
                calendar.timeInMillis
            }
            2 -> { // 7 days before
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            3 -> { // On the day
                endDate
            }
            else -> endDate
        }
    }

    companion object {
        fun setAlarm(context: Context, trial: Trial) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("trial_id", trial.id)
                putExtra("trial_name", trial.name)
                putExtra("trial_description", trial.description)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                trial.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule exact alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        trial.reminderTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    trial.reminderTime,
                    pendingIntent
                )
            }
        }

        fun cancelAlarm(context: Context, trialId: Int) {
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
    }
}
