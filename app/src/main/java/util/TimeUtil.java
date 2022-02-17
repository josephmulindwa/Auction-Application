package util;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
    private static final String TAG = "TimeUtil";
    public static volatile boolean timeSet = false;
    private static volatile long[] globalTimeDifference = {0, 0, 0, 0};
    private static final long secondsInMillis = 1000;
    private static final long minutesInMillis = secondsInMillis * 60;
    private static final long hoursInMillis = minutesInMillis * 60;
    private static final long daysInMillis = hoursInMillis * 24;

    // get semi-accurate time as combination of local device and
    public static long getCurrentTimeStamp(){
        return addTime(new Date(), globalTimeDifference).getTime();
    }

    // check websites for time online
    private static void atomic_getSiteTime(){
        String url = "https://time.is/Uganda";
        DownloadUtil timePageDl = new DownloadUtil(){
            @Override
            public void onStart(){
                timeSet = false;
                Log.i(TAG, "started time url fetch");
            }

            @Override
            public void onFinish(byte[] bytes, boolean success){
                if(success) {
                    String html = new String(bytes);
                    // Log.i(TAG, html);
                    String searchKeyStart = "_tD(", searchKeyEnd = ")";
                    int startIndex = html.indexOf(searchKeyStart);
                    while(startIndex != -1){
                        html = html.substring(startIndex + searchKeyStart.length());
                        if(!html.isEmpty()){
                            // if found char()
                            if(html.charAt(0) >= '0' && html.charAt(0) <= '9'){
                                String inTimeStampString = html.substring(0, html.indexOf(searchKeyEnd));
                                Log.i(TAG, "TimeStamp => " + inTimeStampString);
                                long inTimeStamp = Long.parseLong(inTimeStampString);
                                globalTimeDifference = getTimeDifference(inTimeStamp, new Date().getTime());
                                timeSet = true;
                                break;
                            }
                        }else {
                            break;
                        }
                        startIndex = html.indexOf(searchKeyStart);
                    }
                }
            }
        };

        timePageDl.downloadBytes(url);
    }

    public static void getSiteTime(){
        HandlerThread handlerThread = new HandlerThread("SiteTime");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                atomic_getSiteTime();
            }
        });
    }

    public static long[] getTimeDifference(long startTime, long endTime){
        long difference = endTime - startTime;

        long elapsedDays = difference / daysInMillis;
        if(elapsedDays != 0) {
            difference = difference / daysInMillis;
        }

        long elapsedHours = difference / hoursInMillis;
        difference = difference % hoursInMillis;

        long elapsedMinutes = difference / minutesInMillis;
        difference = difference % minutesInMillis;

        long elapsedSeconds = difference /secondsInMillis;

        long[] results = new long[4];
        results[0] = elapsedDays;
        results[1] = elapsedHours;
        results[2] = elapsedMinutes;
        results[3] = elapsedSeconds;
        //Log.i(TAG, "out:" + results[1] + ", " + results[2] +", " + results[3]);
        return  results;
    }

    public static Date addTime(Date date, long[] dhms){
        long days = dhms[0];
        long hours = dhms[1];
        long minutes = dhms[2];
        long seconds = dhms[3];

        Calendar calendar =  Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, (int)days);
        calendar.add(Calendar.HOUR_OF_DAY, (int)hours);
        calendar.add(Calendar.MINUTE, (int)minutes);
        calendar.add(Calendar.SECOND, (int)seconds);

        return calendar.getTime();
    }
}
