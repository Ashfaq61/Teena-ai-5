package com.teena.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teena.assistant.core.model.FeatureStatus
import com.teena.assistant.ui.theme.TeenaColors

sealed interface VoiceOrbState {
    data class Unavailable(val reason: String) : VoiceOrbState
    data object Idle : VoiceOrbState
    data object Connecting : VoiceOrbState
    data object Listening : VoiceOrbState
    data object Thinking : VoiceOrbState
    data object Speaking : VoiceOrbState
    data class Error(val reason: String) : VoiceOrbState
}

@Composable
fun FeatureStatusBadge(
    status: FeatureStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        FeatureStatus.ACTIVE -> TeenaColors.Emerald.copy(alpha = 0.16f) to TeenaColors.Emerald
        FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED -> TeenaColors.Cyan.copy(alpha = 0.16f) to TeenaColors.Cyan
        FeatureStatus.PARTIAL -> TeenaColors.Amber.copy(alpha = 0.16f) to TeenaColors.Amber
        FeatureStatus.BLOCKED -> TeenaColors.Rose.copy(alpha = 0.16f) to TeenaColors.Rose
        FeatureStatus.FAILED -> TeenaColors.Rose.copy(alpha = 0.24f) to TeenaColors.Rose
        FeatureStatus.NOT_IMPLEMENTED -> TeenaColors.Slate.copy(alpha = 0.20f) to TeenaColors.TextSecondary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TeenaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = CardDefaults.cardColors(containerColor = TeenaColors.Surface)
    val border = BorderStroke(1.dp, TeenaColors.Border)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
fun TeenaVoiceOrb(
    state: VoiceOrbState,
    modifier: Modifier = Modifier
) {
    val active = state is VoiceOrbState.Listening ||
        state is VoiceOrbState.Speaking ||
        state is VoiceOrbState.Connecting ||
        state is VoiceOrbState.Thinking

    val transition = rememberInfiniteTransition(label = "voice_orb")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (active) 1.08f else 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 900 else 2400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val primaryColor = when (state) {
        is VoiceOrbState.Unavailable -> TeenaColors.Slate
        is VoiceOrbState.Idle -> TeenaColors.Cyan.copy(alpha = 0.5f)
        is VoiceOrbState.Connecting -> TeenaColors.Amber
        is VoiceOrbState.Listening -> TeenaColors.Cyan
        is VoiceOrbState.Thinking -> TeenaColors.Violet
        is VoiceOrbState.Speaking -> TeenaColors.Emerald
        is VoiceOrbState.Error -> TeenaColors.Rose
    }

    val stateLabel = when (state) {
        is VoiceOrbState.Unavailable -> "NOT IMPLEMENTED"
        is VoiceOrbState.Idle -> "STANDBY"
        is VoiceOrbState.Connecting -> "CONNECTING"
        is VoiceOrbState.Listening -> "LISTENING"
        is VoiceOrbState.Thinking -> "THINKING"
        is VoiceOrbState.Speaking -> "SPEAKING"
        is VoiceOrbState.Error -> "ERROR"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(132.dp)) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * pulse
                    ),
                    radius = radius * pulse,
                    center = center
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.55f),
                    radius = radius * 0.72f,
                    center = center,
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.85f),
                    radius = radius * 0.44f * pulse,
                    center = center,
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.22f),
                    radius = radius * 0.36f,
                    center = center
                )
            }
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        if (state is VoiceOrbState.Unavailable) {
            Text(
                text = state.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = TeenaColors.TextSecondary
            )
        } else if (state is VoiceOrbState.Error) {
            Text(
                text = state.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = TeenaColors.Rose
            )
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TeenaColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = TeenaColors.TextPrimary
        )
    }
}
