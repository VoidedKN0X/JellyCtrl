# Changelog

What changed in each JellyCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/JellyCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.1.15 (2026-10-18)

### Changed

- The **You** tab is gone from the bar at the bottom. Tap your profile picture at the top right of Home, Search or Library to open it instead. Everything that was there (favourites, downloads, Live TV, remote control, the server dashboard, settings, accounts and your password) is still on that page, now with a back arrow
- The bottom bar now has three tabs: Home, Search and Library. On tablets and unfolded foldables the side rail matches
- Search and Library now show your Jellyfin profile picture (or your initial if you haven't set one) next to their titles, like Home already did

## v0.1.14 (2026-10-18)

A second security pass, mostly around the server dashboard.

### New

- **Protect the server dashboard** (Settings > Privacy, on by default for administrators): while a dashboard screen is open, it can't be captured by screenshots or screen recording, and the recent-apps preview is blank. API keys, logs, users and server settings stay off your screen captures.

### Security

- The server log viewer hides access tokens, API keys and passwords that appear in logged request addresses and headers, and copied logs are hidden from clipboard previews.
- Password, secret and key fields in server settings (for example the HTTPS certificate password) are masked, with a button to show them.
- Setting a user's password with the box left blank now clearly says it removes the password, and the button reads "Remove password".
- Password boxes in the dashboard no longer let the keyboard learn or suggest what you type.
- Approving a Quick Connect code asks you to confirm first and warns never to use a code someone sent you (for administrators: it would give them control of the server).
- Signing in with Quick Connect stops waiting after 10 minutes instead of polling forever.
- Website and trailer links from the server only open in a browser; other kinds of links in item details are ignored.
- The mpv player only plays web streams from the server and your own downloads, never other local files or special mpv addresses, and it never runs scripts or tries to fetch streams through youtube-dl.
- Your password is never sent over plain HTTP to a server you haven't agreed to use without encryption, even before you're signed in.
- Installing a plugin from the catalog reminds you that plugins get full access to the server.
- Nothing from the app, including downloads, is copied when you move to a new phone.

### Fixed

- Tapping back several times quickly, or while a screen was still sliding in, could close one screen too many and leave a black screen. Repeated taps are now ignored until the screen has finished changing, and the home screen is never closed.
- Saving metadata, deleting an item or user, or approving a Quick Connect code no longer closes a different screen if you had already gone back before the server answered.

## v0.1.13 (2026-10-18)

- Version numbers are now plain, with no build number on the end, and a new version only comes out when something in the app actually changes. Nothing else in the app has changed.

## v0.1.10 (2026-10-16)

Security hardening.

- Your access token is sent in a header only to your own server, and never appears in stream, download or artwork links or in error messages
- The mpv player always checks server certificates (including ones you've installed yourself)
- Sign-in no longer quietly falls back from HTTPS to HTTP; HTTP accounts are marked "Not encrypted (HTTP)"
- Safer download file names (existing downloads keep their names)
- Confirmation before removing a library folder, signing out a device or revoking an API key
- Copied API keys are hidden from clipboard previews
- Only this app, trusted controllers, Bluetooth and Android Auto can control music playback

## v0.1.9 (2026-10-16)

- Fixed: videos briefly showed "Live" before the player knew their length

## v0.1.7 (2026-10-16)

- **Floating glass tab bar** on phones: a frosted-glass island with a sliding selection, a glass mini player above it, and content scrolling underneath. Wide screens get a side rail

## v0.1.6 (2026-10-16)

- **New app icon**: a widescreen key, with a themed (monochrome) version and a matching notification icon

## v0.1.5 (2026-10-16)

- **Redesign**: cleaner and simpler, with the Manrope typeface, a graphite palette and accent colours taken from the artwork
- Four tabs, a hero home screen, immersive detail pages, minimal player controls and grouped settings

## v0.1.2 (2026-10-15)

First release: a Jellyfin client for Android.

- Browse movies, shows, music and Live TV
- Media3 video player with an mpv fallback for tricky files
- Downloads, remote control of other devices, and a server admin dashboard
- Phone and tablet / foldable layouts
