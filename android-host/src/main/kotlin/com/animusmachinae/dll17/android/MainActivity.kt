package com.animusmachinae.dll17.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.animusmachinae.dll17.core.crypto.CoreCryptoModule
import com.animusmachinae.dll17.core.math.CoreMathModule
import com.animusmachinae.dll17.core.state.CoreStateModule

/**
 * R000 shell activity.
 *
 * It proves the Android project builds, installs and launches, and that the
 * Android layer can read core module identity without the core modules knowing
 * anything about Android. It contains no organism behavior and no presentation
 * contract: nothing here may become a `PresentationContractCatalog` entry
 * without going through that catalog first.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HostShell(HostShellState.r000())
                }
            }
        }
    }
}

/** Immutable description of what the shell is allowed to say in R000. */
data class HostShellState(
    val displayName: String,
    val phase: String,
    val organismState: String,
    val modules: List<String>,
) {
    companion object {
        fun r000(): HostShellState = HostShellState(
            displayName = "Digital Living Lifeform",
            phase = "R000 - greenfield project initialization",
            organismState = "No organism exists yet. Canonical logic is gated on R001.",
            modules = listOf(
                CoreMathModule.ID,
                CoreCryptoModule.ID,
                CoreStateModule.ID,
            ),
        )
    }
}

@Composable
fun HostShell(state: HostShellState) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = state.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(text = state.phase, style = MaterialTheme.typography.bodyMedium)
        Text(text = state.organismState, style = MaterialTheme.typography.bodyMedium)
        state.modules.forEach { module ->
            Text(text = "core module: $module", style = MaterialTheme.typography.bodySmall)
        }
    }
}
