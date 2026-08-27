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
| Gradle | Gradle 8.7, AGP 8.5.0 | Gradle 8.13, AGP 8.13.2 | Keep ours. The versions are compatible and the wrapper distribution is checksum-verified. |
| Gradle CI state | No shared Gradle setup or cache documented | Both workflows use the pinned official `gradle/actions/setup-gradle` action with the basic open-source cache provider | Keep the basic provider for reusable Gradle state without adding a dependency or proprietary cache service. |
| Verification | `./gradlew build` | Generated-asset assertions, unit tests, lint, debug/release builds, 100% behavior coverage, and wrapper validation | Keep ours. The narrower checks produce stronger project-specific signal. |
| Versioning | `100 + BUILD_NUMBER`; tag name becomes version name | CI run number and `1.0.0-dev.<run>` | Keep ours for development and use the tag name for signed releases. Both produce increasing build codes. |
| Signing | Release signing on every workflow event | CI debug artifacts; unsigned release verification builds | Add tag-only signing with protected secrets. Keep untrusted verification unsigned. |
| Release publication | Tag-triggered GitHub Release | Tag-triggered GitHub Release; manually dispatched `UNSAFE-DEBUG` artifact only | Add a tag-only release workflow with APK and checksum publication. |
| Actions permissions | `contents: write` for branch and PR builds | `contents: read` for verification | Keep verification read-only. Write access exists only in the tag release job. |
| Delivery manifest | Generates a Karoo manifest and points the app at it | No delivery manifest yet | Defer until the public release URL, icon, screenshots, and signing identity are ready. |

## Issue #3 toolchain consultation

The conservative AGP 8 toolchain was checked against official primary sources on 2026-08-26.
The selected versions are the latest compatible patches within the requested minor lines. JDK 17
and Android SDK 34 remain unchanged.

| Component | Selected version | Official source |
| --- | --- | --- |
| Android Gradle Plugin | 8.13.2 | [AGP 8.13 release notes](https://developer.android.com/build/releases/agp-8-13-0-release-notes) |
| Gradle wrapper | 8.13 | [Gradle 8.13 release notes](https://docs.gradle.org/8.13/release-notes.html) and [official release checksums](https://gradle.org/release-checksums/) |
| Kotlin Gradle plugin | 2.3.21 | [Kotlin release process](https://kotlinlang.org/docs/releases.html) |
| Dokka Gradle plugin | 2.2.0 | [Dokka v2 migration guide](https://kotlinlang.org/docs/dokka-migration.html) |
| kotlinx.coroutines | 1.11.0 | [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases) |

The AGP 8.13.2 release notes require Gradle 8.13 and JDK 17. The Kotlin compiler keeps the
existing JVM 1.8 bytecode target through the typed `compilerOptions` API. Dokka v2 uses the
stable `dokkaGeneratePublicationHtml` task and retains the library module name, version, locally
patched vendored-source links, samples, public-only visibility, custom assets, style sheet, footer,
and ignored documentation output under the module build directory.

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

Both workflows use the pinned official `gradle/actions/setup-gradle` action with its basic
open-source cache provider. The Gradle wrapper is validated by the official Gradle
`wrapper-validation` action before either workflow can execute Gradle, and the wrapper
distribution checksum remains pinned locally. Wrapper checksum validation and Gradle dependency
verification are separate integrity controls.

The audit identified Gradle advisories that are mitigated by repository restriction and dependency
verification. The coordinated toolchain upgrade is followed by the full Karoo device matrix. This
release-alignment change deliberately does not mix compatibility work into signing and publication
work.
