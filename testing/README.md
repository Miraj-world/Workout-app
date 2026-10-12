# Testing

This folder holds test reports and selected evidence. Runnable Android tests stay in the standard Gradle source directory so Android Studio and the instrumentation runner can discover them.

```text
app/src/androidTest/java/com/cs533/workoutlog/
  DataTests.java          Database, validation, images, performance
  ProgressDataTests.java Statistics queries, sample data, rollback
  ScreenTests.java        Workout and exercise screen interactions
  StatsTests.java         Statistics and chart calculations
  TabTests.java           Tabs, rotation, shutdown regression
testing/
  README.md
  reports/
    BUG_FIX_REPORT.md
    Workout App Debugging and Testing.docx
  evidence/
    2026-10-11/           Baseline, reproduction, final results, launch screenshot
verification/            Earlier verification notes and helper scripts
```

## Run the tests

Use a development emulator; screen tests temporarily create and clean up records in the app database. Other data tests use disposable databases. From the project root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_281'
.\gradlew.bat assembleDebug assembleDebugAndroidTest --offline --no-daemon
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb install -r .\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w com.cs533.workoutlog.test/android.test.InstrumentationTestRunner
```

Latest recorded result: **24 tests passed** on October 11, 2026. See [the bug-fix report](reports/BUG_FIX_REPORT.md) and [final test output](evidence/2026-10-11/bugfix-final-tests.log). The Word document was moved here without editing its contents; the Markdown report records this testing session.
