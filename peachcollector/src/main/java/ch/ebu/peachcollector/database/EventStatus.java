package ch.ebu.peachcollector.database;

import androidx.annotation.NonNull;
import androidx.room.*;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "EventStatus",
        foreignKeys = @ForeignKey(onDelete = CASCADE,
        entity = Event.class,
        parentColumns = "id",
        childColumns = "event_id"),
        indices = {
            @Index("event_id"),
        })
public class EventStatus {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "event_id")
    public int eventID;

    @NonNull
    @ColumnInfo(name = "publisher_name")
    private String mPublisherName;

    @ColumnInfo(name = "status")
    private int mStatus;

    public EventStatus(@NonNull int eventID, @NonNull String publisherName, int status) {
        this.eventID = eventID;
        this.mPublisherName = publisherName;
        this.mStatus = status;
    }

    public EventStatus(@NonNull Event event, @NonNull String publisherName, int status) {
        this.eventID = event.getId();
        this.mPublisherName = publisherName;
        this.mStatus = status;
    }

    public int getId() { return this.id; }
    public int getEventID() { return this.eventID; }
    public String getPublisherName() { return this.mPublisherName; }
    public int getStatus() { return this.mStatus; }

    public void setId(int id) { this.id = id; }
    public void setStatus(int eventStatus) { this.mStatus = eventStatus; }
}
