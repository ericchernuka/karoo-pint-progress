# Plan 005: Check every generated file before release builds

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- .github/workflows/release.yml .github/workflows/verify.yml`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P2
- Effort: S (S: hours; M: about one day, including checks)
- Risk: LOW
- Depends on: none
- Category: dx
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (local)

## Why this matters

The generator writes drawable XML and two Kotlin files. The release diff gate checks only XML and PintAssetDrawables.kt, so it can build an uncommitted generated core/PintAsset.kt. Verify CI has a later whole-tree check, but release CI does not.

## Current state

`tools/generate-drawables.mjs:1`

````text
import fs from "node:fs";
import path from "node:path";

const destination = path.resolve("pint/src/main/res/drawable");
const kotlinDestination = path.resolve(
  "pint/src/main/kotlin/io/ericchernuka/pintprogress/PintAssetDrawables.kt",
);
const assetDestination = path.resolve(
  "pint/src/main/kotlin/io/ericchernuka/pintprogress/core/PintAsset.kt",
);
fs.mkdirSync(destination, { recursive: true });
````

`.github/workflows/release.yml:41`

````text
      - name: Verify generated drawables
        run: node tools/generate-drawables.mjs && node tools/validate-drawables.mjs && git diff --exit-code -- pint/src/main/res/drawable pint/src/main/kotlin/io/ericchernuka/pintprogress/PintAssetDrawables.kt
      - name: Build signed release
````

`.github/workflows/verify.yml:29`

````text
      - name: Verify generated drawables
        run: node tools/generate-drawables.mjs && node tools/validate-drawables.mjs && git diff --exit-code -- pint/src/main/res/drawable pint/src/main/kotlin/io/ericchernuka/pintprogress/PintAssetDrawables.kt
      - name: Verify Dokka output
        run: |
          set -euo pipefail
          ./gradlew :lib:dokkaGeneratePublicationHtml --rerun-tasks
          python3 tools/verify-dokka-output.py
          test -z "$(git status --porcelain=v1)"
````

## Conventions and commands

Run commands from the repository root. This is a Kotlin Android app with a vendored Karoo SDK in lib, pure product decisions under pint/core, Android adapters, JUnit tests, and a separate QA application. Existing JUnit style uses `@Test` and `assertEquals(expected, actual)`; see tools/karoo-calorie-source/src/test/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStateTest.kt. Workflow steps use existing pinned Actions and shell commands. Prefer native shell/Python stdlib checks over a new dependency.

| Purpose | Command | Expected |
| --- | --- | --- |
| Full product gate | `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` | Exit 0, including 100% core instruction and branch coverage |
| QA gate | `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` | Exit 0 |
| Static resources | `node tools/validate-drawables.mjs` | Drawable visual contracts passed |
| Patch hygiene | `git diff --check` | Exit 0 |
| Scope | `git status --short` and `git diff --name-only` | Only approved scope plus pre-existing user edits |

The audit ran resource validation and patch hygiene successfully. It did not run Gradle or hardware checks. These build commands come from repository configuration, not a claimed successful audit build. Use another valid ANDROID_HOME if this machine-specific SDK path is absent. Do not install tools or dependencies without approval.

## Scope

Only these files may change:

- `.github/workflows/release.yml`
- `.github/workflows/verify.yml`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/005-generated-release-gate` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Make the generated-output check complete

After generation and validation, require a clean Git tree in both existing generated-output steps. Use git status --porcelain=v1 so tracked edits and new generated files both fail. Keep the Dokka-specific check. This is a CI clean-checkout contract; do not run this assertion against a developer tree with approved edits.

**Verify:** `git diff --check` → exit 0; both workflow edits are limited to output verification.

### Step 2: Exercise the gate in a disposable copy

Create a temporary copy of the repository at the implementation commit, outside the user checkout. Run generation and the gate: it must pass. In the copy only, insert a harmless comment into generator output for core/PintAsset.kt, commit that generator-only change locally if necessary to isolate generated drift, then regenerate: the gate must fail. Also create an untracked generated-file fixture and confirm failure. Remove only the disposable copy.

**Verify:** `node tools/validate-drawables.mjs` → exit 0 in the real checkout; disposable gate cases return 0, nonzero, nonzero.

### Step 3: Run product verification

Run the full product gate after the workflow check.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` → exit 0.

## Test plan

Use executable clean-tree and drift cases in a disposable checkout. Do not add a test that only matches workflow strings. Never mutate generated assets in the user tree for the negative test.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If the generator is already non-deterministic on a clean checkout, stop and report its exact changed paths. Do not fix generator content under this plan.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

Keep the gate independent of a hard-coded output list so a future generator output is covered.

## Execution results

Both workflows now require clean porcelain status after generation and validation. A disposable initialized repository passed clean generation, rejected generated core mapping drift from a committed generator-only fixture change, and rejected an untracked drawable fixture. Product/QA full gate passed at the runtime milestone with these workflow edits present. Resource validation and diff hygiene passed. No generator or generated source edits entered this commit.
