# Drawable workflow

## Source of truth

`tools/generate-drawables.mjs` owns all mug and field-fill vector variants and
`PintAssetDrawables.kt` and `core/PintAsset.kt`. Never edit generated drawable XML or either Kotlin
mapping directly.

Mug states include 0% through 95% in 5% steps, full bubbles, draining, and unavailable. Each mug
state has regular, compact, and icon variants. Field-fill states include 0% through 95% in 5% steps,
full foam, draining, and unavailable. Available field-fill states use a static beer texture with
scalloped foam, side bubbles, and rounded highlights. `ic_pint.xml` is the shared extension icon.

## Change workflow

```bash
node tools/generate-drawables.mjs
node tools/validate-drawables.mjs
git diff --check
```

Commit the generator and every generated output together. CI regenerates and compares the drawable
XML and both mappings in `PintAssetDrawables.kt`; local review must also confirm that generated
`core/PintAsset.kt` matches the generator.

The validator also owns static resource contracts that support the generated assets. It checks all
mug and field-fill alignment layouts, edge-to-edge fill rendering, responsive image bounds, initial
count state, field labels, extension metadata, and generated drawable mappings. Run it when any of
those resources change, even when mug geometry does not.

## Visual contracts

- Preserve the `83 x 112` viewport and straight-sided mug silhouette.
- Keep handle joins behind the opaque mug body to prevent antialiasing seams.
- Keep normal amber and foam inside the inner glass.
- Only `pint_full_bubbles` may crown the rim. It must remain inside the viewport and mug width.
- Preserve the mug aspect ratio in every mug layout. The normalized field-fill rectangle uses
  `fitXY` because its purpose is to cover the host content rectangle.
- The field-fill completion frame reaches 100% with a deep foam cap. The draining frame uses the
  same 55% transition level as the mug presentation and keeps a thin foam cap.
- Keep field-fill bubbles and highlights outside the central half so the completed-pint number stays
  dominant. Mirror the complete fill artwork behind a left-aligned count so the highlights stay on
  the opposite side. Body bubbles use stable positions and appear only after the beer reaches them.
- Add foam pockets from 80% fill. Keep unavailable progress plain.
- Use semantic resources from `values/colors.xml` and `values-night/colors.xml`.
- Keep field-fill beer, foam, and bubbles on their Pints Fill-specific semantic resources so
  dark-mode changes do not change the mug palette. Dark-mode bubbles must not use the light count
  color. Keep the fill-count halo transparent in light mode and dark in night mode.
- Validate contrast and geometry for regular, compact, and icon variants.

## Visual QA

Inspect at native size and enlarged scale in both themes. Check:

- no viewport clipping;
- no handle/body intersection;
- foam visibly meets amber at 80% through full;
- full foam rises slightly above the rim without hiding the outline;
- thin strokes survive the 32 x 43 icon treatment;
- unavailable state remains distinct from empty.
- field-fill texture stays legible without obscuring `0`, `1`, or `100` in short, compact, and tall
  fields.

The README image is a separate, resource-accurate product illustration under `docs/images/`. Update
both SVG and PNG when its represented output changes. Do not label it an emulator screenshot unless
it was captured from an emulator.
