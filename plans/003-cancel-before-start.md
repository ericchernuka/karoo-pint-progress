# Plan 003: Install cancellation before starting field workers

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- pint/src/main/kotlin/io/ericchernuka/pintprogress/PintProgressDataType.kt pint/src/testRelease/kotlin/io/ericchernuka/pintprogress/PintCancellationReleaseTest.kt`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P2
- Effort: S (S: hours; M: about one day, including checks)
- Risk: LOW
- Depends on: none
- Category: bug
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (local)

## Why this matters

launchCancellable starts on Dispatchers.Default before setting the callback. An emitter already cancelled by the host can start work before late callback registration cancels it. The vendored SDK handles late registration, so this is a startup window, not a permanent leak.

## Current state

`pint/src/main/kotlin/io/ericchernuka/pintprogress/PintProgressDataType.kt:132`

````text
internal fun Emitter<*>.launchCancellable(
    label: String,
    block: suspend CoroutineScope.() -> Unit,
) {
    val job = CoroutineScope(Dispatchers.Default).launch(block = block)
    setCancellable {
        job.cancel()
        if (BuildConfig.DEBUG) Log.d("PintProgressField", "cancellation label=$label")
    }
}
````

`pint/src/testRelease/kotlin/io/ericchernuka/pintprogress/PintCancellationReleaseTest.kt:10`

````text

class PintCancellationReleaseTest {
    @Test
    fun `preview cancellation callback cancels numeric and graphical jobs`() = runBlocking {
        for (style in PintFieldStyle.entries) {
            val label = style.cancellationLabel(preview = true)
            val emitter = CancellableEmitter()
            val started = CountDownLatch(1)
            val cancelled = CountDownLatch(1)

            emitter.launchCancellable(label) {
                started.countDown()
                try {
                    awaitCancellation()
                } finally {
                    cancelled.countDown()
                }
            }

            assertTrue(started.await(1, SECONDS))
            requireNotNull(emitter.callback).invoke()
            assertTrue(cancelled.await(1, SECONDS))
        }
    }
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

- `pint/src/main/kotlin/io/ericchernuka/pintprogress/PintProgressDataType.kt`
- `pint/src/testRelease/kotlin/io/ericchernuka/pintprogress/PintCancellationReleaseTest.kt`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/003-cancel-before-start` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Cover an already cancelled emitter

Extend the release-test fake so cancel is remembered and setCancellable invokes immediately when already cancelled, matching the SDK CancellationHandle. Add a test that verifies the Job is inactive when callback registration invokes cancellation; retain the live cancellation test. Use an observable registration barrier or bounded latch to expose the eager-start ordering, without sleeps.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :pint:testReleaseUnitTest` → new regression demonstrates the old startup order; existing test remains meaningful.

### Step 2: Start the job after registration

Use CoroutineStart.LAZY in the shared helper. Install the existing cancellation callback, then call job.start(). Keep Dispatchers.Default and debug-only logging. All numeric and graphical live/preview callers retain this helper.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :pint:testReleaseUnitTest` → exit 0; pre-cancelled worker body never starts and running worker cancels.

### Step 3: Check all product behavior

Run the complete product gate.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` → exit 0.

## Test plan

Use PintCancellationReleaseTest, since debug cancellation logging calls Android Log. Test already-cancelled registration and cancellation after startup. Synchronize the test; do not rely on a lucky worker schedule. Do not modify lib.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If a reliable regression requires a public production test hook or a new coroutine dependency, stop and propose the smallest test boundary first.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

Future worker routes must use the same start order. This plan does not alter QA sensor startup; that adjacent window remains a separate follow-up.

## Execution results

The registration-barrier regression failed with eager launch (10 release tests, 1 failure). Release tests pass with lazy launch followed by registration and start. The fake remembers prior cancellation as the SDK does. Existing live cancellation checks remain. The barrier uses a bounded latch, not sleeps. Full gate is batched with plan 004. No SDK edits or dependencies were added.
