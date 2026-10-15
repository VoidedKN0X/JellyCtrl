# JellyCtrl

A native Android client for [Jellyfin](https://jellyfin.org), by VKNXWorks. Built with Kotlin and Jetpack Compose, for Android 13 and newer, with layouts that adapt to phones, tablets and foldables.

## Features

**Connecting**
- Finds servers on your network automatically, or enter any address (http/https, custom ports, reverse-proxy paths)
- Sign in with username/password or Quick Connect, pick from the server's public user list
- Multiple accounts and servers, switch without signing out
- Access tokens stored encrypted with the Android Keystore
- Approve Quick Connect codes for other devices

**Browsing**
- Home: Continue watching, Next up, Live TV on now, Continue listening, Latest per library
- Libraries with sort (name, date added, release, ratings, runtime, play count, random), filters (played, unplayed, favorites, in progress), genres, shuffle
- Movies, shows, seasons, episodes, collections, playlists, music videos, home videos, photos, books, folders
- Detail pages with backdrop/logo, ratings, cast & crew, chapters, extras, trailers, similar items, media info, external links
- People pages with filmography, genre and studio pages
- Search across every item type, Favorites
- Mark played/unplayed, favorite, add to playlist, long-press menus everywhere

**Video playback**
- Media3 (ExoPlayer) with an automatic **mpv fallback** for anything Media3 can't handle (styled ASS subtitles, DTS/TrueHD, uncommon containers, missing hardware decoders) - direct play instead of transcoding
- If a player fails, it retries with the other engine, then with server transcoding
- Choose version, audio track and subtitles before playing or during playback; external subtitles side-loaded
- Quality/bitrate limits (separate limit on mobile data) with server transcoding (HLS)
- Skip intro / recap / credits using Jellyfin media segments (button or automatic)
- Next episode countdown and autoplay, play all / shuffle for series, seasons, collections and playlists
- Trickplay thumbnail previews while scrubbing, chapter names on the seek bar
- Gestures: double-tap to seek, swipe for brightness/volume/seeking
- Picture-in-picture (auto on leave), rotation lock, playback speed, fit/crop/stretch
- Progress, pause and stop reported to the server so watch status syncs everywhere
- Live TV channels with transcoding/live streams

**Music**
- Albums, album artists, songs, playlists, genres
- Background playback with notification, lock-screen and Bluetooth controls
- Queue (jump to any track, remove, add to queue), shuffle, repeat, instant mix
- Synced lyrics (tap a line to seek)

**Live TV**
- Channels with what's on now, 12-hour guide, schedule/cancel recordings, recordings library

**Downloads**
- Movies, episodes, whole seasons/series, albums for offline use
- Resumable, Wi-Fi only option, progress notifications; offline files play in the right engine

**Remote control**
- See other Jellyfin devices on your account and control them (play/pause, stop, next/previous, send messages)

**Admin dashboard** (administrator accounts)
- Server info, restart, shut down, scan all libraries, active sessions with transcode info, running tasks
- Users: create, rename, delete, set password, every permission, library access
- Libraries: add (with a server folder browser), rename, add/remove folders, scan, remove
- Scheduled tasks: run, stop, progress
- Plugins: enable, disable, uninstall, install from the catalog
- Logs viewer with filter and copy, activity log, devices (sign out), API keys
- General, playback/transcoding, networking, metadata, Live TV and branding settings
- Edit an item's metadata, refresh metadata, delete items

**Settings**
- Player engine (automatic, Media3 only, mpv only), quality limits, skip behaviour, seek step, gestures, PiP, mpv hardware decoding
- Your Jellyfin audio/subtitle language and subtitle mode preferences
- Music quality, Wi-Fi-only downloads, theme (system, dark, light, pure black), Material You colors

## Building

GitHub Actions builds a release APK on every push to `main` and publishes it to the repo's Releases page (`.github/workflows/build.yml`). You can also run it manually from the Actions tab.

Locally: Android Studio (or JDK 17 + Android SDK 36) and `./gradlew assembleRelease`.

### Release signing

Until a release key is configured, CI signs the APK with a throwaway debug key (the workflow shows a warning). Debug-signed and release-signed builds can't update each other, so set the key up before you rely on updates.

1. Create a keystore once:
   ```
   keytool -genkeypair -v -keystore jellyctrl-release.jks -alias jellyctrl -keyalg RSA -keysize 4096 -validity 10000
   ```
2. In the repo go to **Settings → Secrets and variables → Actions** and add:
   - Secret `KEYSTORE_BASE64` - output of `base64 -w0 jellyctrl-release.jks`
   - Secret `KEYSTORE_PASSWORD`
   - Secret `KEY_PASSWORD`
   - Variable `KEY_ALIAS` (e.g. `jellyctrl`)
3. Keep the `.jks` file and passwords somewhere safe - losing them means users must reinstall to update.

## Requirements

- Android 13+ (arm64 or x86_64)
- Jellyfin server 10.9 or newer (10.10+ for skip intro / media segments)
