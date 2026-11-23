package tech.onova.offlinetranslator.core.designsystem.components

import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Before the window's first frame is drawn: the dialog theme's dim and grow-in animation go, and the window takes the
 * page's colour instead of staying transparent until Compose has drawn (`FullScreenPage`).
 */
@Composable
internal actual fun PlainPageWindow(background: Color) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val colour = background.toArgb()
    SideEffect {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setWindowAnimations(0)
        window.setBackgroundDrawable(ColorDrawable(colour))
    }
}
