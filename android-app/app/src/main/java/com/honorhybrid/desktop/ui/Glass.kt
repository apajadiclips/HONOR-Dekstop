package com.honorhybrid.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * "Liquid Glass" surface: a dark translucent tint so white text stays readable on any wallpaper,
 * a light rim, and a bright highlight along the top edge.
 */
/** Set from the "Kabur kaca" slider: 0 = clear glass, 1 = frosted/dark. */
object GlassStyle { var strength = 0.6f }

fun Modifier.glass(radius: Dp = 22.dp, strong: Boolean = true): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    val k = 0.45f + GlassStyle.strength * 0.9f
    return this
        .shadow(if (strong) 18.dp else 0.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
        .clip(shape)
        .background(
            Brush.verticalGradient(
                if (strong) listOf(Color(0x1A2438).copy(alpha = (0.43f * k).coerceAtMost(0.95f)),
                    Color(0x101828).copy(alpha = (0.55f * k).coerceAtMost(0.95f)))
                else listOf(Color.White.copy(alpha = 0.14f * k), Color.White.copy(alpha = 0.08f * k))
            )
        )
        .border(
            1.dp,
            Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x24FFFFFF), Color(0x14FFFFFF))),
            shape,
        )
        .drawWithContent {
            drawContent()
            // Specular highlight on the top edge.
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, Color(0x99FFFFFF), Color.Transparent)),
                Offset(size.width * 0.08f, 1.5f), Offset(size.width * 0.92f, 1.5f), strokeWidth = 1.5f,
            )
        }
}

val Muted = Color(0xFFE4EAF5)
