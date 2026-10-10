package com.cs533.workoutlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public abstract class BaseActivity extends Activity {
    protected WorkoutDb db;
    @Override protected void onCreate(Bundle state) { super.onCreate(state);db=new WorkoutDb(this); }
    @Override protected void onDestroy() { if(db!=null)db.close();super.onDestroy(); }
    @SuppressWarnings("unchecked") protected <T extends android.view.View> T view(int id) { return (T)findViewById(id); }
    protected void header(String title) { ((TextView)findViewById(R.id.page_title)).setText(title); Button back=view(R.id.back);if(back!=null)back.setOnClickListener(v->onBackPressed()); }
    protected void message(String text) { Toast.makeText(this,text,Toast.LENGTH_LONG).show(); }
    protected void discard(boolean dirty,Runnable action) {
        if(!dirty)action.run();else new AlertDialog.Builder(this).setTitle("Discard unsaved changes?").setMessage("Your changes have not been saved.").setNegativeButton("Keep editing",null).setPositiveButton("Discard",(d,w)->action.run()).show();
    }
}
