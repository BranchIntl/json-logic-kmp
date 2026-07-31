# json-logic-kmp

A [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) implementation of
[JsonLogic](https://jsonlogic.com) — build rules as JSON, share them between front-end and back-end,
and evaluate them the same way on every platform Kotlin runs on.

This library is a fork of [jamsesso/json-logic-java](https://github.com/jamsesso/json-logic-java)
(MIT, © 2018 Sam Jesso), rewritten as a pure Kotlin Multiplatform module. It follows the Java
original closely — before the Java sources were removed from this repository, both engines were run
side by side over the same 335-case fixture corpus (289 value cases, 46 error cases) and
machine-verified to agree on every one — but where the two disagree, the
[JsonLogic reference implementation](https://github.com/jwadhams/json-logic-js) wins. See
[Known deviations & sharp edges](#known-deviations--sharp-edges) below for where the two parted ways,
and for what else to watch for before you rely on this library.

**Try it in your browser: [crafted.branch.co/json-logic-kmp](https://crafted.branch.co/json-logic-kmp/)**

## Supported targets

| Target                | Notes             |
|------------------------|-------------------|
| `jvm`                  |                   |
| `android`              | `minSdk 21`       |
| `iosArm64`             |                   |
| `iosSimulatorArm64`    |                   |
| `wasmJs`               | Node.js runtime   |

Values are modeled as `kotlinx.serialization.JsonElement` on every target. A rule crosses the API
boundary as either a `JsonElement` or a JSON string, but data is always a `JsonElement?` — parse a
serialized data string with `Json.parseToJsonElement` first (see [Usage](#usage) below).

## Playground

<https://crafted.branch.co/json-logic-kmp/> — two JSON editors and a live result panel, with
example presets and a reference for every operation. Links are shareable: **Share** puts the
current rule and data in the URL.

Nothing is evaluated on a server. The playground is a [Compose
Multiplatform](https://www.jetbrains.com/compose-multiplatform/) app in `playground/`, compiled to
WebAssembly against this library's own `wasmJs` target, so the engine answering in the browser is
the one that ships to every other platform. It is deployed from `main` by
`.github/workflows/pages.yml` and is not part of any published artifact.

To run it locally, with live reload:

```bash
./gradlew :playground:wasmJsBrowserDevelopmentRun
```

To build the deployable bundle, into `playground/build/dist/wasmJs/productionExecutable`:

```bash
./gradlew :playground:wasmJsBrowserDistribution
```

## Supported operations

`JsonLogic()` registers all 34 operations below by default, in the same order as the engine this
library ports. `var` is also fully supported as a first-class part of the rule syntax, rather than a
registered operation, so it isn't counted among the 34.

- **Numeric** (11): `+` `-` `*` `/` `%` `min` `max` `>` `>=` `<` `<=`
- **Logic & boolean** (10): `if` / `?:` `==` `!=` `===` `!==` `!` `!!` `and` `or`
- **Array** (8): `map` `filter` `reduce` `all` `some` `none` `merge` `in`
- **String** (2): `cat` `substr`
- **Data access** (2): `missing` `missing_some`
- **Miscellaneous** (1): `log`

Full semantics for each operation are documented at
[jsonlogic.com/operations.html](https://jsonlogic.com/operations.html).

## Installation

Releases are published to GitHub Packages (not Maven Central). GitHub Packages requires
authentication to resolve Maven artifacts even from a public repository, so add credentials
alongside the repository:

```kotlin
// settings.gradle.kts or build.gradle.kts
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/BranchIntl/json-logic-kmp")
        credentials {
            // A personal access token with the read:packages scope, e.g. from ~/.gradle/gradle.properties
            // or the GITHUB_ACTOR/GITHUB_TOKEN environment variables in CI.
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Then declare the dependency. In a Kotlin Multiplatform project, add it to `commonMain` so every
target picks it up:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("co.branch:json-logic-kmp:0.1.0")
        }
    }
}
```

Gradle's Kotlin Multiplatform metadata resolves the correct platform artifact for each source set
automatically; a single coordinate covers every target.

For a JVM- or Android-only consumer (not a multiplatform module), the plain top-level form also
works:

```kotlin
dependencies {
    implementation("co.branch:json-logic-kmp:0.1.0")
}
```

## Usage

### Evaluate a rule against data

```kotlin
import co.branch.jsonlogic.JsonLogic
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

val jsonLogic = JsonLogic()

val result = jsonLogic.apply(
    """{"var": "a"}""",
    buildJsonObject { put("a", 1) },
)
// result.jsonPrimitive.content == "1"
```

The `data` parameter is always a `JsonElement?` — there is no overload that takes a data *string*.
If your data arrives as serialized JSON rather than a `JsonElement`, parse it first:

```kotlin
import kotlinx.serialization.json.Json

val data = Json.parseToJsonElement("""{"a": 1}""")
jsonLogic.apply("""{"var": "a"}""", data)
```

### Parse once, apply many times

`apply(rule, ...)` parses the rule fresh on every call. There is no internal parse cache, so parse a
rule once with `parse()` and reuse the resulting node whenever you evaluate it repeatedly:

```kotlin
val rule = jsonLogic.parse("""{"===": [{"var": "a"}, 1.0]}""")

jsonLogic.apply(rule, buildJsonObject { put("a", 1) }) // true
jsonLogic.apply(rule, buildJsonObject { put("a", 2) }) // false
```

`parse` takes a rule in either of the forms `apply` accepts, so a rule that already is a
`JsonElement` — one that arrived inside a larger payload, say — pre-parses without being serialized
back to text first:

```kotlin
val payload = Json.parseToJsonElement("""{"rule": {"===": [{"var": "a"}, 1.0]}}""").jsonObject

val rule = jsonLogic.parse(payload.getValue("rule"))
```

The absence of a cache is deliberate. Only you know what identifies a rule — a screen id, a field
name, a hash — and a cache inside a `JsonLogic` instance would make it mutable, undoing the property
that lets one configured instance be shared across threads. A `Map` from your own rule key to the
parsed node, populated as rules arrive, is the whole of what a cache needs to be here.

### Custom operations

Register a custom operation from a plain function over its already-evaluated arguments — the
convenience form for an operation that doesn't need to control which arguments are evaluated or in
what data context:

```kotlin
jsonLogic.addOperation("greet") { args -> "Hello, ${args[0]}!" }

jsonLogic.apply("""{"greet": ["world"]}""") // "Hello, world!"
```

For full control, implement `JsonLogicExpression` (or its `PreEvaluatedArgumentsExpression`
convenience subtype) directly:

```kotlin
import co.branch.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression

jsonLogic.addOperation(object : PreEvaluatedArgumentsExpression {
    override val key: String = "double"
    override fun evaluate(arguments: List<Any?>, data: Any?, jsonPath: String): Any? =
        (arguments[0] as Double) * 2
})

jsonLogic.apply("""{"double": [21]}""") // 42.0
```

Registering under a key that's already registered — including one of the 34 defaults — replaces it:
whichever registration happens last wins. Both overloads rebuild the evaluator eagerly, so batch
custom-operation registration during setup rather than per request.

### `truthy`

`JsonLogic.truthy` mirrors JsonLogic's own truthiness rules (matching JavaScript's, not Kotlin's):

```kotlin
JsonLogic.truthy(0)               // false
JsonLogic.truthy(1)               // true
JsonLogic.truthy("")              // false
JsonLogic.truthy("Hello world!")  // true
```

`truthy` takes `Any?`, so it also accepts values the engine itself never produces, such as a Kotlin
or Java array — see [Known deviations & sharp edges](#known-deviations--sharp-edges) below for how
that case behaves.

## Known deviations & sharp edges

- **Unchecked exceptions.** `JsonLogicException` extends `RuntimeException`; upstream's version is a
  checked exception that every caller must declare or catch. No `try`/`catch` or `throws` clause is
  needed to call into this library.
- **No parse cache; `addOperation` rebuilds eagerly.** Parse a rule once via `parse()` and reuse it
  rather than re-parsing on every `apply(rule, ...)` call. Finish configuring an instance (default
  registrations plus any `addOperation` calls) on one thread before sharing it; once configuration is
  complete and the instance is safely published, concurrent `apply` calls are safe, but concurrent
  `addOperation` calls — with each other or with an in-flight `apply` — are not.
- **Strict JSON parsing.** Rule strings are parsed as strict JSON via `kotlinx.serialization`.
  Upstream's Gson-backed parser was lenient (unquoted keys and similar); those inputs no longer parse.
- **Numbers are always `Double`, and render the way JavaScript writes them.** Every numeric value is
  normalized to `Double` inside the engine, so integers beyond 2^53 lose precision. Results cross back
  into JSON through ECMAScript's `Number::toString`, byte-identical on every platform: a whole number
  carries no decimal point (`{"+": [1, 2]}` is `3`), the plain-decimal range is `[1e-6, 1e21)`, and
  everything outside it is exponential (`1e+21`, `1e-7`). `cat` and `substr` render a number the same
  way. Two places still render one with Java's `Double.toString`: `log`'s diagnostic text, and the
  substring test `in` performs against a string — so `{"in": [1, "a1b"]}` is `false` here, where the
  reference implementation looks for `1` rather than `1.0` and answers `true`.
- **Infinity and NaN aren't valid JSON.** A result of positive infinity, negative infinity, or NaN
  (e.g. from `{"/": [1, 0]}`) is returned as a `JsonElement` holding that literal, unquoted text, since
  JSON has no token for it. Reading it back out in Kotlin works fine, but re-encoding it through a
  standard JSON writer produces text most JSON parsers reject.
- **`cat` drops a null argument; `substr` renders one as the text `null`.** The reference joins `cat`'s
  arguments with `Array.prototype.join`, which renders null as the empty string, and stringifies
  `substr`'s source with `String()`, which renders it as `"null"` — so `{"substr": [null, 1]}` is
  `"ull"`. Both diverge from upstream, which throws `NullPointerException` in either case. `substr`
  clamps every offset into range and likewise never throws for one.
- **Preserved upstream quirks:** `all`'s error jsonPath always reports `[1]` for the failing element,
  regardless of its actual index; `substr` and `missing_some` type-check their numeric arguments as
  `Double` internally; a `var`'s default-value expression is evaluated twice when its key resolves to
  null; `missing`'s dotted-key flattening descends only into nested objects, never into arrays.
- **One accepted behavioral difference.** Comparing the raw result of `missing` with `===` or `in` is
  structural here (`{"===": [{"missing": ["a", "b"]}, ["a", "b"]]}` is `true`). Upstream wraps that
  result in an internal type whose `equals` always returns `false`, making the same comparison `false`
  there — and asymmetrically so, since the reverse operand order is `true`. Unreachable through the
  standard fixture corpus; accepted deliberately rather than reproduced.
- **Rule nesting is bounded; a hand-built node is not.** Parsing and evaluating both recurse, so every
  `parse` and `apply` overload that takes a rule as JSON rejects one nested deeper than
  `JsonLogicParser.DEFAULT_MAX_DEPTH` (128 objects and arrays — `{"+": [1, 2]}` is 2 levels, and each
  operator around it adds 2). `parse(rule, maxDepth)` moves the bound. A `JsonLogicNode` you assemble
  yourself and hand to `apply` has been through no such check, and neither has the *data* a rule runs
  against — converting that into the engine's value domain walks the whole tree recursively.
- **A value that contains itself is named, not entered.** `reduce` hands its reducer a single context
  map and mutates it in place, so a reducer returning its own data — or a list built around it —
  leaves a cycle in the value it returns. `cat`, `substr` and `log` render a container reached from
  inside itself as `(this Map)` or `(this Collection)`, at whatever depth it recurs. The names are
  `java.util`'s, but the reach is not: `java.util` compares an entry only against the container
  directly holding it, and overflows the stack on a cycle closing through two of them.
- **`truthy` on a Kotlin/Java array differs from upstream.** Values parsed from rules or `JsonElement`
  data are never arrays (only `List`, `Map`, `String`, `Number`, `Boolean`, and `null` ever reach an
  expression that way), so `truthy` has no case for one and it falls through to the default branch,
  returning `true` — even for an empty array — where upstream's Java duck-typing treated an empty
  array as falsy. Convert to a `List` first if you need array truthiness. A custom operation can still
  introduce an array: `addOperation`'s function type returns an unvalidated `Any?`, and whatever it
  returns flows straight into any surrounding expression. Return domain values from custom
  operations — `List` rather than an array, plus `Double`/`String`/`Boolean`/`null`/`Map` — or an
  array result will reach a nested expression unconverted and hit this same truthiness divergence.

## Contributing

### Prerequisites

- JDK 17 or later
- Android SDK, with `sdk.dir` set in `local.properties` (or `ANDROID_HOME` exported)
- Xcode with its command line tools, only needed to build or test the `iosArm64` /
  `iosSimulatorArm64` targets

### CI lanes

Every pull request runs three lanes, mirrored in `.github/workflows/build.yml`:

| Lane         | Runner       | Command(s) |
|--------------|--------------|------------|
| `jvm-android`| `ubuntu-latest` | `./gradlew :lib:jvmTest :lib:testAndroidHostTest :lib:assemble :lib:apiCheck` |
| `wasm`       | `ubuntu-latest` | `./gradlew :lib:wasmJsNodeTest` |
| `ios`        | `macos-15`      | `./gradlew :lib:iosSimulatorArm64Test :lib:klibApiCheck` |

A fourth lane, `playground`, runs `./gradlew :playground:wasmJsBrowserTest
:playground:wasmJsBrowserDistribution` on pushes to `main` only — it is a demo app rather than part
of the library's correctness contract, and its Wasm bundling costs minutes.

The public API is locked with [binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator)
across both the JVM and KLib (native/wasm) surfaces; a breaking change requires updating the committed
dumps under `lib/api/`. `:playground` is excluded via `apiValidation { ignoredProjects }` — it
publishes nothing and has no dump.

### Release process

1. On `main`, bump `version` in `lib/build.gradle.kts`, date the release's heading in
   `CHANGELOG.md`, and update the coordinates in [Installation](#installation).
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

## License

MIT — see [LICENSE](LICENSE). Original work © 2018 Sam Jesso
([jamsesso/json-logic-java](https://github.com/jamsesso/json-logic-java)); this repository is a
Kotlin Multiplatform port of that work.

The playground bundles [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), under the SIL
Open Font License 1.1. Its licence travels with it, in
`playground/src/wasmJsMain/resources/JetBrainsMono-OFL.txt` and on the deployed site.
