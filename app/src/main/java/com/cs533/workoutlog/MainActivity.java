package com.cs533.workoutlog;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

/** Hosts the History and Progress tabs (FR-22) and the shared Add Workout action. */
public class MainActivity extends BaseActivity {
    static final int TAB_HISTORY = 0;
    static final int TAB_PROGRESS = 1;
    private static final String STATE_TAB = "tab";
    private static final String TAG_HISTORY = "history";
    private static final String TAG_PROGRESS = "progress";

    final Worker worker = new Worker();
    private HistoryFragment history;
    private ProgressFragment progress;
    private Button historyTab;
    private Button progressTab;
    private int tab;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_history);
        findViewById(R.id.add_workout).setOnClickListener(v -> startActivity(new Intent(this, WorkoutFormActivity.class)));
        historyTab = view(R.id.tab_history);
        progressTab = view(R.id.tab_progress);
        historyTab.setOnClickListener(v -> select(TAB_HISTORY, true));
        progressTab.setOnClickListener(v -> select(TAB_PROGRESS, true));

        FragmentManager fragments = getFragmentManager();
        // After rotation the FragmentManager restores both fragments itself; adding them again would duplicate them.
        if (state == null) {
            fragments.beginTransaction()
                    .add(R.id.tab_content, new HistoryFragment(), TAG_HISTORY)
                    .add(R.id.tab_content, new ProgressFragment(), TAG_PROGRESS)
                    .commit();
            fragments.executePendingTransactions();
        }
        history = (HistoryFragment) fragments.findFragmentByTag(TAG_HISTORY);
        progress = (ProgressFragment) fragments.findFragmentByTag(TAG_PROGRESS);
        select(state == null ? TAB_HISTORY : state.getInt(STATE_TAB, TAB_HISTORY), false);
    }

    int selectedTab() {
        return tab;
    }

    void select(int newTab, boolean announce) {
        tab = newTab;
        if (history == null || progress == null) {
            return;
        }
        boolean showHistory = newTab == TAB_HISTORY;
        getFragmentManager().beginTransaction()
                .show(showHistory ? history : progress)
                .hide(showHistory ? progress : history)
                .commit();
        historyTab.setSelected(showHistory);
        progressTab.setSelected(!showHistory);
        ((TextView) findViewById(R.id.page_title)).setText(showHistory ? R.string.history_title : R.string.progress_title);
        if (announce) {
            String name = getString(showHistory ? R.string.tab_history : R.string.tab_progress);
            findViewById(R.id.tab_bar).announceForAccessibility(getString(R.string.tab_selected_announcement, name));
        }
    }

    /** Called after sample data is loaded or all workouts are deleted (FR-24). */
    void workoutsChanged() {
        history.refresh();
        progress.refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt(STATE_TAB, tab);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        // BaseActivity closes the database before dispatching fragment destruction.
        // Stop background database work and remove its callbacks before that happens.
        worker.shutdown();
        super.onDestroy();
    }
}
