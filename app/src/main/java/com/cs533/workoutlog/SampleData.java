package com.cs533.workoutlog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Builds realistic demonstration workouts (FR-24): about four sessions a week over
 * the last six months, warm-up entries ramping to a working weight, slow weight
 * progression with a lighter deload week every few weeks, and bodyweight push-ups
 * with a blank weight. No database or Android code here; {@link WorkoutDb} inserts the result.
 */
final class SampleData {
    static final int TARGET = 1000;
    static final int DAYS = 182;
    /** Chance of training on Monday through Sunday. */
    private static final double[] TRAINING_DAY = {0.8, 0.45, 0.8, 0.45, 0.75, 0.55, 0.2};
    private static final String[] NOTES = {"Felt strong", "Good form today", "Tired, kept it steady", "New personal best!", "Short session", "Focused on tempo"};

    private SampleData() {
    }

    static final class Entry {
        final long exerciseId;
        final String date;
        final int sets;
        final int reps;
        final String weight;
        final String notes;

        Entry(long exerciseId, String date, int sets, int reps, String weight, String notes) {
            this.exerciseId = exerciseId;
            this.date = date;
            this.sets = sets;
            this.reps = reps;
            this.weight = weight;
            this.notes = notes;
        }
    }

    /** Starting working weight, gain over six months, plate step and working reps; start 0 means bodyweight. */
    private static final class Profile {
        final double start;
        final double gain;
        final double step;
        final int reps;

        Profile(double start, double gain, double step, int reps) {
            this.start = start;
            this.gain = gain;
            this.step = step;
            this.reps = reps;
        }
    }

    private static Profile profile(String name) {
        switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "bench press":
                return new Profile(135, 30, 5, 5);
            case "squat":
                return new Profile(185, 45, 5, 5);
            case "deadlift":
                return new Profile(225, 50, 5, 5);
            case "shoulder press":
                return new Profile(75, 15, 2.5, 6);
            case "push-up":
                return new Profile(0, 0, 0, 15);
            default:
                return new Profile(40, 10, 2.5, 10);
        }
    }

    /** Exactly {@link #TARGET} entries dated from {@link #DAYS} days ago through today, oldest first. */
    static List<Entry> generate(List<WorkoutDb.Exercise> exercises, String today, Random random) {
        if (exercises.isEmpty()) {
            throw new IllegalArgumentException("No exercises");
        }
        long last = Stats.day(today);
        long first = last - DAYS + 1;
        List<Long> sessions = new ArrayList<>();
        for (long d = first; d <= last; d++) {
            int weekday = (int) (d - Stats.weekStart(d));
            // Always train today so the demo shows a current streak.
            if (d == last || random.nextDouble() < TRAINING_DAY[weekday]) {
                sessions.add(d);
            }
        }
        List<Entry> entries = new ArrayList<>();
        int rotation = 0;
        for (int s = 0; s < sessions.size(); s++) {
            long d = sessions.get(s);
            double progress = (double) (d - first) / (DAYS - 1);
            // Spread the target evenly across sessions so the total is exact.
            int n = (int) ((long) TARGET * (s + 1) / sessions.size() - (long) TARGET * s / sessions.size());
            int count = Math.min(exercises.size(), Math.max(1, (n + 2) / 3));
            // Every fifth block of four sessions (about one week in five) is a lighter deload week.
            boolean deload = (s / 4) % 5 == 4;
            for (int i = 0; i < count; i++) {
                WorkoutDb.Exercise exercise = exercises.get((rotation + i) % exercises.size());
                int m = n / count + (i < n % count ? 1 : 0);
                addExercise(entries, exercise, Stats.date(d), m, progress, deload, random);
            }
            rotation += count;
        }
        return entries;
    }

    private static void addExercise(List<Entry> out, WorkoutDb.Exercise exercise, String date, int m, double progress, boolean deload, Random random) {
        Profile p = profile(exercise.name);
        double working = (p.start + p.gain * progress) * (1 + (random.nextDouble() - 0.5) * 0.04) * (deload ? 0.9 : 1);
        for (int j = 0; j < m; j++) {
            boolean warmUp = j < m - 1;
            String note = !warmUp && random.nextDouble() < 0.06 ? NOTES[random.nextInt(NOTES.length)] : "";
            if (p.start == 0) {
                int reps = (int) Math.round(p.reps + 10 * progress + random.nextGaussian() * 2) - (warmUp ? 4 : 0);
                out.add(new Entry(exercise.id, date, warmUp ? 1 : 3, Math.max(5, reps), "", note));
                continue;
            }
            double fraction = m == 1 ? 1 : 0.55 + 0.45 * j / (m - 1);
            double weight = Math.round(working * fraction / p.step) * p.step;
            int sets = warmUp ? 1 + random.nextInt(2) : 3 + random.nextInt(3);
            int reps = warmUp ? p.reps + 2 * (m - 1 - j) : Math.max(1, p.reps + random.nextInt(3) - 1);
            out.add(new Entry(exercise.id, date, sets, reps, weight(weight), note));
        }
    }

    private static String weight(double value) {
        double clamped = Math.max(0, Math.min(2000, value));
        return BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
