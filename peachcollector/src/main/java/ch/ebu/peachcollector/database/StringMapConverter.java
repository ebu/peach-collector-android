package ch.ebu.peachcollector.database;


import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class StringMapConverter {
    @TypeConverter
    public static Map<String, Object> fromString(String value) {
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        return new Gson().fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromStringMap(Map<String, Object> map) {
        Gson gson = new Gson();
        return gson.toJson(map);
    }
}