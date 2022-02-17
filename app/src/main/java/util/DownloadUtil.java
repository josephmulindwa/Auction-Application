package util;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadUtil {
    private static final String TAG = "DownloadUtil";

    public boolean endDownloadCondition(){
        return false;
    }

    private void atomic_downloadBytes(String uri){
        byte[] out = null;
        boolean success = false;
        onStart();
        try {
            URL url = new URL(uri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if(endDownloadCondition()){ return; }
            int BUFFER_SIZE = 2048*2048;
            byte[] buffer = new byte[BUFFER_SIZE];

            InputStream in = connection.getInputStream();
            int reads;
            while((reads = in.read(buffer)) > 0){
                if(endDownloadCondition()){ return; }
                baos.write(buffer, 0, reads);
            }
            out = baos.toByteArray();
            success = true;
            in.close();
            baos.close();
            connection.disconnect();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        onFinish(out, success);
    }

    public synchronized void downloadBytes(String uri){
        HandlerThread downloadHandlerThread = new HandlerThread(TAG);
        downloadHandlerThread.start();
        Handler handler = new Handler(downloadHandlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() { atomic_downloadBytes(uri); }
        });
    }

    public void onFinish(byte[] bytes, boolean success){ }

    public void onStart(){ }

}
