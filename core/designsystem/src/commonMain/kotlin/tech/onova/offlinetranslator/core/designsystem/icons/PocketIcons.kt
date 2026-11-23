package tech.onova.offlinetranslator.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 24f
private const val STROKE_WIDTH = 2f

/**
 * Builds a single-color 24×24 line icon from SVG path data. Tool packs use this so their icons
 * share one visual language and never carry raw sizes (Principle VII).
 */
public fun pocketIcon(
    name: String,
    pathData: String,
    filled: Boolean = false,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT,
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = if (filled) null else SolidColor(Color.Black),
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

/** UI icons shared by every screen. */
public object PocketIcons {
    public val MoreVertical: ImageVector = pocketIcon("MoreVertical", "M12 5h.01 M12 12h.01 M12 19h.01")
    public val Pin: ImageVector = pocketIcon("Pin", "M12 17v5 M9 3h6l-1 7 4 4v3H6v-3l4-4z")
    public val Share: ImageVector = pocketIcon("Share", "M4 12v8h16v-8 M12 3v13 M8 7l4-4 4 4")
    public val Back: ImageVector = pocketIcon("Back", "M19 12H5 M11 6l-6 6 6 6")
    public val Settings: ImageVector =
        pocketIcon(
            "Settings",
            "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73" +
                "l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73" +
                "l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44" +
                "a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39" +
                "a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73" +
                "l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" +
                " M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0",
        )
    public val Search: ImageVector = pocketIcon("Search", "M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0 M16.5 16.5 21 21")
    public val Star: ImageVector =
        pocketIcon(
            "Star",
            "M12 3l2.8 5.7 6.2.9-4.5 4.4 1 6.2L12 17.3l-5.5 2.9 1-6.2L3 9.6l6.2-.9z",
            filled = true,
        )
    public val StarOutline: ImageVector =
        pocketIcon("StarOutline", "M12 3l2.8 5.7 6.2.9-4.5 4.4 1 6.2L12 17.3l-5.5 2.9 1-6.2L3 9.6l6.2-.9z")
    public val Info: ImageVector = pocketIcon("Info", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 11v5 M12 8h.01")
    public val Check: ImageVector = pocketIcon("Check", "M5 12.5l4.5 4.5L19 7.5")
    public val ChevronRight: ImageVector = pocketIcon("ChevronRight", "M9 6l6 6-6 6")
    public val ChevronLeft: ImageVector = pocketIcon("ChevronLeft", "M15 6l-6 6 6 6")
    public val ChevronDown: ImageVector = pocketIcon("ChevronDown", "M6 9l6 6 6-6")
    public val ChevronUp: ImageVector = pocketIcon("ChevronUp", "M6 15l6-6 6 6")
    public val Close: ImageVector = pocketIcon("Close", "M6 6l12 12 M18 6L6 18")

    /** All there and working: a check in a circle (spec 080, Manage languages). */
    public val Complete: ImageVector =
        pocketIcon("Complete", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M8 12.5l2.7 2.7L16.2 9.5")

    /** Only in part: a warning triangle (spec 080, Manage languages). */
    public val Partial: ImageVector = pocketIcon("Partial", "M12 3.5L2.5 20h19z M12 10v4.5 M12 17.2h.01")
    public val Edit: ImageVector = pocketIcon("Edit", "M4 20h4L19 9l-4-4L4 16z M14 6l4 4")
    public val Play: ImageVector = pocketIcon("Play", "M8 5l11 7-11 7z")
    public val SkipNext: ImageVector = pocketIcon("SkipNext", "M6 5l10 7-10 7z M19 5v14")
    public val SkipPrevious: ImageVector = pocketIcon("SkipPrevious", "M18 5L8 12l10 7z M5 5v14")
    public val Plus: ImageVector = pocketIcon("Plus", "M12 5v14 M5 12h14")
    public val Minus: ImageVector = pocketIcon("Minus", "M5 12h14")
    public val Reset: ImageVector = pocketIcon("Reset", "M4 12a8 8 0 1 0 2.4-5.7 M4 4v4h4")
    public val Vibrate: ImageVector = pocketIcon("Vibrate", "M8 4h8v16H8z M4 8v8 M20 8v8")
    public val Calibrate: ImageVector =
        pocketIcon("Calibrate", "M12 2v4 M12 18v4 M2 12h4 M18 12h4 M7 12a5 5 0 1 0 10 0a5 5 0 1 0-10 0")
    public val ArrowUp: ImageVector = pocketIcon("ArrowUp", "M12 19V5 M6 11l6-6 6 6")
    public val ArrowDown: ImageVector = pocketIcon("ArrowDown", "M12 5v14 M6 13l6 6 6-6")
    public val Swap: ImageVector = pocketIcon("Swap", "M20 8H4 M8 4L4 8l4 4 M4 16h16 M16 12l4 4-4 4")
    public val Delete: ImageVector =
        pocketIcon("Delete", "M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v6 M14 11v6")
    public val Backspace: ImageVector = pocketIcon("Backspace", "M21 5H9l-6 7 6 7h12z M12 9l6 6 M18 9l-6 6")
    public val Copy: ImageVector = pocketIcon("Copy", "M9 9h11v11H9z M5 15H4V4h11v1")

    /** Lucide's "mic": speak instead of typing (spec 012). */
    public val Microphone: ImageVector =
        pocketIcon(
            "Microphone",
            "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z M19 10v2a7 7 0 0 1-14 0v-2 M12 19v3",
        )

    /** Lucide's "volume-2": hear a translated turn again (spec 082, the Conversation; owner, 2026-11-22). */
    public val Speaker: ImageVector =
        pocketIcon(
            "Speaker",
            "M11 5L6 9H2v6h4l5 4z M15.5 8.5a5 5 0 0 1 0 7 M19 5a10 10 0 0 1 0 14",
        )

    /** A square: stop, and drop what was being heard (spec 012, the Conversation's halves; owner, 2026-11-19). */
    public val Stop: ImageVector = pocketIcon("Stop", "M6 6h12v12H6z")

    /** A tray with an arrow coming down: save as a file (spec 062), beside Share's arrow going up. */
    public val Save: ImageVector = pocketIcon("Save", "M4 14v6h16v-6 M12 3v12 M8 11l4 4 4-4")

    /** "Aa": change the case of a text (spec 069, the Notepad's format menu). */
    public val TextCase: ImageVector =
        pocketIcon("TextCase", "M2 19 6.5 6 11 19 M3.6 14.5h5.8 M20 13v6 M14 16a3 3 0 1 0 6 0a3 3 0 1 0-6 0")

    /** ▾: a small option button that opens a list, as Area & volume's ( mm ▾ ) (spec 070). */
    public val MenuArrow: ImageVector = pocketIcon("MenuArrow", "M6 9h12l-6 7z", filled = true)

    /** A house with a plus: add this tool to the home screen. */
    public val AddToHome: ImageVector =
        pocketIcon("AddToHome", "M3 11.5 12 4l9 7.5 M5.5 9.5V20h6.5 M18 14v6 M15 17h6")

    /** A zero and a one, for the Programmer calculator's bit grid. */
    public val Bits: ImageVector =
        pocketIcon("Bits", "M8 6c-2 0-3 2.7-3 6s1 6 3 6 3-2.7 3-6-1-6-3-6z M15 8.5 18 6v12")

    /** Three rows of three keys, to go back from the bit grid to the keys. */
    public val Keypad: ImageVector =
        pocketIcon(
            "Keypad",
            "M4 4h4v4H4z M10 4h4v4h-4z M16 4h4v4h-4z M4 10h4v4H4z M10 10h4v4h-4z M16 10h4v4h-4z " +
                "M4 16h4v4H4z M10 16h4v4h-4z M16 16h4v4h-4z",
        )
    public val History: ImageVector = pocketIcon("History", "M3 12a9 9 0 1 0 3-6.7 M3 4v5h5 M12 7v5l3 3")
    public val Picture: ImageVector =
        pocketIcon("Picture", "M4 5h16v14H4z M4 16l5-5 4 4 2-2 5 5 M15.5 9h.01")
    public val Unavailable: ImageVector =
        pocketIcon("Unavailable", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M5.6 5.6l12.8 12.8")
}
