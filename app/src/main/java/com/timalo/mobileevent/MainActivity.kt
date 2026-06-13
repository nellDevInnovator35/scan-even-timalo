package com.timalo.mobileevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.timalo.mobileevent.navigation.AppNavigation
import com.timalo.mobileevent.ui.theme.MobileEventTheme

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = AppContainer(this)
        val factory = AppViewModelFactory(container)

        setContent {
            MobileEventTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Token initial : décide de la route de départ
                    val token by container.prefs.tokenFlow.collectAsState(initial = null)
                    AppNavigation(
                        factory = factory,
                        prefs = container.prefs,
                        initialToken = token
                    )
                }
            }
        }
    }
}
