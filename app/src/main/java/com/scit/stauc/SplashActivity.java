package com.scit.stauc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.ArrayList;

import database.FSStoreFetcher;
import model.Profile;
import util.AdsHandler;
import util.AppUtils;
import util.PreferenceUtils;
import util.Storage;
import util.TimeUtil;

public class SplashActivity extends AppCompatActivity {
    private boolean rememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashfile);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {

            }
        });
        rememberMe = PreferenceUtils.getRememberState(this);

        TimeUtil.getSiteTime();
        // maintain Handler until timeSet

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(rememberMe) {
                    Storage.profile = PreferenceUtils.readProfile(SplashActivity.this);
                    if(Storage.profile == null){
                        PreferenceUtils.setRememberStats(SplashActivity.this, false);
                        Intent intent = CustomerNavigationActivity.newIntent(SplashActivity.this);
                        startActivity(intent);
                        finish();
                    }else {
                        ProfileFetcher profileFetcher = new ProfileFetcher();
                        profileFetcher.query(null);
                    }
                }
                Intent intent = CustomerNavigationActivity.newIntent(SplashActivity.this);
                startActivity(intent);
                finish();
            }
        }, 300);
    }

    private class ProfileFetcher extends FSStoreFetcher<Profile> {
        private boolean found;

        public ProfileFetcher(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartFetch(){
            found = false;
        }

        @Override
        public void onFind(Profile profile){
            Storage.profile = profile;
            found = true;
            Storage.profileLoaded = true;
        }

        @Override
        public boolean validateCondition(Profile profile){
            if(profile == null || Storage.profile == null){ return false; }
            return profile.getEmail().equals(Storage.profile.getEmail());
        }

        @Override
        public boolean endFetchCondition(){
            return found;
        }
    }
}