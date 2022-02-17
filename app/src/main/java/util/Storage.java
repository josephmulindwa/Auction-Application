package util;

import java.util.ArrayList;

import model.MessageBin;
import model.PostableItem;
import model.Profile;

public class Storage {
    public static PostableItem.CRITERIA ITEM_SORT_CRITERIA = PostableItem.CRITERIA.TOPBID;
    public static volatile boolean profileLoaded = false;
    public static volatile boolean itemsLoading = false;
    public static volatile Profile profile = null;
    public static volatile PostableItem postableItem = null;
    public static volatile ArrayList<PostableItem> postableItems = new ArrayList<>();

    public static void reset(){
        profileLoaded = false;
        itemsLoading = false;
        profile = null;
        postableItem = null;
        postableItems.clear();
    }
}
