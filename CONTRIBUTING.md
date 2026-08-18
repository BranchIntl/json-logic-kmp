# Contributing

What follows is what building and testing the repository itself needs.
[README.md](README.md) is the library's own documentation, and cutting a release is
[PUBLISHING.md](PUBLISHING.md).

## Prerequisites

- JDK 17 or later
- Android SDK, with `sdk.dir` set in `local.properties` (or `ANDROID_HOME` exported)
- Xcode with its command line tools, only needed to build or test the `iosArm64` /
  `iosSimulatorArm64` targets
- Chrome, for the playground's browser tests. Karma finds a standard install itself; point
  `CHROME_BIN` at anything else
- fontTools with woff2 support (`pip install 'fonttools[woff]' brotli`), only needed to re-cut the
  playground's bundled font — see [The bundled font](#the-bundled-font)

## The bundled font

The playground ships a subset of JetBrains Mono. The full face lives in `playground/fonts/`, which
is outside the source tree because it is the input to the cut and is never served. Re-cut it with:

```bash
pyftsubset playground/fonts/JetBrainsMono-Regular.ttf \
  --output-file=playground/src/jsMain/resources/fonts/JetBrainsMono-Regular-subset.woff2 \
  --flavor=woff2 --unicodes="U+0020-007E,U+00A0,U+2013-2014,U+2018-2019,U+201C-201D,U+2026" \
  --layout-features='' --no-hinting --drop-tables+=DSIG --name-IDs='*'
```

That reproduces the committed file byte for byte. The `unicode-range` on the page's `@font-face`
names the same codepoints and has to move with it, since anything outside the range is drawn by the
system monospace instead. `--layout-features=''` is what leaves the ligature tables behind:
JetBrains Mono draws `>=` as a single `≥` and `!=` as `≠`, and those are operator names the reader
has to be able to type back after reading them.

A subsetter passes over a codepoint the source face does not have without saying so, so check the
cut rather than assuming it:

```bash
python3 -c "
from fontTools.ttLib import TTFont
wanted = set(range(0x20, 0x7F)) | {0xA0, 0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2026}
cut = set(TTFont('playground/src/jsMain/resources/fonts/JetBrainsMono-Regular-subset.woff2').getBestCmap())
print(len(wanted), 'requested;', len(wanted - cut), 'missing')
"
```

103 requested, 0 missing, 4,888 bytes on disk.

## CI lanes

Every pull request runs four lanes, mirrored in `.github/workflows/build.yml`:

| Lane         | Runner       | Command(s) |
|--------------|--------------|------------|
| `jvm-android`| `ubuntu-latest` | `./gradlew :lib:jvmTest :lib:testAndroidHostTest :lib:assemble :lib:apiCheck` |
| `wasm`       | `ubuntu-latest` | `./gradlew :lib:wasmJsNodeTest` |
| `ios`        | `macos-15`      | `./gradlew :lib:iosSimulatorArm64Test :lib:klibApiCheck` |
| `playground` | `ubuntu-latest` | `./gradlew :playground:jsBrowserTest :playground:jsBrowserDistribution` |

The `playground` lane earns its place by compiling the library rather than by exercising the demo:
the playground takes `lib/src/commonMain/kotlin` as a source directory of its own, so it is the
only lane that builds the library for a browser, and a change that does not compile on Kotlin/JS
would reach `main` unnoticed without it.

The public API is locked with [binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator)
across both the JVM and KLib (native/wasm) surfaces; a breaking change requires updating the committed
dumps under `lib/api/`. `:playground` is excluded via `apiValidation { ignoredProjects }` — it
publishes nothing and has no dump.
