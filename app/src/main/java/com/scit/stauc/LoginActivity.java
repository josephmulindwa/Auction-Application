package com.scit.stauc;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;

import java.util.ArrayList;

import database.FSStoreFetcher;
import model.Profile;
import util.AppUtils;
import util.PreferenceUtils;
import util.Storage;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final String EXTRA_FINISH = "LoginActivity.finish";
    public static int REQUEST_REGISTER_FINISH = 4;
    private EditText emailEditor;
    private EditText passwordEditor;
    private Button loginButton;
    private TextView forgotPassword;
    private CheckBox rememberMeCheckBox;
    private boolean finishOnLogin = false;
    private ProgressBar progressBar;
    private ViewGroup container;
    private String email = null;
    private String password = null;

    public static Intent newIntent(Context context){
        return new Intent(context, LoginActivity.class);
    }

    public static Intent newIntent(Context context, boolean finishOnLogin){
        Intent i = new Intent(context, LoginActivity.class);
        i.putExtra(EXTRA_FINISH, finishOnLogin);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.hide();
        }

        finishOnLogin = getIntent().getBooleanExtra(EXTRA_FINISH, false);

        container = findViewById(R.id.frame_container);
        progressBar = (ProgressBar)LayoutInflater.from(this).
                inflate(R.layout.progress_bar_layer, container, false);
        emailEditor = findViewById(R.id.login_email_editor);
        passwordEditor = findViewById(R.id.login_password_editor);
        forgotPassword = findViewById(R.id.forgot_password);
        rememberMeCheckBox = findViewById(R.id.remember_me_check_box);

        passwordEditor.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_ACTION_GO){
                    login();
                    return true;
                }
                return false;
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(i);
            }
        });

        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                login();
            }
        });
        TextView signIn = findViewById(R.id.sign_in_text_view);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = RegisterActivity.newIntent(LoginActivity.this, finishOnLogin);
                startActivityForResult(intent, REQUEST_REGISTER_FINISH);
                clearFields();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != Activity.RESULT_OK){
            return;
        }
        if(data != null && requestCode == REQUEST_REGISTER_FINISH){
            finishOnLogin = data.getBooleanExtra(RegisterActivity.EXTRA_REGISTER_FINISH, false);
            if(finishOnLogin){
                finish();
            }
        }
    }

    private void login(){
        email = emailEditor.getText().toString();
        password = passwordEditor.getText().toString();
        boolean errored = false;
        if(email.isEmpty()){
            emailEditor.setError(getString(R.string.empty_field));
            errored = true;
        }else if(!AppUtils.isValidEmailString(email)){
            emailEditor.setError(getString(R.string.invalid_email));
            errored = true;
        }
        if(password.isEmpty()){
            passwordEditor.setError(getString(R.string.empty_field));
            errored = true;
        }
        if(errored) { return ; }

        ProfileFetcher profileFetcher = new ProfileFetcher();
        profileFetcher.query(null);
    }

    public void clearFields(){
        // called by children
        emailEditor.setText(null);
        emailEditor.setError(null);
        passwordEditor.setText(null);
        passwordEditor.setError(null);
        rememberMeCheckBox.setChecked(false);
    }

    private class ProfileFetcher extends FSStoreFetcher<Profile>{
        private boolean emailExists;
        private boolean found;

        public ProfileFetcher(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartFetch(){
            container.removeView(progressBar);
            container.addView(progressBar);
            loginButton.setEnabled(false);
            found = false;
            emailExists = false;
        }

        @Override
        public void onFinish(){
            // something that happens on all(success & fail)
            container.removeView(progressBar);
            loginButton.setEnabled(true);
        }

        @Override
        public void onSucceed(){
            if(!found){
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(!emailExists) {
                            emailEditor.setError(getString(R.string.invalid_email));
                        }else {
                            passwordEditor.setError(getString(R.string.invalid_password));
                        }
                    }
                });
            }
        }

        @Override
        public void onFail(){

        }

        @Override
        public void onFind(Profile profile){
            found = true;
            Storage.profile = profile;
            Storage.profileLoaded = true;
            PreferenceUtils.writeProfile(LoginActivity.this, profile);
            PreferenceUtils.setRememberStats(LoginActivity.this, rememberMeCheckBox.isChecked());
            if (finishOnLogin) {
                finish();
            } else {
                Intent intent = CustomerNavigationActivity.newIntent(LoginActivity.this);
                startActivity(intent);
            }
        }

        @Override
        public boolean validateCondition(Profile profile){
            if(profile.getEmail().equals(email)){
                emailExists = true;
            }
            return profile.getPassword().equals(password) && profile.getEmail().equals(email);
        }

        @Override
        public boolean endFetchCondition(){
            return found;
        }
    }

}