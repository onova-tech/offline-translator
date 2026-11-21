package tech.onova.offlinetranslator

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import tech.onova.offlinetranslator.core.designsystem.theme.PocketTheme
import tech.onova.offlinetranslator.core.domain.ports.AppSettingsRepository
import tech.onova.offlinetranslator.core.domain.settings.AppSettings
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import tech.onova.offlinetranslator.navigation.AppNavHost

/** AppCompatActivity so the per-app language API can recreate it on change (spec 001 research R5). */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { OfflineTranslatorApp() }
    }
}

@Composable
fun OfflineTranslatorApp(modifier: Modifier = Modifier) {
    AppShell(modifier) { AppNavHost() }
}

/** What every screen sits in: the stored theme and the system bars. */
@Composable
fun AppShell(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val repository: AppSettingsRepository = koinInject()
    val settings: AppSettings? by repository.settings.collectAsState(initial = null)
    // Wait for the stored theme so the first frame never flashes the wrong one.
    val loaded = settings ?: return
    val darkTheme =
        when (loaded.theme) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    // The bar icons follow the chosen theme, not the phone's: a forced Light or Dark must stay readable.
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, darkTheme) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { }
    }
    PocketTheme(darkTheme = darkTheme, dynamicColor = loaded.dynamicColor) {
        Box(modifier, content = content)
    }
}
