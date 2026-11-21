package tech.onova.offlinetranslator.core.designsystem.components

/** One point of a live graph: [position] runs from 0 (the oldest edge) to 1 (now). */
public data class GraphPoint(
    val position: Float,
    val value: Float,
)
