package tech.onova.offlinetranslator.core.domain.ports

/** Keeps the screen on while a tool that is watched continuously is open. */
public interface ScreenAwake {
    public fun keepOn(on: Boolean)
}
