# Verification of the first working version

## Environment

- Android Studio installed: 4.1.0.
- Build: Android Gradle Plugin 3.1.0, Gradle 4.4.1, Java 8, compile SDK 23, Build Tools 27.0.3.
- Runtime: Pixel_2_API_27 emulator, Android 8.1.0 / API 27.
- Emulator was restarted with software graphics after it disconnected during the first installation attempt.

## Confirmed results

- Debug APK and instrumentation APK built successfully offline.
- Both APKs installed successfully on the emulator.
- All 11 instrumentation tests passed in the final run. See `device-tests.log`.
- The final 1,000-entry database check loaded records in 11 ms and saved a new record in 4 ms. This measures database work on this emulator, not end-to-end rendering on every device. See `runtime.log`.
- No AndroidRuntime crash was reported in the captured runtime log.
- A real-input walkthrough selected Squat, entered 3 sets and 12 repetitions with 45 lb and notes, saved it, and verified the history and details screens. The temporary entry was removed with the deletion confirmation.
- Final history, form, and detail screenshots were visually inspected. The initial form was adjusted to avoid automatically opening the keyboard and scrolling away from its title.

## Automated coverage

1. Built-in exercise initialization, trimmed custom names, case-insensitive duplicates, and persistence after reopening the database.
2. Exercise/date/sets/repetitions/weight/notes validation, including upper bounds, invalid dates, overflow, and excess decimal places.
3. Workout creation, retrieval, update without duplication, ordering, persistence, blank weight versus zero, and deletion while retaining exercises.
4. Foreign-key enforcement and same-date ordering.
5. Private image copying, source deletion, and rejection of unreadable images.
6. Database load/save performance with 1,000 entries.
7. Workout form save, detail display, and edit through activity controls.
8. Invalid form handling and draft restoration after rotation.
9. Add New Exercise from the dropdown, returning with the new exercise selected and previous workout values retained.
10. Deletion cancellation/confirmation and unsaved-change keep/discard actions.
11. Duplicate exercise feedback, launching the actual Android image picker, and cancellation without losing the exercise name.

## Remaining project review

- Review the interface and wording with the group and professor's rubric.
- Run a physical-device test and a complete TalkBack, large-font, and smaller-screen review.
- The image-copy helper is tested using a real image file; choosing images from multiple external providers should also be checked manually.
- The database is reopened in persistence tests. A full emulator reboot and OS process-kill scenario have not been acceptance-tested.
- Version 1 uses static illustrations. No GIFs or animation playback is included.
- The older SDK parser emits metadata warnings about newer installed SDK packages. They do not prevent the verified build; the build log ends with BUILD SUCCESSFUL.

# Verification of the course requirement additions (FR-22 to FR-24)

## Environment

- Build: unchanged toolchain (Android Gradle Plugin 3.1.0, Gradle 4.4.1, Java 8, compile SDK 23, Build Tools 27.0.3). Built on Linux with Android Studio 4.1's bundled Java 8 runtime.
- Runtime: Pixel_2_API_27 emulator, Android 8.1 / API 27, started with `-gpu host`. The emulator (version 37.2) crashed several times in its host graphics layer; this was unrelated to the app, and the tests passed after a restart.

## Confirmed results

- `assembleDebug assembleDebugAndroidTest` builds successfully. No dependencies, build files, or manifest entries were added.
- All 23 instrumentation tests passed: the original 11 (DataTests, ScreenTests) and 12 new ones (StatsTests, ProgressDataTests, TabTests).
- Inserting 1,000 sample workouts in one transaction took 403 ms; the existing 1,000-row history check loaded in 19 ms and saved in 3 ms (`WorkoutAcceptance` log tag).
- The sample generator was also checked on a desktop JVM with 200 random seeds: every run produced exactly 1,000 valid entries dated within the last 182 days.
- Screens were checked by hand on the emulator: both tabs, every chart with sample data, the confirmation dialogs, Delete all, and the empty Progress tab.

## New automated coverage

| Test class | Test | What it checks |
|---|---|---|
| StatsTests | testStreakEndsTodayOrYesterday | Streak ending today, ending yesterday, broken streak, and a month boundary |
| StatsTests | testSummaryTiles | Total, last 7 days (today plus six days), streak, exercises used |
| StatsTests | testWeeklyCountsStartMonday | 8 weeks starting Monday, a Sunday counted in the earlier week, labels |
| StatsTests | testVolumeAndTopExercises | sets × reps × weight, blank weight = 0, top 5 ordering, ties by name, zero volume left out |
| StatsTests | testRecordsHeaviestFirstWithFirstDate | Heaviest first, first date the record was reached, blank weights ignored, zero counted |
| StatsTests | testHeaviestPerDateSkipsBlankWeights | Maximum per date, blank weights skipped, oldest first |
| StatsTests | testEmptyData | Every statistic with no workouts |
| StatsTests | testFormattingAndChartHelpers | Number formatting, 1/2/5 × 10^n gridline steps, compact labels, thumbnail sample size |
| ProgressDataTests | testStatRowsAndWeightedExercises | New WorkoutDb queries: rows oldest first, blank weight kept blank, weighted-exercise list |
| ProgressDataTests | testSampleDataInsertAndDeleteAllKeepExercises | 1,000 rows in range using every exercise, weight progression, delete all keeps built-in and custom exercises |
| ProgressDataTests | testSampleDataRollsBackAsOneTransaction | A cancelled insert saves nothing and leaves existing workouts untouched |
| TabTests | testTabSwitchAndRotationKeepSelectedTab | Tab highlight, 48 dp tabs, Add Workout on both tabs, rotation keeps Progress selected, one copy of each fragment, tiles match the database |

## Manual checklist for AT-18 onward

Steps and expected results are in SRS.md Section 10.4. Record Pass/Fail, the date, the tester, and the device in the Result column.

| ID | Scenario | Result |
|---|---|---|
| AT-18 | Tab switching, highlight, and TalkBack announcement | |
| AT-19 | Rotation keeps the Progress tab and the chosen exercise; no duplicates | |
| AT-20 | Original history acceptance tests still pass inside the History tab | |
| AT-21 | Empty Progress tab: zero tiles and empty-state messages | |
| AT-22 | Summary tile values for a known set of workouts | |
| AT-23 | Weight line chart: dropdown contents, max per date, blank weights skipped | |
| AT-24 | Weekly bars start on Monday | |
| AT-25 | Volume chart and personal records with sample data | |
| AT-26 | Load sample data: cancel, load, progress indicator, both tabs refresh | |
| AT-27 | Delete all workouts keeps exercises | |
| AT-28 | Progress updates after add, edit, and delete | |
| AT-29 | History thumbnails for a large custom image | |
| AT-30 | TalkBack summaries and largest font size on the Progress tab | |
| AT-31 | Airplane mode and no manifest permissions | |

## Remaining review for the additions

- AT-18 to AT-31 above still need a manual pass; only the behaviour listed under "New automated coverage" is automated.
- TalkBack was checked through accessibility attributes (selected state, content descriptions), not by listening on a device.
- `SRS.docx` has not been updated with Section 10; `SRS.md` is current.
