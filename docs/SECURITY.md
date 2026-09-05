# Security model

## Trust boundaries

- The exported extension service is required for Karoo discovery. Binder calls are accepted only
  from the Karoo system package through `allowsKarooCaller`.
- The launcher activity exposes only a non-sensitive calorie preference. It accepts no intent data.
- Preferences use app-private storage and contain only calories per beer.
- The app consumes Karoo's calorie stream and emits package-owned static resources.

## Prohibited by default

Do not add these without explicit maintainer approval and a documented threat review:

- Android permissions;
- network or external storage access;
- analytics, telemetry, advertising, or crash-reporting SDKs;
- WebViews, native code, dynamic loading, or executable downloads;
- public signing keys, keystores, passwords, tokens, or encoded credentials;
- relaxed caller authorization or exported components with privileged actions.

## Change checklist

1. Inspect manifest changes and the merged manifest.
2. Review new dependencies and every transitive artifact.
3. Confirm `gradle/verification-metadata.xml` changes match the intended dependency graph.
4. Search the repository and diff for credentials, signing files, unexpected URLs, and permissions.
5. Run caller-policy tests and the full build.
6. Confirm CI actions remain pinned to immutable commit SHAs. Keep verification permissions
   read-only; limit release write permission to the draft-release job.
7. For release changes, review artifact provenance and checksum checks, the independently approved
   signer, and the manual draft publication boundary. Run the release checker tests listed in
   `AGENTS.md` and follow the remote validation and evidence requirements in [RELEASE.md](RELEASE.md).

Useful checks:

```bash
rg -n '<uses-permission|android.permission|WebView|DexClassLoader|System\.load' pint/src
rg -n 'BEGIN .*PRIVATE KEY|github_pat_|ghp_|AKIA' . --glob '!**/.git/**'
git diff -- gradle/libs.versions.toml gradle/verification-metadata.xml pint/src/main/AndroidManifest.xml
```

These searches are guardrails, not proof. Review behavior and data flow directly.

## Signing

Signing material never belongs in the repository or CI logs. A distributable APK must use one
project-owned private key. Record only public certificate fingerprints with release evidence.
