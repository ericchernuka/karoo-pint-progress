# Plan 004: Keep the final fill update when a transition completes

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- pint/src/main/kotlin/io/ericchernuka/pintprogress/PintProgressDataType.kt pint/src/test/kotlin/io/ericchernuka/pintprogress/PintDataFieldRuntimeTest.kt docs/TEST_BOUNDARY.md`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P1
- Effort: M (S: hours; M: about one day, including checks)
- Risk: MED
- Depends on: 003
- Category: bug
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (local)

## Why this matters

The input mapper can read an active transition, then completion can detach it and read its old frame, then the mapper can write the new frame into the detached object and drop the input. The reducer already recorded that input and suppresses repeats. The display can stay stale until another fill bucket arrives. This interleaving was identified from source, not reproduced during the audit.

## Current state

`pint/src/main/kotlin/io/ericchernuka/pintprogress/PintProgressDataType.kt:197`

````text
        calorieStates
            .combine(caloriesPerBeer) { state, target -> state to target }
            .conflate()
            .mapNotNull { (state, target) ->
                val update = reducer.accept(state, target, activeTransition.get()?.completed)
                    ?: return@mapNotNull null
                if (update is PintViewUpdate.RefreshTransition) {
                    val transition = activeTransition.get()
                    if (transition?.completed == update.steady.progress.completed) {
                        transition.steady.set(update.steady)
                        null
                    } else {
                        PintViewUpdate.Render(update.steady)
                    }
                } else {
                    update
                }
            }
            .collectLatest { update ->
                when (update) {
                    is PintViewUpdate.Render -> emitAfter(0) { update.frame }

                    is PintViewUpdate.BeginTransition -> {
                        val transition = ActiveGraphicalTransition(
                            update.steady.progress.completed,
                            AtomicReference(update.steady),
                        )
                        activeTransition.set(transition)
                        try {
                            update.transientFrames.forEach { timedFrame ->
                                emitAfter(timedFrame.delayMillis) { timedFrame.frame }
                            }
                            emitAfter(update.steadyDelayMillis) {
                                activeTransition.compareAndSet(transition, null)
                                transition.steady.get()
                            }
                        } finally {
                            activeTransition.compareAndSet(transition, null)
                        }
                    }

                    is PintViewUpdate.RefreshTransition -> emitAfter(0) { update.steady }
````

`pint/src/main/kotlin/io/ericchernuka/pintprogress/core/PintViewReducer.kt:35`

````text
        val current = progressFrom(state, normalizedTarget)
        if (!targetChanged && previousTarget != null && current == previous) {
            previousCaloriesPerBeer = normalizedTarget
            return null
        }

        // Treat a settings change as a new baseline to prevent a false threshold celebration
        val plan = PintProgressReducer.plan(if (targetChanged) null else previous, current)
        previous = current
        previousCaloriesPerBeer = normalizedTarget
````

`pint/src/test/kotlin/io/ericchernuka/pintprogress/PintDataFieldRuntimeTest.kt:241`

````text
    @Test
    fun `same-pint calorie update refreshes the final frame without interrupting transition`() = runTest {
        val source = MutableStateFlow<StreamState>(streaming(149.0))
        val output = mutableListOf<PintFrame>()
        val times = mutableListOf<Long>()
        val job = backgroundScope.launch {
            runtime().runGraphicalStream(source, flowOf(150)) {
                output += it
                times += currentTime
            }
        }

        runCurrent()
        source.value = streaming(150.0)
        advanceSeconds(1)
        source.value = streaming(158.0)
        runCurrent()
        advanceSeconds(2)
        job.cancelAndJoin()

        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(0, 19)),
                PintFrame.FullBubbles(1),
                PintFrame.Draining(1),
                PintFrame.Steady(PintProgress(1, 1)),
            ),
            output,
        )
        assertEquals((0L..3_000L step 1_000L).toList(), times)
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
- `pint/src/test/kotlin/io/ericchernuka/pintprogress/PintDataFieldRuntimeTest.kt`
- `docs/TEST_BOUNDARY.md`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/004-atomic-transition-completion` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Reproduce the completion boundary

Extend the same-pint test with a controlled concurrency check that orders refresh and completion at their shared-state boundary. First try existing injected emit and clock functions. Cover both refresh-before-completion and completion-before-refresh. Avoid a probabilistic stress loop as the only regression. If the current private state prevents control, propose one internal state owner for this real concurrency boundary before implementation.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :pint:testDebugUnitTest --tests io.ericchernuka.pintprogress.PintDataFieldRuntimeTest` → regression exposes lost refresh on the old path; record exact failure.

### Step 2: Make shared-state decisions atomic

Use a short shared monitor for active-transition lookup, refresh-or-render, and completion detach-and-read. Installation and finally cleanup must use that same ownership rule. Hold no lock across delay, emit, collect, or any suspension. Remove atomic fields only if all accesses move under the monitor. Keep the reducer input order, collectLatest cancellation, and one-Hz pacing. A completed transition must cause a late refresh to render normally.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :pint:testDebugUnitTest --tests io.ericchernuka.pintprogress.PintDataFieldRuntimeTest` → exit 0; newest bucket is delivered for both orderings, with spacing at least 1000 ms.

### Step 3: Verify the full behavior contract

Retain same-pint transient frames, target-change baselines, unavailable/reset handling, and detach checks. Update the test boundary description only to name the actual new coverage.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` → exit 0; core instruction and branch coverage stay 100%.

## Test plan

Extend the existing runtime tests, which inject time and capture frames. Assert newest steady frame, no lost repeat, no extra completion animation, and emission spacing. Use explicit barriers for concurrent checks and bounded waits to fail cleanly. If a small internal state owner is approved, test its real production operations plus runtime routing; do not copy the algorithm into a test.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If the source does not permit the reported interleaving, stop and report evidence instead of applying a speculative lock. If testing requires a new public API or broad coordinator rewrite, stop for design review.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

Future changes to transition lifetime must keep refresh and final-frame capture under the same synchronization boundary. Plan 003 touches the same file and must be reconciled first.

## Execution results

Approved one internal GraphicalTransitionState in the existing file because the original private interleaving cannot be controlled by emit/clock callbacks. The owner serializes input acceptance, refresh, installation, completion, and cleanup without suspending under its monitor. Both semantic orderings pass. A contending refresh waits while completion owns the monitor and then returns the newest Render. Temporarily removing accept synchronization made that test fail (9 focused tests, 1 failure); restoring it passes the complete product/QA gate. This mutation check validates the new boundary; it does not claim reproduction of the original private race. Existing pacing, transient frames, reset, target changes, and cancellation tests remain. Resource validation and diff hygiene passed. Device checks remain pending.
