package tech.onova.offlinetranslator.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import tech.onova.offlinetranslator.core.domain.ports.PrivacyChoices
import tech.onova.offlinetranslator.core.domain.ports.PrivacyChoicesResult
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.AppSettings
import tech.onova.offlinetranslator.core.domain.settings.DateFormatChoice
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import tech.onova.offlinetranslator.core.domain.settings.TimeFormatChoice
import tech.onova.offlinetranslator.core.domain.settings.UnitSystem
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

    private class FakePrivacyChoices : PrivacyChoices {
        override val required = MutableStateFlow(false)
        var result = PrivacyChoicesResult.OPENED
        var opened = 0

        override suspend fun open(): PrivacyChoicesResult {
            opened++
            return result
        }
    }

    private val settings = FakeSettings()
    private val language = FakeLanguage()
    private val privacyChoices = FakePrivacyChoices()

    private fun viewModel(
        dynamicColor: Boolean = true,
        region: String = "BR",
    ) = SettingsViewModel(
        appSettings = settings,
        languageController = language,
        capabilities = FakeCapabilities(dynamicColor),
        regionProvider = { region },
        appInfo = AppInfo(versionName = "1.2.3"),
        privacyChoices = privacyChoices,
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
            assertEquals(UnitSystem.METRIC, state.unitSystem)
            assertEquals("1.2.3", state.versionName)
        }

    @Test
    fun dynamicColorOptionOnlyWhenSupported() =
        runTest(dispatcher) {
            assertTrue(loadedState(viewModel(dynamicColor = true)).dynamicColorAvailable)
            assertFalse(loadedState(viewModel(dynamicColor = false)).dynamicColorAvailable)
        }

    @Test
    fun unitDefaultFollowsRegionUntilChosen() =
        runTest(dispatcher) {
            val vm = viewModel(region = "US")
            assertEquals(UnitSystem.IMPERIAL, loadedState(vm).unitSystem)
            vm.setUnitSystem(UnitSystem.METRIC)
            advanceUntilIdle()
            assertEquals(UnitSystem.METRIC, vm.uiState.value.unitSystem)
            assertEquals(UnitSystem.METRIC, settings.settings.value.unitSystem)
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

    @Test
    fun privacyChoicesAreShownOnlyWhenRequired() =
        runTest(dispatcher) {
            val vm = viewModel()
            assertFalse(loadedState(vm).privacyChoicesVisible)
            privacyChoices.required.value = true
            advanceUntilIdle()
            assertTrue(vm.uiState.value.privacyChoicesVisible)
        }

    @Test
    fun privacyChoicesThatCannotOpenProduceAMessage() =
        runTest(dispatcher) {
            val vm = viewModel()
            privacyChoices.result = PrivacyChoicesResult.UNAVAILABLE
            vm.openPrivacyChoices()
            assertEquals(SettingsMessage.PrivacyChoicesUnavailable, vm.messages.first())
            assertEquals(1, privacyChoices.opened)
        }

    @Test
    fun privacyChoicesThatOpenProduceNoMessage() =
        runTest(dispatcher) {
            val vm = viewModel()
            val messages = mutableListOf<SettingsMessage>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { messages += it } }
            vm.openPrivacyChoices()
            advanceUntilIdle()
            assertEquals(1, privacyChoices.opened)
            assertEquals(emptyList(), messages)
        }

    @Test
    fun dateAndTimeFormatsAreSavedAndShown() =
        runTest(dispatcher) {
            val vm = viewModel()
            assertEquals(DateFormatChoice.FOLLOW_LANGUAGE, loadedState(vm).dateFormat)
            assertEquals(TimeFormatChoice.FOLLOW_DEVICE, loadedState(vm).timeFormat)
            vm.setDateFormat(DateFormatChoice.YEAR_MONTH_DAY)
            vm.setTimeFormat(TimeFormatChoice.TWENTY_FOUR_HOUR)
            advanceUntilIdle()
            assertEquals(DateFormatChoice.YEAR_MONTH_DAY, settings.settings.value.dateFormat)
            assertEquals(TimeFormatChoice.TWENTY_FOUR_HOUR, loadedState(vm).timeFormat)
        }
}
