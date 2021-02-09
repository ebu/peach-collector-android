
[![](https://jitpack.io/v/bkhezry/MapDrawingTools.svg)](https://jitpack.io/#bkhezry/MapDrawingTools)
# About

The **Peach Collector** module provides simple functionalities to facilitate the collect of events. `PeachCollector` helps you by managing a queue of events serialized until they are successfully published.

# Compatibility

The library is suitable for applications running on Android API 16 and above. The framework is built using the build tools version 29.0.2

# Setup
## 1. Provide the gradle dependency
Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
  repositories {
    ...
    maven { url "https://jitpack.io" }
  }
}
```
Add the dependency:
```gradle
dependencies {
  implementation 'com.github.ebu:peach-collector-android:1.0.0'
}
```

## 2. Initialize the collector
In your main activity, provide the application to the PeachCollector init function
```java
PeachCollector.init(getApplication());
```

## 3. Add a publisher
You need to add a `Publisher` to start sending the queued events.
You can either provide a __SiteKey__ or set a __URL address__ in order to configure the publisher.
```java
Publisher publisher = new Publisher("zzebu00000000017");
PeachCollector.addPublisher(publisher, "My Publisher");
```

## Configuring the collector

- A user ID can be defined using the **`userID`** PeachCollector property.
- If userIDs are generated automatically for anonymous user. You can use the `userIsLoggedIn` flag to define if the user is logged in or not
- For debugging purpose, a **`isUnitTesting`** flag is available. If true, notifications will be sent by the collector (see `PEACH_LOG_NOTIFICATION` constants)
- The collector retrieves the *Advertising ID* to set as the *device ID* in order to track users that do not have user IDs. People can choose to limit tracking on their devices and the Advertising ID will not be available anymore. In this case, if there is no **`userID`** defined, no events will be recorder or sent. Unless you set the **`shouldCollectAnonymousEvents`** flag to *true*. Default is *false*.
- Optionally, you can define an **`implementationVersion`** (that will be added to the request's payload).
- `inactivityInterval` is the minimum duration (in milliseconds) of inactivity that will cause the `sessionStartTimestamp` to be reset when app becomes active (default is 1800000 milliseconds, half an hour)
- `maximumStorageDays` is the maximum number of days an event should be kept in the queue (if it could not be sent).
- `maximumStoredEvents` is the maximum number of events that should be kept in the queue. 
`maximumStorageDays` and `maximumStoredEvents` should be set before the initialisation of the framework as it will be used to clean the queue during the initialisation.
- An `appID` can be defined if you don't want to use the default value (which is the bundle ID of the app).
```java
PeachCollector.isUnitTesting = true;
PeachCollector.shouldCollectAnonymousEvents = true;
PeachCollector.userID = "123e4567-e89b-12d3-a456-426655440000";
PeachCollector.implementationVersion = "1";
PeachCollector.inactivityInterval = 3600000;
PeachCollector.maximumStorageDays = 5;
PeachCollector.maximumStoredEvents = 1000;
PeachCollector.appID = "my.test.app";
```

### Configuring a Publisher
A publisher needs to be initialized with a __SiteKey__ or a full __URL address__ as seen previously.
But it has 4 others properties that are worth mentioning :

**`interval`**: The interval in seconds at which events are sent to the server (interval starts after the first event is queued). Default is 20 seconds.

**`maxEventsPerBatch`**: Number of events queued that triggers the publishing process even if the desired interval hasn't been reached. Default is 20 events.

**`maxEventsPerBatchAfterOfflineSession`**: Maximum number of events that can be sent in a single batch. Especially useful after a long offline session. Default is 1000 events.

**`gotBackPolicy`**: How the publisher should behave after an offline period. Available options are `SendAll` (sends requests with **`maxEventsPerBatchAfterOfflineSession`** continuously), `SendBatchesRandomly` (separates requests by a random delay between 0 and 60 seconds).

```java
publisher.serviceURL = "http://newEndPoint.com";
publisher.interval = 30;  
publisher.maxEventsPerBatch = 50;
publisher.maxEventsPerBatchAfterOfflineSession = 500;
```

### Flushing and Cleaning

**`Flush`** is called when the application is about to go to background, or if a special type of event is sent while in background (events that will potentially push the application into an inactive state). It will try to send all the queued events (even if the maximum number of events hasn't been reached)

**`Clean`** will simply remove all current queued events. It is never called in the life cycle of the framework.

`Flush` and `Clean` can be called manually.


### Recording an Event

```java
// page view event

Event.sendPageView("page00"+i, null, "reco00");


// recommendation displayed event

EventContextComponent carouselComponent = new EventContextComponent();  
carouselComponent.type = "Carousel";  
carouselComponent.name = "recoCarousel";  
carouselComponent.version = "1.0";  

ArrayList<String> items = new ArrayList<>();  
items.add("media00");  
items.add("media01");  
items.add("media02");  
items.add("media03");  

Event.sendRecommendationDisplayed("reco00", items , null, null, carouselComponent);

```
