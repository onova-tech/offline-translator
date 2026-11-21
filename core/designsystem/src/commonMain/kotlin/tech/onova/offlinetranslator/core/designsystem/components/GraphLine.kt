package tech.onova.offlinetranslator.core.designsystem.components

/**
 * One line of a [SeriesGraph]: its [points], placed from 0 to 1 across, and its [name] for the legend. A [compared]
 * line, such as a scenario kept to compare with, is drawn dashed in the tertiary colour.
 */
public class GraphLine(
    public val points: List<GraphPoint>,
    public val name: String,
    public val compared: Boolean = false,
)
