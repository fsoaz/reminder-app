package com.trialreminder.app.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.trialreminder.app.AddTrialActivity
import com.trialreminder.app.AlarmScheduler
import com.trialreminder.app.R
import com.trialreminder.app.data.TrialDatabase
import com.trialreminder.app.model.Trial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrialAdapter(
    private val context: android.content.Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<TrialAdapter.TrialViewHolder>() {

    private val trials = mutableListOf<Trial>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun submitList(newTrials: List<Trial>) {
        trials.clear()
        trials.addAll(newTrials)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trial, parent, false)
        return TrialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrialViewHolder, position: Int) {
        holder.bind(trials[position])
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
            textViewDescription.visibility =
                if (trial.description.isBlank()) View.GONE else View.VISIBLE

            textViewEndDate.text = context.getString(
                R.string.ends_reminder_format,
                dateFormat.format(Date(trial.endDate)),
                dateFormat.format(Date(trial.reminderTime))
            )

            buttonDelete.setOnClickListener {
                showDeleteConfirmation(trial)
            }

            buttonEdit.setOnClickListener {
                val intent = Intent(context, AddTrialActivity::class.java).apply {
                    putExtra(AddTrialActivity.EXTRA_TRIAL_ID, trial.id)
                }
                context.startActivity(intent)
            }
        }

        private fun showDeleteConfirmation(trial: Trial) {
            AlertDialog.Builder(context)
                .setTitle(R.string.delete_trial_title)
                .setMessage(context.getString(R.string.delete_trial_message, trial.name))
                .setPositiveButton(R.string.delete) { _, _ ->
                    deleteTrial(trial)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        private fun deleteTrial(trial: Trial) {
            lifecycleScope.launch(Dispatchers.IO) {
                val database = TrialDatabase.getDatabase(context)
                database.trialDao().delete(trial)
                AlarmScheduler.cancel(context, trial.id)
                withContext(Dispatchers.Main) {
                    onDataChanged()
                }
            }
        }
    }
}
