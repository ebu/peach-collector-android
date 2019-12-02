package ch.ebu.peachcollector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.ebu.peachcollector.Constant.*;

public class EventContext {
    @Nullable String contextID;
    @Nullable String itemID;
    @Nullable List<String> items;
    @Nullable Number hitIndex;
    @Nullable String appSectionID;
    @Nullable String source;
    @Nullable String referrer;
    @Nullable EventContextComponent component;

    public static EventContext recommendationContext(@Nullable List<String> items,
                                                     @Nullable String appSectionID,
                                                     @Nullable String source,
                                                     @Nullable EventContextComponent component,
                                                     @Nullable Number hitIndex,
                                                     @Nullable String itemID){
        EventContext context = new EventContext();
        context.items = items;
        context.appSectionID = appSectionID;
        context.source = source;
        context.component = component;
        context.hitIndex = hitIndex;
        context.itemID = itemID;
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
    Map<String, Object> jsonRepresentation() {
        Map<String, Object> json = new HashMap<>();
        if(contextID != null) { json.put(CONTEXT_ID_KEY, contextID); }
        if(itemID != null) { json.put(CONTEXT_ITEM_ID_KEY, itemID); }
        if(items != null) { json.put(CONTEXT_ITEMS_KEY, items); }
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
