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
- Never delete or overwrite a file that can't be opened, never replace an existing `data.key`, and never create a Keystore key on the decrypt path (`KeystoreAes.existing`). An ordinary file only becomes a sealed one through `SealedFile.sealInPlace` (temp file, sync, read back, compare, atomic rename). A sealed file that can't be opened makes its `SealedStore` `unreadable`: nothing is written, `AppContainer.dataUnreadable` goes on and `AppRoot` shows the "Can't open your downloads" screen ("Try again", and "Start empty", which renames the files to `*.unreadable`). Only "Forgot PIN?" (`deleteAll`, `forget(null)`) wipes such a file, through `SealedStore.clear`.
- `SealedStore` reads and writes the disk and may ask the Keystore: never call it on the main thread. Workers wait for `DownloadRepository.awaitLoaded()`.
- `SealedFileTest` covers the format, the migration step, the key rules and `SealedStore` (`./gradlew :app:testDebugUnitTest`; CI doesn't run unit tests).

## Dependency verification

Every library and plugin download is checked against `gradle/verification-metadata.xml` (SHA-256 only, no PGP). When you add, remove or update a dependency or Gradle plugin, run the manual **Dependency verification metadata** workflow (`gh api -X POST repos/VoidedKN0X/JellyCtrl/actions/workflows/verification-metadata.yml/dispatches -f ref=main`), take the file from its log (gzipped and base64-encoded between the BEGIN/END markers), check the diff (only Maven Central, Google's Maven and the Gradle plugin portal) and commit it in the same push. Keep the `<trusted-artifacts>` entries for `-sources.jar` and `-javadoc.jar`. Never turn verification off or to lenient in CI.
