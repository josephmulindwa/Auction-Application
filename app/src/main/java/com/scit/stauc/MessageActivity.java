package com.scit.stauc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import util.AppUtils;
import util.Storage;

public class MessageActivity extends SingleFragmentActivity implements AppUtils.ToolbarChanger {
    private static final String TAG = "MessageActivity";
    private static final String EXTRA_RECEIPIENT = "MessageActivity.Receipient";
    public static final String EXTRA_INFO_TEXT = "MessageActivity.ShowInfo";
    private String receipient = null;

    public static void sendMessage(Context context, String receipientId, String info){
        Intent intent = MessageActivity.newIntent(context, receipientId);
        if(info != null) {
            intent.putExtra(EXTRA_INFO_TEXT, info);
        }
        context.startActivity(intent);
    }

    public static Intent newIntent(Context context, String receipient){
        Intent i = new Intent(context, MessageActivity.class);
        i.putExtra(EXTRA_RECEIPIENT, receipient);
        return i;
    }

    @Override
    public Fragment createFragment(){
        receipient = getIntent().getStringExtra(EXTRA_RECEIPIENT);
        String infoText = getIntent().getStringExtra(EXTRA_INFO_TEXT);
        return MessageFragment.newInstance(receipient, infoText);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
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

    @Override
    public void setTitleText(String s){
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(s);
    }

    @Override
    public void setHeaderImage(Bitmap bitmap) { }

    @Override
    public void setHeaderName(String name) { }

    @Override
    public void setHeaderEmail(String email) { }

}
