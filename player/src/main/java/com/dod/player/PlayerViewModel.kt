package com.dod.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.ranges.coerceAtLeast

class PlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    private val _isPipMode = MutableStateFlow(false)
    val isPipMode = _isPipMode.asStateFlow()

    private var lastVolume: Float = 1f

    val exoPlayer: ExoPlayer = ExoPlayer
        .Builder(application)
        .setSeekBackIncrementMs(DodPlayerSetting.backwardSec * 1000L)
        .setSeekForwardIncrementMs(DodPlayerSetting.forwardSec * 1000L)
        .build()
        .apply {
            playWhenReady = true
        }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events
            ) {
                val params = player.trackSelectionParameters
                _playerState.update {
                    DodPlayerSetting.isPlaying = player.isPlaying
                    it.copy(
                        isPlaying = player.isPlaying,
                        duration = player.duration.coerceAtLeast(0),
                        isMuted = player.volume == 0f,
                        hasSubtitle = player.currentTracks.groups.any { group -> group.type == C.TRACK_TYPE_TEXT },
                        isSubtitleOn = !params.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                    )
                }
            }
        })

        viewModelScope.launch {
            DodPlayerSetting.isPipMode.collect {
                if(DodPlayerSetting.needPip) {
                    _isPipMode.value = it
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                _playerState.update {
                    it.copy(currentPosition = exoPlayer.currentPosition.coerceAtLeast(0))
                }
                delay(1000)
            }
        }
    }

    fun setMedia(
        videoUrl: String,
        subtitleUrl: String
    ) {
        if (exoPlayer.currentMediaItem?.mediaId == videoUrl) return

        val subtitleMimeType = getMimeTypeFromUrl(subtitleUrl)
        val subtitleLanguage = getLanguageFromUrl(subtitleUrl)

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoUrl)
            .setMediaId(videoUrl)

        if (subtitleMimeType != null) {
            val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUrl.toUri())
                .setMimeType(subtitleMimeType)
                .setLanguage(subtitleLanguage)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
    }

    private fun getMimeTypeFromUrl(
        url: String
    ): String? {
        val extension = url.substringAfterLast('.', "").lowercase()

        return when (extension) {
            "vtt" -> MimeTypes.TEXT_VTT
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "srt.txt" -> MimeTypes.APPLICATION_SUBRIP // 이전 예시 같은 경우 처리
            "ssa", "ass" -> MimeTypes.TEXT_SSA // (확장) 다른 자막 형식도 추가 가능
            else -> null // 지원하지 않는 형식이면 null 반환
        }
    }

    private fun getLanguageFromUrl(url: String): String {
        val fileName = url.substringAfterLast('/').substringBeforeLast('.')
        val potentialCode = fileName.substringAfterLast('.').ifEmpty { fileName.substringAfterLast('_') }.lowercase()
        return when (potentialCode) {
            "ko", "kr", "korean" -> "ko"
            "en", "us", "gb", "english" -> "en"
            "ja", "jp", "japanese" -> "ja"
            else -> if (potentialCode.length == 2) potentialCode else "und"
        }
    }

    fun toggleMute() {
        if (exoPlayer.volume > 0f) {
            lastVolume = exoPlayer.volume
            exoPlayer.volume = 0f
        } else {
            exoPlayer.volume = lastVolume
        }
    }

    fun toggleSubtitles() {
        val currentParams = exoPlayer.trackSelectionParameters
        val isTextDisabled = currentParams.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)

        exoPlayer.trackSelectionParameters = currentParams.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !isTextDisabled)
            .build()
    }

    fun togglePlayPause() {
        if(exoPlayer.isPlaying) {
            exoPlayer.pause()
        }else {
            exoPlayer.play()
        }
    }

    fun seekBack() {
        exoPlayer.seekBack()
    }

    fun seekForward() {
        exoPlayer.seekForward()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}

data class PlayerState(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isMuted: Boolean = false,
    val isPlaying: Boolean = false,
    val isSubtitleOn: Boolean = false,
    val hasSubtitle: Boolean = false
)

enum class TapAnimationState {
    NONE,
    REWIND,
    FORWARD
}