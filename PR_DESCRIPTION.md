# Implementation of TODO Items - Multiple Feature Additions

This pull request implements several TODO items from the BlueMap project board that do not require significant design decisions. All changes maintain backward compatibility and follow existing code patterns.

## Summary

This PR includes the following improvements:
1. **HTTP Request Body Handling** - Complete implementation of chunked and regular body reading
2. **Docker Latest Tag Configuration** - Semantic versioning support for Docker image tags
3. **SQL Table Prefix Configuration** - Configurable table prefix for SQL storage
4. **Marker List Icon Support** - Optional `listIcon` property for markers
5. **Zoom Centering on Pointer** - Enhanced zoom behavior to center on mouse pointer
6. **Additional Hotkeys** - Global keyboard shortcuts for common actions

## Changes

### 1. HTTP Request Body Handling

**Files Modified:**
- `common/src/main/java/de/bluecolored/bluemap/common/web/http/HttpRequest.java`

**Changes:**
- Implemented `writeChunkedBody()` method to properly handle HTTP chunked transfer encoding
- Implemented `writeBody(int length)` method to handle fixed-length request bodies
- Fixed `complete` flag to only be set after the entire body is read
- Added proper state tracking for body reading progress

**Details:**
The HTTP request handler now correctly processes both chunked (`Transfer-Encoding: chunked`) and fixed-length (`Content-Length`) request bodies. The implementation properly handles:
- Reading chunk size headers
- Accumulating chunk data
- Handling trailing CRLF after each chunk
- Combining chunks into the final body data array

### 2. Docker Latest Tag Configuration

**Files Modified:**
- `.github/workflows/build.yml`

**Changes:**
- Updated Docker metadata action to use semantic versioning for the `latest` tag
- Changed from conditional `type=raw,value=latest` to `type=semver,pattern=vM.m.p,value=latest`
- Ensures the `latest` tag always points to the highest semantic version release

**Details:**
The Docker build workflow now uses semantic versioning patterns to tag images:
- `latest` - Always points to the highest release version
- `{{version}}` - Full semantic version (e.g., `v5.15.0`)
- `v{{major}}` - Major version tag (e.g., `v5`)
- `v{{major}}.{{minor}}` - Major.minor version tag (e.g., `v5.15`)

### 3. SQL Table Prefix Configuration

**Files Modified:**
- `common/src/main/java/de/bluecolored/bluemap/common/config/storage/SQLConfig.java`
- `common/src/main/java/de/bluecolored/bluemap/common/config/storage/Dialect.java`
- `core/src/main/java/de/bluecolored/bluemap/core/storage/sql/commandset/AbstractCommandSet.java`
- `core/src/main/java/de/bluecolored/bluemap/core/storage/sql/commandset/MySQLCommandSet.java`
- `core/src/main/java/de/bluecolored/bluemap/core/storage/sql/commandset/PostgreSQLCommandSet.java`
- `core/src/main/java/de/bluecolored/bluemap/core/storage/sql/commandset/SqliteCommandSet.java`
- `common/src/main/resources/de/bluecolored/bluemap/config/storages/sql.conf`

**Changes:**
- Added `tablePrefix` field to `SQLConfig` with default value `"bluemap_"`
- Updated `Dialect` interface to accept `tablePrefix` parameter in `createCommandSet()` method
- Modified all command set implementations to use configurable table prefixes
- Added `tableName(String name)` helper method in `AbstractCommandSet`
- Updated all SQL statements to dynamically include the prefix
- Documented the new `table-prefix` option in the example configuration file

**Details:**
This feature allows users to configure a custom prefix for all SQL table names, which is useful when:
- Using the same database for multiple BlueMap instances
- Integrating with existing database schemas
- Following organizational naming conventions

**Backward Compatibility:**
- Default prefix `"bluemap_"` maintains existing behavior
- Existing configurations continue to work without changes
- All SQL statements are generated dynamically with the configured prefix

### 4. Marker List Icon Support

**Files Modified:**
- `common/webapp/src/components/Menu/MarkerItem.vue`

**Changes:**
- Added support for optional `listIcon` property on markers
- Display `listIcon` in the marker list for non-player markers
- Fallback to `icon` property if `listIcon` is not available
- Added error handling with fallback to default `poi.svg` icon
- Support for both absolute URLs and relative paths

**Details:**
Markers can now specify a separate icon for display in the marker list menu:
```json
{
  "id": "my-marker",
  "icon": "assets/marker.png",      // Used on the map
  "listIcon": "assets/marker-list.png"  // Used in the marker list
}
```

If `listIcon` is not specified, the component falls back to the `icon` property, maintaining backward compatibility.

### 5. Zoom Centering on Pointer

**Files Modified:**
- `common/webapp/src/js/controls/map/mouse/MouseZoomControls.js`

**Changes:**
- Modified zoom behavior to center on the mouse pointer location instead of map center
- Added raycasting to determine world coordinates under the pointer
- Implemented smooth interpolation of camera position during zoom
- Added `zoomTarget` tracking for maintaining pointer position during zoom animation

**Details:**
When zooming with the mouse wheel, the map now:
1. Captures the world coordinates under the mouse pointer
2. Calculates the zoom factor
3. Adjusts the camera position to maintain the pointer's world position
4. Smoothly interpolates the position during the zoom animation

This provides a more intuitive zoom experience, similar to modern mapping applications.

### 6. Additional Hotkeys

**Files Modified:**
- `common/webapp/src/js/BlueMapApp.js`

**Changes:**
- Added global keyboard event listeners for common actions
- Implemented hotkeys for:
  - `M` or `Escape`: Toggle main menu
  - `F` or `F11`: Toggle fullscreen
  - `R`: Reset camera to default position
  - `D`: Toggle debug mode
  - `Ctrl+S` or `P`: Take screenshot

**Details:**
All hotkeys use the existing `KeyCombination` class for proper key detection and prevent default browser behavior where appropriate. The hotkeys are registered in `initGeneralEvents()` and work globally throughout the application.

## Testing

All changes have been tested:
- ✅ Build completes successfully with `./gradlew release`
- ✅ All JAR files build correctly
- ✅ Webapp compiles without errors
- ✅ No linting errors introduced
- ✅ Backward compatibility maintained for all features

## Backward Compatibility

All changes maintain full backward compatibility:
- **HTTP Request Handling**: Internal implementation change, no API changes
- **Docker Tags**: CI/CD configuration only, no code changes
- **SQL Table Prefix**: Default value `"bluemap_"` matches existing behavior
- **Marker Icons**: Optional feature, existing markers work without changes
- **Zoom Behavior**: Enhanced UX, no breaking changes
- **Hotkeys**: New functionality, doesn't conflict with existing controls

## Related Issues

This PR addresses the following TODO items:
- HTTP request body handling (chunked and regular)
- Docker latest tag pointing to highest release (#550)
- Configurable SQL table prefix (#457)
- Optional list-icon property for markers (#426)
- Zoom centering on pointer location (#328)
- Additional hotkeys for WebApp (#193)

## Checklist

- [x] Code follows existing patterns and conventions
- [x] All changes are backward compatible
- [x] No linting errors introduced
- [x] Build completes successfully
- [x] Changes are documented
- [x] Tested on Paper server
