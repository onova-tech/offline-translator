package tech.onova.offlinetranslator.support

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import tech.onova.offlinetranslator.core.domain.catalog.HardwareRequirement
import tech.onova.offlinetranslator.core.domain.permissions.AppPermission
import tech.onova.offlinetranslator.core.domain.permissions.PermissionController
import tech.onova.offlinetranslator.core.domain.permissions.PermissionStatus
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.ports.AppSettingsRepository
import tech.onova.offlinetranslator.core.domain.ports.ClipboardWriter
import tech.onova.offlinetranslator.core.domain.ports.DeviceCapabilities
import tech.onova.offlinetranslator.core.domain.ports.MonotonicClock
import tech.onova.offlinetranslator.core.domain.ports.MonotonicInstant
import tech.onova.offlinetranslator.core.domain.ports.ProcessVisibility
import tech.onova.offlinetranslator.core.domain.ports.ScreenAwake
import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.core.domain.ports.SettingsPages
import tech.onova.offlinetranslator.core.domain.ports.TextSharer
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage
import tech.onova.offlinetranslator.core.domain.settings.AppSettings

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

class FakeScreenAwake : ScreenAwake {
    var keptOn = false

    override fun keepOn(on: Boolean) {
        keptOn = on
    }
}
