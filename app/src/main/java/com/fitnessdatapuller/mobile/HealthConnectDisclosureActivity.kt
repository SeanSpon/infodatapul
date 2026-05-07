package com.fitnessdatapuller.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class HealthConnectDisclosureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthConnectDisclosureTheme {
                DisclosureScreen(
                    title = "Health Connect data use",
                    buttonLabel = "Done",
                    onDone = ::finish,
                )
            }
        }
    }
}

class HealthConnectOnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthConnectDisclosureTheme {
                DisclosureScreen(
                    title = "Connect FitnessDataPuller Mobile",
                    buttonLabel = "Continue",
                    onDone = ::finish,
                )
            }
        }
    }
}

@Composable
private fun DisclosureScreen(
    title: String,
    buttonLabel: String,
    onDone: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF05070A)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                "FitnessDataPuller Mobile reads only the Health Connect data shown in the permission screen: " +
                    "steps, active calories, sleep sessions, heart rate, and weight. The app uses this data to build " +
                    "the JSON payload you preview in the Sync tab and send it to the API endpoint you configure in Settings.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "The app does not write Health Connect records. You can revoke Health Connect access at any time from " +
                    "Android Settings > Security and privacy > Privacy controls > Health Connect > App permissions.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun HealthConnectDisclosureTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF65D46E),
            secondary = Color(0xFF8AB4F8),
            background = Color(0xFF05070A),
            surface = Color(0xFF05070A),
            onPrimary = Color(0xFF031006),
            onBackground = Color(0xFFE8EAED),
            onSurface = Color(0xFFE8EAED),
        ),
        content = content,
    )
}
