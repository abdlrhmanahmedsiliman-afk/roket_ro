package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 0.dp,
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.03f))
    ),
    backgroundBrush: Brush = Brush.linearGradient(
        colors = listOf(AppColors.Surface.copy(alpha = 0.75f), AppColors.SurfaceSoft.copy(alpha = 0.55f))
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = AppColors.NeonBlue)
        ) { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = AppColors.NeonBlue.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(clickableModifier)
            .padding(contentPadding)
    ) {
        content()
    }
}
