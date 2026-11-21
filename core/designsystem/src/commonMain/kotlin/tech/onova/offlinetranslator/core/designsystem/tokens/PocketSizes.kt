package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Component sizes. Interactive elements never go below [touchTarget] (Principle X). The sizes with a getter follow
 * the window (spec 015): exactly the larger value from phone size up, and towards the smaller one in a small window.
 * They can be read only in composition.
 */
public object PocketSizes {
    internal val GRAPH_MIN_HEIGHT = FluidDp(32.dp, 56.dp)
    internal val PREVIEW_MIN_HEIGHT = FluidDp(64.dp, 96.dp)
    internal val CONTENT_MIN_HEIGHT = FluidDp(64.dp, 96.dp)
    internal val GAUGE_MIN = FluidDp(48.dp, 64.dp)
    internal val DIAL_MIN = FluidDp(72.dp, 120.dp)
    internal val PROGRESS_RING_MIN = FluidDp(64.dp, 96.dp)
    internal val FAVORITE_CARD_HEIGHT = FluidDp(72.dp, 108.dp)
    internal val LARGE_BUTTON_HEIGHT = FluidDp(56.dp, 96.dp)
    internal val TOUCH_TARGET = FluidDp(36.dp, 48.dp)
    internal val TIME_FIELD_WIDTH = FluidDp(84.dp, 112.dp)
    internal val TEXT_FIELD_HEIGHT = FluidDp(36.dp, 56.dp)
    internal val BUTTON_HEIGHT = FluidDp(36.dp, 56.dp)
    internal val KEYPAD_KEY_MIN_HEIGHT = FluidDp(36.dp, 56.dp)
    internal val CALCULATOR_DISPLAY_MIN_HEIGHT = FluidDp(56.dp, 112.dp)
    internal val TIMER_FORM_RESERVED_HEIGHT = FluidDp(240.dp, 320.dp)
    internal val TOP_BAR_HEIGHT = FluidDp(40.dp, 64.dp)

    /** The top bar's height, which grows with a title of two lines. */
    public val topBarHeight: Dp
        @Composable @ReadOnlyComposable
        get() = TOP_BAR_HEIGHT.control()

    /** A launcher's adaptive icon: its whole layer, and the tool's glyph inside the 66 dp every launcher shows. */
    public val shortcutIcon: Dp = 108.dp
    public val shortcutGlyph: Dp = 56.dp

    /** What a Material chip adds to its label's width: its padding on both sides. */
    public val chipChrome: Dp = 32.dp

    /**
     * The least a control is touched at (Principle X): 48 dp from phone size up and always with a larger text setting,
     * and down to 36 dp in a reduced window, so the window can show its content without scrolling.
     */
    public val touchTarget: Dp
        @Composable @ReadOnlyComposable
        get() = touchTargetAt(LocalWindowFraction.current, LocalDensity.current.fontScale)

    /** A window at least this tall shows a tool's main reading at the largest size (spec 018). */
    public val mainReadingRoomyHeight: Dp = 700.dp

    /** The dashes of a line across a chart. */
    public val chartDash: Dp = 4.dp

    /**
     * The Conversation's microphone (spec 012): one and a half touch targets, 72 dp from phone size up, smaller in a
     * reduced window so both halves keep their turns.
     */
    public val microphoneButton: Dp
        @Composable @ReadOnlyComposable
        get() = touchTarget * MICROPHONE_SHARE

    /**
     * The least height of one of `FacingHalves`' halves: its microphone, a touch target's height of turns (a scroller
     * is never shorter, L8) and the half's margins. In a window too short for two, the halves scroll instead of
     * squeezing.
     */
    public val facingHalfLeastHeight: Dp
        @Composable @ReadOnlyComposable
        get() = microphoneButton + touchTarget + FACING_HALF_MARGINS

    /** What Material draws an icon button at, inside its touch target. */
    public val iconButton: Dp = 40.dp

    /** Give this to a Material icon button: it stays at 40 dp whatever the target, unless told otherwise. */
    public val iconButtonSize: Modifier
        @Composable @ReadOnlyComposable
        get() = touchTarget.let { if (it < iconButton) Modifier.size(it) else Modifier }

    /** A text field's outline: Material's height from phone size up and with a larger text setting. */
    public val textFieldHeight: Dp
        @Composable @ReadOnlyComposable
        get() =
            if (LocalDensity.current.fontScale > 1f) {
                TEXT_FIELD_HEIGHT.max
            } else {
                TEXT_FIELD_HEIGHT.at(controlFraction(LocalWindowFraction.current))
            }

    /** A field that holds a time such as 01:30:00, beside a name that takes the rest of the row. */
    public val timeFieldWidth: Dp
        @Composable @ReadOnlyComposable
        get() = TIME_FIELD_WIDTH.at(controlFraction(LocalWindowFraction.current))

    public val icon: Dp = 24.dp

    /** The ▾ of a small option button that opens a list ([MenuChoice], spec 070). */
    public val menuArrow: Dp = 18.dp

    /** A row of plain text in a list that is not touched, such as the BMI scale; a control uses [touchTarget]. */
    public val textRow: Dp = 32.dp
    public val toolIcon: Dp = 26.dp
    public val favoriteIcon: Dp = 28.dp
    public val toolTile: Dp = 56.dp

    /** A live graph (spec 005 FR-037): its height and the width of its line. */
    public val graphHeight: Dp = 160.dp

    /** The least a graph is squeezed to before its screen scrolls (see FitColumn). */
    public val graphMinHeight: Dp
        @Composable @ReadOnlyComposable
        get() = GRAPH_MIN_HEIGHT.at(LocalWindowFraction.current)

    /** The least a camera view is squeezed to before its screen scrolls (see FitColumn). */
    public val previewMinHeight: Dp
        @Composable @ReadOnlyComposable
        get() = PREVIEW_MIN_HEIGHT.at(LocalWindowFraction.current)

    /** A color sample shown beside its name and codes. */
    public val swatch: Dp = 72.dp

    /** A color's small square at the start of a history row (spec 044). */
    public val rowSwatch: Dp = 40.dp

    /** The chosen color's square inside a field, beside the field's arrow (spec 078). */
    public val fieldSwatch: Dp = 24.dp

    /** The ring around the chosen square of a color grid: it marks the choice by shape, not by color (spec 078). */
    public val swatchRing: Dp = 3.dp

    /** The thin outline every square of a color grid has, so white on white still shows (spec 078). */
    public val swatchOutline: Dp = 1.dp

    /** A window at least this wide, and wider than tall, puts a tool's controls beside its content. */
    public val twoPaneMinWidth: Dp = 560.dp

    /** The least height a tool's content keeps when its bottom controls are tall; they scroll instead. */
    public val contentMinHeight: Dp
        @Composable @ReadOnlyComposable
        get() = CONTENT_MIN_HEIGHT.at(LocalWindowFraction.current)

    /** The most a graph grows to on a tall screen (see FitColumn). */
    public val graphMaxHeight: Dp = 240.dp
    public val graphStroke: Dp = 2.dp

    /**
     * The least of a graph that gives all its room away rather than make its screen scroll, as the Financial
     * calculator's, whose values fold away to give it room (spec 071). Too low for its labels, it isn't drawn (072).
     */
    public val graphLeastHeight: Dp = 0.dp

    /** A compared line's dashes and gaps ([SeriesGraph], spec 071). */
    public val graphDash: Dp = 6.dp

    /** A graph's mark (spec 072): its tick on the x axis and its dot on each line. */
    public val graphTick: Dp = 6.dp
    public val graphDot: Dp = 3.dp

    /** The sample of a line beside its name in a graph's legend. */
    public val legendSample: Dp = 24.dp

    /** The tuner's needle dial (spec 007 US2) and the metronome's beat dots, at most this wide (US3). */
    public val gaugeHeight: Dp = 120.dp

    /** The least and the most a needle gauge is flexed to (see FitColumn). */
    public val gaugeMin: Dp
        @Composable @ReadOnlyComposable
        get() = GAUGE_MIN.at(LocalWindowFraction.current)
    public val gaugeMax: Dp = 200.dp
    public val beatDot: Dp = 24.dp

    /** The Bubble level's dial (spec 005 US1). */
    public val levelDial: Dp = 200.dp

    /** A dial (Level, Protractor, Compass, Plumb bob) grows from the first to the second with the space it gets. */
    public val dialMin: Dp
        @Composable @ReadOnlyComposable
        get() = DIAL_MIN.at(LocalWindowFraction.current)
    public val dialMax: Dp = 480.dp

    /** Battery info's drawings (spec 009 US1): the battery, and the thermometer beside it as tall. */
    public val batteryWidth: Dp = 64.dp
    public val batteryHeight: Dp = 120.dp
    public val thermometerWidth: Dp = 36.dp

    /** The Distance meter's crosshair (spec 009 US3) and the Heart rate progress ring (US4). */
    public val crosshair: Dp = 56.dp

    /** The least a progress ring is squeezed to before its screen scrolls (see FitColumn). */
    public val progressRingMin: Dp
        @Composable @ReadOnlyComposable
        get() = PROGRESS_RING_MIN.at(LocalWindowFraction.current)

    public val progressRing: Dp = 160.dp
    public val progressRingStroke: Dp = 8.dp
    public val toolTileMinWidth: Dp = 84.dp
    public val favoriteCardHeight: Dp
        @Composable @ReadOnlyComposable
        get() = FAVORITE_CARD_HEIGHT.at(LocalWindowFraction.current)
    public val favoriteCardMinWidth: Dp = 104.dp
    public val emptyStateIcon: Dp = 40.dp

    /** The dot of a list item in a tool's help. */
    public val helpItemDot: Dp = 6.dp
    public val largeButtonHeight: Dp
        @Composable @ReadOnlyComposable
        get() = LARGE_BUTTON_HEIGHT.at(LocalWindowFraction.current)
    public val buttonHeight: Dp
        @Composable @ReadOnlyComposable
        get() = BUTTON_HEIGHT.control()
    public val contentMaxWidth: Dp = 720.dp
    public val sliderThumbWidth: Dp = 4.dp

    /** Calculator-style keys (spec 004): comfortably above the touch target from phone size up. */
    public val keypadKeyMinHeight: Dp
        @Composable @ReadOnlyComposable
        get() = KEYPAD_KEY_MIN_HEIGHT.control()

    /** The tallest a keypad key grows on a large screen (spec 004 FR-044). */
    public val keypadKeyMaxHeight: Dp = 80.dp

    /** Room kept above a calculator keypad for the expression and result. */
    public val calculatorDisplayMinHeight: Dp
        @Composable @ReadOnlyComposable
        get() = CALCULATOR_DISPLAY_MIN_HEIGHT.at(LocalWindowFraction.current)

    /** What the new-timer form needs besides its number pad: title, display, name and buttons. */
    public val timerFormReservedHeight: Dp
        @Composable @ReadOnlyComposable
        get() = TIMER_FORM_RESERVED_HEIGHT.at(LocalWindowFraction.current)

    /** Ruler graduations and markers. */
    public val rulerTickMinor: Dp = 10.dp
    public val rulerTickHalf: Dp = 16.dp
    public val rulerTickMajor: Dp = 26.dp
    public val rulerStroke: Dp = 1.dp
    public val rulerMarkerStroke: Dp = 2.dp
    public val rulerMarkerHandle: Dp = 48.dp
    public val rulerLabelGap: Dp = 4.dp

    internal fun touchTargetAt(
        fraction: Float,
        fontScale: Float,
    ): Dp = if (fontScale > 1f) TOUCH_TARGET.max else TOUCH_TARGET.at(controlFraction(fraction))

    /** Controls give way before the text does: they reach their smallest halfway down to the smallest window. */
    internal fun controlFraction(fraction: Float): Float =
        ((fraction - CONTROLS_SMALLEST_AT) / (1f - CONTROLS_SMALLEST_AT)).coerceAtLeast(0f)

    /** A size that is touched: it follows the window, and never goes under this window's [touchTarget]. */
    @Composable
    @ReadOnlyComposable
    private fun FluidDp.control(): Dp = maxOf(touchTarget, at(controlFraction(LocalWindowFraction.current)))

    private const val CONTROLS_SMALLEST_AT = 0.5f

    private const val MICROPHONE_SHARE = 1.5f

    /** A half's margins (2 × xs) and the space above its microphone row (xs), with a little to spare. */
    private val FACING_HALF_MARGINS = 32.dp
}
