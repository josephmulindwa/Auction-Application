package com.scit.stauc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.scit.stauc.R;

import database.StorageHandler;
import model.Profile;
import util.AppUtils;
import util.DownloadUtil;
import util.PreferenceUtils;
import util.Storage;

public class ProfileFragment extends Fragment{
    private Profile profile;
    private ImageView profilePhoto;
    private TextView profileName;
    private TextView profileEmail;
    private TextView profileTelephone;
    private TextView editProfileView;
    private Button uploadsButton;
    private AppUtils.ToolbarChanger toolbarChanger;

    public static ProfileFragment newInstance(){
        return new ProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        toolbarChanger = (AppUtils.ToolbarChanger) getActivity();
        toolbarChanger.setTitleText(getString(R.string.profile));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_profile, parent, false);
        profilePhoto = v.findViewById(R.id.profile_photo);
        profileName = v.findViewById(R.id.profile_name);
        profileEmail = v.findViewById(R.id.profile_email);
        profileTelephone = v.findViewById(R.id.profile_telephone);
        editProfileView = v.findViewById(R.id.edit_profile_text_view);
        uploadsButton = v.findViewById(R.id.my_uploads_button);

        editProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = RegisterActivity.newIntent(getActivity(), null);
                startActivity(i);
            }
        });

        uploadsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = MyUploadsActivity.newIntent(getActivity());
                startActivity(i);
            }
        });

        return v;
    }

    @Override
    public void onResume(){
        super.onResume();
        loadProfile();
    }

    private void loadProfile(){
        profile = Storage.profile;
        if(profile == null){
            return;
        }
        String fullName = profile.getFName() + " " + profile.getLName();
        profileName.setText(fullName);
        profilePhoto.setImageResource(R.drawable.avator0);
        profileEmail.setText(profile.getEmail());
        profileTelephone.setText(profile.getTelephone());
        if(toolbarChanger != null){
            toolbarChanger.setHeaderName(profile.getName());
            toolbarChanger.setHeaderEmail(profile.getEmail());
        }
        Log.i("RegisterActivity", "ProfileImPath:" + profile.imagePath);
        if(profile.imagePath != null){
            ProfileImageFetcher imageFetcher = new ProfileImageFetcher();
            imageFetcher.downloadBytes(profile.imagePath);
        }
    }

    private class ProfileImageFetcher extends DownloadUtil{
        @Override
        public void onFinish(byte[] bytes, boolean success){
            if(!success){ return; }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Log.i("RegisterActivity", "toolbarc:"+toolbarChanger);
            if(toolbarChanger != null){
                toolbarChanger.setHeaderImage(bitmap);
            }
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    profilePhoto.setImageBitmap(bitmap);
                }
            });
        }
    }
}
