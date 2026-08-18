# Publishing

Releases are published to GitHub Packages as `co.branch:json-logic-kmp`, by
`.github/workflows/publish.yml`. That workflow fires only on `workflow_dispatch`. A dispatch can be
run against any ref, so it is also guarded to `main`: dispatching it from a feature branch is a
no-op rather than a publish.

One run produces six Maven publications. It runs on macOS so that the iOS publications, which
cannot be built anywhere else, go out alongside the rest in the same run:

| Publication           | Coordinates                                  |
|-----------------------|----------------------------------------------|
| `kotlinMultiplatform` | `co.branch:json-logic-kmp`                   |
| `jvm`                 | `co.branch:json-logic-kmp-jvm`               |
| `android`             | `co.branch:json-logic-kmp-android`           |
| `iosArm64`            | `co.branch:json-logic-kmp-iosarm64`          |
| `iosSimulatorArm64`   | `co.branch:json-logic-kmp-iossimulatorarm64` |
| `wasmJs`              | `co.branch:json-logic-kmp-wasm-js`           |

The first is the coordinate a consumer writes, and the only one: Gradle's Kotlin Multiplatform
metadata resolves the per-target artifact behind it. See
[Installation](README.md#installation) for the consuming side of that.

## Release process

1. On `main`, bump `version` in `lib/build.gradle.kts`, rename the `## [Unreleased]` heading in
   `CHANGELOG.md` to the version being released and its date, open a fresh empty `## [Unreleased]`
   above it, and update the coordinates in [Installation](README.md#installation). The empty heading
   is what gives the next change somewhere to land without deciding a version number for it.
2. From the `main` branch, dispatch the **Publish** workflow (`.github/workflows/publish.yml`) —
   either from the Actions tab or with `gh workflow run publish.yml --ref main`.
3. The workflow builds and tests every macOS-buildable lane, then publishes all publications to
   GitHub Packages. It only runs against `main`; dispatching it against any other ref is a no-op.
4. Tag the published commit and cut a GitHub Release from that tag, using the version just
   published: `git tag -a v<version> -m v<version> && git push origin v<version>`. GitHub Packages
   records no source revision alongside a version, so the tag is what ties a published artifact to
   the commit it was built from.

GitHub Packages will not overwrite a version that already exists, so a bad publish cannot be
re-pushed under the same coordinates — delete that version from the repository's Packages page, or
release the next patch version instead.
