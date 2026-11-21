# JellyCtrl

## Releasing

- A release is published only when `versionName` in `app/build.gradle.kts` is bumped. Bump `versionCode` by 1 at the same time.
- Tags are `vX.Y.Z` and releases are named "JellyCtrl X.Y.Z". Never add a build number (`-build.N`, "(build N)").
- Use the same version for every change that ships together; README, licence and CI changes don't need a bump.

## Changelog

Every new version needs a detailed entry at the top of `CHANGELOG.md`, under a heading for that version (match the existing headings). CI refuses to release without one, and the entry becomes the release notes on GitHub, in Obtainium and in the CtrlF-Droid repo.

- Write for people using the app, not for developers: what's new, what changed, what was fixed.
- Cover every user-visible change in the version, one bullet each, with enough detail to understand it without opening the app.
- Group with `### New`, `### Changed`, `### Fixed` and `### Security` when there's more than a couple of items.

## Encryption at rest

- `downloads/index.json` (the download list, in the external app folder) and `filesDir/offline-progress.json` are sealed with AES-256-GCM (`data/crypto/SealedFile`, marker `JELLYCTRL-REST1`) through `data/crypto/SealedStore`, under the data key (`DataKeyStore`: 32 random bytes in `filesDir/data.key`, wrapped by the Keystore key `jellyctrl_data_at_rest` through `KeystoreWrapper`/`KeystoreAes`; no user authentication, so `DownloadWorker` and `ProgressSyncWorker` work in the background). `KeystoreCrypto` (the token key) and the app-lock vault are separate and untouched.
- Not sealed, on purpose: downloaded media, `.part` files, posters and subtitles (Media3, mpv and Coil open them by path; large), Coil's cache, `filesDir/mpv/`, DataStore settings and accounts, WorkManager's database.
- Readers must accept both sealed and ordinary files (`SealedFile.isSealed`). Without a data key the app reads and writes ordinary files as before.
- Never delete or overwrite a file that can't be opened, never replace an existing `data.key`, and never create a Keystore key on the decrypt path (`KeystoreAes.existing`). An ordinary file only becomes a sealed one through `SealedFile.sealInPlace` (temp file, sync, read back, compare, atomic rename). A sealed file that can't be opened makes its `SealedStore` `unreadable`: nothing is written and `AppContainer.dataUnreadable` goes on. The app is NOT blocked by that (browsing, streaming and playback don't need these files): only the Downloads page shows the "Can't open your downloads" message (`UnreadableData` in `ui/nav/AppRoot.kt`, called from `DownloadsScreen`; "Try again", and "Start empty", which renames the files to `*.unreadable`), and the Downloads rows on the Library and You pages say it can't be opened. Only "Forgot PIN?" (`deleteAll`, `forget(null)`) wipes such a file, through `SealedStore.clear`.
- While the download list is unreadable its entries are unknown (the list in memory is empty), and that must never be read as "nothing downloaded, so clean up": `enqueue` and `delete` do nothing (the download buttons show a notice through `downloadsUnreadable`), `DownloadWorker` returns `Result.retry()` before it looks anything up or touches a file, and playback simply streams (`completedFile` is null). Never add code that removes files because they aren't in the list.
- `SealedStore` reads and writes the disk and may ask the Keystore: never call it on the main thread. Workers wait for `DownloadRepository.awaitLoaded()`.
- `SealedFileTest` covers the format, the migration step, the key rules and `SealedStore` (`./gradlew :app:testDebugUnitTest`; CI doesn't run unit tests).

## Dependency verification

Every library and plugin download is checked against `gradle/verification-metadata.xml` (SHA-256 only, no PGP). When you add, remove or update a dependency or Gradle plugin, run the manual **Dependency verification metadata** workflow (`gh api -X POST repos/VoidedKN0X/JellyCtrl/actions/workflows/verification-metadata.yml/dispatches -f ref=main`), take the file from its log (gzipped and base64-encoded between the BEGIN/END markers), check the diff (only Maven Central and Google's Maven) and commit it in the same push. Keep the `<trusted-artifacts>` entries for `-sources.jar` and `-javadoc.jar`. Never turn verification off or to lenient in CI.

## Build and signing (CI)

`.github/workflows/build.yml` has four jobs: `version` (is this a release?), `build` (Gradle, **no secrets**, `UNSIGNED_RELEASE=true` gives `app-release-unsigned.apk`; runs every check), `sign` (no checkout, no Gradle: `zipalign` + `apksigner` with the release key, then checks the signer against `EXPECTED_CERT_SHA256` and that the APK's contents didn't change) and `release` (attestation, release notes, publish).

- Never give the `build` job a secret, and never run Gradle or check out the repo in `sign`. New build checks (tests, lint, APK checks) go in `build`.
- `EXPECTED_CERT_SHA256` in `build.yml` must match the fingerprint in `SECURITY.md` and `README.md`; the build fails if it doesn't.
- No Gradle cache in any workflow (`cache-disabled: true`): a cache saved by a failed run used to break every later build.
- `settings.gradle.kts` lists only `google()` and `mavenCentral()` for plugins, not `gradlePluginPortal()`: the portal serves different files for the Kotlin plugin markers, which made verification fail whenever Maven Central had a hiccup. Don't add it back unfiltered (see the comment in that file).

## Build toolchain

- AGP 9.5.1, Gradle 9.9.2, Kotlin 2.4.21, Compose BOM 2026.09.00, `compileSdk` 37 (Compose 1.12 needs it), `minSdk` 35, `targetSdk` 36. All versions are in `gradle/libs.versions.toml`.
- AGP 9 has built-in Kotlin: there is no `org.jetbrains.kotlin.android` plugin any more. The Kotlin version is pinned by the `kotlin-gradle-plugin` classpath entry in the root `build.gradle.kts`; the Compose and serialization compiler plugins use the same `kotlin` version. Don't re-add the Kotlin Android plugin.
- AGP 9's R8 is stricter (`-keep class X` without a member block no longer keeps the constructor; resources are shrunk together with code). Keep rules in `proguard-rules.pro` must name the members they need, and resources must be referenced through `R`, never by name.
- Any change here needs the dependency verification procedure (see "Dependency verification").

## Languages

- The app follows the phone's language: English in `app/src/main/res/values/strings.xml`, Dutch (Belgian, informal "je") in `values-nl/strings.xml`. There is no in-app language picker; `res/xml/locales_config.xml` lists the two for Android's own per-app language setting.
- Every user-visible text comes from resources (`stringResource`, or `str()`/`plural()` outside composables). A new string goes into both files in the same push, with the same placeholders. Technical names, units and the app name stay untranslated.
- The names of server configuration fields in the dashboard are made from Jellyfin's own field names and stay English.
