package com.scit.stauc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import database.FSStoreFetcher;
import database.RTDataFetcher;
import model.Message;
import model.MessageBin;
import model.Profile;
import util.AppUtils;
import util.DownloadUtil;
import util.MessageRoomListener;
import util.Storage;
import view.CircleImageView;

public class MessageRoomFragment extends Fragment {
    private static final String TAG = "MessageRoom";

    private RecyclerView mRecyclerView;
    private ViewGroup container;
    private ProgressBar progressBar;
    private MessageRoomListener messageRoomListener;
    private ArrayList<MessageBin> messageBins = new ArrayList<>();

    public static MessageRoomFragment newInstance(){
        return new MessageRoomFragment();
    }

    @Override
    public void onResume(){
        super.onResume();
        AppUtils.ToolbarChanger toolbarChanger = (AppUtils.ToolbarChanger) getActivity();
        if(toolbarChanger != null){
            toolbarChanger.setTitleText(getString(R.string.messages));
        }
        fetchMessages();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(messageRoomListener != null){
            messageRoomListener.stopListening();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.refresh_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int itemId = item.getItemId();
        if(itemId == R.id.action_refresh){
            fetchMessages();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_category_view, parent, false);
        mRecyclerView = v.findViewById(R.id.category_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        container = v.findViewById(R.id.frame_container);
        progressBar = (ProgressBar) inflater.inflate(R.layout.progress_bar_layer, container, false);
        return v;
    }

    private void fetchMessages(){
        if(Storage.profile == null){
            return;
        }
        if(messageRoomListener != null){
            messageRoomListener.stopListening();
        }
        if(messageRoomListener != null && messageRoomListener.isLoading()){
            return;
        }
        messageRoomListener = new MessageRoomListener(container, progressBar){
            @Override
            public void onFinishFetch(ArrayList<MessageBin> bins){
                Log.i(TAG, "binsize = "+bins.size());
                messageBins = bins;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        updateView();
                    }
                });
            }
        };
        messageRoomListener.fetchMessages();
    }

    private void updateView(){
        if(!messageBins.isEmpty()) {
            mRecyclerView.setVisibility(View.VISIBLE);
            MessageBinAdapter messageBinAdapter = new MessageBinAdapter();
            mRecyclerView.setAdapter(messageBinAdapter);
        }else{
            mRecyclerView.setVisibility(View.GONE);
        }
    }

    private class MessageBinHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final CircleImageView photoView;
        private final TextView nameView;
        private final TextView dateView;
        private final TextView contentView;
        private String receipientId = null;
        private final int readColor = R.color.cool_gray_4;
        private final int unReadColor = R.color.black;
        private final int readTypeface = Typeface.NORMAL;
        private final int unReadTypeface = Typeface.BOLD;

        public MessageBinHolder(View view){
            super(view);
            photoView = itemView.findViewById(R.id.dp_card_view);
            nameView = itemView.findViewById(R.id.name_view);
            dateView = itemView.findViewById(R.id.date_view);
            contentView = itemView.findViewById(R.id.content_view);
            photoView.setImageResource(R.drawable.ic_baseline_person_24);
            photoView.setHighlightEnable(false);
            itemView.setOnClickListener(this);
        }

        public void bindMessageBin(MessageBin messageBin){
            Log.i(TAG, "has messagebin");
            Message topMsg = messageBin.getTopMessage();
            if(topMsg == null){
                return;
            }
            //dpCard.setLetter(messageBin.getRecipient());
            //nameView.setText(messageBin.getRecipient());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String dateString = dateFormat.format(new Date(topMsg.getTimeStamp()));
            dateView.setText(dateString);
            contentView.setText(topMsg.getContent());
            boolean isRead = topMsg.getSender().equals(Storage.profile.getId()) || topMsg.getRead();
            showAsReadSync(isRead);
            String userKey = "_" + Storage.profile.getId() + "_";
            // here!
            receipientId = messageBin.getId().replace(userKey, "").replace("_", "");
            Log.i(TAG, "resKey : "+receipientId);
            ProfileDataFetcher profileDataFetcher = new ProfileDataFetcher(receipientId.trim(), nameView, photoView);
            profileDataFetcher.query(null);
        }

        private void showAsReadSync(boolean isread){
            TextView[] textViews = {nameView, dateView, contentView};
            for (TextView textView : textViews){
                textView.setTextColor(getResources().getColor(isread ?readColor : unReadColor));
                textView.setTypeface(textView.getTypeface(), isread ? readTypeface : unReadTypeface);
            }
        }

        @Override
        public void onClick(View view){
            if(receipientId != null) {
                Intent i = MessageActivity.newIntent(getActivity(), receipientId);
                startActivity(i);
            }
        }

    }

    private class MessageBinAdapter extends RecyclerView.Adapter<MessageBinHolder>{
        @Override
        public MessageBinHolder onCreateViewHolder(ViewGroup parent, int viewType){
            View v = LayoutInflater.from(getActivity()).inflate(R.layout.message_bin_view, parent, false);
            return new MessageBinHolder(v);
        }

        @Override
        public int getItemCount(){
            return messageBins.size();
        }

        @Override
        public void onBindViewHolder(MessageBinHolder holder, int position){
            holder.bindMessageBin(messageBins.get(position));
        }
    }

    // getName & profileImage
    private class ProfileDataFetcher extends FSStoreFetcher<Profile> {
        private boolean found;
        private final String userId;
        private TextView textView;
        private CircleImageView cCard;

        public ProfileDataFetcher(String userId, TextView textView, CircleImageView cCard){
            super(AppUtils.MODEL_PROFILE, Profile.class);
            this.userId = userId;
            this.textView = textView;
            this.cCard = cCard;
        }

        @Override
        public void onStartFetch(){
            found = false;
        }

        @Override
        public void onFind(Profile profile){
            Log.i(TAG, "found Profile with name:"+profile.getName());
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() { textView.setText(profile.getName()); }
            });

            DownloadUtil dlUtil = new DownloadUtil(){
                @Override
                public void onFinish(byte[] bytes, boolean success){
                    if(!success){
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() { cCard.setImageResource(R.drawable.ic_baseline_person_24); }
                        });
                        return;
                    }
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            cCard.setImageBitmap(bitmap);
                        }
                    });
                }
            };
            if(profile.imagePath != null) {
                dlUtil.downloadBytes(profile.imagePath);
            }else{
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() { cCard.setImageResource(R.drawable.ic_baseline_person_24); }
                });
            }
            found = true;
        }

        @Override
        public void onSucceed(){ }

        @Override
        public void onFail(){ }

        @Override
        public boolean validateCondition(Profile profile){ return profile.getId().equals(userId); }

        @Override
        public boolean endFetchCondition(){ return found; }
    }

}
