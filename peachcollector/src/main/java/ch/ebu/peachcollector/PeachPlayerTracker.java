package ch.ebu.peachcollector;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class PeachPlayerTracker {

    private static volatile PeachPlayerTracker sharedTracker;

    private Player player;
    private String itemID;
    private EventContext context;
    private EventProperties props;
    private Map<String, Object> metadata;
    private HashMap<String, Timer> publisherTimers;
    private Date trackingStartDate;

    private Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(@Player.State int state) {
            sharedTracker.updateTimeSpent();
            if (state == Player.STATE_READY && sharedTracker.player.isPlaying()) { // playing
                sharedTracker.startHeartbeats();
                sharedTracker.props.playbackPosition = sharedTracker.player.getCurrentPosition() / 1000;
                Event.sendMediaPlay(sharedTracker.itemID, sharedTracker.props, sharedTracker.context, sharedTracker.metadata);
            }
            else if (state == Player.STATE_ENDED) { // reached the end
                sharedTracker.stopHeartbeats();
                sharedTracker.props.playbackPosition = sharedTracker.player.getCurrentPosition() / 1000;
                Event.sendMediaEnd(sharedTracker.itemID, sharedTracker.props, sharedTracker.context, sharedTracker.metadata);
            }
            else {
                // Paused because of buffering
            }
        }
        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason){
            sharedTracker.updateTimeSpent();
            if (playWhenReady) { // playing
                sharedTracker.startHeartbeats();
                sharedTracker.props.playbackPosition = sharedTracker.player.getCurrentPosition() / 1000;
                Event.sendMediaPlay(sharedTracker.itemID, sharedTracker.props, sharedTracker.context, sharedTracker.metadata);
            }
            else { // paused
                sharedTracker.stopHeartbeats();
                sharedTracker.props.playbackPosition = sharedTracker.player.getCurrentPosition() / 1000;
                Event.sendMediaPause(sharedTracker.itemID, sharedTracker.props, sharedTracker.context, sharedTracker.metadata);
            }
        }
        @Override
        public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
            sharedTracker.updateTimeSpent();
            sharedTracker.props.playbackPosition = newPosition.positionMs / 1000;
            sharedTracker.props.previousPlaybackPosition = oldPosition.positionMs / 1000;
            Event.sendMediaSeek(sharedTracker.itemID, sharedTracker.props, sharedTracker.context, sharedTracker.metadata);
            sharedTracker.props.previousPlaybackPosition = null;
        }
        @Override
        public void onVolumeChanged(float volume) {
            sharedTracker.props.volume = volume;
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            sharedTracker.props.playbackRate = playbackParameters.speed;
        }
    };
    /**
     *  Initialize the player tracker. This will not trigger any events.
     *  Accepts any {@link Player} implementation (ExoPlayer, MediaController, etc.)
     *  @param player The player used in the application.
     */
    public static void setPlayer(Player player) {
        if (sharedTracker == null) {
            sharedTracker = new PeachPlayerTracker();
        }

        sharedTracker.player = player;

        if (sharedTracker.itemID != null) {
            sharedTracker.player.addListener(sharedTracker.playerListener);
        }
    }

    /**
     *  Start tracking a media item. Properties will be automatically updated and events will be sent
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadata (should be kept as small as possible)
     */
    public static void trackMedia(@NonNull String mediaID,
                                  @Nullable EventProperties properties,
                                  @Nullable EventContext context,
                                  @Nullable Map<String, Object> metadata){
        sharedTracker.stopHeartbeats();
        boolean isNewItem = sharedTracker.itemID == null || !mediaID.equals(sharedTracker.itemID);
        sharedTracker.itemID = mediaID;
        sharedTracker.props = properties;
        sharedTracker.context = context;
        sharedTracker.metadata = metadata;
        sharedTracker.publisherTimers = new HashMap<>();
        sharedTracker.trackingStartDate = new Date();

        if (sharedTracker.props == null) {
            sharedTracker.props = new EventProperties();
        }
        sharedTracker.props.playbackRate = sharedTracker.player != null ? sharedTracker.player.getPlaybackParameters().speed : 1;
        sharedTracker.props.playbackPosition = sharedTracker.player != null ? sharedTracker.player.getCurrentPosition() / 1000 : 0;
        if (isNewItem) {
            sharedTracker.props.timeSpent = 0;
        }

        if (sharedTracker.player != null) {
            sharedTracker.player.addListener(sharedTracker.playerListener);
            if (sharedTracker.player.isPlaying()) {
                sharedTracker.startHeartbeats();
            }
        }
    }

    /**
     *  Stop tracking of the current media item
     */
    public static void clearCurrentItem() {
        if (sharedTracker == null) { return; }
        sharedTracker.stopHeartbeats();
        if (sharedTracker.player != null) {
            sharedTracker.player.removeListener(sharedTracker.playerListener);
        }

        sharedTracker.itemID = null;
        sharedTracker.props = null;
        sharedTracker.context = null;
        sharedTracker.metadata = null;
        sharedTracker.trackingStartDate = null;
    }

    private void startHeartbeats() {
        for (final String publisherName: PeachCollector.sharedCollector.publishers.keySet()) {
            final Publisher publisher = PeachCollector.sharedCollector.publishers.get(publisherName);
            long interval = publisher.playerTrackerHeartbeatInterval;

            Timer timer = sharedTracker.publisherTimers.get(publisherName);
            if (timer == null) {

                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (props == null) return;
                                updateTimeSpent();
                                props.playbackPosition = sharedTracker.player.getCurrentPosition() / 1000;
                                Event.sendMediaHeartbeat(itemID, props, context, metadata, publisherName);
                            }
                        };
                        mainHandler.post(myRunnable);
                    }
                }, interval * 1000, interval * 1000);
                sharedTracker.publisherTimers.put(publisherName, timer);
            }
        }
    }

    private void stopHeartbeats() {
        for (String publisherName: PeachCollector.sharedCollector.publishers.keySet()) {
            if (sharedTracker.publisherTimers == null) return;
            Timer timer = sharedTracker.publisherTimers.get(publisherName);
            if (timer != null) {
                timer.cancel();
                timer.purge();
                sharedTracker.publisherTimers.remove(publisherName);
            }
        }
    }

    private void updateTimeSpent() {
        if (sharedTracker.trackingStartDate == null) return;
        Date now = new Date();
        long diff =  now.getTime() - sharedTracker.trackingStartDate.getTime();
        sharedTracker.props.timeSpent = diff / 1000;
    }
}
