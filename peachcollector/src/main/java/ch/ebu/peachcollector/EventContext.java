package ch.ebu.peachcollector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.ebu.peachcollector.Constant.*;

public class EventContext {
    @Nullable String contextID;
    @Nullable String type;
    @Nullable String itemID;
    @Nullable List<String> items;
    @Nullable Number hitIndex;
    @Nullable Number itemIndex;
    @Nullable Number itemsCount;
    @Nullable String appSectionID;
    @Nullable String source;
    @Nullable String referrer;
    @Nullable EventContextComponent component;
    @Nullable String experimentID;
    @Nullable String experimentComponent;

    public static EventContext collectionContext(@Nullable List<String> items,
                                                 @Nullable String appSectionID,
                                                 @Nullable String source,
                                                 @Nullable EventContextComponent component,
                                                 @Nullable Number hitIndex,
                                                 @Nullable String itemID,
                                                 @Nullable Number itemIndex,
                                                 @Nullable Number itemsCount,
                                                 @Nullable String experimentID,
                                                 @Nullable String experimentComponent){
        EventContext context = new EventContext();
        context.items = items;
        context.appSectionID = appSectionID;
        context.source = source;
        context.component = component;
        context.hitIndex = hitIndex;
        context.itemID = itemID;
        context.itemIndex = itemIndex;
        context.itemsCount = itemsCount;
        if (experimentID != null) {
            context.experimentID = experimentID;
        }
        else {
            context.experimentID = "default";
        }
        if (experimentComponent != null) {
            context.experimentComponent = experimentComponent;
        }
        else {
            context.experimentComponent = "main";
        }
        return context;
    }

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

    public static EventContext mediaContext(@NonNull String contextID,
                                            @NonNull String type,
                                            @Nullable String appSectionID,
                                            @Nullable String source,
                                            @Nullable EventContextComponent component){
        EventContext context = EventContext.mediaContext(contextID, appSectionID, source, component);
        context.type = type;
        return context;
    }

    @Nullable private Map<String, Object> customFields;

    private void addObject(String key, Object value) {
        if (value == null) {
            remove(key);
            return;
        }

        if (customFields == null) {
            customFields = new HashMap<>();
        }

        customFields.put(key, value);
    }

    /**
     * Add a custom string field to the context
     */
    public void add(String key, String value){
        addObject(key, value);
    }

    /**
     * Add a custom number field to the context
     */
    public void add(String key, Number value){
        addObject(key, value);
    }

    /**
     * Add a custom boolean field to the context
     */
    public void add(String key, Boolean value){
        addObject(key, value);
    }

    /**
     * Remove a custom field previously added
     */
    public void remove(String key){
        if (customFields != null && customFields.containsKey(key)){
            customFields.remove(key);
            if (customFields.size() == 0) customFields = null;
        }
    }

    /**
     * Retrieve a custom field previously added.
     * @return null if the field was not found
     */
    @Nullable public Object get(String key){
        if (customFields != null) return customFields.get(key);
        return null;
    }

    @Nullable
    Map<String, Object> jsonRepresentation() {
        Map<String, Object> json = new HashMap<>();
        if(contextID != null) { json.put(CONTEXT_ID_KEY, contextID); }
        if(type != null) { json.put(CONTEXT_TYPE_KEY, type); }
        if(itemID != null) { json.put(CONTEXT_ITEM_ID_KEY, itemID); }
        if(items != null) { json.put(CONTEXT_ITEMS_KEY, items); }
        if(hitIndex != null) { json.put(CONTEXT_HIT_INDEX_KEY, hitIndex); }
        if(itemIndex != null) { json.put(CONTEXT_ITEM_INDEX_KEY, itemIndex); }
        if(itemsCount != null) { json.put(CONTEXT_ITEMS_COUNT_KEY, itemsCount); }
        if(appSectionID != null) { json.put(CONTEXT_PAGE_URI_KEY, appSectionID); }
        if(source != null) { json.put(CONTEXT_SOURCE_KEY, source); }
        if(referrer != null) { json.put(CONTEXT_REFERRER_KEY, referrer); }
        if(component != null && component.jsonRepresentation() != null) {
            json.put(CONTEXT_COMPONENT_KEY, component.jsonRepresentation());
        }
        if(experimentID != null) { json.put(CONTEXT_EXPERIMENT_ID_KEY, experimentID); }
        if(experimentComponent != null) { json.put(CONTEXT_EXPERIMENT_COMPONENT_KEY, experimentComponent); }
        if(customFields != null) {
            for (Map.Entry<String, Object> entry : customFields.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        }
        if (json.isEmpty()) return null;
        return json;
    }
}
