<div align="center">
  <img src="design/icon/app-icon.svg" width="128" alt="Everything Launcher Icon">

  <h1>Everything Launcher</h1>

  <p><strong>The fast, minimalist, and truly universal Android launcher.</strong></p>

  <div>
    <a href="https://developer.android.com/about/versions/pie"><img height="28" src="https://img.shields.io/badge/Android-9.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 9+"></a>
    <a href="https://kotlinlang.org/"><img height="28" src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"></a>
    <a href="https://developer.android.com/jetpack/compose"><img height="28" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"></a>
    <a href="LICENSE"><img height="28" src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square" alt="GPL-3.0 License"></a>
  </div>

  <p>Everything Launcher brings together ultra-fast universal search, an integrated Google News RSS feed screen, intelligent contact actions, a clean auto-centering dock with the Everything action button, full settings backup & restore, and deep customization without ads or telemetry.</p>
</div>

---

## 🌟 Key Features

### 🔍 Spotlight Universal Search
- **Live Math & Percentage Calculator**: Type arithmetic expressions and percentage phrases (e.g., `15% of 250`, `20% off 80`, `(45 * 12) / 3`) with instant calculations and one-tap copy.
- **System Settings Search**: Search and jump directly into Android system settings (Wi-Fi, Bluetooth, Hotspot, Display, Battery, Sound, Storage, Developer Options, and more).
- **Quick AI & Web Search Chips**: One-tap query forwarding to AI engines (**ChatGPT**, **Gemini**, **Perplexity**, **Claude**) and web search targets (**Google**, **DuckDuckGo**, **YouTube**, **Wikipedia**, **Reddit**, **GitHub**, **Play Store**, **Maps**, **Amazon**, **Brave**).
- **Customizable Search Chips**: Enable, hide, or reorder individual chips to fit your personal workflow.
- **Live Autocomplete Suggestions**: Real-time web search suggestions powered by Google Suggest with keystroke debouncing.
- **Dynamic App Sections**: Easily toggle whether to show **Recently Opened**, **Frequently Opened**, or **Recently Installed** apps when the search bar is empty.
- **A-Z Alphabet Rail**: Fast alphabetical quick-scrub rail and complete A-Z app list directly inside the search screen.

### 📰 Integrated Newsfeed Screen (Google News)
- **Swipe-to-Read Feed**: Swipe to the left page to access a clean, distraction-free newsfeed blending seamlessly with your launcher's theme.
- **Worldwide Regions**: Select your local edition from a comprehensive list of worldwide countries and languages.
- **Topic Filters**: Browse by category including **Top Stories**, **World**, **Business**, **Technology**, **Entertainment**, **Sports**, **Science**, and **Health**.
- **Integrated Search & Weather**: Search directly from the feed screen or check current weather conditions at a glance.

### 📞 Direct Contact Quick Actions
- When contacts appear in search results, dedicated quick action icons appear right on the card:
  - **📞 Call Button**: Instantly dials the contact without leaving the launcher.
  - **💬 SMS Button**: Opens a direct text message conversation with the contact.
  - Tapping the contact card opens their full contact profile.

### 📱 Centered Bottom Dock & Everything Button
- **Fixed Home Dock**: Pin any app stack to the bottom as an auto-fitting, centered home dock.
- **Everything Button Integration**: When fixed at the bottom, the launcher's Everything button automatically moves into the dock alongside up to 4 apps (up to 5 grid slots total), seamlessly integrating into the dock.
- **Custom Gestures**: Assign actions for Tap, Double-Tap, Long-Press, Swipe-Up, and Swipe-Down on the Everything Button.
- **App Stack Names**: Toggle text labels beneath icons in stacks and dock.

### 💾 Complete Backup & Restore
- **Full Configuration Backup**: Export all homescreen favorites, custom icons, folders, app stacks, and fine-tuned settings to a single timestamped JSON file.
- **One-Tap Restore**: Restore your entire setup seamlessly across devices or fresh installs.

### 🧙‍♂️ First-Time Setup Wizard & Searchable Settings
- **Searchable Settings Screen**: Instantly filter and search through all launcher options and preferences.
- **Setup Wizard**: Interactive onboarding guide helping new users grant optional permissions, configure feed regions, and personalize their launcher.

### 🏠 Home Screen & Favorites Overflow
- **Flexible Favorites Layout**: Choose between a scrollable favorites list or an automatic overflow folder (e.g., a "More" folder) to keep your home screen tidy and minimal.
- **Wallpaper Dimming**: Configurable background dimming to maximize typography readability on bright wallpapers.

### 🎨 Design & Personalization
- **Icon Designer**: Customize shapes, sizes, colors, and letter fallbacks for any individual app icon.
- **Clock Widget**: Tailor the home screen clock with custom typography, spacing, date formats, and weather integration.
- **Material You**: Dynamic wallpaper-derived color palettes with dark and AMOLED theme support.

### ⚡ Performance & Low-End Device Optimization
- **Ultra-Small APK Size**: Reduced from 56 MB to **~13 MB** (~6.8 MB download) via R8 Full Mode, resource shrinking, and locale filtering.
- **Butter-Smooth Frame Rates**: Zero UI-thread logging in release builds, stable LazyList keys with Compose view holder recycling, and thread-safe singleton preferences caching for smooth 60fps scrolling on older phones.

---

## 🤝 Acknowledgements & Credits

Everything Launcher is built upon and inspired by the incredible work of the open-source Android community. We extend our deepest gratitude to the creators and maintainers of the following projects:

1. **[Grace Launcher](https://github.com/Galaxy-rio/GraceLauncher)** by **[Galaxy-rio](https://github.com/Galaxy-rio)**
   - The foundation of this launcher. Grace Launcher provides the minimalist list layout, gesture navigation, app stacks, clock widget designer, and Jetpack Compose architecture.
2. **[Quick Search](https://github.com/teja2495/quick-search)** by **[Teja Karlapudi (teja2495)](https://github.com/teja2495)**
   - Inspired the universal spotlight search experience, device settings indexing, math/percentage calculator engine, Google suggestion integration, and AI/web search target chips.
3. **[Scout](https://github.com/ssjrocks/scout)** by **[Shashank (ssjrocks)](https://github.com/ssjrocks)**
   - Inspired the clean, swipe-accessible Google News RSS feed screen with worldwide region selection and customizable topic feeds.

---

## 📥 Download

Pre-built APKs are automatically compiled and released on GitHub Actions:
- **Latest Optimized Release APK**: Download **EverythingLauncher-Release-Optimized** from [GitHub Actions](https://github.com/zakir786lrk/Everything-launcher-/actions) or [GitHub Releases](https://github.com/zakir786lrk/Everything-launcher-/releases).
- **Requirements**: Android 9.0 (API 28) or higher.

---

## 🛠️ Building From Source

To build the project locally, ensure you have **OpenJDK 21** and the **Android SDK (API 35+)** installed:

```bash
# Clone the repository
git clone https://github.com/zakir786lrk/Everything-launcher-.git
cd Everything-launcher-

# Build the Optimized Release APK (signed with debug key for direct testing)
./gradlew :app:assembleRelease

# Or build the Debug APK
./gradlew :app:assembleDebug
```

The compiled APKs will be generated at:
- Release: `app/build/outputs/apk/release/app-release.apk`
- Debug: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Privacy & Permissions

Everything Launcher is designed with privacy as a first principle:
- **No telemetry, analytics, or user tracking.**
- **Network Access**: Used solely for opt-in live search suggestions, user-requested web searches, and fetching the Google News RSS feed directly from official endpoints. No user data is sent to private servers.
- **Contacts & Usage Access**: Optional permissions used exclusively on-device to power instant search suggestions and contact dial/SMS shortcuts.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** (GPLv3). See the [LICENSE](LICENSE) file for details.
