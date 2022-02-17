package com.scit.stauc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import database.FSStoreFetcher;
import database.FSStoreValueChanger;
import database.RTDataFetcher;
import model.PostableItem;
import model.Profile;
import model.TopBidHolder;
import util.AdapterUtil;
import util.AppUtils;
import util.DownloadUtil;
import util.FirebaseHandleUtil;
import util.Storage;
import util.TimeUtil;

public class BidsViewFragment extends Fragment{
    private static final String TAG = "BidsViewFragment";
    private RecyclerView bidsRecycler;
    private TextView nothingHereView;
    private List<PostableItem> postableItems = new ArrayList<>();
    private BidItemAdapter itemAdapter;
    private AppUtils.ToolbarChanger toolbarChanger;
    private PostableFetcher postableFetcher;

    public static BidsViewFragment newInstance(){
        BidsViewFragment fragment = new BidsViewFragment();
        return fragment;
    }

    @Override
    public void onResume(){
        super.onResume();
        //fillItems();
        toolbarChanger = (AppUtils.ToolbarChanger) getActivity();
        if(toolbarChanger != null) {
            toolbarChanger.setTitleText(getString(R.string.my_bids));
        }
    }
    
    private void fillItems(){
        if(Storage.profile == null){
            Intent i = LoginActivity.newIntent(getActivity(), true);
            startActivity(i);
            return;
        }
        // if not profile fully loaded; load profile
        if(!Storage.profileLoaded){
            ProfileFetcher profileFetcher = new ProfileFetcher();
            profileFetcher.query(null);
            return;
        }
        if(Storage.itemsLoading){
            return;
        }
        //Toast.makeText(getActivity(), "Loading...", Toast.LENGTH_SHORT).show();
        HashMap<String, Integer> bids = Storage.profile.getBids();
        if(bids == null){ return; }
        bids = new HashMap<>(bids);
        Log.i(TAG, "bids:"+bids.size());
        if(Storage.postableItems == null || Storage.postableItems.isEmpty()){
            Log.i(TAG, "loading items...");
            postableFetcher = new PostableFetcher(bids);
            postableFetcher.getAll(Storage.postableItems);
        }else {
            Log.i(TAG, "do fill...");
            doFill(bids);
        }
    }

    private void doFill(HashMap<String, Integer> bids){
        postableItems.clear();
        HashMap<String, Integer> foundBids = new HashMap<>();
        for(Map.Entry<String, Integer> entry : bids.entrySet()){
            // search them or fetch them;
            for(PostableItem item : Storage.postableItems){
                if(item.id.equals(entry.getKey())){
                    postableItems.add(item);
                    foundBids.put(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }

        // add those that weren't added due to offline
        if(!TimeUtil.timeSet) {
            for (PostableItem item : Storage.postableItems) {
                long[] elapseData = TimeUtil.getTimeDifference(TimeUtil.getCurrentTimeStamp(), item.endTimeStamp);
                if (elapseData[0] < 0 || elapseData[1] < 0 || elapseData[2] < 0 || elapseData[3] < 0) {
                    if (item.miscKey != null && item.miscKey.equals(Storage.profile.getId())) {
                        boolean isPriceValid = (item.topBid >= item.buyPrice) && (item.topBid > item.price);
                        if (isPriceValid) {
                            foundBids.put(item.id, item.topBid);
                        }
                    }
                }
            }
        }

        Log.i(TAG, "loaded items..." + postableItems.size());
        // update Profile
        Storage.profile.setBids(foundBids);
        FSStoreValueChanger<Profile> profileBidsUpdater = new FSStoreValueChanger<Profile>(
                AppUtils.MODEL_PROFILE, Profile.class);
        profileBidsUpdater.update(Storage.profile.getId(), "bids", foundBids);
        notifyRecycler();
    }
    
    private void notifyRecycler(){
        if(postableItems.isEmpty()){
            nothingHereView.setVisibility(View.VISIBLE);
            bidsRecycler.setVisibility(View.GONE);
        }else {
            bidsRecycler.setVisibility(View.VISIBLE);
            nothingHereView.setVisibility(View.GONE);
            itemAdapter = new BidItemAdapter();
            bidsRecycler.setAdapter(itemAdapter);
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
            if(Storage.itemsLoading){
                return true;
            }
            Storage.postableItems.clear();
            fillItems();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_bids_view, container, false);
        nothingHereView = v.findViewById(R.id.nothing_here_view);
        bidsRecycler = v.findViewById(R.id.category_recycler);
        bidsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        if(postableItems.isEmpty()){
            nothingHereView.setVisibility(View.VISIBLE);
            bidsRecycler.setVisibility(View.GONE);
        }else {
            bidsRecycler.setVisibility(View.VISIBLE);
            nothingHereView.setVisibility(View.GONE);
            itemAdapter = new BidItemAdapter();
            bidsRecycler.setAdapter(itemAdapter);
        }
        fillItems();
        return v;
    }

    private class BidItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final ImageView photoView;
        private final TextView titleView;
        private final TextView auctionEndLabelView;
        private final TextView currentBidView;
        private final TextView myBidView;
        private final ImageView successStateView;
        private final Button buyItemButton;
        private PostableItem mPostableItem;
        private boolean clickable = true;
        private int mMyBid;

        public BidItemHolder(View view){
            super(view);
            photoView = itemView.findViewById(R.id.item_photo_view);
            titleView = itemView.findViewById(R.id.title_text_view);
            auctionEndLabelView = itemView.findViewById(R.id.auction_end_label_view);
            currentBidView = itemView.findViewById(R.id.current_bid_text_view);
            myBidView = itemView.findViewById(R.id.my_bid_edit_view);
            buyItemButton = itemView.findViewById(R.id.buy_item_button);
            successStateView = itemView.findViewById(R.id.success_state_view);

            buyItemButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mPostableItem != null && mPostableItem.owner != null){
                        MessageActivity.sendMessage(getActivity(), mPostableItem.owner,
                                getString(R.string.message_item_owner));
                    }
                }
            });

            itemView.setOnClickListener(this);
        }

        public void bindPostable(PostableItem postableItem){
            mPostableItem = postableItem;
            titleView.setText(postableItem.name);
            long[] elapseData = TimeUtil.getTimeDifference(TimeUtil.getCurrentTimeStamp(), postableItem.endTimeStamp);
            if(elapseData[0] < 0 || elapseData[1] < 0 || elapseData[2] < 0 || elapseData[3] < 0){
                boolean isPriceValid = (postableItem.topBid >= postableItem.buyPrice) && (postableItem.topBid > postableItem.price);
                if (isPriceValid) {
                    successStateView.setVisibility(View.VISIBLE);

                }else{
                    successStateView.setVisibility(View.GONE);
                }
                auctionEndLabelView.setTextColor(getResources().getColor(R.color.red_american_rose_920));
                auctionEndLabelView.setText(R.string.auction_ended);
                auctionEndLabelView.setVisibility(View.VISIBLE);
                clickable = false;
            }else{
                successStateView.setVisibility(View.GONE);
                auctionEndLabelView.setTextColor(getResources().getColor(R.color.gray_battleship));
                String s = "Aution ends in " + (elapseData[0] == 0 ? "<1" : elapseData[0]) + " days";
                auctionEndLabelView.setText(s);
                auctionEndLabelView.setVisibility(View.VISIBLE);
                clickable = true;
            }
            Integer myBid = Storage.profile.getBidForItem(postableItem.id);
            if(myBid == null){ myBid = 0; }
            String codedMyBid = String.format(Locale.getDefault(), "%d", myBid);
            myBidView.setText(codedMyBid);
            String codedCurrentBid = String.format(Locale.getDefault(),
                    "%d", postableItem.topBid);
            currentBidView.setText(codedCurrentBid);
            mMyBid = myBid;
            if(postableItem.images != null && !postableItem.images.isEmpty()) {
                downloadBitmap(postableItem.images.get(0));
            }
            if(mMyBid > postableItem.buyPrice && mMyBid == postableItem.topBid && !clickable){
                buyItemButton.setVisibility(View.VISIBLE);
            }

            TopBidFetcher topBidFetcher = new TopBidFetcher(currentBidView, postableItem.id);
            topBidFetcher.listen(null);
        }

        @Override
        public void onClick(View view){
            if(!clickable){
                return;
            }
            Intent i = ItemDetailActivity.newIntent(getActivity(), mPostableItem);
            startActivity(i);
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
                            mPostableItem.bitmap = bitmap;
                            if(photoView != null){ photoView.setImageBitmap(bitmap); }
                        }
                    });
                }
            };
            downloadUtil.downloadBytes(path);
        }
    }

    public class BidItemAdapter extends RecyclerView.Adapter<BidItemHolder>{

        @NonNull
        @Override
        public BidItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(
                    R.layout.horizontal_bid_item, parent, false);
            return new BidItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BidItemHolder holder, int position) {
            PostableItem item = postableItems.get(position);
            holder.bindPostable(item);
        }

        @Override
        public int getItemCount() {
            return postableItems.size();
        }
    }

    private class ProfileFetcher extends FSStoreFetcher<Profile> {
        private boolean found;

        public ProfileFetcher(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartFetch(){
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
            fillItems();
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

    private class PostableFetcher extends FSStoreFetcher<PostableItem>{
        HashMap<String, Integer> bids;
        public PostableFetcher(HashMap<String, Integer> bids){
            super(AppUtils.MODEL_POSTABLE, PostableItem.class);
            this.bids = bids;
        }

        @Override
        public void onStartFetch(){
            Storage.itemsLoading = true;
            Storage.postableItems.clear();
        }

        @Override
        public void onFinish(){
            Storage.itemsLoading = false;
            doFill(bids);
            // FirebaseHandleUtil.deleteExpiredBids();
        }

        @Override
        public boolean validateCondition(PostableItem item){
            boolean valid = FirebaseHandleUtil.filter(item);
            if(valid){
                for(int i=0; i<HomeFragment.categoryBitmapIds.length;i++){
                    if (HomeFragment.categories[i].toString().equals(item.category)){
                        if(HomeFragment.categoryBitmapIds[i] == null && item.images != null && !item.images.isEmpty()){
                            HomeFragment.categoryBitmapIds[i] = item.images.get(0);
                        }
                        break;
                    }
                }
            }
            return valid;
        }
    }

    public class TopBidFetcher extends RTDataFetcher<TopBidHolder> {
        private boolean found;
        private TextView textView;
        private String id;

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
            if(holder == null){ return false; }
            if(holder.id == null){ return false; }
            return holder.id.equals(id);
        }

        @Override
        public void onFind(TopBidHolder bidHolder){
            found = true;
            int topBid = bidHolder.topBid;
            Log.i(TAG, "Found bidHolder:"+topBid);
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

}
