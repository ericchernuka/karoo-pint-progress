# Drawable workflow

## Source of truth

`tools/generate-drawables.mjs` owns the shared mug icon, all field-fill vectors,
`PintAssetDrawables.kt`, and `core/PintAsset.kt`. Never edit generated drawable XML or either Kotlin
mapping directly.

Field-fill states include 0% through 95% in 5% steps, full foam, draining, and unavailable.
Available states use flat beer with a scalloped foam band. `ic_pint.xml` is the shared app, extension, data-type, and settings icon.

## Change workflow

```bash
node tools/generate-drawables.mjs
node tools/validate-drawables.mjs
git diff --check
```

Commit the generator and every generated output together. CI regenerates and compares the drawable
XML, `PintAssetDrawables.kt`, and `core/PintAsset.kt`.

The validator also owns static resource contracts that support the generated assets. It checks the
shared icon, all field-fill alignment layouts, edge-to-edge rendering, initial count state, field
labels, extension metadata, and generated drawable mappings. Run it when any of those resources
change.

## Visual contracts

- Preserve the shared icon's `83 x 112` viewport and straight-sided mug silhouette.
- Keep its handle joins behind the opaque mug body to prevent antialiasing seams. Keep its amber
  and foam inside the inner glass.
- The normalized field-fill rectangle uses `fitXY` because its purpose is to cover the host content
  rectangle.
- The field-fill completion frame reaches 100% with a deep foam cap. The draining frame uses 55%
  fill with a thin foam cap.
- Field-fill art contains only full-width bands: surface, beer, and foam. `fitXY` stretches the art
  to any field aspect ratio, so dots, pills, and highlights would smear. Keep unavailable progress
  plain.
- Use semantic resources from `values/colors.xml` and `values-night/colors.xml`.
- Keep field-fill beer and foam on their Pints Fill-specific semantic resources so dark-mode
  changes do not change the shared icon palette. Keep the fill-count halo transparent in light mode and dark in night mode.

## Visual QA

Inspect at native size and enlarged scale in both themes. Check:

- no shared-icon viewport clipping or handle/body intersection;
- icon foam meets its amber fill;
- unavailable field-fill state remains distinct from empty;
- field-fill bands stay legible without obscuring `0`, `1`, or `100` in short, compact, and tall
  fields.

README images under `docs/images/` are device captures. Record their device and KOS version, and do
not describe them as emulator screenshots or generated artwork.
