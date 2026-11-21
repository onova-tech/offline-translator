package tech.onova.offlinetranslator.core.designsystem.components

/**
 * A last segment of a [SegmentedChoice] that opens a list instead of choosing at once (owner, 2026-11-13, the Tuner's
 * instruments: "( Cromático | Cordas ▾ )"). It reads the chosen one of [options] with a small ▾, or [label] while none
 * of them is chosen; screen readers hear [label] as its name.
 */
public class ChoiceMenu<K>(
    public val label: String,
    public val options: List<ChoiceOption<K>>,
)
