package com.scit.stauc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import database.FSStoreFetcher;
import database.FSStoreValueChanger;
import database.StorageHandler;
import model.Profile;
import util.AppUtils;
import util.DownloadUtil;
import util.PreferenceUtils;
import util.Storage;

public class RegisterActivity extends AppCompatActivity {
    private static final String DIALOG_ID = "RegisterDialogId";
    private static final String TAG = "RegisterActivity";
    private static final String EXTRA_FINISH = "RegisterActivity.finish";
    public static final String EXTRA_REGISTER_FINISH = "Login_Register.finish";
    public static final String EXTRA_HAS_PROFILE = "Register.HasProfile";
    private static final int REQUEST_PERMISSIONS = 5;
    private static final int REQUEST_PHOTO = 6;
    private EditText mFNameEditor;
    private EditText mLNameEditor;
    private EditText mEmailEditor;
    private EditText mPasswordEditor;
    private EditText mTelephoneEditor;
    private ImageView mImagePhotoView;
    private ProgressBar progressBar;
    private Button continueButton;
    private ViewGroup container;
    private ArrayList<Profile> profiles;
    private String email;
    private Profile mProfile;
    private boolean finishOnLogin;
    private boolean hasProfile;
    private volatile boolean photoChanged = false;
    private Bitmap photo = null;
    private File photoFile = null;
    private Intent photoIntent = null;
    private boolean canTakePhoto = false;

    public static Intent newIntent(Context context){
        return new Intent(context, RegisterActivity.class);
    }

    public static Intent newIntent(Context context, boolean finishOnLogin){
        Intent i = new Intent(context, RegisterActivity.class);
        i.putExtra(EXTRA_FINISH, finishOnLogin);
        return i;
    }

    public static Intent newIntent(Context context, Profile profile){
        Intent i = new Intent(context, RegisterActivity.class);
        i.putExtra(EXTRA_HAS_PROFILE, true);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.hide();
        }

        hasProfile = getIntent().getBooleanExtra(EXTRA_HAS_PROFILE, false);
        finishOnLogin = getIntent().getBooleanExtra(EXTRA_FINISH, false);
        container = findViewById(R.id.frame_container);
        progressBar = (ProgressBar) LayoutInflater.from(this).inflate(R.layout.progress_bar_layer,
                container, false);

        mFNameEditor = findViewById(R.id.fnameEditor);
        mLNameEditor = findViewById(R.id.lnameEditor);
        mEmailEditor = findViewById(R.id.emailEditor);
        mPasswordEditor = findViewById(R.id.passwordEditor);
        mTelephoneEditor = findViewById(R.id.telEditor);
        mImagePhotoView = findViewById(R.id.profile_photo);
        continueButton = findViewById(R.id.continue_button);

        photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        canTakePhoto = (photoIntent.resolveActivity(getPackageManager()) != null);
        photoFile = getPhotoFile();
        Uri uri = FileProvider.getUriForFile(RegisterActivity.this,
                getApplicationContext().getPackageName() + ".provider",
                photoFile);
        photoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        photoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

        if(hasProfile){
            mProfile = Storage.profile;
            if(!Storage.profileLoaded){
                CurrentProfileFetcher currentProfileFetcher = new CurrentProfileFetcher();
                currentProfileFetcher.query(null);
            }else {
                fillFields();
            }
        }else{
            mProfile = null;
        }

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onContinue();
            }
        });

        mPasswordEditor.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (mPasswordEditor.getRight() -
                            mPasswordEditor.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        //Toast.makeText(RegisterActivity.this, "C:"+mPasswordEditor.getInputType(), Toast.LENGTH_SHORT).show();
                        int currentInputType = mPasswordEditor.getInputType();
                        if(currentInputType == InputType.TYPE_TEXT_VARIATION_PASSWORD || currentInputType == 129) {
                            mPasswordEditor.setInputType(InputType.TYPE_CLASS_TEXT);
                        }else {
                            mPasswordEditor.setInputType(129);
                            mPasswordEditor.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        mImagePhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(RegisterActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
                    }
                }else{
                    if(canTakePhoto){
                        if(photo != null) {
                            startPhotoEditDialog();
                        }else{
                            if(mProfile.imagePath == null) {
                                startActivityForResult(photoIntent, REQUEST_PHOTO);
                            } // else still loading
                        }
                    }
                }
            }
        });
    }

    private void startPhotoEditDialog(){
        if(photoIntent == null || !canTakePhoto){
            return;
        }
        View view = LayoutInflater.from(this).inflate(
                R.layout.image_edit_view_dialog, container, false
        );
        ImageView photoView = view.findViewById(R.id.image_photo_view);
        ImageView takePhotoIcon = view.findViewById(R.id.capture_photo_icon);
        photoView.setImageBitmap(photo);

        AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        alertDialog.show();

        takePhotoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(photoIntent, REQUEST_PHOTO);
                alertDialog.dismiss();
            }
        });
    }

    private File getPhotoFile(){
        File externalFilesDir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if(externalFilesDir == null){
            return null;
        }
        File file;
        do { // generate file until its new
            Date date = new Date();
            Random random = new Random();
            String photoPath = String.format(Locale.getDefault(), "IMG_%d_%d",
                    date.getTime(), random.nextInt(100000));
            file = new File(externalFilesDir, photoPath);
        }while (file.exists());
        return file;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants){
        if(requestCode == REQUEST_PERMISSIONS){
            boolean granted = true;
            for (int i=0; i<permissions.length; i++){
                if(grants[i] != PackageManager.PERMISSION_GRANTED){
                    granted = false;
                    break;
                }
            }

            if(granted && canTakePhoto){
                if(photo != null) {
                    startPhotoEditDialog();
                }else{
                    if(mProfile.imagePath == null) {
                        startActivityForResult(photoIntent, REQUEST_PHOTO);
                    } // else still loading
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != Activity.RESULT_OK){
            return;
        }
        if(requestCode == REQUEST_PHOTO){
            Log.i(TAG, "returned from capture intent");
            if(photoFile == null || !photoFile.exists()){
                Log.i(TAG, "image not set");
                photo = null;
            }else{
                Log.i(TAG, "image successfully set");
                photo = BitmapFactory.decodeFile(photoFile.getPath());
                photo = AppUtils.getCroppedBitmap(photo, 400, 400);
                mImagePhotoView.setImageBitmap(photo);
                photoChanged = true;
            }
        }
    }

    private void fillFields(){
        if(mProfile == null){
            return;
        }
        mEmailEditor.setText(mProfile.getEmail());
        mEmailEditor.setEnabled(false);
        mEmailEditor.setTextColor(getResources().getColor(R.color.gray_battleship));
        mFNameEditor.setText(mProfile.getFName());
        mLNameEditor.setText(mProfile.getLName());
        mPasswordEditor.setText(mProfile.getPassword()); // setInputType
        mTelephoneEditor.setText(mProfile.getTelephone());
        continueButton.setText(R.string.save);
        if(mProfile.imagePath != null) {
            ProfileImageFetcher profileImageFetcher = new ProfileImageFetcher();
            profileImageFetcher.downloadBytes(mProfile.imagePath);
        }
    }

    private void onContinue(){
        boolean errored = false;
        String fname = mFNameEditor.getText().toString().trim();
        String lname = mLNameEditor.getText().toString().trim();
        email = mEmailEditor.getText().toString().trim();
        String password = mPasswordEditor.getText().toString().trim();
        String telephone = mTelephoneEditor.getText().toString().trim();
        if(fname.isEmpty()){ mFNameEditor.setError(getString(R.string.empty_field)); errored=true; }
        if(lname.isEmpty()){ mLNameEditor.setError(getString(R.string.empty_field)); errored=true; }
        if(email.isEmpty()){
            mEmailEditor.setError(getString(R.string.empty_field)); errored=true;
        }else if(!AppUtils.isValidEmailString(email)){
            mEmailEditor.setError(getString(R.string.invalid_email));
            errored = true;
        }
        if(password.isEmpty()){ mPasswordEditor.setError(getString(R.string.empty_field)); errored=true; }
        if(telephone.isEmpty()){ mTelephoneEditor.setError(getString(R.string.empty_field)); errored=true; }
        if(errored){ return; }

        if(!hasProfile) {
            mProfile = new Profile(fname, lname, email, password, telephone);
            uploadProfile();
        }else{
            if(fname.equals(mProfile.getFName()) && lname.equals(mProfile.getLName()) &&
            password.equals(mProfile.getPassword()) && telephone.equals(mProfile.getTelephone()) && !photoChanged){
                //Intent i = CustomerNavigationActivity.newIntent(RegisterActivity.this);
                //startActivity(i);
                finish();
            }else{
                mProfile = new Profile(fname, lname, email, password, telephone);
                uploadProfile();
            }
        }
    }

    private class ProfileFetcher extends FSStoreFetcher<Profile> {
        public ProfileFetcher(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartFetch(){
            container.removeView(progressBar);
            container.addView(progressBar);
            profiles = new ArrayList<>();
        }

        @Override
        public void onSucceed(){
            container.removeView(progressBar);
            if(profiles.isEmpty()){
                ProfileWriter profileWriter = new ProfileWriter();
                profileWriter.set(mProfile.getId(), mProfile);
            }else{
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() { mEmailEditor.setError(getString(R.string.email_exists)); }
                });
            }
        }

        @Override
        public void onFail(){
            container.removeView(progressBar);
        }

        @Override
        public boolean validateCondition(Profile profile){
            return profile.getEmail().equals(email);
        }

        @Override
        public boolean endFetchCondition(){
            return !profiles.isEmpty();
        }
    }

    private class ProfileWriter extends FSStoreValueChanger<Profile>{
        public ProfileWriter(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }
        @Override
        public void onStartChange(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
        }

        @Override
        public void onSucceed(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            Storage.profile = mProfile;
            Storage.profileLoaded = true;
            PreferenceUtils.writeProfile(RegisterActivity.this, mProfile);
            PreferenceUtils.setRememberStats(RegisterActivity.this, true);
            if(finishOnLogin){
                Intent data = new Intent();
                data.putExtra(EXTRA_REGISTER_FINISH, finishOnLogin);
                setResult(Activity.RESULT_OK, data);
                finish();
            }else {
                //Intent intent = CustomerNavigationActivity.newIntent(RegisterActivity.this); // id
                //startActivity(intent);
                finish();
            }
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RegisterActivity.this, "An error occurred!", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    public class ProfileImageUploader extends StorageHandler{
        @Override
        public void onStart(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
        }

        @Override
        public void onSuccess(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            ProfileFetcher profileFetcher = new ProfileFetcher();
            profileFetcher.query(profiles);
        }

        @Override
        public void onGetDownloadUri(Uri downloadUri){
            mProfile.imagePath = downloadUri.toString();
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
        }
    }

    private class ProfileImageFetcher extends DownloadUtil {
        @Override
        public void onFinish(byte[] bytes, boolean success){
            if(!success){ return; }
            photo = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mImagePhotoView.setImageBitmap(photo);
                }
            });
        }
    }

    private void uploadProfile(){
        if(photo != null){
            Log.i(TAG, "uploading with profile");
            ProfileImageUploader imageUploader = new ProfileImageUploader();
            byte[] imageBytes = AppUtils.getByteArrayForBitmap(photo);
            imageUploader.uploadBytes(imageBytes, mProfile.getId(), "0", StorageHandler.PROFILEPATH);
        }else{
            if(!hasProfile) {
                Log.i(TAG, "uploading without image && !hasProfile");
                ProfileFetcher profileFetcher = new ProfileFetcher();
                profileFetcher.query(profiles);
            }else{
                Log.i(TAG, "uploading without image && hasProfile");
                ProfileWriter profileWriter = new ProfileWriter();
                profileWriter.setMerge(mProfile.getId(), mProfile);
            }
        }
    }

    private class CurrentProfileFetcher extends FSStoreFetcher<Profile> {
        private boolean found;

        public CurrentProfileFetcher(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartFetch(){
            container.removeView(progressBar);
            container.addView(progressBar);
            found = false;
        }

        @Override
        public void onFind(Profile profile){
            Storage.profile = profile;
            Storage.profileLoaded = true;
            fillFields();
            found = true;
        }

        @Override
        public void onSucceed(){
            container.removeView(progressBar);
        }

        @Override
        public void onFail(){
            container.removeView(progressBar);
        }

        @Override
        public boolean validateCondition(Profile profile){
            return profile.getEmail().equals(Storage.profile.getEmail());
        }

        @Override
        public boolean endFetchCondition(){
            return found;
        }
    }

}