package co.farmpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import co.farmpulse.app.ui.theme.FarmPulseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FarmPulseTheme {
                EntryScreen()
            }
        }
    }
}

@Composable
fun EntryScreen() {
    Text(text = "FarmPulse — loading...")
}

