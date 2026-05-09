package com.fitnessdatapuller.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            SyncNotifications.postPersistent(
                applicationContext,
                "Auto-sync scheduled. Tap Sync now to update immediately.",
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = SettingsStore(applicationContext)
        val healthConnectManager = HealthConnectManager(applicationContext)
        val syncClient = SyncClient()

        SyncNotifications.ensureChannel(applicationContext)
        SyncScheduler.scheduleDaily(applicationContext)
        ensureNotificationPermission()
        SyncNotifications.postPersistent(
            applicationContext,
            "Auto-sync scheduled. Tap Sync now to update immediately.",
        )

        setContent {
            FitnessDataPullerTheme {
                App(
                    settingsStore = settingsStore,
                    healthConnectManager = healthConnectManager,
                    syncClient = syncClient,
                )
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun App(
    settingsStore: SettingsStore,
    healthConnectManager: HealthConnectManager,
    syncClient: SyncClient,
) {
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    var settings by remember { mutableStateOf(settingsStore.load()) }
    var selectedTab by remember { mutableStateOf(0) }
    var permissionStatus by remember { mutableStateOf("Checking…") }
    var preview by remember { mutableStateOf("Press Sync Today to read Health Connect data.") }
    var lastResult by remember { mutableStateOf("Never synced") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    fun refreshPermissionStatus() {
        scope.launch {
            permissionStatus = if (!healthConnectManager.isAvailable()) {
                "Health Connect is not available. Install or enable Health Connect, then reopen the app."
            } else if (healthConnectManager.hasAllPermissions()) {
                "All requested read permissions granted."
            } else {
                "Missing one or more Health Connect read permissions."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = healthConnectManager.requestPermissionsContract(),
    ) { grantedPermissions ->
        permissionStatus = if (grantedPermissions.containsAll(healthConnectManager.permissions)) {
            "All requested read permissions granted."
        } else {
            "Missing one or more Health Connect read permissions."
        }
        refreshPermissionStatus()
    }

    LaunchedEffect(Unit) { refreshPermissionStatus() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF05070A)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Sync") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Settings") })
            }

            when (selectedTab) {
                0 -> MainScreen(
                    permissionStatus = permissionStatus,
                    preview = preview,
                    lastResult = lastResult,
                    error = error,
                    isSyncing = isSyncing,
                    onRequestPermissions = {
                        scope.launch {
                            error = null
                            when {
                                !healthConnectManager.isAvailable() -> {
                                    permissionStatus = "Health Connect is not available. Install or enable Health Connect, then reopen the app."
                                    error = permissionStatus
                                }
                                healthConnectManager.hasAllPermissions() -> {
                                    permissionStatus = "All requested read permissions granted."
                                }
                                else -> permissionLauncher.launch(healthConnectManager.permissions)
                            }
                        }
                    },
                    onSyncToday = {
                        scope.launch {
                            error = null
                            isSyncing = true
                            lastResult = "Syncing…"
                            if (!healthConnectManager.isAvailable()) {
                                permissionStatus = "Health Connect is not available. Install or enable Health Connect, then reopen the app."
                                error = permissionStatus
                                lastResult = "Sync failed"
                                isSyncing = false
                                return@launch
                            }
                            if (!healthConnectManager.hasAllPermissions()) {
                                permissionStatus = "Missing one or more Health Connect read permissions."
                                lastResult = "Waiting for Health Connect permissions"
                                isSyncing = false
                                permissionLauncher.launch(healthConnectManager.permissions)
                                return@launch
                            }
                            runCatching {
                                val payload = healthConnectManager.readToday()
                                preview = syncClient.preview(payload)
                                val result = syncClient.sync(settings, payload)
                                payload to result
                            }.onSuccess { (payload, result) ->
                                lastResult = result
                                val workouts = payload.workoutCount
                                val cal = payload.nutrition.calories?.toInt()
                                val summary = buildString {
                                    append("Synced · steps ")
                                    append(payload.steps)
                                    if (cal != null && cal > 0) {
                                        append(" · food ")
                                        append(cal)
                                        append(" kcal")
                                    }
                                    if (workouts > 0) {
                                        append(" · ")
                                        append(workouts)
                                        append(if (workouts == 1) " workout" else " workouts")
                                    }
                                }
                                SyncNotifications.postPersistent(appContext, summary)
                                refreshPermissionStatus()
                            }.onFailure { throwable ->
                                error = throwable.message ?: throwable::class.java.simpleName
                                lastResult = "Sync failed"
                            }
                            isSyncing = false
                        }
                    },
                )
                1 -> SettingsScreen(
                    settings = settings,
                    onSave = { updated ->
                        settingsStore.save(updated)
                        settings = settingsStore.load()
                        lastResult = "Settings saved locally"
                        selectedTab = 0
                    },
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    permissionStatus: String,
    preview: String,
    lastResult: String,
    error: String?,
    isSyncing: Boolean,
    onRequestPermissions: () -> Unit,
    onSyncToday: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("FitnessDataPuller Mobile", style = MaterialTheme.typography.headlineMedium)
        StatusCard(
            title = "Auto-sync",
            body = "Daily auto-sync runs at 8:00 AM. The persistent SeanOS notification has a Sync now button so you can trigger it from the drop-down anytime.",
        )
        StatusCard(title = "Health Connect permission status", body = permissionStatus)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRequestPermissions, enabled = !isSyncing) { Text("Request permissions") }
            Button(onClick = onSyncToday, enabled = !isSyncing) { Text(if (isSyncing) "Syncing…" else "Sync Today") }
        }
        StatusCard(title = "JSON preview", body = preview, monospace = true)
        StatusCard(title = "Last sync result", body = lastResult)
        if (error != null) {
            StatusCard(title = "Error", body = error, error = true)
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: SyncSettings,
    onSave: (SyncSettings) -> Unit,
) {
    var apiBaseUrl by remember(settings) { mutableStateOf(settings.apiBaseUrl) }
    var syncApiKey by remember(settings) { mutableStateOf(settings.syncApiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = apiBaseUrl,
            onValueChange = { apiBaseUrl = it },
            label = { Text("API Base URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        OutlinedTextField(
            value = syncApiKey,
            onValueChange = { syncApiKey = it },
            label = { Text("SYNC_API_KEY") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (syncApiKey.isBlank()) VisualTransformation.None else PasswordVisualTransformation(),
        )
        Button(
            onClick = { onSave(SyncSettings(apiBaseUrl = apiBaseUrl, syncApiKey = syncApiKey)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save settings locally")
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    monospace: Boolean = false,
    error: Boolean = false,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (error) Color(0xFF3A1116) else Color(0xFF101720),
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (error) Color(0xFFFF6B7A) else Color(0xFF65D46E), RoundedCornerShape(99.dp)),
                )
                Spacer(Modifier.size(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            )
        }
    }
}

@Composable
private fun FitnessDataPullerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF65D46E),
            secondary = Color(0xFF8AB4F8),
            background = Color(0xFF05070A),
            surface = Color(0xFF05070A),
            error = Color(0xFFFF6B7A),
            onPrimary = Color(0xFF031006),
            onBackground = Color(0xFFE8EAED),
            onSurface = Color(0xFFE8EAED),
        ),
        content = content,
    )
}
