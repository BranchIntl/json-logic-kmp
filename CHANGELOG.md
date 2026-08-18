# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

Where the [JsonLogic reference implementation](https://github.com/jwadhams/json-logic-js) and the
engine this library ports disagree, the reference now wins.

### Added

- `JsonLogic.parse` accepts a rule as a `JsonElement`, not only as a string, so a rule that already
  is a `JsonElement` pre-parses without being serialized back to text. There is still no internal
  parse cache, and the README now says why.
- A nesting bound on every parse entry point: a rule nested deeper than
  `JsonLogicParser.DEFAULT_MAX_DEPTH` (128 containers) is rejected with `JsonLogicParseException`,
  and `parse(rule, maxDepth)` overloads move that bound. Parsing and evaluating both recurse, so a
  deeply nested rule from an untrusted source could previously exhaust the stack. On the string path
  the bound is applied to the text before the JSON parser sees it, since building the tree overflows
  first — measured between 2,000 and 5,000 levels on a 50 KB rule.

### Changed

- The playground is a plain Kotlin/JS app drawing to the DOM, where it was a Compose Multiplatform
  app rendering through skiko on `wasmJs`. It carries the same two editors, live result panel,
  example presets, operation reference and shareable links, and it still answers with this
  library's own engine: it compiles `lib/src/commonMain/kotlin` as a source directory of its own,
  so no `js` publication stands behind it. Opening it fetches three files — the page, the bundle
  and the font — coming to **about 100,300 bytes over the wire against the Compose build's
  4.54 MB**: the first two gzipped as a server would send them and the woff2 at its own size,
  being compressed already, where the Compose figure is what the deployed site sends for its five
  files. Neither total is exact to the byte, the minifier ordering its identifier names differently
  on every build and a CDN compressing to its own settings. The licence notice and the source map
  are deployed beside the three and requested by nobody. It renders on a device with no WebGL
  rather than showing a blank page, and its monospace face is a 4,888-byte subset of JetBrains Mono
  where the Compose build bundled the whole 270,224-byte file. Undo reaches the edits the examples
  and operation rows make: the rule and the data are separate text controls, and Blink gives each
  its own undo entry, so a chip press or an operation row takes two presses of Cmd-Z to walk back
  to what the reader had, the state in between pairing the new rule with the old data. WebKit
  coalesces the two entries into one, and a single press returns both fields to the text they were
  loaded with — the reader's own typing along with the replacement — after which further presses do
  nothing. `./gradlew :playground:jsBrowserDistribution` builds it, into
  `playground/build/dist/js/productionExecutable`.
- Numeric results render the way ECMAScript's `Number::toString` does, which is what the reference
  implementation serializes: `{"+": [1, 2]}` now returns the JSON token `3` rather than `3.0`, and
  the plain-decimal range widens from `[1e-3, 1e7)` to `[1e-6, 1e21)`. Numbers are still normalized
  to `Double` inside the engine, and string-to-number coercion is unchanged.
- `cat` renders a null argument as the empty string rather than throwing `NullPointerException`,
  matching the reference's use of `Array.prototype.join` — so a rule interpolating a variable still
  renders before that variable is set. The community conformance suite asserts this, and the engine
  this library ports has [an open bug](https://github.com/jamsesso/json-logic-java/issues/36) for it.
- `substr` takes `String.prototype.substr` semantics: every offset is clamped into range, so it no
  longer throws on a null source or an out-of-range start/length, and a start before the beginning
  now yields the whole string where it previously yielded the empty one. Its argument-count and
  numeric-type checks are unchanged.
- `cat` and `substr` render a number the same way results do, so `cat` no longer switches to
  scientific notation at `1e7`.
- The substring test `in` performs against a string renders its first argument the same way too, so
  `{"in": [1, "a1b"]}` is `true` where it was `false` — the reference looks for `1`, not `1.0`. This was
  the last value a rule can observe still rendered with Java's `Double.toString`; only `log`'s
  diagnostic text is.
- `in` also renders a null first argument rather than answering `false` on sight of one, so
  `{"in": [null, "a null value"]}` is `true`, matching the reference's coercion of the needle through
  `String()` and `substr`'s own rendering of a null source.

### Fixed

- Rendering a value that contains itself no longer exhausts the stack. `reduce` hands its reducer a
  single context map and mutates it in place, so a rule whose reducer returns its own data — or a list
  built around it — leaves a cycle in the value it returns, and `cat`, `substr`, `in` and `log`
  recursed into it until the stack ran out, which no caller can catch on Native or Wasm. A container
  reached from inside itself now renders as `(this Map)` or `(this Collection)` at whatever depth it
  recurs; the `java.util` rendering this port follows compares an entry only against the container
  directly holding it, so a cycle closing through two of them slipped past.
- Numeric rule literals convert through the library's own `Double.parseDouble`-faithful parser
  instead of the platform's `String.toDouble`, which is documented as platform-dependent and, on
  `wasmJs`, is not correctly rounded: 301 of 20,000 randomly generated JSON numerals came back off
  by one unit in the last place, `1.797693134862315808e308` yielded the largest finite double where
  it should overflow to `Infinity`, and `4.4e-323` could not read back a number this library had
  itself rendered. On Kotlin/Native the same conversion discards digits past roughly 312 characters.
  Both Java and ECMAScript require the correctly rounded result, so this converged the four
  targets that can be tested rather than trading one dialect for another. Every literal in the
  fixture corpus is short enough to convert identically everywhere, which is why the corpus never
  caught it.
- A rule literal that is not a number raises `JsonLogicParseException`, carrying the path of the
  node that failed, rather than letting the platform's `NumberFormatException` escape. `JsonLogic`
  documents every parse failure as a `JsonLogicParseException`, so `{"+": [abc, 1]}` — which the
  JSON reader hands over as a bare token — previously threw past a caller catching
  `JsonLogicException`.

## [0.1.0] - 2026-07-28

### Added

- Complete Kotlin Multiplatform port of [jamsesso/json-logic-java](https://github.com/jamsesso/json-logic-java)
  at upstream `49995a7`, targeting `jvm`, `android` (`minSdk 21`), `iosArm64`, `iosSimulatorArm64`,
  and `wasmJs` (Node.js), with values modeled as `kotlinx.serialization.JsonElement`.
- All 34 default operations plus `var`, machine-checked bug-for-bug against the upstream Java engine
  over the full 335-case fixture corpus (289 value cases, 46 error cases) before the Java sources were
  removed from the repository.
- Published to GitHub Packages as `co.branch:json-logic-kmp`, via a manually dispatched Publish
  workflow.
- An interactive playground at <https://crafted.branch.co/json-logic-kmp/> — a Compose
  Multiplatform app in `playground/`, compiled to WebAssembly against this library's `wasmJs`
  target, with two JSON editors, a live result panel, example presets, a reference for every
  operation, and shareable links that carry the rule and data in the URL. Deployed from `main` by
  `.github/workflows/pages.yml`; excluded from the published artifacts and from binary-compatibility
  validation.

### Changed

Deliberate design changes from the engine this library ports — see
[README.md](README.md#known-deviations--sharp-edges) for the full list:

- `JsonLogicException` is now an unchecked `RuntimeException`; upstream's was a checked exception.
- No internal parse cache: rules are parsed explicitly via `parse()` and the result reused, rather
  than cached implicitly behind `apply(String, ...)`. `addOperation` rebuilds the evaluator eagerly
  on every call.
- Rule strings parse as strict JSON (`kotlinx.serialization`); upstream's Gson-backed parser was
  lenient (unquoted keys and similar).
- Distributed via GitHub Packages under `co.branch:json-logic-kmp` rather than upstream's Maven
  Central coordinates.
