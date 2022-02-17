package com.scit.stauc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import model.PostableItem;
import util.Storage;

public class ItemDetailActivity extends SingleFragmentActivity{

    public static Intent newIntent(Context context, PostableItem postableItem){
        Intent intent = new Intent(context, ItemDetailActivity.class);
        Storage.postableItem = postableItem;
        return intent;
    }

    @Override
    public Fragment createFragment(){
        return ItemDetailFragment.newInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
        //        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setBackgroundDrawable(new ColorDrawable(
                    Color.HSVToColor(100, new float[]{0f, 0f, .5f})));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
