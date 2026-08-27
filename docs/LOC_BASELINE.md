# Maintained source LOC baseline

Baseline commit: `a86f780f5a0da69b8f63d7a4f86a242b71f2b9ac`

The measurement counts physical lines, including blank and comment lines, in tracked project-owned
Kotlin, XML, JavaScript, and Gradle files. It excludes `lib/`, build and Gradle caches, generated mug
drawable XML (`pint/src/main/res/drawable/pint_*.xml` and `ic_pint.xml`), the generated
`PintAssetDrawables.kt`, Gradle verification metadata, documentation, binaries, and lockfiles.

Run this command from the repository root for both baseline and final measurements:

```bash
git ls-files | rg '\.(kt|xml|js|mjs|gradle|gradle\.kts)$' \
  | rg -v '^(lib/|.*/build/|.*/\.gradle/|pint/src/main/res/drawable/(pint_|ic_pint\.xml$)|pint/src/main/kotlin/io/ericchernuka/pintprogress/PintAssetDrawables\.kt$|gradle/verification-metadata\.xml$)' \
  | xargs wc -l
```

Baseline result: **3,456 lines**.

## Removed implementation-detail test

`PintProgressReducerTest.progress and render models expose their values` only repeated Kotlin data
class property behavior. The remaining reducer, view reducer, presentation, and runtime tests still
construct and compare every model type through the product's render and animation behavior.
