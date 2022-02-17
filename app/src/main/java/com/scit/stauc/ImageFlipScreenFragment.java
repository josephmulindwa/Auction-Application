package com.scit.stauc;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import java.util.List;

public class ImageFlipScreenFragment extends Fragment {
    private ViewPager viewPager;
    private ImageView imageView;
    private final Context mContext;
    private final ImageView.ScaleType mScaleType;
    private List<Bitmap> bitmaps;

    public ImageFlipScreenFragment(Context context, List<Bitmap> bitmaps, ImageView.ScaleType scaleType){
        mContext = context;
        mScaleType = scaleType;
        this.bitmaps = bitmaps;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        viewPager = new ViewPager(mContext);
        viewPager.setLayoutParams(params);
        viewPager.setId(View.generateViewId());
        ImageAdapter imageAdapter = new ImageAdapter(getFragmentManager(), mContext, bitmaps);
        viewPager.setAdapter(imageAdapter);
        viewPager.setCurrentItem(getStartPosition());
        return viewPager;
    }

    public int getStartPosition(){ // Override this
        return 0;
    }

    public class ImageAdapter extends ImageFlipPagerAdapter{
        public ImageAdapter(FragmentManager fm, Context context, List<Bitmap> bitmaps){
            super(fm, context, bitmaps);
        }

        @Override
        public ImageView.ScaleType getScaleType(){
            return mScaleType;
        }
    }

}
