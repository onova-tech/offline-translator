package tech.onova.offlinetranslator.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tech.onova.offlinetranslator.core.domain.catalog.HardwareRequirement
import tech.onova.offlinetranslator.core.domain.ports.AppInfo
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.ports.AppSettingsRepository
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.AppSettings
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeSettings : AppSettingsRepository {
        override val settings = MutableStateFlow(AppSettings())

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }

    private class FakeLanguage : AppLanguageController {
        var language = AppLanguage.SYSTEM

        override fun current() = language

        override fun apply(language: AppLanguage) {
            this.language = language
        }
    }

    private class FakeCapabilities(
        override val supportsDynamicColor: Boolean,
    ) : DeviceCapabilities {
        override val presentHardware: Set<HardwareRequirement> = emptySet()
        override val hasVibrator = false
    }

    private val settings = FakeSettings()
    private val language = FakeLanguage()

    private fun viewModel(dynamicColor: Boolean = true) =
        SettingsViewModel(
            appSettings = settings,
            languageController = language,
            capabilities = FakeCapabilities(dynamicColor),
            appInfo = AppInfo(versionName = "1.2.3"),
        )

    private fun TestScope.loadedState(vm: SettingsViewModel): SettingsUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        advanceUntilIdle()
        return vm.uiState.value
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun exposesDefaultsAndVersion() =
        runTest(dispatcher) {
            val state = loadedState(viewModel())
            assertEquals(ThemeMode.SYSTEM, state.theme)
            assertTrue(state.dynamicColor)
            assertEquals(AppLanguage.SYSTEM, state.language)
            assertEquals("1.2.3", state.versionName)
        }

    @Test
    fun dynamicColorOptionOnlyWhenSupported() =
        runTest(dispatcher) {
            assertTrue(loadedState(viewModel(dynamicColor = true)).dynamicColorAvailable)
            assertFalse(loadedState(viewModel(dynamicColor = false)).dynamicColorAvailable)
        }

    @Test
    fun themeAndDynamicColorArePersisted() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.setTheme(ThemeMode.DARK)
            vm.setDynamicColor(false)
            advanceUntilIdle()
            assertEquals(AppSettings(theme = ThemeMode.DARK, dynamicColor = false), settings.settings.value)
        }

    @Test
    fun languageIsAppliedThroughThePlatform() =
        runTest(dispatcher) {
            val vm = viewModel()
            loadedState(vm)
            vm.setLanguage(AppLanguage.PT_BR)
            advanceUntilIdle()
            assertEquals(AppLanguage.PT_BR, language.language)
            assertEquals(AppLanguage.PT_BR, vm.uiState.value.language)
        }
}
