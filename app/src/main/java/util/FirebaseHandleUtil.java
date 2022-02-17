package util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.scit.stauc.HomeFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import database.FSStoreFetcher;
import database.FSStoreValueChanger;
import database.RTDataFetcher;
import database.RTSingleValueChanger;
import database.StorageHandler;
import model.Notification;
import model.PostableItem;
import model.TopBidHolder;

public class FirebaseHandleUtil {

    // try when
    public static void performBid(PostableItem item, int topBid){
        NotificationUploader notificationUploader = new NotificationUploader();
        if(topBid <= item.buyPrice){ // auction failed
            String id = item.owner + "_null_" + item.id + Notification.KEY.FAIL.toString();
            Notification failNotification = new Notification(id, Notification.KEY.FAIL,
                    item.id, "An auction was unsuccessful!",
                    "The auction for item '" + item.name + "' was unsuccessful. The auction failed " +
                            "to meet your reserve price target. You can delete this item " +
                            "from your Profile Page.");
            failNotification.startTimeStamp = new Date().getTime();
            failNotification.endTimeStamp = 1;
            notificationUploader.upload(failNotification);
            return;
        }
        if(Storage.profile == null || Storage.profile.getBids() == null){
            return;
        }
        HashMap<String, Integer> bids = Storage.profile.getBids();
        Integer myBid = bids.get(item.id);
        if(myBid == null){
            return;
        }
        if(myBid != topBid){
            // delete bid by association
            return;
        }

        if(item.miscKey != null){ return; } // someone already won bid in rt

        item.miscKey = Storage.profile.getId();
        int index = Storage.postableItems.indexOf(item);
        Storage.postableItems.set(index, item); // update locally

        // notification to contact owner in order to buy item:buyer_id
        String id = item.miscKey + "_" + item.owner + "_" + item.id + Notification.KEY.BUY.toString();
        Notification buyNotification = new Notification(id, Notification.KEY.BUY,
                item.id, "You have won an auction!",
                "The auction for item '" + item.name + "' has ended with you as the winner.");
        buyNotification.startTimeStamp = new Date().getTime();
        buyNotification.endTimeStamp = 1;

        notificationUploader.upload(buyNotification);

        // notification to contact buyer : owner_id
        id = item.owner + "_" + item.miscKey + "_" + "_" + item.id + Notification.KEY.CONTACT.toString();
        Notification contactBuyerNotification = new Notification(id, Notification.KEY.CONTACT,
                item.miscKey, "An auction ended successfully!",
                "The auction for an item you uploaded '"+item.name+ "' has ended successfully.");
        contactBuyerNotification.startTimeStamp = new Date().getTime();
        contactBuyerNotification.endTimeStamp = 1;
        notificationUploader.upload(contactBuyerNotification);

        FSStoreValueChanger<PostableItem> postableUploader = new FSStoreValueChanger<PostableItem>(
                AppUtils.MODEL_POSTABLE, PostableItem.class){
              @Override
              public void onSucceed(){
                  ;
              }
        };
        postableUploader.setMerge(item.id, item); // upload with misk key
    }

    public static boolean filter(PostableItem item){
        long[] elapseData = TimeUtil.getTimeDifference(TimeUtil.getCurrentTimeStamp(), item.endTimeStamp);
        if(elapseData[0] < 0 || elapseData[1] < 0 || elapseData[2] < 0 || elapseData[3] < 0){
            // don't show expired to guests
            if(Storage.profile == null){ return false; }
            if(item.owner.equals(Storage.profile.getId())){
                return true;
            }
            if(item.miscKey != null && item.miscKey.equals(Storage.profile.getId())){
                boolean isPriceValid = (item.topBid >= item.buyPrice) && (item.topBid > item.price);
                HashMap<String, Integer> myBids = Storage.profile.getBids();
                if(isPriceValid) {  // offline when auction ended
                    if (myBids == null || myBids.get(Storage.profile.getId()) == null) {
                        Storage.profile.addBid(item.id, item.topBid);
                        performBid(item, item.topBid);
                    }
                    return true;
                }
            }

            if(Storage.profile.getBids() != null){
                Integer myBid = Storage.profile.getBids().get(item.id);
                if(myBid == null){
                    return false;
                }else {
                    return myBid > item.topBid;
                }
            }
            return false;
        }
        return true;
    }

    public static class NotificationUploader extends RTSingleValueChanger<Notification>{
        private String id;
        private Notification notification;
        private boolean mustSend;

        public NotificationUploader(){
            super(AppUtils.MODEL_NOTIFICATION, Notification.class);
            mustSend = false;
        }

        public void setMustSend(boolean send){ mustSend = send; }

        @Override
        public void onChange(DatabaseReference dRef) {
            dRef.child(id).setValue(notification);
        }

        public void upload(Notification notification){
            this.notification = notification;
            final boolean[] notiFound = {false};

            RTDataFetcher<Notification> notiFetcher = new
                    RTDataFetcher<Notification>(AppUtils.MODEL_NOTIFICATION, Notification.class){
                @Override
                public boolean validateCondition(Notification inNotification){
                    if(inNotification == null){
                        return false;
                    }
                    return inNotification.id != null && notification.id.equals(notification.id);
                }

                @Override
                public void onFind(Notification inNotification){
                    notiFound[0] = true;
                }

                @Override
                public void onFinish(){
                    if(!notiFound[0]){
                        atomicUpload();
                    }
                }
            };

            if(mustSend){
                atomicUpload();
            }else{
                notiFetcher.fetch(null);
            }
        }

        private void atomicUpload(){
            if(notification != null){
                this.id = notification.id;
                change();
            }
        }
    }
}
