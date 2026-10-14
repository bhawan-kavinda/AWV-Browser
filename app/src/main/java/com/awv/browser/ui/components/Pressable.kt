package com.awv.browser.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Tap target with a small press-scale animation (no ripple, matches the demo's flat look). */
fun Modifier.pressable(
    enabled: Boolean = true,
    label: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
        .semantics { if (label != null) contentDescription = label }
}

/** Plain tap target (no scale animation, no ripple) for segmented controls etc. */
fun Modifier.tappable(
    label: String? = null,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .clickable(interactionSource = source, indication = null, role = role, onClick = onClick)
        .semantics { if (label != null) contentDescription = label }
}
