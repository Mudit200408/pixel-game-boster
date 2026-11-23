# Pixel Game Booster Development Rules

## 1. Zero Hardcoded Strings Policy
- ALL user-facing strings (labels, titles, subtitles, descriptions, button texts, dialog texts, toasts, error messages, and accessibility content descriptions) MUST be defined in `app/src/main/res/values/strings.xml`.
- In Jetpack Compose UI: always use `stringResource(id = R.string.<string_name>)`.
- In Android Context-aware components (Services, Activities, Utilities, Receivers): always use `context.getString(R.string.<string_name>)`.
- NEVER introduce hardcoded string literals into UI composables, dialogs, or user-facing notifications.

## 2. Workspace Boundaries
- The sibling `essentials` directory is STRICTLY READ-ONLY reference material.
- NEVER edit, touch, or modify any files in `../essentials/`. All project modifications belong exclusively in `pixel-game-booster/`.

## 3. Quality & Verification
- Always ensure all unit tests pass (`./gradlew testDebugUnitTest`) before finalizing changes.
- Ensure all builds pass assembleRelease without warnings or lint vital regressions.
