package com.scit.stauc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import model.Profile;
import util.AppUtils;
import util.Storage;

public class DrawerNavigationActivity extends SingleFragmentActivity implements AppUtils.ToolbarChanger{
    private static final String EXTRA_FRAGMENT_CODE = "secondary.fragment.code";
    private static final int REQUEST_LOGIN = 78;
    public static Bitmap PROFILE_BITMAP = null;
    public static String PROFILE_EMAIL = null;
    public static String PROFILE_NAME = null;

    public static Intent newIntent(Context context, int fragmentCode){
        Intent i = new Intent(context, DrawerNavigationActivity.class);
        i.putExtra(EXTRA_FRAGMENT_CODE, fragmentCode);
        return i;
    }

    @Override
    public Fragment createFragment(){
        int fragmentCode = getIntent().getIntExtra(EXTRA_FRAGMENT_CODE, 0);
        Fragment fragment = new Fragment();
        if(fragmentCode < 2 && Storage.profile == null) {
            Intent i = LoginActivity.newIntent(this, true);
            startActivityForResult(i, REQUEST_LOGIN);
        }
        switch (fragmentCode){
            case 0:
                fragment = ProfileFragment.newInstance();
                break;
            case 1: // upload
                fragment = UploadFragment.newInstance();
                break;
            case 2:
                fragment = NotificationsFragment.newInstance();
                break;
            case 3:
                fragment = HelpFragment.newInstance();
                break;
            case 4: // contact us
                fragment = ContactUsFragment.newInstance();
                break;
        }
        return fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
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
            if(getSupportFragmentManager().getFragments().size() > 1){
                onBackPressed();
            }else{
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitleText(String s){
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(s);
        }
    }

    @Override
    public void setHeaderImage(Bitmap bitmap) {
        PROFILE_BITMAP = bitmap;
    }

    @Override
    public void setHeaderName(String name) {
        PROFILE_NAME = name;
    }

    @Override
    public void setHeaderEmail(String email) {
        PROFILE_EMAIL = email;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(Storage.profile == null){
            finish();
        }
    }

}
