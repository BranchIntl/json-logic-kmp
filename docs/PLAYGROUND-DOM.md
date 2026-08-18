# Playground on the DOM — tracking doc

The playground deployed at https://crafted.branch.co/json-logic-kmp/ is Compose Multiplatform on
wasmJs rendering through skiko, and that renderer is what the visitor pays for: **4,543,654 bytes of
live gzip, 74% of it skiko**, 4.3 s to first paint on fast 4G, a permanent ~115 ms penalty on every
load even when the whole bundle is cached, and a silent blank page on any device without WebGL. The
app itself is small; the canvas under it is not.

A spike proved the alternative. Rebuilt as plain Kotlin/JS against the DOM, the same playground
reaches feature parity at **92,589 bytes gzip — 46× smaller** — missing only soft wrap, the bundled
font and the hand-drawn icons. This round takes that spike to production: it earns those three back,
hardens the hand-rolled editor, runs the engine's numeric tests on Kotlin/JS for the first time, and
then replaces the deployed playground with it. The library is not touched — the playground compiles
`lib/src/commonMain/kotlin` as its own source rather than adding a `js` target to a published
coordinate, so `git diff main -- lib/` stays empty for the whole round.

This file tracks that work, and is deleted by the last task once the work is done — the same
lifecycle the Compose playground's own tracking doc had in
[PR #17](https://github.com/BranchIntl/json-logic-kmp/pull/17). Checkbox states: `[ ]` not started ·
`[~]` in progress · `[x]` committed.

## Pre-flight probe

The transparent-overlay editor lays a `color: transparent` textarea over a highlighted `<pre>`, and
an IME paints its pre-edit text through the textarea it is composing into. If that text were
invisible through the overlay, or if the render path wrote `value` mid-composition and aborted the
session, the technique would be dead and the round would take the CodeMirror bail-out instead.

**Result: the render pipeline handles composition-time input correctly.** Across a seven-step
synthetic Japanese sequence the highlight layer matched `textarea.value` at every step, and a setter
spy recorded **zero** writes to `value` from the render path. What a synthetic sequence cannot settle
is what a real IME does at a real keyboard, or whether the IME's own composition underline is legible
through the overlay; both need a human and are scheduled for the commit-12 matrix.

## Tasks

- [x] **0 tracking doc** — this file, including the probe result above.
- [x] **1 add the DOM module** — the spike's sources with `playground-shared` folded into its own src
  tree, the library consumed as a `srcDir` rather than a project dependency, `include` +
  `ignoredProjects`, regenerated yarn lock, and every declaration a test will reach widened from
  file-`private` to `internal`.
- [x] **2 carry the playground's tests onto the DOM build** — the four existing tests copied into
  `jsTest` (the originals stay until the swap, or the Compose module's test task finds none and
  fails). First time they run on Kotlin/JS: 18 tests, all green.
- [x] **3 run the engine's numeric tests on Kotlin/JS** — mount the fixture-free number-literal and
  stringify tests from the library's `commonTest` as a `jsTest` source directory. `CanonicalNumber`
  and `BigUInt` are `Long`-heavy bit code that had never executed on Kotlin/JS `Long` emulation.
  All 30 pass: 48 tests in the module, up from 18, with no divergence to report.
- [x] **4 absorb the operation-pick behaviour** — picking an operation replaces the rule and clears
  the data; the at-cursor insert path goes away with it.
- [x] **5 undo across programmatic edits** — chips and operation rows write through an edit command
  so the browser's own undo stack survives them.
- [x] **6 extract the highlight span model** — a pure `text → spans → lines` mapping, property-tested,
  with the DOM rendering still flat.
- [x] **7 soft wrap** — per-line blocks with in-line numbers and break-parity CSS between the two
  layers; the sticky gutter and its horizontal-scroll machinery go away.
- [ ] **8 composition, mobile and accessibility input handling** — input attributes, aria, and the
  16 px floor that stops Safari zooming on focus.
- [ ] **9 restore the parity details the spike dropped** — pre-boot dark background, live OS-theme
  listener, and the Share button's two-state message.
- [ ] **10 bundle the JetBrains Mono subset** — subset the repo's own face, ship it with its OFL, and
  apply the stack to everything monospaced rather than only the editors.
- [ ] **11 draw the theme and disclosure icons** — inline SVG in place of the text glyphs.
- [ ] **12 verification gate** — run the full IME, mobile and wrap matrix and record the results here.
- [ ] **13 the swap** — the DOM module becomes `playground/`, the Compose module and its catalog
  entries are deleted, Pages deploys the new output, and the README and CHANGELOG land in the same
  commit so no doc is false at any boundary.
- [ ] **14 run the playground lane on pull requests** — the only lane that compiles the library's
  sources for a browser, so it is the guard against source-directory drift.
- [ ] **15 remove this doc**.
