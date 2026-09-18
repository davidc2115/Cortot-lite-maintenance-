package fr.cortotelite.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF0B1F3A)
private val Gold = Color(0xFFC6A15B)
private val Cream = Color(0xFFF6F1E6)

private val scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Navy,
    background = Navy,
    onBackground = Cream,
    surface = Color(0xFF132846),
    onSurface = Cream,
    secondary = Gold,
    onSecondary = Navy
)

@Composable
fun CortotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
