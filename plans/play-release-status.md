# Play release status

Updated 30 October 2026.

## Console

- All App content declarations completed. The Need attention tab is empty.
- Data safety saved: no data collected or shared. This applies to the current local-only build.
- Advertising ID saved: No.
- Privacy policy: https://7hr4iz3.github.io/cashflow/privacy.html, verified HTTP 200.
- Finance category and store contact email iamthraize@gmail.com saved.
- Internal test release 0.1.0, version code 1, published on 30 October 2026. Console confirms Available to internal testers; not reviewed.
- Play reports two non-blocking warnings: missing deobfuscation file and native debug symbols. This build does not enable R8 obfuscation.
- Internal tester list: Main Emails, 4 users, selected.
- Internal test opt-in link: https://play.google.com/apps/internaltest/4701200218531720597
- Production access requires a closed test with at least 12 opted-in testers for 14 days. Dashboard currently reports 0 testers.

## Signing

The user approved creating a new Cashflow upload key. It is stored outside the repository:

- `~/.config/cashflow/signing/upload.jks`
- `~/.config/cashflow/signing/release.properties`

The directory is private and both files are readable only by the current user. Back up both files securely. Do not commit signing material or credentials. The Android Gradle build reads the local properties file when available.

## Source and checks

- Added Privacy policy in Settings, opening the public policy in the browser.
- Release merged manifest has no INTERNET or AD_ID permission; backup remains disabled.
- Shared JVM tests: 13 tests, zero failures or errors. Gradle reused passing results.
- `:shared:jvmTest :androidApp:bundleRelease` succeeded. `jarsigner -verify` reports jar verified. The privacy link has not yet been exercised on a device.
- Bundle: `androidApp/build/outputs/bundle/release/androidApp-release.aab`
- SHA-256: `6398b52d964205916344c57d173503bb2035826e9eeed20f887e9c5e197ec25e`

## Subsequent AI work

The source is now version 0.2.1/code 3 (Convex development backend, three tiers, billing, opt-in Jev help, Android app-picker setup). Version 0.2.0/code 2 was uploaded on 10 Nov 2026 and is already released on Closed testing (managed publishing is off, so saving a release publishes it). Internal testing holds a draft of 0.2.1/code 3 that has not been published. See [AI and subscription verification](ai-subscription-verification.md) for checks and activation requirements.
