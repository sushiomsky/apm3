package com.sushiomsky.proxmark

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sushiomsky.proxmark.persistence.ThemeMode
import com.sushiomsky.proxmark.ui.ProxmarkApp
import com.sushiomsky.proxmark.ui.theme.ProxmarkTheme
import com.sushiomsky.proxmark.viewmodel.MainViewModel
import com.sushiomsky.proxmark.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch

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
        enableEdgeToEdge()

        lifecycleScope.launch {
            viewModel.shareEvent.collect { (content, title) ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, content)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, title))
            }
        }

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
