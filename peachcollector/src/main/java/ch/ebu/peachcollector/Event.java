package ch.ebu.peachcollector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.ebu.peachcollector.Constant.*;

@Entity(tableName = "Event", indices = @Index(value = {"id"}, unique = true))
public class Event {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    @ColumnInfo(name = "eventID")
    private String mEventID;

    @NonNull
    @ColumnInfo(name = "type")
    private String mType;

    @ColumnInfo(name = "creationDate")
    private long mCreationDate;

    @ColumnInfo(name = "pageStartDate")
    private long mPageStartDate;

    @Nullable
    @ColumnInfo(name = "properties")
    private Map<String, Object> mProperties;

    @Nullable
    @ColumnInfo(name = "context")
    private Map<String, Object> mContext;

    @Nullable
    @ColumnInfo(name = "metadata")
    private Map<String, Object> mMetadata;

    public Event(@NonNull String eventID, @NonNull String type) {
        this.mEventID = eventID;
        this.mType = type;
        this.mCreationDate = new Date().getTime();
    }

    public int getId() { return this.id; }
    public String getEventID() { return this.mEventID; }
    public String getType() { return this.mType; }
    public long getCreationDate() { return mCreationDate; }
    public long getPageStartDate() { return mPageStartDate; }
    public Map<String, Object> getMetadata() { return mMetadata; }
    public Map<String, Object> getContext() { return mContext; }
    public Map<String, Object> getProperties() { return mProperties; }

    public void setId(int id) { this.id = id; }
    public void setCreationDate(long creationDate) { this.mCreationDate = creationDate; }
    public void setPageStartDate(long pageStartDate) { this.mPageStartDate = pageStartDate; }
    public void setMetadata(@NonNull Map<String, Object> metadata) { this.mMetadata = metadata; }
    public void setContext(@NonNull Map<String, Object> context) { this.mContext = context; }
    public void setProperties(@NonNull Map<String, Object> properties) { this.mProperties = properties; }


    public void setContext(@Nullable EventContext context) {
        if (context != null) this.mContext = context.jsonRepresentation();
        else this.mContext = null;
    }

    public void setProperties(@Nullable EventProperties properties) {
        if (properties != null) this.mProperties = properties.jsonRepresentation();
        else this.mProperties = null;
    }

    public Map<String, Object> jsonRepresentation() {
        if (mType == null || mEventID == null || mCreationDate == 0) return null;
        Map<String, Object> json = new HashMap<>();
        json.put(EVENT_TYPE_KEY, mType);
        json.put(EVENT_ID_KEY, mEventID);
        json.put(EVENT_TIMESTAMP_KEY, mCreationDate);
        if(mContext != null) { json.put(EVENT_CONTEXT_KEY, mContext); }
        if(mProperties != null) { json.put(EVENT_PROPERTIES_KEY, mProperties); }
        if(mMetadata != null) { json.put(EVENT_METADATA_KEY, mMetadata); }
        if (json.isEmpty()) return null;
        return json;
    }



    /**
     *  Send a new event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param type    Name of the event's type.
     *  @param eventID unique identifier related to the event (e.g., data source id for a recommendation hit, media id for a media play)
     *  @param properties optional properties related to the event
     *  @param context optional context of the event (usually contains a component, e.g. Carousel, VideoPlayer...)
     *  @param metadata optional dictionary of metadatas (should be kept as small as possible)
     */
    public static void send(@NonNull String type,
                            @NonNull String eventID,
                            @Nullable EventProperties properties,
                            @Nullable EventContext context,
                            @Nullable Map<String, Object> metadata){
        Event event = new Event(eventID, type);
        event.setProperties(properties);
        event.setContext(context);
        event.mMetadata = metadata;
        PeachCollector.sendEvent(event);
    }


    /**
     *  Send a recommendation hit event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param recommendationID    Unique identifier of the recommendation.
     *  @param itemID Unique identifier for the clip, media or article hit
     *  @param hitIndex Index of the item that has been hit
     *  @param appSectionID Unique identifier of app section where the recommendation is displayed
     *  @param source Identifier of the element in which the recommendation is displayed (the module, view or popup)
     *  @param component Description of the element in which the recommendation is displayed
     */
    public static void sendRecommendationHit(@NonNull String recommendationID,
                                             @NonNull String itemID,
                                             @NonNull Number hitIndex,
                                             @Nullable String appSectionID,
                                             @Nullable String source,
                                             @Nullable EventContextComponent component){
        EventContext context = EventContext.recommendationContext(null, appSectionID, source, component, hitIndex, itemID);
        Event.send(Constant.EventType.RecommendationHit, recommendationID, null, context, null);
    }

    /**
     *  Send a recommendation displayed event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param recommendationID Unique identifier of the recommendation.
     *  @param itemsDisplayed List of unique identifiers for the clips, medias or articles show/displayed
     *  @param appSectionID Unique identifier of app section where the recommendation is displayed
     *  @param source Identifier of the element in which the recommendation is displayed (the module, view or popup)
     *  @param component Description of the element in which the recommendation is displayed
     */
    public static void sendRecommendationDisplayed(@NonNull String recommendationID,
                                                   @NonNull List<String> itemsDisplayed,
                                                   @Nullable String appSectionID,
                                                   @Nullable String source,
                                                   @Nullable EventContextComponent component) {
        EventContext context = EventContext.recommendationContext(itemsDisplayed, appSectionID, source, component, null, null);
        Event.send(Constant.EventType.RecommendationDisplayed, recommendationID, null, context, null);
    }

    /**
     *  Send a recommendation loaded event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param recommendationID Unique identifier of the recommendation.
     *  @param items List of unique identifiers for the clips, medias or articles recommended
     *  @param appSectionID Unique identifier of app section where the recommendation is displayed
     *  @param source Identifier of the element in which the recommendation is displayed (the module, view or popup)
     *  @param component Description of the element in which the recommendation is displayed
     */
    public static void sendRecommendationLoaded(@NonNull String recommendationID,
                                                @NonNull List<String> items,
                                                @Nullable String appSectionID,
                                                @Nullable String source,
                                                @Nullable EventContextComponent component) {
        EventContext context = EventContext.recommendationContext(items, appSectionID, source, component, null, null);
        Event.send(Constant.EventType.RecommendationLoaded, recommendationID, null, context, null);
    }

    /**
     *  Send a page view event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param pageID Unique identifier of the page.
     *  @param referrer Identifier of the previous page that led to this page view
     *  @param recommendationID recommendation identifier If the page view is initiated by a recommendation hit
     */
    public static void sendPageView(@NonNull String pageID,
                                    @Nullable String referrer,
                                    @Nullable String recommendationID){
        EventContext context = new EventContext();
        context.referrer = referrer;
        context.contextID = recommendationID;
        Event.send(Constant.EventType.PageView, pageID, null, context, null);
    }

    /**
     *  Send a media play event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadatas (should be kept as small as possible)
     */
    public static void sendMediaPlay(@NonNull String mediaID,
                                     @Nullable EventProperties properties,
                                     @Nullable EventContext context,
                                     @Nullable Map<String, Object> metadata){
        Event.send(Constant.EventType.MediaPlay, mediaID, properties, context, metadata);
    }

    /**
     *  Send a media pause event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadatas (should be kept as small as possible)
     */
    public static void sendMediaPause(@NonNull String mediaID,
                                      @Nullable EventProperties properties,
                                      @Nullable EventContext context,
                                      @Nullable Map<String, Object> metadata){
        Event.send(Constant.EventType.MediaPause, mediaID, properties, context, metadata);
    }

    /**
     *  Send a media seek event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadatas (should be kept as small as possible)
     */
    public static void sendMediaSeek(@NonNull String mediaID,
                                     @Nullable EventProperties properties,
                                     @Nullable EventContext context,
                                     @Nullable Map<String, Object> metadata) {
        Event.send(Constant.EventType.MediaSeek, mediaID, properties, context, metadata);
    }

    /**
     *  Send a media stop event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadatas (should be kept as small as possible)
     */
    public static void sendMediaStop(@NonNull String mediaID,
                                     @Nullable EventProperties properties,
                                     @Nullable EventContext context,
                                     @Nullable Map<String, Object> metadata) {
        Event.send(Constant.EventType.MediaStop, mediaID, properties, context, metadata);
    }

    /**
     *  Send a media stop event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadatas (should be kept as small as possible)
     */
    public static void sendMediaEnd(@NonNull String mediaID,
                                    @Nullable EventProperties properties,
                                    @Nullable EventContext context,
                                    @Nullable Map<String, Object> metadata) {
        Event.send(Constant.EventType.MediaEnd, mediaID, properties, context, metadata);
    }

    /**
     *  Send a media heartbeat event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadatas (should be kept as small as possible)
     */
    public static void sendMediaHeartbeat(@NonNull String mediaID,
                                          @Nullable EventProperties properties,
                                          @Nullable EventContext context,
                                          @Nullable Map<String, Object> metadata) {
        Event.send(Constant.EventType.MediaHeartbeat, mediaID, properties, context, metadata);
    }


    /**
     *  Send a media playlist add event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  Properties should contain the playlist ID to which it is added and also insert position
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadatas (should be kept as small as possible)
     */
    public static void sendMediaPlaylistAdd(@NonNull String mediaID,
                                            @Nullable EventProperties properties,
                                            @Nullable EventContext context,
                                            @Nullable Map<String, Object> metadata) {
        Event.send(Constant.EventType.MediaPlaylistAdd, mediaID, properties, context, metadata);
    }

    /**
     *  Send a media playlist remove event. Event will be added to the queue and sent accordingly to publishers' configurations.
     *  Properties should contain the playlist ID from which it is removed
     *  @param mediaID Unique identifier of the media
     *  @param properties Properties of the media and it's current state
     *  @param context Context of the media (e. g. view where it's displayed, component used to play the media...)
     *  @param metadata Metadatas (should be kept as small as possible)
     */
    public static void sendMediaPlaylistRemove(@NonNull String mediaID,
                                               @Nullable EventProperties properties,
                                               @Nullable EventContext context,
                                               @Nullable Map<String, Object> metadata) {
        Event.send(Constant.EventType.MediaPlaylistRemove, mediaID, properties, context, metadata);
    }
}
