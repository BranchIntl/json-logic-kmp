# Reference conformance — tracking doc

This library was ported from [`jamsesso/json-logic-java`](https://github.com/jamsesso/json-logic-java)
for bug-for-bug parity, machine-verified over a 335-case corpus. Integrating it into a consumer
surfaced four places where that parity carries a defect forward or leaves a gap. This round resolves
them under one governing rule:

> **Where the JsonLogic reference implementation
> ([`jwadhams/json-logic-js`](https://github.com/jwadhams/json-logic-js)) and the
> `jamsesso/json-logic-java` original disagree, the reference wins.**

The library's self-description moves with it: from "bug-for-bug compatible with json-logic-java" to
JsonLogic-conformant, ported from json-logic-java, with documented divergences where the Java port is
the ecosystem outlier.

Checkbox states: `[ ]` not started · `[~]` in progress · `[x]` done.

## Evidence

Established by running the reference implementation under Node and replaying this repository's own
fixture corpus through it.

- **`cat` on a null operand.** The reference is `Array.prototype.join.call(arguments, "")`, and
  `join` renders null as `""`. The community
  [`json-logic/compat-tables`](https://github.com/json-logic/compat-tables) suite codifies
  `{"cat": [null, "test", null]}` → `"test"`; json-logic-js and json-logic-engine score 9/9 on that
  suite and jamsesso scores 8/9, failing exactly that case.
  [jamsesso#36](https://github.com/jamsesso/json-logic-java/issues/36) has been open on it since
  2023. Across 40+ probes the reference never throws on a null operand for any operator:
  never-throw-on-null is the ecosystem invariant, and the `NullPointerException` inherited here is
  the outlier.
- **`substr` on a null operand.** The reference is `String(source).substr(start, end)`, so null
  renders as the text `"null"` and `{"substr": [null, 1]}` is `"ull"`. The reference is deliberately
  inconsistent with its own `cat` here; this library matches it rather than inventing a third answer.
  `substr` diverges from the reference in five ways, not one: the NPE, an `IndexOutOfBoundsException`
  on two out-of-range forms, and two clamping cases that return `""` where the reference returns
  content.
- **Number form.** No JsonLogic prose specifies integer versus float form, and the distinction is
  explicitly outside the conformance contract — the community JVM harness compares numerically within
  `1e-10`. So this is about byte-parity with the reference and consumer ergonomics, not conformance.
  `jsonlogic.com/tests.json` expects an integer literal in 47 cases and a float in 2.
- **Recursion depth.** Unspecified, and effectively unimplemented across the ecosystem: the reference
  itself dies with `RangeError` at roughly 2497 nesting levels on a 25 KB rule, and
  json-logic-engine's `maxDepth` bounds output *data* depth rather than rule depth. No DoS advisories
  exist for any implementation. The bound added here is this library's own invention.
- **The corpus does not block any of this.** Replaying all 289 value fixtures through the reference:
  288 agree byte-for-byte under strict JSON comparison. The single disagreement is
  `{"+": {"merge": [1, [2]]}}` — fixture `3`, reference `1` — a pre-existing `+` divergence, out of
  scope. No fixture covers a null operand, an out-of-range `substr`, or a number magnitude where the
  two layouts differ, so every change below is pinned only by hand-written tests.

## Tasks

- [x] **T0 — This tracking doc.**
- [x] **T1 — ECMAScript number renderer.** `internal fun ecmaDoubleToString` alongside
      `canonicalDoubleToString` in `internal/CanonicalNumber.kt`. Both Java (JDK 19+) and ECMAScript
      define the same shortest-round-tripping digits, so the digit search is shared; the ES path skips
      the Java-only two-digit tie-break (`Double.MIN_VALUE` is `5e-324`, not `4.9E-324`) and lays the
      digits out ES-style: plain decimal for `10^-6 ≤ |x| < 10^21` with no forced `.0`, `d.ddde±nn`
      outside it, `-0.0` as `0`. Infinity and NaN keep rendering as themselves. Verified by a
      boundary table in `commonTest` and a `wasmJsTest` oracle against V8's own `String(x)`.
- [x] **T2 — Integer-form results at the JSON boundary.** `valueToJsonElement` renders through the ES
      renderer, so `{"+": [1, 2]}` is `3` rather than `3.0`. Input coercion and the internal `Double`
      value domain are untouched.
- [x] **T3 — `cat` and `substr` reference conformance.** `cat` renders null as `""`; `substr` takes
      `String.prototype.substr` semantics plus the reference's negative-length emulation and stops
      throwing on operand shape. The arity and numeric-type `JsonLogicEvaluationException`s stay: the
      reference has no equivalent, three error fixtures lock them, and they beat silent coercion.
- [x] **T4 — Depth-bounded parse.** A default bound on JSON nesting levels, enforced while descending,
      plus explicit `maxDepth` overloads. The string path additionally gets an iterative bracket
      pre-scan ahead of `Json.parseToJsonElement`, whose stackless path covers deep *objects* but not
      the object/array alternation every JsonLogic rule is built from.
- [ ] **T5 — `JsonLogic.parse(JsonElement)`.** Makes `parse` symmetric with `apply` for a consumer
      whose rules already are `JsonElement`. No internal parse cache: rule identity lives with the
      caller, and a cache would put mutable state back into an instance documented as safe to share
      once configured.
- [ ] **T6 — Docs and API dumps.** `CHANGELOG.md` `[Unreleased]`, the affected README sharp-edges
      bullets, the playground's copy about null operands and number form, and refreshed
      `binary-compatibility-validator` dumps.

## Out of scope

Reference divergences found while reviewing that no filed feedback covers. Each is its own decision.

- **`in`'s number rendering.** `{"in": [1, "a1b"]}` is `false` here and `true` in the reference,
  because `InExpression` renders `1.0` as `"1.0"` for the substring test.
- **Rule-side numeric literals.** `JsonLogicParser` parses them with Kotlin's `content.toDouble()`,
  documented as platform-dependent, while the data side uses `parseJavaDouble`; a
  `NumberFormatException` from it also escapes `parse(String)` past its `catch (SerializationException)`.
- **Collection rendering in string contexts.** `cat`, `substr`, and `in` render a `List` as `[a, b]`
  and a `Map` as `{k=v}`, where the reference gives `1,2` and `[object Object]`.
- **`log`'s printed text**, the one remaining place a number renders in Java form.
