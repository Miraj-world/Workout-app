package com.cs533.workoutlog;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import java.io.File;
import java.io.FileOutputStream;

public class DataTests extends InstrumentationTestCase {
    private Context context;
    private WorkoutDb db;
    @Override protected void setUp() throws Exception { super.setUp();context=getInstrumentation().getTargetContext();context.deleteDatabase("acceptance-test.db");db=new WorkoutDb(context,"acceptance-test.db"); }
    @Override protected void tearDown() throws Exception { db.close();context.deleteDatabase("acceptance-test.db");super.tearDown(); }
    public void testBuiltInsAndDuplicateNames(){
        assertEquals(5,db.exercises().size());
        try{db.addExercise("  bEnCh PrEsS  ",null);fail("Duplicate accepted");}catch(SQLiteConstraintException expected){}
        long custom=db.addExercise("  Biceps Curl  ",null);assertTrue(custom>0);assertEquals("Biceps Curl",db.exercises().get(1).name);
        db.close();db=new WorkoutDb(context,"acceptance-test.db");assertEquals(6,db.exercises().size());
    }
    public void testValidationBoundaries(){
        assertNull(Validation.workout(1,Validation.today(),"100","1000","2000.00",""));
        assertNull(Validation.workout(1,Validation.today(),"1","1","",""));
        assertNotNull(Validation.workout(0,Validation.today(),"3","10","",""));
        for(String value:new String[]{"0","-1","101","1.5","abc","999999999999"})assertFalse(value,Validation.integer(value,100));
        for(String value:new String[]{"-1","2000.01","2.333","NaN","1e3","."})assertFalse(value,Validation.weight(value));
        assertFalse(Validation.validDate("2099-01-01"));assertFalse(Validation.validDate("2026-02-30"));assertFalse(Validation.validDate("2026-2-3"));
        try{db.addExercise(" ",null);fail();}catch(IllegalArgumentException expected){}
        assertNotNull(Validation.workout(1,Validation.today(),"3","10","",new String(new char[501])));
    }
    public void testCrudPersistenceOrderingAndExerciseRetention(){
        long exercise=db.addExercise("Test Curl",null);
        long older=db.save(0,exercise,"2020-01-01",3,10,"","");
        long newer=db.save(0,exercise,Validation.today(),4,12,"0","Good session");
        assertEquals(newer,db.workouts().get(0).id);assertEquals("",db.workout(older).weight);assertEquals("0",db.workout(newer).weight);
        db.save(newer,exercise,Validation.today(),5,8,"35.50","Updated");assertEquals(2,db.workouts().size());assertEquals(5,db.workout(newer).sets);
        db.close();db=new WorkoutDb(context,"acceptance-test.db");assertEquals("Updated",db.workout(newer).notes);
        db.delete(older);db.delete(newer);assertEquals(0,db.workouts().size());assertEquals(6,db.exercises().size());
    }
    public void testForeignKeyAndSameDateOrder(){
        long exercise=db.exercises().get(0).id;
        long first=db.save(0,exercise,Validation.today(),3,10,"","");long second=db.save(0,exercise,Validation.today(),3,10,"","");
        assertEquals(second,db.workouts().get(0).id);
        try{db.save(0,999999,Validation.today(),3,10,"","");fail("Missing exercise accepted");}catch(SQLiteConstraintException expected){}
        assertEquals(2,db.workouts().size());assertNotNull(db.workout(first));
    }
    public void testImageCopySurvivesSourceDeletion() throws Exception {
        File original=new File(context.getCacheDir(),"test-source.png");Bitmap b=Bitmap.createBitmap(40,40,Bitmap.Config.ARGB_8888);
        try(FileOutputStream out=new FileOutputStream(original)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
        String saved=Images.importImage(context,Uri.fromFile(original));assertTrue(original.delete());assertTrue(new File(saved).exists());
        assertNotNull(android.graphics.BitmapFactory.decodeFile(saved));Images.removeDraft(saved);
        File invalid=new File(context.getCacheDir(),"invalid.png");try(FileOutputStream out=new FileOutputStream(invalid)){out.write(new byte[]{1,2,3});}
        try{Images.importImage(context,Uri.fromFile(invalid));fail("Unreadable image accepted");}catch(java.io.IOException expected){}finally{invalid.delete();}
    }
    public void testThousandEntryPerformance(){
        long e=db.exercises().get(0).id;
        db.getWritableDatabase().beginTransaction();try{for(int i=0;i<1000;i++)db.save(0,e,"2020-01-01",3,10,"","");db.getWritableDatabase().setTransactionSuccessful();}finally{db.getWritableDatabase().endTransaction();}
        long start=android.os.SystemClock.elapsedRealtime();assertEquals(1000,db.workouts().size());long load=android.os.SystemClock.elapsedRealtime()-start;
        start=android.os.SystemClock.elapsedRealtime();db.save(0,e,Validation.today(),3,10,"","");long save=android.os.SystemClock.elapsedRealtime()-start;
        android.util.Log.i("WorkoutAcceptance","1000 rows: load="+load+"ms, save="+save+"ms");assertTrue("History exceeded 2 seconds: "+load,load<2000);assertTrue("Save exceeded 2 seconds: "+save,save<2000);
    }
}
