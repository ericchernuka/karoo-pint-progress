# Drawable workflow

## Source of truth

`tools/generate-drawables.mjs` owns the shared mug icon, all field-fill vectors,
`PintAssetDrawables.kt`, and `core/PintAsset.kt`. Never edit generated drawable XML or either Kotlin
mapping directly.

Field-fill states include 0% through 95% in 5% steps, full foam, draining, and unavailable.
Available states use a static beer texture with scalloped foam, side bubbles, and rounded
highlights. `ic_pint.xml` is the shared app, extension, data-type, and settings icon.

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
- Keep field-fill bubbles and highlights outside the central half so the completed-pint number stays
  dominant. Mirror the complete fill artwork behind a left-aligned count so the highlights stay on
  the opposite side. Body bubbles use stable positions and appear only after the beer reaches them.
- Add foam pockets from 80% fill. Keep unavailable progress plain.
- Use semantic resources from `values/colors.xml` and `values-night/colors.xml`.
- Keep field-fill beer, foam, and bubbles on their Pints Fill-specific semantic resources so
  dark-mode changes do not change the shared icon palette. Dark-mode bubbles must not use the light
  count color. Keep the fill-count halo transparent in light mode and dark in night mode.

## Visual QA

Inspect at native size and enlarged scale in both themes. Check:

- no shared-icon viewport clipping or handle/body intersection;
- icon foam meets its amber fill;
- unavailable field-fill state remains distinct from empty;
- field-fill texture stays legible without obscuring `0`, `1`, or `100` in short, compact, and tall
  fields.

README images under `docs/images/` are device captures. Record their device and KOS version, and do
not describe them as emulator screenshots or generated artwork.
