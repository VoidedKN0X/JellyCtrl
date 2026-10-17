# JellyCtrl

**Your Jellyfin library, with a player that plays everything.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/JellyCtrl?label=download)](https://github.com/VoidedKN0X/JellyCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

JellyCtrl is a native Android app for [Jellyfin](https://jellyfin.org). Watch movies and shows, listen to music, watch Live TV and manage your server, on phones, tablets and foldables.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/JellyCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="48"></a>

## Features

- **Plays almost anything directly**: Media3 player with an automatic mpv fallback for difficult formats and subtitles
- **Home screen** with Continue watching, Next up and the latest from each library
- **Skip intro, recap and credits**, next-episode autoplay and scrubbing previews
- **Music** with background playback, queue, shuffle and synced lyrics
- **Live TV** with a guide and recordings
- **Downloads** for watching offline
- **Remote control** for your other Jellyfin devices
- **Full admin dashboard**: users, libraries, plugins, scheduled tasks, logs and server settings
- **Finds servers on your network**; Quick Connect sign-in; several accounts
- Picture-in-picture, gestures, quality limits for mobile data

## Requirements

- Android 13 or newer
- Jellyfin 10.9 or newer (10.10 or newer for skip intro)

## Install

1. Download the latest `.apk` file from the [Releases page](https://github.com/VoidedKN0X/JellyCtrl/releases/latest).
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates with Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium), then tap the button below on your phone. Obtainium adds JellyCtrl and installs every new release for you.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/JellyCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="48"></a>

If the button doesn't open Obtainium, add `https://github.com/VoidedKN0X/JellyCtrl` in Obtainium by hand.

## Getting started

1. Pick your server from the list, or enter its address.
2. Sign in with your username and password, or use **Quick Connect**.
3. Your libraries appear on the Home screen.

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
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| **JellyCtrl** (this app) | Movies, shows, music and Live TV from Jellyfin |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |

## License

JellyCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

The Manrope font is licensed under the SIL Open Font License; see `FONT-LICENSE-Manrope.txt`.
