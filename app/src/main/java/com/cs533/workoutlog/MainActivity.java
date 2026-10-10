package com.cs533.workoutlog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

public class MainActivity extends BaseActivity {
    private List<WorkoutDb.Workout> workouts;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_history);
        findViewById(R.id.add_workout).setOnClickListener(v->startActivity(new Intent(this,WorkoutFormActivity.class)));
        ListView list=view(R.id.history_list);
        list.setEmptyView(findViewById(R.id.empty));
        list.setOnItemClickListener((parent,v,position,id)->startActivity(new Intent(this,DetailActivity.class).putExtra("id",workouts.get(position).id)));
    }
    @Override protected void onResume() {
        super.onResume();
        try {
            workouts=db.workouts();
            ((TextView)findViewById(R.id.history_count)).setText(workouts.size()+ (workouts.size()==1?" workout recorded":" workouts recorded"));
            ((ListView)findViewById(R.id.history_list)).setAdapter(new BaseAdapter(){
                public int getCount(){return workouts.size();} public Object getItem(int p){return workouts.get(p);} public long getItemId(int p){return workouts.get(p).id;}
                public View getView(int p,View old,ViewGroup parent){
                    View row=old==null?LayoutInflater.from(MainActivity.this).inflate(R.layout.row_workout,parent,false):old;
                    WorkoutDb.Workout w=workouts.get(p);
                    ((TextView)row.findViewById(R.id.row_name)).setText(w.name);
                    ((TextView)row.findViewById(R.id.row_date)).setText(w.date);
                    ((TextView)row.findViewById(R.id.row_summary)).setText(w.sets+" sets  ·  "+w.reps+" reps"+(w.weight.isEmpty()?"":"  ·  "+w.weight+" lb"));
                    Images.show(MainActivity.this,(ImageView)row.findViewById(R.id.row_image),w.image,w.name);
                    return row;
                }
            });
        } catch(RuntimeException e){message("Could not load workout history. Please reopen the app.");}
    }
}
