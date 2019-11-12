package ch.ebu.peachcollector;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Event.class, EventStatus.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class, StringMapConverter.class})
public abstract class RoomDatabase extends androidx.room.RoomDatabase {
    public abstract EventDao peachCollectorEventDao();

    private static volatile RoomDatabase INSTANCE;

    public static RoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RoomDatabase.class) {
                if (INSTANCE == null) {
                    androidx.room.RoomDatabase.Builder<RoomDatabase> builder = Room.databaseBuilder(context.getApplicationContext(),
                            RoomDatabase.class, "peach_collector_database")
                            .addCallback(sRoomDatabaseCallback);
                    if (PeachCollector.isUnitTesting) builder.allowMainThreadQueries();
                    INSTANCE = builder.build();
                }
            }
        }
        return INSTANCE;
    }

    private static androidx.room.RoomDatabase.Callback sRoomDatabaseCallback = new androidx.room.RoomDatabase.Callback(){
        @Override
        public void onOpen (@NonNull SupportSQLiteDatabase db){
            super.onOpen(db);
        }
    };





}


