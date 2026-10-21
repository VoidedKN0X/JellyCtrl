# Changelog

What changed in each JellyCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/JellyCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.2.0 (2026-10-21)

JellyCtrl gets the app lock every Ctrl app shares.

### New

- **App lock** (**Settings > Privacy and security > App lock**, off until you turn it on). JellyCtrl asks for its own app PIN when you open it: at least 6 digits, separate from your phone's screen lock, and very obvious ones (like 111111 or 123456) are refused. Turning the lock off asks for the PIN too.
- **Unlock with fingerprint** (optional). Only strong biometrics such as the fingerprint sensor count, not face unlock that only uses the camera. The PIN is still needed after the phone restarts and at least every 72 hours.
- **Lock again**: as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts. Opening a trailer or link in your browser or answering the notification permission question doesn't count as leaving, unless you stay away for more than 5 minutes.
- **Change app PIN** in the same place; it asks for your current PIN first.
- **Music and downloads keep going while JellyCtrl is locked.** Music that's playing (or paused with a queue) and downloads in progress carry on, with their controls in the notification, on the lock screen and on Bluetooth devices. A video you were watching waits, paused, under the lock screen. Starting something new needs an unlock. Downloads that were still waiting when the phone restarted start once you unlock.
- **Hide in screenshots and recent apps** (**Settings > Privacy and security**): screenshots and screen recordings of JellyCtrl, including the video player, come out black, and the recent-apps screen shows a blank card. Off by default. With the app lock on, the recent-apps screen always shows a blank card.

### Changed

- **Settings are in the same order as in the other Ctrl apps**: Appearance and Privacy and security first, then Watching, Player, Audio and subtitles, and Music and downloads.
- **Protect the server dashboard** (admins) moved into **Privacy and security**, next to the new options. It still works the same and is still on by default.

### Security

- **Your sign-ins are encrypted with your PIN while JellyCtrl is locked.** The key that decrypts them only exists in memory while the app is unlocked and is wiped the moment it locks. A locked app, or a copy of its files, can't use your access tokens. Only music or downloads that are already running keep the sign-in they need in memory, until they stop.
- **The PIN can't be guessed off the phone.** It's never stored. It's stretched with 600,000 rounds of PBKDF2 and mixed with a secret held in the phone's secure hardware (StrongBox where available), so copying JellyCtrl's files to a computer isn't enough to try PINs there.
- **Wrong PINs lock the screen**: after 5 tries you wait 30 seconds, doubling with every further 5 wrong tries up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't reset the wait.
- **Fingerprint unlock can't be faked**: the fingerprint has to unlock a key in the phone's secure hardware, so a tampered "success" on a rooted phone doesn't open the app. Adding a new fingerprint turns fingerprint unlock off until you turn it on again with your PIN.
- **Forgot your PIN?** The lock screen offers to remove every account and server, and delete the downloads and cached pictures kept on the phone. Because the PIN encrypts your sign-ins, there's no way around it. Nothing changes on your Jellyfin server.
- **Password screens stay out of screenshots**: the sign-in screen and the Change password dialog can't be captured by screenshots or screen recording, and the recent-apps preview is blank while they're open.

### Fixed

- **README**: the Ctrl series table lists AdCtrl and MemoCtrl, and the README now describes the app lock and the local-network server search.

## v0.1.26 (2026-10-20)

### Changed

- **64-bit ARM phones only.** JellyCtrl now ships only the 64-bit ARM (arm64-v8a) build that nearly every phone from the last several years uses, like every Ctrl app, so the download is smaller. Old 32-bit phones and x86 devices (emulators, some Chromebooks) are no longer supported.
- **README**: Requirements say 64-bit ARM phones only

## v0.1.25 (2026-10-20)

JellyCtrl is now Google-free and de-Googled.

### Changed

- **Media controls trust the apps built into your phone.** JellyCtrl's music player used to accept controls only from a fixed list of named apps (system UI, Bluetooth, car mode, a car assistant and voice controls), some of them Google's. It now accepts the apps that are part of your phone's system, plus apps Android itself marks as trusted for media control, so the notification, lock screen, Bluetooth and car controls keep working as before, also on de-Googled phones whose built-in apps have different names. JellyCtrl checks which app is really asking before it trusts it, and apps you installed yourself still can't control playback.
- **README**: the Google-free line now reads "Google-free and de-Googled": no Google Play services, Firebase or other Google services, the app never contacts Google, and it works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps). It's the only place Google is mentioned in the README.

### Privacy

- **No mention of Google left in the app's own code.** JellyCtrl has no Google Play services, Firebase or other Google services and never contacts Google: it only talks to your Jellyfin server. The list of named Google apps in the music player and a technical note in its setup file are gone. It works the same on de-Googled phones such as GrapheneOS, CalyxOS, /e/OS and LineageOS without Google apps.

## v0.1.24 (2026-10-20)

### Privacy

- **No more usage stats: JellyCtrl is now telemetry-free.** The optional anonymous usage stats are gone completely, along with the Usage stats setting and the one-time question. The app no longer contacts VKNXWorks' stats server at all, and the settings it used are deleted from your phone when you update. JellyCtrl now only talks to your own Jellyfin server.

### Changed

- **README**: a Screenshots section (screenshots are coming soon), and a link preview image (`.github/assets/social-preview.png`) with JellyCtrl's icon, name and summary, for when the repo is shared on Reddit, Discord or elsewhere
- **README**: a Privacy section and a Telemetry-free badge replace the Usage stats section, and the link preview image has a Telemetry-free tag

## v0.1.23 (2026-10-19)

### Fixed

- **Play in JellyCtrl from ArrCtrl works.** Opening a movie or episode from ArrCtrl always failed: JellyCtrl showed "Couldn't look it up in Jellyfin: The coroutine scope left the composition" (or, in 0.1.20, said the item wasn't in your library). JellyCtrl was cancelling its own lookup the moment it started. The lookup now runs to the end and opens the movie or episode page

### Changed

- **One About section in every Ctrl app**: Support, Report a bug, Source code and Licence (GPL-3.0), with the version underneath, at the bottom of the You page (tap your profile picture). Every Ctrl app now has the same four rows, in the same order, with the same wording and icons
- **Report a bug** is a new row in **About** that opens JellyCtrl's GitHub issues page, where you can report a problem or suggest an idea
- **Source code and issues** is now called **Source code**, since reporting bugs has its own row. It still opens JellyCtrl's GitHub repository
- The line under **About** now reads "JellyCtrl 0.1.23 · VKNXWorks", the same as in the other Ctrl apps

## v0.1.22 (2026-10-19)

### New

- **Support JellyCtrl** under **About** at the bottom of the You page (tap your profile picture) opens VKNXWorks' GitHub Sponsors page in your browser. JellyCtrl is free and open source, and sponsoring helps keep it that way
- **Source code and issues** in the same **About** section opens JellyCtrl's GitHub repository, where you can read the code or report a bug
- **Licence** in the same section opens the GNU General Public License v3.0 that JellyCtrl is released under

## v0.1.21 (2026-10-19)

### Fixed

- **Play in JellyCtrl from ArrCtrl now finds your movies and episodes.** JellyCtrl said a movie or series wasn't in your library even when it was. It only looked for a TMDB id (movies) or a TVDB id (series), and recent Jellyfin versions often don't store TVDB ids at all. It now searches by title first and matches on any id ArrCtrl sends (TMDB, TVDB or IMDb); if none match, it takes the one item with the same title and year. Only then does it go through the whole library by id. Use it with ArrCtrl 1.0.1 or newer, which sends the title, year and all ids; links from older ArrCtrl versions still work as before
- **A clear message when the lookup fails.** If Jellyfin can't be reached, JellyCtrl now says so (with the reason) instead of claiming the movie or series isn't in your library, and the "couldn't find" message names the movie or series

## v0.1.20 (2026-10-19)

### New

- **Play in JellyCtrl from ArrCtrl.** The "Play in JellyCtrl" button on a movie or episode in [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) now opens that movie or episode's page in JellyCtrl, ready to play, instead of just starting the app. JellyCtrl finds it in your library by its TMDB id (movies) or TVDB id, season and episode number (episodes). If the episode isn't there yet you get the series page, and if the movie or series isn't in your library a short message says so
- Other apps can use the same `jellyctrl://item?type=movie&tmdb=…` and `jellyctrl://item?type=episode&tvdb=…&season=…&episode=…` links. They only open a page, never start playback by themselves, and web pages can't use them

## v0.1.19 (2026-10-19)

JellyCtrl is now completely Google-free. Nothing changes in how you use it.

### Privacy

- **Emoji no longer make JellyCtrl ask Google for a font.** A standard Android library inside the app asked Google Play services for a downloadable emoji font every time JellyCtrl started, which could make Google Play services fetch that font from Google. This lookup is now switched off: emoji are drawn with your phone's own emoji font. On phones without Google Play services nothing changes; on other phones, emoji can look slightly different if the phone's built-in emoji font is older.
- **Checked: JellyCtrl is Google-free.** It contains no Google Play services, Firebase, ML Kit, Google Cast or Play Core, and it doesn't load fonts or anything else from Google's servers. It also never switches server addresses by itself: it always uses the address you set up, and only you can change it.

### Changed

- **README: new Google-free line** under Requirements: works without Google Play services, contains no Google libraries that contact Google, and draws emoji with your phone's own emoji font.
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
