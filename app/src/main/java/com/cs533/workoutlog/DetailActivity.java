package com.cs533.workoutlog;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class DetailActivity extends BaseActivity {
    private long id;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_detail);header("Workout Details");id=getIntent().getLongExtra("id",0);
        findViewById(R.id.edit).setOnClickListener(v->startActivity(new Intent(this,WorkoutFormActivity.class).putExtra("id",id)));
        findViewById(R.id.delete).setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete workout?").setMessage("This workout entry will be removed. The exercise stays in your dropdown.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{
            try{db.delete(id);message("Workout deleted");finish();}catch(RuntimeException e){message("Could not delete. Please try again.");}
        }).show());
    }
    @Override protected void onResume(){
        super.onResume();WorkoutDb.Workout w=db.workout(id);if(w==null){finish();return;}
        ((TextView)findViewById(R.id.detail_name)).setText(w.name);
        ((TextView)findViewById(R.id.detail_date)).setText(w.date);
        ((TextView)findViewById(R.id.detail_sets)).setText(String.valueOf(w.sets));
        ((TextView)findViewById(R.id.detail_reps)).setText(String.valueOf(w.reps));
        ((TextView)findViewById(R.id.detail_weight)).setText(w.weight.isEmpty()?"Not entered":w.weight+" lb");
        ((TextView)findViewById(R.id.detail_notes)).setText(w.notes.isEmpty()?"Not entered":w.notes);
        Images.show(this,(ImageView)findViewById(R.id.exercise_image),w.image,w.name);
    }
}
