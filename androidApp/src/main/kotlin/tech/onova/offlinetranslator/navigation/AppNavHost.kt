package tech.onova.offlinetranslator.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import tech.onova.offlinetranslator.R
import tech.onova.offlinetranslator.debug.debugAboutRows
import tech.onova.offlinetranslator.feature.settings.SettingsScreen
import tech.onova.offlinetranslator.feature.settings.licenses.LicensesScreen
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.OfflineLanguagesSection
import tech.onova.offlinetranslator.tool.ToolScreen

/** The Translator's permanent id (spec 012). */
private const val TRANSLATOR = "translator"

/** The Translator, where the app opens (spec 081). */
@Serializable
data object TranslatorKey : NavKey

@Serializable
data object SettingsKey : NavKey

/** Settings → About → Open-source licenses (spec 012 T057). */
@Serializable
data object LicensesKey : NavKey

/** Back stack as saved state; predictive back comes with NavDisplay. The Translator is always at its bottom. */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(TranslatorKey)
    val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    val open: (NavKey) -> Unit = { key -> if (backStack.lastOrNull() != key) backStack.add(key) }
    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = goBack,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<TranslatorKey> {
                    ToolScreen(toolId = TRANSLATOR, onOpenSettings = { open(SettingsKey) })
                }
                entry<SettingsKey> {
                    SettingsScreen(
                        onBack = goBack,
                        offlineLanguages = { sectionModifier -> OfflineLanguagesSection(sectionModifier) },
                        about = debugAboutRows,
                        onOpenLicenses = { open(LicensesKey) },
                    )
                }
                entry<LicensesKey> {
                    val resources = LocalContext.current.resources
                    LicensesScreen(
                        onBack = goBack,
                        readList = {
                            withContext(Dispatchers.IO) {
                                resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
                            }
                        },
                    )
                }
            },
    )
}
