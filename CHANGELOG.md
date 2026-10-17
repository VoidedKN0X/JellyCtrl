# Changelog

What changed in each JellyCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/JellyCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

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
