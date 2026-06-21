# CLAUDE.md

Stable working rules for this repository. Keep this file short and rule-focused — it is
not a changelog. Per-change history lives in git commits.

## What this is

An **Android app** (Kotlin + Jetpack Compose + Room) for reading, searching, and
navigating the Constitution of Ukraine. Single Gradle module `:app`; namespace
`ua.constitution`; `minSdk 24`. The UI and content are **Ukrainian-first**
(`metadata.json` is the Ukrainian store listing — do not "translate" it to English).
It is not a web app.

## Build / test / lint

Requires **JDK 21** (or 17). The machine default is JDK 25, which Gradle 9.x **cannot**
launch — builds will fail with a cryptic toolchain error if you use it.

```bash
# Unit tests (the primary gate)
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem ./gradlew :app:testDebugUnitTest

# Static analysis (complexity gate)
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem ./gradlew :app:detekt

# Compile / package check
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem ./gradlew :app:assembleDebug
```

`downloadAnthem` is an **opt-in** task that re-fetches `anthem.ogg` from Wikimedia; it is
no longer wired into `preBuild`, so normal builds never hit the network. The `.ogg` is
committed. Appending `-x downloadAnthem` is harmless and safe in any offline/CI context.

## Architecture (layer map)

Source root: `app/src/main/java/ua/constitution/`. Dependency direction is one-way:
**`ui` → `domain` ← `data`**.

- **`domain/`** — pure business logic, **no Android/Compose imports**. Subpackages:
  `text` (superscript/selection/formatting/styling), `content` (search, chapter ranges,
  segment merging, the read interfaces), `link` (article + URL resolution), `title`,
  `bookmark` (edit parsing/reconciliation).
- **`data/`** — `model` (immutable `ConstitutionContent` + data classes), `database`
  (Room entities/DAO/provider), `repository` (`BookmarkRepository` + impl), `source`
  (JSON load, integrity check, deserializer).
- **`ui/`** — Compose only. `screens`, `article`, `model`, `theme`, `viewmodel`, plus
  loose top-level helper files directly in `package ua.constitution.ui`
  (`ColorUtils`, `UrlOpener`, `ClipboardCopy`, `TimeFormatter`, `BookmarkLookup`,
  `EraserIcon`, `ImmersiveFullscreen`, `SelectionToolbarPosition`). Those flat helpers
  are intentional — don't "tidy" them into subpackages without a reason.

## Package conventions

Packages mirror directories: `ua.constitution.<layer>.<feature>`
(e.g. `ua.constitution.domain.text`, `ua.constitution.ui.screens`). `MainActivity.kt`
sits at the root and is `package ua.constitution`. **Tests are deliberately flat**: every
file in `app/src/test/java/ua/constitution/` is `package ua.constitution` regardless of
subject — do not mirror the production subpackages into the test tree.

## Where do I add X?

- Text formatting / selection math → `domain/text/`
- Article lookup / search / chapter ranges → `domain/content/`
- Link / URL resolution → `domain/link/`
- Bookmark edit parsing → `domain/bookmark/`
- JSON parsing / integrity → `data/source/`; Room entities/DAO → `data/database/`
- Screens & composables → `ui/screens/` and `ui/article/`
- App state seam → `ui/viewmodel/ConstitutionViewModel.kt`
- Colors → `ui/theme/Color.kt` (brand) and `ui/theme/HighlightPalette.kt` (hex strings)
- User-facing strings → `res/values*/strings.xml` (Ukrainian-first)

## JSON parsing reality

The Constitution JSON is loaded from `assets/constitution_ua.json` and parsed with
**`org.json` (`JSONObject`/`JSONArray`)** — the Android-bundled library. There is **no**
Retrofit/Moshi/OkHttp networking layer (those dependencies were removed as unused). Do
not assume Moshi adapters or `@JsonClass`; do not introduce a networking stack without an
explicit request.

## Dependency injection

Manual DI / composition root is `MainActivity.kt` (~46 lines). **No Hilt/Dagger.**
`data/model/ConstitutionContent` implements four segregated read interfaces
(`ConstitutionContentSource`, `ChapterSource`, `ArticleLookup`, `IntegrityStatus`) and is
passed to `ConstitutionViewModelFactory` once per interface — that is deliberate ISP, not
a copy-paste mistake.

## Characterization tests (READ THIS BEFORE "FIXING" A FAILING TEST)

`app/src/test/java/ua/constitution/` (~233 `@Test`, JUnit4 + Robolectric + pure JVM)
**intentionally pins current behavior, including bug-like quirks**. A failing
characterization test is a **behavior change to confirm with the maintainer, not an
auto-fix.** Known pinned quirks include:

- Integrity `verificationPass` is `true` even on SHA-256 mismatch/compute failure.
- A non-empty search query searches globally and ignores the chapter filter.
- `formatStringToSuperscript` applies the dot pattern before the hyphen pattern.
- `BookmarkEditsParser` drops empty range lists; any range missing a required field
  discards the entire parse.

Test classes are `<Subject>Test`; fakes are `Fake<Subject>`. Gesture/scroll/animation
paths are not Robolectric-triggerable, so their logic is extracted into pure, unit-tested
holders (e.g. `DashboardNavState`, `StylingGesture`) rather than tested end-to-end.

## Detekt gate

`config/detekt/detekt.yml` is complexity-focused only (CC ≤ 8, params ≤ 8, nesting,
method length ≤ 60). `maxIssues=0` + `config/detekt/baseline.xml` mean any **new**
non-baselined finding fails the build. The baseline holds the irreducible declarative
`@Composable` shells. Do not regenerate the baseline just to silence a new finding.
