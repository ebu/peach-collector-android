package ch.ebu.peachcollector;

import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static ch.ebu.peachcollector.Constant.*;

public class EventContextComponent {
    @Nullable public String type;
    @Nullable public String name;
    @Nullable public String version;

    @Nullable
    public Map<String, Object> jsonRepresentation() {
        Map<String, Object> json = new HashMap<>();
        if(type != null) { json.put(CONTEXT_COMPONENT_TYPE_KEY, type); }
        if(name != null) { json.put(CONTEXT_COMPONENT_NAME_KEY, name); }
        if(version != null) { json.put(CONTEXT_COMPONENT_VERSION_KEY, version); }
        if(json.isEmpty()) return null;
        return json;
    }
}
