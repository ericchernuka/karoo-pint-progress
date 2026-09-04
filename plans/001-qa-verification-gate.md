# Plan 001: Include the QA driver in repository verification

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- .github/workflows/verify.yml AGENTS.md README.md docs/TEST_BOUNDARY.md`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P2
- Effort: S (S: hours; M: about one day, including checks)
- Risk: LOW
- Depends on: none
- Category: tests
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (local)

## Why this matters

The included :calorie-source Android application has tests and resources, but normal CI and the documented full gate only build lib and pint. A broken QA driver can therefore pass the full gate and delay device verification.

## Current state

`.github/workflows/verify.yml:37`

````text
      - name: Verify and build
        run: >-
          ./gradlew
          -PpintVersionCode="${GITHUB_RUN_NUMBER}"
          -PpintVersionName="1.0.0-dev.${GITHUB_RUN_NUMBER}"
          :lib:testDebugUnitTest
          :pint:testReleaseUnitTest
          :pint:lintDebug
          :pint:assembleDebug
          :pint:assembleRelease
          :pint:jacocoBehaviorTestCoverageVerification
          --no-daemon
````

`settings.gradle.kts:12`

````text
include(":calorie-source")
project(":calorie-source").projectDir = file("tools/karoo-calorie-source")
````

`docs/TEST_BOUNDARY.md:4`

````text

```bash
# Focused JVM suite
./gradlew :pint:testDebugUnitTest

# Controlled Karoo Calories source
./gradlew :calorie-source:testDebugUnitTest :calorie-source:assembleDebug

# Dokka HTML documentation
./gradlew :lib:dokkaGeneratePublicationHtml
python3 tools/verify-dokka-output.py

# Full repository gate
./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification

# Generated drawable and static-resource contracts
node tools/generate-drawables.mjs
node tools/validate-drawables.mjs
```
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

- `.github/workflows/verify.yml`
- `AGENTS.md`
- `README.md`
- `docs/TEST_BOUNDARY.md`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/001-qa-verification-gate` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Establish the QA baseline

Run `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` before edits. Record any existing failure. Do not fix unrelated failures.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` → exit 0; QA tests, lint, and debug assembly succeed.

### Step 2: Extend the existing gate

Add :calorie-source:testDebugUnitTest, :calorie-source:lintDebug, and :calorie-source:assembleDebug to the existing Verify and build step. Add the same tasks to each documented full gate. Preserve the separate pint-only focused command and existing debug artifact upload path. Do not upload the QA APK.

**Verify:** `rg -n ':calorie-source:(testDebugUnitTest|lintDebug|assembleDebug)' .github/workflows/verify.yml AGENTS.md README.md docs/TEST_BOUNDARY.md` → each full gate includes all three tasks.

### Step 3: Run the combined gate

Run the product and QA checks together. Keep the existing release tests and core coverage gate.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` → exit 0; core coverage remains 100%.

## Test plan

Use existing QA JUnit tests. No new tests that only inspect workflow text. Verify the new tasks with Gradle and inspect the upload path in the diff.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If QA lint already fails, report the failure and request a scoped follow-up before changing application code.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

When an Android module is added, update the full gate. QA artifacts remain local development inputs.

## Execution results

QA baseline passed. The combined product and QA gate passed (243 tasks); resource validation and git diff --check passed. All four full gate locations include the three QA tasks. Debug upload path is unchanged. Commands used the installed Android SDK and shared Gradle cache. Existing SDK XML and vendored Kotlin annotation warnings remain. Remote CI was not dispatched.
