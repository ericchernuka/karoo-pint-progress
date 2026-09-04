# Plan 007: Publish the exact approved candidate without rebuilding

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- .github/workflows/release.yml docs/RELEASE.md docs/RELEASE_EVIDENCE_TEMPLATE.md tools/verify-release-candidate.py tools/test_verify_release_candidate.py`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P1
- Effort: M (S: hours; M: about one day, including checks)
- Risk: MED
- Depends on: 005, 006
- Category: security
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (local; remote and device verification pending)

## Why this matters

The candidate process tests before its final signed build, and the tag workflow then rebuilds and publishes with a workflow run-number version. The release policy invalidates evidence after any byte, commit, or signer change. The publication path does not consume the approval record. Build once, test that signed artifact, and promote those exact bytes.

## Current state

`.github/workflows/release.yml:43`

````text
      - name: Build signed release
        env:
          PINT_KEY_ALIAS: ${{ secrets.PINT_KEY_ALIAS }}
          PINT_KEY_PASSWORD: ${{ secrets.PINT_KEY_PASSWORD }}
          PINT_KEYSTORE_PASSWORD: ${{ secrets.PINT_KEYSTORE_PASSWORD }}
          PINT_KEYSTORE_BASE64: ${{ secrets.PINT_KEYSTORE_BASE64 }}
        run: >-
          ./gradlew
          -PpintVersionCode="${GITHUB_RUN_NUMBER}"
          -PpintVersionName="${GITHUB_REF_NAME#v}"
          :lib:testDebugUnitTest
          :pint:testReleaseUnitTest
          :pint:lintDebug
          :pint:assembleRelease
          :pint:jacocoBehaviorTestCoverageVerification
          --no-daemon
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

`docs/RELEASE.md:17`

````text

1. Start from the intended `main` commit with a clean tree.
2. Regenerate and validate drawables.
3. Run the full verification command from `AGENTS.md`.
4. Run the device matrix in `KAROO_DATA_FIELD_CONTRACT.md`.
5. Complete the security checklist in `SECURITY.md`.
6. Choose a unique increasing `versionCode` and the intended `versionName`.
7. Build from the audited commit, sign outside the repository, and verify the final APK.
8. Complete a copy of the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md). Keep every
   failed and waived row visible. For a pre-release candidate, paste the completed record into the
   pull request description or a dedicated PR comment before moving the candidate to `main`.
   Upload photos, recordings, and logs as PR attachments and link them from the record. Do not
   commit candidate evidence or captures unless a maintainer explicitly requests it.

## Automated signed release

Push a tag such as `v1.0.0` after the candidate gate. The tag-only release workflow builds with
that version name and its monotonically increasing GitHub Actions run number, verifies the signing
certificate and checksum, and publishes the APK to a GitHub Release. Configure the four
`PINT_*` signing secrets in the repository before using this workflow. Verification builds never
receive these secrets.
````

`docs/RELEASE.md:45`

````text
## Release evidence

For every published release, include a completed copy of the [release evidence
template](RELEASE_EVIDENCE_TEMPLATE.md) in the GitHub Release notes or attach it as a Markdown
evidence file. Record together:

- Git commit SHA;
- version name and code;
- APK SHA-256;
- signing certificate SHA-256 fingerprint;
- successful CI run;
- completed device matrix;
- release notes and known limitations.

The template is the record format. Keep failed and waived rows visible, with the reason and
approver. If the APK bytes, candidate commit, or signing identity changes after evidence
collection, the record is invalid and the relevant checks must run again.
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
- `docs/RELEASE_EVIDENCE_TEMPLATE.md`
- `tools/verify-release-candidate.py`
- `tools/test_verify_release_candidate.py`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/007-promote-tested-apk` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Specify the candidate contract before workflow edits

In docs/RELEASE.md define two explicit manual modes in the existing release workflow: prepare candidate and publish candidate. Prepare takes exact commit, version name, and increasing version code and builds/signs once into a GitHub Actions artifact. Publish takes immutable successful preparation run/artifact identity plus completed approval evidence, downloads that artifact, verifies it, and publishes without Gradle or signing secrets. Remove the old tag-triggered rebuild path. A release tag must resolve to the candidate commit. Use official GitHub CLI/Actions documentation to verify artifact lookup and permission behavior before coding. If the operator prefers another storage surface, stop for that material choice.

**Verify:** `git diff --check` → exit 0; both modes, input identity, retention failure, and permissions are specified.

### Step 2: Implement identity validation with local regression checks

Add a stdlib checker for a small candidate identity record: commit, package, version name/code, APK SHA-256, signer SHA-256. Compare independently supplied expected values and metadata measured from the downloaded APK; never trust filenames alone. Require approval decision, approver, and evidence reference; preserve failed/waived rows. APPROVED WITH WAIVERS needs explicit waiver entries. The workflow must verify artifact run provenance and tag commit, then execute this checker before release creation. Approval is a maintainer assertion; do not claim a JSON parser proves device checks occurred.

**Verify:** `python3 -m unittest discover -s tools -p test_verify_release_candidate.py` → exit 0; mismatches, missing approval, and malformed records fail closed.

### Step 3: Separate preparation from publication

Keep existing full verification and generated gate in prepare. Keep project secrets only in prepare. Capture version code before device tests and never replace it with the publish run number. Use pinned existing Actions or the installed gh CLI; new actions need verified immutable pins. Publish must recheck checksum, APK metadata, approved signer, provenance, and evidence identity; attach the evidence and APK checksum to the release. Disable any bypass path. Downloads and publication should fail if artifacts expire; no rebuild fallback.

**Verify:** `python3 -m unittest discover -s tools -p "test_verify_release_*.py"` → exit 0; all release checker suites pass.

### Step 4: Correct the operator sequence

Order docs as: choose identity, prepare signed artifact, install/test that artifact, record hash/signer and approval, then publish those bytes. Explain version-code monotonicity against the previous published APK, artifact retention, missing evidence, and tag mismatch. Run local checks; keep an actual remote dry run and publication pending operator authorization.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` → exit 0; local implementation checks pass, remote verification explicitly pending.

## Test plan

Test exact identity success and one-field mismatches for APK bytes, SHA, package, version code/name, signer, commit and approval. Cover missing artifact/evidence, rejected approval, and waivers without reasons. Add a fixture showing two different publish run numbers cannot alter candidate identity. Use temporary files and dummy hashes, never project signing material. Before live use, an authorized controlled run must prove downloaded and published hashes match; local tests alone do not validate GitHub permissions.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If artifact identity cannot be tied to the selected successful preparation run, stop. If approval provenance or artifact retention needs an external service, stop. Do not dispatch workflows, create tags, change remote settings, publish, commit, or push without separate instruction.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

Plans 005 and 006 modify this workflow first; retain their guarantees when refreshing excerpts. There must be only one publication path. Expired candidates require new preparation and new evidence, not reuse of old approval.

## Execution results

Implemented the approved GitHub Actions artifact transport with separate manual prepare and publish
jobs. Candidate commit must equal preparation dispatch SHA. Preparation runs the full product and
QA gate, signs once, records identity, and uploads a 30-day artifact named by run ID and attempt.
Publication checks the exact attempt API, successful prepare job, artifact ID/name/run/SHA/archive
digest, checksum basename and bytes, measured APK metadata, approved signer, existing tag SHA, and
complete approval rows before release creation. Publication uses no Gradle or signing secrets and
retains generated release notes. Missing fingerprint, expired artifact, absent evidence, failed rows,
and incomplete waivers block publication. Maintainer approval remains an assertion, not device proof.

Required scope additions were approved: README.md for the obsolete tag-only release paragraph,
.github/workflows/verify.yml and docs/TEST_BOUNDARY.md for the Python checker gate. Both checkers now
run in normal Verify CI without Python bytecode output. No dependencies or new Actions were added.

Tests first rejected the candidate checker stub. Six release test groups now pass, including each
identity field, preparation attempt/artifact mismatches, evidence completeness and waivers, archive
path rejection, and the actual CLI record/unpack/verify flow with measured metadata rejection. Ruby
parsed both workflow YAML files and bash -n passed every run block. Drawable validation and patch
hygiene passed. Full product/QA Gradle gate passed at the runtime milestone and was independently
rerun by the advisor. Actual GitHub preparation/publication and device checks remain pending; local
fixtures cannot establish permissions, artifact retention service behavior, or physical results.

Official artifact, workflow-run-attempt, workflow syntax, gh api and gh release create documentation
was read. Installed gh help and upstream create.go confirm notes-file populates the same release
body as notes and works with generated notes. The approved public PINT_SIGNER_SHA256 remains
configuration work for the maintainer. No workflow dispatch, tag, publication, or remote settings
change occurred.

Review correction: tag lookup uses the explicit tags/ ref namespace, so a same-name branch cannot satisfy the candidate tag check.

Sol Medium review found the documented GitHub workflow path ref suffix was rejected. Realistic @main and @refs/heads/ec/candidate fixtures failed before the correction; comparing the path before its @ suffix passes both while rejecting a wrong workflow with a suffix. The complete release checker suite passes after the fix.
