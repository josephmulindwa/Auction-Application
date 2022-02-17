package com.scit.stauc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import database.FSStoreFetcher;
import database.RTDataFetcher;
import model.PostableItem;
import model.TopBidHolder;
import util.AdViewerInterface;
import util.AdapterUtil;
import util.AppUtils;
import util.DownloadUtil;
import util.FirebaseHandleUtil;
import util.Storage;
import util.TimeUtil;
import view.DotIndicatorView;

public class HomeFragment extends Fragment {
    private static final String TAG = "NavigationHomeFragment";
    public static final String TOP_PICKS = "Top Picks";
    public static final String RECENT_UPLOADS = "Recent Uploads";
    private final NestedRecyclerItem topPicksNestedRecycler = new NestedRecyclerItem(TOP_PICKS);
    private final NestedRecyclerItem recentUploadsNestedRecycler = new NestedRecyclerItem(RECENT_UPLOADS);
    private RecyclerView topPicksRecyclerView = null;
    private RecyclerView recentUploadsRecyclerView = null;
    private AppUtils.ToolbarChanger toolbarChanger;
    private HandlerThread helperThread;
    public static final PostableItem.CATEGORY[] categories = PostableItem.CATEGORY.values();
    private static final Bitmap[] categoryBitmaps = new Bitmap[categories.length];
    public final static String[] categoryBitmapIds = new String[categories.length];
    private final ImageView[] categoryViews = new ImageView[categories.length];
    private String[] categoryTitles;
    private Handler scrollHandler;
    private Runnable pagerUpdater;
    private ParentAdapter parentAdapter;
    private PostableFetcher postableFetcher;
    private boolean active = true; // activity is visibly active

    public static HomeFragment newInstance(){
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        active = true;
        toolbarChanger = (AppUtils.ToolbarChanger) getActivity();
        categoryTitles = new String[categories.length];
        helperThread = new HandlerThread(TAG);
        helperThread.start();
        for(int i=0; i < categories.length;i++){
            categoryTitles[i] = categories[i].toString();
            categoryBitmapIds[i] = null;
            categoryBitmaps[i] = null;
            categoryViews[i] = null;
        }
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
            postableFetcher = new PostableFetcher();
            postableFetcher.getAll(Storage.postableItems);
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = v.findViewById(R.id.home_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        parentAdapter = new ParentAdapter(getActivity());
        recyclerView.setAdapter(parentAdapter);
        loadPostableItems();
        return v;
    }

    private void loadPostableItems(){
        if(Storage.postableItems.isEmpty()) {
            if(!Storage.itemsLoading) {
                postableFetcher = new PostableFetcher();
                postableFetcher.getAll(Storage.postableItems);
            }
        }else{
            loadNestedRecyclers();
        }
    }

    private synchronized void loadNestedRecyclers(){
        if(Storage.itemsLoading){ return; }
        topPicksNestedRecycler.items.clear();
        recentUploadsNestedRecycler.items.clear();
        topPicksNestedRecycler.items.addAll(getTopPicks());
        recentUploadsNestedRecycler.items.addAll(getRecentPicks());

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(topPicksRecyclerView != null){
                    topPicksRecyclerView.setAdapter(new PostableItemAdapter(topPicksNestedRecycler.items));
                }
                if(recentUploadsRecyclerView != null){
                    recentUploadsRecyclerView.setAdapter(new PostableItemAdapter(recentUploadsNestedRecycler.items));
                }
            }
        });
    }

    public static synchronized ArrayList<PostableItem> getTopPicks(){
        ArrayList<PostableItem> items = new ArrayList<>(Storage.postableItems);
        Storage.ITEM_SORT_CRITERIA = PostableItem.CRITERIA.TOPBID;
        Collections.sort(items);
        return items;
    }

    public static synchronized ArrayList<PostableItem> getRecentPicks(){
        ArrayList<PostableItem> items = new ArrayList<>(Storage.postableItems);
        Storage.ITEM_SORT_CRITERIA = PostableItem.CRITERIA.DATE_UPLOAD;
        Collections.sort(items);
        return items;
    }

    private void downloadCategoryBitmap(int index, ImageView imageView){
        if(categoryBitmaps[index] != null){ return; }

        Handler dlHandler = new Handler(helperThread.getLooper());
        dlHandler.post(new Runnable() {
            @Override
            public void run() {
                if(Storage.postableItems == null){ return; }
                String url = categoryBitmapIds[index];
                DownloadUtil downloadUtil = new DownloadUtil(){
                    @Override
                    public boolean endDownloadCondition(){
                        return !active;
                    }

                    @Override
                    public void onFinish(byte[] bytes, boolean success){
                        if(!success){ return; }
                        categoryBitmaps[index] = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if(imageView == null){ return; }
                                imageView.setImageBitmap(categoryBitmaps[index]);
                            }
                        });
                    }
                };
                if(url != null && categoryBitmaps[index] == null) {
                    downloadUtil.downloadBytes(url);
                }
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(scrollHandler != null && pagerUpdater != null){
            scrollHandler.removeCallbacks(pagerUpdater);
        }
    }

    private class HorizontalRecyclerHolder extends RecyclerView.ViewHolder{
        private final TextView categoryView;
        public RecyclerView childRecyclerView;
        private int categoryIndex = 0;
        private String categoryTitle = null;
        private final View categorySelectArrow;

        public HorizontalRecyclerHolder(View view){
            super(view);
            categoryView = itemView.findViewById(R.id.category_text_view);
            childRecyclerView = itemView.findViewById(R.id.child_recycler_view);
            categorySelectArrow = itemView.findViewById(R.id.category_select_arrow);
        }

        public void bindHolder(int categoryIndex, String categoryTitle){
            categoryView.setText(categoryTitle);
            categorySelectArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(categoryIndex == 0 || categoryTitle == null){ return; }
                    if(getActivity() != null) {
                        Intent i = CategoryViewActivity.newIntent(getActivity(),
                                categoryTitle, 100);
                        getActivity().startActivity(i);
                    }
                }
            });
        }
    }

    private class HomeCategoryHolder extends RecyclerView.ViewHolder{
        public TextView browseTextView;
        public ViewPager categoryViewPager;

        public HomeCategoryHolder(View view){
            super(view);
            DisplayMetrics displayMetrics = AppUtils.getDeviceDisplayMetrics(getActivity());
            int height = (int) (displayMetrics.heightPixels * 0.5);
            itemView.getLayoutParams().height = height;
            browseTextView = itemView.findViewById(R.id.browse_text_view);
            categoryViewPager = itemView.findViewById(R.id.category_view_pager);
            categoryViewPager.setAdapter(new CategoryPagerAdapter(getActivity()));
            DotIndicatorView dotIndicatorView = itemView.findViewById(R.id.dot_indicator_view);
            dotIndicatorView.setCount(categoryTitles.length);
            categoryViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

                @Override
                public void onPageSelected(int position) {
                    dotIndicatorView.setCurrentIndex(position);
                }

                @Override
                public void onPageScrollStateChanged(int state) { }
            });
            // autoScroll
            scrollHandler = new Handler();
            long elapse = 3000;
            pagerUpdater = new Runnable() {
                @Override
                public void run() {
                    int index = categoryViewPager.getCurrentItem();
                    index = (index + 1) % categoryTitles.length; // categoryCount
                    categoryViewPager.setCurrentItem(index, (index != 0));
                    //dotIndicatorView.setCurrentIndex(index);
                    scrollHandler.postDelayed(this, elapse);
                }
            };
            scrollHandler.postDelayed(pagerUpdater, elapse);
        }
    }

    public class CategoryPagerAdapter extends PagerAdapter{
        private final Context mContext;

        public CategoryPagerAdapter(Context context){
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.category_view_internal_layout,
                    container, false);
            ImageView categoryImageView = view.findViewById(R.id.adapter_category_image_view);
            TextView categoryTextView = view.findViewById(R.id.adapter_category_text_view);
            Bitmap bitmap = categoryBitmaps[position];
            categoryViews[position] = categoryImageView;
            if(bitmap != null) {
                categoryImageView.setImageBitmap(bitmap);
            }
            categoryTextView.setText(categoryTitles[position]);
            container.addView(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = CategoryViewActivity.newIntent(getActivity(),
                            categoryTitles[position], position);
                    startActivity(i);
                }
            });
            return view;
        }

        @Override
        public int getCount(){
            return categoryTitles.length;
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

    private class PostableItemAdapter extends RecyclerView.Adapter<PostableItemAdapter.PostableItemHolder>{
        // child Recycler
        private final List<PostableItem> items;
        public PostableItemAdapter(List<PostableItem> mList){
            items = mList;
        }

        @NonNull
        @Override
        public PostableItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            View v= LayoutInflater.from(getActivity()).inflate(R.layout.horizontal_recycler_item,
                    parent, false);
            // change size of recycler item
            DisplayMetrics displayMetrics = AppUtils.getDeviceDisplayMetrics(getActivity());
            v.getLayoutParams().width = (int) (displayMetrics.widthPixels * 0.7);
            v.getLayoutParams().height = (int) (displayMetrics.heightPixels * 0.4);
            return new PostableItemHolder(v);
        }

        @Override
        public void onBindViewHolder(PostableItemHolder holder, int position){
            holder.bindPostable(items.get(position));
        }

        @Override
        public int getItemCount(){
            return items.size();
        }

        private class PostableItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            private final long repostDelay = 1000;
            private final TextView mTitleView;
            private final TextView mPriceView;
            private final ImageView mImageView;
            private PostableItem mPostableItem;
            private final TextView countdownTextView;
            private final TextView timeFormatTextView;
            private long endTime = -1;
            private long startTime = -1;
            private boolean clickable = true;
            private String itemId = null;

            public PostableItemHolder(View view){
                super(view);
                mTitleView = itemView.findViewById(R.id.recycler_title_view);
                mPriceView = itemView.findViewById(R.id.recycler_price_view);
                mImageView = itemView.findViewById(R.id.recycler_image_view);
                countdownTextView = itemView.findViewById(R.id.auction_count_view);
                timeFormatTextView = itemView.findViewById(R.id.auction_time_format_view);
                itemView.setOnClickListener(this);

                Handler timeHandler = new Handler(helperThread.getLooper());
                timeHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(endTime > 0) {
                            long[] elapseData = TimeUtil.getTimeDifference(TimeUtil.getCurrentTimeStamp(), endTime);
                            if(elapseData[0] < 0 || elapseData[1] < 0 || elapseData[2] < 0 || elapseData[3] < 0){
                                clickable = false;
                                showAsNotBiddable();
                                FirebaseHandleUtil.performBid(mPostableItem, mPostableItem.topBid); // accurate topBid
                                timeHandler.removeCallbacks(this);
                                return;
                            }else{
                                showAsBiddable();
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
                                // save thread if in days
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
                        }
                        timeHandler.postDelayed(this, repostDelay);
                    }
                }, repostDelay);
            }

            public void bindPostable(PostableItem postable){
                mPostableItem = postable;
                itemId = mPostableItem.id;
                mTitleView.setText(postable.name);
                int usePrice = postable.topBid == 0 ? postable.price : postable.topBid;
                String bidString = String.format(Locale.getDefault(), "UGX %d", usePrice);
                mPriceView.setText(bidString);
                if(postable.images != null && !postable.images.isEmpty()){
                    downloadBitmap(mPostableItem.images.get(0));
                }
                endTime = postable.endTimeStamp;
            }

            private void showAsNotBiddable(){
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(getActivity() == null){
                            return;
                        }
                        TextView auctionEndLabel = itemView.findViewById(R.id.auction_end_label_view);
                        auctionEndLabel.setText(R.string.auction_ended);
                        auctionEndLabel.setTextColor(getResources().getColor(R.color.red_american_rose_920));
                        countdownTextView.setText(null);
                        timeFormatTextView.setText(null);
                    }
                });
            }

            private void showAsBiddable(){
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(getActivity() == null){
                            return;
                        }
                        TextView auctionEndLabel = itemView.findViewById(R.id.auction_end_label_view);
                        auctionEndLabel.setText(R.string.auction_ends_in);
                        auctionEndLabel.setTextColor(getResources().getColor(R.color.gray_battleship));
                        countdownTextView.setText(R.string.time_holder);
                        timeFormatTextView.setText(R.string.hours_short);
                    }
                });
            }

            @Override
            public void onClick(View v){
                if(!clickable){
                    if(Storage.profile != null && mPostableItem.owner.equals(Storage.profile.getId())){
                        Intent intent = MyUploadsActivity.newIntent(getActivity());
                        startActivity(intent);
                        return;
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), R.string.item_not_available, Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                Intent intent = ItemDetailActivity.newIntent(getActivity(), mPostableItem);
                startActivity(intent);
            }

            public void downloadBitmap(String path){
                if(mPostableItem.bitmap != null){
                    mImageView.setImageBitmap(mPostableItem.bitmap);
                    return;
                }
                DownloadUtil downloadUtil = new DownloadUtil(){
                    @Override
                    public boolean endDownloadCondition(){
                        return !active;
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
                                if(mImageView != null) {
                                    //mPostableItem.bitmap = bitmap;
                                    mImageView.setImageBitmap(bitmap);
                                }
                            }
                        });
                    }
                };
                downloadUtil.downloadBytes(path);
            }
        }
    }

    class ParentAdapter extends AdapterUtil.NestedRecyclerAdapter{
        private final Context ctx;

        public ParentAdapter(Context context){
            super(context);
            ctx = context;
        }

        @Override
        public int getItemCount(){
            return 3;
        }

        @Override
        public int getLayoutRes(int viewType){
            switch(viewType){
                case 0:
                case 2:
                    return R.layout.horizontal_recycler_view;
                case 1:
                    return R.layout.home_category_pager_block;
            }
            return 0;
        }

        @Override
        public RecyclerView.ViewHolder getViewHolder(View view, int viewType){
            switch (viewType) {
                case 0:
                case 2:
                    return new HorizontalRecyclerHolder(view);
                case 1:
                    return new HomeCategoryHolder(view);
            }
            return null;
        }

        @Override
        public int getItemViewType(int position){
            return position;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position){
            switch(viewHolder.getItemViewType()){
                case 0:
                    Log.i(TAG, "@Position " + position + " -> " + viewHolder);
                    HorizontalRecyclerHolder holder = (HorizontalRecyclerHolder) viewHolder;
                    NestedRecyclerItem recyclerItem = getItemAtPosition(position);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(ctx,
                            LinearLayoutManager.HORIZONTAL, false);
                    holder.childRecyclerView.setLayoutManager(layoutManager);
                    topPicksRecyclerView = holder.childRecyclerView;
                    holder.bindHolder(2+5, recyclerItem.getCategory());
                    //holder.childRecyclerView.setHasFixedSize(true);
                    PostableItemAdapter childAdapter = new PostableItemAdapter(recyclerItem.items);
                    topPicksRecyclerView.setAdapter(childAdapter);
                    break;
                case 1:
                    Log.i(TAG, "@Position " + position + " -> " + viewHolder);
                    HomeCategoryHolder vholder = (HomeCategoryHolder) viewHolder;
                    break;
                case 2:
                    Log.i(TAG, "@Position " + position + " -> " + viewHolder);
                    holder = (HorizontalRecyclerHolder) viewHolder;
                    recyclerItem = getItemAtPosition(position);
                    layoutManager = new LinearLayoutManager(ctx,
                            LinearLayoutManager.HORIZONTAL, false);
                    holder.childRecyclerView.setLayoutManager(layoutManager);
                    recentUploadsRecyclerView = holder.childRecyclerView;
                    holder.bindHolder(3+5, recyclerItem.getCategory());
                    //holder.childRecyclerView.setHasFixedSize(true);
                    childAdapter = new PostableItemAdapter(recyclerItem.items);
                    recentUploadsRecyclerView.setAdapter(childAdapter);
                    break;
            }
        }

        public NestedRecyclerItem getItemAtPosition(int position){
            if(position == 0){
                return topPicksNestedRecycler;
            }else if(position == 2){
                return recentUploadsNestedRecycler;
            }
            return null;
        }
    }

    // collect all Postables with getAll;
    private class PostableFetcher extends FSStoreFetcher<PostableItem>{
        public PostableFetcher(){
            super(AppUtils.MODEL_POSTABLE, PostableItem.class);
        }

        @Override
        public void onStartFetch(){
            Storage.itemsLoading = true;
            Storage.postableItems.clear();
        }

        @Override
        public void onFinish(){
            Storage.itemsLoading = false;
            loadNestedRecyclers();
            // update recycler views
            for(int i=0; i<categories.length;i++){
                downloadCategoryBitmap(i, categoryViews[i]);
            }
        }

        @Override
        public boolean validateCondition(PostableItem item){
            boolean valid = FirebaseHandleUtil.filter(item);
            if(valid){
                for(int i=0; i<categoryTitles.length;i++){
                    if (categoryTitles[i].equals(item.category)){
                        if(categoryBitmapIds[i] == null && item.images != null && !item.images.isEmpty()){
                            categoryBitmapIds[i] = item.images.get(0);
                        }
                        break;
                    }
                }
            }
            return valid;
        }
    }


    @Override
    public void onStart(){
        super.onStart();
        toolbarChanger.setTitleText(getString(R.string.app_name));
        active = true;
        loadNestedRecyclers();
    }

    @Override
    public void onResume(){
        super.onResume();
        for(int i=0; i<categories.length; i++){
            if(categoryViews[i] != null) {
                downloadCategoryBitmap(i, categoryViews[i]);
            }
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        active = false;
    }
}
