# Kotlin Multiplatform migration — tracking doc

This repository is a fork of [`jamsesso/json-logic-java`](https://github.com/jamsesso/json-logic-java)
(MIT, © 2018 Sam Jesso) being migrated **in place** to a Kotlin Multiplatform library:
package `co.branch.jsonlogic`, coordinates `co.branch:json-logic-kmp`, targets `jvm`, `android`,
`iosArm64`, `iosSimulatorArm64`, `wasmJs` (Node), value model `kotlinx.serialization.JsonElement`.
Goal: **bug-for-bug parity** with the Java original at upstream `main@49995a7` (the oracle), proven
mechanically by a temporary `:parity` subproject that runs both engines over the 289 value fixtures and
46 error fixtures on the JVM and diffs the results. The Java sources stay in the tree (as `:parity`)
until the diff reports zero disagreements, then they are deleted.

This file is the single tracking document for the migration and is deleted in the final workstream.
Checkbox states: `[ ]` not started · `[~]` in progress · `[x]` merged to `main`.

## Execution model

- An orchestrating session (Claude Fable 5) plans, validates, and merges; implementation is done by
  dispatched subagents (Claude Opus 5 / Sonnet 5 per workstream). Every PR is adversarially reviewed
  with Codex (max 2 rounds; behavioral findings require a `rule + data + expected` repro to count)
  before it is opened.
- Work lands on `main` via one PR per workstream, developed in an isolated git worktree branched from
  current `origin/main` (`kmp/<workstream>` branches). PRs are opened only after local validation
  lanes pass and the Codex review converges; they merge only after CI is green. **Squash-merge only.**
- Merge queue of one: before opening the PR, the branch is rebased onto current `origin/main` and the
  lanes re-run on the rebased head; that SHA is recorded in the PR body.

### Standing rules for workstream branches

- Branch only from current `origin/main`, never from a sibling workstream branch.
- `parity/` is **read-only** after WS-A (it is the oracle); enforced per PR via `git diff --stat`.
- Only WS-A, WS-B (fixture codegen task) and WS-I2 (binary-compatibility-validator) may modify build
  files (`settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, wrapper).
- `KMP-MIGRATION.md` is updated only by the orchestrator, directly on `main`, immediately after a
  merge — never on workstream branches.
- No edits under `.github/workflows/` except where a workstream explicitly owns them (WS-A, WS-I2,
  WS-J).

### PR body template

1. **What was done** — workstream reference + summary of the change.
2. **How it was validated** — exact Gradle commands per lane with per-target test counts; toolchain
   versions; the rebased HEAD SHA the lanes ran on; CI lane results; Codex review rounds with a
   findings-disposition table (confirmed/fixed/rejected-with-reason); fixture-subset pass counts for
   expression batches.

## Kickoff

- [x] **K1** — grant the `workflow` scope to the active `gh` token (`gh auth refresh -h github.com -s workflow`); required to push workflow-file changes (WS-A's CI replacement).
- [x] **K2** — commit this tracking doc; delete upstream `publish.yml` (its push-to-`main` trigger
  would fire on every squash-merge and attempt a Sonatype upload + version-bump push-back; the
  `workflow_dispatch` GitHub Packages replacement arrives in WS-J).
- [x] **K3** — after WS-A merges: one serial full 5-target build to prime the Kotlin/Native and wasm
  toolchain caches before parallel worktrees spawn. (Satisfied by the full `./gradlew build` run on
  WS-A's rebased head during validation — konan, Node and Android SDK caches are warm and shared.)

## Workstreams

Dependency order: A → {B, C, D} → E → {F1, F2, F3} → G → I1 → H → I2 → J → K. One PR each.

- [x] **WS-A build-modernization** (Opus 5) — Gradle wrapper 6.5.1→9.3.1; `settings.gradle.kts`; root
  project becomes the KMP module (all 5 targets, complete build) + `:parity` subproject (Java + Gson +
  JUnit moved **verbatim**, all 110 tests green); complete `gradle/libs.versions.toml` pre-declaring
  every downstream dependency; `.gitignore`; drop `.tool-versions`; strip the dead `maven`/
  `uploadArchives` publishing; replace `build.yml` with three CI lanes (jvm+android+parity on ubuntu
  JDK 21, wasm on ubuntu, iOS simulator on macOS) triggered on PRs to `main` and pushes to `main`.
  Note: binary-compatibility-validator is deliberately **not** applied until WS-I2, so `api/` dumps
  never become a cross-branch conflict while the API surface grows.
- [~] **WS-B fixtures** (Sonnet 5) — move fixtures to `fixtures/`; Gradle codegen task base64-embeds
  them into generated `commonTest` sources (chunked literals — JVM 65 535-byte constant cap); fixture
  model that skips the 17 string section-header entries; semantic comparator (numbers by double value,
  deep for arrays/objects); engine-agnostic replay harness (engine passed as a function) with an
  operator filter for partial fixture subsets; `:parity` test resources re-pointed at `fixtures/`.
  Depends: A.
- [~] **WS-C canonical helpers** (Opus 5) — `internal/CanonicalNumber.kt` (ECMAScript-style
  `Double`→`String`, Java-compatible strict `String`→`Double`) and `internal/JavaSplit.kt`, with
  cross-target determinism tests (jvm, wasmJsNode, iosSimulatorArm64). Depends: A.
- [~] **WS-D AST + parser** (Sonnet 5) — sealed `ast/JsonLogicNode.kt` hierarchy; `JsonLogicParser.kt`
  (`JsonElement` → node tree); parse exception. Depends: A.
- [ ] **WS-E evaluator core** (Opus 5) — `JsonLogicEvaluator`; `JsonLogicExpression` +
  `PreEvaluatedArgumentsExpression` interfaces; evaluation exception; shared `truthy`; shared
  error-message/jsonPath formatting helpers; numeric result construction via `CanonicalNumber`;
  `var` resolution incl. the `MISSING` sentinel; `MissingExpression` (`missing`/`missing_some`).
  Depends: C, D.
- [ ] **WS-F1 numeric + equality** (Opus 5) — `Math` (`+ - * / % min max`), `NumericComparison`
  (`> >= < <=`), `Equality`/`Inequality` (loose-coercion matrix), `StrictEquality`/`StrictInequality`.
  Depends: E.
- [ ] **WS-F2 control + string ops** (Sonnet 5) — `If` (`if`/`?:`), `Logic` (`and`/`or`), `Not`
  (`!`/`!!`), `Log`, `Concatenate` (`cat`), `Substring` (`substr`). Depends: E.
- [ ] **WS-F3 array ops** (Opus 5) — `Map`, `Filter`, `Reduce`, `All`, `ArrayHas` (`some`/`none`),
  `Merge`, `In`. Depends: E.
- [ ] **WS-G entry point** (Sonnet 5) — `JsonLogic` public API; default-operator registration table;
  custom-operation registration; enable the full fixture replay across all targets. Depends: F1–F3.
- [ ] **WS-I1 parity gate** (Opus 5) — JVM diff runner in `:parity` (Java engine vs KMP engine over
  all 289 + 46 cases); fix every divergence; acceptance = zero disagreements. Depends: G.
- [ ] **WS-H test port** (Sonnet 5) — port the remaining hand-written JUnit test classes to
  `commonTest`; test-only, no `commonMain` edits. Depends: I1.
- [ ] **WS-I2 teardown + API surface** (Sonnet 5) — delete `:parity` + the Java tree; apply
  binary-compatibility-validator; `apiDump`; commit `api/`; add `apiCheck` to CI. Depends: H.
- [ ] **WS-J publishing** (Sonnet 5) — `maven-publish` to GitHub Packages under `BranchIntl`; new
  `publish.yml` on `workflow_dispatch`. Depends: I2.
- [ ] **WS-K docs** (Sonnet 5) — README rewrite (KMP usage, fork/MIT attribution, known quirks incl.
  Infinity/NaN and 2^53 precision notes, build prerequisites); CHANGELOG; delete this file. Depends: I2.

## Done criteria

- All target lanes green locally **and** in CI; the 289 value + 46 error fixtures pass on every
  target, not just the JVM.
- The `:parity` diff reported zero disagreements against the Java engine (oracle `49995a7`) before its
  deletion.
- `apiCheck` green against committed dumps; `:parity` and the Java sources gone from the tree.
- Every merged PR carries its validation evidence and Codex disposition table.
