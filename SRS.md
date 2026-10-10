# Software Requirements Specification
## Workout Log Android Application

**Version:** 1.0 — Draft for group review  
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
