# DeX Launcher

A high-performance, feature-rich Samsung DeX-inspired desktop launcher for Android, built with Kotlin and Jetpack Compose. Designed for tablets, foldables, and productivity enthusiasts seeking a desktop-like experience on Android (Android 10 / API 29+).

---

## 🚀 Key Features

- **Samsung DeX Inspired UI**: Professional desktop layout with a taskbar, floating windows/overlays, App Drawer, and Quick Panel.
- **Advanced Standby / Lock Screen Clock Customization**: 
  - Drag-and-drop clock positioning in a simulated standby screen.
  - Custom fonts, color palettes, opacity control, and blur effects for each corner of the clock.
- **Custom Wallpapers (Home & Standby)**:
  - Separate background customization for Desktop and Standby mode.
  - Supports static images and looping video backgrounds (with infinite repeat mode using ExoPlayer).
- **Taskbar & App Drawer**:
  - Toggleable App Drawer overlay with smooth animations (`AnimatedVisibility`) and click-outside/Back-button dismissal.
  - Taskbar displaying pinned apps alongside currently running applications.
  - Long-press and drag icons from App Drawer to Desktop (with grid snapping) or Taskbar.
- **High Performance & Low-End Optimization**:
  - LRU caching for app icons to eliminate UI stutter and lagging.
  - Immutable data models to minimize unnecessary recompositions.
  - Optimized resource usage for low-RAM and older devices.
- **Quick Panel**:
  - Quick access panel for system toggles and utilities without navigating away from the desktop.

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **Media Playback**: ExoPlayer for looping video wallpapers
- **Minimum SDK**: Android 10 (API 29)
- **Target SDK**: Latest Android SDK

---

## 📦 Project Structure

```
com.example.dexlauncher/
│
├── data/               # App repositories, data store, and icon caching
├── service/            # Overlay and taskbar background services
└── ui/                 # Jetpack Compose screens & components
    ├── desktop/        # Desktop grid, wallpaper, and icon management
    ├── drawer/         # App drawer overlay & drag-and-drop logic
    ├── standby/        # Standby mode & custom clock configuration
    ├── quickpanel/     # Quick settings panel
    └── taskbar/        # Taskbar component
```

---

## 🚦 Getting Started

1. Clone the repository.
2. Open the project in Android Studio (Koala or newer recommended).
3. Sync Gradle and run on an Android 10+ device or emulator.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
