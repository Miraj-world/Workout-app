package com.cs533.workoutlog;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Progress tab (FR-23): summary tiles, three charts and personal records. Database
 * reads and {@link Stats} aggregation run on the activity's {@link Worker} thread.
 */
public class ProgressFragment extends Fragment {
    private static final String STATE_EXERCISE = "exercise";
    private static final int WEEKS = 8;
    private static final int TOP_EXERCISES = 5;

    /** Everything the tab shows, computed off the UI thread. */
    private static final class Result {
        List<Stats.Row> rows;
        List<String> exercises;
        String exercise;
        Stats.Summary summary;
        List<Stats.Point> weights;
        List<Stats.Point> weekly;
        List<Stats.Point> volume;
        List<Stats.Record> records;
    }

    private Worker.Task task;
    private Worker.Task chartTask;
    private List<Stats.Row> rows = Collections.emptyList();
    private List<String> exercises = new ArrayList<>();
    private String exercise;
    private Spinner spinner;
    private ChartView weightChart;
    private ChartView weeklyChart;
    private ChartView volumeChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_progress, container, false);
        exercise = state == null ? null : state.getString(STATE_EXERCISE);
        spinner = (Spinner) root.findViewById(R.id.progress_exercise);
        weightChart = (ChartView) root.findViewById(R.id.chart_weight);
        weeklyChart = (ChartView) root.findViewById(R.id.chart_weekly);
        volumeChart = (ChartView) root.findViewById(R.id.chart_volume);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) {
            }

            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String name = exercises.get(position);
                if (!name.equals(exercise)) {
                    exercise = name;
                    loadWeightChart();
                }
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(STATE_EXERCISE, exercise);
    }

    @Override
    public void onDestroy() {
        cancel(task);
        cancel(chartTask);
        super.onDestroy();
    }

    private static void cancel(Worker.Task t) {
        if (t != null) {
            t.cancel();
        }
    }

    private MainActivity host() {
        return (MainActivity) getActivity();
    }

    /** Reloads everything. Called from onResume and whenever workouts change. */
    void refresh() {
        if (getView() == null) {
            return;
        }
        cancel(task);
        cancel(chartTask);
        loading(true, true, getString(R.string.progress_loading));
        WorkoutDb db = host().db;
        String today = Validation.today();
        String wanted = exercise;
        task = host().worker.submit(t -> {
            Result r = new Result();
            r.rows = Collections.unmodifiableList(db.statRows());
            r.exercises = new ArrayList<>();
            for (WorkoutDb.Exercise e : db.weightedExercises()) {
                r.exercises.add(e.name);
            }
            r.exercise = r.exercises.contains(wanted) ? wanted : (r.exercises.isEmpty() ? null : r.exercises.get(0));
            r.summary = Stats.summary(r.rows, today);
            r.weights = r.exercise == null ? Collections.<Stats.Point>emptyList() : Stats.heaviestPerDate(r.rows, r.exercise);
            r.weekly = Stats.weeklyCounts(r.rows, today, WEEKS);
            r.volume = Stats.topVolume(r.rows, TOP_EXERCISES);
            r.records = Stats.records(r.rows);
            return r;
        }, (r, error) -> {
            task = null;
            if (error != null) {
                loading(true, false, getString(R.string.progress_load_failed));
                return;
            }
            loading(false, false, "");
            show(r);
        });
    }

    private void loading(boolean visible, boolean spinning, String text) {
        View root = getView();
        root.findViewById(R.id.progress_loading).setVisibility(visible ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.progress_spinner).setVisibility(spinning ? View.VISIBLE : View.GONE);
        ((TextView) root.findViewById(R.id.progress_status)).setText(text);
    }

    private void show(Result r) {
        rows = r.rows;
        exercises = r.exercises;
        exercise = r.exercise;
        View root = getView();
        tile(root, R.id.tile_total, R.id.stat_total, R.string.stat_total, r.summary.total);
        tile(root, R.id.tile_week, R.id.stat_week, R.string.stat_last_seven, r.summary.lastSevenDays);
        tile(root, R.id.tile_streak, R.id.stat_streak, R.string.stat_streak, r.summary.streak);
        tile(root, R.id.tile_exercises, R.id.stat_exercises, R.string.stat_exercises, r.summary.exercisesUsed);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, exercises);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setVisibility(exercises.isEmpty() ? View.GONE : View.VISIBLE);
        if (exercise != null) {
            spinner.setSelection(exercises.indexOf(exercise), false);
        }
        showWeightChart(exercise, r.weights);

        weeklyChart.setData(ChartView.BARS, r.weekly, 1, getString(R.string.chart_weekly_empty),
                getString(R.string.chart_weekly_description, describe(r.weekly, "")));
        volumeChart.setData(ChartView.HORIZONTAL_BARS, r.volume, 1, getString(R.string.chart_volume_empty),
                getString(R.string.chart_volume_description, describe(r.volume, " lb")));
        showRecords(root, r.records);
    }

    private void tile(View root, int tileId, int valueId, int labelId, int value) {
        String text = String.format(Locale.US, "%,d", value);
        ((TextView) root.findViewById(valueId)).setText(text);
        root.findViewById(tileId).setContentDescription(getString(R.string.stat_tile_description, getString(labelId), text));
    }

    private void loadWeightChart() {
        cancel(chartTask);
        List<Stats.Row> snapshot = rows;
        String name = exercise;
        chartTask = host().worker.submit(t -> Stats.heaviestPerDate(snapshot, name), (points, error) -> {
            chartTask = null;
            showWeightChart(name, error == null ? points : Collections.<Stats.Point>emptyList());
        });
    }

    private void showWeightChart(String name, List<Stats.Point> points) {
        String empty = getString(exercises.isEmpty() ? R.string.chart_weight_no_exercises : R.string.chart_weight_empty);
        String description = empty;
        if (points.size() >= 2) {
            Stats.Point first = points.get(0);
            Stats.Point last = points.get(points.size() - 1);
            double max = 0;
            for (Stats.Point p : points) {
                max = Math.max(max, p.value);
            }
            description = getString(R.string.chart_weight_description, name, points.size(),
                    Stats.format(first.value), first.longLabel, Stats.format(last.value), last.longLabel, Stats.format(max));
        }
        weightChart.setData(ChartView.LINE, points, 2, empty, description);
    }

    private String describe(List<Stats.Point> points, String unit) {
        StringBuilder text = new StringBuilder();
        for (Stats.Point p : points) {
            if (text.length() > 0) {
                text.append("; ");
            }
            text.append(getString(R.string.chart_point_description, p.longLabel, Stats.format(p.value) + unit));
        }
        return text.toString();
    }

    private void showRecords(View root, List<Stats.Record> records) {
        LinearLayout list = (LinearLayout) root.findViewById(R.id.records_list);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        for (Stats.Record record : records) {
            View row = inflater.inflate(R.layout.row_record, list, false);
            String weight = Stats.format(record.weight);
            String date = Stats.longDate(Stats.day(record.date));
            ((TextView) row.findViewById(R.id.record_name)).setText(record.exercise);
            ((TextView) row.findViewById(R.id.record_date)).setText(date);
            ((TextView) row.findViewById(R.id.record_weight)).setText(getString(R.string.record_value, weight));
            row.setContentDescription(getString(R.string.record_description, record.exercise, weight, date));
            list.addView(row);
        }
        list.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.records_empty).setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
