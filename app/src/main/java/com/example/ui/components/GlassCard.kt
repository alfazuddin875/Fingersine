package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassWhite

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    glowColor: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val glassShape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .shadow(
                elevation = if (glowColor != Color.Transparent) 8.dp else 0.dp,
                shape = glassShape,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(glassShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x1BFFFFFF),
                        Color(0x0AFFFFFF)
                    )
                )
            )
            .border(
                border = BorderStroke(
                    width = borderWidth,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x4DFFFFFF),
                            Color(0x14FFFFFF)
                        )
                    )
                ),
                shape = glassShape
            )
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = GlassBorder,
    glowColor: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val buttonShape = RoundedCornerShape(24.dp)
    
    Box(
        modifier = modifier
            .shadow(
                elevation = if (glowColor != Color.Transparent && enabled) 12.dp else 0.dp,
                shape = buttonShape,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(buttonShape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33FFFFFF),
                            Color(0x11FFFFFF)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x0DFFFFFF),
                            Color(0x05FFFFFF)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (enabled) borderColor else Color(0x11FFFFFF),
                shape = buttonShape
            )
            .padding(vertical = 12.dp, horizontal = 24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
        content = content
    )
}
