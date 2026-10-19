# Changelog

What changed in each JellyCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/JellyCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## Unreleased

### Changed

- **README brought up to date with 0.1.18.** The feature list now also mentions Live TV channels and scheduled recordings, the Wi-Fi-only download option, Favourites and photo libraries, devices and API keys in the admin dashboard, **Protect the server dashboard**, approving Quick Connect codes for other devices, and audio and subtitle language preferences. **Requirements** now says the app is for 64-bit phones and tablets (arm64-v8a, plus x86_64). **Getting started** uses the current **Sign in with a code instead** button name and explains that favourites, downloads, Live TV, settings and the server dashboard are behind your profile picture now that the You tab is gone. Nothing in the app has changed.
- Tidied the changelog: removed a doubled bullet marker in the previous version's notes.

## v0.1.18 (2026-10-19)

Fourth security and privacy review, focused on what's left on the phone and what the app sends.

### Security

- **Signing out removes that account's cached artwork.** Posters, backdrops, profile pictures and the seek-preview (trickplay) frames from a server stayed in the app's image cache after you signed out of it, or after the server rejected the login. They're now deleted from the cache when an account is removed. Your downloads aren't touched
- **Downloads in progress stay private on the lock screen.** The download notification showed the title being downloaded; on a locked phone it now only says "Downloading", with the progress bar
- **Switching usage stats off takes effect straight away.** Turning off **Settings > Usage stats** now also throws away any stats still waiting to be sent and stops one being sent at that moment, and switching them on again starts a new random session

### Changed

- **Usage stats share a little less.** Starting playback now only says whether it was video, music or Live TV, no longer whether you pressed Shuffle, so the stats match exactly what the README promises
- The README's **Usage stats** section now says that, apart from those opt-in stats, JellyCtrl only talks to your own Jellyfin server; trailers and links to sites like IMDb only open in your browser when you tap them
- **All Ctrl apps in the README.** The README's Ctrl series table now lists all 14 apps. New in the table: [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) (Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app), [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) (scan and browse documents on Paperless-ngx), [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) (manage your Proxmox VE server) and [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) (monitor and manage your TrueNAS server). Every app in the table is also in the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) for automatic updates.

## v0.1.17 (2026-10-19)

### New

- **Optional anonymous usage stats.** After you sign in, JellyCtrl asks once whether you'd like to share anonymous usage stats. Nothing is sent unless you tap **Share stats**, and you can switch it on or off any time in **Settings > Usage stats**
- **What's shared:** which screens you open (for example "Home", "Item" or "Settings", never which movie, show, library or person), whether you start video, music or Live TV, the app version, Android version, phone model, language, phone or tablet, and which video player setting you use
- **What's never shared:** your server address, username, passwords, what you watch, listen to or search for, or any ID that follows you between sessions. Each session gets a new random ID, and stats from before and after you switch them off and on again aren't linked
- **Where it goes:** to VKNXWorks' own self-hosted server (Aptabase), not to Google or any analytics company. Builds you make from source can't send stats at all, and the setting doesn't appear in them

### Changed

- **PrintCtrl in the Ctrl series list.** The README's table of Ctrl apps now includes [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl), the app for Bambu Lab printers through a self-hosted Bambuddy server, and Spoolman spools.
- The README has a new **Usage stats** section listing exactly what's collected

## v0.1.16 (2026-10-18)

A security follow-up that closes gaps in 0.1.14's protections.

### Security

- **Your password can't be redirected.** If the server (or something in front of it, like a reverse proxy) answered a sign-in, password change or new-user request with a redirect, the request was sent on again with your password in it, even to plain HTTP or to another site. Such redirects are now refused with a message, and the same check runs on every step of a redirect for images, video, music and downloads: your access token is removed before a request goes to any other address, and nothing with your token or password goes over plain HTTP unless you accepted HTTP for that server
- **Better masking in server logs and error messages.** The password-change fields ("CurrentPw", "NewPw") are now hidden, passwords and tokens in quotes are hidden in full even when they contain spaces or commas (before, only the part up to the first space was), and "Bearer" tokens are hidden too
- **Server dashboard protection can't be skipped.** The screenshot and recent-apps protection for the server dashboard now finds the app's window in every case, instead of quietly doing nothing if Android handed the screen a wrapped context

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
