package com.cs533.workoutlog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All data remains inside this application's private database. */
public class WorkoutDb extends SQLiteOpenHelper {
    public static class Exercise {
        public long id; public String name, image;
        public Exercise(long id, String name, String image) { this.id=id; this.name=name; this.image=image; }
        @Override public String toString() { return name; }
    }
    public static class Workout {
        public long id, exerciseId; public String name, image, date, weight, notes;
        public int sets, reps;
    }
    public WorkoutDb(Context context) { this(context, "workout-log.db"); }
    WorkoutDb(Context context, String name) { super(context, name, null, 1); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE exercises (_id INTEGER PRIMARY KEY, name TEXT NOT NULL, name_key TEXT NOT NULL UNIQUE, image TEXT, builtin INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE workouts (_id INTEGER PRIMARY KEY, exercise_id INTEGER NOT NULL REFERENCES exercises(_id), date TEXT NOT NULL, sets INTEGER NOT NULL CHECK(sets BETWEEN 1 AND 100), reps INTEGER NOT NULL CHECK(reps BETWEEN 1 AND 1000), weight TEXT, notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX workout_order ON workouts(date DESC, created_at DESC, _id DESC)");
        String[] names={"Bench Press","Squat","Deadlift","Push-up","Shoulder Press"};
        String[] images={"bench_press","squat","deadlift","push_up","shoulder_press"};
        for (int i=0;i<names.length;i++) {
            ContentValues v=new ContentValues(); v.put("name",names[i]); v.put("name_key",key(names[i])); v.put("image","res:"+images[i]); v.put("builtin",1); db.insertOrThrow("exercises",null,v);
        }
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { throw new IllegalStateException("Database migration required"); }
    private static String key(String name) { return name.trim().toLowerCase(Locale.ROOT); }
    public List<Exercise> exercises() {
        List<Exercise> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT _id,name,image FROM exercises ORDER BY name COLLATE NOCASE",null)) {
            while(c.moveToNext()) result.add(new Exercise(c.getLong(0),c.getString(1),c.getString(2)));
        } return result;
    }
    public long addExercise(String name,String image) {
        name=name.trim();
        if(name.isEmpty() || name.length()>60) throw new IllegalArgumentException("Enter an exercise name of 1 to 60 characters.");
        ContentValues v=new ContentValues(); v.put("name",name);v.put("name_key",key(name));v.put("image",image);
        return getWritableDatabase().insertOrThrow("exercises",null,v);
    }
    public long save(long id,long exerciseId,String date,int sets,int reps,String weight,String notes) {
        String error=Validation.workout(exerciseId,date,String.valueOf(sets),String.valueOf(reps),weight,notes);
        if(error!=null) throw new IllegalArgumentException(error);
        ContentValues v=new ContentValues();v.put("exercise_id",exerciseId);v.put("date",date);v.put("sets",sets);v.put("reps",reps);
        if(weight.trim().isEmpty()) v.putNull("weight"); else v.put("weight",weight.trim());
        v.put("notes",notes.trim());
        if(id>0) { if(getWritableDatabase().update("workouts",v,"_id=?",new String[]{String.valueOf(id)})!=1) throw new IllegalStateException("Entry no longer exists."); return id; }
        v.put("created_at",System.currentTimeMillis()); return getWritableDatabase().insertOrThrow("workouts",null,v);
    }
    public List<Workout> workouts() { return query(null); }
    public Workout workout(long id) { List<Workout> rows=query(id);return rows.isEmpty()?null:rows.get(0); }
    private List<Workout> query(Long id) {
        List<Workout> result=new ArrayList<>();
        String sql="SELECT w._id,w.exercise_id,e.name,e.image,w.date,w.sets,w.reps,w.weight,w.notes FROM workouts w JOIN exercises e ON e._id=w.exercise_id";
        sql+=id==null?" ORDER BY w.date DESC,w.created_at DESC,w._id DESC":" WHERE w._id=?";
        try(Cursor c=getReadableDatabase().rawQuery(sql,id==null?null:new String[]{id.toString()})) {
            while(c.moveToNext()) { Workout w=new Workout();w.id=c.getLong(0);w.exerciseId=c.getLong(1);w.name=c.getString(2);w.image=c.getString(3);w.date=c.getString(4);w.sets=c.getInt(5);w.reps=c.getInt(6);w.weight=c.isNull(7)?"":c.getString(7);w.notes=c.getString(8);result.add(w); }
        } return result;
    }
    public void delete(long id) { if(getWritableDatabase().delete("workouts","_id=?",new String[]{String.valueOf(id)})!=1) throw new IllegalStateException("Entry no longer exists."); }

    /** Lets a long write stop early; the whole transaction is then rolled back. */
    public interface Cancel {
        boolean cancelled();
    }

    public int workoutCount() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM workouts", null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Every workout as a plain row for {@link Stats}, oldest first. Blank weight stays null. */
    public List<Stats.Row> statRows() {
        List<Stats.Row> result = new ArrayList<>();
        String sql = "SELECT w.date,e.name,w.sets,w.reps,w.weight FROM workouts w JOIN exercises e ON e._id=w.exercise_id ORDER BY w.date,w.created_at,w._id";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                result.add(new Stats.Row(c.getString(0), c.getString(1), c.getInt(2), c.getInt(3), c.isNull(4) ? null : c.getString(4)));
            }
        }
        return result;
    }

    /** Exercises with at least one workout that has a weight, sorted by name, for the Progress chart dropdown. */
    public List<Exercise> weightedExercises() {
        List<Exercise> result = new ArrayList<>();
        String sql = "SELECT e._id,e.name,e.image FROM exercises e WHERE EXISTS (SELECT 1 FROM workouts w WHERE w.exercise_id=e._id AND w.weight IS NOT NULL AND TRIM(w.weight)<>'') ORDER BY e.name COLLATE NOCASE";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                result.add(new Exercise(c.getLong(0), c.getString(1), c.getString(2)));
            }
        }
        return result;
    }

    /**
     * Inserts sample workouts in one transaction through {@link #save}, so each entry gets
     * the normal validation. Any failure or cancellation rolls back every entry.
     */
    public int insertSampleData(List<SampleData.Entry> entries, Cancel cancel) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (SampleData.Entry e : entries) {
                if (cancel.cancelled()) {
                    throw new IllegalStateException("Cancelled");
                }
                save(0, e.exerciseId, e.date, e.sets, e.reps, e.weight, e.notes);
            }
            db.setTransactionSuccessful();
            return entries.size();
        } finally {
            db.endTransaction();
        }
    }

    /** Removes every workout entry. Exercises, including custom ones, are kept (FR-18). */
    public int deleteAllWorkouts() {
        // "1" makes delete() return the number of rows removed.
        return getWritableDatabase().delete("workouts", "1", null);
    }
}
