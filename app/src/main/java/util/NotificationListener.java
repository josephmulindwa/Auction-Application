package util;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;

import database.RTDataFetcher;
import model.Notification;

public class NotificationListener {
    private volatile boolean loading = false;
    private ViewGroup container;
    private ProgressBar progressBar;
    private NotificationFetcher notificationFetcher;
    private final ArrayList<Notification> notifications = new ArrayList<>();

    public NotificationListener(ViewGroup container, ProgressBar progressBar){
        this.container = container;
        this.progressBar = progressBar;
    }

    public void onFinishFetch(ArrayList<Notification> notifications){
        // updateView
    }

    public void stopListening(){
        if(notificationFetcher != null){
            notificationFetcher.stopListening();
        }
    }

    public void fetchNotifications(){
        if(loading){ return; }
        if(notificationFetcher != null) {
            notificationFetcher.stopListening();
        }
        notificationFetcher = new NotificationFetcher();
        notificationFetcher.listen(notifications);
    }

    public synchronized boolean isLoading(){
        return loading;
    }

    public void setContainer(ViewGroup container){
        this.container = container;
    }

    public synchronized void setProgressBar(ProgressBar progressBar){
        this.progressBar = progressBar;
    }

    private class NotificationFetcher extends RTDataFetcher<Notification> {
        public NotificationFetcher(){
            super(AppUtils.MODEL_NOTIFICATION, Notification.class);
        }

        @Override
        public void onStartFetch(){
            loading = true;
            notifications.clear();
            if(progressBar != null && container != null){
                container.removeView(progressBar);
                container.addView(progressBar);
            }
        }

        @Override
        public boolean validateCondition(Notification notification){
            /*if(notification.endTimeStamp <= 0){ // don't load expired notifications
                NotificationDeleter notificationDeleter = new NotificationDeleter(notification.id);
                notificationDeleter.change();
                return false;
            }*/
            if(notification.id.startsWith(AppUtils.ADMIN_ID) ||
                    notification.id.startsWith(AppUtils.HELP_CENTER_ID)){
                return true;
            }
            if(Storage.profile == null || notification.id.equals("null")){
                return false;
            }
            return notification.id.startsWith(Storage.profile.getId());
        }

        @Override
        public void onFinish(){
            loading = false;
            if(progressBar != null && container != null){
                container.removeView(progressBar);
            }
            onFinishFetch(notifications);
        }

        @Override
        public void onFail(){
            loading = false;
            if(progressBar != null && container != null){
                container.removeView(progressBar);
            }
        }
    }

}
