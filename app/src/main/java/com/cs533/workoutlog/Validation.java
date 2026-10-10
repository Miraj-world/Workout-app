package com.cs533.workoutlog;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Validation {
    private Validation() {}
    public static String today() { return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()); }
    public static boolean validDate(String date) {
        try { SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);return date.matches("\\d{4}-\\d{2}-\\d{2}") && f.format(f.parse(date)).equals(date) && date.compareTo(today())<=0; }
        catch(Exception e) { return false; }
    }
    public static boolean integer(String text,int max) {
        try { return text.matches("[0-9]+") && Integer.parseInt(text)>=1 && Integer.parseInt(text)<=max; } catch(Exception e) { return false; }
    }
    public static boolean weight(String text) {
        text=text.trim();if(text.isEmpty())return true;
        try { return text.matches("[0-9]+(\\.[0-9]{1,2})?") && new BigDecimal(text).compareTo(new BigDecimal("2000"))<=0; }catch(Exception e){return false;}
    }
    public static String workout(long exercise,String date,String sets,String reps,String weight,String notes) {
        if(exercise<=0)return "Choose an exercise.";
        if(!validDate(date))return "Choose today or an earlier valid date.";
        if(!integer(sets,100))return "Enter sets from 1 to 100.";
        if(!integer(reps,1000))return "Enter repetitions from 1 to 1,000.";
        if(!weight(weight))return "Enter weight from 0 to 2,000 lb with up to two decimal places.";
        if(notes.length()>500)return "Keep notes within 500 characters.";
        return null;
    }
}
