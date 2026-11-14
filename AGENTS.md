# Everything Launcher - Agent Guidelines & Context

## Project Overview
- **Application**: Everything Launcher (formerly Grace Launcher)
- **Package**: `rikazzz.everythinglauncher`
- **Target SDK**: Android 37 | **Min SDK**: Android 28
- **Primary Tech**: Kotlin 2.4, Jetpack Compose (Material 3), Room Database, KSP, Coroutines & Flow.

## Architecture & Code Guidelines
1. **Compose UI & Performance**:
   - Always use stable keys in lazy lists (`LazyColumn`, `LazyVerticalGrid`) with `contentType`.
   - Never use dynamic indices in item keys (e.g., `key = "${item.key}_$index"`), as this forces full recomposition of all items on insert/delete.
   - Use `LauncherCustomPreferences.getInstance(context)` instead of creating new instances in composables.
   - Debounce rapid search / text events before triggering background or network tasks (e.g., web suggestions).
2. **Build Optimization & APK Size**:
   - Release builds use R8 Full Mode (`android.enableR8.fullMode=true`), `isMinifyEnabled = true`, `isShrinkResources = true`, and `proguard-rules.pro`.
   - Release APK is signed with the debug keystore (`signingConfig = signingConfigs.getByName("debug")`) so release builds can be directly installed and tested without manual signing.
   - Unused locales from third-party libraries are stripped using `resourceConfigurations`.
   - Logging calls (`Log.d`, `Log.v`) are stripped in release builds via ProGuard to minimize UI thread overhead on older devices.
3. **CI / CD**:
   - GitHub Actions workflow (`.github/workflows/build.yml`) builds both `assembleRelease` and `assembleDebug`, uploading `EverythingLauncher-Release-Optimized` and `EverythingLauncher-Debug`.
