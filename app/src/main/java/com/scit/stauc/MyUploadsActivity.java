package com.scit.stauc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import model.PostableItem;
import util.AppUtils;

public class MyUploadsActivity extends SingleFragmentActivity implements AppUtils.ToolbarChanger{

    public static Intent newIntent(Context context){
        return new Intent(context, MyUploadsActivity.class);
    }

    @Override
    public Fragment createFragment(){
        return MyUploadsFragment.newInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        }
        setTitleText("My Uploads");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitleText(String s){
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null) {
            actionBar.setTitle(s);
        }
    }

    @Override
    public void setHeaderImage(Bitmap bitmap) {

    }

    @Override
    public void setHeaderName(String name) {

    }

    @Override
    public void setHeaderEmail(String email) {

    }

}
