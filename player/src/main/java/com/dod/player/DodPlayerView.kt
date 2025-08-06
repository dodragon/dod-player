package com.dod.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun DodPlayerView(
    modifier: Modifier = Modifier,
    activity: Activity,
    videoUrl: String,
    subtitleUrl: String,
    viewModel: PlayerViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isPipMode by viewModel.isPipMode.collectAsStateWithLifecycle()

    val exoPlayer = viewModel.exoPlayer

    LaunchedEffect(Unit) {
        viewModel.setMedia(videoUrl, subtitleUrl)
    }

    LaunchedEffect(isFullScreen) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullScreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (activity.isInPictureInPictureMode == false) {
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = if (isFullScreen) {
            modifier
                .fillMaxSize()
        } else {
            modifier
                .fillMaxWidth()
                .aspectRatio(DodPlayerSetting.screenRatioWidth.toFloat() / DodPlayerSetting.screenRatioHeight.toFloat())
        }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    player = exoPlayer
                }
            }
        )

        if(!isPipMode) {
            CustomPlayerController(
                modifier = Modifier.fillMaxSize(),
                playerState = playerState,
                onPlayPauseToggle = { viewModel.togglePlayPause() },
                onSeekBack = { viewModel.seekBack() },
                onSeekForward = { viewModel.seekForward() },
                topController = {
                    CustomTopController(
                        playerState = playerState,
                        onSubtitleToggle = { viewModel.toggleSubtitles() }
                    )
                },
                bottomController = {
                    CustomBottomController(
                        playerState = playerState,
                        isFullScreen = isFullScreen,
                        onMuteToggle = { viewModel.toggleMute() },
                        onSeek = { position -> viewModel.seekTo(position) },
                        onFullScreenToggle = { isFullScreen = !isFullScreen }
                    )
                }
            )
        }
    }
}

@Composable
fun CustomPlayerController(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    onPlayPauseToggle: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    topController: @Composable () -> Unit,
    bottomController: @Composable () -> Unit
) {
    var controlsVisible by rememberSaveable { mutableStateOf(false) }
    var tapAnimationState by remember { mutableStateOf(TapAnimationState.NONE) }

    LaunchedEffect(controlsVisible, playerState.isPlaying) {
        if(controlsVisible && playerState.isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(tapAnimationState) {
        if(tapAnimationState != TapAnimationState.NONE) {
            delay(600)
            tapAnimationState = TapAnimationState.NONE
        }
    }

    val rewindAlpha by animateFloatAsState(
        targetValue = if(tapAnimationState == TapAnimationState.REWIND) 1f else 0f,
        label = ""
    )
    val forwardAlpha by animateFloatAsState(
        targetValue = if(tapAnimationState == TapAnimationState.FORWARD) 1f else 0f,
        label = ""
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        controlsVisible = true
                        if(offset.x < size.width / 2) {
                            onSeekBack()
                            tapAnimationState = TapAnimationState.REWIND
                        }else {
                            onSeekForward()
                            tapAnimationState = TapAnimationState.FORWARD
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(DodPlayerSetting.skipBackwardIconId),
                contentDescription = "Rewind 10s",
                tint = DodPlayerSetting.enableColor,
                modifier = Modifier.size(48.dp).alpha(rewindAlpha)
            )
            Icon(
                painter = painterResource(DodPlayerSetting.skipForwardIconId),
                contentDescription = "Forward 10s",
                tint = DodPlayerSetting.enableColor,
                modifier = Modifier.size(48.dp).alpha(forwardAlpha)
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DodPlayerSetting.controllerBackgroundColor.copy(alpha = DodPlayerSetting.controllerBackgroundColorAlpha))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                ) {
                    topController()
                }

                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        modifier = Modifier
                            .size(80.dp),
                        painter = painterResource(if (playerState.isPlaying) DodPlayerSetting.playIconId else DodPlayerSetting.pauseIconId),
                        contentDescription = "Play/Pause",
                        tint = DodPlayerSetting.enableColor,
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    bottomController()
                }
            }
        }
    }
}

@Composable
fun CustomTopController(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    onSubtitleToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if(playerState.hasSubtitle) {
            IconButton(
                onClick = onSubtitleToggle
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    painter = painterResource(DodPlayerSetting.subtitleIconId),
                    contentDescription = "Subtitle",
                    tint = if(playerState.isSubtitleOn) DodPlayerSetting.enableColor else DodPlayerSetting.disableColor
                )
            }
        }

        if(DodPlayerSetting.needClose) {
            IconButton(onClick = DodPlayerSetting.onClose) {
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    painter = painterResource(DodPlayerSetting.closeIconId),
                    contentDescription = "Exit",
                    tint = DodPlayerSetting.enableColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBottomController(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    isFullScreen: Boolean,
    onMuteToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onFullScreenToggle: () -> Unit,
) {
    var scrubbingPosition by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if(DodPlayerSetting.needTimeText) {
            Text(
                modifier = Modifier
                    .padding(start = 8.dp),
                text = DodPlayerSetting.timeFormat(formatDuration(scrubbingPosition ?: playerState.currentPosition), formatDuration(playerState.duration)),
                fontSize = 12.sp,
                color = DodPlayerSetting.timeTextColor,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                value = (scrubbingPosition ?: playerState.currentPosition).toFloat(),
                onValueChange = { scrubbingPosition = it.toLong() },
                onValueChangeFinished = {
                    val finalPosition = scrubbingPosition ?: playerState.currentPosition
                    onSeek(finalPosition)
                    scrubbingPosition = null
                },
                valueRange = 0f..(playerState.duration.toFloat().takeIf { it > 0 } ?: 0f),
                track = { sliderState ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(
                                color = DodPlayerSetting.enableColor,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        val activeTrackWidth = if (sliderState.valueRange.endInclusive > sliderState.valueRange.start) {
                            (sliderState.value - sliderState.valueRange.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                        } else {
                            0f
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(activeTrackWidth)
                                .height(6.dp)
                                .background(
                                    color = DodPlayerSetting.enableColor,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                },
                thumb = {
                    Spacer(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = DodPlayerSetting.enableColor,
                                shape = CircleShape
                            )
                    )
                }
            )

            if(DodPlayerSetting.needFullscreen || DodPlayerSetting.needVolumeControl) {
                Spacer(modifier = Modifier.width(16.dp))

                if(DodPlayerSetting.needVolumeControl) {
                    IconButton(onClick = onMuteToggle) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp),
                            painter = painterResource(if (playerState.isMuted) DodPlayerSetting.volumeOffIconId else DodPlayerSetting.volumeOnIconId),
                            contentDescription = "Mute/Unmute",
                            tint = DodPlayerSetting.enableColor
                        )
                    }
                }

                if(DodPlayerSetting.needFullscreen) {
                    IconButton(onClick = onFullScreenToggle) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp),
                            painter = painterResource(if (isFullScreen) DodPlayerSetting.fullscreenOffIconId else DodPlayerSetting.fullscreenOnIconId),
                            contentDescription = "Fullscreen/Exit",
                            tint = DodPlayerSetting.enableColor
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d", minutes, seconds)
}