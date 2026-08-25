# Release process

## Artifact classes

| Artifact | Purpose | Signing |
| --- | --- | --- |
| Local or CI debug APK | Development and Karoo smoke testing | Debug key, not distributable |
| Release APK from the standard build | Verification input | Intentionally unsigned |
| Published APK | User distribution | Stable project-owned private key |

GitHub Actions assigns each run a monotonically increasing `versionCode`, but its debug signing key
is ephemeral. Debug APKs from different runs may require uninstalling the prior build. Normal
verification pushes do not publish debug APKs; a manually dispatched verification run can produce a
short-lived artifact explicitly marked `UNSAFE-DEBUG`.

## Candidate gate

1. Start from the intended `main` commit with a clean tree.
2. Regenerate and validate drawables.
3. Run the full verification command from `AGENTS.md`.
4. Run the device matrix in `KAROO_DATA_FIELD_CONTRACT.md`.
5. Complete the security checklist in `SECURITY.md`.
6. Choose a unique increasing `versionCode` and the intended `versionName`.
7. Build from the audited commit, sign outside the repository, and verify the final APK.

## Automated signed release

Push a tag such as `v1.0.0` after the candidate gate. The tag-only release workflow builds with
that version name and its monotonically increasing GitHub Actions run number, verifies the signing
certificate and checksum, and publishes the APK to a GitHub Release. Configure the four
`PINT_*` signing secrets in the repository before using this workflow. Verification builds never
receive these secrets.

Example unsigned candidate build:

```bash
./gradlew -PpintVersionCode=NNN -PpintVersionName=X.Y.Z :pint:assembleRelease
```

## Release evidence

Record together:

- Git commit SHA;
- version name and code;
- APK SHA-256;
- signing certificate SHA-256 fingerprint;
- successful CI run;
- completed device matrix;
- release notes and known limitations.

Useful verification commands:

```bash
apkanalyzer manifest version-code app.apk
apkanalyzer manifest version-name app.apk
apksigner verify --print-certs app.apk
sha256sum app.apk
```

## Publication rules

- Never publish the CI debug artifact as a release.
- Never publish an unsigned release APK.
- Never rebuild after evidence collection. If bytes change, repeat verification and checksums.
- Keep package name `io.ericchernuka.pintprogress` and the signing identity stable for upgrades.
- If using a Karoo delivery manifest, ensure its URL, `latestVersionCode`, APK, and checksum agree.
