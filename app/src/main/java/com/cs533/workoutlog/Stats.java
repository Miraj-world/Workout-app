package com.cs533.workoutlog;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Progress statistics (FR-23). Pure Java with no Android imports: every method takes
 * plain rows and, where it matters, today's date, so results are repeatable in tests.
 * Dates are yyyy-MM-dd strings, as stored by {@link WorkoutDb}.
 */
public final class Stats {
    private Stats() {
    }

    /** One workout entry. A blank weight is stored as null. */
    public static final class Row {
        public final String date;
        public final String exercise;
        public final int sets;
        public final int reps;
        public final BigDecimal weight;

        public Row(String date, String exercise, int sets, int reps, String weight) {
            this.date = date;
            this.exercise = exercise;
            this.sets = sets;
            this.reps = reps;
            this.weight = weight == null || weight.trim().isEmpty() ? null : new BigDecimal(weight.trim());
        }
    }

    /** One chart value with its axis label and a longer label for screen readers. */
    public static final class Point {
        public final String label;
        public final String longLabel;
        public final double value;

        public Point(String label, String longLabel, double value) {
            this.label = label;
            this.longLabel = longLabel;
            this.value = value;
        }
    }

    /** Heaviest weight for one exercise and the first date it was lifted. */
    public static final class Record {
        public final String exercise;
        public final BigDecimal weight;
        public final String date;

        Record(String exercise, BigDecimal weight, String date) {
            this.exercise = exercise;
            this.weight = weight;
            this.date = date;
        }
    }

    public static final class Summary {
        public int total;
        public int lastSevenDays;
        public int streak;
        public int exercisesUsed;
    }

    public static Summary summary(List<Row> rows, String today) {
        Summary s = new Summary();
        s.total = rows.size();
        s.lastSevenDays = lastSevenDays(rows, today);
        s.streak = streak(rows, today);
        s.exercisesUsed = exercisesUsed(rows);
        return s;
    }

    /** Workouts dated today or in the six days before it. */
    public static int lastSevenDays(List<Row> rows, String today) {
        long end = day(today);
        int count = 0;
        for (Row r : rows) {
            long d = day(r.date);
            if (d <= end && d > end - 7) {
                count++;
            }
        }
        return count;
    }

    /**
     * Consecutive days with at least one workout, ending today, or ending yesterday
     * when nothing is logged yet today. Otherwise 0.
     */
    public static int streak(List<Row> rows, String today) {
        Set<Long> days = new HashSet<>();
        for (Row r : rows) {
            days.add(day(r.date));
        }
        long d = day(today);
        if (!days.contains(d)) {
            d--;
        }
        int streak = 0;
        while (days.contains(d)) {
            streak++;
            d--;
        }
        return streak;
    }

    public static int exercisesUsed(List<Row> rows) {
        Set<String> names = new HashSet<>();
        for (Row r : rows) {
            names.add(r.exercise);
        }
        return names.size();
    }

    /** Workout counts for the given number of weeks, oldest first, ending with the week containing today. Weeks start Monday. */
    public static List<Point> weeklyCounts(List<Row> rows, String today, int weeks) {
        long firstMonday = weekStart(day(today)) - 7L * (weeks - 1);
        int[] counts = new int[weeks];
        for (Row r : rows) {
            long d = day(r.date);
            if (d >= firstMonday && d <= day(today)) {
                counts[(int) ((d - firstMonday) / 7)]++;
            }
        }
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            long monday = firstMonday + 7L * i;
            points.add(new Point(shortDate(monday), "Week of " + longDate(monday), counts[i]));
        }
        return points;
    }

    /** The heaviest weight on each date for one exercise, oldest first. Blank weights are skipped. */
    public static List<Point> heaviestPerDate(List<Row> rows, String exercise) {
        TreeMap<String, BigDecimal> best = new TreeMap<>();
        for (Row r : rows) {
            if (r.weight == null || !r.exercise.equals(exercise)) {
                continue;
            }
            BigDecimal old = best.get(r.date);
            if (old == null || r.weight.compareTo(old) > 0) {
                best.put(r.date, r.weight);
            }
        }
        List<Point> points = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : best.entrySet()) {
            long d = day(e.getKey());
            points.add(new Point(shortDate(d), longDate(d), e.getValue().doubleValue()));
        }
        return points;
    }

    /** Sets × reps × weight per exercise. A blank weight counts as zero volume. */
    public static BigDecimal volume(Row r) {
        if (r.weight == null) {
            return BigDecimal.ZERO;
        }
        return r.weight.multiply(BigDecimal.valueOf((long) r.sets * r.reps));
    }

    /** Exercises with the largest total volume, largest first; exercises with zero volume are left out. Ties sort by name. */
    public static List<Point> topVolume(List<Row> rows, int limit) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Row r : rows) {
            BigDecimal old = totals.get(r.exercise);
            totals.put(r.exercise, old == null ? volume(r) : old.add(volume(r)));
        }
        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(totals.entrySet());
        Collections.sort(sorted, (a, b) -> {
            int byVolume = b.getValue().compareTo(a.getValue());
            return byVolume != 0 ? byVolume : a.getKey().compareToIgnoreCase(b.getKey());
        });
        List<Point> points = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : sorted) {
            if (points.size() == limit || e.getValue().signum() == 0) {
                break;
            }
            points.add(new Point(e.getKey(), e.getKey(), e.getValue().doubleValue()));
        }
        return points;
    }

    /** Heaviest weight per exercise with the earliest date it was reached, heaviest first. */
    public static List<Record> records(List<Row> rows) {
        Map<String, Record> best = new LinkedHashMap<>();
        for (Row r : rows) {
            if (r.weight == null) {
                continue;
            }
            Record old = best.get(r.exercise);
            int cmp = old == null ? 1 : r.weight.compareTo(old.weight);
            if (cmp > 0 || (cmp == 0 && r.date.compareTo(old.date) < 0)) {
                best.put(r.exercise, new Record(r.exercise, r.weight, r.date));
            }
        }
        List<Record> result = new ArrayList<>(best.values());
        Collections.sort(result, (a, b) -> {
            int byWeight = b.weight.compareTo(a.weight);
            return byWeight != 0 ? byWeight : a.exercise.compareToIgnoreCase(b.exercise);
        });
        return result;
    }

    /** Formats a weight or volume without trailing zeros, e.g. 135, 137.5, 12,500. */
    public static String format(BigDecimal value) {
        // Android's stripTrailingZeros() leaves zero as "0.0", so handle zero first.
        if (value.signum() == 0) {
            return "0";
        }
        BigDecimal v = value.stripTrailingZeros();
        if (v.scale() < 0) {
            v = v.setScale(0);
        }
        return String.format(Locale.US, v.scale() == 0 ? "%,d" : "%,." + v.scale() + "f", v.scale() == 0 ? v.longValueExact() : v);
    }

    public static String format(double value) {
        return format(BigDecimal.valueOf(value));
    }

    /** Day number since 1970-01-01, computed in UTC so daylight saving never shifts a date. */
    public static long day(String date) {
        try {
            return utc("yyyy-MM-dd").parse(date).getTime() / 86_400_000L;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Bad date: " + date, e);
        }
    }

    public static String date(long day) {
        return utc("yyyy-MM-dd").format(day * 86_400_000L);
    }

    /** The Monday on or before the given day. */
    public static long weekStart(long day) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        c.setTimeInMillis(day * 86_400_000L);
        int sinceMonday = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        return day - sinceMonday;
    }

    static String shortDate(long day) {
        return utc("MMM d").format(day * 86_400_000L);
    }

    static String longDate(long day) {
        return utc("MMMM d, yyyy").format(day * 86_400_000L);
    }

    private static SimpleDateFormat utc(String pattern) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        f.setLenient(false);
        return f;
    }
}
