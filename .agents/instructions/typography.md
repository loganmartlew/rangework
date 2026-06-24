# Typography instructions

Applies to: `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/**/*.kt`

## Typefaces

| Family  | File pattern    | Weights used    | Role                                   |
| ------- | --------------- | --------------- | -------------------------------------- |
| DM Sans | `dm_sans_*.ttf` | 300 · 400 · 500 | All UI text via `MaterialTheme`        |
| DM Mono | `dm_mono_*.ttf` | 400 · 500       | Numeric, timer, and code-like contexts |

Font files live in `apps/mobile/androidApp/src/main/res/font/`. `FontFamily` declarations and all `TextStyle` definitions are in `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme/Type.kt`.

## Applying text styles

### Standard UI text — use `MaterialTheme.typography.*`

```kotlin
Text(text = session.name, style = MaterialTheme.typography.headlineMedium)
Text(text = step.instruction, style = MaterialTheme.typography.bodyMedium)
Text(text = "Start session", style = MaterialTheme.typography.labelLarge)
```

### Numeric and timer text — use `RangeworkMono`

```kotlin
Text(text = formatTimer(remainingSeconds), style = RangeworkMono.large, color = MaterialTheme.colorScheme.secondary)
Text(text = "20 balls", style = RangeworkMono.medium)
Text(text = "×12 reps", style = RangeworkMono.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
```

## When to use DM Mono (`RangeworkMono`)

Use `RangeworkMono` when the text is any of:

| Criterion                        | Examples                                        |
| -------------------------------- | ----------------------------------------------- |
| Countdown or elapsed timer       | `02:30` · `00:45` · `1:05:12`                   |
| Ball or rep count                | `20 balls` · `×12` · `50 reps`                  |
| Distance or carry value          | `52 yd` · `148 m` · `carry: 90 yd`             |
| Percentage or rate metric        | `68%` · `make 7/10` · `hit rate: 70%`           |
| Step or unit position            | `Step 2 of 5` · `Unit 3 of 4`                   |
| Performance log value            | `avg carry: 51.2 yd` · `dispersion: ±4 yd`      |
| Settings value for numeric field | `15 balls` · `90 sec rest` · `5 reps`           |
| Code-like text to copy verbatim  | Connection URLs, endpoints, server identifiers   |

Use `MaterialTheme.typography` (DM Sans) when the text is any of:

| Criterion                         | Examples                                        |
| --------------------------------- | ----------------------------------------------- |
| Name, title, or label             | `Morning session` · `50-yard pitch drill`       |
| Instructional prose               | `Open stance, ball centre. Focus on…`           |
| Chip, tag, or category label      | `Wedge` · `Short game` · `Irons`               |
| Navigation, button, or action     | `Start session` · `Save` · `Add unit`           |
| Metadata or descriptive copy      | `Last run 2 days ago · 45 min`                  |
| Section header or screen title    | `Drill library` · `Session templates`           |

### Edge cases

- **Mixed lines** (e.g. `Balls: 20`): label in `bodyMedium`, value in `RangeworkMono.medium` via `AnnotatedString` or two adjacent `Text` composables.
- **Empty/placeholder states**: use `bodyMedium` or `bodySmall` in `onSurfaceVariant`. Do not use mono for placeholder text.
- **Input fields**: use `bodyLarge` for `TextField` content regardless of whether it accepts numbers. Exception: dedicated numeric steppers may use `RangeworkMono.medium` for the displayed value only.

## Colour pairings

| Style                        | Primary colour token  | Secondary use                      |
| ---------------------------- | --------------------- | ---------------------------------- |
| `headlineLarge/Medium/Small` | `onBackground`        | —                                  |
| `titleLarge/Medium`          | `onSurface`           | `onSurfaceVariant` for de-emphasis |
| `bodyLarge/Medium`           | `onSurface`           | `onSurfaceVariant` for secondary   |
| `bodySmall`                  | `onSurfaceVariant`    | `onSurface` when prominent         |
| `labelLarge`                 | `onSurface`           | `onPrimary` when inside a button   |
| `labelMedium/Small`          | `onSurfaceVariant`    | uppercase for section headers      |
| `RangeworkMono.large`        | `secondary` (#386044) | `onSurface` for neutral metrics    |
| `RangeworkMono.medium`       | `onSurface`           | `secondary` for highlighted values |
| `RangeworkMono.small`        | `onSurfaceVariant`    | `onSurface` for prominent inline   |

## Rules

- Do not use `RangeworkMono` for non-numeric prose.
- Do not use `MaterialTheme.typography` for timer or metric values.
- Do not hardcode `fontFamily = DmMono` at call sites — always go through `RangeworkMono.*`.
- Do not use font weights other than those declared in the `FontFamily` (300, 400, 500 for DM Sans; 400, 500 for DM Mono).
- Do not apply `textTransform` inside a `TextStyle` — apply `.uppercase()` at the call site.
- Do not use `sp` values not listed in the spec; raise for design review instead.
- Do not create anonymous `TextStyle(fontFamily = DmSans, fontSize = 15.sp)` inline — all styles must be named and defined in `Type.kt`.

## Theme integration

`RangeworkTypography` is wired into `MaterialTheme` via `RangeworkTheme`. `RangeworkMono` is a plain Kotlin object — import it directly:

```kotlin
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
```

## Adding new text styles

1. Check whether an existing role fits first.
2. If genuinely new, add it to `RangeworkMono` (numeric contexts) or add a named extension to `Type.kt`.
3. Never create anonymous inline styles.
