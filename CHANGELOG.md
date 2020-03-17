
# Change Log
All notable changes to this project will be documented in this file.

## [1.0.5] - 2020-03-16

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
