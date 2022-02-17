package com.scit.stauc;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import util.AppUtils;

public class ImageFlipScreenActivity extends SingleFragmentActivity{
    private static final String EXTRA_POSITION = "ImageFlipScreen.Position";

    public static Intent newIntent(Context context, int position){
        Intent i = new Intent(context, ImageFlipScreenActivity.class);
        i.putExtra(EXTRA_POSITION, position);
        return i;
    }

    @Override
    public Fragment createFragment(){
        int pos = getIntent().getIntExtra(EXTRA_POSITION, 0);
        return new FullScreenFragment(this, new ArrayList<>(),
                ImageView.ScaleType.CENTER_CROP, pos);
    }

    public static class FullScreenFragment extends ImageFlipScreenFragment{
        private final int position;

        public FullScreenFragment(Context context, List<Bitmap> bitmaps,
                                  ImageView.ScaleType scaleType, int position) {
            super(context, bitmaps, scaleType);
            this.position = position;
        }

        @Override
        public int getStartPosition(){
            return position;
        }
    }

}
