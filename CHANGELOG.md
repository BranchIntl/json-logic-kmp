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
  built around it — leaves a cycle in the value it returns, and `cat`, `substr` and `log` recursed
  into it until the stack ran out, which no caller can catch on Native or Wasm. A container reached
  from inside itself now renders as `(this Map)` or `(this Collection)` at whatever depth it recurs;
  the `java.util` rendering this port follows compares an entry only against the container directly
  holding it, so a cycle closing through two of them slipped past.

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
