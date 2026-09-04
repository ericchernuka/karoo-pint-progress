# Improvement execution

The user authorized all seven fixes and two design studies as separate commits, with a separate Sol Medium review. Work is in the same Git repository on branch `ec/improve-all`, checked out at `/private/tmp/karoo-improve-all`. The original task checkout remains at its starting commit; these commits have not been merged or pushed.

Baseline: `da70ab23de201215f2a391cdccda29affb67fd51`. Execution date: 2026-09-04.

## Execution order and status

| Plan | Commit | Status |
| --- | --- | --- |
| [001: Include the QA driver in repository checks](001-qa-verification-gate.md) | `aa13283` | DONE locally |
| [002: Start fresh QA output paused](002-pause-qa-startup.md) | `2d3b69e` | DONE locally |
| [003: Register field cancellation before starting work](003-cancel-before-start.md) | `762ff14` | DONE locally |
| [004: Keep fill refresh and transition completion atomic](004-atomic-transition-completion.md) | `9985318` | DONE locally |
| [005: Check all generated outputs before builds](005-generated-release-gate.md) | `44b5407` | DONE locally |
| [006: Reject unapproved release signers](006-approved-release-signer.md) | `4e72734` | DONE locally |
| [007: Publish the approved candidate without rebuilding](007-promote-tested-apk.md) | `dae9d51` | DONE locally |
| [008: Record the QA target preset decision](008-target-aware-qa-design.md) | `842470d` | DONE locally |
| [009: Record the guided QA sequence decision](009-guided-qa-design.md) | `a6208c7` | DONE locally |

The nine plan commits remain intact. A tenth additive review-correction commit contains the GitHub path fix and final review records; no history rewrite is used. Plans 008 and 009 are completed design studies, not selector or sequence-runner features. Both recommend retaining the corrected fixed-target manual workflow until there is a demonstrated need and an approved follow-up design.

## Verification

The executor ran focused regression checks before and after fixes. The advisor independently reran the full product and QA Gradle gate; it passed, including `jacocoBehaviorTestCoverageVerification` at the required 100% instruction and branch threshold for core. There is no separate JaCoCo XML report configured.

The advisor also verified all six Python release test groups, candidate CLI record/unpack/verify with synthetic archive/API fixtures and negative identity cases, clean/core-drift/untracked generated-output cases, resource validation, YAML parsing, all workflow shell blocks with `bash -n`, and commit-range patch hygiene. The transition contention test fails when synchronization is removed. The old private race was established by source analysis, not a runtime reproduction.

Sol Medium preflight feedback was incorporated into the contention check, full QA gate, artifact/run-attempt binding, and failed/waived approval rules. Final Sol review found one API-path defect, corrected with realistic suffixed-path fixtures; the focused Sol recheck returned APPROVE with no actionable findings and five candidate tests passed. The index bookkeeping finding is addressed by this execution record. See [the review record](REVIEW.md). These are local checks; they do not establish real GitHub permissions or device results.

The review-correction commit contains the final status records as well as the small source fix. Use `git log --oneline da70ab2..ec/improve-all` to view all ten commits.

## Pending release prerequisites

- Configure the approved public `PINT_SIGNER_SHA256` from a maintainer-approved identity. Missing configuration blocks preparation; no fingerprint was invented.
- Merge the workflow/checker changes through the normal repository process before publishing, then perform an authorized prepare/publish verification.
- Run the applicable Karoo device matrix and record approval for the exact signed candidate.

No device install, tag creation, remote settings change, workflow dispatch, issue, push, or release publication occurred in this task.

## Dependency and scope notes

001 precedes QA startup. 003 and 004 share the runtime file. 005, 006, and 007 preserve the generated-output and approved-signer gates in sequence. Plan 007 also updates README, Verify CI, and test-command documentation because the new release contract and Python suites require those changes. Its preparation manifest and later approval bind artifact identity without a circular archive digest. One internal transition owner was approved in the existing runtime file; no public hook or new dependency was added.

## Findings considered and rejected

- Permanent cancellation leak: the SDK CancellationHandle invokes late callbacks. The retained finding is the smaller worker-start window.
- Missing Binder authorization: inspected Binder methods enforce caller policy.
- Generated-output omission in normal Verify CI: its later whole-tree check catches drift. Release CI lacks that check; plan 005 makes both local generated steps complete.
- Numeric flooring and paced in-flight output: existing tests specify this behavior.
- BigDecimal conversion optimization: bounded one-Hz work has no measured performance defect.
- Empty preview list guard: current production callers supply fixed nonempty lists.
- Broad rendering test rewrite: physical rendering is an explicit device boundary.
- Automatic proof of dropout: KOS behavior needs device evidence; source alone cannot establish a defect.
- Broad refactor or new dependencies: no demonstrated requirement.
