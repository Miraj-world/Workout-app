package com.cs533.workoutlog;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.test.InstrumentationTestCase;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;

public class ScreenTests extends InstrumentationTestCase {
    private WorkoutDb db;
    private long exercise,entry;
    @Override protected void setUp() throws Exception {super.setUp();db=new WorkoutDb(getInstrumentation().getTargetContext());exercise=db.exercises().get(0).id;}
    @Override protected void tearDown() throws Exception {if(entry>0&&db.workout(entry)!=null)db.delete(entry);db.close();super.tearDown();}
    private Activity launch(Class<?> type,long id){Intent intent=new Intent(getInstrumentation().getTargetContext(),type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(id>0)intent.putExtra("id",id);Activity a=getInstrumentation().startActivitySync(intent);getInstrumentation().waitForIdleSync();return a;}
    private void ui(Runnable r) throws Exception {getInstrumentation().runOnMainSync(r);getInstrumentation().waitForIdleSync();}
    private void shot(String name) throws Exception {android.os.SystemClock.sleep(400);Bitmap b=getInstrumentation().getUiAutomation().takeScreenshot();if(b!=null){File f=new File(getInstrumentation().getTargetContext().getFilesDir(),name+".png");try(FileOutputStream out=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}}
    public void testWorkoutFormSaveEditAndDetails() throws Exception {
        Activity form=launch(WorkoutFormActivity.class,0);
        ui(()->{((Spinner)form.findViewById(R.id.exercise_spinner)).setSelection(1);((EditText)form.findViewById(R.id.sets)).setText("3");((EditText)form.findViewById(R.id.reps)).setText("10");((EditText)form.findViewById(R.id.weight)).setText("25.50");((EditText)form.findViewById(R.id.notes)).setText("Acceptance test");});
        shot("workout-form");ui(()->form.findViewById(R.id.save_workout).performClick());
        WorkoutDb.Workout found=null;for(WorkoutDb.Workout w:db.workouts())if("Acceptance test".equals(w.notes))found=w;
        assertNotNull(found);entry=found.id;
        Activity detail=launch(DetailActivity.class,entry);assertEquals("25.50 lb",((TextView)detail.findViewById(R.id.detail_weight)).getText().toString());shot("workout-details");ui(detail::finish);
        Activity edit=launch(WorkoutFormActivity.class,entry);ui(()->{((EditText)edit.findViewById(R.id.sets)).setText("4");edit.findViewById(R.id.save_workout).performClick();});assertEquals(4,db.workout(entry).sets);
    }
    public void testInvalidFormAndRotation() throws Exception {
        Activity form=launch(WorkoutFormActivity.class,0);
        ui(()->{((Spinner)form.findViewById(R.id.exercise_spinner)).setSelection(1);((EditText)form.findViewById(R.id.sets)).setText("0");((EditText)form.findViewById(R.id.reps)).setText("8");form.findViewById(R.id.save_workout).performClick();});
        assertNotNull(((EditText)form.findViewById(R.id.sets)).getError());assertFalse(form.isFinishing());
        ui(()->{((EditText)form.findViewById(R.id.sets)).setText("7");((EditText)form.findViewById(R.id.notes)).setText("Rotation draft");});
        Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(WorkoutFormActivity.class.getName(),null,false);
        ui(()->form.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        Activity rotated=monitor.waitForActivityWithTimeout(5000);getInstrumentation().removeMonitor(monitor);assertNotNull("Rotation did not recreate form",rotated);getInstrumentation().waitForIdleSync();
        assertEquals("7",((EditText)rotated.findViewById(R.id.sets)).getText().toString());assertEquals("Rotation draft",((EditText)rotated.findViewById(R.id.notes)).getText().toString());assertEquals(1,((Spinner)rotated.findViewById(R.id.exercise_spinner)).getSelectedItemPosition());
        ui(()->{rotated.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);rotated.finish();});
    }
    public void testCustomExerciseReturnsToWorkout() throws Exception {
        Activity form=launch(WorkoutFormActivity.class,0);
        ui(()->((EditText)form.findViewById(R.id.sets)).setText("6"));
        Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(ExerciseFormActivity.class.getName(),null,false);
        ui(()->{Spinner spinner=(Spinner)form.findViewById(R.id.exercise_spinner);spinner.setSelection(spinner.getCount()-1);});
        Activity add=monitor.waitForActivityWithTimeout(5000);getInstrumentation().removeMonitor(monitor);assertNotNull(add);
        String name="Test Exercise "+System.currentTimeMillis();
        ui(()->{((EditText)add.findViewById(R.id.exercise_name)).setText(name);});shot("custom-exercise");ui(()->add.findViewById(R.id.save_exercise).performClick());
        assertEquals("6",((EditText)form.findViewById(R.id.sets)).getText().toString());assertEquals(name,((Spinner)form.findViewById(R.id.exercise_spinner)).getSelectedItem().toString());
        ui(form::finish);
        db.getWritableDatabase().delete("exercises","name=?",new String[]{name});
    }
    private void dialogAction(String label) throws Exception {
        android.os.SystemClock.sleep(250);
        android.view.accessibility.AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
        assertNotNull(root);
        boolean clicked=false;
        for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(label)) {
            if(node.getText()!=null && label.equalsIgnoreCase(node.getText().toString())){clicked=node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);if(clicked)break;}
        }
        assertTrue("Dialog action not found: "+label,clicked);getInstrumentation().waitForIdleSync();
    }
    public void testDeleteConfirmationAndDiscard() throws Exception {
        entry=db.save(0,exercise,Validation.today(),3,12,"","Delete test");
        Activity detail=launch(DetailActivity.class,entry);
        ui(()->detail.findViewById(R.id.delete).performClick());dialogAction("Cancel");assertNotNull(db.workout(entry));
        ui(()->detail.findViewById(R.id.delete).performClick());dialogAction("Delete");assertNull(db.workout(entry));assertEquals(5,db.exercises().size());
        Activity form=launch(WorkoutFormActivity.class,0);
        ui(()->{((EditText)form.findViewById(R.id.sets)).setText("3");form.onBackPressed();});
        dialogAction("Keep editing");assertFalse(form.isFinishing());assertEquals("3",((EditText)form.findViewById(R.id.sets)).getText().toString());
        ui(form::onBackPressed);dialogAction("Discard");assertTrue(form.isFinishing());
    }
    public void testExerciseValidationAndImagePickerCancellation() throws Exception {
        Activity add=launch(ExerciseFormActivity.class,0);
        ui(()->{((EditText)add.findViewById(R.id.exercise_name)).setText(" bench PRESS ");add.findViewById(R.id.save_exercise).performClick();});
        assertNotNull(((EditText)add.findViewById(R.id.exercise_name)).getError());
        ui(()->{((EditText)add.findViewById(R.id.exercise_name)).setText("Unsaved custom exercise");add.findViewById(R.id.choose_image).performClick();});
        android.os.SystemClock.sleep(600);
        android.view.accessibility.AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
        assertNotNull(root);assertEquals("com.android.documentsui",root.getPackageName().toString());
        assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK));getInstrumentation().waitForIdleSync();
        assertEquals("Unsaved custom exercise",((EditText)add.findViewById(R.id.exercise_name)).getText().toString());assertFalse(add.isFinishing());ui(add::finish);
    }
}
