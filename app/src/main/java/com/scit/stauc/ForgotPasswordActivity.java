package com.scit.stauc;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;
import java.util.Random;

import database.FSStoreFetcher;
import database.FSStoreValueChanger;
import model.Profile;
import util.AppUtils;
import util.PreferenceUtils;
import util.Storage;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText mFNameEditor;
    private EditText mLNameEditor;
    private EditText mEmailEditor;
    private EditText mTelephoneEditor;
    private ProgressBar progressBar;
    private Button continueButton;
    private ViewGroup container;

    private String fname = null;
    private String lname = null;
    private String email = null;
    private String telephone = null;
    private Profile inProfile = null;
    private String timeSeedKey;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mFNameEditor = findViewById(R.id.fnameEditor);
        mLNameEditor = findViewById(R.id.lnameEditor);
        mEmailEditor = findViewById(R.id.emailEditor);
        mTelephoneEditor = findViewById(R.id.telEditor);
        continueButton = findViewById(R.id.continue_button);
        container = findViewById(R.id.frame_container);
        progressBar = (ProgressBar) LayoutInflater.from(this).inflate(R.layout.progress_bar_layer,
                container, false);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onContinue();
            }
        });
    }

    private void onContinue(){
        boolean errored = false;
        fname = mFNameEditor.getText().toString().trim();
        lname = mLNameEditor.getText().toString().trim();
        email = mEmailEditor.getText().toString().trim();
        telephone = mTelephoneEditor.getText().toString().trim();

        if(fname.isEmpty()){ mFNameEditor.setError(getString(R.string.empty_field)); errored=true; }
        if(lname.isEmpty()){ mLNameEditor.setError(getString(R.string.empty_field)); errored=true; }
        if(email.isEmpty()){
            mEmailEditor.setError(getString(R.string.empty_field)); errored=true;
        }else if(!AppUtils.isValidEmailString(email)){
            mEmailEditor.setError(getString(R.string.invalid_email));
            errored = true;
        }
        if(telephone.isEmpty()){ mTelephoneEditor.setError(getString(R.string.empty_field)); errored=true; }
        if(errored){ return; }

        ProfileFetcher profileFetcher = new ProfileFetcher();
        profileFetcher.query(null);
    }

    private String getTimeSeedKey(){
         long timeStamp = new Date().getTime();
         int k = new Random().nextInt(9);
         String timeStampString = String.format("%d", timeStamp);
         int l = new Random().nextInt(4);
         return timeStampString.substring(0, l) + k + timeStampString.substring(k, 6);
    }

    private void setEditStates(boolean enabled){
        EditText[] editTexts = {mFNameEditor, mLNameEditor, mEmailEditor, mTelephoneEditor};
        for(EditText editText : editTexts){
            editText.setEnabled(enabled);
        }
    }

    private class ProfileFetcher extends FSStoreFetcher<Profile> {
        private final EditText[] editTexts = {mFNameEditor, mLNameEditor, mEmailEditor, mTelephoneEditor};
        private final String[] values = {fname, lname, email, telephone};
        private final boolean[] validStates = new boolean[editTexts.length];

        public ProfileFetcher(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartFetch(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
            inProfile = null;
            setEditStates(false);
        }

        public void onFind(Profile profile){
            String[] inValues = {profile.getFName(), profile.getLName(),
                    profile.getEmail(), profile.getTelephone()};
            boolean validProfile = true;
            for(int i=0; i<inValues.length;i++){
                if(!inValues[i].equals(values[i])){
                    validStates[i] = false;
                    validProfile = false;
                }else{
                    validStates[i] = true;
                }
            }

            if(validProfile) { inProfile = profile; }
        }

        @Override
        public void onSucceed(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            setEditStates(true);

            if(inProfile != null){
                timeSeedKey = getTimeSeedKey();
                inProfile.setPassword(timeSeedKey);
                ProfileWriter profileWriter = new ProfileWriter();
                profileWriter.set(inProfile.getId(), inProfile);
            }else{
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        for(int i=0; i<validStates.length;i++){
                            if(!validStates[i]){
                                editTexts[i].setError("Enter a correct value!");
                            }else{
                                editTexts[i].setError(null);
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            setEditStates(true);
        }

        @Override
        public boolean validateCondition(Profile profile){
            return profile.getEmail().equals(email);
        }

        @Override
        public boolean endFetchCondition(){
            return inProfile != null;
        }
    }

    private class ProfileWriter extends FSStoreValueChanger<Profile> {
        public ProfileWriter(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }
        @Override
        public void onStartChange(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
            setEditStates(false);
        }

        @Override
        public void onSucceed(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            setEditStates(true);

            Storage.profile = inProfile;
            Storage.profileLoaded = true;
            PreferenceUtils.writeProfile(ForgotPasswordActivity.this, inProfile);
            PreferenceUtils.setRememberStats(ForgotPasswordActivity.this, false);
            // show Dialog here
            View infoDialogView = LayoutInflater.from(ForgotPasswordActivity.this).inflate(
                    R.layout.notification_dialog_view, container, false);
            TextView titleTextView = infoDialogView.findViewById(R.id.title_text_view);
            TextView contentTextView = infoDialogView.findViewById(R.id.content_text_view);
            Button actionButton = infoDialogView.findViewById(R.id.notification_action_view);
            actionButton.setVisibility(View.GONE);

            titleTextView.setText("Info");
            contentTextView.setText("A new password : " + timeSeedKey + " has been set." +
                    "You can set a new password from the Edit Profile section on your Profile page.");

            AlertDialog notifyDialog = new AlertDialog.Builder(ForgotPasswordActivity.this)
                    .setView(infoDialogView)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startNavigationActivity();
                        }
                    })
                    .create();
            notifyDialog.show();

            notifyDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    startNavigationActivity();
                }
            });

        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            setEditStates(true);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ForgotPasswordActivity.this, "An error occurred!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startNavigationActivity(){
        Intent intent = CustomerNavigationActivity.newIntent(ForgotPasswordActivity.this); // id
        startActivity(intent);
    }

}
