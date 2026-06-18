---
applyTo: "apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/**/*.kt,apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme/*.kt"
---

# Typography instructions

## Rangework typography spec

## Typefaces

| Family | Variable file | Weights used | Role |
|----------|------------------------|--------------------|-----------------------------------|
| DM Sans | dm_sans_*.ttf | 300 · 400 · 500 | All UI text via MaterialTheme |
| DM Mono | dm_mono_*.ttf | 400 · 500 | Numeric and timer contexts only |

Font files now source from `packages/ui-tokens/assets/fonts/` and are generated into Android resources during builds. The `FontFamily` declarations and all
`TextStyle` definitions are in `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme/Type.kt`.

---

## How to apply text styles

### Standard UI text — use MaterialTheme

For all non-numeric UI text, use `MaterialTheme.typography.*`:

```kotlin
// Session title
Text(
 text = session.name,
 style = MaterialTheme.typography.headlineMedium,
)

// Drill instruction body
Text(
 text = step.instruction,
 style = MaterialTheme.typography.bodyMedium,
)

// Button label
Text(
 text = "Start session",
 style = MaterialTheme.typography.labelLarge,
)
```

### Numeric and timer text — use RangeworkMono

For any value that is a number, a measurement, a timer, or a count,
use `RangeworkMono` instead of `MaterialTheme.typography`:

```kotlin
// Rest timer
Text(
 text = formatTimer(remainingSeconds),
 style = RangeworkMono.large,
 color = MaterialTheme.colorScheme.secondary,
)

// Ball count or carry distance
Text(
 text = "20 balls",
 style = RangeworkMono.medium,
)

// Inline rep count or step number annotation
Text(
 text = "×12 reps",
 style = RangeworkMono.small,
 color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

---

## Decision rules — when to use DM Mono

Use `RangeworkMono` (DM Mono) when the text meets **any** of these criteria:

| Criterion | Example values |
|---------------------------------------------|------------------------------------------------|
| It is a countdown or elapsed timer | `02:30` · `00:45` · `1:05:12` |
| It is a ball or rep count | `20 balls` · `×12` · `50 reps` |
| It is a distance or carry value | `52 yd` · `148 m` · `carry: 90 yd` |
| It is a percentage or rate metric | `68%` · `make 7/10` · `hit rate: 70%` |
| It is a step or unit position | `Step 2 of 5` · `Unit 3 of 4` |
| It is a performance log value | `avg carry: 51.2 yd` · `dispersion: ±4 yd` |
| It is a settings value for a numeric field | `15 balls` · `90 sec rest` · `5 reps` |

Use `MaterialTheme.typography` (DM Sans) when the text meets **any** of these:

| Criterion | Example values |
|---------------------------------------------|------------------------------------------------|
| It is a name, title, or label | `Morning session` · `50-yard pitch drill` |
| It is instructional prose | `Open stance, ball centre. Focus on…` |
| It is a chip, tag, or category label | `Wedge` · `Short game` · `Irons` |
| It is navigation, button, or action text | `Start session` · `Save` · `Add unit` |
| It is metadata or descriptive copy | `Last run 2 days ago · 45 min` |
| It is a section header or screen title | `Drill library` · `Session templates` |

### Edge cases

- **Mixed lines**: if a line contains both a label and a number
 (e.g. `Balls: 20`), set the label in `bodyMedium` and the value in
 `RangeworkMono.medium` using an `AnnotatedString` or two adjacent `Text`
 composables.

- **Empty/placeholder states**: use `bodyMedium` or `bodySmall` in
 `onSurfaceVariant` colour. Do not use mono for placeholder text like
 `No sessions yet`.

- **Input fields**: use `bodyLarge` for `TextField` content regardless of
 whether the field accepts numbers. The user is typing, not reading a metric.
 Exception: dedicated numeric steppers (e.g. ball count stepper) may use
 `RangeworkMono.medium` for the displayed value only, not the field itself.

---

## Colour pairings

These are the standard colour + style combinations. Do not invent new pairings
without design review.

| Style | Primary colour token | Secondary use |
|---------------------|-------------------------------|--------------------------------------|
| `headlineLarge/Medium/Small` | `onBackground` | — |
| `titleLarge/Medium` | `onSurface` | `onSurfaceVariant` for de-emphasis |
| `bodyLarge/Medium` | `onSurface` | `onSurfaceVariant` for secondary |
| `bodySmall` | `onSurfaceVariant` | `onSurface` when prominent |
| `labelLarge` | `onSurface` | `onPrimary` when inside a button |
| `labelMedium/Small` | `onSurfaceVariant` | uppercase for section headers |
| `RangeworkMono.large` | `secondary` (#386044) | `onSurface` for neutral metrics |
| `RangeworkMono.medium` | `onSurface` | `secondary` for highlighted values |
| `RangeworkMono.small` | `onSurfaceVariant` | `onSurface` for prominent inline |

---

## Do not

- Do not use `RangeworkMono` for non-numeric text, even in data-dense screens.
- Do not use `MaterialTheme.typography` for timer or metric values.
- Do not hardcode `fontFamily = DmMono` at call sites — always go through
 `RangeworkMono.*` so sizing and tracking remain consistent.
- Do not use font weights other than those declared in the `FontFamily`
 (300, 400, 500 for DM Sans; 400, 500 for DM Mono). Other weights will
 cause the system to synthesise a fake bold, which looks wrong.
- Do not apply `textTransform` inside a `TextStyle` definition — apply
 `.uppercase()` at the call site where the convention applies (section
 headers, `labelSmall` category labels).
- Do not use `sp` values not listed in this spec. If a one-off size is
 needed, raise it for design review rather than inventing an ad-hoc value.

---

## Theme integration

`RangeworkTypography` is wired into `MaterialTheme` via `RangeworkTheme`:

```kotlin
MaterialTheme(
 colorScheme = colorScheme,
 typography = RangeworkTypography,
 content = content,
)
```

`RangeworkMono` is a plain Kotlin object — it does not go through
`MaterialTheme`. Import it directly:

```kotlin
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
```

---

## Adding new text styles

If a new screen or component needs a text style not covered here:

1. Check whether an existing role fits with a colour or weight adjustment.
2. If a genuinely new size or weight is needed, add it to `RangeworkMono`
 (for numeric contexts) or raise a request to add a named extension to
 `Type.kt`.
3. Do not create anonymous `TextStyle(fontFamily = DmSans, fontSize = 15.sp)`
 inline — all styles must be named and defined in `Type.kt` so they
 can be updated globally.
