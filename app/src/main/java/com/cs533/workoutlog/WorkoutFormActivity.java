package com.cs533.workoutlog;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WorkoutFormActivity extends BaseActivity {
    private static final int ADD_EXERCISE=10;
    private long id,selected;
    private String date,baseline;
    private EditText sets,reps,weight,notes;
    private Spinner spinner;
    private Button dateButton;
    private List<WorkoutDb.Exercise> exercises;
    private boolean binding;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_workout_form);
        findViewById(R.id.page_title).setFocusableInTouchMode(true);
        if(state==null)findViewById(R.id.page_title).requestFocus();
        id=getIntent().getLongExtra("id",0);date=Validation.today();
        header(id==0?"Add Workout":"Edit Workout");
        sets=view(R.id.sets);reps=view(R.id.reps);weight=view(R.id.weight);notes=view(R.id.notes);spinner=view(R.id.exercise_spinner);dateButton=view(R.id.date_button);
        if(id>0) {
            WorkoutDb.Workout w=db.workout(id);
            if(w==null){message("This entry no longer exists.");finish();return;}
            selected=w.exerciseId;date=w.date;sets.setText(String.valueOf(w.sets));reps.setText(String.valueOf(w.reps));weight.setText(w.weight);notes.setText(w.notes);
        }
        if(state!=null){selected=state.getLong("selected");date=state.getString("date",date);sets.setText(state.getString("sets"));reps.setText(state.getString("reps"));weight.setText(state.getString("weight"));notes.setText(state.getString("notes"));}
        dateButton.setText(date);
        bindExercises();
        baseline=state==null?snapshot():state.getString("baseline",snapshot());
        dateButton.setOnClickListener(v->{
            String[] parts=date.split("-");
            DatePickerDialog picker=new DatePickerDialog(this,(p,y,m,d)->{date=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);dateButton.setText(date);},Integer.parseInt(parts[0]),Integer.parseInt(parts[1])-1,Integer.parseInt(parts[2]));
            picker.getDatePicker().setMaxDate(Calendar.getInstance().getTimeInMillis());picker.show();
        });
        findViewById(R.id.save_workout).setOnClickListener(v->save());
        findViewById(R.id.cancel).setOnClickListener(v->onBackPressed());
    }
    private void bindExercises() {
        binding=true;exercises=db.exercises();
        List<String> names=new ArrayList<>();names.add(getString(R.string.choose_exercise));
        int position=0;for(WorkoutDb.Exercise e:exercises){names.add(e.name);if(e.id==selected)position=names.size()-1;}names.add(getString(R.string.add_exercise));
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);spinner.setSelection(position);binding=false;
        updateImage();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View v,int p,long itemId){
                if(binding)return;
                if(p==exercises.size()+1){
                    int old=0;for(int i=0;i<exercises.size();i++)if(exercises.get(i).id==selected)old=i+1;
                    spinner.setSelection(old);startActivityForResult(new Intent(WorkoutFormActivity.this,ExerciseFormActivity.class),ADD_EXERCISE);
                }else{selected=p==0?0:exercises.get(p-1).id;updateImage();}
            }
        });
    }
    private void updateImage(){
        WorkoutDb.Exercise current=null;for(WorkoutDb.Exercise e:exercises)if(e.id==selected)current=e;
        Images.show(this,(ImageView)findViewById(R.id.exercise_image),current==null?null:current.image,current==null?null:current.name);
    }
    private String snapshot(){return selected+"|"+date+"|"+sets.getText()+"|"+reps.getText()+"|"+weight.getText()+"|"+notes.getText();}
    private void save(){
        sets.setError(null);reps.setError(null);weight.setError(null);notes.setError(null);
        String s=sets.getText().toString().trim(),r=reps.getText().toString().trim(),w=weight.getText().toString().trim(),n=notes.getText().toString();
        String error=Validation.workout(selected,date,s,r,w,n);
        TextView errorView=view(R.id.form_error);errorView.setText(error==null?"":error);errorView.setVisibility(error==null?View.GONE:View.VISIBLE);
        if(error!=null){
            if(!Validation.integer(s,100)){sets.setError("Use 1 to 100 sets");sets.requestFocus();}
            else if(!Validation.integer(r,1000)){reps.setError("Use 1 to 1,000 repetitions");reps.requestFocus();}
            else if(!Validation.weight(w)){weight.setError("Use 0 to 2,000 with up to two decimal places");weight.requestFocus();}
            else if(n.length()>500)notes.setError("Maximum 500 characters");
            message(error);return;
        }
        try{db.save(id,selected,date,Integer.parseInt(s),Integer.parseInt(r),w,n);message("Workout saved");finish();}
        catch(RuntimeException e){errorView.setText("Could not save. Your entries are still here; please try again.");errorView.setVisibility(View.VISIBLE);}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==ADD_EXERCISE&&result==RESULT_OK&&data!=null){selected=data.getLongExtra("exercise_id",selected);bindExercises();}}
    @Override protected void onSaveInstanceState(Bundle state){state.putLong("selected",selected);state.putString("date",date);state.putString("sets",sets.getText().toString());state.putString("reps",reps.getText().toString());state.putString("weight",weight.getText().toString());state.putString("notes",notes.getText().toString());state.putString("baseline",baseline);super.onSaveInstanceState(state);}
    @Override public void onBackPressed(){discard(!snapshot().equals(baseline),()->finish());}
}
