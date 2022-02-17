package com.scit.stauc;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

public class ImageFlipPagerAdapter extends FragmentStatePagerAdapter{
    private final List<Bitmap> bitmaps;
    private final Context context;

    public ImageFlipPagerAdapter(FragmentManager fm, Context context, List<Bitmap> bitmaps){
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.context = context;
        this.bitmaps = bitmaps;
    }

    @Override
    public int getCount(){
        return bitmaps.size();
    }

    @Override
    public Fragment getItem(int position){
        return new FlippableImageFragment(context, bitmaps.get(position), getScaleType());
    }

    public ImageView.ScaleType getScaleType(){
        return ImageView.ScaleType.CENTER_CROP;
    }

    public static class FlippableImageFragment extends Fragment {
        // shows a single Image and handles its reactions
        private ImageView imageView;
        private final Bitmap mBitmap;
        private final Context mContext;
        private final ImageView.ScaleType mScaleType;

        public FlippableImageFragment(Context context, Bitmap bitmap, ImageView.ScaleType scaleType){
            mContext = context;
            mBitmap = bitmap;
            if(scaleType == null){
                mScaleType = ImageView.ScaleType.CENTER_CROP;
            }else{
                mScaleType = scaleType;
            }
        }

        public ImageView getImageView() {
            return imageView;
        }

        public void onClickItem(){ } // override

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(params);
            imageView.setScaleType(mScaleType);
            imageView.setImageBitmap(mBitmap);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickItem();
                }
            });
            return imageView;
        }

    }

}
