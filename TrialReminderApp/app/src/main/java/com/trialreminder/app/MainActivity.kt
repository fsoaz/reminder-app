package com.trialreminder.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.trialreminder.app.adapter.TrialAdapter
import com.trialreminder.app.data.TrialDatabase
import com.trialreminder.app.model.Trial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrialAdapter
    private lateinit var database: TrialDatabase

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied. Notifications won't work.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database
        database = TrialDatabase.getDatabase(this)

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load trials
        loadTrials()

        // Setup FAB to add new trial
        val fab: FloatingActionButton = findViewById(R.id.fabAddTrial)
        fab.setOnClickListener {
            val intent = Intent(this, AddTrialActivity::class.java)
            startActivity(intent)
        }

        // Request notification permission for Android 13+
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        loadTrials()
    }

    private fun loadTrials() {
        CoroutineScope(Dispatchers.IO).launch {
            val trials = database.trialDao().getAllTrials()
            withContext(Dispatchers.Main) {
                adapter = TrialAdapter(trials, this@MainActivity)
                recyclerView.adapter = adapter
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
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
