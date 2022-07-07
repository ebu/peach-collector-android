
# Change Log
All notable changes to this project will be documented in this file.

## [1.1.2] - 2022-06-07

### Added
- Possibility to add custom fields to the payload's client description. It can be done by configuring the `Publisher`

## [1.1.1] - 2022-03-11

### Changed
- `userIsLoggedIn` is now private. Added setter that will make sure the value is updated in all publishers' client data.

## [1.1.0] - 2021-11-24

### Added
- Remote configuration for Publisher initialisation

## [1.0.13] - 2021-08-11

### Added
- `collectionItemDisplayed` event and updated collection context.

## [1.0.12] - 2021-07-19

### Added
- `collectionLoaded`, `collectionDisplayed` and `collectionHit` events with a specific collection context.

## [1.0.11] - 2021-03-25

### Added
- `setDeviceID` function in the `PeachCollector`. If deviceID is set before initialization, PeachCollector will not try to retrieve the advertising ID.

## [1.0.10] - 2021-02-05

### Added
- `appID` can be defined in the `PeachCollector`. The default value is the Package Name of your app.

## [1.0.9] - 2020-11-05

### Added
- `userIsLoggedIn` flag in `PeachCollector` to help when userIDs are generated automatically for anonymous users

## [1.0.8] - 2020-07-01

### Fixed
- Exception when first custom field added is null (in EventProperties and EventContext)

## [1.0.7] - 2020-04-22

### Added
- Fallbacks for when `PeachCollector` is not initialised

## [1.0.6] - 2020-03-16

### Added
- **`maximumStoredEvents`** field in `PeachCollector` to limit queue size
- **`maximumStorageDays`**  field in `PeachCollector` to limit queue size

## [1.0.4] - 2020-01-31

### Added
- **`isPlaying`** field in `EventProperties`, for media_seek events
- **`type`**  field in `EventContext`
- possibility to add, retrieve and remove custom fields from `EventProperties` and `EventContext`


## [1.0.3] - 2020-01-28

### Added
- `media_playlist_add` and `media_playlist_remove` events helpers
- **`playlistID`** and **`insertPosition`** properties in `EventProperties`, for media events related to a playlist addition or removal


## [1.0.2] - 2020-01-09

### Added
- **`content-type`** value to request's header because it's not automatically set on old android versions
