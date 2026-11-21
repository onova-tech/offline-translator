package tech.onova.offlinetranslator.support

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import tech.onova.offlinetranslator.core.domain.ads.DailyUsage
import tech.onova.offlinetranslator.core.domain.catalog.CatalogStateRules
import tech.onova.offlinetranslator.core.domain.catalog.Favorite
import tech.onova.offlinetranslator.core.domain.catalog.HardwareRequirement
import tech.onova.offlinetranslator.core.domain.catalog.RecentUse
import tech.onova.offlinetranslator.core.domain.catalog.ToolId
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionController
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.AcknowledgedNotices
import tech.onova.offlinetranslator.core.domain.ports.AdConsent
import tech.onova.offlinetranslator.core.domain.ports.AdShowOutcome
import tech.onova.offlinetranslator.core.domain.ports.AlertPlayer
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.ports.AppSettingsRepository
import tech.onova.offlinetranslator.core.domain.ports.AppVisibility
import tech.onova.offlinetranslator.core.domain.ports.BarcodeDecoder
import tech.onova.offlinetranslator.core.domain.ports.BarcodeFormat
import tech.onova.offlinetranslator.core.domain.ports.BarcodeResult
import tech.onova.offlinetranslator.core.domain.ports.CameraConfig
import tech.onova.offlinetranslator.core.domain.ports.CameraController
import tech.onova.offlinetranslator.core.domain.ports.CameraFacing
import tech.onova.offlinetranslator.core.domain.ports.CameraFrame
import tech.onova.offlinetranslator.core.domain.ports.CameraSession
import tech.onova.offlinetranslator.core.domain.ports.CameraStatus
import tech.onova.offlinetranslator.core.domain.ports.CatalogStateRepository
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter
import tech.onova.offlinetranslator.core.domain.ports.ContactDraft
import tech.onova.offlinetranslator.core.domain.ports.CueAlarm
import tech.onova.offlinetranslator.core.domain.ports.CueAlarmScheduler
import tech.onova.offlinetranslator.core.domain.ports.CuePlayer
import tech.onova.offlinetranslator.core.domain.ports.CueSound
import tech.onova.offlinetranslator.core.domain.ports.DailyUsageRepository
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities
import tech.onova.offlinetranslator.core.domain.ports.DocumentFiles
import tech.onova.offlinetranslator.core.domain.ports.DocumentRead
import tech.onova.offlinetranslator.core.domain.ports.FirstDayOfWeek
import tech.onova.offlinetranslator.core.domain.ports.ImageSharer
import tech.onova.offlinetranslator.core.domain.ports.InterstitialAds
import tech.onova.offlinetranslator.core.domain.ports.MonotonicClock
import tech.onova.offlinetranslator.core.domain.ports.MonotonicInstant
import tech.onova.offlinetranslator.core.domain.ports.Picture
import tech.onova.offlinetranslator.core.domain.ports.PictureLoader
import tech.onova.offlinetranslator.core.domain.ports.PrivacyChoices
import tech.onova.offlinetranslator.core.domain.ports.PrivacyChoicesResult
import tech.onova.offlinetranslator.core.domain.ports.ProcessVisibility
import tech.onova.offlinetranslator.core.domain.ports.ReminderNotice
import tech.onova.offlinetranslator.core.domain.ports.ReminderNotifier
import tech.onova.offlinetranslator.core.domain.ports.ReminderScheduler
import tech.onova.offlinetranslator.core.domain.ports.RemoveAdsRepository
import tech.onova.offlinetranslator.core.domain.ports.RemoveAdsStore
import tech.onova.offlinetranslator.core.domain.ports.RunningNotice
import tech.onova.offlinetranslator.core.domain.ports.RunningNotifications
import tech.onova.offlinetranslator.core.domain.ports.ScreenAwake
import tech.onova.offlinetranslator.core.domain.ports.ScreenBrightness
import tech.onova.offlinetranslator.core.domain.ports.SeenHints
import tech.onova.offlinetranslator.core.domain.ports.SensorKind
import tech.onova.offlinetranslator.core.domain.ports.SensorSample
import tech.onova.offlinetranslator.core.domain.ports.SensorSource
import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.core.domain.ports.SettingsPages
import tech.onova.offlinetranslator.core.domain.ports.SystemHandoff
import tech.onova.offlinetranslator.core.domain.ports.TextSharer
import tech.onova.offlinetranslator.core.domain.ports.TimeZoneNames
import tech.onova.offlinetranslator.core.domain.ports.TimerAlarm
import tech.onova.offlinetranslator.core.domain.ports.TimerAlarmScheduler
import tech.onova.offlinetranslator.core.domain.ports.ToolSessions
import tech.onova.offlinetranslator.core.domain.ports.TorchController
import tech.onova.offlinetranslator.core.domain.ports.WifiJoinRequest
import tech.onova.offlinetranslator.core.domain.purchase.PurchaseResult
import tech.onova.offlinetranslator.core.domain.purchase.RemoveAdsRecord
import tech.onova.offlinetranslator.core.domain.purchase.StoreAnswer
import tech.onova.offlinetranslator.core.domain.purchase.StorePurchase
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.AppSettings
import tech.onova.offlinetranslator.core.toolapi.CameraPreviewRenderer
import tech.onova.offlinetranslator.core.toolapi.DocumentPicker
import tech.onova.offlinetranslator.core.toolapi.PicturePicker
import tech.onova.offlinetranslator.core.toolapi.PreviewFilter
import tech.onova.offlinetranslator.core.toolapi.PreviewOptions
import tech.onova.offlinetranslator.core.toolapi.ToolShortcuts
import tech.onova.offlinetranslator.feature.toolsbasic.counter.domain.CounterRepository
import tech.onova.offlinetranslator.feature.toolsbasic.counter.domain.CounterState
import tech.onova.offlinetranslator.feature.toolsbasic.ruler.domain.RulerSettings
import tech.onova.offlinetranslator.feature.toolsbasic.ruler.domain.RulerSettingsRepository
import tech.onova.offlinetranslator.feature.toolscalculation.bmi.domain.BmiRepository
import tech.onova.offlinetranslator.feature.toolscalculation.bmi.domain.BmiState
import tech.onova.offlinetranslator.feature.toolscalculation.calculator.domain.CalculatorRepository
import tech.onova.offlinetranslator.feature.toolscalculation.calculator.domain.CalculatorState
import tech.onova.offlinetranslator.feature.toolscalculation.converter.domain.ConverterRepository
import tech.onova.offlinetranslator.feature.toolscalculation.converter.domain.ConverterState
import tech.onova.offlinetranslator.feature.toolscalculation.finance.domain.FinanceRepository
import tech.onova.offlinetranslator.feature.toolscalculation.finance.domain.FinanceState
import tech.onova.offlinetranslator.feature.toolscalculation.password.domain.PasswordOptions
import tech.onova.offlinetranslator.feature.toolscalculation.password.domain.PasswordRepository
import tech.onova.offlinetranslator.feature.toolscalculation.percent.domain.PercentRepository
import tech.onova.offlinetranslator.feature.toolscalculation.percent.domain.PercentState
import tech.onova.offlinetranslator.feature.toolscalculation.random.domain.RandomRepository
import tech.onova.offlinetranslator.feature.toolscalculation.random.domain.RandomState
import tech.onova.offlinetranslator.feature.toolscalculation.shapes.domain.ShapesRepository
import tech.onova.offlinetranslator.feature.toolscalculation.shapes.domain.ShapesState
import tech.onova.offlinetranslator.feature.toolscamera.colorpicker.domain.ColorHistory
import tech.onova.offlinetranslator.feature.toolscamera.colorpicker.domain.ColorHistoryRepository
import tech.onova.offlinetranslator.feature.toolscamera.flashlight.domain.FlashlightRepository
import tech.onova.offlinetranslator.feature.toolscamera.flashlight.domain.FlashlightState
import tech.onova.offlinetranslator.feature.toolscamera.magnifier.domain.MagnifierRepository
import tech.onova.offlinetranslator.feature.toolscamera.magnifier.domain.MagnifierState
import tech.onova.offlinetranslator.feature.toolscamera.nightvision.domain.NightVisionRepository
import tech.onova.offlinetranslator.feature.toolscamera.nightvision.domain.NightVisionState
import tech.onova.offlinetranslator.feature.toolscamera.scanner.domain.ScanHistory
import tech.onova.offlinetranslator.feature.toolscamera.scanner.domain.ScanHistoryRepository
import tech.onova.offlinetranslator.feature.toolscamera.screenlight.domain.ScreenLightRepository
import tech.onova.offlinetranslator.feature.toolscamera.screenlight.domain.ScreenLightState
import tech.onova.offlinetranslator.feature.toolsdevice.nfc.domain.NfcHistory
import tech.onova.offlinetranslator.feature.toolsdevice.nfc.domain.NfcHistoryRepository
import tech.onova.offlinetranslator.feature.toolsgps.places.domain.PlacesRepository
import tech.onova.offlinetranslator.feature.toolsgps.places.domain.PlacesState
import tech.onova.offlinetranslator.feature.toolsmotion.compass.domain.CompassRepository
import tech.onova.offlinetranslator.feature.toolsmotion.compass.domain.CompassState
import tech.onova.offlinetranslator.feature.toolsmotion.gmeter.domain.GMeterRepository
import tech.onova.offlinetranslator.feature.toolsmotion.gmeter.domain.GMeterState
import tech.onova.offlinetranslator.feature.toolsmotion.level.domain.LevelRepository
import tech.onova.offlinetranslator.feature.toolsmotion.level.domain.LevelState
import tech.onova.offlinetranslator.feature.toolsmotion.metal.domain.MetalDetectorRepository
import tech.onova.offlinetranslator.feature.toolsmotion.metal.domain.MetalDetectorState
import tech.onova.offlinetranslator.feature.toolsmotion.pedometer.domain.PedometerRepository
import tech.onova.offlinetranslator.feature.toolsmotion.pedometer.domain.StepDay
import tech.onova.offlinetranslator.feature.toolsmotion.pedometer.domain.StepLog
import tech.onova.offlinetranslator.feature.toolsmotion.pedometer.domain.StepLogRepository
import tech.onova.offlinetranslator.feature.toolsmotion.protractor.domain.ProtractorRepository
import tech.onova.offlinetranslator.feature.toolsmotion.protractor.domain.ProtractorState
import tech.onova.offlinetranslator.feature.toolsmotion.vibration.domain.VibrationRepository
import tech.onova.offlinetranslator.feature.toolsmotion.vibration.domain.VibrationState
import tech.onova.offlinetranslator.feature.toolsnotes.domain.Note
import tech.onova.offlinetranslator.feature.toolsnotes.domain.NoteRules
import tech.onova.offlinetranslator.feature.toolsnotes.domain.NoteSummary
import tech.onova.offlinetranslator.feature.toolsnotes.domain.NotesRepository
import tech.onova.offlinetranslator.feature.toolstime.clock.domain.WorldClockRepository
import tech.onova.offlinetranslator.feature.toolstime.clock.domain.WorldClockState
import tech.onova.offlinetranslator.feature.toolstime.dates.domain.DateCalcRepository
import tech.onova.offlinetranslator.feature.toolstime.dates.domain.DateCalcState
import tech.onova.offlinetranslator.feature.toolstime.reminders.domain.ReminderRepository
import tech.onova.offlinetranslator.feature.toolstime.reminders.domain.RemindersState
import tech.onova.offlinetranslator.feature.toolstime.timer.domain.IntervalsRepository
import tech.onova.offlinetranslator.feature.toolstime.timer.domain.IntervalsState
import tech.onova.offlinetranslator.feature.toolstime.timer.domain.StopwatchRepository
import tech.onova.offlinetranslator.feature.toolstime.timer.domain.StopwatchSession
import tech.onova.offlinetranslator.feature.toolstime.timer.domain.TimersRepository
import tech.onova.offlinetranslator.feature.toolstime.timer.domain.TimersState
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class InMemoryAppSettings : AppSettingsRepository {
    override val settings = MutableStateFlow(AppSettings())

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.value = transform(settings.value)
    }
}

class FakeCapabilities(
    override val presentHardware: Set<HardwareRequirement> = HardwareRequirement.entries.toSet(),
    override val supportsDynamicColor: Boolean = false,
    override val hasVibrator: Boolean = true,
) : DeviceCapabilities

class FakeLanguage : AppLanguageController {
    var language = AppLanguage.SYSTEM

    override fun current() = language

    override fun apply(language: AppLanguage) {
        this.language = language
    }
}

class FakeClipboard(
    var confirmsCopies: Boolean = false,
) : ClipboardWriter {
    val copied = mutableListOf<String>()

    /** Copies marked sensitive, such as a password (spec 034). */
    val secrets = mutableListOf<String>()

    override fun copy(
        label: String,
        text: String,
    ): Boolean {
        copied += text
        return confirmsCopies
    }

    override fun copySecret(
        label: String,
        text: String,
    ): Boolean {
        secrets += text
        return confirmsCopies
    }
}

class FakeMonotonicClock(
    var millis: Long = 0,
) : MonotonicClock {
    override fun now() = MonotonicInstant(bootId = 1, elapsedMillis = millis)
}

class FakeAppVisibility : ProcessVisibility {
    override val isVisible = MutableStateFlow(true)
}

class FakePermissionController : PermissionController {
    val statuses = mutableMapOf<AppPermission, PermissionStatus>()
    val answers = ArrayDeque<PermissionStatus>()
    val openedSettings = mutableListOf<AppPermission>()

    override fun status(permission: AppPermission) = statuses[permission] ?: PermissionStatus.DENIED

    override suspend fun request(permission: AppPermission): PermissionStatus {
        val answer = answers.removeFirstOrNull() ?: PermissionStatus.DENIED
        statuses[permission] = answer
        return answer
    }

    override fun openSettings(permission: AppPermission) {
        openedSettings += permission
    }
}

class FakeWallClock(
    var instant: Instant,
) : Clock {
    override fun now(): Instant = instant
}

class FakeSettingsPages : SettingsPages {
    val opened = mutableListOf<SettingsPage>()

    override fun open(page: SettingsPage) {
        opened += page
    }
}

class FakeTextSharer : TextSharer {
    var shared: String? = null

    override fun share(text: String) {
        shared = text
    }
}
