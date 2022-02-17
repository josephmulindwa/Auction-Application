package com.scit.stauc;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import database.RTDataFetcher;
import database.RTSingleValueChanger;
import model.Notification;
import util.AppUtils;
import util.FirebaseHandleUtil;
import util.Storage;

public class NotificationsFragment extends Fragment{
    private TextView nothingHereView;
    private RecyclerView notificationsRecycler;
    private ViewGroup container;
    private ProgressBar progressBar;
    private NotificationAdapter notificationAdapter;
    private final ArrayList<Notification> notifications = new ArrayList<>();

    public static NotificationsFragment newInstance(){
        return new NotificationsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_category_view, parent, false);
        nothingHereView = v.findViewById(R.id.nothing_here_view);
        notificationsRecycler = v.findViewById(R.id.category_recycler);
        notificationsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        container = v.findViewById(R.id.frame_container);
        progressBar = (ProgressBar)inflater.inflate(R.layout.progress_bar_layer, container, false);
        notificationAdapter = new NotificationAdapter();
        notificationsRecycler.setAdapter(notificationAdapter);
        return v;
    }

    @Override
    public void onResume(){
        super.onResume();
        NotificationFetcher notificationFetcher = new NotificationFetcher();
        notificationFetcher.fetch(notifications); // just fetch
    }

    private void updateView(){
        if(notifications.isEmpty()){
            nothingHereView.setVisibility(View.VISIBLE);
            notificationsRecycler.setVisibility(View.GONE);
        }else{
            nothingHereView.setVisibility(View.GONE);
            notificationsRecycler.setVisibility(View.VISIBLE);
            notificationAdapter.notifyDataSetChanged();
        }
    }

    private class NotificationFetcher extends RTDataFetcher<Notification>{
        public NotificationFetcher(){
            super(AppUtils.MODEL_NOTIFICATION, Notification.class);
        }

        @Override
        public void onStartFetch(){
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
            if(progressBar != null && container != null){
                container.removeView(progressBar);
            }
            updateView();
        }

        @Override
        public void onFail(){
            if(progressBar != null && container != null){
                container.removeView(progressBar);
            }
        }
    }

    private class NotificationDeleter extends RTSingleValueChanger<Notification>{
        private String notificationId;

        public NotificationDeleter(String notificationId){
            super(AppUtils.MODEL_NOTIFICATION, Notification.class);
            this.notificationId = notificationId;
        }

        @Override
        public void onStartChange(){
            if(progressBar != null && container != null){
                container.removeView(progressBar);
                container.addView(progressBar);
            }
        }

        @Override
        public void onFail(){
            if(progressBar != null && container != null){
                container.removeView(progressBar);
            }
        }

        @Override
        public void onFinish(){
            if(progressBar != null && container != null){
                container.removeView(progressBar);
            }
            for(Notification notification : notifications){
                if(notification.id.equals(notificationId)){
                    notifications.remove(notification);
                    break;
                }
            }
            updateView();
        }

        @Override
        public void onChange(DatabaseReference dRef){
            dRef.child(notificationId).removeValue();
        }

    }

    private class NotificationHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        private final TextView notificationTitleView;
        private final TextView notificationMessageView;
        private final TextView notificationDateView;
        private final Button notificationActionButton;
        //private final View cancelButton;
        private final int readColor = R.color.cool_gray_4;
        private final int unReadColor = R.color.black;
        private final int readTypeface = Typeface.NORMAL;
        private final int unReadTypeface = Typeface.BOLD;
        private Notification mNotification;

        public NotificationHolder(View view){
            super(view);
            notificationActionButton = itemView.findViewById(R.id.notification_action_view);
            notificationDateView = itemView.findViewById(R.id.notification_date_view);
            notificationMessageView = itemView.findViewById(R.id.notification_message_view);
            notificationTitleView = itemView.findViewById(R.id.notification_title_view);
            //cancelButton = itemView.findViewById(R.id.cancel_button);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view){
            if(mNotification == null){
                return;
            }

            mNotification.seen = true;
            FirebaseHandleUtil.NotificationUploader notificationUploader =
                    new FirebaseHandleUtil.NotificationUploader();
            notificationUploader.upload(mNotification);


            View popUp = LayoutInflater.from(getActivity()).inflate(
                    R.layout.notification_dialog_view, container, false);
            TextView titleTextView = popUp.findViewById(R.id.title_text_view);
            TextView contentTextView = popUp.findViewById(R.id.content_text_view);
            Button notificationActionButton = popUp.findViewById(R.id.notification_action_view);
            titleTextView.setText(mNotification.title);
            contentTextView.setText(mNotification.value);

            String key = mNotification.key;
            if(key != null){
                if(key.equals(Notification.KEY.BUY.toString())){
                    notificationActionButton.setText("Contact Owner");
                    notificationActionButton.setVisibility(View.VISIBLE);
                }else if(key.equals(Notification.KEY.CONTACT.toString())){
                    notificationActionButton.setText("Message Buyer");
                    notificationActionButton.setVisibility(View.VISIBLE);
                }else{
                    notificationActionButton.setVisibility(View.GONE);
                }
            }else{
                notificationActionButton.setVisibility(View.GONE);
            }

            notificationActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mNotification.key.equals(Notification.KEY.CONTACT.toString())){
                        MessageActivity.sendMessage(getActivity(), mNotification.target, null);
                    }else if(mNotification.key.equals(Notification.KEY.BUY.toString())){
                        MessageActivity.sendMessage(getActivity(), mNotification.target, getString(R.string.message_item_owner));
                    }
                }
            });

            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setView(popUp)
                    .create();
            alertDialog.show();
        }

        public boolean onLongClick(View view){
            if(mNotification == null){
                return false;
            }
            if(mNotification.id.startsWith(AppUtils.ADMIN_ID) ||
                    mNotification.id.startsWith(AppUtils.HELP_CENTER_ID)){
                return false;
            }
            View deletePopUp = LayoutInflater.from(getActivity()).inflate(
                    R.layout.delete_view, container, false);
            TextView contentView = deletePopUp.findViewById(R.id.content_text_view);
            contentView.setText(R.string.operation_undone);
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setView(deletePopUp)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            NotificationDeleter notificationDeleter = new NotificationDeleter(mNotification.id);
                            notificationDeleter.change();
                        }
                    })
                    .create();
            alertDialog.show();
            return true;
        }

        public void bindNotification(Notification notification){
            Date date = new Date(notification.startTimeStamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH-mm",
                    Locale.getDefault());
            String dateString = dateFormat.format(date);
            notificationDateView.setText(dateString);
            notificationMessageView.setText(notification.value);
            notificationTitleView.setText(notification.title);
            mNotification = notification;
            showAsReadSync(notification.seen);
        }

        private void showAsReadSync(boolean isread){
            TextView[] textViews = {notificationTitleView, notificationMessageView};
            for (TextView textView : textViews){
                textView.setTextColor(getResources().getColor(isread ?readColor : unReadColor));
                textView.setTypeface(textView.getTypeface(), isread ? readTypeface : unReadTypeface);
            }
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationHolder>{

        @NonNull
        @Override
        public NotificationHolder onCreateViewHolder(ViewGroup parent, int viewwType){
            View view = LayoutInflater.from(getActivity()).inflate(
                    R.layout.notification_item, parent, false);
            return new NotificationHolder(view);
        }

        @Override
        public void onBindViewHolder(NotificationHolder holder, int position){
            holder.bindNotification(notifications.get(position));
        }

        @Override
        public int getItemCount(){
            return notifications.size();
        }

    }

}
