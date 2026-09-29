# Changelog

## 2.0.0

- Kotlin Multiplatform: Android, JVM desktop, iOS (arm64 device + simulator), macOS arm64,
  JavaScript and WebAssembly, published to Maven Central as `io.github.rajumark:comeback`.
- New constructor `Comeback()`: the model ships inside the library on every platform, so no
  `Context` is needed. `Comeback(context)` still compiles on Android (deprecated).
- Same model and same results as 1.x; parity with the Python reference (354 vectors) is tested on every target.
- The sample is now a Compose Multiplatform app (Android, desktop, iOS) plus a web page (JS and Wasm).

## 1.1.0

- License changed to the Hoverfly Community License: free for products with up to 10,000 monthly
  active devices per platform, commercial license above that. No code or model changes.
- 1.0.0 and earlier stay under Apache-2.0.

## 1.0.0

- First version: `Comeback(context).replies(message, limit, minConfidence)`.
- Model v3: 1303 replies in intent groups, English, int8 weights (4.7 MB).
- Pure Kotlin inference with no dependencies (the same network as Moji). minSdk 21.
