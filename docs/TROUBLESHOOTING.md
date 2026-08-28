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
Karoo's data-field picker under Pint Progress as `Pint Mug`. These are separate surfaces.

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

Reproduce the case in the pure size and typography tests and the resource validator. Fix derived
policy first, then verify large, medium, short, narrow, and small treatments on device. A completed
count must remain visible in every live graphical treatment.
Include a navigation toast or reroute resize because Karoo can reduce the host container without
calling `startView` again.

The issue #11 device study found three important limits:

- Treat the visible glyph raster, the `TextView` bounds, and the mug bounds as separate geometry.
  A centered `TextView` can still place a digit curve too close to its lower raster edge.
- Use fixed `1+`, `99+`, and `100+` diagnostic states through the production rendering path when
  investigating geometry. Add colored root, count, suffix, and mug outlines only in a disposable
  local debug build. Remove these fields and outlines before review.
- Validate in the live ride view. Page-editor navigation can cover the bottom row and cannot prove
  whether the underlying `RemoteViews` clips.

The verified fix keeps count and mug scaling linked, applies a measured optical offset, and reserves
8 dp of vertical glyph clearance in constrained regular fields. Do not replace that clearance with
another baseline translation. A translation changes position but does not reduce an oversized glyph.

Reference vectors captured on a Hammerhead k24 running KOS 1.650.2509 were:

| Layout | `gridSize` | `viewSize` | Resolved dp | `textSize` | Boundaries | Treatment |
| --- | --- | --- | --- | ---: | --- | --- |
| Full-width constrained | 60 × 25 | 478 × 243 px | 255 × 130 dp | 96 sp | on | regular |
| Half-width compact | 30 × 15 | 238 × 148 px | 127 × 79 dp | 50 sp | on | compact |
| Full-width roomy | 60 × 60 | 478 × 642 px | 255 × 342 dp | 96 sp | on | regular |

These values are evidence from one host version, not new layout thresholds. Always record the actual
`ViewConfig` for the candidate under test.

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
