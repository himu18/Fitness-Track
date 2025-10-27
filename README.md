FitnessTrack (Android, Kotlin, Jetpack Compose)

FitnessTrack is a lightweight fitness tracking app that counts steps, tracks a daily goal, shows a simple 7-day history, and supports background tracking using a foreground service. It uses only native Android APIs and SharedPreferences (no database) and follows a basic MVVM architecture.

Features

- Step counting with SensorManager
- Default daily goal of 100 steps (configurable in code)
- Auto-stop counting when goal is reached (capped at 100)
- Background tracking using a Foreground Service (auto-started on app launch)
- 7-day history view (stored in SharedPreferences)
- Data persists across app restarts and background removal
- MVVM with ViewModel + Repository + Manager
- Material 3 UI with Today and History tabs

Tech Stack

- Kotlin, Jetpack Compose, Material 3
- ViewModel + StateFlow
- SensorManager (TYPE_STEP_COUNTER / TYPE_STEP_DETECTOR)
- Foreground Service + Notification channel
- SharedPreferences for storage (no local DB)

Project Structure

- App.kt: Application class providing global context
- MainActivity.kt: Compose UI and navigation (Today, History)
- StepSensorListener.kt: Sensor listener to detect steps
- StepCountManager.kt: Reads/writes step data and history in SharedPreferences
- StepTrackingService.kt: Foreground service for background step tracking
- ui/viewmodel/StepTrackingViewModel.kt: UI state + periodic saves
- repository/StepCountRepository.kt: Abstraction over the manager

Behavior

- Daily goal: 100 steps (default). All progress displays use 100 as target.
- Goal reached: step accumulation stops for today; further sensor events are ignored.
- New day: yesterday's steps saved to history, counters reset, and goal-achieved flag cleared.
- Background: service runs with low priority notification; steps continue counting.

Permissions

- ACTIVITY_RECOGNITION (Android 10+)
- FOREGROUND_SERVICE and FOREGROUND_SERVICE_HEALTH
- POST_NOTIFICATIONS (Android 13+)

These are requested at runtime where applicable.

Build & Run

1. Open the project in Android Studio.
2. Connect a device or start an emulator with step counter support.
3. Run the app. Grant activity recognition and notifications when prompted.
4. The foreground service starts automatically on launch.

Notes

- History stores only the last 7 days and is pruned automatically.
- No internet or external storage used.
- If your emulator lacks step sensors, use a real device for accurate counting.

License

MIT


