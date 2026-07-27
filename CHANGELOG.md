# Changelog

All notable changes to this project are documented in this file.

## [0.1.0] - Unreleased

### Added

- Complete Kotlin Multiplatform port of [jamsesso/json-logic-java](https://github.com/jamsesso/json-logic-java)
  at upstream `49995a7`, targeting `jvm`, `android` (`minSdk 21`), `iosArm64`, `iosSimulatorArm64`,
  and `wasmJs` (Node.js), with values modeled as `kotlinx.serialization.JsonElement`.
- All 34 default operations plus `var`, machine-checked bug-for-bug against the upstream Java engine
  over the full 335-case fixture corpus (289 value cases, 46 error cases) before the Java sources were
  removed from the repository.
- Published to GitHub Packages as `co.branch:json-logic-kmp`, via a manually dispatched Publish
  workflow.

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
