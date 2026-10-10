# Workout Log

An offline Android group project built with Java, XML layouts, and SQLite. The requirements are in `SRS.docx` and `SRS.md`.

## Open in Android Studio

Open this folder as the project root:

Open the cloned `Workout-app` folder (the folder containing `settings.gradle`).

Use the Gradle wrapper and a Java 8 JDK. This project uses Android Gradle Plugin 3.1.0, Gradle 4.4.1, SDK platform 23, and Build Tools 27.0.3 to match Android Studio 4.1. It targets API 27 and is tested on Android 8.1. The compile SDK and emulator API do not have to match. This is a coursework/local-install configuration; a Play Store release would require a current toolchain and target SDK.

Copy `gradle.properties.example` to `gradle.properties` and set your Java 8 path if needed. Android Studio creates `local.properties` for your SDK location. Both local configuration files are excluded from Git.

## Build and run

In PowerShell from the project folder:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk8' # Replace with your Java 8 JDK path
.\gradlew.bat assembleDebug --offline --no-daemon
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.cs533.workoutlog/.MainActivity
```

The first build on a teammate's machine may need internet access; omit `--offline` to download dependencies. Select the `app` module and `Pixel_2_API_27` in Android Studio to run through the IDE.

## Features

- Workout history, newest first, with a helpful empty state.
- Five built-in exercises with original vector illustrations.
- An exercise dropdown with Add New Exercise.
- Optional custom images selected through the system picker and copied into app-private storage.
- Required date, sets, and repetitions; optional pounds and notes.
- View, edit, and confirm deletion of workout entries.
- Case-insensitive duplicate exercise protection and field validation.
- Form state retained during rotation and discard confirmation for unsaved changes.
- No accounts, internet permission, analytics, or cloud services.

Each entry records one exercise with the same repetitions and weight for all sets. Custom exercise editing/deletion, animations, per-set values, and cloud backup are outside version 1.

## Code map

| File | Responsibility |
|---|---|
| `WorkoutDb.java` | Exercise and workout tables, seeds, queries, and persistence |
| `Validation.java` | Date, number, weight, and length checks |
| `MainActivity.java` | History and navigation |
| `WorkoutFormActivity.java` | Add/edit entry, dropdown, date picker, and rotation state |
| `ExerciseFormActivity.java` | New exercise and image selection |
| `DetailActivity.java` | Entry details, edit, and confirmed deletion |
| `Images.java` | Bounded image decoding, private copy, and fallback icon |
| `res/layout` | XML screen layouts |
| `res/drawable` | Original vector exercise illustrations and card background |

The resource-generation script is retained in `verification/generate_resources.py` for provenance. Edit XML directly during normal development; running that script replaces the initial layouts and illustrations.

## Device tests

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest --offline --no-daemon
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb install -r .\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w com.cs533.workoutlog.test/android.test.InstrumentationTestRunner
```

The data tests use a separate disposable database. Screen tests temporarily add and clean up their own records in the app database. Use a development emulator for testing.

The test summary, walkthrough scripts, and selected screenshots are in `verification`. Generated build logs, runtime logs, and UI dumps remain local and are excluded from Git. The entire SRS acceptance matrix is a final project checklist; passing the automated tests does not by itself establish complete accessibility or all device compatibility.

## Artwork

All bundled exercise diagrams were created specifically for this project as editable Android vector XML. They are simplified exercise identifiers, not detailed technique instructions. Custom images are supplied by the user.
