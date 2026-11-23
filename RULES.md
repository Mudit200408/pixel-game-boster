# Pixel Game Booster Development Rules

## 1. Zero Hardcoded Strings Policy
- ALL user-facing strings (labels, titles, subtitles, descriptions, button texts, dialog texts, toasts, error messages, and accessibility content descriptions) MUST be defined in `app/src/main/res/values/strings.xml`.
- In Jetpack Compose UI: always use `stringResource(id = R.string.<string_name>)`.
- In Android Context-aware components (Services, Activities, Utilities, Receivers): always use `context.getString(R.string.<string_name>)`.
- NEVER introduce hardcoded string literals into UI composables, dialogs, or user-facing notifications.

## 2. Design Language (Material 3 Expressive)
- **Theme & Color:** Always wrap UI in `PixelGameBoosterTheme` (`MaterialExpressiveTheme` with `MotionScheme.expressive()`). Support Dynamic Color (Monet) on Android 12+ (`SDK_INT >= 31`) and AMOLED Pitch Black mode (`background`, `surface`, `surfaceContainer`, `surfaceContainerLowest`, `surfaceContainerLow` overridden to `Color.Black`). Never hardcode raw unharmonized colors when Material 3 color roles (`surfaceContainer`, `surfaceContainerHigh`, `secondaryContainer`, `primaryContainer`, `onSurfaceVariant`, `outlineVariant`) apply.
- **Typography:** Use `GoogleSansFlexRounded` (`R.font.google_sans_flex` with `FontVariation.Setting("ROND", 100f)`) across all typography roles—never fall back to default Roboto. Use `PullTitleHeader` for collapsible screen titles.
- **Shapes & Grouped Cards:** Follow `Shapes` (`extraSmall = 4.dp`, `small = 12.dp`, `medium = 16.dp`, `large = 20.dp`, `extraLarge = 24.dp`). Group settings rows inside `RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp)` with `IconToggleItem`:
  - **Unchecked / Unselected:** `surfaceContainer` background with positional corners (`24.dp` outer ends, `4.dp` inner edges; or `16.dp` in standalone sheets like `AppSelectionSheet`).
  - **Checked / Selected:** Morph to `RoundedCornerShape(24.dp)` with `secondaryContainer` background, `onSecondaryContainer` text, and `SemiBold` title weight.
- **Clean Copy:** Keep raw ADB/shell commands out of main row subtitles; encapsulate technical command details inside `FeatureInfoDialog` via `IconToggleItem(command = ...)`.
- **Expressive Controls & Navigation:** Use M3E connected `ToggleButton` groups (`SegmentedPicker`), `PixelFloatingToolbar` (`HorizontalFloatingToolbar`) with `WindowInsets.navigationBars` clearance, `ModalBottomSheet` with `skipPartiallyExpanded = true`, and `PredictiveBackHandler` with interactive scale/corner/edge-shift transitions.
- **Adaptive Layouts:** Support portrait, landscape, and tablets via `rememberAdaptiveLayoutInfo()` (`isWideScreen`, `isCompactHeight`, `isTablet`), using multi-column layouts on wide screens.

## 3. Tactile Haptic Engine
- Every interactive control (switches, `IconToggleItem`, `AppSelectionSheet` rows, buttons, `SegmentedPicker`, sliders, drag thresholds) MUST trigger gated haptics via `HapticUtil` (`performVirtualKeyHaptic`, `performUIHaptic`, `performToggle`, `performSliderHaptic`, `performLightHaptic`).
- `LocalHapticFeedback` is globally wrapped with `HapticUtil.gate(...)` inside `PixelGameBoosterTheme`.

## 4. In-Game Overlay & System Lifecycle
- **Overlay Dragging & Docking:** Use `pointerInteropFilter` with hardware `MotionEvent.rawX`/`rawY` for pill dragging, edge docking (`Gravity.TOP or Gravity.START/END`, `x = 0`), and `setSystemGestureExclusionRects` over pill bounds.
- **Live FPS Monitoring:** Use `TaskFpsMonitor` (`ITaskFpsCallback` via Shizuku) only when the overlay is expanded or `showLiveFps == true`; unregister immediately when collapsed.
- **State Snapshots & Recovery:** Persist system-level game mode overrides (`low_power`, `low_power_trigger_level`, `back_gesture_inset_scale_*`, display preferred mode) in `GameModeManager` so process death, mid-game master toggle off, or direct Game A → Game B switching cleanly restores all system settings.
- **App Suspension Safety:** Always exclude the active game package from its own per-game App Suspension picker (`excludedPackages`) and filter it out at runtime before running `cmd package suspend`.

## 5. Workspace Boundaries
- The sibling `essentials` directory is STRICTLY READ-ONLY reference material.
- NEVER edit, touch, or modify any files in `../essentials/`. All project modifications belong exclusively in `pixel-game-booster/`.

## 6. Quality & Verification
- Always ensure all unit tests pass (`./gradlew testDebugUnitTest`) before finalizing changes.
- Ensure all builds pass `assembleRelease` without warnings or lint vital regressions.
