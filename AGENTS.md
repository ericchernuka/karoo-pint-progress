# Agent workflow

## Project map

- [`pint/`](pint/) contains the Pint Progress product code, tests, and Android resources.
- [`lib/`](lib/) is the vendored Karoo extension SDK; its provenance and modifications are in
  [`NOTICE`](NOTICE).
- [`tools/generate-drawables.mjs`](tools/generate-drawables.mjs) owns the mug drawable assets.
- [`docs/TEST_BOUNDARY.md`](docs/TEST_BOUNDARY.md) defines the local verification boundary and
  behavior that still needs an on-device Karoo smoke test.

## Verification

Use the repository's full verification command from [`README.md`](README.md):

```bash
./gradlew :lib:testDebugUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification
```

Before handoff, also run `git diff --check`. The canonical CI sequence is in
[`.github/workflows/verify.yml`](.github/workflows/verify.yml).

## Generated assets

Never hand-edit mug drawable variants. Change [`tools/generate-drawables.mjs`](tools/generate-drawables.mjs)
and rerun it to regenerate them. Keep light and night colors in resource qualifiers
([`pint/src/main/res/values/colors.xml`](pint/src/main/res/values/colors.xml) and
[`pint/src/main/res/values-night/colors.xml`](pint/src/main/res/values-night/colors.xml)).

## Security invariants

Without explicit maintainer approval, do not add Android permissions, network or storage access,
analytics, a WebView, native code, dynamic loading, public signing material, or a bypass of the
Karoo caller gate. Preserve CI's read-only, no-secrets boundary described in [`README.md`](README.md).

## Vendored SDK

Avoid changes under [`lib/`](lib/). If a change is unavoidable, preserve upstream headers, record
the modification in [`NOTICE`](NOTICE), and add focused tests around it.

## Karoo field behavior

Preserve the one-Hz view-update limit and cancellation behavior. Give all six `ViewConfig` inputs
explicit semantics: `gridSize`, `viewSize`, `textSize`, `alignment`, `boundariesEnabled`, and
`preview`. Do not treat merely reading a setting as proof that its behavior is correct. Retain the
on-device Karoo smoke-test requirement in [`README.md`](README.md) and
[`docs/TEST_BOUNDARY.md`](docs/TEST_BOUNDARY.md).

## Release

Debug APKs are for development only. Never commit keys or present intentionally unsigned output as
distributable; follow the release constraints in [`README.md`](README.md).
