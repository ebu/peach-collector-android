package ch.ebu.peachcollector;

public class Constant {

    public static final String PEACH_LOG_NOTIFICATION = "ch.ebu.testingLog";
    public static final String PEACH_LOG_NOTIFICATION_MESSAGE = "Message";

    public static final String PEACH_SCHEMA_VERSION_KEY = "peach_schema_version";
    public static final String PEACH_FRAMEWORK_VERSION_KEY = "peach_framework_version";
    public static final String PEACH_IMPLEMENTATION_VERSION_KEY = "peach_implementation_version";
    public static final String SENT_TIMESTAMP_KEY = "sent_timestamp";
    public static final String SESSION_START_TIMESTAMP_KEY = "session_start_timestamp";
    public static final String USER_ID_KEY = "user_id";


    // EVENT KEYS

    public static final String EVENTS_KEY = "events";
    public static final String EVENT_TYPE_KEY = "type";
    public static final String EVENT_ID_KEY = "id";
    public static final String EVENT_TIMESTAMP_KEY = "event_timestamp";
    public static final String EVENT_CONTEXT_KEY = "context";
    public static final String EVENT_PROPERTIES_KEY = "props";
    public static final String EVENT_METADATA_KEY = "metadata";

    public static final String CONTEXT_ID_KEY = "id";
    public static final String CONTEXT_ITEMS_KEY = "items";
    public static final String CONTEXT_ITEM_ID_KEY = "item_id";
    public static final String CONTEXT_HIT_INDEX_KEY = "hit_index";
    public static final String CONTEXT_PAGE_URI_KEY = "page_uri";
    public static final String CONTEXT_SOURCE_KEY = "source";
    public static final String CONTEXT_REFERRER_KEY = "referrer";
    public static final String CONTEXT_COMPONENT_KEY = "component";
    public static final String CONTEXT_COMPONENT_TYPE_KEY = "type";
    public static final String CONTEXT_COMPONENT_NAME_KEY = "name";
    public static final String CONTEXT_COMPONENT_VERSION_KEY = "version";

    public static final String MEDIA_TIME_SPENT_KEY = "time_spent_s";
    public static final String MEDIA_PLAYBACK_POSITION_KEY = "playback_position_s";
    public static final String MEDIA_PREVIOUS_PLAYBACK_POSITION_KEY = "previous_playback_position_s";
    public static final String MEDIA_VIDEO_MODE_KEY = "video_mode";
    public static final String MEDIA_AUDIO_MODE_KEY = "audio_mode";
    public static final String MEDIA_START_MODE_KEY = "start_mode";
    public static final String MEDIA_PREVIOUS_ID_KEY = "previous_id";
    public static final String MEDIA_PLAYBACK_RATE_KEY = "playback_rate";
    public static final String MEDIA_VOLUME_KEY = "volume";


    // CLIENT JSON KEYS

    public static final String CLIENT_KEY = "client";
    public static final String CLIENT_ID_KEY = "id";
    public static final String CLIENT_TYPE_KEY = "type";
    public static final String CLIENT_APP_ID_KEY = "app_id";
    public static final String CLIENT_APP_NAME_KEY = "name";
    public static final String CLIENT_APP_VERSION_KEY = "version";

    public static final String DEVICE_KEY = "device";
    public static final String DEVICE_TYPE_KEY = "type";
    public static final String DEVICE_VENDOR_KEY = "vendor";
    public static final String DEVICE_MODEL_KEY = "model";
    public static final String DEVICE_SCREEN_SIZE_KEY = "screen_size";
    public static final String DEVICE_LANGUAGE_KEY = "language";
    public static final String DEVICE_TIMEZONE_KEY = "timezone";

    public static final String OS_KEY = "os";
    public static final String OS_NAME_KEY = "name";
    public static final String OS_VERSION_KEY = "version";


    public final class EventType {
        public static final String MediaPlay = "media_play"; // MEDIA
        public static final String MediaPause = "media_pause";
        public static final String MediaStop = "media_stop";
        public static final String MediaSeek = "media_seek";
        public static final String MediaVideoModeChanged = "media_video_mode_changed";
        public static final String MediaAudioModeChanged = "media_audio_mode_changed";
        public static final String MediaAudioChanged = "media_audio_changed";
        public static final String MediaHeartbeat = "media_heartbeat";
        public static final String RecommendationLoaded = "recommendation_loaded"; // RECOMMENDATION
        public static final String RecommendationHit = "recommendation_hit";
        public static final String RecommendationDisplayed = "recommendation_displayed";
        public static final String ArticleStart = "article_start"; // ARTICLE
        public static final String ArticleEnd = "article_end";
        public static final String ReadMore = "read_more";
        public static final String PageView = "page_view";
    }

    public final class Status {
        public static final int QUEUED = 0;
        public static final int SENT_TO_PUBLISHER = 1;
        public static final int PUBLISHED = 2;
    }

    public enum GoBackOnlinePolicy {
        SEND_ALL,
        SEND_ALL_RANDOMLY
    }
}
