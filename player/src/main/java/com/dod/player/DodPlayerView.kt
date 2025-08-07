package com.dod.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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
import androidx.core.graphics.drawable.toDrawable

@Composable
fun DodPlayerView(
    modifier: Modifier = Modifier,
    mediaItems: List<DodPlayerItem>,
    viewModel: PlayerViewModel = viewModel()
) {
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mediaItems) {
        if (mediaItems.isNotEmpty()) {
            viewModel.setMediaItems(mediaItems)
        }
    }

    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
        ) {
            val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
            LaunchedEffect(Unit) {
                dialogWindowProvider?.window?.let { window ->
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    window.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val layoutParams = window.attributes
                        layoutParams.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        window.attributes = layoutParams
                    }
                }
            }
            CustomPlayerContent(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                isFullScreen = true,
                onFullScreenToggle = { isFullScreen = !isFullScreen },
                viewModel = viewModel
            )
        }
    } else {
        CustomPlayerContent(
            modifier = modifier
                .background(Color.Black),
            isFullScreen = false,
            onFullScreenToggle = { isFullScreen = !isFullScreen },
            viewModel = viewModel
        )
    }
}

@Composable
fun CustomPlayerContent(
    modifier: Modifier = Modifier,
    isFullScreen: Boolean,
    onFullScreenToggle: () -> Unit,
    viewModel: PlayerViewModel
) {
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isPipMode by viewModel.isPipMode.collectAsStateWithLifecycle()
    val exoPlayer = viewModel.exoPlayer

    var showSubtitleDialog by remember { mutableStateOf(false) }

    if (showSubtitleDialog) {
        SubtitleSelectionDialog(
            availableSubtitles = playerState.availableSubtitles,
            selectedSubtitle = playerState.selectedSubtitle,
            onSubtitleSelected = { track ->
                viewModel.selectSubtitle(track)
                showSubtitleDialog = false
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    LaunchedEffect(isFullScreen) {
        activity?.let {
            val window = it.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                view.keepScreenOn = true
            } else {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                view.keepScreenOn = false
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == false) {
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

        if (!isPipMode && !playerState.isBuffering) {
            CustomPlayerController(
                modifier = Modifier.fillMaxSize(),
                playerState = playerState,
                onPlayPauseToggle = { viewModel.togglePlayPause() },
                onSeekBack = { viewModel.seekBack() },
                onSeekForward = { viewModel.seekForward() },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() },
                isFullScreen = isFullScreen,
                topController = {
                    CustomTopController(
                        playerState = playerState,
                        onSubtitleClick = { showSubtitleDialog = true }
                    )
                },
                bottomController = {
                    CustomBottomController(
                        playerState = playerState,
                        isFullScreen = isFullScreen,
                        onMuteToggle = { viewModel.toggleMute() },
                        onSeek = { position -> viewModel.seekTo(position) },
                        onFullScreenToggle = onFullScreenToggle
                    )
                }
            )
        }

        AnimatedVisibility(
            visible = playerState.isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(if(isFullScreen) 60.dp else 48.dp),
                color = DodPlayerSetting.enableColor
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
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isFullScreen: Boolean,
    topController: @Composable () -> Unit,
    bottomController: @Composable () -> Unit
) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var tapAnimationState by remember { mutableStateOf(TapAnimationState.NONE) }

    val activity = LocalActivity.current
    LaunchedEffect(controlsVisible, isFullScreen) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen && controlsVisible) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(controlsVisible, playerState.isPlaying) {
        if (controlsVisible && playerState.isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(tapAnimationState) {
        if (tapAnimationState != TapAnimationState.NONE) {
            delay(600)
            tapAnimationState = TapAnimationState.NONE
        }
    }

    val rewindAlpha by animateFloatAsState(targetValue = if (tapAnimationState == TapAnimationState.REWIND) 1f else 0f, label = "")
    val forwardAlpha by animateFloatAsState(targetValue = if (tapAnimationState == TapAnimationState.FORWARD) 1f else 0f, label = "")

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { controlsVisible = !controlsVisible },
                onDoubleTap = { offset ->
                    controlsVisible = true
                    if (offset.x < size.width / 2) {
                        onSeekBack()
                        tapAnimationState = TapAnimationState.REWIND
                    } else {
                        onSeekForward()
                        tapAnimationState = TapAnimationState.FORWARD
                    }
                }
            )
        }
    ) {
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
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    topController()
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if(isFullScreen) 48.dp else 28.dp)
                ) {
                    Icon(
                        painter = painterResource(DodPlayerSetting.skipBackwardIconId),
                        contentDescription = "Rewind",
                        tint = DodPlayerSetting.enableColor,
                        modifier = Modifier.size(if(isFullScreen) 48.dp else 28.dp).alpha(rewindAlpha)
                    )

                    IconButton(
                        modifier = Modifier.size(if(isFullScreen) 54.dp else 32.dp),
                        onClick = onPrevious,
                        enabled = playerState.totalMediaCount > 1
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.skip_previous_circle),
                            contentDescription = "Previous Track",
                            tint = if (playerState.currentMediaIndex > 0) DodPlayerSetting.enableColor else DodPlayerSetting.disableColor
                        )
                    }

                    IconButton(
                        modifier = Modifier.size(if(isFullScreen) 100.dp else 62.dp),
                        onClick = onPlayPauseToggle
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(if (playerState.isPlaying) DodPlayerSetting.pauseIconId else DodPlayerSetting.playIconId),
                            contentDescription = "Play/Pause",
                            tint = DodPlayerSetting.enableColor,
                        )
                    }

                    IconButton(
                        modifier = Modifier.size(if(isFullScreen) 54.dp else 32.dp),
                        onClick = onNext,
                        enabled = playerState.totalMediaCount > 1
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.skip_next_circle),
                            contentDescription = "Next Track",
                            tint = if (playerState.currentMediaIndex < playerState.totalMediaCount - 1) DodPlayerSetting.enableColor else DodPlayerSetting.disableColor
                        )
                    }

                    Icon(
                        painter = painterResource(DodPlayerSetting.skipForwardIconId),
                        contentDescription = "Forward",
                        tint = DodPlayerSetting.enableColor,
                        modifier = Modifier.size(if(isFullScreen) 48.dp else 28.dp).alpha(forwardAlpha)
                    )
                }

                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
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
    onSubtitleClick: () -> Unit
) {
    val activity = LocalActivity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (playerState.hasSubtitle) {
            IconButton(onClick = onSubtitleClick) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(DodPlayerSetting.subtitleIconId),
                    contentDescription = "Subtitle",
                    tint = if(playerState.selectedSubtitle != null) DodPlayerSetting.enableColor else DodPlayerSetting.disableColor
                )
            }
        }

        if (DodPlayerSetting.needClose) {
            IconButton(onClick = {
                activity?.finish()
            }) {
                Icon(
                    modifier = Modifier.size(24.dp),
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
    Log.e("isMute", playerState.isMuted.toString())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (DodPlayerSetting.needTimeText) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = DodPlayerSetting.timeFormat(
                    formatDuration(scrubbingPosition ?: playerState.currentPosition),
                    formatDuration(playerState.duration)
                ),
                fontSize = 12.sp,
                color = DodPlayerSetting.timeTextColor,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
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

            if (DodPlayerSetting.needFullscreen || DodPlayerSetting.needVolumeControl) {
                Spacer(modifier = Modifier.width(16.dp))
                if (DodPlayerSetting.needVolumeControl) {
                    IconButton(onClick = onMuteToggle) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(if (playerState.isMuted) DodPlayerSetting.volumeOffIconId else DodPlayerSetting.volumeOnIconId),
                            contentDescription = "Mute/Unmute",
                            tint = DodPlayerSetting.enableColor
                        )
                    }
                }
                if (DodPlayerSetting.needFullscreen) {
                    IconButton(onClick = onFullScreenToggle) {
                        Icon(
                            modifier = Modifier.size(24.dp),
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

@Composable
fun SubtitleSelectionDialog(
    availableSubtitles: List<TrackInfo>,
    selectedSubtitle: TrackInfo?,
    onSubtitleSelected: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "자막 선택",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

            SubtitleDialogRow(
                text = "자막 끄기",
                isSelected = selectedSubtitle == null,
                onClick = { onSubtitleSelected(null) }
            )

            availableSubtitles.forEach { track ->
                SubtitleDialogRow(
                    text = track.displayName,
                    isSelected = track == selectedSubtitle,
                    onClick = { onSubtitleSelected(track) }
                )
            }
        }
    }
}

@Composable
private fun SubtitleDialogRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(R.drawable.check),
            contentDescription = null,
            tint = if (isSelected) DodPlayerSetting.enableColor else DodPlayerSetting.disableColor
        )
        Spacer(Modifier.width(16.dp))
        Text(text = text, color = Color.White, fontSize = 16.sp)
    }
}


@SuppressLint("DefaultLocale")
private fun formatDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d", minutes, seconds)
}