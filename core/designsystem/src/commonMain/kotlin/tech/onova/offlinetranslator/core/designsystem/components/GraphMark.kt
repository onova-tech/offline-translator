package tech.onova.offlinetranslator.core.designsystem.components

/**
 * A point a [SeriesGraph] marks (spec 072): where it is across, from 0 to 1, what it is ("18 meses"), and each line's
 * value there, in the order of the graph's lines.
 */
public class GraphMark(
    public val position: Float,
    public val label: String,
    public val values: List<String>,
)
