package com.thingsenz.flashlight.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thingsenz.flashlight.BuildConfig
import com.thingsenz.flashlight.FlashMode
import com.thingsenz.flashlight.FlashlightUiState
import com.thingsenz.flashlight.FlashlightViewModel
import com.thingsenz.flashlight.ui.theme.FlashlightTheme
import com.thingsenz.flashlight.ui.theme.SosActive
import com.thingsenz.flashlight.ui.theme.SosIdle
import com.thingsenz.flashlight.ui.theme.StrobeActive
import com.thingsenz.flashlight.ui.theme.StrobeIdle

private const val GITHUB_URL = "https://github.com/JohnX4321/Compose_Flashlight"

@Composable
fun FlashlightRoute(viewModel: FlashlightViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    FlashlightScreen(
        uiState = uiState,
        onToggle = viewModel::onTorchTap,
        onLevelChange = viewModel::setLevel,
        onSosTap = viewModel::onSosTap,
        onStrobeTap = viewModel::onStrobeTap,
        onInfoClick = viewModel::openInfoDialog,
        onInfoDismiss = viewModel::dismissInfoDialog,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightScreen(
    uiState: FlashlightUiState,
    onToggle: () -> Unit,
    onLevelChange: (Int) -> Unit,
    onSosTap: () -> Unit,
    onStrobeTap: () -> Unit,
    onInfoClick: () -> Unit,
    onInfoDismiss: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Flashlight") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (!uiState.hasFlash) {
                Text(
                    text = "This device doesn't have a usable camera flash.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        TorchIllustration(
                            isOn = uiState.isTorchOn,
                            enabled = true,
                            onToggle = onToggle
                        )
                        PowerLevelRail(
                            level = uiState.level,
                            maxLevel = uiState.maxLevel,
                            enabled = uiState.isTorchOn && uiState.activeMode == FlashMode.NONE,
                            onLevelChange = onLevelChange
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        ModeButton(
                            label = "SOS",
                            active = uiState.activeMode == FlashMode.SOS,
                            activeColor = SosActive,
                            idleColor = SosIdle,
                            onClick = onSosTap
                        )
                        ModeButton(
                            label = "Strobe",
                            active = uiState.activeMode == FlashMode.STROBE,
                            activeColor = StrobeActive,
                            idleColor = StrobeIdle,
                            onClick = onStrobeTap
                        )
                    }
                }
            }
        }
    }

    if (uiState.showInfoDialog) {
        InfoDialog(onDismiss = onInfoDismiss)
    }
}

@Composable
private fun ModeButton(
    label: String,
    active: Boolean,
    activeColor: Color,
    idleColor: Color,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (active) activeColor else idleColor,
        animationSpec = tween(200),
        label = "mode-border"
    )
    val infinite = rememberInfiniteTransition(label = "mode-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .size(84.dp)
            .scale(if (active) pulse else 1f),
        shape = CircleShape,
        border = BorderStroke(3.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = borderColor)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("About") },
        text = {
            Column {
                Text("Flashlight")
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(
                                url = GITHUB_URL,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                        ) {
                            append("Source code")
                        }
                    }
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun FlashlightScreenPreview() {
    FlashlightTheme {
        FlashlightScreen(
            uiState = FlashlightUiState(isTorchOn = true, maxLevel = 5, level = 3),
            onToggle = {},
            onLevelChange = {},
            onSosTap = {},
            onStrobeTap = {},
            onInfoClick = {},
            onInfoDismiss = {}
        )
    }
}
