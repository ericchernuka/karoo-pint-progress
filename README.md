# Pint Progress for Karoo

> Modified by Eric Chernuka for Pint Progress. See [NOTICE](NOTICE) for attribution.

Pint Progress is a battery-conscious graphical Karoo data field that turns the native **Calories from Power** ride total into a playful beer-progress indicator.

Pint Progress is branding, not a volume claim: its default beer target is **150 kcal**, approximately a 12 fl oz / 355 ml 5% ABV beer. The field fills in 5% steps, gradually builds foam from 80%, celebrates each observed completion with bubbles rising above the rim, briefly drains, then starts the next glass. The `×N` counter appears after the first completed beer.

![Pint Progress graphical data field at 80% with two completed pints](docs/images/pint-progress-preview.png)

*Resource-accurate preview of the graphical field at 80% to next and `×2` completed. Karoo host chrome varies by device software and is not shown.*

The field uses Karoo's cumulative `TYPE_CALORIES_ID` stream. It does not estimate calories itself, so its count follows the native Karoo calorie model and active ride state.

## Battery behavior

- One native calorie subscription exists only while the field is visible.
- Normal rendering changes only at 5% fill boundaries or completed-beer boundaries.
- Completion animation has three static frames separated by one second, matching Karoo's one-view-update-per-second limit.
- Every actual view update is spaced at least one second apart, so a quick source-state change is deferred instead of dropped by Karoo.
- There is no timer, sensor scan, background job, bitmap allocation, or developer FIT field.

## Build and test

The repository vendors the open-source Karoo extension SDK source in `lib/`, so a local build does not need GitHub Packages credentials.

```bash
./gradlew :lib:testDebugUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification
```

This produces a local debug APK and an intentionally unsigned release APK. It also verifies 100% instruction and branch coverage for every deterministic behavior class: calorie conversion, threshold animation, view-reducer coalescing, preview state, drawable frame selection, labels, counters, and caller authorization. The thin Android/Karoo boundary is compile-verified against the vendored official SDK; see [the test boundary policy](docs/TEST_BOUNDARY.md).

## Development install on a Karoo

1. Enable developer mode and USB debugging on the Karoo.
2. Install the debug APK with `adb install -r pint/build/outputs/apk/debug/pint-debug.apk`.
3. Add **Pint Progress** to a ride page from the Karoo data-field picker.
4. Start a ride with power-based calories available.

The debug APK is for local development only. Do not attach it to a public release. The release build is deliberately unsigned: any distributable release must be rebuilt from an audited commit, signed with a project-owned release key, and published with its commit and SHA-256 recorded.

Do a short on-device smoke test before relying on it in a ride: verify initial rendering, the 80% foam state, a completion animation, a page change, and that the Karoo system can load the caller-gated extension.

## Security model

- The app requests no Android permissions and contains no network client, storage access, WebView, analytics SDK, native code, or dynamic code loading.
- It consumes only Karoo's cumulative **Calories from Power** stream and renders package-owned static `RemoteViews` resources.
- Android requires the extension service to be exported for Karoo discovery. Every Binder transaction is caller-gated to the Karoo system package (`io.hammerhead.appstore`), malformed view configuration is rejected before parsing, duplicate sessions cancel their predecessor, and active sessions are bounded.
- CI has read-only repository permissions, immutable action pins, no repository secrets, and Gradle SHA-256 dependency verification recorded in `gradle/verification-metadata.xml`.

## Customize the drink target

For now, change `DEFAULT_BEER_CALORIES` in `pint/src/main/kotlin/io/ericchernuka/pintprogress/core/PintProgressReducer.kt`, rebuild, and reinstall. Keeping the target compile-time constant makes a ride deterministic and keeps the runtime path minimal. A profile setting screen is a deliberately deferred iteration.

## License and attribution

This repository is Apache-2.0. See [NOTICE](NOTICE) for project authorship, vendored-source attribution, and prominent notices for modified upstream files. The included `lib/` directory is the open-source Karoo extension SDK from SRAM, retained under its Apache-2.0 license, sourced from `hammerheadnav/karoo-ext` at commit `f79f103` (SDK 1.1.9).

https://github.com/hammerheadnav/karoo-ext
