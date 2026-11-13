<div align="center">
  <img src="design/icon/app-icon.svg" width="128" alt="Everything Launcher Icon">

  <h1>Everything Launcher</h1>

  <p><strong>The fast, minimalist, and truly universal Android launcher.</strong></p>

  <div>
    <a href="https://developer.android.com/about/versions/pie"><img height="28" src="https://img.shields.io/badge/Android-9.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 9+"></a>
    <a href="https://kotlinlang.org/"><img height="28" src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"></a>
    <a href="https://developer.android.com/jetpack/compose"><img height="28" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"></a>
    <a href="LICENSE"><img height="28" src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square" alt="GPL-3.0 License"></a>
  </div>

  <p>Everything Launcher brings together ultra-fast universal search, hidden activity launching, intelligent contact actions, a clean auto-centering dock with the Grace action button, and deep customization without ads or telemetry.</p>
</div>

---

## 🌟 Key Features

### 🔍 Spotlight Universal Search
- **Live Math & Percentage Calculator**: Type arithmetic expressions and percentage phrases (e.g. `15% of 250`, `20% off 80`, `(45 * 12) / 3`) with instant calculation and copy-to-clipboard.
- **System Settings Search**: Search and jump directly into Android system settings (Wi-Fi, Bluetooth, Hotspot, Display, Battery, Sound, Storage, Developer Options, and more).
- **Quick AI & Web Search Chips**: One-tap query forwarding to AI engines (**ChatGPT**, **Gemini**, **Perplexity**, **Claude**) and web search targets (**Google**, **DuckDuckGo**, **YouTube**, **Wikipedia**, **Reddit**, **GitHub**, **Play Store**, **Maps**, **Amazon**, **Brave**).
- **Customizable Search Chips**: Enable or hide individual chips to fit your workflow.
- **Live Autocomplete Suggestions**: Real-time web search suggestions powered by Google Suggest.
- **Search Recents & Frequency**: Easily toggle whether to show **Recently Opened**, **Frequently Opened**, or **Recently Installed** apps when the search bar is empty.

### 📞 Direct Contact Quick Actions
- When contacts appear in search results, dedicated quick action icons appear right on the card:
  - **📞 Call Button**: Instantly dials the contact without leaving the launcher.
  - **💬 SMS Button**: Opens a direct text message conversation with the contact.
  - Tapping the contact card opens their full contact profile.

### ⚡ Custom Activity Shortcuts & Quick Settings Tile
- **Activity Scanner**: Discover and launch internal and hidden activities across all installed applications.
- **Root Fallback**: Automatically leverages root `su` execution when launching non-exported or protected activities.
- **Placement Toggles**: Choose whether custom activities appear in Search, the App Drawer, or both.
- **Quick Settings Tile**: Assign any custom activity shortcut to Android's Quick Settings tile (`CustomShortcutTileService`) for access anywhere on your device.

### 📱 Centered Bottom Dock with Grace Button
- **Fixed Home Dock**: Pin any app stack to the bottom as an auto-fitting, centered home dock.
- **Grace Button Integration**: When fixed at the bottom, the launcher's Grace button automatically moves into the dock alongside up to 4 apps (up to 5 grids total), seamlessly hiding the floating button from the home screen.
- **App Stack Names**: Toggle text labels beneath icons in stacks and dock.

### 🎨 Design & Personalization
- **Icon Designer**: Customize shapes, sizes, and colors for any individual app icon.
- **Clock Widget**: Tailor the home screen clock with custom typography, spacing, and layouts.
- **Material You**: Dynamic wallpaper-derived colors and dark theme support.
- **Lag-Free Performance**: In-memory caching, zero IPC queries on keystrokes, and debounced network requests.

---

## 🤝 Acknowledgements & Credits

Everything Launcher is built upon and inspired by the incredible work of the open-source Android community. We extend our deepest gratitude to the creators and maintainers of the following projects:

1. **[Grace Launcher](https://github.com/Galaxy-rio/GraceLauncher)** by **[Galaxy-rio](https://github.com/Galaxy-rio)**
   - The foundation of this launcher. Grace Launcher provides the minimalist list layout, gesture navigation, app stacks, clock widget designer, and Jetpack Compose architecture.
2. **[Quick Search](https://github.com/teja2495/quick-search)** by **[Teja Karlapudi (teja2495)](https://github.com/teja2495)**
   - Inspired the universal spotlight search experience, device settings indexing, math/percentage calculator engine, Google suggestion integration, and AI/web search target chips.
3. **[Activity Launcher](https://github.com/ActivityLauncher/ActivityLauncher)** by **[ActivityLauncher Team / butzist](https://github.com/ActivityLauncher/ActivityLauncher)**
   - Inspired the internal activity scanner, custom shortcut management for hidden Android activities, root intent launching, and Quick Settings tile integration.

---

## 📥 Download

Pre-built debug APKs are automatically compiled and released on GitHub Actions:
- **Latest APK Release**: Check the [GitHub Releases](https://github.com/zakir786lrk/Everything-launcher-/releases) or the Actions artifacts on [zakir786lrk/Everything-launcher-](https://github.com/zakir786lrk/Everything-launcher-).
- **Requirements**: Android 9.0 (API 28) or higher.

---

## 🛠️ Building From Source

To build the project locally, ensure you have **OpenJDK 21** and the **Android SDK (API 35+)** installed:

```bash
# Clone the repository
git clone https://github.com/zakir786lrk/Everything-launcher-.git
cd Everything-launcher-

# Build the Debug APK
./gradlew :app:assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Privacy & Permissions

Everything Launcher is designed with privacy as a first principle:
- **No telemetry, analytics, or user tracking.**
- **Network Access**: Used solely for opt-in live search suggestions and opening user-requested web searches. No user data is sent to private servers.
- **Contacts & Usage Access**: Optional permissions used exclusively on-device to power instant search suggestions and contact dial/SMS shortcuts.
- **Root Access**: Completely optional; only used if requested to launch unexported activities that Android restricts.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** (GPLv3). See the [LICENSE](LICENSE) file for details.
