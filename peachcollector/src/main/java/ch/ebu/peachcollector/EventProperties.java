package ch.ebu.peachcollector;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import static ch.ebu.peachcollector.Constant.*;

public class EventProperties {

    /**
     *  The playlist ID of the playlist the media is added to or removed from
     */
    @Nullable public String playlistID;

    /**
     *  The position of the item in the playlist refered py `playlistID`
     *  Can be either "top" or "end"
     */
    @Nullable public String insertPosition;

    /**
     *  The time spent by the user watching this media (in seconds)
     */
    @Nullable public Number timeSpent;

    /**
     *  Playback position for the media (in seconds)
     *  For a live stream 0.0 is the max value. A negative value mean a timeshift in the past
     */
    @Nullable public Number playbackPosition;

    /**
     *  Previous playback position for the media (in seconds)
     *  For a live stream 0.0 is the max value. A negative value mean a timeshift in the past
     *  Usually used along a media seek event or after a media pause event
     */
    @Nullable public Number previousPlaybackPosition;

    /**
     *  Boolean value to know if media is playing at the moment of the event
     *  Usefull for the `media_seek` event
     */
    @Nullable public Boolean isPlaying;

    /**
     *  In case of "auto continue" start mode, previousMediaID should be defined
     */
    @Nullable public String previousMediaID;

    /**
     *  Speed of playback. Value is relative to normal playback speed
     *  - 0.5 for 2x slow motion
     *  - 1 for normal playback
     *  - 2 for fast forward
     */
    @Nullable public Number playbackRate;

    /**
     *  Volume of playback in percentage.
     *  - 0 means the media is muted.
     *  - 1 is 100% volume level
     */
    @Nullable public Number volume;

    /**
     *  Mode for a video media : bar, mini, normal, wide, pip, fullscreen, cast, preview
     */
    @Nullable public String videoMode;

    /**
     *  Describes how the media is listenned to : normal, in background or if it is muted
     */
    @Nullable public String audioMode;

    /**
     *  How the media was started (normal, by "auto play", or by "auto continue")
     */
    @Nullable public String startMode;


    @Nullable
    public Map<String, Object> jsonRepresentation() {
        Map<String, Object> json = new HashMap<>();
        if(playlistID != null) { json.put(MEDIA_PLAYLIST_ID_KEY, playlistID); }
        if(insertPosition != null) { json.put(MEDIA_INSERT_POSITION_KEY, insertPosition); }
        if(timeSpent != null) { json.put(MEDIA_TIME_SPENT_KEY, timeSpent); }
        if(playbackPosition != null) { json.put(MEDIA_PLAYBACK_POSITION_KEY, playbackPosition); }
        if(previousPlaybackPosition != null) { json.put(MEDIA_PREVIOUS_PLAYBACK_POSITION_KEY, previousPlaybackPosition); }
        if(isPlaying != null) { json.put(MEDIA_IS_PLAYING_KEY, isPlaying); }
        if(previousMediaID != null) { json.put(MEDIA_PREVIOUS_ID_KEY, previousMediaID); }
        if(playbackRate != null) { json.put(MEDIA_PLAYBACK_RATE_KEY, playbackRate); }
        if(volume != null) { json.put(MEDIA_VOLUME_KEY, volume); }
        if(videoMode != null) { json.put(MEDIA_VIDEO_MODE_KEY, videoMode); }
        if(audioMode != null) { json.put(MEDIA_AUDIO_MODE_KEY, audioMode); }
        if(startMode != null) { json.put(MEDIA_START_MODE_KEY, startMode); }
        if(json.isEmpty()) return null;
        return json;
    }
    
}
