package com.cs533.workoutlog;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Workout History tab (FR-01, FR-02, FR-20, FR-22) with the sample data actions (FR-24). */
public class HistoryFragment extends Fragment {
    private List<WorkoutDb.Workout> workouts = new ArrayList<>();
    private Worker.Task task;
    private int thumbnailSize;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);
        thumbnailSize = getResources().getDimensionPixelSize(R.dimen.thumbnail_size);
        ListView list = (ListView) root.findViewById(R.id.history_list);
        list.setEmptyView(root.findViewById(R.id.empty));
        list.setOnItemClickListener((parent, v, position, id) -> startActivity(new Intent(getActivity(), DetailActivity.class).putExtra("id", workouts.get(position).id)));
        list.setAdapter(adapter);
        root.findViewById(R.id.load_sample).setOnClickListener(v -> confirmLoadSample());
        root.findViewById(R.id.delete_all).setOnClickListener(v -> confirmDeleteAll());
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroy() {
        if (task != null) {
            task.cancel();
        }
        super.onDestroy();
    }

    private MainActivity host() {
        return (MainActivity) getActivity();
    }

    void refresh() {
        View root = getView();
        if (root == null) {
            return;
        }
        try {
            workouts = host().db.workouts();
            ((TextView) root.findViewById(R.id.history_count)).setText(getResources().getQuantityString(R.plurals.workouts_recorded, workouts.size(), count(workouts.size())));
            root.findViewById(R.id.delete_all).setEnabled(!workouts.isEmpty() && task == null);
            adapter.notifyDataSetChanged();
        } catch (RuntimeException e) {
            message(getString(R.string.history_load_failed));
        }
    }

    private void confirmLoadSample() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.load_sample_title)
                .setMessage(R.string.load_sample_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.load_sample_confirm, (d, w) -> loadSample())
                .show();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.delete_all_title)
                .setMessage(R.string.delete_all_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_all_confirm, (d, w) -> deleteAll())
                .show();
    }

    private void loadSample() {
        WorkoutDb db = host().db;
        String today = Validation.today();
        run(t -> db.insertSampleData(SampleData.generate(db.exercises(), today, new Random()), t::isCancelled),
                count -> getString(R.string.sample_loaded, count(count)),
                getString(R.string.sample_failed));
    }

    private void deleteAll() {
        WorkoutDb db = host().db;
        run(t -> db.deleteAllWorkouts(),
                count -> getString(R.string.all_deleted, count(count)),
                getString(R.string.delete_all_failed));
    }

    private interface Text {
        String of(int count);
    }

    /** Runs a write on the worker thread with a progress indicator, then refreshes both tabs. */
    private void run(Worker.Job<Integer> job, Text success, String failure) {
        busy(true);
        task = host().worker.submit(job, (count, error) -> {
            task = null;
            busy(false);
            message(error == null ? success.of(count) : failure);
            host().workoutsChanged();
        });
    }

    private void busy(boolean busy) {
        View root = getView();
        if (root == null) {
            return;
        }
        root.findViewById(R.id.history_busy).setVisibility(busy ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.load_sample).setEnabled(!busy);
        root.findViewById(R.id.delete_all).setEnabled(!busy && !workouts.isEmpty());
    }

    private static String count(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    private void message(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
    }

    private final BaseAdapter adapter = new BaseAdapter() {
        public int getCount() {
            return workouts.size();
        }

        public Object getItem(int p) {
            return workouts.get(p);
        }

        public long getItemId(int p) {
            return workouts.get(p).id;
        }

        public View getView(int p, View old, ViewGroup parent) {
            View row = old == null ? LayoutInflater.from(getActivity()).inflate(R.layout.row_workout, parent, false) : old;
            WorkoutDb.Workout w = workouts.get(p);
            ((TextView) row.findViewById(R.id.row_name)).setText(w.name);
            ((TextView) row.findViewById(R.id.row_date)).setText(w.date);
            ((TextView) row.findViewById(R.id.row_summary)).setText(w.sets + " sets  ·  " + w.reps + " reps" + (w.weight.isEmpty() ? "" : "  ·  " + w.weight + " lb"));
            Images.thumbnail(getActivity(), (ImageView) row.findViewById(R.id.row_image), w.image, w.name, thumbnailSize);
            return row;
        }
    };
}
