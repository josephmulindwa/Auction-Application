package model;

import android.graphics.Bitmap;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import util.AppUtils;
import util.Storage;

public class PostableItem implements Comparable{

    public enum CATEGORY{ SCHOLASTICS, CLOTHES, FURNITURE, BEDDINGS, ELECTRONICS, MISCELLANEOUS };
    public enum CRITERIA{ NAME, BUYPRICE, PRICE, TOPBID, DATE_END, DATE_UPLOAD};
    public String id;
    public String name;
    public String description;
    public String miscKey = null;
    public int buyPrice = 0;
    public int price;
    public List<String> images;
    public int topBid;
    public String owner;
    public String category;
    public long startTimeStamp;
    public long endTimeStamp;
    public Bitmap bitmap;
    public HashMap<String, Integer> logs;

    public PostableItem(){

    }

    // compare used for topPicks

    @Override
    public int compareTo(Object o) {
        PostableItem obj1 = this;
        PostableItem obj2 = (PostableItem) o;
        CRITERIA criteria = Storage.ITEM_SORT_CRITERIA;
        if(criteria == CRITERIA.TOPBID) {
            return Integer.compare(obj1.topBid, obj2.topBid);
        }else if(criteria == CRITERIA.PRICE){
            return Integer.compare(obj1.price, obj2.price);
        }else if(criteria == CRITERIA.BUYPRICE){
            return Integer.compare(obj1.buyPrice, obj2.buyPrice);
        }else if(criteria == CRITERIA.DATE_END){
            if(obj1.endTimeStamp == obj2.endTimeStamp){ return 0; }
            Date date1 = new Date(obj1.endTimeStamp);
            Date date2 = new Date(obj2.endTimeStamp);
            if(date1.after(date2)){ return 1; }
            else{ return -1; }
        }else if(criteria == CRITERIA.DATE_UPLOAD){
            if(obj1.startTimeStamp == obj2.startTimeStamp){ return 0; }
            Date date1 = new Date();
            date1.setTime(startTimeStamp);
            Date date2 = new Date();
            date2.setTime(startTimeStamp);
            if(date1.after(date2)){ return 1; }
            else{ return -1; }
        }
        return obj1.name.toLowerCase().compareTo(obj2.name.toLowerCase());
    }

    public PostableItem(String name, String description, int price, CATEGORY category){
        this.name = name;
        this.description = description;
        this.price = price;
        this.topBid = price;
        this.category = category.toString();
        images = new ArrayList<>();
        logs = new HashMap<>();
        bitmap = null;
        String nameHash = null;
        try{
            nameHash = AppUtils.getSHA1Hash(name);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        SimpleDateFormat dateFormatter = (SimpleDateFormat)SimpleDateFormat.getDateTimeInstance();
        String timeKey = dateFormatter.format(new Date());
        timeKey = timeKey.replace(':', '_');
        timeKey = timeKey.replace(",", "");
        timeKey = timeKey.replace(" ", "");
        String salt = String.format(Locale.getDefault(), "%d", new Random().nextInt(1000000));
        this.id = timeKey + "_" + nameHash + "_" + salt;
    }

    public void addLog(String id, Integer integer){
        if(logs == null){
            logs = new HashMap<>();
        }
        logs.put(id, integer);
    }

}
