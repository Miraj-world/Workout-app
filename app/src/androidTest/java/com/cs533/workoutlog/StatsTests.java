package com.cs533.workoutlog;

import android.test.InstrumentationTestCase;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** FR-23 statistics. Stats is pure Java, so these use a fixed "today" (Wednesday 2026-10-07; Monday is 2026-10-05). */
public class StatsTests extends InstrumentationTestCase {
    private static final String TODAY = "2026-10-07";

    private static Stats.Row row(String date, String exercise, int sets, int reps, String weight) {
        return new Stats.Row(date, exercise, sets, reps, weight);
    }

    private static List<Stats.Row> dates(String... dates) {
        List<Stats.Row> rows = new ArrayList<>();
        for (String d : dates) {
            rows.add(row(d, "Squat", 3, 5, ""));
        }
        return rows;
    }

    public void testStreakEndsTodayOrYesterday() {
        assertEquals(3, Stats.streak(dates("2026-10-07", "2026-10-06", "2026-10-05", "2026-10-03"), TODAY));
        assertEquals("Nothing today yet, streak ends yesterday", 2, Stats.streak(dates("2026-10-06", "2026-10-05", "2026-10-06"), TODAY));
        assertEquals("Last workout two days ago", 0, Stats.streak(dates("2026-10-05", "2026-10-04"), TODAY));
        assertEquals("Streak crosses a month boundary", 8, Stats.streak(dates("2026-10-07", "2026-10-06", "2026-10-05", "2026-10-04", "2026-10-03", "2026-10-02", "2026-10-01", "2026-09-30"), TODAY));
    }

    public void testSummaryTiles() {
        List<Stats.Row> rows = dates("2026-10-07", "2026-10-07", "2026-10-01", "2026-09-30");
        rows.add(row("2026-10-02", "Bench Press", 3, 5, "100"));
        Stats.Summary s = Stats.summary(rows, TODAY);
        assertEquals(5, s.total);
        assertEquals("Today and the six days before it", 4, s.lastSevenDays);
        assertEquals(1, s.streak);
        assertEquals(2, s.exercisesUsed);
    }

    public void testWeeklyCountsStartMonday() {
        // First of eight weeks starts Monday 2026-08-17; 2026-08-16 is the Sunday before it.
        List<Stats.Point> weeks = Stats.weeklyCounts(dates("2026-08-16", "2026-08-17", "2026-08-23", "2026-08-24", "2026-10-04", "2026-10-05", "2026-10-07"), TODAY, 8);
        assertEquals(8, weeks.size());
        assertEquals("Aug 17", weeks.get(0).label);
        assertEquals("Oct 5", weeks.get(7).label);
        assertEquals("Week of August 17, 2026", weeks.get(0).longLabel);
        double[] expected = {2, 1, 0, 0, 0, 0, 1, 2};
        for (int i = 0; i < 8; i++) {
            assertEquals("Week " + i, expected[i], weeks.get(i).value, 0);
        }
    }

    public void testVolumeAndTopExercises() {
        assertEquals(0, new BigDecimal("3000").compareTo(Stats.volume(row(TODAY, "Squat", 3, 10, "100"))));
        assertEquals(0, new BigDecimal("25").compareTo(Stats.volume(row(TODAY, "Curl", 2, 5, "2.5"))));
        assertEquals("Blank weight is zero volume", 0, Stats.volume(row(TODAY, "Push-up", 3, 20, "")).signum());
        List<Stats.Row> rows = Arrays.asList(
                row(TODAY, "Squat", 3, 10, "100"), row(TODAY, "Squat", 1, 10, "50"),
                row(TODAY, "Bench Press", 3, 10, "100"), row(TODAY, "Bench Press", 1, 1, ""),
                row(TODAY, "Deadlift", 1, 5, "200"), row(TODAY, "Abs", 3, 10, "100"),
                row(TODAY, "Curl", 2, 5, "2.5"), row(TODAY, "Row", 1, 1, "1"), row(TODAY, "Push-up", 3, 20, ""));
        List<Stats.Point> top = Stats.topVolume(rows, 5);
        assertEquals(5, top.size());
        assertEquals("Squat", top.get(0).label);
        assertEquals(3500, top.get(0).value, 0);
        assertEquals("Ties sort by name", "Abs", top.get(1).label);
        assertEquals("Bench Press", top.get(2).label);
        assertEquals("Deadlift", top.get(3).label);
        assertEquals("Curl", top.get(4).label);
        assertEquals("Zero volume is left out", 0, Stats.topVolume(Collections.singletonList(row(TODAY, "Push-up", 3, 20, "")), 5).size());
    }

    public void testRecordsHeaviestFirstWithFirstDate() {
        List<Stats.Row> rows = Arrays.asList(
                row("2026-09-01", "Bench Press", 3, 5, "135"), row("2026-09-10", "Bench Press", 3, 5, "145.5"),
                row("2026-09-20", "Bench Press", 3, 5, "145.50"), row("2026-09-02", "Squat", 3, 5, "185"),
                row("2026-09-03", "Push-up", 3, 20, ""), row("2026-09-04", "Curl", 3, 10, "0"));
        List<Stats.Record> records = Stats.records(rows);
        assertEquals("Blank-only exercises have no record", 3, records.size());
        assertEquals("Squat", records.get(0).exercise);
        assertEquals("Bench Press", records.get(1).exercise);
        assertEquals("145.5", Stats.format(records.get(1).weight));
        assertEquals("Earliest date the heaviest weight was reached", "2026-09-10", records.get(1).date);
        assertEquals("Zero is a recorded weight", "Curl", records.get(2).exercise);
    }

    public void testHeaviestPerDateSkipsBlankWeights() {
        List<Stats.Row> rows = Arrays.asList(
                row("2026-09-02", "Bench Press", 3, 5, "140"), row("2026-09-01", "Bench Press", 1, 10, "95"),
                row("2026-09-01", "Bench Press", 3, 5, "135"), row("2026-09-03", "Bench Press", 3, 5, ""),
                row("2026-09-03", "Squat", 3, 5, "200"));
        List<Stats.Point> points = Stats.heaviestPerDate(rows, "Bench Press");
        assertEquals(2, points.size());
        assertEquals("Sep 1", points.get(0).label);
        assertEquals(135, points.get(0).value, 0);
        assertEquals(140, points.get(1).value, 0);
    }

    public void testEmptyData() {
        List<Stats.Row> none = Collections.emptyList();
        Stats.Summary s = Stats.summary(none, TODAY);
        assertEquals(0, s.total);
        assertEquals(0, s.lastSevenDays);
        assertEquals(0, s.streak);
        assertEquals(0, s.exercisesUsed);
        List<Stats.Point> weeks = Stats.weeklyCounts(none, TODAY, 8);
        assertEquals(8, weeks.size());
        for (Stats.Point p : weeks) {
            assertEquals(0, p.value, 0);
        }
        assertTrue(Stats.topVolume(none, 5).isEmpty());
        assertTrue(Stats.records(none).isEmpty());
        assertTrue(Stats.heaviestPerDate(none, "Squat").isEmpty());
    }

    public void testFormattingAndChartHelpers() {
        assertEquals("0", Stats.format(new BigDecimal("0.00")));
        assertEquals("137.5", Stats.format(new BigDecimal("137.50")));
        assertEquals("12,500", Stats.format(12500));
        assertEquals("2026-10-05", Stats.date(Stats.weekStart(Stats.day("2026-10-11"))));
        assertEquals(200000, ChartView.niceStep(617045, 4), 0);
        assertEquals(20, ChartView.niceStep(45, 4), 0);
        assertEquals(2, ChartView.niceStep(7, 4), 0);
        assertEquals(1, ChartView.niceStep(0, 4), 0);
        assertEquals("617k", ChartView.compact(617045));
        assertEquals("1.2M", ChartView.compact(1200000));
        assertEquals("137.5", ChartView.compact(137.5));
        assertEquals("1024x768 image for a 168 px row decodes at 1/4", 4, Images.thumbnailSample(1024, 768, 168));
        assertEquals(1, Images.thumbnailSample(100, 100, 168));
    }
}
