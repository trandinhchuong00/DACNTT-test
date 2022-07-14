package com.oam.utils;

import org.springframework.stereotype.Component;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Component
public class DateTimeHelper {

    public static String getCurrentDateTime(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.UK);
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }
    public static String getDayOfMonth(String format,String day){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format, Locale.UK);
        int dayNum;
        switch (day.toLowerCase()){
            case "today":
                dayNum = 0;
                break;
            case "tomorrow":
                dayNum = 1;
                break;
            case "overmorrow":
                dayNum = 2;
                break;
            default:
                dayNum = 0;
        }
        LocalDateTime now = LocalDateTime.now().plusDays(dayNum);
        return dtf.format(now);
    }

    public static String getCurrentDayOfWeek(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE", Locale.UK);
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }
    public static String plusMinutes(String time, String minutes){
        String newTime = "";
        try{
            SimpleDateFormat df = new SimpleDateFormat("HH:mm");
            Date d = df.parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.add(Calendar.MINUTE, Integer.parseInt(minutes));
            newTime = df.format(cal.getTime());
        }
        catch (ParseException parseException){
            parseException.printStackTrace();
        }
        return newTime;
    }
}
