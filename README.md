# JellyCtrl

**Your Jellyfin library, with a player that plays everything.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/JellyCtrl?label=download)](https://github.com/VoidedKN0X/JellyCtrl/releases/latest) ![Android 15+](https://img.shields.io/badge/Android-15%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

JellyCtrl is a native Android app for [Jellyfin](https://jellyfin.org). Watch movies and shows, listen to music, watch Live TV and manage your server, on phones, tablets and foldables.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/JellyCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Plays almost anything directly**: Media3 player with an automatic mpv fallback for difficult formats and subtitles
- **Home screen** with Continue watching, Next up and the latest from each library
- **Skip intro, recap and credits**, next-episode autoplay and scrubbing previews
- **Search and download subtitles** from the player or the title page with your server's subtitle plugins (such as OpenSubtitles); they load straight into what you're watching
- **Music** with background playback, queue, shuffle and synced lyrics
- **Live TV** with channels, a guide, recordings and scheduled recordings
- **Downloads** for watching offline, with a Wi-Fi-only option
- **Favourites** and photo libraries
- **Remote control** for your other Jellyfin devices
- **Full admin dashboard**: users, libraries, plugins, scheduled tasks, logs, devices, API keys and server settings
- **App lock** with its own PIN and optional fingerprint unlock; your sign-ins stay encrypted while the app is locked, and music and downloads that are already running carry on
- **Hide in screenshots** and **Hide in recent apps**, two separate switches, both off by default
- **Finds servers on your network**; Quick Connect sign-in, and approve Quick Connect codes for other devices; several accounts
- **Open in ArrCtrl** from movie and series pages, to manage them in Radarr and Sonarr
- Picture-in-picture, swipe gestures, audio and subtitle language preferences, a separate quality limit for mobile data

## Requirements

- Android 15 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- Jellyfin 10.9 or newer (10.10 or newer for skip intro)
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google services, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. Open the [latest release](https://github.com/VoidedKN0X/JellyCtrl/releases/latest) on your phone. Scroll down to **Assets** and tap the file that ends in `.apk` (for example `JellyCtrl-v0.5.6.apk`) to download it. The "Source code" zip and tar.gz files are not the app.
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

## Is it safe to install?

- **Open source.** All of JellyCtrl's code is in this repo, under the GPL-3.0 licence. Anyone can read it.
- **Built by GitHub, not on a personal computer.** Every release on the Releases page is built by GitHub Actions straight from the code in this repo, with [the build workflow](.github/workflows/build.yml).
- **Always signed with the same key.** Every release is signed with the same release key. Android refuses to install an update signed with a different key over the app you have, and the CtrlF-Droid repo only accepts APKs signed with this key. Its SHA-256 certificate fingerprint is:

  ```
  70:90:2F:85:07:E5:E4:65:81:26:A7:33:25:68:38:B9:EE:59:50:F9:8C:F3:FB:CC:5A:59:EB:2D:B3:92:9C:17
  ```

  Advanced users can check it with [AppVerifier](https://github.com/soupslurpr/AppVerifier).

**Check a download**

- The APK's SHA-256 checksum is at the bottom of each release's notes. Compare it with `sha256sum JellyCtrl-vX.Y.Z.apk` (or `Get-FileHash` on Windows).
- Each release also has a signed build provenance attestation. With the [GitHub CLI](https://cli.github.com/), `gh attestation verify JellyCtrl-vX.Y.Z.apk -R VoidedKN0X/JellyCtrl` proves the APK was built by this repo's build workflow from the tagged commit, not uploaded by hand.
- The signing-certificate fingerprint above still applies: Android and AppVerifier check it on the phone.

**Permissions** JellyCtrl asks for, and what each one is for:

| Permission | What it's for |
| --- | --- |
| Internet (`INTERNET`) | Talking to your Jellyfin server |
| Network state (`ACCESS_NETWORK_STATE`) | Knowing whether you're online and on mobile data, for offline mode and the mobile data quality limit |
| Wi-Fi multicast (`CHANGE_WIFI_MULTICAST_STATE`) | Hearing the answers from Jellyfin servers on your Wi-Fi when it looks for them |
| Notifications (`POST_NOTIFICATIONS`) | The music player controls and download progress. Android asks you first. |
| Foreground service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC`) | Keeping music playing and downloads running while JellyCtrl is in the background |
| Keep awake (`WAKE_LOCK`) | Music and downloads don't stop when the screen turns off |
| Biometrics (`USE_BIOMETRIC`) | Optional fingerprint unlock for the app lock |
| Hide other apps' floating windows (`HIDE_OVERLAY_WINDOWS`) | Hide other apps' floating windows over the PIN screen, the lock screen and confirmations, so nothing can be drawn over them to trick a tap (never over video or picture-in-picture) |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Picking unfinished downloads up again after the phone restarts (added by Android's WorkManager) |

Android also lists a "dynamic receiver" permission (`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) that AndroidX adds to every app so only JellyCtrl itself can send it internal broadcasts; it gives the app no access to anything. JellyCtrl has no location, camera, microphone, contacts, storage or Wi-Fi state permissions.

See [Privacy](#privacy) for what JellyCtrl sends and to whom.

## App lock

Turn it on under your profile picture › **Settings > Privacy and security > App lock**. It's off until you turn it on.

- **Its own app PIN** of at least 6 digits, separate from your phone's screen lock. Very obvious PINs such as 111111 or 123456 are refused.
- **Unlock with fingerprint** (optional) only accepts strong biometrics, such as the fingerprint sensor, checked by the phone's secure hardware. Face unlock that only uses the camera isn't accepted. The PIN is still needed after the phone restarts and at least every 72 hours, and adding a new fingerprint to the phone turns fingerprint unlock off until you turn it on again.
- **Your sign-ins are encrypted with the PIN while JellyCtrl is locked.** The key only exists in memory while the app is unlocked and is wiped the moment it locks. The PIN itself is never stored: it's stretched with 600,000 rounds of PBKDF2 and mixed with a key held in the phone's secure hardware, so a copy of the app's files isn't enough to guess it on a computer.
- **Wrong PINs:** after 5 tries you wait 30 seconds, doubling up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't skip the wait.
- **Lock again:** as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts. Opening a trailer or link in your browser and answering the notification permission question don't count as leaving, unless you stay away for more than 5 minutes. Sending a video to picture-in-picture counts as leaving: the video keeps playing in its small window, and the lock screen shows when you open it full screen or go back to JellyCtrl.
- **Change app PIN** asks for your current PIN first, and turning the app lock off asks for the PIN too.
- **Forgot your PIN?** It can't be recovered, because it encrypts your sign-ins. The lock screen offers to remove every account and delete the downloads and cached pictures kept on the phone; nothing changes on your Jellyfin server.
- **With the app lock on**, the recent-apps screen always shows a blank card, music that's already playing (or paused with a queue) and downloads in progress carry on while JellyCtrl is locked, with their controls in the notification, on the lock screen and on Bluetooth devices: they keep the sign-in they need in memory until they stop, and anything new needs an unlock. A video waits, paused, under the lock screen, and downloads that were waiting when the phone restarted start after you unlock.
- **Nothing can be drawn over the lock screen or PIN pad.** While the lock screen, a PIN screen or a confirmation (delete, sign out, uninstall, restart the server, approve a Quick Connect sign-in, use HTTP) is showing, other apps' floating windows such as chat heads are hidden, so they can't cover a button to trick you into tapping it. Apps that aren't real accessibility tools also can't read the lock and PIN screens; screen readers such as TalkBack still work. Floating windows show again as soon as those screens close, and they're never hidden over the video player or picture-in-picture.

Two more switches in the same section work with or without the app lock, and are both off until you turn them on:

- **Hide in screenshots**: screenshots and screen recordings show a blank screen (and so does the recent-apps view).
- **Hide in recent apps**: the recent-apps view shows a blank card instead of JellyCtrl. Screenshots still work.

The sign-in screen, the Change password dialog, the server dashboard screens that show API keys, user passwords, credential settings or server logs, and the lock and PIN screens are always kept out of screenshots and recordings, whatever these switches say.

## Privacy

**Telemetry-free.** JellyCtrl has no analytics, crash reporting, usage stats or tracking of any kind. It only talks to your own Jellyfin server. Neither the video nor the music player hands playback diagnostics to Android's media diagnostics service. Finding servers sends a short "where are you?" message on your own Wi-Fi network only, which Jellyfin servers there answer. Artwork comes through your server, never straight from sites like TMDB; trailers and links to sites like IMDb only open in your browser when you tap them. Subtitle search goes through your server too: its subtitle plugins (such as OpenSubtitles) do the searching, never JellyCtrl. **Open in ArrCtrl** only passes the title's TMDB or TVDB id, and only to ArrCtrl. Your password is never stored: JellyCtrl keeps only the access token your server gives it, encrypted on the phone (and with your app PIN while the app lock is on), and only sends it to that server.

## How JellyCtrl is made

JellyCtrl is a one-person project by VoidedKN0X (VKNXWorks). Much of the code is written with the help of AI (Claude, by Anthropic). I decide what the app does, review the changes and use the app myself. Every change is listed in the [changelog](CHANGELOG.md), and you can read every line of code here.

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

Every library and Gradle plugin download is checked against the SHA-256 checksums in `gradle/verification-metadata.xml`; the build stops if anything doesn't match. When a dependency or plugin changes, run the **Dependency verification metadata** workflow (Actions tab), check the diff of the file it prints, and commit the new file in the same push. On Windows or macOS, Android's `aapt2` tool is a different download that isn't listed, so add `--dependency-verification=lenient` to local builds there.

## Support the project

JellyCtrl is free and open source. If it's useful to you, tap **Support JellyCtrl** under **About** on the You page (tap your profile picture) to sponsor development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). Found a problem or have an idea? Tap **Report a bug** in the same section, or [open an issue](https://github.com/VoidedKN0X/JellyCtrl/issues/new/choose) and pick **Bug report** or **Feature request**. Starring the repo helps too.

Found a security problem? Please don't open a public issue; see [SECURITY.md](SECURITY.md).

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CallCtrl](https://github.com/VoidedKN0X/CallCtrl) | Phone app: dialer, call history and in-call screen |
| [ChatCtrl](https://github.com/VoidedKN0X/ChatCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [ClockCtrl](https://github.com/VoidedKN0X/ClockCtrl) | Alarms, timers, stopwatch and world clock |
| [ContactCtrl](https://github.com/VoidedKN0X/ContactCtrl) | Contacts: browse, edit, import and export |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [DateCtrl](https://github.com/VoidedKN0X/DateCtrl) | Calendar for the calendars on your phone and your accounts |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| **JellyCtrl** (this app) | Movies, shows, music and Live TV from Jellyfin |
| [LijnCtrl](https://github.com/VoidedKN0X/LijnCtrl) | Live De Lijn bus and tram departures |
| [LumenCtrl](https://github.com/VoidedKN0X/LumenCtrl) | Private, offline photo and video editor |
| [MailCtrl](https://github.com/VoidedKN0X/MailCtrl) | Email for Gmail and Proton Mail |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| **RecorderCtrl** (this app) | Private, offline voice recorder |
| [RideCtrl](https://github.com/VoidedKN0X/RideCtrl) | Offline maintenance, fuel and mod log for your vehicles |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [SumCtrl](https://github.com/VoidedKN0X/SumCtrl) | Offline calculator, unit converter and work tools |
| [SynapseCtrl](https://github.com/VoidedKN0X/SynapseCtrl) | Administer your Matrix Synapse server |
| [TextCtrl](https://github.com/VoidedKN0X/TextCtrl) | Private SMS and MMS messaging |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |
| [WrtCtrl](https://github.com/VoidedKN0X/WrtCtrl) | Manage your OpenWrt router |

## License

JellyCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

JellyCtrl is an independent app and isn't made by or affiliated with the Jellyfin project.

The bundled fonts are licensed under the SIL Open Font License 1.1: [Space Grotesk](https://github.com/floriankarsten/space-grotesk) (see `FONT-LICENSE-SpaceGrotesk.txt`) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) (see `FONT-LICENSE-JetBrainsMono.txt`). Copies are also included in the app under `assets/licenses/`.
