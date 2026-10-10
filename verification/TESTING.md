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
