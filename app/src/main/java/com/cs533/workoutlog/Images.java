package com.cs533.workoutlog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.UUID;

public final class Images {
    private Images() {}
    public static void show(Context context,ImageView view,String ref,String name) {
        view.setImageResource(R.drawable.exercise_default);
        view.setContentDescription(name==null?"Default exercise icon":name+" exercise illustration");
        try {
            if(ref==null || ref.isEmpty())return;
            if(ref.startsWith("res:")) { int id=context.getResources().getIdentifier(ref.substring(4),"drawable",context.getPackageName());if(id!=0)view.setImageResource(id); }
            else { BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(ref,options);options.inSampleSize=sample(options.outWidth,options.outHeight);options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeFile(ref,options);if(bitmap!=null)view.setImageBitmap(bitmap); }
        } catch(RuntimeException ignored) { view.setImageResource(R.drawable.exercise_default); }
    }
    private static int sample(int w,int h) { int n=1;while(w/n>1024 || h/n>1024)n*=2;return n; }
    /** Decode bounded pixels, then save our own image independently of the source URI. */
    public static String importImage(Context context,Uri uri) throws IOException {
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();BitmapFactory.decodeStream(in,null,o);}
        if(o.outWidth<=0 || o.outHeight<=0)throw new IOException("Unreadable image");
        o.inSampleSize=sample(o.outWidth,o.outHeight);o.inJustDecodeBounds=false;
        Bitmap bitmap;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();bitmap=BitmapFactory.decodeStream(in,null,o);}
        if(bitmap==null)throw new IOException("Unreadable image");
        File folder=new File(context.getFilesDir(),"exercise-images");if(!folder.exists()&&!folder.mkdirs()){bitmap.recycle();throw new IOException();}
        File file=new File(folder,UUID.randomUUID()+".jpg");
        try(FileOutputStream out=new FileOutputStream(file)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,88,out))throw new IOException();}
        catch(IOException e){file.delete();throw e;}finally{bitmap.recycle();}
        return file.getAbsolutePath();
    }
    public static void removeDraft(String path) { if(path!=null&&!path.startsWith("res:"))new File(path).delete(); }
}
