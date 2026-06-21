package ua.constitution

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import ua.constitution.data.database.DatabaseProvider
import ua.constitution.data.source.ConstitutionLoader
import ua.constitution.data.repository.ConstitutionRepository
import ua.constitution.ui.theme.MyApplicationTheme
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

        // Load official formatted articles from JSON assets into an immutable content store.
        val content = ConstitutionLoader.load(this)

        // content implements all four segregated read interfaces (ISP), so it is passed for each.
        val factory = ConstitutionViewModelFactory(repository, content, content, content, content)
        val viewModel = ViewModelProvider(this, factory)[ConstitutionViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppDashboard(viewModel = viewModel)
            }
        }
    }
}
