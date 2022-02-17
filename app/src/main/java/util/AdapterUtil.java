package util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import com.scit.stauc.ItemDetailActivity;

import model.PostableItem;

import com.scit.stauc.MyUploadsActivity;
import com.scit.stauc.R;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterUtil {
    private static final String TAG = "AdapterUtil";

    // abstract adapter that can hold any other viewholders of different types
    // just define an external viewholder & its binding and add it to this class
    public abstract static class NestedRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private final Context ctx;

        public NestedRecyclerAdapter(Context context){
            ctx = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
            View view = LayoutInflater.from(ctx).inflate(
                    getLayoutRes(viewType),
                    parent,
                    false
            );
            return getViewHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position){
            ;
        }

        @Override
        public abstract int getItemCount();

        @LayoutRes
        public abstract int getLayoutRes(int viewType);

        public abstract RecyclerView.ViewHolder getViewHolder(View view, int viewType);

        // return List associated with viewType : override or use custom
        public <T> List<T> getItems(int viewType){
            return null;
        };
    }

    public static class PostableItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final Context context;
        private final TextView titleTextView;
        private final TextView priceTextView;
        private final ImageView photoView;
        private final TextView countdownTextView;
        private final TextView timeFormatTextView;
        private PostableItem mPostableItem;
        private final PostableItemAdapter adapter;
        private long endTime = -1;
        private long startTime = -1;
        private boolean clickable = true;
        private final long REPOST_DELAY = 1000;

        public PostableItemHolder(View view, Context context, PostableItemAdapter adapter){
            super(view);
            this.context = context;
            this.adapter = adapter;
            titleTextView = itemView.findViewById(R.id.title_text_view);
            priceTextView = itemView.findViewById(R.id.price_text_view);
            photoView = itemView.findViewById(R.id.item_photo);
            countdownTextView = itemView.findViewById(R.id.auction_count_view);
            timeFormatTextView = itemView.findViewById(R.id.auction_time_format_view);
            photoView.setOnClickListener(this);

            HandlerThread timerThread = new HandlerThread("AdapterUtil.Timer");
            if(!timerThread.isAlive()){
                timerThread.start();
            }
            Handler timeHandler = new Handler(timerThread.getLooper());
            timeHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(endTime > 0) {
                        long[] elapseData = TimeUtil.getTimeDifference(TimeUtil.getCurrentTimeStamp(), endTime);
                        if(elapseData[0] < 0 || elapseData[1] < 0 || elapseData[2] < 0 || elapseData[3] < 0){
                            clickable = false;
                            showAsNotBiddable();
                            FirebaseHandleUtil.performBid(mPostableItem, mPostableItem.topBid);
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
                                }
                            });
                        }
                    }
                    timeHandler.postDelayed(this, REPOST_DELAY);
                }
            }, REPOST_DELAY);
        }

        public void bindPostable(PostableItem item){
            mPostableItem = item;
            titleTextView.setText(item.name);
            int usePrice = item.topBid == 0 ? item.price : item.topBid;
            String bidString = String.format(Locale.getDefault(), "UGX %d", usePrice);
            priceTextView.setText(bidString);
            if(item.images != null && !item.images.isEmpty()){
                downloadBitmap(item.images.get(0));
            }
            endTime = item.endTimeStamp;
        }

        public void downloadBitmap(String path){
            if(mPostableItem.bitmap != null){
                photoView.setImageBitmap(mPostableItem.bitmap);
                return;
            }
            DownloadUtil downloadUtil = new DownloadUtil(){
                @Override
                public void onStart(){

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
                            mPostableItem.bitmap = bitmap;
                            photoView.setImageBitmap(bitmap);
                        }
                    });
                }
            };
            downloadUtil.downloadBytes(path);
        }

        private void showAsNotBiddable(){
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if(context == null){
                        return;
                    }
                    TextView auctionEndLabel = itemView.findViewById(R.id.auction_end_label_view);
                    auctionEndLabel.setText(R.string.auction_ended);
                    auctionEndLabel.setTextColor(context.getResources().getColor(R.color.red_american_rose_920));
                    countdownTextView.setText(null);
                    timeFormatTextView.setText(null);
                }
            });
        }

        private void showAsBiddable(){
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if(context == null){
                        return;
                    }
                    TextView auctionEndLabel = itemView.findViewById(R.id.auction_end_label_view);
                    auctionEndLabel.setTextColor(context.getResources().getColor(R.color.gray_battleship));
                }
            });
        }

        @Override
        public void onClick(View view){
            if(mPostableItem.id == null){ return; }
            if(!clickable){
                if(Storage.profile != null && mPostableItem.owner.equals(Storage.profile.getId())){
                    Intent intent = MyUploadsActivity.newIntent(context);
                    context.startActivity(intent);
                    return;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(context == null){ return; }
                        Toast.makeText(context, R.string.item_not_available, Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            onPostableItemClick(context, mPostableItem);
        }
    }

    public static void onPostableItemClick(Context context, PostableItem item){
        Intent intent = ItemDetailActivity.newIntent(context, item);
        context.startActivity(intent);
    }

    public static class PostableItemAdapter extends RecyclerView.Adapter<PostableItemHolder>{
        private final Context context;
        public List<PostableItem> mItems;

        public PostableItemAdapter(Context context, List<PostableItem> mItems){
            this.context = context;
            this.mItems = mItems;
        }

        @Override
        public PostableItemHolder onCreateViewHolder(ViewGroup parent, int itemType){
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.postable_recycler_item, parent, false);
            DisplayMetrics displayMetrics = AppUtils.getDeviceDisplayMetrics(context);
            v.getLayoutParams().height = (int) (displayMetrics.heightPixels * 0.54);
            return new PostableItemHolder(v, context, this);
        }

        @Override
        public void onBindViewHolder(PostableItemHolder holder, int position){
            holder.bindPostable(mItems.get(position));
        }

        @Override
        public int getItemCount(){
            return mItems.size();
        }
    }

}
