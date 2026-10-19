# JellyCtrl

**Your Jellyfin library, with a player that plays everything.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/JellyCtrl?label=download)](https://github.com/VoidedKN0X/JellyCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

JellyCtrl is a native Android app for [Jellyfin](https://jellyfin.org). Watch movies and shows, listen to music, watch Live TV and manage your server, on phones, tablets and foldables.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/JellyCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Plays almost anything directly**: Media3 player with an automatic mpv fallback for difficult formats and subtitles
- **Home screen** with Continue watching, Next up and the latest from each library
- **Skip intro, recap and credits**, next-episode autoplay and scrubbing previews
- **Music** with background playback, queue, shuffle and synced lyrics
- **Live TV** with channels, a guide, recordings and scheduled recordings
- **Downloads** for watching offline, with a Wi-Fi-only option
- **Favourites** and photo libraries
- **Remote control** for your other Jellyfin devices
- **Full admin dashboard**: users, libraries, plugins, scheduled tasks, logs, devices, API keys and server settings
- **Protect the server dashboard** (admins, on by default): dashboard screens can't be screenshotted or recorded, and the recent-apps preview is blank
- **Finds servers on your network**; Quick Connect sign-in, and approve Quick Connect codes for other devices; several accounts
- Picture-in-picture, swipe gestures, audio and subtitle language preferences, a separate quality limit for mobile data

## Requirements

- Android 13 or newer
- Jellyfin 10.9 or newer (10.10 or newer for skip intro)
- A 64-bit phone or tablet (arm64-v8a; x86_64 is included too)

## Install

1. Download the latest `.apk` file from the [Releases page](https://github.com/VoidedKN0X/JellyCtrl/releases/latest).
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds JellyCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/JellyCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/JellyCtrl` in Obtainium by hand.

## Getting started

1. Pick your server from the list, or enter its address.
2. Sign in with your username and password, or tap **Sign in with a code instead** to use Quick Connect.
3. Your libraries appear on the Home screen.

Favourites, downloads, Live TV, remote control, **Settings**, accounts and the server dashboard are behind your profile picture at the top right.

## Usage stats

JellyCtrl can share anonymous usage stats, **only if you say yes**. It asks once after you sign in, and you can change it any time in **Settings > Usage stats**.

- **Shared:** which screens you open (the kind of screen, never which item, library or person), whether you start video, music or Live TV, the app version, Android version, phone model, language, phone or tablet, and your video player setting.
- **Never shared:** your server address, username or passwords, what you watch, listen to or search for, or any ID that stays the same between sessions.
- **Where:** VKNXWorks' own self-hosted [Aptabase](https://aptabase.com) server. Nothing goes to Google or analytics companies. Like any website, the server sees your IP address; Aptabase uses it only to work out the country and doesn't store it.

Builds from source don't include the stats key, so they can't send anything and the setting is hidden. The code is in [`Telemetry.kt`](app/src/main/java/com/vknxworks/jellyctrl/data/telemetry/Telemetry.kt).

Apart from these opt-in stats, JellyCtrl only talks to your own Jellyfin server. Artwork comes through your server, never straight from sites like TMDB; trailers and links to sites like IMDb only open in your browser when you tap them.

## Build from source

You need [Android Studio](https://developer.android.com/studio), or JDK 17 and the Android SDK.

1. Clone the repo:
   ```bash
   git clone https://github.com/VoidedKN0X/JellyCtrl.git
   cd JellyCtrl
   ```
2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
   On Windows, use `gradlew.bat assembleDebug`.
3. The APK is in `app/build/outputs/apk/debug/`. Copy it to your phone and install it.

Or open the folder in Android Studio and press **Run** with your phone connected.

## Support the project

JellyCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). Starring the repo and [reporting bugs](https://github.com/VoidedKN0X/JellyCtrl/issues) helps too.

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| **JellyCtrl** (this app) | Movies, shows, music and Live TV from Jellyfin |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |

## License

JellyCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

The Manrope font is licensed under the SIL Open Font License; see `FONT-LICENSE-Manrope.txt`.
