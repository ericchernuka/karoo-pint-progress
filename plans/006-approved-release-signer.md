# Plan 006: Reject APKs signed by an unapproved certificate

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- .github/workflows/release.yml docs/RELEASE.md tools/verify-release-signer.py tools/test_verify_release_signer.py`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P2
- Effort: S (S: hours; M: about one day, including checks)
- Risk: LOW
- Depends on: 005
- Category: security
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (local; approved fingerprint configuration pending)

## Why this matters

apksigner verify --print-certs checks signature validity and prints signer identity, but release CI never compares that identity against the approved project key. A valid APK signed by another key can be published and fail upgrades.

## Current state

`.github/workflows/release.yml:59`

````text
      - name: Verify release APK
        run: |
          APK=pint/build/outputs/apk/release/pint-release.apk
          test "$(apkanalyzer manifest version-code "$APK")" = "${GITHUB_RUN_NUMBER}"
          test "$(apkanalyzer manifest version-name "$APK")" = "${GITHUB_REF_NAME#v}"
          "$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose --print-certs "$APK"
          sha256sum "$APK" > "$APK.sha256"
      - name: Publish GitHub release
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          gh release create "$GITHUB_REF_NAME" \
            pint/build/outputs/apk/release/pint-release.apk \
            pint/build/outputs/apk/release/pint-release.apk.sha256 \
            --generate-notes \
            --latest
````

`docs/RELEASE.md:76`

````text
## Publication rules

- Never publish the CI debug artifact as a release.
- Never publish an unsigned release APK.
- Never rebuild after evidence collection.
- Keep package name `io.ericchernuka.pintprogress` and the signing identity stable for upgrades.
- If using a Karoo delivery manifest, ensure its URL, `latestVersionCode`, APK, and checksum agree.
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
- `docs/RELEASE.md`
- `tools/verify-release-signer.py`
- `tools/test_verify_release_signer.py`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/006-approved-release-signer` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Resolve the expected identity

Obtain the approved public SHA-256 certificate fingerprint from a maintainer-approved prior release record. Never derive the expected value from the APK currently under test. Specify a repository variable PINT_SIGNER_SHA256 in the workflow and document who supplies it. Preparing this change does not authorize changing remote variables or reading private keys.

**Verify:** `git diff --check` → exit 0; public-identity source and missing-variable behavior documented.

### Step 2: Add a narrow signer comparison

Keep apksigner verification. Feed its successful output and the expected public fingerprint into a stdlib Python checker. Accept exactly one signer certificate SHA-256 digest equal to the configured 64-hex value after case normalization; reject missing, malformed, different, or multiple signer digests. Do not accept a matching digest among unknown extra signers. Run the check before publication. Check the installed apksigner output format first; do not parse unrelated source-stamp digests.

**Verify:** `python3 -m unittest discover -s tools -p test_verify_release_signer.py` → exit 0; all acceptance and rejection cases pass.

### Step 3: Document and verify the gate

Document missing configuration as a release block and preserve unsigned local builds. Run product verification and inspect the workflow order.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` → exit 0.

## Test plan

Use stdlib unittest with public dummy 64-hex fingerprints and synthetic apksigner output. Cases: exact match, uppercase match, wrong signer, missing expected value, malformed expected value, no signer, multiple signers. Confirm actual installed apksigner output shape from an available non-secret APK; do not generate or access project signing keys. Live publication is outside this plan.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If the approved fingerprint is unavailable, leave release configuration pending and report it; never invent one. If the app uses certificate rotation or multiple approved signers, stop for an explicit policy.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

Fingerprint rotation is an explicit release decision. Plan 007 must retain this check when separating build and publication.

## Execution results

The stdlib checker passes exact/case-normalized identity and rejects wrong, missing, malformed, multiple, and source-stamp-only identity. The stub failed seven negative cases before validation was implemented; the full suite now passes. Installed apksigner 34.0.0 output was checked on the development APK; its public debug certificate is not the approved release certificate. The workflow preserves signature verification before comparison. PINT_SIGNER_SHA256 remains unset pending a maintainer-approved public release record, and the check fails closed. Full product/QA gate passed in the prior milestone; this commit changes only workflow, docs, and stdlib checks. git diff --check passed. No remote changes or private key access.
