package com.example.proxmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.proxmark.persistence.ThemeMode
import com.example.proxmark.ui.ProxmarkApp
import com.example.proxmark.ui.theme.ProxmarkTheme
import com.example.proxmark.viewmodel.MainViewModel
import com.example.proxmark.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val appContainer = (application as ProxmarkApplication).container
        MainViewModelFactory(appContainer.repository)
    }

    private val nfcReader by lazy {
        (application as ProxmarkApplication).container.builtInNfcReader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode = uiState.settings.themeMode
            ProxmarkTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> null
                }
            ) {
                ProxmarkApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcReader.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        nfcReader.disableForegroundDispatch(this)
    }
}
