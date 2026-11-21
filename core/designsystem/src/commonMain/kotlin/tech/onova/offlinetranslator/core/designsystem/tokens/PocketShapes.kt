package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii from the mockups: 10 swatch, 18 tile, 24 card, 28 sheet, full button. */
public object PocketShapes {
    public val swatch: Shape = RoundedCornerShape(10.dp)
    public val tile: Shape = RoundedCornerShape(18.dp)
    public val card: Shape = RoundedCornerShape(24.dp)
    public val sheet: Shape = RoundedCornerShape(28.dp)
    public val pill: Shape = CircleShape

    public val material: Shapes =
        Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        )
}
