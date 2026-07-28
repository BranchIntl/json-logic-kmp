# Playground implementation tasks

Tracking doc for [AMP-3994](https://linear.app/branch/issue/AMP-3994/jsonlogic-playground-cmpwasm-github-pages)
— a Compose Multiplatform / Wasm playground for this library, deployed to GitHub Pages.

This doc is temporary and is deleted before the PR is opened.

Legend: `[ ]` not started · `[~]` in progress · `[x]` complete.

## Tasks

- [~] **0 — Tracking doc + branch rename.** This file; branch renamed to Linear's suggested name.
- [ ] **1 — Gradle wiring + "hello" bundle.** `include(":playground")`, version catalog entries for
      Compose Multiplatform 1.11.1 / Material 3 1.9.0 / kotlinx-browser 0.5.0, BCV
      `ignoredProjects`, `playground/build.gradle.kts`, `index.html`, `Main.kt`, placeholder `App`.
- [ ] **2 — Theme + shell.** Light/dark schemes seeded from `isSystemInDarkTheme()` with a manual
      toggle, `JsonSyntaxColors` composition local, header, responsive two-column layout.
- [ ] **3 — Evaluation core + editors + result.** Debounced evaluation, sealed `EvalOutcome`,
      editors with a line gutter, result panel with pretty-printing and error cards.
- [ ] **4 — JSON syntax highlighting.** Partial-input-tolerant lexer feeding a
      `VisualTransformation` with `OffsetMapping.Identity`.
- [ ] **5 — Curated examples.** Chip row of hand-written rule/data pairs spanning the operation
      families.
- [ ] **6 — Operations reference.** All 34 operators, collapsible, click-to-insert at the cursor.
- [ ] **7 — Shareable URL state.** base64url hash round-trip, Share button, clipboard copy. Covers
      [AMP-3995](https://linear.app/branch/issue/AMP-3995/jsonlogic-playground-shareable-url-state).
- [ ] **8 — CI lanes.** Root-scope the library lanes, add a main-only `playground` lane, qualify
      `publish.yml`.
- [ ] **9 — Pages workflow.** `pages.yml` with SHA-pinned actions and least-privilege permissions.
- [ ] **10 — Docs.** README playground section and CI lane table; CHANGELOG entry.

## Manual step

GitHub Pages must be enabled by hand before `pages.yml` can succeed: **Settings → Pages → Build and
deployment → Source: "GitHub Actions"**. `configure-pages` can do this via its `enablement` input,
but that requires `administration:write`, which `GITHUB_TOKEN` does not carry.
