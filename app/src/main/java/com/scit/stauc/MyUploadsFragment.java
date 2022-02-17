package com.scit.stauc;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import database.FSStoreFetcher;
import database.FSStoreValueChanger;
import database.RTDataFetcher;
import database.RTSingleValueChanger;
import database.StorageHandler;
import model.Notification;
import model.PostableItem;
import model.Profile;
import model.TopBidHolder;
import util.AppUtils;
import util.DownloadUtil;
import util.FirebaseHandleUtil;
import util.Storage;
import util.TimeUtil;

public class MyUploadsFragment extends Fragment {
    public static final String TAG = "MyUploadsFragment";
    private TextView nothingHereView;
    private RecyclerView uploadsRecycler;
    private ViewGroup container;
    private ProgressBar progressBar;
    private ArrayList<PostableItem> items = new ArrayList<>();

    public static MyUploadsFragment newInstance(){
        return new MyUploadsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        super.onCreateView(inflater, parent, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_category_view, parent, false);
        nothingHereView = v.findViewById(R.id.nothing_here_view);
        uploadsRecycler = v.findViewById(R.id.category_recycler);
        uploadsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        container = v.findViewById(R.id.frame_container);
        progressBar = (ProgressBar)inflater.inflate(R.layout.progress_bar_layer, container, false);

        ProfileFetcher profileFetcher = new ProfileFetcher();
        profileFetcher.query(null);
        updateView();

        return v;
    }

    private class ProfileFetcher extends FSStoreFetcher<Profile> {
        private boolean found;

        public ProfileFetcher(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartFetch(){
            if(progressBar != null && container != null){
                container.removeView(progressBar);
                container.addView(progressBar);
            }
            found = false;
        }

        @Override
        public void onFind(Profile profile){
            Storage.profile = profile;
            Storage.profileLoaded = true;
            found = true;
        }

        @Override
        public void onSucceed(){
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

        @Override
        public boolean validateCondition(Profile profile){
            return profile.getEmail().equals(Storage.profile.getEmail());
        }

        @Override
        public boolean endFetchCondition(){
            return found;
        }
    }

    private void updateView(){
        if(getActivity() == null || nothingHereView == null || uploadsRecycler == null){
            return;
        }
        ArrayList<String> uploads = Storage.profile.getUploads();
        //Log.i(TAG, "Upload size : "+uploads.size());
        if(uploads == null || uploads.isEmpty()){
            nothingHereView.setVisibility(View.VISIBLE);
            uploadsRecycler.setVisibility(View.GONE);
        }else{
            items.clear();
            for(PostableItem item : Storage.postableItems){
                if(uploads.contains(item.id)){
                    items.add(item);
                }
            }
            UploadedItemAdapter itemAdapter = new UploadedItemAdapter();
            uploadsRecycler.setAdapter(itemAdapter);
        }
    }

    private class UploadedItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final ImageView photoView;
        private final TextView titleView;
        private final TextView auctionEndLabelView;
        private final TextView currentBidView;
        private final TextView auxilliaryLabelView;
        private final TextView auxilliaryAmountView;
        private final ImageView successStateView;
        private final Button contactButton;
        private PostableItem mPostableItem;
        private boolean clickable = true;

        public UploadedItemHolder(View view){
            super(view);
            photoView = itemView.findViewById(R.id.item_photo_view);
            titleView = itemView.findViewById(R.id.title_text_view);
            auctionEndLabelView = itemView.findViewById(R.id.auction_end_label_view);
            currentBidView = itemView.findViewById(R.id.current_bid_text_view);
            auxilliaryLabelView = itemView.findViewById(R.id.my_bid_label_view);
            auxilliaryAmountView = itemView.findViewById(R.id.my_bid_edit_view);
            successStateView = itemView.findViewById(R.id.success_state_view);
            contactButton = itemView.findViewById(R.id.buy_item_button);
            View mainView = itemView.findViewById(R.id.main_view);
            contactButton.setText(R.string.contact_buyer);
            //itemView.setOnClickListener(this);
            photoView.setOnClickListener(this);
            mainView.setOnClickListener(this);
        }

        public void bindPostable(PostableItem postableItem){
            mPostableItem = postableItem;
            titleView.setText(postableItem.name);
            long[] elapseData = TimeUtil.getTimeDifference(TimeUtil.getCurrentTimeStamp(), postableItem.endTimeStamp);
            auctionEndLabelView.setVisibility(View.VISIBLE);
            if(elapseData[0] < 0 || elapseData[1] < 0 || elapseData[2] < 0 || elapseData[3] < 0){
                auctionEndLabelView.setTextColor(getResources().getColor(R.color.red_american_rose_920));
                auctionEndLabelView.setText(R.string.auction_ended);
                clickable = false;
                //successStateView.setVisibility(View.VISIBLE);
            }else{
                successStateView.setVisibility(View.GONE);
                String dayT = elapseData[0] == 0 ? "<1" : String.format(Locale.getDefault(), "%d",
                        elapseData[0]);
                auctionEndLabelView.setTextColor(getResources().getColor(R.color.gray_battleship));
                auctionEndLabelView.setText("Auction ends in " + dayT + " days");
                clickable = true;
            }

            if(postableItem.buyPrice != 0){
                auxilliaryLabelView.setText(R.string.expected_ugx);
                auxilliaryAmountView.setText(String.format(Locale.getDefault(),
                        "%d", postableItem.buyPrice));
            }else{
                auxilliaryLabelView.setText(R.string.start_price_ugx);
                auxilliaryAmountView.setText(String.format(Locale.getDefault(),
                        "%d", postableItem.price));
            }

            boolean isPriceValid = (postableItem.topBid >= postableItem.buyPrice) &&
                    (postableItem.topBid > postableItem.price);

            if(isPriceValid) {
                successStateView.setVisibility(View.VISIBLE);
                successStateView.setImageResource(R.drawable.ic_baseline_check_circle_outline_24);
                successStateView.setContentDescription(getString(R.string.auction_succeeded));
                if(getActivity() != null) {
                    successStateView.setColorFilter(ContextCompat.getColor(getActivity(),
                            R.color.green_pyne_880), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }else{
                successStateView.setVisibility(View.GONE);
            }

            if(postableItem.miscKey != null && isPriceValid &&
                    (elapseData[0] < 0 || elapseData[1] < 0 || elapseData[2] < 0 || elapseData[3] < 0)){
                contactButton.setVisibility(View.VISIBLE);
                contactButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MessageActivity.sendMessage(getActivity(), postableItem.miscKey, null);
                    }
                });
            }else{
                contactButton.setVisibility(View.GONE);
            }

            String codedCurrentBid = String.format(Locale.getDefault(),
                    "%d", postableItem.topBid);
            currentBidView.setText(codedCurrentBid);
            if(postableItem.images != null && !postableItem.images.isEmpty()) {
                downloadBitmap(postableItem.images.get(0));
            }

            TopBidFetcher topBidFetcher = new TopBidFetcher(currentBidView, postableItem.id);
            topBidFetcher.listen(null);
        }

        @Override
        public void onClick(View view){
            if(!clickable || mPostableItem.buyPrice != 0){
                View deletePopView = LayoutInflater.from(getActivity()).inflate(
                        R.layout.delete_view, container, false);
                TextView contentTextView = deletePopView.findViewById(R.id.content_text_view);
                contentTextView.setText(R.string.operation_undone);
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setView(deletePopView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteItem(mPostableItem);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                alertDialog.show();
            }
        }

        private void downloadBitmap(String path){
            if(mPostableItem.bitmap != null){
                photoView.setImageBitmap(mPostableItem.bitmap);
                return;
            }
            DownloadUtil downloadUtil = new DownloadUtil(){
                @Override
                public void onFinish(byte[] bytes, boolean success){
                    if(!success){
                        return;
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            //mPostableItem.bitmap = bitmap;
                            if(photoView != null){ photoView.setImageBitmap(bitmap); }
                        }
                    });
                }
            };
            downloadUtil.downloadBytes(path);
        }
    }

    public class UploadedItemAdapter extends RecyclerView.Adapter<UploadedItemHolder>{

        @NonNull
        @Override
        public UploadedItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(
                    R.layout.horizontal_bid_item, parent, false);
            return new UploadedItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UploadedItemHolder holder, int position) {
            PostableItem item = items.get(position);
            holder.bindPostable(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private void deleteItem(PostableItem item){
        ItemDeleter itemDeleter = new ItemDeleter(item);
        itemDeleter.delete(item.id);
        FirebaseHandleUtil.NotificationUploader notificationUploader = new FirebaseHandleUtil.NotificationUploader();
        String notifId = item.miscKey + "_"+item.owner+"_"+ item.id + Notification.KEY.NONE.toString();
        Notification deleteNotification = new Notification(notifId, Notification.KEY.NONE,
                item.id, "An item was deleted!",
                "The item '" + item.name + "' that you bid on has been deleted or archived."
        );
        deleteNotification.startTimeStamp = new Date().getTime();
        deleteNotification.endTimeStamp = 1;
        notificationUploader.upload(deleteNotification);
    }

    public class TopBidFetcher extends RTDataFetcher<TopBidHolder> {
        private boolean found;
        private final TextView textView;
        private final String id;

        public TopBidFetcher(TextView textView, String id){
            super(AppUtils.MODEL_POSTABLE, TopBidHolder.class);
            this.textView = textView;
            this.id = id;
        }

        @Override
        public void onStartFetch(){
            found = false;
        }

        @Override
        public boolean validateCondition(TopBidHolder holder){
            if(holder.id == null){
                return false;
            }
            return holder.id.equals(id);
        }

        @Override
        public void onFind(TopBidHolder bidHolder){
            found = true;
            int topBid = bidHolder.topBid;
            if(textView != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() { textView.setText(String.format(Locale.getDefault(), "%d", topBid)); }
                });
            }
        }

        @Override
        public boolean endLoopCondition(){
            return found;
        }
    }

    private class ItemDeleter extends FSStoreValueChanger<PostableItem>{
        private final PostableItem item;
        private final String itemId;

        public ItemDeleter(PostableItem item){
            super(AppUtils.MODEL_POSTABLE, PostableItem.class);
            this.itemId = item.id;
            this.item = item;
        }

        @Override
        public void onStartChange(){
            if(container != null && progressBar != null){
                container.removeView(progressBar);
                container.addView(progressBar);
            }
        }

        @Override
        public void onSucceed(){
            final boolean[] completeStates = {false, false};

            RTSingleValueChanger<TopBidHolder> bidDeleter = new RTSingleValueChanger<TopBidHolder>(
                    AppUtils.MODEL_POSTABLE, TopBidHolder.class){
                @Override
                public void onChange(DatabaseReference dRef){
                    dRef.child(itemId).removeValue();
                }

                @Override
                public void onFinish(){
                    completeStates[0] = true;
                    if(completeStates[1]){
                        if(container != null && progressBar != null){
                            container.removeView(progressBar);
                        }
                        updateView();
                    }
                    if(item.images != null && !item.images.isEmpty()) {
                        StorageHandler imageDeleter = new StorageHandler();
                        for (String imageUrl : item.images) {
                            imageDeleter.deleteBytes(imageUrl);
                        }
                    }
                }


                @Override
                public void onFail(){
                    if(container != null && progressBar != null){
                        container.removeView(progressBar);
                    }
                }
            };
            bidDeleter.change();

            FSStoreValueChanger<Profile> profileUpdater = new FSStoreValueChanger<Profile>(
                    AppUtils.MODEL_PROFILE, Profile.class){
              @Override
              public void onSucceed(){
                  completeStates[1] = true;
                  if(completeStates[0]){
                      if(container != null && progressBar != null){
                          container.removeView(progressBar);
                      }
                      updateView();
                  }
              }

              @Override
              public void onFail(){
                  if(container != null && progressBar != null){
                      container.removeView(progressBar);
                  }
              }
            };
            Storage.profile.removeUpload(itemId);
            profileUpdater.update(Storage.profile.getId(), "uploads", Storage.profile.getUploads());
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null){
                container.removeView(progressBar);
            }
        }
    }
}
