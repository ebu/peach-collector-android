[![](https://jitpack.io/v/bkhezry/MapDrawingTools.svg)](https://jitpack.io/#bkhezry/MapDrawingTools)
## About

The **Peach Collector** library provides simple functionalities to facilitate the collect of events. `PeachCollector` helps you by managing a queue of events serialized until they are successfully published.

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
  implementation 'com.github.ebu:peach-collector-android:0.1-beta'
}
```

## 2. Initialize the collector
```java
PeachCollector.init(getApplicationContext());  
```

## 3. Add a publisher
```java
PeachCollector.addPublisher(new Publisher("zzebu00000000017"), "My Publisher");
```
