# Changelog

What changed in each JellyCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/JellyCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## Unreleased

Changes on `main` that aren't in a release yet. They ship with the next version.

### Changed

- **Subtitles in the Media3 player are drawn as outlined text**, without the black box behind them, so they look the same as in mpv and follow the new size and colour settings.
- **With "Audio passthrough" off, Media3 always decodes sound on the phone.** Until now Media3 would pass Dolby or DTS through by itself when a connected HDMI or USB output reported support for it; that now only happens with the switch on.

### New

- **Request in SeerrCtrl.** When SeerrCtrl (the Ctrl app for Seerr and Jellyseerr) is installed, the ⋯ menu on a movie or series page has **Request in SeerrCtrl**. It opens that title's page in SeerrCtrl, where you can request it (more seasons of a show, or a 4K copy). JellyCtrl only passes the title's TMDB id, and only to SeerrCtrl; nothing is requested until you do it there. The row isn't shown when SeerrCtrl isn't installed or Jellyfin has no TMDB id for the title. Built by CI only; not yet run on a phone.
- **Chapter skipping in the video player.** When a film or episode has chapters, the player shows the current chapter's name above the seek bar with an arrow on each side: the left one goes back to the start of this chapter (or to the chapter before it when this one began less than three seconds ago), the right one jumps to the next chapter. Tapping the name opens the list of all chapters with their times, to jump straight to one. Works the same in both players (Media3 and mpv) and for downloads; live TV has no chapters. Built by CI only; not yet run on a phone.
- **Sleep timer in the video player.** Playback options (the sliders button) › **Sleep timer**: pause after 15, 30, 45, 60, 90 or 120 minutes, or **When this video ends**, which stops the next episode from starting on its own. When the timer pauses the video, the screen is allowed to turn off again; it stays on as before once you press play. The timer belongs to the player you have open and is gone when you close it. Works in both players. Built by CI only; not yet run on a phone.
- **Subtitle size and colour.** Settings › **Subtitle look**, and the audio and subtitles sheet in the player: six sizes (50% to 200%) and five colours (white, yellow, green, light blue, grey), remembered for every video. Applies to text subtitles in both players; picture subtitles (PGS, DVD) are drawn as they are, and styled ASS subtitles in mpv keep their own colours. Built by CI only; not yet run on a phone.
- **Subtitle timing, in the mpv player.** In the player's audio and subtitles sheet, subtitles can be shown earlier or later in steps of 0.1 and 0.5 seconds (up to a minute either way), with **Reset**. It applies to the video you're watching and starts at zero for the next one. Media3 has no way to shift subtitles, so there the sheet shows a row that switches this video to mpv instead of a control that does nothing. Built by CI only; not yet run on a phone.
- **Audio passthrough.** Settings › Player › **Audio passthrough** (off by default) sends Dolby Digital, Dolby Digital Plus, DTS, DTS-HD and Dolby TrueHD sound undecoded to a TV, receiver or soundbar connected over HDMI or USB, but only the formats Android says the connected output takes; the row under the switch shows which those are right now. In mpv those formats are passed through and everything else is decoded as before; in Media3 they are played without a decoder on the phone. On the phone's speaker, wired or Bluetooth headphones nothing changes. Built by CI only and not run on any device; it needs a real receiver to confirm.

### Project

- **Checksum list regenerated without the old entries.** `gradle/verification-metadata.xml` was regenerated on CI with the fixed workflow and committed exactly as printed (its SHA-256 matches the log). It now lists only the 471 components the build downloads today; the leftovers from earlier library and build-tool versions are gone.
- **The checksum list no longer collects old entries.** The hand-started workflow that regenerates `gradle/verification-metadata.xml` used to add the new checksums to the existing file, so entries for library versions that are no longer used stayed in it. It now clears the old entries first and writes only what the build uses, and prints the file's SHA-256 so the copy that gets committed can be checked against the log. Nothing in the app changes.

## v0.8.0 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published. It is the first release built with the new build tools (Android Gradle Plugin 9, stricter code shrinking), so update over your installed version and check that your data and settings are still there.

### Fixed

- **Fixed before release: the app would have crashed at start with the new build tools.** JellyCtrl uses WorkManager for its background work, and WorkManager stores its jobs in a small Room database that is created by name when the app starts. The shrinker (R8) in Android Gradle Plugin 9 is stricter than the old one and removed the constructor that needs, which makes the app stop right at launch (a known problem with Room 2.6.1 under Android Gradle Plugin 9). A keep rule for that constructor, the same one newer Room versions ship themselves, is now in `app/proguard-rules.pro`. No released version was affected: the new build tools have not been in any release. Built by CI only; not yet run on a phone.

### Changed

- **A download list that can't be opened no longer blocks the whole app.** Since 0.7.0 the list of your downloads is encrypted on the phone, and when it couldn't be opened (the key in the phone's secure storage was lost, or the file was damaged) JellyCtrl showed "Can't open your downloads" over everything, so you couldn't browse or stream either. Now that message, with the same **Try again** and **Start empty** (and the same question before starting empty), only appears on the **Downloads** page. Home, search, your libraries, streaming and music work as usual. The Downloads rows on the Library tab and the You page say "Can't be opened: tap to see why" instead of a count.
- **What stays exactly as careful as before while the list can't be opened:** nothing is deleted, renamed or written over until you choose **Start empty** (which still only sets the unreadable files aside). A title you downloaded earlier streams from your server instead of playing from the phone, and can't be played offline until the list opens again. Tapping **Download** shows a short notice instead of starting a download, because a new download couldn't be saved in the list. A download that Android restarts in the background waits and tries again later, without touching any file; it carries on once the list opens, and stops if you start empty. If only the small file with your offline positions can't be opened, the Downloads page shows the same message; positions of what you watch offline are then kept until the app closes and still sent to your server when it's reachable, but not saved on the phone.
- **Starting the app waits less on the settings file.** At start JellyCtrl reads its settings before showing anything, so the app lock can never show unlocked for a moment. That read now begins in the background before the network client is set up, instead of after, so the two overlap and the app usually no longer has to stand still for it. What is read, and that the app waits for it, is unchanged.
- **The video player does less work while playing.** The player reports its position four times a second, and every report made the whole player screen work out its layout again: the controls, titles, buttons and the code behind picture-in-picture. Now only the time, the seek bar with its chapter name, and the "up next" countdown follow the position. Nothing looks or behaves differently; it should be a little lighter on the battery and smoother on slower phones. The players themselves (Media3 and mpv) are untouched.
- **Newer build tools and libraries; nothing changes in how JellyCtrl looks or works.** JellyCtrl is now built with the current stable toolchain: Android Gradle Plugin 8.11.1 → 9.5.1, Gradle 8.14.3 → 9.9.2, Kotlin 2.2.0 → 2.4.21 (with its Compose and serialization compiler plugins) and Compose BOM 2025.07.00 → 2026.09.00 (Compose 1.12.1, Material 3 1.4.0). The libraries move to the versions the newest Ctrl apps use: AndroidX Activity 1.10.1 → 1.13.0, Core 1.16.0 → 1.18.0, Lifecycle 2.9.1 → 2.10.0, Navigation 2.9.1 → 2.9.8, DataStore 1.1.7 → 1.3.0, kotlinx.serialization 1.9.0 → 1.13.0 and kotlinx.coroutines 1.10.2 → 1.11.0. Everything else stays at the version it had (Media3 1.8.0, OkHttp 4.12.0, Coil 3.2.0, WorkManager 2.10.2, libmpv 1.0.0). No library was added, and the app is still Google-free (CI checks the release dependencies for Play services, Firebase, ML Kit, Play Core and Tink).
- **Compiled against the Android 17 SDK.** Compose 1.12 needs `compileSdk` 37, so that goes from 36 to 37. The Android versions JellyCtrl runs on and targets are unchanged (`minSdk` 35, `targetSdk` 36), so Android treats the app exactly as before.
- **The app is shrunk by a newer R8.** Android Gradle Plugin 9 compiles Kotlin itself (the separate Kotlin Android plugin is gone from the build files) and brings the R8 version that understands Kotlin 2.4. JellyCtrl's own keep rules are unchanged. The newer R8 is stricter about what it keeps, so the release build needs a check on a phone (both video players (Media3 and mpv), music, downloads) before the next release.
- The checksums Gradle verifies every download against (`gradle/verification-metadata.xml`) were regenerated on CI for the new versions. Built and checked by CI only; not yet run on a phone.

### Security

- **The signing key is no longer available to the build (nothing in the app changed).** Until now the release key and its passwords were present in the same GitHub Actions job that runs Gradle, so any build plugin or library could in principle have read them. The build job now has no secrets at all and produces an unsigned APK. A separate job, which checks out no code and runs no Gradle, signs it with `apksigner`, and then refuses to hand it on unless it is signed by exactly the certificate published in SECURITY.md and README.md and its contents are identical to what the build job made. Releases are signed with the same key and the same signature scheme (v2) as before, so updates install as usual. The first JellyCtrl release made this way is the next one; the same pipeline already published a release of another Ctrl app (DateCtrl 0.3.1).
- **The build checks that the published fingerprint and the enforced one are the same.** If `build.yml`, SECURITY.md and README.md ever disagree about the signing certificate, the build fails.

### Project

- **Builds no longer use a saved Gradle cache, and the Gradle plugin portal is no longer a plugin source.** Builds of Ctrl apps sometimes failed with "Dependency verification failed ... from repository Gradle Central Plugin Repository", and kept failing until the caches were deleted by hand. The cause: with a restored cache Gradle still fetches the plugin files from Maven Central on every build; when Maven Central can't be reached for a moment, Gradle takes the Kotlin plugin "marker" file from the Gradle plugin portal instead, the portal serves a different file under the same name, verification rightly refuses it, and the failed run then saved that state as the cache for every later run. Now every build and the weekly code scan start from a clean Gradle home (a build takes a few minutes longer), and `settings.gradle.kts` lists only Maven Central and Google's Maven for plugins: everything the build needs is there. Dependency verification itself is unchanged and as strict as before.

## v0.7.0 (2026-11-20)

**This release encrypts some of what the app stores on the phone. An older version cannot read it afterwards, so export or back up anything you need before you update (details below).** Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Security

- **The list of your downloads is now encrypted on the phone.** JellyCtrl keeps a small list next to your downloads (`downloads/index.json`, in the app's own folder) with the title, series name and Jellyfin details of every download, so it could be read by anyone who got a copy of the app's files, for example over USB. That list, and the small file with how far you got in downloads you watched offline (`offline-progress.json`), are now encrypted with AES-256-GCM. The key is 32 random bytes kept in the app's private folder (`data.key`), itself encrypted by a key in the phone's secure hardware (Android Keystore; StrongBox when the phone has it) that can't be copied off the phone. It needs no PIN or fingerprint, so downloads and the sending of offline progress keep working in the background. This protects those files when they are copied off the phone; it does not protect against someone who can use the unlocked phone or a compromised app while it runs, and it works the same with the app lock on or off.
- **What is not encrypted, and why.** The downloaded films, episodes and songs themselves, half-finished downloads, their posters and their subtitle files stay ordinary files: the video players (Media3 and mpv), the music player and the picture loader open them straight from the folder, and they are far too large to decrypt in memory. Their file names are made from a hash, not from the title. Also unchanged: cached artwork (it is downloaded again when needed), mpv's certificate bundle, your settings and the list of accounts (the sign-in tokens in it were already encrypted), and the small database Android's WorkManager keeps for queued downloads.
- **Moving to the encrypted files is automatic and can't lose the list.** The first time this version starts, each of the two files is copied in encrypted form to a temporary file, written to disk, read back, decrypted and compared with the original, and only then put in the original's place in one step. If anything goes wrong, or the app is stopped halfway, the original file stays in use and it is tried again at the next start. On a phone where the secure hardware doesn't work, the files simply stay as they were.
- **If the key is ever lost** (for example the phone's secure storage was reset), JellyCtrl does not start with an empty list and does not overwrite anything: it shows "Can't open your downloads" with **Try again**. **Start empty** is there for when that doesn't help: the list that can't be opened is renamed (`index.json.unreadable`), not deleted, and the downloaded files stay on the phone without being listed; downloading a title again puts it back in the list.
- **Don't go back to an older version without expecting to lose the list.** JellyCtrl 0.6.1 and older can't read the encrypted list: they show no downloads and write a new, empty list over it the next time something is downloaded. The downloaded files stay, but they are no longer listed. There is no export for downloads; they can be downloaded again from your server.

### Changed

- **The Downloads page shows a spinner for the moment the list is being read**, instead of "Nothing downloaded" for a split second.
- **A download that restarts together with the app waits for the download list to be read** before it looks itself up. Before, a download picked up by Android right at app start could find the list still empty and stop.

## v0.6.1 (2026-11-20)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Changed

- **Home opens a little sooner.** Continue watching, Continue listening and Next up are now asked for at the same moment as your list of libraries, instead of only after that list has arrived. That saves one round trip to the server each time Home loads. What Home shows is the same.
- **Smoother scrolling in library grids.** A grid (a library, a list, Live TV recordings) checks that no item is in it twice, because that would crash the grid. That check went over every loaded item each time anything on the grid was redrawn; it now only runs when the loaded items actually change.
- **Artwork is drawn in a lighter way.** Posters, backdrops, logos, cast photos, chapter pictures and the music artwork were each drawn through a slower kind of image view (Coil's `SubcomposeAsyncImage`) that the image library itself advises against in lists and grids. They now use the plain image view. It should look the same: the same plain colour while a picture loads, and the same name or film icon in its place when there is no picture or it can't be loaded. Not yet checked on a phone.

## Project updates (2026-11-19)

### Project

- **The weekly code scan no longer fails at random.** CodeQL has to watch the app being compiled, but its build could take the compiled code from Gradle's build cache instead, and then the scan stopped with "CodeQL detected code written in Java/Kotlin but this run didn't build any of it" (and GitHub mailed a failed run). The scan's build now runs with `--no-build-cache`, so the code is always compiled. No app changes, so no new version.

## v0.6.0 (2026-11-13)

Better downloads: smaller converted copies, subtitles that come along, whole seasons or just the next few episodes, and what you watch offline counts on your server.

### New

- **Download a smaller version.** Under **Settings > Downloads**, pick a video quality (20 down to 1 Mbps) and a music quality (320 down to 128 kbps). Your server then converts the download (video to H.264 and AAC in MP4, music to MP3), so a two-hour film at 4 Mbps takes about 3.6 GB instead of whatever the original weighs. **Original file** (the default) downloads the file as it is, like before.
- **Subtitles come with your downloads.** Subtitles that are separate files on your server (such as .srt files next to the video, or ones you downloaded with subtitle search) are saved with the download, and a converted video gets all its text subtitles saved this way. They show up in the player's subtitle menu offline, in Media3 and in mpv, and the one your server would pick is switched on. Picture subtitles (PGS, DVD) can't be saved separately: keep the original file if you need those.
- **Download a whole season, a whole series or the next few unwatched episodes.** The download button on a series or season now asks: every episode, the whole season, or the next 1, 3, 5 or 10 episodes after the last one you watched. Episodes you already have are skipped.
- **Watched offline counts.** When you watch a download (or play a downloaded song) without a connection, JellyCtrl remembers how far you got, or that you finished it, and tells your server as soon as the phone is online again, so Continue watching, Next up and the watched marks are right on every device.
- **Space for downloads**: pick how much room downloads may use (2 to 128 GB, or no limit). A download that doesn't fit stops with a message instead of filling the phone.
- **The Downloads page shows each download's quality** (original or converted, and at what bitrate) and how many subtitles came with it.

### Changed

- **Downloads keep trying for longer.** A download that loses its connection now tries up to 8 times (WorkManager waits longer between tries) before it shows as failed; original files carry on where they stopped. A converted download starts that file again, because your server converts it on the fly.
- **Retrying a failed download keeps the quality it was started with.**
## v0.5.12 (2026-11-13)

### Security

- **Saved logins are protected by the phone's secure chip where it has one.** On a fresh install (or after clearing JellyCtrl's data), the key that encrypts your access tokens is now created in the phone's StrongBox secure element when the phone has one, and in the normal Android Keystore otherwise. If you already use JellyCtrl nothing changes for you: your existing key keeps working and you stay signed in.

### Project

- The Gradle wrapper download is now checked against its published SHA-256 checksum before a build runs.

## v0.5.11 (2026-11-12)

### Changed

- **Remote control and the admin pages stop asking the server when JellyCtrl isn't on screen.** Remote control (every 5 seconds), the admin dashboard (every 10) and scheduled tasks (every 3) kept refreshing with the app in the background, which matters most while music keeps JellyCtrl running. They now pause when you leave the app and catch up at once when you come back.
- **Slightly smaller download.** The bundled JetBrains Mono font (numbers, readouts and labels) no longer carries the Cyrillic, Vietnamese and maths-alphabet letters JellyCtrl never shows itself, which takes about 0.1 MB off the download and 0.2 MB off the installed app. Everything looks the same; if a name or a log line shown in that font uses one of those scripts, those letters are now drawn with your phone's own font.

## v0.5.10 (2026-11-05)

JellyCtrl now needs Android 15 or newer.

### Changed

- **Needs Android 15 or newer.** Android 13 no longer gets security updates and Android 14's end early 2027, so JellyCtrl now supports Android 15, 16 and 17 only. On older phones the version you have keeps working, but it won't get updates.

## v0.5.9 (2026-11-05)

Floating windows from other apps can no longer cover the lock screen, PIN screens or confirmations.

### Security

- **No floating windows over the lock screen, PIN screens and confirmations.** While the lock screen, choosing or changing your PIN, turning the lock off, **Forgot PIN?**, or a confirmation is showing, other apps' floating windows, such as chat heads and screen overlays, are hidden. A malicious app can then no longer lay something over a button to trick you into tapping it. This covers deleting an item or a download, signing out, approving a Quick Connect sign-in, using an unencrypted connection, and the server dashboard's uninstall, revoke, delete, remove, sign-out, restart and shut-down confirmations. Floating windows show again as soon as that screen closes, and they're never hidden over the video player or picture-in-picture. This uses a new permission, "Hide other apps' floating windows", which Android grants at install without asking.
- **Lock and PIN screens are hidden from apps that misuse accessibility** (Android 14 and newer). Only real accessibility tools, such as TalkBack and other screen readers, can read the lock screen and PIN pad; other apps with accessibility access can't see what's on them. Screen readers work exactly as before.
- **Stricter handling of requests from other apps** (Android 16 and newer). Other apps can only open JellyCtrl the way it expects: the home-screen icon and ArrCtrl's **Play in JellyCtrl** keep working, and anything else is refused by Android. Media controls in other apps (watches, cars, media-control apps) can still connect to the music player as before.

### Project

- **Every library and Gradle plugin download is checked.** The build now refuses any library or plugin whose SHA-256 checksum doesn't match the list in the repo, so a changed or swapped download can't end up in the app.
- README: a **Check a download** section (the SHA-256 in the release notes and `gh attestation verify`), the new permission in the permissions table, and how to update the checksum list when a dependency changes.

## Project updates (2026-11-05)

### Project

- **Releases can be checked against this repo.** Each release now carries a signed build provenance attestation (made by GitHub with Sigstore) that ties the APK to this repo's build workflow and the tagged commit, and the release notes list the APK's SHA-256 checksum.
- **Tighter build workflows.** Every workflow starts with no permissions and each job gets only what it needs; only the release job can write, and only to publish the release and its attestation.
- **New security checks on GitHub:** zizmor checks the workflows for security problems on every workflow change, and CodeQL scans the app's code every week and on every app change (results in the Security tab; it never blocks a release).
- **New manual workflow** that records the SHA-256 of every library and Gradle plugin the build downloads, so the build can refuse anything that has been changed or swapped. No app changes, so no new version.

## v0.5.8 (2026-11-05)

The video player no longer hands playback diagnostics to Android.

### Privacy

- **No playback diagnostics are handed to Android.** The Media3 video player used to report playback details (such as what format is playing, buffering and errors) to Android's own media diagnostics service, which can pass them on to Google when the phone shares usage and diagnostics data. The video player now has this turned off, like the music player already did, so no playback diagnostics are handed to Android. Playback itself, picture-in-picture and the mpv player work as before.

## v0.5.7 (2026-11-05)

Subtitles in the mpv player can no longer pass your login to another site, and an unused Wi-Fi permission is gone.

### Security

- **Subtitles in the mpv player keep your login on your own server.** When the mpv player loaded a separate subtitle file and your Jellyfin server answered with a redirect to another address, mpv would follow it with your access token attached. JellyCtrl now follows a subtitle's redirects itself first, as it already did for the video, and only gives mpv the final address; your token is only added while that address is still your own server. A subtitle whose address can't be checked is left out instead of being loaded unchecked. This closes the gap mentioned in 0.5.6.

### Privacy

- **The Wi-Fi state permission is gone.** Finding Jellyfin servers on your Wi-Fi only needs the multicast permission, which stays; JellyCtrl never read anything about your Wi-Fi network, so Android no longer lists Wi-Fi state access for it.
- **No debug logging in release builds.** Debug, verbose and info messages from the libraries JellyCtrl uses are now stripped from the app, so nothing about your server or what you watch can end up in the phone's system log.

### Project

- The build now checks that the Gradle wrapper is an official Gradle release, and fails if any Google Play services, Firebase, ML Kit, Play Core or Tink library ever ends up in the app.
- `.gitignore` also covers `local.properties` in any folder and `google-services.json`, so they can't be committed by accident.
- README: every permission in the app is now listed with what it's for, and the Ctrl series list now has all 28 apps.

## Project updates (2026-11-03)

### Project

- **README:** the Ctrl series list now links to ChatCtrl, the Matrix chat app in the series. No app changes, so no new version.
- **README:** the Ctrl series list now includes SumCtrl, the offline calculator, unit converter and work-tools app. No app changes, so no new version.

## v0.5.6 (2026-11-02)

Server logs in the dashboard are now always kept out of screenshots, plus a security check of the whole app.

### Security

- **Server logs stay out of screenshots and recordings.** Opening a log under **Server dashboard > Logs** now always keeps the screen out of screenshots, screen recordings and the recent-apps preview, like the API keys, users and server settings screens already did, whatever **Hide in screenshots** says. Tokens and passwords in logs were already masked, but logs still hold user names, IP addresses and file paths.
- **Checked the rest of the app.** Your access token is only sent to your own Jellyfin server, never in a web address, and never to another site through a redirect: this holds for artwork, video (both Media3 and mpv), music and downloads. Subtitles the mpv player loads separately get your token only for your own server's addresses; a redirect from your own server to another site isn't checked for them yet. Sign-in tries https first and only uses plain HTTP after you say yes, and HTTP accounts keep their "Not encrypted (HTTP)" label. Links from ArrCtrl are checked before use and only open a page. Other apps can't browse your music through JellyCtrl's player, and downloads are always saved inside JellyCtrl's own folder. No changes were needed there.

### Project

- New **SECURITY.md**: how to report a security problem privately, and the fingerprint of the key every release is signed with.
- New bug report and feature request forms for GitHub issues.
- README: clearer install steps for people new to GitHub (which file to tap on the Releases page), a new **Is it safe to install?** section with the signing fingerprint and every permission explained, a **How JellyCtrl is made** section, the full Ctrl series list, a note that JellyCtrl isn't affiliated with the Jellyfin project, and the empty Screenshots section removed.

## v0.5.5 (2026-11-02)

Bottom sheets no longer show a stray grey box or outline.

### Fixed

- **Bottom sheets no longer show a stray grey box or outline.** The thin border around bottom sheets was drawn in the wrong place: a grey rectangle cut across the sheet (for example across the first row of the menu you get by long-pressing an item in **Continue watching**), and a stray rounded outline showed near the top of the screen. Sheets now have no outline at all: just the card colour with rounded top corners, separated from the page by the dimmed background behind them. Their colours, corners, drag handle and behaviour are unchanged.
- This applies to every bottom sheet: the long-press menu on movies, episodes, albums and other items, choice lists in **Settings** (such as streaming quality or lock timeout), the audio, subtitle and version picker and the **More** menu on title pages, subtitle search, the queue and lyrics in the music player, and the audio and subtitle and playback options sheets in the video player.
- Dialogs keep their thin border (it was always drawn in the right place there), and popup menus keep the style from 0.5.4.

## v0.5.4 (2026-11-02)

Popup menus no longer show a grey box.

### Fixed

- **Popup menus no longer show a grey box around them.** Menus still used Material's default shadow, which was drawn inside the menu's own window and showed up as a grey, square-edged box, most visibly in light mode. Menus now use the flat Signal style in both themes: the card colour (white in light mode, dark grey in dark mode), 16dp rounded corners, a thin border and no shadow. Menu text uses the normal text colour and icons the muted grey; **Remove library** stays red, icon included.
- Menus changed: the library menu on each library in **Server dashboard > Libraries** (Scan this library, Add folder, Rename, Remove library) and the library type picker when adding a library. JellyCtrl has no other popup menus, tooltips or popups. Nothing else changes in how they work.

## v0.5.3 (2026-11-02)

Mark as watched and Favourite now show their result straight away, everywhere.

### Fixed

- **Mark as watched shows up right away.** Marking something watched (or unwatched) from the long-press menu in a library, a genre or studio list, or Search did change it on your server, but the card didn't update until you left and came back, so it looked like nothing happened. The watched tick now appears on the card at once.
- **Episode lists update on a show's page.** On a show's page, marking an episode watched from its long-press menu, or marking the whole show watched, didn't update the episode list below. The watched dots now refresh straight away.
- **The Watched and Favourite buttons on a details page react at once.** The button changes as soon as you tap it instead of after the page reloads. If your server refuses the change, the button switches back and you see the reason.
- **Other screens catch up too.** After marking something on a details page and going back, Home (Continue Watching and Next Up), Favourites, your libraries and Search now show the new state instead of the old one. Libraries keep your scroll position while they update, and items drop out of an Unplayed or Favourites filter once they no longer match.
- **The long-press menu closes as soon as you pick Mark as watched or Favourite**, instead of staying open until your server answers.

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
