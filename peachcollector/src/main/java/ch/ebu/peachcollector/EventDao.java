package ch.ebu.peachcollector;

import androidx.room.*;

import java.util.List;

@Dao
public interface EventDao {

    // allowing the insert of the same word multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Event event);

    @Query("DELETE FROM Event")
    void deleteAll();

    @Delete
    void delete(Event event);

    @Query("SELECT * from Event ORDER BY id ASC")
    List<Event> getAllEvents();

    @Query("DELETE FROM Event WHERE id IN (SELECT id FROM Event ORDER BY id DESC LIMIT :limit OFFSET :offset)")
    void deleteEvents(int limit, int offset);

    @Query("DELETE FROM Event WHERE creationDate < :dateLimit")
    void deleteEvents(long dateLimit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventStatus event);

    @Query("SELECT * from EventStatus ORDER BY id ASC")
    List<EventStatus> getAllEventStatuses();

    @Query("SELECT * from Event WHERE id = :eventRowId")
    Event getEvent(int eventRowId);

    @Query("SELECT * from EventStatus WHERE event_id = :eventRowId AND publisher_name = :publisherName")
    EventStatus getEventStatus(int eventRowId, String publisherName);

    @Query("SELECT * from EventStatus WHERE event_id = :eventRowId AND status < " + Constant.Status.PUBLISHED + "")
    public List<EventStatus> getPendingEventStatuses(int eventRowId);

    @Query("SELECT * FROM EventStatus WHERE event_id = :eventRowId")
    public List<EventStatus> getStatusesForEvent(int eventRowId);

    @Query("SELECT * FROM EventStatus WHERE publisher_name = :publisherName AND status < " + Constant.Status.PUBLISHED + "")
    public List<EventStatus> getPendingStatuses(String publisherName);

    @Query("SELECT * FROM EventStatus WHERE publisher_name = :publisherName")
    public List<EventStatus> getStatuses(String publisherName);

    @Query("SELECT * FROM EventStatus WHERE publisher_name = :publisherName AND status = :status")
    public List<EventStatus> getStatusesForPublisher(String publisherName, int status);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(EventStatus eventStatus);

    @Query("UPDATE EventStatus SET status = :status WHERE event_id = :eventRowId AND publisher_name = :publisherName")
    int updateStatus(int eventRowId, String publisherName, int status);
}
