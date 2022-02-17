package com.scit.stauc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import database.FSStoreFetcher;
import database.FSStoreValueChanger;
import database.RTDataFetcher;
import database.RTSingleValueChanger;
import model.PostableItem;
import model.Profile;
import model.TopBidHolder;
import util.AppUtils;
import util.DownloadUtil;
import util.Storage;
import util.TimeUtil;

public class ItemDetailFragment extends Fragment{
    private static final String TAG = "ItemDetailFragment";
    private static final long REPOST_DELAY = 1000;
    private FlippableClickableImageAdapter pagerAdapter;
    private List<Bitmap> bitmaps = new ArrayList<>();
    private TopBidFetcher topBidFetcher = null;
    private TextView currentBidView;
    private TextView internalCurrentBidView;
    private View orLayout;
    private TextView buynowClickableView;
    private ProgressBar bidLoadProgressBar;
    private ProgressBar currentBidLoadProgressBar;
    private Button placeBidButton;
    private TextView posterNameTextView;
    private TextView recentBidsView;
    private View confirmView;
    private boolean clickable = true;
    private boolean loadedBitmaps = false;
    private volatile boolean setImage = false;
    private volatile boolean active;
    private volatile boolean found = false;


    public static ItemDetailFragment newInstance(){
       return new ItemDetailFragment();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(Storage.profile != null && Storage.postableItem.owner.equals(Storage.profile.getId())){
            orLayout.setVisibility(View.GONE);
            placeBidButton.setVisibility(View.GONE);
            buynowClickableView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        active = true;
        if(topBidFetcher != null){
            topBidFetcher.stopListening();
        }
        topBidFetcher = new TopBidFetcher();
        topBidFetcher.listen(null);
    }

    public void onStop(){
        super.onStop();
        active = false;
        if(topBidFetcher != null){
            topBidFetcher.stopListening();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        DisplayMetrics displayMetrics = AppUtils.getDeviceDisplayMetrics(getActivity());
        View v = inflater.inflate(R.layout.fragment_item_detail, container, false);
        ViewPager imagePager = v.findViewById(R.id.image_pager);

        View flipperContainer = v.findViewById(R.id.flipper_container);
        flipperContainer.getLayoutParams().height = (int) (displayMetrics.heightPixels * 0.6);
        pagerAdapter = new FlippableClickableImageAdapter();
        imagePager.setAdapter(pagerAdapter);

        TextView descriptionView = v.findViewById(R.id.description_text_view);
        if(Storage.postableItem.description != null && !Storage.postableItem.description.isEmpty()) {
            descriptionView.setText(Storage.postableItem.description);
        }
        TextView titleView = v.findViewById(R.id.title_text_view);
        titleView.setText(Storage.postableItem.name);
        TextView priceView = v.findViewById(R.id.price_text_view);
        priceView.setText("UGX " + Storage.postableItem.price);
        TextView countdownTextView = v.findViewById(R.id.auction_count_view);
        View buyPriceLayout = v.findViewById(R.id.buy_now_layout);
        orLayout = v.findViewById(R.id.or_layout);
        buynowClickableView = v.findViewById(R.id.buy_now_textview);
        TextView buynowPriceTextView = v.findViewById(R.id.buy_now_price_text_view);
        TextView timeFormatTextView = v.findViewById(R.id.auction_time_format_view);
        TextView uploadDateTextView = v.findViewById(R.id.upload_date_text_view);
        TextView auctionEndLabel = v.findViewById(R.id.auction_end_label_view);
        currentBidView = v.findViewById(R.id.current_bid_text_view);
        currentBidLoadProgressBar = v.findViewById(R.id.current_bid_load_progress_bar);
        placeBidButton = v.findViewById(R.id.place_bid_button);
        posterNameTextView = v.findViewById(R.id.poster_label);
        recentBidsView = v.findViewById(R.id.recent_bids_view);

        recentBidsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Storage.postableItem.logs == null || Storage.postableItem.logs.isEmpty()){
                    Toast.makeText(getActivity(), "Nothing yet!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = BidderViewActivity.newItent(getActivity());
                startActivity(i);
            }
        });

        loadBitmaps();

        String pattern = "EE dd MMM, yyyy KK:mm aa";
        SimpleDateFormat dateFormat =  new SimpleDateFormat(pattern, Locale.getDefault());
        String dateTimeString = dateFormat.format(Storage.postableItem.startTimeStamp);
        String viewableTime = "Uploaded " + dateTimeString;
        uploadDateTextView.setText(viewableTime);

        ProfileNameFetcher nameFetcher = new ProfileNameFetcher(Storage.postableItem.owner);
        nameFetcher.query(null);

        if(Storage.postableItem.buyPrice == 0 || Storage.postableItem.topBid > Storage.postableItem.buyPrice){
            buyPriceLayout.setVisibility(View.GONE);
            orLayout.setVisibility(View.GONE);
            buynowClickableView.setVisibility(View.GONE);
        }else{
            buyPriceLayout.setVisibility(View.VISIBLE);
            buynowPriceTextView.setText(Integer.toString(Storage.postableItem.buyPrice));
            orLayout.setVisibility(View.VISIBLE);
            buynowClickableView.setVisibility(View.VISIBLE);
        }

        buynowClickableView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!found){
                    return;
                }
                if(!clickable){
                    Toast.makeText(getActivity(), R.string.item_not_available, Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!TimeUtil.timeSet){
                    Toast.makeText(getActivity(), R.string.time_not_synced, Toast.LENGTH_SHORT).show();
                    TimeUtil.getSiteTime();
                    return;
                }
                if(Storage.profile == null){
                    login();
                    return;
                }
                MessageActivity.sendMessage(getActivity(), Storage.postableItem.owner,
                        getString(R.string.message_item_owner));
            }
        });

        placeBidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!found || !clickable){
                    return;
                }
                if(!TimeUtil.timeSet){
                    Toast.makeText(getActivity(), R.string.time_not_synced, Toast.LENGTH_SHORT).show();
                    TimeUtil.getSiteTime();
                    return;
                }
                if(Storage.profile == null){
                    login();
                    return;
                }
                View bidForm = LayoutInflater.from(getActivity())
                        .inflate(R.layout.form_bidrequest, null);
                View closeView = bidForm.findViewById(R.id.close_view);
                internalCurrentBidView = bidForm.findViewById(R.id.current_bid_text_view);
                EditText mybidEditView = bidForm.findViewById(R.id.my_bid_edit_view);
                // set update handler here
                internalCurrentBidView.setText(String.format(Locale.getDefault(), "%d", Storage.postableItem.topBid));

                AlertDialog bidDialog = new AlertDialog.Builder(getActivity())
                        .setView(bidForm)
                        .create();
                bidDialog.show();

                closeView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) { bidDialog.dismiss(); }
                });

                View cancelView = bidForm.findViewById(R.id.cancel_button);
                confirmView = bidForm.findViewById(R.id.confirm_button);
                bidLoadProgressBar = bidForm.findViewById(R.id.bid_load_progress_bar);

                cancelView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) { bidDialog.dismiss(); }
                });

                final boolean[] isValidBid = {false};
                confirmView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(isValidBid[0]){
                            String myBid = mybidEditView.getText().toString();
                            int bid = Integer.parseInt(myBid);
                            updateBidValue(bid);
                            bidDialog.dismiss();
                        }else{
                            mybidEditView.setError("Enter a valid amount!");
                        }
                    }
                });

                mybidEditView.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String myBid = mybidEditView.getText().toString();
                        if(!myBid.isEmpty() && getActivity()!=null) {
                            int parsedInt = Integer.parseInt(myBid);
                            if (parsedInt > Storage.postableItem.topBid) {
                                if(AppUtils.isValidAmount(parsedInt)) {
                                    mybidEditView.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
                                    isValidBid[0] = true;
                                }else{
                                    isValidBid[0] = false;
                                    mybidEditView.setError("Enter a valid amount!");
                                    mybidEditView.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_400));
                                }
                            } else {
                                mybidEditView.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_400));
                                mybidEditView.setError("Enter a higher amount!");
                                isValidBid[0] = false;
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
            }
        });

        HandlerThread timerThread = new HandlerThread("ItemDetail.Timer");
        if(!timerThread.isAlive()){
            timerThread.start();
        }
        Handler timeHandler = new Handler(timerThread.getLooper());
        timeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Storage.postableItem == null || !active){
                    timeHandler.removeCallbacks(this);
                    return;
                }
                long[] elapseData = TimeUtil.getTimeDifference(TimeUtil.getCurrentTimeStamp(), Storage.postableItem.endTimeStamp);
                if(elapseData[0] < 0 || elapseData[1] < 0 || elapseData[2] < 0 || elapseData[3] < 0){
                    clickable = false;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            auctionEndLabel.setText(R.string.auction_ended);
                            auctionEndLabel.setTextColor(getResources().getColor(R.color.red_american_rose_920));
                            countdownTextView.setText(null);
                            timeFormatTextView.setText(null);
                            if(getActivity() != null) {
                                Toast.makeText(getActivity(), R.string.item_not_available, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    timeHandler.removeCallbacks(this);
                    return;
                }
                if(elapseData[0] != 0){
                    String dayString = String.format(Locale.getDefault(),
                            "%d", elapseData[0]);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            countdownTextView.setText(dayString);
                            timeFormatTextView.setText(R.string.days_short);
                        }
                    });
                    if(elapseData[0] > 2){ timeHandler.removeCallbacks(this); }
                }else {
                    String timeString = String.format(Locale.getDefault(),
                            "%02d:%02d:%02d",
                            elapseData[1], elapseData[2], elapseData[3]);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            countdownTextView.setText(timeString);
                            timeFormatTextView.setText(R.string.hours_short);
                        }
                    });
                }
                timeHandler.postDelayed(this, REPOST_DELAY);
            }
        }, REPOST_DELAY);
        return v;
    }

    private synchronized void loadBitmaps(){
        DownloadUtil downloadUtil = new DownloadUtil(){
            @Override
            public void onStart(){
                bitmaps = new ArrayList<>();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(Storage.postableItem.bitmap != null && !setImage) {
                            bitmaps.add(Storage.postableItem.bitmap);
                            pagerAdapter.notifyDataSetChanged();
                            setImage = true;
                        }
                    }
                });

            }

            @Override
            public void onFinish(byte[] bytes, boolean success){
                if(!success){
                    return;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if(!loadedBitmaps) {
                            bitmaps.clear();
                            loadedBitmaps = true;
                        }
                        bitmaps.add(bitmap);
                        pagerAdapter.notifyDataSetChanged();
                    }
                });
            }
        };
        if(Storage.postableItem.images != null) {
            for (String path : Storage.postableItem.images) {
                downloadUtil.downloadBytes(path);
            }
        }
    }

    private void login(){
        Intent i = LoginActivity.newIntent(getActivity(), true);
        startActivity(i);
    }

    private void updateBidValue(int bid){
        if(Storage.profile == null){
            return;
        }
        Log.i(TAG, "updating bid value...");
        Storage.postableItem.miscKey = Storage.profile.getId(); // necessary to ensure misc != null offline
        Storage.postableItem.addLog(Storage.profile.getId(), bid);
        Storage.profile.addBid(Storage.postableItem.id, bid);
        FSStoreValueChanger<Profile> profileChanger = new FSStoreValueChanger<>(AppUtils.MODEL_PROFILE, Profile.class);
        profileChanger.setMerge(Storage.profile.getId(), Storage.profile);
        RTSingleValueChanger<TopBidHolder> bidValueChanger = new RTSingleValueChanger<TopBidHolder>(
                AppUtils.MODEL_POSTABLE, TopBidHolder.class){
            @Override
            public void onChange(DatabaseReference dRef){
                Log.i(TAG, "setting " + AppUtils.MODEL_POSTABLE + ", " + bid);
                TopBidHolder topBidHolder = new TopBidHolder(Storage.postableItem.id, bid);
                Storage.postableItem.topBid = bid;
                dRef.child(topBidHolder.id).setValue(topBidHolder);
            }
            @Override
            public void onFinish(){
                FSStoreValueChanger<PostableItem> itemChanger = new FSStoreValueChanger<>(AppUtils.MODEL_POSTABLE, PostableItem.class);
                PostableItem postableItem = Storage.postableItem;
                postableItem.bitmap = null; // essential
                itemChanger.setMerge(postableItem.id, postableItem);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() { Toast.makeText(getActivity(), "Bid Updated!", Toast.LENGTH_SHORT).show(); }
                });
            }
        };
        bidValueChanger.change();
    }

    public class FlippableClickableImageAdapter extends PagerAdapter{
        public final Context mContext = getActivity();

        @Override
        public Object instantiateItem(ViewGroup container, int position){
            ImageView imageView = new ImageView(mContext);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            );
            imageView.setLayoutParams(params);
            Bitmap bitmap = bitmaps.get(position);
            imageView.setImageBitmap(bitmap);
            container.addView(imageView);
            imageView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    //Intent intent = ImageFlipScreenActivity.newIntent(getActivity(), position);
                    //startActivity(intent);
                }
            });
            return (Object) imageView;
        }

        @Override
        public int getCount(){
            return bitmaps.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object){
            return view == ((View) object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object){
            container.removeView((View) object);
        }
    }

    public class TopBidFetcher extends RTDataFetcher<TopBidHolder> {

        public TopBidFetcher(){
            super(AppUtils.MODEL_POSTABLE, TopBidHolder.class);
        }

        @Override
        public void onStartFetch(){
            currentBidLoadProgressBar.setVisibility(View.VISIBLE);
            if(bidLoadProgressBar != null){
                bidLoadProgressBar.setVisibility(View.VISIBLE);
            }
            if(confirmView != null){
                confirmView.setEnabled(false);
            }
            found = false;
        }

        @Override
        public boolean validateCondition(TopBidHolder holder){
            return holder.id.equals(Storage.postableItem.id);
        }

        @Override
        public void onFind(TopBidHolder bidHolder){
            found = true;
            int topBid = bidHolder.topBid;
            Storage.postableItem.topBid = topBid;
            Log.i(TAG, "Found bidHolder:"+topBid);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    String currentBidString = String.format(Locale.getDefault(), "%d", topBid);
                    if (internalCurrentBidView != null) {
                        internalCurrentBidView.setText(currentBidString);
                    }
                    currentBidView.setText(currentBidString);
                }
            });
            if(topBid > Storage.postableItem.buyPrice) {
                if (orLayout != null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() { orLayout.setVisibility(View.GONE); }
                    });
                }
            }
        }

        @Override
        public boolean endLoopCondition(){
            return found;
        }

        @Override
        public void onFail(){
            currentBidLoadProgressBar.setVisibility(View.GONE);
            if(bidLoadProgressBar != null){
                bidLoadProgressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onFinish(){
            currentBidLoadProgressBar.setVisibility(View.GONE);
            if(bidLoadProgressBar != null){
                bidLoadProgressBar.setVisibility(View.GONE);
            }
            if(confirmView != null){
                confirmView.setEnabled(true);
            }
        }
    }

    private class ProfileNameFetcher extends FSStoreFetcher<Profile> {
        private boolean found;
        private final String id;

        public ProfileNameFetcher(String id){
            super(AppUtils.MODEL_PROFILE, Profile.class);
            this.id = id;
        }

        @Override
        public void onStartFetch(){ found = false; }

        @Override
        public void onFind(Profile profile){
            posterNameTextView.setText(profile.getName());
            found = true;
        }

        @Override
        public boolean validateCondition(Profile profile){ return profile.getId().equals(id); }

        @Override
        public boolean endFetchCondition(){ return found; }
    }
}
