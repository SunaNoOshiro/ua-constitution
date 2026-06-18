package ua.constitution

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ua.constitution.data.database.ConstitutionDatabase
import ua.constitution.data.model.ConstitutionData
import ua.constitution.data.repository.ConstitutionRepository
import ua.constitution.ui.theme.MyApplicationTheme
import ua.constitution.ui.viewmodel.ConstitutionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load official formatted articles from JSON assets
        ConstitutionData.initialize(this)

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

        val database = ConstitutionDatabase.getDatabase(this)
        val repository = ConstitutionRepository(database.constitutionDao())

        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ConstitutionViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ConstitutionViewModel(
                        repository,
                        ConstitutionData,
                        ConstitutionData,
                        ConstitutionData,
                        ConstitutionData
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        })[ConstitutionViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppDashboard(viewModel = viewModel)
            }
        }
    }
}
