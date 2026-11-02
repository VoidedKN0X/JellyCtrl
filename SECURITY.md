# Security policy

Thank you for helping keep JellyCtrl and its users safe.

## Reporting a problem

**Please don't report security problems in a public issue.** Report them privately instead:

1. Go to the [Security tab](https://github.com/VoidedKN0X/JellyCtrl/security) of this repo.
2. Click **Report a vulnerability** and describe what you found, how to reproduce it, and which version of JellyCtrl you used.

Only the maintainer can see the report. I'll reply as soon as I can, work on a fix, and credit you in the changelog if you'd like.

## Supported versions

Only the latest release gets security fixes. Please update before reporting, and check the [changelog](CHANGELOG.md) to see whether it's already fixed.

## What counts

- Your password, access token, API keys or other data leaking to anyone other than your own Jellyfin server (for example through artwork, video, subtitle or download requests, or a redirect)
- Ways around the app lock, or protected screens showing up in screenshots when they shouldn't
- Another app on the phone being able to make JellyCtrl do something you didn't ask for, or read your library through its music player
- A Jellyfin server or a link from another app being able to save files outside JellyCtrl's downloads folder, or open other apps
- Anything that lets someone run code or read files they shouldn't

Problems in Jellyfin itself should go to that project.

## How releases are protected

Every release is built from this repo by GitHub Actions and signed with the same release key. Its SHA-256 certificate fingerprint is:

```
70:90:2F:85:07:E5:E4:65:81:26:A7:33:25:68:38:B9:EE:59:50:F9:8C:F3:FB:CC:5A:59:EB:2D:B3:92:9C:17
```

The [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this key.
