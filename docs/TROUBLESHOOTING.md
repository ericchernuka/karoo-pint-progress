# Troubleshooting

## Install appears stuck

Karoo's installer can remain on `Installing...` after Android has completed the package operation.
Leave the installer and verify whether Pint Progress is present. If it launches or appears in the
data-field picker, installation succeeded.

If it did not install, inspect the actual package-manager result:

```bash
adb install -r pint-debug.apk
adb shell pm path io.ericchernuka.pintprogress
```

Common causes:

- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: signing certificate differs. Uninstall first.
- `INSTALL_FAILED_VERSION_DOWNGRADE`: candidate `versionCode` is lower. Build a higher code or use
  `adb install -r -d` only for deliberate development downgrades.
- Parse or invalid APK errors: download again and compare SHA-256.

Uninstalling removes the saved calorie target:

```bash
adb uninstall io.ericchernuka.pintprogress
```

## App or settings screen is missing

The settings screen is the launcher activity named Pint Progress. The data field itself appears in
Karoo's data-field picker under Pint Progress as `Pints`. These are separate surfaces.

Verify the package, then launch it directly:

```bash
adb shell pm path io.ericchernuka.pintprogress
adb shell monkey -p io.ericchernuka.pintprogress 1
```

Restart Karoo after a fresh sideload if the extension catalog has not refreshed.

## Field is searching or not filling

Pint Progress reads Karoo's cumulative Calories from Power stream. It does not estimate calories.
Confirm an active ride and a working power source. Idle, searching, unavailable, negative, and
non-finite values intentionally render the unavailable state.

## Field clips or aligns incorrectly

Do not patch a layout from one screenshot. Capture:

- tile grid span and physical `viewSize`;
- `textSize`, alignment, and boundary setting;
- preview versus live mode;
- light or dark theme;
- completed count length;
- screenshot and Karoo software version.

Reproduce the case in the pure size, layout, typography, and gravity tests. Fix derived policy first,
then verify large, medium, short, narrow, and icon-only treatments on device.

## Settings do not appear to apply

The target is global, not per tile. It is 80 to 400 kcal in 5 kcal steps. A visible field observes
changes, but actual `RemoteViews` updates remain limited to one per second. Changing the target resets
transition history, so it will not celebrate calories already accumulated.

## Generated assets fail CI

```bash
node tools/generate-drawables.mjs
node tools/validate-drawables.mjs
git diff -- pint/src/main/res/drawable \
  pint/src/main/kotlin/io/ericchernuka/pintprogress/PintAssetDrawables.kt
```

Commit generator and generated changes together. Never repair generated XML directly.

## Dependency verification fails

First confirm that the dependency change is intentional. Regenerate SHA-256 metadata only for the
required build, then inspect every added component before committing:

```bash
./gradlew --write-verification-metadata sha256 :pint:assembleDebug
git diff -- gradle/verification-metadata.xml gradle/libs.versions.toml
```

Do not disable dependency verification.

## Useful logs

```bash
adb logcat -s PintProgressField
adb logcat | rg 'pintprogress|KarooExtension|PackageManager'
```

Debug logs include resolved `ViewConfig`, dp dimensions, density, and chosen layout. Do not add
production telemetry to replace focused debugging.
