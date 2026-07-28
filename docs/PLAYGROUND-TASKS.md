# Playground implementation tasks

Tracking doc for [AMP-3994](https://linear.app/branch/issue/AMP-3994/jsonlogic-playground-cmpwasm-github-pages)
— a Compose Multiplatform / Wasm playground for this library, deployed to GitHub Pages.

This doc is temporary and is deleted before the PR is opened.

Legend: `[ ]` not started · `[~]` in progress · `[x]` complete.

## Tasks

- [x] **0 — Tracking doc + branch rename.** This file; branch renamed to Linear's suggested name.
- [x] **1 — Gradle wiring + "hello" bundle.** `include(":playground")`, version catalog entries for
      Compose Multiplatform 1.11.1 / Material 3 1.9.0 / kotlinx-browser 0.5.0, BCV
      `ignoredProjects`, `playground/build.gradle.kts`, `index.html`, `Main.kt`, placeholder `App`.
- [x] **2 + 3 — Theme, shell, and live evaluation.** Merged: a shell with inert text boxes is not
      independently verifiable, so the theme, layout and evaluation landed together. Light/dark
      seeded from `isSystemInDarkTheme()` with a manual toggle, `SyntaxColors` composition local,
      header, responsive two-column layout, debounced evaluation, sealed `EvalOutcome`, editors
      with a line gutter, result panel with pretty-printing and error cards.

      Also bundles JetBrains Mono: `FontFamily.Monospace` does not resolve under the web renderer
      and silently falls back to the proportional default, which misaligns the JSON and the gutter
      measured against it.
- [x] **4 — JSON syntax highlighting.** Partial-input-tolerant lexer feeding a
      `VisualTransformation` with `OffsetMapping.Identity`.
- [x] **5 — Curated examples.** Chip row of hand-written rule/data pairs spanning the operation
      families, plus a `commonTest` source set that pins what each one evaluates to. Tests run under
      `wasmJsBrowserTest`: Compose UI links skiko unconditionally, and skiko cannot load under Node.
- [x] **6 + 7 — Operations reference and shareable URL state.** Merged: both wire into the same
      handful of lines in `App`, and splitting them would have left one commit's feature
      unreachable. All 34 operators plus `var`, collapsible, click-to-insert at the cursor;
      base64url hash round-trip with a Share button and clipboard copy. Covers
      [AMP-3995](https://linear.app/branch/issue/AMP-3995/jsonlogic-playground-shareable-url-state).
- [ ] **8 — CI lanes.** Root-scope the library lanes, add a main-only `playground` lane, qualify
      `publish.yml`.
- [ ] **9 — Pages workflow.** `pages.yml` with SHA-pinned actions and least-privilege permissions.
- [ ] **10 — Docs.** README playground section and CI lane table; CHANGELOG entry.

## Manual step

GitHub Pages must be enabled by hand before `pages.yml` can succeed: **Settings → Pages → Build and
deployment → Source: "GitHub Actions"**. `configure-pages` can do this via its `enablement` input,
but that requires `administration:write`, which `GITHUB_TOKEN` does not carry.
