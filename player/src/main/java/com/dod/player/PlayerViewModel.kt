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

    private var mediaItems = listOf<DodPlayerItem>()

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
                if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                    DodPlayerSetting.isPlaying = player.isPlaying
                    _playerState.update { it.copy(isPlaying = player.isPlaying) }
                }

                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                    _playerState.update { it.copy(isBuffering = player.playbackState == Player.STATE_BUFFERING) }
                }

                if (events.contains(Player.EVENT_VOLUME_CHANGED)) {
                    _playerState.update { it.copy(isMuted = player.volume == 0f) }
                }

                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) || events.contains(Player.EVENT_TRACKS_CHANGED)) {
                    updatePlayerState(player)
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

    private fun updatePlayerState(
        player: Player
    ) {
        val availableSubtitles = getAvailableSubtitles(player)
        val selectedSubtitle = availableSubtitles.find { it.isSelected }

        _playerState.update {
            it.copy(
                duration = player.duration.coerceAtLeast(0),
                isMuted = player.volume == 0f,
                hasSubtitle = availableSubtitles.isNotEmpty(),
                availableSubtitles = availableSubtitles,
                selectedSubtitle = selectedSubtitle,
                currentMediaIndex = player.currentMediaItemIndex,
                totalMediaCount = player.mediaItemCount
            )
        }
    }

    fun setMediaItems(
        items: List<DodPlayerItem>,
        startIndex: Int = 0
    ) {
        if (mediaItems == items) return
        mediaItems = items
        val exoPlayerMediaItems = items.map { dodPlayerItem ->
            buildMediaItem(dodPlayerItem)
        }
        exoPlayer.setMediaItems(exoPlayerMediaItems, startIndex, C.TIME_UNSET)
        exoPlayer.prepare()
    }

    private fun buildMediaItem(
        item: DodPlayerItem
    ): MediaItem {
        val subtitleConfigs = item.subtitles.mapIndexed { index, subtitle ->
            MediaItem.SubtitleConfiguration.Builder(subtitle.url.toUri())
                .setMimeType(getMimeTypeFromUrl(subtitle.url))
                .setLanguage(getLanguageFromUrl(subtitle.url))
                .setSelectionFlags(if (index == 0) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
        return MediaItem.Builder()
            .setUri(item.videoUrl)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()
    }

    private fun getAvailableSubtitles(
        player: Player
    ): List<TrackInfo> {
        val tracks = player.currentTracks
        // 현재 재생 중인 미디어 아이템 정보를 가져옴
        val currentItem = mediaItems.getOrNull(player.currentMediaItemIndex) ?: return emptyList()

        return tracks.groups
            .mapIndexedNotNull { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_TEXT) {
                    (0 until group.length).map { trackIndex ->
                        val format = group.getTrackFormat(trackIndex)
                        val trackLanguage = format.language

                        // ExoPlayer의 언어 코드를 이용해 사용자가 입력한 SubtitleItem을 찾음
                        val userDefinedDisplayName = currentItem.subtitles
                            .find { getLanguageFromUrl(it.url) == trackLanguage }?.displayName

                        // 사용자 정의 이름이 있으면 사용하고, 없으면 기존 방식으로 대체
                        val displayName = userDefinedDisplayName
                            ?: format.label
                            ?: trackLanguage
                            ?: "Unknown"

                        TrackInfo(
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            displayName = displayName, // << 최종 displayName 적용
                            isSelected = group.isTrackSelected(trackIndex),
                            language = trackLanguage
                        )
                    }
                } else null
            }
            .flatten()
    }

    fun selectSubtitle(
        track: TrackInfo?
    ) {
        val parametersBuilder = exoPlayer.trackSelectionParameters.buildUpon()

        if (track == null) {
            // 자막 끄기: Text 트랙 타입을 비활성화
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            // 특정 자막 선택하기
            parametersBuilder
                // 1. Text 트랙 타입을 활성화
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                // 2. 선택된 자막의 언어 코드를 선호 언어로 설정
                .setPreferredTextLanguage(track.language)
        }

        exoPlayer.trackSelectionParameters = parametersBuilder.build()
    }

    private fun getMimeTypeFromUrl(
        url: String?
    ): String? {
        val extension = url?.substringAfterLast('.', "")?.lowercase()

        return when (extension) {
            "vtt" -> MimeTypes.TEXT_VTT
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "srt.txt" -> MimeTypes.APPLICATION_SUBRIP
            "ssa", "ass" -> MimeTypes.TEXT_SSA
            else -> null
        }
    }

    private fun getLanguageFromUrl(
        url: String?
    ): String? {
        val fileName = url?.substringAfterLast('/')?.substringBeforeLast('.')
        val potentialCode = fileName?.substringAfterLast('.')?.ifEmpty { fileName.substringAfterLast('_') }?.lowercase()
        return when (potentialCode) {
            "ko", "kr", "korean" -> "ko"
            "en", "us", "gb", "english" -> "en"
            "ja", "jp", "japanese" -> "ja"
            else -> if (potentialCode?.length == 2) potentialCode else "und"
        }
    }

    fun playPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun playNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
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
    val isBuffering: Boolean = false,
    val hasSubtitle: Boolean = false,
    val availableSubtitles: List<TrackInfo> = emptyList(),
    val selectedSubtitle: TrackInfo? = null,
    val currentMediaIndex: Int = 0,
    val totalMediaCount: Int = 0
)

data class TrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val displayName: String,
    val language: String?,
    val isSelected: Boolean
)

data class DodPlayerItem(
    val videoUrl: String,
    val subtitles: List<SubtitleItem> = emptyList()
)

data class SubtitleItem(
    val url: String,
    val displayName: String
)

enum class TapAnimationState {
    NONE,
    REWIND,
    FORWARD
}