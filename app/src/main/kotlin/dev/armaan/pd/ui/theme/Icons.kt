package dev.armaan.pd.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Material icon outlines, drawn in place: A3 allows no icon library. 24 dp grid. */
object PdIcons {
    val Check = icon("M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")
    val Add = icon("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    val Back = icon("M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z")
    val Chevron = icon("M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z")
    val Delete = icon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z")

    private fun icon(path: String) = ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(addPathNodes(path), fill = SolidColor(Color.Black)).build()
}
