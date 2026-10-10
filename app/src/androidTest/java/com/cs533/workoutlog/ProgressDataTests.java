package com.cs533.workoutlog;

import android.content.Context;
import android.test.InstrumentationTestCase;
import java.util.List;
import java.util.Random;

/** FR-23/FR-24 database queries and sample data, using a separate disposable database. */
public class ProgressDataTests extends InstrumentationTestCase {
    private static final String NAME = "progress-test.db";
    private Context context;
    private WorkoutDb db;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getTargetContext();
        context.deleteDatabase(NAME);
        db = new WorkoutDb(context, NAME);
    }

    @Override
    protected void tearDown() throws Exception {
        db.close();
        context.deleteDatabase(NAME);
        super.tearDown();
    }

    private long exercise(String name) {
        for (WorkoutDb.Exercise e : db.exercises()) {
            if (e.name.equals(name)) {
                return e.id;
            }
        }
        throw new AssertionError(name);
    }

    public void testStatRowsAndWeightedExercises() {
        db.save(0, exercise("Bench Press"), "2026-01-02", 3, 5, "135.5", "");
        db.save(0, exercise("Bench Press"), "2026-01-01", 3, 5, "", "");
        db.save(0, exercise("Push-up"), "2026-01-03", 3, 20, "", "");
        assertEquals(3, db.workoutCount());
        List<Stats.Row> rows = db.statRows();
        assertEquals(3, rows.size());
        assertEquals("Oldest first", "2026-01-01", rows.get(0).date);
        assertNull("Blank weight stays blank", rows.get(0).weight);
        assertEquals("135.5", Stats.format(rows.get(1).weight));
        assertEquals("Bench Press", rows.get(1).exercise);
        List<WorkoutDb.Exercise> weighted = db.weightedExercises();
        assertEquals("Only exercises with a weighted entry", 1, weighted.size());
        assertEquals("Bench Press", weighted.get(0).name);
        db.save(0, exercise("Push-up"), "2026-01-04", 3, 20, "0", "");
        assertEquals("Zero is a recorded weight", 2, db.weightedExercises().size());
    }

    public void testSampleDataInsertAndDeleteAllKeepExercises() {
        db.addExercise("Biceps Curl", null);
        String today = Validation.today();
        long start = android.os.SystemClock.elapsedRealtime();
        int inserted = db.insertSampleData(SampleData.generate(db.exercises(), today, new Random(7)), () -> false);
        long elapsed = android.os.SystemClock.elapsedRealtime() - start;
        android.util.Log.i("WorkoutAcceptance", "sample insert of " + inserted + " rows: " + elapsed + "ms");
        assertEquals(SampleData.TARGET, inserted);
        assertEquals(1000, db.workoutCount());
        List<Stats.Row> rows = db.statRows();
        long first = Stats.day(today) - SampleData.DAYS + 1;
        for (Stats.Row r : rows) {
            assertTrue(r.date, Stats.day(r.date) >= first && r.date.compareTo(today) <= 0);
        }
        assertEquals("Every existing exercise is used", 6, Stats.exercisesUsed(rows));
        assertEquals("Push-ups are bodyweight", 5, db.weightedExercises().size());
        List<Stats.Point> bench = Stats.heaviestPerDate(rows, "Bench Press");
        assertTrue("Weight progresses", bench.get(bench.size() - 1).value > bench.get(0).value);

        assertEquals(1000, db.deleteAllWorkouts());
        assertEquals(0, db.workoutCount());
        assertEquals("Exercises, including custom ones, are kept", 6, db.exercises().size());
        assertEquals(0, db.deleteAllWorkouts());
    }

    public void testSampleDataRollsBackAsOneTransaction() {
        db.save(0, exercise("Squat"), "2026-01-01", 3, 5, "185", "Existing");
        int[] calls = {0};
        try {
            db.insertSampleData(SampleData.generate(db.exercises(), Validation.today(), new Random(3)), () -> ++calls[0] > 500);
            fail("Cancelled insert completed");
        } catch (IllegalStateException expected) {
        }
        assertEquals("No sample rows kept; existing workout untouched", 1, db.workoutCount());
        assertEquals("Existing", db.workouts().get(0).notes);
    }
}
