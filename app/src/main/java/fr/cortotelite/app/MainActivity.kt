package fr.cortotelite.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.cortotelite.app.ui.CortotRoot
import fr.cortotelite.app.ui.theme.CortotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as CortotApp).repo
        setContent { CortotTheme { CortotRoot(repo) } }
    }
}
