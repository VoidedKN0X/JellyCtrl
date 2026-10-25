# Changelog

What changed in each JellyCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/JellyCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.5.2 (2026-10-25)

Music stops properly when you close JellyCtrl.

### Fixed

- **Closing JellyCtrl now stops paused music and removes its notification.** When you swiped JellyCtrl away from the recent apps with music paused (or nothing in the queue), the music service could stay in the background with its paused notification for several minutes, and Android could then end it with a crash. Now it stops at once and its notification goes away. Music that is actually playing still carries on when you close the app, as before.
- **Music controls work again after the music service stopped.** If the music service was closed (from Recents, or ended by Android) while JellyCtrl stayed in memory, the mini player and play buttons could stop responding until the app was restarted. JellyCtrl now notices, clears the mini player, and connects again the next time you play something.
- **Less background work.** The music controls no longer check the player twice a second while nothing is playing, and opening the app no longer starts the music service when no music is loaded.

### Security

- **No playback metrics for Android.** The music player no longer reports playback statistics to Android's media metrics service.

## v0.5.1 (2026-10-25)

Fixes for crashes when leaving the app, and a few security and privacy tightenings.

### Fixed

- **No more crash when closing JellyCtrl with the app lock on.** With the lock set to lock as soon as you leave, pressing Back on the first screen or swiping JellyCtrl away from the recent apps could crash it, because the lock screen tried to open on a screen that was already closing. JellyCtrl still locks, and the lock screen shows the next time you open it.
- **No more crash when closing a video in picture-in-picture with the app lock on.** Closing the small window locks the app; the player's lock screen no longer tries to appear on the closing player.
- **mpv no longer crashes the app when a video is closed.** Late taps, track changes or events that arrived just after the mpv player was shut down could reach it after it was gone; they're now ignored.
- **Library lists no longer crash on duplicates.** When the server returned the same title on two pages (for example with **Random** order, or while items were being added), scrolling the library could crash it. Each title now shows once.
- **Subtitle search no longer crashes** when two of your server's subtitle plugins return the same result; it's listed once.
- **A stopped download no longer stays "downloading".** When Android or the Wi-Fi-only setting paused a download, it could keep showing as running (and, with the app lock on, keep the sign-in in memory). It now shows as waiting and carries on by itself later.

### Security

- **Fewer ways in for other apps.** Two receivers that Android libraries add for app-store and debugging tools (the profile installer and WorkManager diagnostics) are removed, so the music service and the "Play in JellyCtrl" link are the only parts other apps can reach.

### Changed

- **README:** the Privacy section says that subtitle search goes through your server's subtitle plugins, and that Open in ArrCtrl only passes the title's ids to ArrCtrl.
- Internal cleanup: tidied code formatting.

## v0.5.0 (2026-10-25)

Download subtitles while you watch, and jump from a movie or series to ArrCtrl.

### New

- **Search for subtitles from the player.** Open **Audio and subtitles** in the video player and tap **Search for subtitles** at the bottom of the list. JellyCtrl asks the subtitle plugins on your Jellyfin server (such as OpenSubtitles) for subtitles for what you're watching and lists what they find, with the provider, the name of the release, the format, the rating and the number of downloads. Labels show exact matches for your file (listed first), hearing-impaired subtitles (for the deaf and hard of hearing), forced subtitles and machine-translated ones.
- **Pick the language.** The search starts in your subtitle language from **Settings** (your Jellyfin subtitle language preference), or your phone's language when you haven't set one. Tap **Language** to search in any language your server knows.
- **The download plays straight away.** Tap a result and the server downloads it and keeps it with the video. JellyCtrl then refreshes the list of subtitles and switches to the new one where you are, with both players: Media3 reopens the same stream at the same spot with the new subtitles added, and mpv adds them without stopping the video. When the server is converting the video, or the new file changes the order of the tracks, JellyCtrl reloads the video at the same position with the new subtitles chosen. The downloaded subtitles stay on the server for every device and every later playback.
- **Search for subtitles from the title page too.** Movies, episodes and other videos have **Search for subtitles** in the **More** menu and at the bottom of the audio and subtitles picker. A downloaded subtitle is chosen for when you press Play.
- **Clear messages when it can't work.** If your server has no subtitle plugin installed, JellyCtrl says so and explains that a server admin can install one (for example OpenSubtitles) under **Plugins** in the Jellyfin dashboard. If your account isn't allowed to manage subtitles, it says that the admin can turn on **Allow subtitle management** for you. When nothing is found it suggests trying another language. Subtitle search isn't offered for downloads played offline or for Live TV.
- **Open in ArrCtrl.** Movie and series pages have **Open in ArrCtrl** in the **More** menu when ArrCtrl is installed. It opens the movie in Radarr or the series in Sonarr inside ArrCtrl, using the TMDB id for movies and the TVDB and TMDB ids for series (whichever Jellyfin has). It only shows when Jellyfin has one of those ids for the title. Only the ids are passed on, and only to ArrCtrl. An older ArrCtrl that doesn't know these links yet just opens.

## v0.4.0 (2026-10-25)

Light mode that follows the device theme.

### New

- **Light mode that follows the device theme.** JellyCtrl now switches between a dark and a light version of the Ctrl / Signal look by itself, following your phone's dark theme setting (including a schedule, if you've set one). It changes straight away while the app is open. There is no setting for it in the app, and it still doesn't use wallpaper (Material You) colours.
- **Signal Light colours.** In light mode the background is a warm off-white (#F5F5F1) with white cards, light grey borders and lines, near-black text (#0B0B0D) and grey secondary text. The lavender accent (#B48CFF) is unchanged for filled things: the "Continue watching" card, main buttons, the selected tab, switches that are on, selected chips and the PIN dots. Accent-coloured text and icons, such as the slash in the JELLY/CTRL wordmark, labels like "NEW · 1 OF 6" and the type above a title, the playing song and watched dots, links, a text field you're typing in, dialog icons and the fingerprint key, use a darker violet (#6A3BDB) so they stay easy to read on white. Dark mode keeps the lavender everywhere.
- **Status and navigation bars follow the theme.** They take the background colour, with dark icons in light mode and light icons in dark mode. The screen shown while the app starts also matches the theme.
- **Everything follows the theme:** Home, Search, Library, title pages, the music player and mini player, the You page, Settings, the server dashboard, sign-in, the app lock screen, dialogs, sheets, switches (grey track with a grey knob when off in light mode), progress bars (dark fill on a light grey track in light mode) and the floating tab bar (white at 92% with a light border). Error text uses a darker red (#D93636) in light mode.
- **Download notifications** use the accent colour of the current theme.

### Unchanged

- **The video player and the photo viewer stay dark** in both themes, so nothing bright sits next to the picture. This includes the player's audio, subtitle and options sheets and its status bar.

## v0.3.0 (2026-10-25)

A new look: JellyCtrl now uses **Ctrl / Signal**, the design shared by every Ctrl series app. Everything works as before; only how it looks has changed.

### Changed

- **Dark only, with one accent colour.** JellyCtrl now always uses a near-black background (#08080A) with dark grey cards and a single lavender accent (#B48CFF) for the things that matter most. The status and navigation bars match the background, with light icons, and the app runs edge to edge.
- **New typefaces.** All text is now set in Space Grotesk, and numbers, times, codes and the small section labels use JetBrains Mono. Both fonts are bundled in the app (nothing is downloaded), replacing Manrope.
- **New top bar.** The main tabs show the **JELLY/CTRL** wordmark on the left and your profile picture (or initials) on the right in a rounded square, with a small accent square while you're signed in. Tapping it still opens the You page with your settings. Pages you open from there have a square back button and the page title.
- **Home.** What you were last watching now sits at the top on a large accent card showing how far you are (in per cent), the title, the time left and a progress bar, with a play button straight on the card. The new-arrivals carousel below it is now a set of rounded cards with a mono "NEW · 1 OF 6" label, and the Play and Details buttons are rounded rectangles. The remote-control button moved into the top bar.
- **Posters and rows.** Posters, thumbnails and chapter images have 14dp rounded corners and a thin border. Row headings are larger and "See all" is a small mono label. Watched ticks and unwatched counts are small accent squares, and resume progress is drawn in the accent colour.
- **Title pages.** The back button is a rounded square, a mono label above the title says what it is (movie, series, collection…), and the artwork fades into the dark background. Episode and track lists are plain rows with thin dividers, two-digit episode and track numbers, mono runtimes, and an accent dot for watched episodes and the song that's playing. Genres, links and season pickers are rounded rectangles; the selected season is filled with the accent. Cast photos are rounded squares, and the file details use a mono layout.
- **Video player.** The controls use rounded-square buttons: back, audio and subtitles and playback options at the top, skip back and forward next to a large accent play/pause button in the middle. Times and the chapter name are in mono, the seek bar and its handle are in the accent colour, and "Skip intro", the up-next card, the gesture hints and the error screen follow the new style.
- **Now playing (music).** A plain dark background instead of a colour taken from the album art, rounded-square buttons, a 28dp-rounded album cover, a large accent play/pause button, mono times, and a "PLAYING FROM" label with the album at the top. The queue shows numbered rows with dividers. The mini player is a floating dark card with an accent play button.
- **Colours no longer change with the artwork.** Title pages and the music player used to tint themselves with a colour from the poster or album art; they now always use the one accent colour.
- **Bottom navigation.** The tab bar is now a floating dark bar with rounded corners and a thin border, 28dp above the bottom of the screen. The selected tab is an accent block with its icon and name; the other tabs show only their icon. Pages scroll clear of it as before. On tablets the side rail uses the same accent.
- **Switches.** All switches (settings, server dashboard, player options) are now rounded rectangles: accent with a dark knob when on, grey when off. They still work with TalkBack as switches.
- **App lock.** The lock screen has a lavender lock badge, a "VKNXWORKS · CTRL" label and a large "Locked" title. PIN digits are shown as small squares that fill with the accent as you type, the keypad has rounded-square keys in JetBrains Mono, and the fingerprint button is now the outlined accent key at the bottom left of the keypad (it becomes OK once you've typed enough digits for a longer PIN). Delete is the key at the bottom right. How the lock works is unchanged.
- **You page.** Your picture or initials are shown in a large accent square next to your name and server address (in mono). The rows are grouped into cards with mono headings (Your stuff, Devices, App, Server, About). **Support JellyCtrl** is now an accent button at the bottom, followed by the version and "GOOGLE-FREE · TELEMETRY-FREE · ARM64".
- **Settings and the server dashboard.** Groups are dark cards with thin borders and lines between rows, under mono section labels. Icons are outlined throughout, and the settings row on the You page uses a sliders icon instead of a gear.
- **Dialogs, sheets and text fields.** Dialogs and bottom sheets are dark cards with 26dp corners and a thin border. Text fields (sign-in, search, passwords, the dashboard) are dark with a thin border that turns lavender when you type in them. Progress bars are 6dp tall with rounded ends. The Search and Library tabs have a mono label above their title, and the Library tiles have rounded corners with the library type as a label.
- **Sign-in screen.** Shows the JELLY/CTRL wordmark, the new text fields and buttons, rounded-square user pictures, and the Quick Connect code in large mono digits.

### Removed

- **Theme and wallpaper colours settings.** **Settings > Appearance** (Follow system, Dark, Light, Pure black, and Use wallpaper colours) is gone: the Signal look is dark only and doesn't use wallpaper (Material You) colours. Whatever you had chosen before is ignored.

## v0.2.9 (2026-10-22)

### Fixed

- **JellyCtrl asks for notifications as soon as you're in.** On Android 13 and newer an app has to ask before it can show notifications. JellyCtrl only asked once music had already started or a download had begun, so the question popped up in the middle of playback, and if it was dismissed or didn't show, the music controls in the notification and on the lock screen and the download progress were silently missing. It now asks once, the first time the home screen is shown after you've signed in (and unlocked the app, if the app lock is on). If you've already answered, it doesn't ask again.
- **No more repeated questions when music starts.** JellyCtrl used to ask again every time music started while notifications weren't allowed. Android's question now only comes up automatically once; after that you can turn notifications on from **Settings**.

### New

- **Notifications in Settings.** A new **Notifications** section shows whether JellyCtrl may show notifications. When they're blocked it says **Notifications are blocked** with an **Allow** button: it shows Android's question again if Android still allows that, or otherwise opens JellyCtrl's notification settings so you can turn them on there. It updates as soon as you come back from Android's settings. When they're allowed, tapping the row opens those settings too.

## v0.2.8 (2026-10-21)

### Fixed

- **Unlock with fingerprint now really works.** JellyCtrl was missing the Android permission that lets an app use the fingerprint sensor, so the phone always answered "no fingerprint available": **Unlock with fingerprint** under **Settings > Privacy and security** stayed greyed out with "Set up a fingerprint on this phone first", even with a fingerprint set up, and the lock screen only ever asked for the PIN. The permission is now included, so you can turn on fingerprint unlock after updating. It still only accepts strong biometrics such as the fingerprint sensor, and JellyCtrl never sees your fingerprint itself; Android only tells it whether the match succeeded.

## v0.2.7 (2026-10-21)

### Fixed

- **Fingerprint unlock works again after you leave the app.** Once you'd left JellyCtrl with the back gesture or by swiping home, the lock screen no longer asked for your fingerprint when you came back, and tapping **Use fingerprint** did nothing either; only the PIN worked until the app was force-closed. The fingerprint prompt was asked for while the app was still coming back on screen, and the phone dropped it without answering, which left JellyCtrl waiting for an answer that never came. Now the prompt only opens once JellyCtrl is fully back on screen, a prompt that never answered can't block the next one, and the lock screen offers your fingerprint again every time you come back to it, not just the first time after it locked. This includes the lock screen over the video player, for example when you open a video full screen again from picture-in-picture. The same applies to the one-time check with your phone's screen lock for people updating from the old app lock.

## v0.2.6 (2026-10-21)

### Security

- **Nothing stays usable on top of the lock screen any more.** When JellyCtrl locked while a menu, dialog or sheet was open (for example the actions sheet of a movie or episode, or the audio and subtitle or settings sheet in the player), that window stayed on top of the lock screen and could still be read and tapped, and a hardware keyboard could still move to buttons hidden behind the lock. While JellyCtrl is locked, its screens are now closed completely instead of only covered, and in the video player the lock screen gets a window of its own above everything else. After you unlock you're back on the same screen and tab as before (an open menu or sheet is closed). Music and downloads carry on as before while the app is locked.
- **The app lock now also works while a video plays in picture-in-picture.** JellyCtrl never counted sending a video to the small picture-in-picture window as leaving the app, so it never locked, however long the video played. Now it does: with **Lock again: as soon as you leave** it locks the moment the video goes to picture-in-picture, and with a lock time it counts from then. The video keeps playing in its small window; the lock screen shows when you open it full screen or go back to JellyCtrl.
- **Your Jellyfin sign-in no longer follows a redirect to another website when a video plays with mpv.** mpv sent your sign-in to wherever the server redirected the video (for example a video file that lives on another website), so that website could see it. JellyCtrl now follows the server's redirects itself before starting mpv, and mpv only gets your sign-in while the address is still your Jellyfin server. If the server redirects too often, playback stops with a message instead.
- **Sign-ins are always sealed with your app PIN when the app lock is on.** If JellyCtrl was closed at exactly the wrong moment while you turned the app lock on, your saved sign-ins could stay protected by the phone's keystore only, without the extra encryption from your PIN. JellyCtrl now checks this every time you unlock (and at start-up when it's unlocked) and seals any sign-in that isn't sealed yet.

### Security

- **No hidden dependency list in the app file any more**: the build tools used to tuck an encrypted list of the libraries JellyCtrl is built with into the APK's signature block. Only Google can read it, so nobody checking the app could see what it holds. It's now left out, the same as in the other Ctrl apps. Nothing changes in how the app works.

## v0.2.5 (2026-10-21)

A tidy-up release that makes JellyCtrl match the other Ctrl apps.

### Changed

- **Your profile picture placeholder now matches the other Ctrl apps.** When your Jellyfin account has no picture, the round button at the top right and the big picture on the You page show your initial on a circle tinted in the app's accent colour, instead of a plain grey circle. Your Jellyfin picture still shows when you have one
- **All text is now in one place, ready for translations.** Every label, message, notification and error the app shows now comes from a single list instead of being written into the code
- **Counts use proper singular and plural wording** (seasons, tracks, episodes, items, downloads, seconds), ready for languages with other plural rules. In English, a server settings page with a single advanced setting it can't show now says "1 advanced setting isn't shown here" instead of "1 advanced settings aren't shown here"
- **README**: the App lock and privacy setting paths now start at your profile picture (tap your profile picture › Settings › Privacy and security)

## v0.2.4 (2026-10-21)

### Security

- **With the app lock on, the recent-apps preview always stays blank**, whatever **Hide in recent apps** is set to, so it never shows what's behind the lock. Android takes that preview the moment you leave the app, before it locks.
- **README**: the App lock and privacy text now reads the same as in the other Ctrl apps.

## v0.2.3 (2026-10-21)

### Changed

- **Hide in screenshots** and **Hide in recent apps** are now two separate switches in Settings › Privacy and security, both off by default. **Hide in screenshots** makes screenshots and screen recordings show a blank screen; **Hide in recent apps** makes the recent apps view show a blank card instead of the app, while screenshots still work. If you had turned on the single "Hide in screenshots and recent apps" switch, both are on after the update.
- **The recent apps view no longer goes blank just because the app lock is on.** It follows **Hide in recent apps** now, so turn that on if you want a blank card there.
- **Protect the server dashboard is gone**: the two switches cover it. Before, server dashboard screens were hidden from screenshots and the recent-apps view by default; now they follow **Hide in screenshots** and **Hide in recent apps** like the rest of the app, so turn those on if you want that back. The dashboard screens where API keys, user passwords or credential settings are typed or shown (API keys, Users, a user's page and server settings) are still always hidden.
- **Always hidden, whatever the switches say**: the lock screen, choosing or changing the app PIN, the sign-in screen and the Change password dialog, because a password or PIN is typed or shown there.
- **README**: describes the two switches.

## v0.2.2 (2026-10-21)

### Changed

- **Hide in screenshots and recent apps** now reads "Screenshots, screen recordings and the recent apps view show a blank screen", the same as in the other Ctrl apps.
- **README**: the App lock section now uses the same wording as the other Ctrl apps.

### Security

- **The lock and PIN screens can't be captured.** While JellyCtrl shows the lock screen, or a screen for choosing, changing or turning off the app PIN, screenshots and screen recordings come out black, in the video player too, whatever "Hide in screenshots and recent apps" is set to.
- **No snapshot at start-up**: the recent-apps view shows a blank card from the moment JellyCtrl opens, before its settings are read.

## v0.2.1 (2026-10-21)

### Security

- **The app lock drops your sign-in sooner after music stops.** In 0.2.0, once you had played music, JellyCtrl kept the sign-in in memory while locked for as long as the app was running, even after the music had stopped. Now only music that's actually playing or paused with a queue (with its notification up) keeps it; when you stop the music or the queue ends while JellyCtrl is locked, the sign-in is dropped until you unlock.

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

### Security

- **No mention of Google left in the app's own code.** JellyCtrl has no Google Play services, Firebase or other Google services and never contacts Google: it only talks to your Jellyfin server. The list of named Google apps in the music player and a technical note in its setup file are gone. It works the same on de-Googled phones such as GrapheneOS, CalyxOS, /e/OS and LineageOS without Google apps.

## v0.1.24 (2026-10-20)

### Security

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

### Security

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
