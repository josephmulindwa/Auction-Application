package util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import model.Profile;


public class AppUtils {
    public static final String MODEL_POSTABLE = "Postable";
    public static final String MODEL_PROFILE = "Profile";
    public static final String MODEL_NOTIFICATION = "Notification";
    public static final String MODEL_MESSAGE = "Message";
    public static final String MODEL_FAQ = "FAQ";
    public static final String ADMIN_ID = "admin490@admin@com";
    public static final String HELP_CENTER_ID = "helpcenter@gmail@com";
    public static final int MIN_ACCEPTED_CURRENCY_VALUE = 100;
    public static final int MAX_ACCEPTED_CURRENCY_VALUE = 10000000;
    public static final int MIN_ACCEPTED_CURRENCY_END = 100;
    public static final int MIN_ITEM_NAME_LENGTH = 3;
    public static final int MIN_QUERY_MATCH_THRESHOLD = 1;
    public static final int MIN_BID_DURATION = 1;
    public static final int MAX_BID_DURATION = 14;
    public static final int MIN_UPLOAD_PHOTOS = 1;
    public static final int MAX_UPLOAD_PHOTOS = 4;
    public static final int MAX_CACHABLE_HISTORY = 15;
    public static final int MAX_FETCHABLE_ITEMS = -1;
    private static final double K = 1000;
    private static final double[] divKeys = {K*K*K*K, K*K*K, K*K, K};
    private static final char[] divValues = {'T', 'B', 'M', 'K'};

    public interface ToolbarChanger{
        void setTitleText(String title);
        void setHeaderImage(Bitmap bitmap);
        void setHeaderName(String name);
        void setHeaderEmail(String email);
    }

    public static String getSHA1Hash(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.reset();
        byte[] buffer = input.getBytes("UTF_8");
        md.update(buffer);
        byte[] digest = md.digest();
        StringBuilder hexStr = new StringBuilder();
        for (byte b : digest) {
            hexStr.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return hexStr.toString();
    }

    public static boolean isAlphanumeric(String s, boolean withNumbers){
        return true;
    }

    public static boolean isValidEmailString(String s){
        if(s.length() < 7){
            return false;
        }
        if(!s.contains("@")){
            return false;
        }
        if(!s.contains(".")){
            return false;
        }
        int indexAt = s.indexOf('@');
        int indexPoint = s.indexOf('.');
        return (indexPoint+1) > indexAt;
    }

    public static void hideKeyboard(Context context, View view){
        if(context == null || view == null){
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static String capitalize(String s){
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public static boolean isBetween(Date date, Date min, Date max){
        return !date.before(min) && !date.after(max);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private View showPopUp(Context context, int resId, View token){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(resId, null);
        final PopupWindow popupWindow = new PopupWindow(context);
        popupWindow.setContentView(popupView);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(token, Gravity.CENTER, 0, 0);
        return popupView;
    }

    public static boolean isValidAmount(int amount){
        if(amount < MIN_ACCEPTED_CURRENCY_VALUE){
            return false;
        }
        int times = amount / MIN_ACCEPTED_CURRENCY_END;
        int presumed = times * MIN_ACCEPTED_CURRENCY_END;
        return (amount == presumed);
    }

    public static DisplayMetrics getDeviceDisplayMetrics(Context context){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }

    public static String getFormattedNumber(long n, int precision){
        double r;
        double precisionValuator = Math.pow(10, precision);
        String ret = Long.toString(n);
        for(int i=0; i<divKeys.length;i++){
            r = (double) n / divKeys[i];
            if((long)r != 0){
                double rounded = Math.round(r * precisionValuator) / precisionValuator;
                // DecimalFormat decimalFormat = new DecimalFormat("0.#");
                if(rounded - (long)rounded == 0){
                    ret = Long.toString((long)r) + divValues[i];
                }else {
                    ret = Double.toString(rounded) + divValues[i];
                }
                break;
            }
        }
        return ret;
    }

    public static double getDoubleFromFormatted(String formatted){
        int indx = -1;
        for(int i=0; i<divKeys.length;i++){
            if(formatted.contains( String.valueOf(divValues[i]) )){
                indx = i;
                formatted = formatted.replace(String.valueOf(divValues[i]), "");
                break;
            }
        }
        double out = Double.parseDouble(formatted);
        if(indx != -1){
            out *= divKeys[indx];
        }
        return out;
    }

    private static Bitmap getScaledBitmap(Bitmap bm, int newWidth, int newHeight){
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public static Bitmap getScaledBitmap(InputStream stream, int newWidth, int newHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream);

        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;
        int inSampleSize = 1;
        if(srcHeight > newHeight || srcWidth > newWidth){
            if(srcWidth > srcHeight){
                inSampleSize = Math.round(srcHeight / newHeight);
            }else{
                inSampleSize = Math.round(srcWidth / newWidth);
            }
        }
        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeStream(stream, null, options);
    }

    public static Bitmap getScaledBitmap(InputStream stream, Activity activity){
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);
        return getScaledBitmap(stream, size.x, size.y);
    }

    public static Bitmap getCroppedBitmap(Bitmap bitmap, int newHeight, int newWidth){
        return ThumbnailUtils.extractThumbnail(bitmap, newWidth, newHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
    }

    public static byte[] getByteArrayForBitmap(Bitmap bitmap){
        /*int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();*/
        if(bitmap == null){ return null; }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }
}