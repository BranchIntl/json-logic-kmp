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
- [x] **8 composition, mobile and accessibility input handling** — input attributes, aria, and the
  16 px floor that stops Safari zooming on focus. 64 tests, up from 61.
- [x] **9 restore the parity details the spike dropped** — pre-boot dark background, live OS-theme
  listener, and the Share button's two-state message.
- [x] **10 bundle the JetBrains Mono subset** — subset the repo's own face, ship it with its OFL, and
  apply the stack to everything monospaced rather than only the editors. 4,888 bytes over the wire;
  the module's live gzip goes 94,407 → 101,990 against the 110,000 ceiling.
- [x] **11 draw the theme and disclosure icons** — inline SVG in place of the text glyphs.
- [x] **12 verification gate** — run every column of the IME, mobile and wrap matrix that can be run
  without a person at a keyboard, and record below what was and was not exercised.
- [x] **13 the swap** — the DOM module becomes `playground/`, the Compose module and its catalog
  entries are deleted, Pages deploys the new output, and the README and CHANGELOG land in the same
  commit so no doc is false at any boundary.
- [x] **14 run the playground lane on pull requests** — the only lane that compiles the library's
  sources for a browser, so it is the guard against source-directory drift.
- [ ] **15 remove this doc**.

## Verification matrix

Run against the module's `build/dist/js` served over plain HTTP from the `/productionExecutable/`
subpath, which is the path shape the site is deployed under. Three engines were reachable:

- **Blink 148** — Chromium 148.0.7778.280, embedded in the Claude app's browser pane (Electron 42),
  macOS 15.7. The engine is Blink; the shell is not Google Chrome, and the shell is what limits the
  input that could be delivered (see *Instruments*).
- **Mobile WebKit** — iPhone 17 Pro simulator, iOS 26.5, `AppleWebKit/605.1.15 Version/26.5`,
  402×874 pt at 3×.
- **Desktop WebKit** — Safari 26.6 on this Mac, geometry only.

A column nobody could genuinely exercise is recorded as not tested. Nothing below is inferred from a
neighbouring observation.

| Column | Where | Result |
|---|---|---|
| Soft-wrap parity, unbroken token | Blink | 300 characters with no break opportunity wrap to six rows in both layers, breaking at offsets 0/52/104/156/208/260 in **both** — character-identical |
| Caret on its glyph | Blink | A two-character selection straddling the break at 260 bands the last glyph of one row and the first of the next; the last two characters band at the end of the final row. With the field's own text painted red over the highlighted copy, the two layers coincide on every row with no doubling |
| Trailing spaces at the wrap column | Blink | 45 characters plus spaces to columns 52, 54 and 70 stay on one row in both layers and never scroll sideways; the first visible character after them opens row 2 at offset 70 in both |
| Tab characters | Blink | Six literal tabs across six lines render at `tab-size: 2` in both layers, the overlay coincides, and the rule evaluates |
| Panel resize across a wrap boundary | Blink | 121 one-pixel steps from 480 px to 360 px: the two layers agree on the row count at every step, including the single transition at 388 px. Two whole-window resizes leave the two content boxes the same width |
| Soft-wrap parity | Mobile WebKit | Rule 6 rows, data 4 rows, textarea and highlighted copy equal in both; content boxes 326.40 against 326.41 px; every line number's top edge on its own line's top edge |
| Soft-wrap parity | Desktop WebKit | Rule 3 rows, data 2 rows, equal in both layers; content boxes equal |
| Undo across programmatic edits | Blink | One undo entry per editor the click actually changed; nothing lost. Detail below |
| Example chips | Blink | All nine: both editors replaced, both status dots ok, result and type name correct, the chip marked selected, wrap parity held in both editors at each step |
| Operation row | Blink | The rule becomes the snippet, the data is emptied, the result is the snippet's own |
| Share | Blink | The fragment is rewritten and `history.length` stays at 1; the Mac's own pasteboard holds the URL afterwards and the button says *Copied*; a rejected clipboard promise and a throwing `navigator.clipboard` both say *In the URL*; loading the copied URL in a fresh document restores both editors and the result |
| Theme toggle | Blink | Light to dark, moon to sun, palette and `color-scheme` both follow |
| Live appearance change | Mobile WebKit | Switching the simulator to dark mid-session moved the page from light to dark inside one page life — no reload |
| Pre-boot background | Blink | With `data-theme` absent and the system dark, the body already paints #131519 and `color-scheme` is dark, so there is no white frame before the bundle runs |
| Syntax error | Blink | The rule dot turns red and reads *syntax error* while the data dot stays ok, and the card fills with kind, detail and path. A well-formed rule naming an unknown operation leaves both dots ok and fills the card with the evaluation failure instead |
| Late font under throttling | Blink | Detail below |
| 16 px floor on a phone | Mobile WebKit | Both layers compute to 16 px with a 26 px line at 402 pt, so the condition Safari zooms for is not met — but see *Not tested* |
| Karma suite | Headless Chrome | 64 tests, no failures |

**Undo.** Each `setText` that changes a field is exactly one entry, so the press count is the number
of editors the click changed. Type into the rule and click a chip: the first undo restores the
*data* editor, the second restores the rule with its whole text selected, the third takes back the
typing, the fourth is refused. Click a chip whose data already matches what is there and only the
rule changes, so one press restores it. An operation row is two, the rule's insertion and the data's
deletion, the empty case included. That is the whole of the "second press" seen while building: the
first press lands in the editor you were not watching, which reads as the selection coming back. No
entry is lost on any path.

**Late font.** The face was held back for ten seconds and both layers were measured every 100 ms
across the swap. Under the shipped stack nothing reflows, because the fallback that macOS resolves
has the same advance width as the subset at 13 px. Repeating the run with a proportional fallback
substituted into the served copy — the only way to make the swap cost a reflow here — the rule went
from three rows to four when the subset landed, and across 273 samples the two layers never once
disagreed on the row count while every line number stayed on its line's top edge. The gutter moved
30.98 px to 30.80 px as `1ch` re-resolved against the new face.

**One thing to fix or accept.** The example chips are synced from the input path only, so on first
paint the chip matching the initial rule and data is not marked selected, and neither is the chip
matching a share link that happens to carry an example. Both light up after the first keystroke.

### Not tested

- **Tapping and typing on iOS.** Zoom on focus, a straight double quote from the software keyboard,
  a chip tap while the keyboard is up, and rotation all need input the simulator would not take
  here. Xcode is installed but `xcode-select` points at the Command Line Tools, so the native
  simulator integration refuses to attach; `simctl` can boot a device, open a URL and take
  screenshots but cannot tap, type or rotate; and AppleScript UI scripting is refused assistive
  access, so the Simulator's own window and menus are out of reach. Everything in the matrix marked
  *Mobile WebKit* was measured by a page that posts its own geometry back to the server, which is
  why the interaction rows are missing. `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`
  hands the simulator back to automation; a phone works just as well. Either way, someone has to tap
  into each editor and watch that the page does not zoom in, type `"` and confirm it stays straight,
  tap an example chip with the keyboard up and confirm the keyboard does not re-raise, and rotate
  the device and confirm the line numbers stay on their lines with no interaction.
- **Android GBoard.** `ANDROID_HOME` is unset on this machine and no emulator is configured. A
  person needs to glide-type inside a string value, accept an autocorrect suggestion and then
  backspace across it, and use the double-space period shortcut, watching for text the highlighted
  copy did not receive.
- **A real input method.** Japanese, Pinyin and Korean on a desktop keyboard. The harness injects
  key events below the macOS input-method layer, which the pre-flight probe already established, so
  no automated run can reach a composition session. A person needs to type, convert, walk the
  clauses, cancel, and commit in both Safari and Chrome, checking at each step that the highlighted
  copy matches the field and that the composing text is legible through the overlay.
- **VoiceOver.** The highlighted copy carries `aria-hidden` and is a sibling of the field rather
  than its parent, so pruning it should leave the field exposed. Whether the rule is announced once,
  and the fields announced as *Rule* and *Data*, wants a person listening.
- **A real ⌘Z and a real ⌘V.** Both are recorded above through the paths the harness could reach —
  `execCommand` for undo, and script-set selections plus typed text for select-all and paste. The
  keystrokes themselves were never delivered.

### Instruments

Worth knowing before trusting a row, and worth repeating if this is ever run again.

- Key events synthesised through the debugging protocol reach the page but not the editing layer:
  ⌘A, ⌘V and ⌘Z all arrive as keystrokes and do nothing. Text insertion does work, so selections
  were set from script and text was typed; undo went through `execCommand`, which pops the same
  stack.
- Finding a field's break offsets by measuring prefixes of its text is only sound where the text has
  no word-break opportunity. On word-wrapping text a truncated prefix ends in a shorter word and the
  method reports the break too late; the row-count comparison and the painted overlay carry those
  cases instead.
- The emulated appearance switch in the desktop pane changes what the media query matches without
  dispatching the change event — an independent listener recorded none — so the live appearance
  listener could only be proved on the simulator.
