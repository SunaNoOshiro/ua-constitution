package ua.constitution

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.constitution.data.database.DatabaseProvider
import ua.constitution.data.repository.ConstitutionRepository
import ua.constitution.data.settings.SettingsRepository
import ua.constitution.data.source.ConstitutionLoader
import ua.constitution.ui.screens.MainAppDashboard
import ua.constitution.ui.theme.AppCanvasYellow
import ua.constitution.ui.theme.LocalFontScale
import ua.constitution.ui.theme.MyApplicationTheme
import ua.constitution.ui.theme.SovereignBlue
import ua.constitution.ui.viewmodel.ConstitutionViewModel
import ua.constitution.ui.viewmodel.ConstitutionViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val database = DatabaseProvider.getDatabase(this)
        val repository = ConstitutionRepository(database.constitutionDao())
        val settings = SettingsRepository(this)

        setContent {
            val fontScale by settings.fontScale.collectAsState()
            MyApplicationTheme {
                CompositionLocalProvider(LocalFontScale provides fontScale) {
                    // The constitution JSON (asset read + SHA-256 + org.json parse + sort) is loaded off
                    // the main thread so cold start doesn't block the UI; a lightweight loading state is
                    // shown until the content store is ready. The parse/integrity logic is unchanged.
                    var viewModel by remember { mutableStateOf<ConstitutionViewModel?>(null) }
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        val content = withContext(Dispatchers.IO) { ConstitutionLoader.load(context) }
                        // content implements all four segregated read interfaces (ISP), so it is passed for each.
                        val factory = ConstitutionViewModelFactory(repository, content, content, content, content)
                        viewModel = ViewModelProvider(this@MainActivity, factory)[ConstitutionViewModel::class.java]
                    }

                    when (val vm = viewModel) {
                        null -> LoadingScreen()
                        else -> MainAppDashboard(viewModel = vm, settings = settings)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppCanvasYellow),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = SovereignBlue)
    }
}
