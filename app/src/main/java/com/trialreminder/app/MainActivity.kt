package com.trialreminder.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.trialreminder.app.adapter.TrialAdapter
import com.trialreminder.app.data.TrialDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: TrialAdapter
    private lateinit var database: TrialDatabase

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, R.string.notification_permission_granted, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = TrialDatabase.getDatabase(this)
        AlarmReceiver.ensureNotificationChannel(this)

        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.textViewEmpty)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TrialAdapter(this, lifecycleScope) { loadTrials() }
        recyclerView.adapter = adapter

        val fab: FloatingActionButton = findViewById(R.id.fabAddTrial)
        fab.setOnClickListener {
            startActivity(Intent(this, AddTrialActivity::class.java))
        }

        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        loadTrials()
    }

    private fun loadTrials() {
        lifecycleScope.launch {
            val trials = withContext(Dispatchers.IO) {
                database.trialDao().getAllTrialsList()
            }
            adapter.submitList(trials)
            val isEmpty = trials.isEmpty()
            emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE

            if (AlarmScheduler.canScheduleExactAlarms(this@MainActivity)) {
                withContext(Dispatchers.IO) {
                    AlarmScheduler.rescheduleFutureAlarms(this@MainActivity, trials)
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    R.string.exact_alarm_denied,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
