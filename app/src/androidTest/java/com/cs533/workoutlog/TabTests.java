package com.cs533.workoutlog;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Locale;

/** FR-22 tabs: switching, rotation keeping the selected tab, and no duplicate fragments. Reads the app database without changing it. */
public class TabTests extends InstrumentationTestCase {
    public void testWorkerStopsBeforeDatabaseCloses() throws Exception {
        Intent intent = new Intent(getInstrumentation().getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity main = (MainActivity) getInstrumentation().startActivitySync(intent);
        java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch stopped = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean openAtStop = new java.util.concurrent.atomic.AtomicBoolean();
        android.database.sqlite.SQLiteDatabase database = main.db.getReadableDatabase();
        ui(() -> main.worker.submit(task -> {
            started.countDown();
            try {
                new java.util.concurrent.CountDownLatch(1).await();
            } catch (InterruptedException expected) {
                openAtStop.set(database.isOpen());
            } finally {
                stopped.countDown();
            }
            return null;
        }, (result, error) -> fail("Destroyed activity received a worker callback")));
        try {
            assertTrue("Background job started", started.await(5, java.util.concurrent.TimeUnit.SECONDS));
            ui(main::finish);
            for (int i = 0; i < 100 && !main.isDestroyed(); i++) {
                android.os.SystemClock.sleep(50);
                getInstrumentation().waitForIdleSync();
            }
            assertTrue("Activity destruction completed", main.isDestroyed());
            assertTrue("Background job stopped", stopped.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertTrue("Database must stay open until the worker stops", openAtStop.get());
            assertFalse("Database closes after worker shutdown", database.isOpen());
        } finally {
            ui(main::finish);
        }
    }

    private void ui(Runnable r) {
        getInstrumentation().runOnMainSync(r);
        getInstrumentation().waitForIdleSync();
    }

    private static int count(View view, int id) {
        int n = view.getId() == id ? 1 : 0;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                n += count(group.getChildAt(i), id);
            }
        }
        return n;
    }

    private void waitForProgress(Activity a) {
        for (int i = 0; i < 50 && a.findViewById(R.id.progress_loading).getVisibility() == View.VISIBLE; i++) {
            android.os.SystemClock.sleep(100);
            getInstrumentation().waitForIdleSync();
        }
        assertEquals("Progress tab finished loading", View.GONE, a.findViewById(R.id.progress_loading).getVisibility());
    }

    public void testTabSwitchAndRotationKeepSelectedTab() throws Exception {
        Intent intent = new Intent(getInstrumentation().getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity main = (MainActivity) getInstrumentation().startActivitySync(intent);
        getInstrumentation().waitForIdleSync();
        assertTrue(main.findViewById(R.id.tab_history).isSelected());
        assertTrue(main.findViewById(R.id.history_count).isShown());
        assertTrue(main.findViewById(R.id.add_workout).isShown());
        assertTrue("Tab touch target", main.findViewById(R.id.tab_progress).getHeight() >= 48 * main.getResources().getDisplayMetrics().density - 1);

        ui(() -> main.findViewById(R.id.tab_progress).performClick());
        assertEquals(MainActivity.TAB_PROGRESS, main.selectedTab());
        assertTrue(main.findViewById(R.id.tab_progress).isSelected());
        assertFalse(main.findViewById(R.id.tab_history).isSelected());
        assertFalse(main.findViewById(R.id.history_count).isShown());
        assertTrue(main.findViewById(R.id.chart_weekly).isShown());
        assertTrue("Add Workout stays on both tabs", main.findViewById(R.id.add_workout).isShown());
        waitForProgress(main);

        Instrumentation.ActivityMonitor monitor = getInstrumentation().addMonitor(MainActivity.class.getName(), null, false);
        ui(() -> main.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        MainActivity rotated = (MainActivity) monitor.waitForActivityWithTimeout(5000);
        getInstrumentation().removeMonitor(monitor);
        assertNotNull("Rotation did not recreate the activity", rotated);
        getInstrumentation().waitForIdleSync();
        try {
            assertEquals(MainActivity.TAB_PROGRESS, rotated.selectedTab());
            assertTrue(rotated.findViewById(R.id.tab_progress).isSelected());
            assertFalse(rotated.findViewById(R.id.history_count).isShown());
            View content = rotated.findViewById(R.id.tab_content);
            assertEquals("History fragment added once", 1, count(content, R.id.history_list));
            assertEquals("Progress fragment added once", 1, count(content, R.id.chart_weekly));
            waitForProgress(rotated);
            WorkoutDb db = new WorkoutDb(rotated);
            String expected = String.format(Locale.US, "%,d", db.workoutCount());
            db.close();
            assertEquals(expected, ((TextView) rotated.findViewById(R.id.stat_total)).getText().toString());

            ui(() -> rotated.findViewById(R.id.tab_history).performClick());
            assertTrue(rotated.findViewById(R.id.history_count).isShown());
            assertEquals(rotated.getString(R.string.history_title), ((TextView) rotated.findViewById(R.id.page_title)).getText().toString());
        } finally {
            ui(() -> {
                rotated.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                rotated.finish();
            });
        }
    }
}
