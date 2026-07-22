package com.trialreminder.app

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.trialreminder.app.data.TrialDatabase
import com.trialreminder.app.model.Trial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddTrialActivity : AppCompatActivity() {

    private lateinit var textViewTitle: TextView
    private lateinit var editTextName: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var buttonSelectDate: Button
    private lateinit var buttonSetReminder: Button
    private lateinit var spinnerReminderTime: Spinner
    private lateinit var database: TrialDatabase

    private var editingTrialId: Int = NO_TRIAL_ID
    private var selectedDate: Long = 0
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trial)

        textViewTitle = findViewById(R.id.textViewTitle)
        editTextName = findViewById(R.id.editTextName)
        editTextDescription = findViewById(R.id.editTextDescription)
        buttonSelectDate = findViewById(R.id.buttonSelectDate)
        buttonSetReminder = findViewById(R.id.buttonSetReminder)
        spinnerReminderTime = findViewById(R.id.spinnerReminderTime)

        val reminderOptions = resources.getStringArray(R.array.reminder_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reminderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReminderTime.adapter = adapter

        database = TrialDatabase.getDatabase(this)
        editingTrialId = intent.getIntExtra(EXTRA_TRIAL_ID, NO_TRIAL_ID)

        if (isEditMode) {
            textViewTitle.setText(R.string.edit_trial)
            buttonSetReminder.setText(R.string.update_reminder)
            loadTrialForEdit(editingTrialId)
        } else {
            textViewTitle.setText(R.string.add_new_trial)
            buttonSetReminder.setText(R.string.set_reminder)
        }

        buttonSelectDate.setOnClickListener {
            showDatePicker()
        }

        buttonSetReminder.setOnClickListener {
            saveTrialAndSetAlarm()
        }
    }

    private val isEditMode: Boolean
        get() = editingTrialId != NO_TRIAL_ID

    private fun loadTrialForEdit(trialId: Int) {
        lifecycleScope.launch {
            val trial = withContext(Dispatchers.IO) {
                database.trialDao().getById(trialId)
            }
            if (trial == null) {
                Toast.makeText(this@AddTrialActivity, R.string.trial_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            populateForm(trial)
        }
    }

    private fun populateForm(trial: Trial) {
        editTextName.setText(trial.name)
        editTextDescription.setText(trial.description)
        selectedDate = trial.endDate
        calendar.timeInMillis = trial.endDate
        buttonSelectDate.text = formatDateLabel(calendar)
        spinnerReminderTime.setSelection(inferReminderOption(trial.endDate, trial.reminderTime))
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 9, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                selectedDate = calendar.timeInMillis
                buttonSelectDate.text = formatDateLabel(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDateLabel(calendar: Calendar): String {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    private fun saveTrialAndSetAlarm() {
        val name = editTextName.text.toString().trim()
        val description = editTextDescription.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, R.string.trial_name_required, Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDate == 0L) {
            Toast.makeText(this, R.string.end_date_required, Toast.LENGTH_SHORT).show()
            return
        }

        val reminderOption = spinnerReminderTime.selectedItemPosition
        val reminderTime = calculateReminderTime(selectedDate, reminderOption)

        if (reminderTime <= System.currentTimeMillis()) {
            Toast.makeText(this, R.string.reminder_in_past, Toast.LENGTH_LONG).show()
            return
        }

        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            promptExactAlarmPermission()
            return
        }

        lifecycleScope.launch {
            if (isEditMode) {
                val trial = Trial(
                    id = editingTrialId,
                    name = name,
                    description = description,
                    endDate = selectedDate,
                    reminderTime = reminderTime
                )
                withContext(Dispatchers.IO) {
                    AlarmScheduler.cancel(this@AddTrialActivity, editingTrialId)
                    database.trialDao().update(trial)
                }
                val scheduled = AlarmScheduler.schedule(this@AddTrialActivity, trial)
                Toast.makeText(this@AddTrialActivity, R.string.trial_updated, Toast.LENGTH_SHORT).show()
                if (!scheduled) {
                    Toast.makeText(this@AddTrialActivity, R.string.exact_alarm_denied, Toast.LENGTH_LONG).show()
                }
            } else {
                val trial = Trial(
                    name = name,
                    description = description,
                    endDate = selectedDate,
                    reminderTime = reminderTime
                )
                val insertedId = withContext(Dispatchers.IO) {
                    database.trialDao().insert(trial)
                }
                val scheduled = AlarmScheduler.schedule(this@AddTrialActivity, trial.copy(id = insertedId.toInt()))
                Toast.makeText(this@AddTrialActivity, R.string.trial_added, Toast.LENGTH_SHORT).show()
                if (!scheduled) {
                    Toast.makeText(this@AddTrialActivity, R.string.exact_alarm_denied, Toast.LENGTH_LONG).show()
                }
            }
            finish()
        }
    }

    private fun promptExactAlarmPermission() {
        AlertDialog.Builder(this)
            .setMessage(R.string.exact_alarm_denied)
            .setPositiveButton(R.string.exact_alarm_open_settings) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_TRIAL_ID = "extra_trial_id"
        private const val NO_TRIAL_ID = -1

        fun inferReminderOption(endDate: Long, reminderTime: Long): Int {
            for (option in 0..2) {
                if (calculateReminderTime(endDate, option) == reminderTime) {
                    return option
                }
            }
            return 3
        }

        private fun calculateReminderTime(endDate: Long, option: Int): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = endDate

            return when (option) {
                0 -> {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    cal.timeInMillis
                }
                1 -> {
                    cal.add(Calendar.DAY_OF_YEAR, -3)
                    cal.timeInMillis
                }
                2 -> {
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    cal.timeInMillis
                }
                else -> endDate
            }
        }
    }
}
