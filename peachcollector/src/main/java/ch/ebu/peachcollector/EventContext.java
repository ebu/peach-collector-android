package ch.ebu.peachcollector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.ebu.peachcollector.Constant.*;

public class EventContext {
    @Nullable public String contextID;
    @Nullable public List<String> items;
    @Nullable public Number itemsDisplayedCount;
    @Nullable public Number hitIndex;
    @Nullable public String appSectionID;
    @Nullable public String source;
    @Nullable public String referrer;
    @Nullable public EventContextComponent component;

    public static EventContext recommendationContext(@NonNull List<String> items,
                                                     @Nullable String appSectionID,
                                                     @Nullable String source,
                                                     @Nullable EventContextComponent component,
                                                     @Nullable Number itemsDisplayedCount,
                                                     @Nullable Number hitIndex){
        EventContext context = new EventContext();
        context.items = items;
        context.appSectionID = appSectionID;
        context.source = source;
        context.component = component;
        context.itemsDisplayedCount = itemsDisplayedCount;
        context.hitIndex = hitIndex;
        return context;
    }

    public static EventContext mediaContext(@NonNull String contextID,
                                            @Nullable String appSectionID,
                                            @Nullable String source,
                                            @Nullable EventContextComponent component){
        EventContext context = new EventContext();
        context.contextID = contextID;
        context.appSectionID = appSectionID;
        context.source = source;
        context.component = component;
        return context;
    }

    @Nullable
    public Map<String, Object> jsonRepresentation() {
        Map<String, Object> json = new HashMap<>();
        if(contextID != null) { json.put(CONTEXT_ID_KEY, contextID); }
        if(items != null) { json.put(CONTEXT_ITEMS_KEY, items); }
        if(itemsDisplayedCount != null) { json.put(CONTEXT_ITEMS_DISPLAYED_KEY, itemsDisplayedCount); }
        if(hitIndex != null) { json.put(CONTEXT_HIT_INDEX_KEY, hitIndex); }
        if(appSectionID != null) { json.put(CONTEXT_PAGE_URI_KEY, appSectionID); }
        if(source != null) { json.put(CONTEXT_SOURCE_KEY, source); }
        if(referrer != null) { json.put(CONTEXT_REFERRER_KEY, referrer); }
        if(component != null && component.jsonRepresentation() != null) {
            json.put(CONTEXT_COMPONENT_KEY, component.jsonRepresentation());
        }
        if (json.isEmpty()) return null;
        return json;
    }
}
