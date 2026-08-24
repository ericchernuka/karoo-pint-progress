# Build system comparison

This records the comparison with [Karoo Reminder](https://github.com/timklge/karoo-reminder)
and the build decisions for Pint Progress.

## How Karoo Reminder works

- It is a single `app` module and consumes `karoo-ext` `1.1.9` from GitHub Packages.
- `settings.gradle.kts` reads `GPR_USER` and `GPR_KEY` from the environment, falling back to
  `local.properties`.
- The Android module derives `versionCode` from `100 + BUILD_NUMBER` and `versionName` from
  `RELEASE_VERSION`.
- The workflow runs on every branch, tag, and pull request. It builds a release APK on every run.
- A tag build creates a GitHub Release, publishing the release APK, screenshots, icon, and a
  generated `manifest.json`.
- Release signing is configured from four environment variables, including a base64-encoded
  keystore. Its workflow grants `contents: write` to the whole job.

The relevant source is [settings](https://github.com/timklge/karoo-reminder/blob/master/settings.gradle.kts),
[app build](https://github.com/timklge/karoo-reminder/blob/master/app/build.gradle.kts), and
[Android workflow](https://github.com/timklge/karoo-reminder/blob/master/.github/workflows/android.yml).

## Current differences

| Concern | Karoo Reminder | Pint Progress | Decision |
| --- | --- | --- | --- |
| Modules | One application module | App plus vendored `lib` SDK module | Keep ours. It avoids GitHub Packages credentials and keeps the SDK boundary auditable. |
| SDK delivery | GitHub Packages dependency | Pinned source copy with NOTICE | Keep ours. Do not reintroduce package credentials without a concrete need. |
| Gradle | Gradle 8.7, AGP 8.5.0 | Gradle 8.7, AGP 8.6.1 | Keep ours. The versions are compatible and ours is already checksum-verified. |
| Verification | `./gradlew build` | Generated-asset assertions, unit tests, lint, debug/release builds, 100% behavior coverage, and wrapper validation | Keep ours. The narrower checks produce stronger project-specific signal. |
| Versioning | `100 + BUILD_NUMBER`; tag name becomes version name | CI run number and `1.1.0-dev.<run>` | Keep ours for development and use the tag name for signed releases. Both produce increasing build codes. |
| Signing | Release signing on every workflow event | CI debug artifacts; unsigned release verification builds | Add tag-only signing with protected secrets. Keep untrusted verification unsigned. |
| Release publication | Tag-triggered GitHub Release | Tag-triggered GitHub Release; manually dispatched `UNSAFE-DEBUG` artifact only | Add a tag-only release workflow with APK and checksum publication. |
| Actions permissions | `contents: write` for branch and PR builds | `contents: read` for verification | Keep verification read-only. Write access exists only in the tag release job. |
| Delivery manifest | Generates a Karoo manifest and points the app at it | No delivery manifest yet | Defer until the public release URL, icon, screenshots, and signing identity are ready. |

## Security conclusions

We should not copy the Reminder workflow's credential handling wholesale. Loading package and
signing credentials into every branch and pull-request job expands the blast radius of a malicious
change. Pint Progress keeps credentials out of verification jobs and only exposes signing secrets
to the tag-only release workflow. GitHub Actions are pinned by commit SHA, checkout credentials
are disabled, and Gradle dependency verification remains enabled.

The release workflow expects these repository secrets:

- `PINT_KEY_ALIAS`
- `PINT_KEY_PASSWORD`
- `PINT_KEYSTORE_PASSWORD`
- `PINT_KEYSTORE_BASE64`

No values belong in the repository, local `gradle.properties`, logs, or documentation.

The Gradle wrapper is now validated by the official Gradle wrapper-validation action before either
workflow can execute Gradle. The wrapper distribution checksum remains pinned locally as well.

The audit also identified Gradle 8.7 advisories that are currently mitigated by repository
restriction and dependency verification. Upgrading Gradle, AGP, Kotlin, coroutines, and the
verification metadata should be a dedicated coordinated maintenance change, followed by the full
Karoo device matrix. This release-alignment change deliberately does not mix that compatibility
upgrade into the signing and publication work.
