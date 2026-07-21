# Trial Reminder

Local Android app that tracks free-trial end dates and sends exact reminders so you can cancel before you get charged.

## Features

- Add a trial with name, optional description, end date, and reminder offset (1 / 3 / 7 days before, or on the day)
- Edit or delete existing trials
- Exact alarms with notification (survives device reboot)
- Empty-state home screen when you have no trials yet

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer, **or** **JDK 17** with the Android SDK
- Android device/emulator API 24+

> Command-line builds need JDK 17 (not newer). Example:
> `JAVA_HOME=/path/to/jdk-17 ./gradlew :app:assembleDebug`

## Build & run

### Android Studio

1. Open this folder as an existing project
2. Let Gradle sync finish
3. Run the `app` configuration on a device or emulator

### Command line

Use **JDK 17** (AGP 8.1 / Room tooling are not reliable on newer JDKs):

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :app:assembleDebug
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Runtime permissions

On first launch (Android 13+), grant **notifications**.

On Android 12+, the app needs **Alarms & reminders** (exact alarms). If scheduling is blocked, you will be prompted to open system settings.

## How reminders work

Reminders fire at **9:00 AM** local time on the chosen reminder day. After reboot, or when exact-alarm permission is granted, future alarms are rescheduled automatically.
