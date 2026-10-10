package com.cs533.workoutlog;

import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;

public class ExerciseFormActivity extends BaseActivity {
    private static final int PICK_IMAGE=20;
    private EditText name;
    private String image;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_exercise_form);header("Add New Exercise");name=view(R.id.exercise_name);
        findViewById(R.id.page_title).setFocusableInTouchMode(true);
        if(state==null)findViewById(R.id.page_title).requestFocus();
        if(state!=null){image=state.getString("image");name.setText(state.getString("name"));}
        preview();
        findViewById(R.id.choose_image).setOnClickListener(v->{
            Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
            try{startActivityForResult(pick,PICK_IMAGE);}catch(RuntimeException e){message("No image picker is available. You can save without an image.");}
        });
        findViewById(R.id.remove_image).setOnClickListener(v->{Images.removeDraft(image);image=null;preview();});
        findViewById(R.id.cancel).setOnClickListener(v->onBackPressed());
        findViewById(R.id.save_exercise).setOnClickListener(v->save());
    }
    private void preview(){Images.show(this,(ImageView)findViewById(R.id.exercise_image),image,null);}
    private void save(){
        try{
            long id=db.addExercise(name.getText().toString(),image);
            setResult(RESULT_OK,new Intent().putExtra("exercise_id",id));image=null;message("Exercise added");finish();
        }catch(SQLiteConstraintException e){name.setError("This exercise name already exists.");name.requestFocus();}
        catch(IllegalArgumentException e){name.setError(e.getMessage());name.requestFocus();}
        catch(RuntimeException e){message("Could not save the exercise. Please try again.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_IMAGE&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            try{String copied=Images.importImage(this,data.getData());Images.removeDraft(image);image=copied;preview();}
            catch(Exception e){message("Could not read or copy that image. Choose another or save without an image.");}
        }
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putString("image",image);state.putString("name",name.getText().toString());super.onSaveInstanceState(state);}
    @Override public void onBackPressed(){discard(!name.getText().toString().isEmpty()||image!=null,()->{Images.removeDraft(image);image=null;finish();});}
}
