package com.trialreminder.app.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.trialreminder.app.AddTrialActivity
import com.trialreminder.app.R
import com.trialreminder.app.data.TrialDatabase
import com.trialreminder.app.model.Trial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrialAdapter(
    private val trials: List<Trial>,
    private val context: Context
) : RecyclerView.Adapter<TrialAdapter.TrialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trial, parent, false)
        return TrialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrialViewHolder, position: Int) {
        val trial = trials[position]
        holder.bind(trial)
    }

    override fun getItemCount(): Int = trials.size

    inner class TrialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewName: TextView = itemView.findViewById(R.id.textViewTrialName)
        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewTrialDescription)
        private val textViewEndDate: TextView = itemView.findViewById(R.id.textViewEndDate)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
        private val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEdit)

        fun bind(trial: Trial) {
            textViewName.text = trial.name
            textViewDescription.text = trial.description
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val endDateString = dateFormat.format(Date(trial.endDate))
            val reminderDateString = dateFormat.format(Date(trial.reminderTime))
            textViewEndDate.text = "Ends: $endDateString\nReminder: $reminderDateString"

            // Delete button click
            buttonDelete.setOnClickListener {
                showDeleteConfirmation(trial)
            }

            // Edit button click - for now just show info
            buttonEdit.setOnClickListener {
                // In a full implementation, you would open an edit activity
                AlertDialog.Builder(context)
                    .setTitle("Edit Trial")
                    .setMessage("Edit functionality can be added here.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        private fun showDeleteConfirmation(trial: Trial) {
            AlertDialog.Builder(context)
                .setTitle("Delete Trial")
                .setMessage("Are you sure you want to delete '${trial.name}'? This will also cancel the reminder.")
                .setPositiveButton("Delete") { _, _ ->
                    deleteTrial(trial)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun deleteTrial(trial: Trial) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = TrialDatabase.getDatabase(context)
                database.trialDao().delete(trial)
                
                // Cancel the alarm
                AddTrialActivity.cancelAlarm(context, trial.id)
            }
        }
    }
}
