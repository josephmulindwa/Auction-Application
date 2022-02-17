package com.scit.stauc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

import model.Message;
import model.MessageBin;
import model.Notification;
import util.AdViewerInterface;
import util.AdsHandler;
import util.AppUtils;
import util.DownloadUtil;
import util.MessageRoomListener;
import util.NotificationListener;
import util.PreferenceUtils;
import util.Storage;
import util.TimeUtil;
import view.BadgeDrawableArrowDrawable;

public class CustomerNavigationActivity extends AppCompatActivity implements AppUtils.ToolbarChanger,
        AdViewerInterface {
    private static final String TAG = "CustomerNavActivity";
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;
    private View headerView;
    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private MessageRoomListener messageRoomListener;
    private NotificationListener notificationListener;
    private AdsHandler adsHandler = new AdsHandler();
    private boolean cancelDownloads = false;

    public static Intent newIntent(Context context){
        return new Intent(context, CustomerNavigationActivity.class);
    }

    public void show(Activity activity){
        adsHandler.initialize(activity);
    }

    @Override
    public void setHeaderImage(Bitmap bitmap){
        Log.i("RegisterActivity", "header:"+headerView);
        if(headerView == null){ return; }
        ImageView imageView = headerView.findViewById(R.id.header_image_view);
        Log.i("RegisterActivity", "bitmap == null:"+(bitmap == null));
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() { imageView.setImageBitmap(bitmap); }
        });
    }

    @Override
    public void setHeaderName(String name) {
        if(headerView == null){ return; }
        TextView textView = headerView.findViewById(R.id.username_text_view);
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() { textView.setText(name); }
        });
    }

    @Override
    public void setHeaderEmail(String email) {
        if(headerView == null){ return; }
        TextView textView = headerView.findViewById(R.id.useremail_text_view);
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() { textView.setText(email); }
        });
    }

    private void showAds(){
        long delay = 300000; // 5 mins
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                show(CustomerNavigationActivity.this);
                //adsHandler = new AdsHandler();
                //adsHandler.initialize(CustomerNavigationActivity.this);
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_navigation);
        Toolbar toolbar = findViewById(R.id.m_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));

        adsHandler = new AdsHandler();
        adsHandler.initialize(this);
        // showAds()

        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        headerView = navigationView.getHeaderView(0);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        fragmentManager = getSupportFragmentManager();
        notificationListener = new NotificationListener(null, null){
            @Override
            public void onFinishFetch(ArrayList<Notification> notifications){
                boolean withUnseen = false;
                for(Notification notification : notifications){
                    if(!notification.seen){
                        withUnseen = true;
                        break;
                    }
                }
                boolean finalWithUnseen = withUnseen;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        /*BadgeDrawableArrowDrawable badgeDrawable = new BadgeDrawableArrowDrawable(
                                getSupportActionBar().getThemedContext());
                        actionBarDrawerToggle.setHomeAsUpIndicator(badgeDrawable);*/
                        if(finalWithUnseen){
                            ViewGroup linearContainer = findViewById(R.id.linear_container);
                            View redBadge = LayoutInflater.from(CustomerNavigationActivity.this)
                                    .inflate(R.layout.badge_empty, linearContainer, false);
                            navigationView.getMenu().findItem(R.id.action_nav_notifications)
                                    .setActionView(redBadge);
                        }else{
                            navigationView.getMenu().findItem(R.id.action_nav_notifications).setActionView(null);
                        }
                    }
                });
            }
        };
        notificationListener.fetchNotifications();

        messageRoomListener = new MessageRoomListener(null, null){
            @Override
            public void onFinishFetch(ArrayList<MessageBin> bins){
                if(Storage.profile == null){ return; }
                boolean withUnread = false;
                for(MessageBin messageBin : bins){
                    Message topMsg = messageBin.getTopMessage();
                    if(topMsg != null && !topMsg.getRead() && !topMsg.getSender().equals(Storage.profile.getId())){
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                bottomNavigationView.getOrCreateBadge(R.id.action_nav_messages);
                            }
                        });
                        withUnread = true;
                        break;
                    }
                }
                if(!withUnread){
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            bottomNavigationView.removeBadge(R.id.action_nav_messages);
                        }
                    });
                }
            }
        };
        if(Storage.profile != null) {
            messageRoomListener.fetchMessages();
        }

        int navId = PreferenceUtils.getCustomerNavigationId(this);
        int[] validIds = {R.id.action_nav_home, R.id.action_nav_search, R.id.action_nav_bids,
            R.id.action_nav_messages};
        int id_ = 0;
        for(int vid : validIds){
            if(vid == navId){
                id_ = vid;
                break;
            }
        }

        Log.i(TAG, "navId loaded="+navId+", Id=" + R.id.action_nav_search);
        if(id_ == 0){
            navId = R.id.action_nav_home;
            PreferenceUtils.setCustomerNavigationId(this, navId);
        }else {
            bottomNavigationView.setSelectedItemId(navId);
        }
        Fragment fragment = getMenuFragmentById(navId);
        fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                final int itemId = item.getItemId();
                @NonNull Fragment fragment = getMenuFragmentById(itemId);
                PreferenceUtils.setCustomerNavigationId(CustomerNavigationActivity.this, itemId);
                fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
                return true;
            }
        });
        bottomNavigationView.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {

            }
        });

        /*bottomNavigationView.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {

            }
        });*/

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) { }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) { }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) { }

            @Override
            public void onDrawerStateChanged(int newState) {
                AppUtils.hideKeyboard(CustomerNavigationActivity.this, navigationView);
                changeLoginMenuItemState();
            }
        });

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        final int itemId = item.getItemId();
                        if(itemId == R.id.action_nav_logout){
                            Intent intent = LoginActivity.newIntent(CustomerNavigationActivity.this);
                            CustomerNavigationActivity.this.finishAffinity();
                            PreferenceUtils.clearPreferences(CustomerNavigationActivity.this);
                            Storage.reset();
                            startActivity(intent);
                        }else{
                            if(itemId == R.id.action_nav_messages){
                                bottomNavigationView.removeBadge(R.id.action_nav_messages);
                            }
                            Intent i = DrawerNavigationActivity.newIntent(
                                    CustomerNavigationActivity.this, getFragmentCodeById(itemId)
                            );
                            startActivity(i);
                        }
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return false;
                    }
                }
        );

        if(Storage.profile != null){
            setHeaderName(Storage.profile.getName());
            setHeaderEmail(Storage.profile.getEmail());
            Log.i("RegisterActivity", "imagePath:"+Storage.profile.imagePath);
            if(Storage.profile.imagePath != null) {
                ProfileImageFetcher profileImageFetcher = new ProfileImageFetcher();
                profileImageFetcher.downloadBytes(Storage.profile.imagePath);
            }
        }
    }

    private Fragment getMenuFragmentById(final int id){
        Fragment fragment = null;
        if (id == R.id.action_nav_home) {
            fragment = HomeFragment.newInstance();
        }else if(id == R.id.action_nav_search) {
            fragment = SearchFragment.newInstance();
        }else if(id == R.id.action_nav_bids) {
            fragment = BidsViewFragment.newInstance();
        }else if(id == R.id.action_nav_messages){
            fragment = MessageRoomFragment.newInstance();
        }
        return fragment;
    }

    private int getFragmentCodeById(final int id){
        if(id == R.id.action_nav_profile){
            return 0;
        }else if(id == R.id.action_nav_upload){
            return 1;
        }else if(id == R.id.action_nav_notifications){
            return 2;
        }else if(id == R.id.action_nav_help){
            return 3;
        }else if(id == R.id.action_nav_contact){
            return 4;
        }
        return -1;
    }

    @Override
    public void onBackPressed(){
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        /*if(!PreferenceUtils.getRememberState(this)){
            Intent i = LoginActivity.newIntent(this);
            startActivity(i);
        }*/
        finishAffinity();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(actionBarDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setTitleText(String title){
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private class ProfileImageFetcher extends DownloadUtil {

        @Override
        public boolean endDownloadCondition() {
            return cancelDownloads;
        }

        @Override
        public void onFinish(byte[] bytes, boolean success){
            if(!success){ return; }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            setHeaderImage(bitmap);
        }
    }

    public void changeLoginMenuItemState(){
        Menu menu = navigationView.getMenu();
        MenuItem menuItem = menu.findItem(R.id.action_nav_logout);
        if(Storage.profile == null){
            menuItem.setTitle(R.string.login);
        }else{
            menuItem.setTitle(R.string.logout);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        TimeUtil.getSiteTime();
        cancelDownloads = false;
        if(DrawerNavigationActivity.PROFILE_BITMAP != null){
            setHeaderImage(DrawerNavigationActivity.PROFILE_BITMAP);
            DrawerNavigationActivity.PROFILE_BITMAP = null;
        }
        if(DrawerNavigationActivity.PROFILE_EMAIL != null){
            setHeaderEmail(DrawerNavigationActivity.PROFILE_EMAIL);
            DrawerNavigationActivity.PROFILE_EMAIL = null;
        }
        if(DrawerNavigationActivity.PROFILE_NAME != null){
            setHeaderName(DrawerNavigationActivity.PROFILE_NAME);
            DrawerNavigationActivity.PROFILE_NAME = null;
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        cancelDownloads = true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(messageRoomListener != null){
            messageRoomListener.stopListening();
        }
        if(notificationListener != null){
            notificationListener.stopListening();
        }
    }

}