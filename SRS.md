# Software Requirements Specification
## Workout Log Android Application

**Version:** 1.1 — Draft for group review (adds Section 10, Course requirement additions)  
**Course:** CS 533  
**Date:** October 8, 2026  
**Group members:** To be confirmed  
**Submission deadline:** To be confirmed

## 1. Introduction

### 1.1 Purpose
We will build a simple Android application that helps users record and review their workouts. This document defines the app's features, screens, data, and expected behavior. We will use it to guide development and testing.

### 1.2 Scope
Our app will work offline. Users will select an exercise from a dropdown, enter workout information, and save the entry. They will be able to view, edit, and delete their workout entries. Users will also be able to add custom exercises to the dropdown and optionally attach an image.

The first version will include static images. GIFs and animations are possible future improvements and are not required for this version. Accounts, cloud synchronization, social features, workout recommendations, and wearable-device integration are outside this version's scope.

### 1.3 Definitions
- **Exercise:** A reusable item in the dropdown, such as Bench Press or Squat.
- **Workout entry:** A record of one exercise performed on a particular date.
- **Built-in exercise:** An exercise supplied with the app.
- **Custom exercise:** An exercise added by the user.
- **Local storage:** Data saved on the device rather than on an online server.

## 2. Overall Description

### 2.1 Intended Users
The app is intended for people who want a straightforward way to keep a personal workout history. Users do not need an account or technical knowledge.

### 2.2 Main User Flow
1. The user opens Workout History and taps Add Workout.
2. The user selects an exercise from the dropdown.
3. If the exercise is missing, the user selects Add New Exercise, enters its name, and optionally chooses an image.
4. After saving the new exercise, the app returns to the workout form with that exercise selected and any previously entered values preserved.
5. The user enters the date, sets, repetitions, optional weight, and optional notes.
6. The user saves the entry and sees it in Workout History.
7. The user can open the entry to view its details, edit it, or delete it after confirmation.

### 2.3 Proposed Development Environment
We plan to use Java, XML layouts, and SQLite for an Android app. Android 8.1 (API 27) is the baseline test device. The final Gradle, SDK, and Java build configuration will be selected and verified before implementation, based on the course requirements and installed tools.

### 2.4 Assumptions and Constraints
- One workout entry represents one exercise, with the same repetitions and weight across the recorded sets. Recording different values for individual sets is outside the initial scope.
- The initial weight unit will be pounds (lb), displayed beside the weight field and in workout details. This choice remains subject to group review.
- Built-in exercise names and images will be bundled with the app.
- Custom images will be selected from the device; taking a photo inside the app is outside the initial scope.
- Data will remain available after the app closes or the device restarts. Uninstalling the app or clearing its data may remove the records.
- The professor's rubric, team size, and deadline must be confirmed before this draft is finalized.

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-01 | The app shall display saved workout entries in Workout History, sorted by workout date from newest to oldest. Entries with the same date shall be sorted by creation time from newest to oldest. |
| FR-02 | Each history item shall show the exercise name, date, sets, and repetitions. |
| FR-03 | The workout form shall provide an exercise dropdown containing built-in and saved custom exercises, sorted alphabetically, plus an Add New Exercise option. |
| FR-04 | The app shall initially provide Bench Press, Squat, Deadlift, Push-up, and Shoulder Press as built-in exercises. |
| FR-05 | The user shall be able to add a custom exercise using a required name and an optional image. |
| FR-06 | Exercise names shall be trimmed before saving. Empty names and duplicate names, ignoring letter case, shall be rejected with a clear message. |
| FR-07 | A saved custom exercise shall immediately appear in the dropdown and remain available after the app restarts. |
| FR-08 | Selecting an exercise shall display its image or a default exercise icon if no image is available. |
| FR-09 | The app shall allow the user to choose an optional custom exercise image through the device's image picker. Cancelling image selection shall leave the exercise form available. |
| FR-10 | The app shall keep an app-owned copy of a selected image so that deleting or moving the original image does not remove the exercise image. If copying fails, the app shall explain the failure and allow saving the exercise without an image. |
| FR-11 | The user shall be able to save a workout entry with a required exercise, date, sets, and repetitions, plus optional weight and notes. |
| FR-12 | The date shall default to today and shall be selectable using a date picker. Future dates shall be rejected because this version records completed workouts. |
| FR-13 | Sets and repetitions shall be positive whole numbers. Weight shall be blank or a nonnegative decimal number with at most two decimal places. Invalid input shall prevent saving and identify the field to correct. |
| FR-14 | The app shall show a success message after saving and refresh Workout History. A failed save shall keep entered values available and show an error message. |
| FR-15 | The user shall be able to open an entry and view its exercise name, image, date, sets, repetitions, weight, and notes. Blank weight and notes shall be displayed as Not entered. |
| FR-16 | The user shall be able to edit a saved workout entry. The form shall show existing values and apply the same validation rules as a new entry. Saving shall update the existing entry without creating a duplicate. |
| FR-17 | The user shall be able to delete a workout entry only after confirming a deletion prompt. Cancelling shall keep the entry unchanged. |
| FR-18 | Deleting a workout entry shall not delete its exercise from the dropdown. |
| FR-19 | The app shall save exercises and workout entries locally and provide its core features without an internet connection. |
| FR-20 | When no entries exist, Workout History shall show a clear empty-state message and an Add Workout action. |
| FR-21 | Unsaved form values shall survive screen rotation. If the user leaves a changed form, the app shall ask whether to discard the unsaved changes. |

Editing or deleting exercise definitions is outside the initial scope. Users can edit or delete workout entries and add new exercise definitions.

## 4. Screen Requirements

### 4.1 Workout History
- Title: Workout History.
- List of saved workout entries.
- Add Workout button.
- Empty-state message when no entries exist.
- Tapping an entry opens Workout Details.

### 4.2 Add/Edit Workout
- Exercise dropdown with Add New Exercise option.
- Selected exercise image or default icon.
- Date picker.
- Sets and repetitions fields with numeric keyboards.
- Optional weight field labeled Weight (lb).
- Optional notes field.
- Save and Cancel actions.
- Clear field validation messages.

### 4.3 Add New Exercise
This may be a separate screen or a dialog, depending on the final layout.
- Required exercise name.
- Optional Choose Image action and image preview.
- Save and Cancel actions.
- Saving returns to the workout form with the new exercise selected.
- Cancelling returns to the workout form without creating an exercise or losing workout values.

### 4.4 Workout Details
- Exercise name and image or default icon.
- Workout date, sets, repetitions, optional weight, and notes.
- Edit action.
- Delete action with confirmation.
- Back navigation to Workout History.

## 5. Data Requirements

### 5.1 Exercise Data
| Field | Description |
|---|---|
| Exercise ID | Unique internal identifier. |
| Name | Required exercise name; maximum 60 characters. |
| Image reference | Optional reference to a bundled image or an app-owned image file. |
| Built-in status | Identifies whether the exercise was supplied with the app. |

### 5.2 Workout Entry Data
| Field | Description |
|---|---|
| Entry ID | Unique internal identifier. |
| Exercise ID | Links the entry to an existing exercise. |
| Workout date | Required calendar date. |
| Sets | Whole number from 1 to 100. |
| Repetitions | Whole number from 1 to 1,000. |
| Weight | Optional value from 0 to 2,000 lb, with at most two decimal places. Blank means not entered; zero is a recorded value. |
| Notes | Optional text; maximum 500 characters. |
| Created time | Used to order entries with the same workout date. |

### 5.3 Data Integrity
- The app shall store exercise definitions and workout entries in separate SQLite tables.
- Each workout entry shall reference an existing exercise.
- Built-in exercises shall be initialized once without duplication on later launches.
- Saving changes shall complete successfully or leave previously saved data unchanged.
- The app shall reject values outside the stated limits with understandable messages.

## 6. Nonfunctional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | The core workflow shall function on the Android 8.1/API 27 test emulator. |
| NFR-02 | With up to 1,000 saved workout entries, history loading and local save operations shall each complete within two seconds on the agreed test emulator under normal conditions. |
| NFR-03 | The app shall use readable labels, visible validation messages, and touch targets of at least 48 dp for primary controls. |
| NFR-04 | Meaningful exercise images shall have accessibility descriptions. Decorative images shall not produce unnecessary screen-reader announcements. |
| NFR-05 | Forms shall remain usable with the keyboard open and shall support scrolling on smaller screens. |
| NFR-06 | Images shall be resized for display to avoid excessive memory use. An unreadable or missing image shall show the default icon without crashing the app. |
| NFR-07 | The app shall not require an account or upload workout records or selected images. Image selection shall not request unrelated device permissions. |
| NFR-08 | Normal actions, including invalid input, image-picker cancellation, rotation, and returning from the background, shall not crash the app or corrupt saved data. |
| NFR-09 | Exercise dropdowns, lists, and detail screens shall consistently reflect successfully saved changes. |

## 7. Acceptance Tests

| ID | Scenario | Expected Result |
|---|---|---|
| AT-01 | Launch the app for the first time. | History shows an empty state; the dropdown contains the five built-in exercises. |
| AT-02 | Select a built-in exercise. | Its illustration appears; the user can enter workout values. |
| AT-03 | Add a custom exercise without an image. | It appears in the dropdown, is selected, and shows the default icon. |
| AT-04 | Add a custom exercise with an image, then move or delete the original image. | The app-owned image still displays, including after restart. |
| AT-05 | Cancel image selection or cancel adding an exercise. | No unwanted exercise is created; previously entered workout values remain. |
| AT-06 | Add a duplicate exercise name using different capitalization or surrounding spaces. | The app rejects the duplicate and explains the problem. |
| AT-07 | Save a valid workout entry and restart the app. | The entry remains in History and its details match the saved values. |
| AT-08 | Try empty required fields, zero sets, negative repetitions, out-of-range values, excess decimal places, or a future date. | Saving is prevented and the relevant fields show clear messages. |
| AT-09 | Save with blank weight, then save another entry with weight zero. | Details distinguish Not entered from 0 lb. |
| AT-10 | Edit an existing workout entry. | Its values update and the number of entries stays unchanged. |
| AT-11 | Cancel deletion, then confirm deletion. | Cancellation keeps the entry; confirmation removes it while keeping its exercise available. |
| AT-12 | Save entries for different dates and several entries for the same date. | History follows the required date and creation-time ordering. |
| AT-13 | Rotate the device while completing a form or return after backgrounding. | The app remains usable and unsaved form values are retained during rotation. |
| AT-14 | Leave a changed form and choose to keep editing or discard changes. | Keep editing preserves values; discard returns without saving them. |
| AT-15 | Use core features with networking disabled. | Adding exercises and adding, viewing, editing, and deleting workout entries still work. |
| AT-16 | Supply an unreadable image or simulate an image-copy failure. | The app shows a useful message or default icon and remains usable. |
| AT-17 | Load History and save an entry with 1,000 existing records. | Each operation meets the two-second performance target on the agreed emulator. |

## 8. Completion Criteria
We will consider the first version complete when all required features are implemented, the acceptance tests pass on the Android 8.1 emulator, and we can demonstrate the full workflow: add an exercise, save a workout, view it, edit it, and delete it. The project must build successfully and include the required course documentation and screenshots.

## 9. Items to Confirm Before Final Approval
1. Group member names, group size, and submission deadline.
2. Professor's rubric, required Android components, and required SRS format.
3. Approval of one exercise per workout entry and a fixed pounds unit.
4. Final built-in exercise images, their source, and permission to use them.
5. Final project toolchain and the group's shared test environment.

## 10. Course requirement additions

The course requires at least one fragment and a data visualization. The requirements below extend Sections 3 to 7. FR-01 to FR-21, NFR-01 to NFR-09 and AT-01 to AT-17 are unchanged; the Workout History screen in Section 4.1 is now the History tab.

### 10.1 Functional Requirements

| ID | Requirement |
|---|---|
| FR-22 | The main screen shall show a two-button tab bar, History and Progress, under the title, and switch between two fragments. The History tab shall contain the existing history list, empty state, and workout count. The Add Workout button shall remain visible on both tabs. The selected tab shall be visibly highlighted, announced by screen readers, and kept after screen rotation without duplicating content. Tab buttons shall be at least 48 dp tall. |
| FR-23 | The Progress tab shall show: (a) summary tiles for total workouts, workouts in the last 7 days, the current streak, and the number of exercises used; (b) a line chart of the heaviest weight on each workout date for one exercise chosen from a dropdown that lists exercises with at least one weighted entry; (c) a bar chart of workouts per week for the last 8 weeks; (d) a horizontal bar chart of total volume for the top 5 exercises; and (e) personal records listing the heaviest weight per exercise and its date, heaviest first. Each chart shall show a short message when there is not enough data. The tab shall scroll, show a loading indicator while it calculates, and refresh when it is shown again and whenever workouts change. |
| FR-24 | The History tab shall offer Load sample data, which after confirmation adds about 1,000 realistic workouts over the last 6 months using the existing exercises, in a single all-or-nothing save. It shall also offer Delete all workouts, which after a confirmation that states exercises are kept, removes every workout entry. Both actions shall refresh both tabs. |

### 10.2 Definitions used by the Progress tab
- **Workout:** one workout entry, as in Section 1.3. Totals and weekly counts count entries.
- **Last 7 days:** today and the six days before it.
- **Current streak:** the number of consecutive days with at least one workout, ending today, or ending yesterday if nothing has been logged today. Otherwise 0.
- **Week:** Monday through Sunday. The last 8 weeks end with the week that contains today.
- **Volume:** sets × repetitions × weight, in pounds. A blank weight counts as zero volume, so bodyweight-only exercises do not appear in the volume chart.
- **Heaviest weight:** blank weights are skipped; zero is a recorded weight. A personal record's date is the first date that weight was reached.
- **Enough data:** the line chart needs at least two dated points; a bar chart needs at least one value above zero.

### 10.3 Nonfunctional notes
- Progress calculations and sample-data inserts run off the main thread and stop when the screen closes; a sample-data load that is interrupted saves nothing.
- History list thumbnails are decoded at row size and cached, in support of NFR-02 and NFR-06.
- The additions use no network access, no new permissions, and no third-party libraries (NFR-07, FR-19).

### 10.4 Acceptance Tests

| ID | Scenario and steps | Expected Result |
|---|---|---|
| AT-18 | Open the app. Tap Progress, then History. Repeat with TalkBack on. | The tapped tab is filled green and the other is pale; the title changes between Workout History and Your Progress; TalkBack announces "Progress tab selected" or "History tab selected" and reads the selected state. Add Workout is visible on both tabs. |
| AT-19 | Select Progress, choose an exercise in the weight dropdown, then rotate the device and rotate back. | Progress stays selected and the chosen exercise is still shown. Each list and chart appears once, with no duplicated content. |
| AT-20 | On the History tab, repeat AT-01, AT-07, AT-11 and AT-12. | Behavior is unchanged: ordering, count, empty state, and opening details all work inside the tab. |
| AT-21 | Delete all workouts, then open Progress. | All four tiles show 0; each chart shows its empty-state message; Personal records shows its empty message; nothing crashes. |
| AT-22 | With no workouts, add workouts dated today (two entries), yesterday, and 5 days ago, using two different exercises. Open Progress. | Total workouts 4; Last 7 days 4; Day streak 2; Exercises used 2. |
| AT-23 | Add Bench Press entries: 100 lb and 120 lb on one date, 130 lb on a later date, and one with a blank weight. Add a Push-up entry with a blank weight. Open Progress and pick Bench Press. | The dropdown lists Bench Press but not Push-up. The line chart shows two points, 120 and 130, with dots and labelled axes. An exercise with weights on only one date shows the "at least two dates" message. |
| AT-24 | Add workouts on a Sunday and on the following Monday, then view Workouts per week. | Eight bars, labelled by the Monday that starts each week. The Sunday entry counts in the earlier week and the Monday entry in the later week. Bar values are printed on the bars. |
| AT-25 | Load sample data, then view Top exercises by volume and Personal records. | At most 5 bars, largest first, with values at the bar ends and gridlines at rounded steps. Records list each weighted exercise once, heaviest first, with the date. Push-up (blank weight) is not in either list. |
| AT-26 | Tap Load sample data and choose Cancel. Tap it again and choose Load. | Cancel adds nothing. Load shows a progress indicator, then a message that 1,000 sample workouts were loaded. Existing workouts are kept, and both tabs show the new data. History loads within the NFR-02 two-second target. |
| AT-27 | Tap Delete all workouts and choose Cancel; then repeat and confirm. Open Add Workout. | Cancel keeps every workout. Confirm removes all workouts; History shows the empty state; Progress shows its empty states; the dropdown still contains every built-in and custom exercise. |
| AT-28 | With Progress data showing, add, edit, and delete a workout through the normal screens, returning to Progress each time. | Tiles, charts, and records update after each change. |
| AT-29 | Create a custom exercise with a large photo, load sample data, add a few workouts for that exercise, and scroll History quickly. Open one entry. | Rows show the small image and scrolling stays smooth; the details screen still shows the larger image. |
| AT-30 | Turn on TalkBack and move through the Progress tab. Also check with the largest font size. | Each tile reads as one item (for example "Total workouts: 1,000"); each chart reads a summary of its data or its empty message; each record reads as one item. The tab scrolls and nothing is cut off. |
| AT-31 | Enable airplane mode and repeat AT-18, AT-26 and AT-27. Inspect the merged manifest. | Everything works offline. The manifest declares no permissions; allowBackup is false; only the launcher activity is exported. |

