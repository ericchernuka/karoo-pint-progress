# Drawable workflow

## Source of truth

`tools/generate-drawables.mjs` owns all mug and field-fill vector variants and
`PintAssetDrawables.kt` and `core/PintAsset.kt`. Never edit generated drawable XML or either Kotlin
mapping directly.

Mug states include 0% through 95% in 5% steps, full bubbles, draining, and unavailable. Each mug
state has regular, compact, and icon variants. Field-fill states include 0% through 95% in 5% steps
and unavailable. `ic_pint.xml` is the shared extension icon.

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
alignment wrappers, responsive mug bounds, initial count visibility, field labels, extension
metadata, and a representative generated drawable mapping. Run it when any of those resources
change, even when mug geometry does not.

## Visual contracts

- Preserve the `83 x 112` viewport and straight-sided mug silhouette.
- Keep handle joins behind the opaque mug body to prevent antialiasing seams.
- Keep normal amber and foam inside the inner glass.
- Only `pint_full_bubbles` may crown the rim. It must remain inside the viewport and mug width.
- Preserve the mug aspect ratio in every mug layout. The normalized field-fill rectangle uses
  `fitXY` because its purpose is to cover the host content rectangle.
- Use semantic resources from `values/colors.xml` and `values-night/colors.xml`.
- Validate contrast and geometry for regular, compact, and icon variants.

## Visual QA

Inspect at native size and enlarged scale in both themes. Check:

- no viewport clipping;
- no handle/body intersection;
- foam visibly meets amber at 80% through full;
- full foam rises slightly above the rim without hiding the outline;
- thin strokes survive the 32 x 43 icon treatment;
- unavailable state remains distinct from empty.

The README image is a separate, resource-accurate product illustration under `docs/images/`. Update
both SVG and PNG when its represented output changes. Do not label it an emulator screenshot unless
it was captured from an emulator.
