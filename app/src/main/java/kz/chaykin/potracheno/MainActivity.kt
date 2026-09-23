package kz.chaykin.potracheno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kz.chaykin.potracheno.ui.PotrachenoRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as PotrachenoApp).container
        setContent {
            PotrachenoRoot(settingsStore = container.settingsStore)
        }
    }
}
