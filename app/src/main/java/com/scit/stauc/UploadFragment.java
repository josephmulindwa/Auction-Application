package com.scit.stauc;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import database.FSStoreFetcher;
import database.FSStoreValueChanger;
import database.RTSingleValueChanger;
import database.StorageHandler;
import model.PostableItem;
import model.Profile;
import model.TopBidHolder;
import util.AppUtils;
import util.PreferenceUtils;
import util.Storage;
import util.TimeUtil;

public class UploadFragment extends Fragment {
    private static final String TAG = "UploadFragment";
    private static final String DIALOG_DATE  = "DATE_PICK";
    private static final String DIALOG_TIME = "TIME_PICK";
    private static final int REQUEST_DATE = 4;
    private static final int REQUEST_TIME = 5;
    private static final int REQUEST_PHOTO = 6;
    private static final int REQUEST_PERMISSIONS = 7;
    private static final int REQUEST_LOGIN = 8;

    private EditText nameEditView;
    private EditText startPriceEditView;
    private EditText reservePriceEditView;
    private EditText descriptionEditView;
    private FrameLayout frameContainer;
    private ProgressBar progressBar;
    private TextView categoryTextView;
    private TextView endDateView;
    private TextView datePickerErrorView;
    private TextView endTimeView;
    private TextView categoryErrorTextView;
    private View startPriceHelpView;
    private View reservePriceHelpView;
    private Button continueButton;
    private RecyclerView photoRecyclerView;
    private TextView addPhotoErrorView;

    private String itemName;
    private int startPrice;
    private int reservePrice;
    private String description;
    private PostableItem.CATEGORY category;
    private PostableItem postableItem;
    private TopBidHolder topBidHolder;
    private PhotoAdapter photoAdapter;
    private Date endDate = null;
    private int endHours=0, endMinutes=0;
    private final Date refDate = new Date();
    private Date dateMin, dateMax;

    private final File[] uploadPhotoFiles = new File[AppUtils.MAX_UPLOAD_PHOTOS];
    private final Bitmap[] uploadPhotos = new Bitmap[AppUtils.MAX_UPLOAD_PHOTOS];
    private final PostableItem.CATEGORY[] categories = PostableItem.CATEGORY.values();
    private int photoIndex = 0;
    private int photoCounts = 0, uploadCount=0;
    private boolean permissionsGranted = false;
    private boolean uploading = false;

    public static UploadFragment newInstance(){
        return new UploadFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        AppUtils.ToolbarChanger drawerActivity = (AppUtils.ToolbarChanger) getActivity();
        if(drawerActivity != null) {
            drawerActivity.setTitleText(getString(R.string.upload));
        }

        Calendar calendar =  Calendar.getInstance();
        dateMin = TimeUtil.addTime(new Date(), new long[]{AppUtils.MIN_BID_DURATION, 0, 0, 0});
        dateMax = TimeUtil.addTime(new Date(), new long[]{AppUtils.MAX_BID_DURATION, 0, 0, 0});

        for(int i=0; i<uploadPhotoFiles.length; i++){
            uploadPhotoFiles[i] = getPhotoFile();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_upload, container, false);
        frameContainer = v.findViewById(R.id.frame_container);
        progressBar = (ProgressBar) inflater.inflate(
                R.layout.progress_bar_layer, frameContainer, false);
        nameEditView = v.findViewById(R.id.name_view);
        startPriceEditView = v.findViewById(R.id.start_price_view);
        reservePriceEditView = v.findViewById(R.id.reserve_price_view);
        descriptionEditView = v.findViewById(R.id.description_view);
        categoryTextView = v.findViewById(R.id.category_text_view);
        categoryErrorTextView = v.findViewById(R.id.category_error_view);
        photoRecyclerView = v.findViewById(R.id.photo_recycler_view);
        startPriceHelpView = v.findViewById(R.id.start_price_help_view);
        reservePriceHelpView = v.findViewById(R.id.reserve_price_help_view);
        endDateView = v.findViewById(R.id.end_date_view);
        endTimeView = v.findViewById(R.id.end_time_view);
        continueButton = v.findViewById(R.id.continue_button);
        datePickerErrorView = v.findViewById(R.id.date_picker_error_view);
        addPhotoErrorView = v.findViewById(R.id.add_photo_error_view);

        View myUploadsView = v.findViewById(R.id.my_uploads_button);
        myUploadsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = MyUploadsActivity.newIntent(getActivity());
                startActivity(i);
            }
        });

        setPhotosErrorState();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false);
        photoRecyclerView.setLayoutManager(linearLayoutManager);
        photoAdapter = new PhotoAdapter();
        photoRecyclerView.setAdapter(photoAdapter);

        startPriceHelpView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHelpDialog(getString(R.string.start_price), getString(R.string.start_price_description));
            }
        });

        reservePriceHelpView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHelpDialog(getString(R.string.reserve_price), getString(R.string.reserve_price_description));
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean valid = validateInput();
                if(valid){
                    uploadPostableItem();
                }
            }
        });

        endDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getFragmentManager();
                if(fragmentManager != null) {
                    Date useDate = (endDate == null) ? new Date() : endDate;
                    DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(useDate);
                    datePickerFragment.setTargetFragment(UploadFragment.this, REQUEST_DATE);
                    datePickerFragment.show(fragmentManager, DIALOG_DATE);
                }
            }
        });

        endTimeView.setText(R.string.time_holder);
        endTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getFragmentManager();
                if(fragmentManager != null){
                    TimePickerFragment timePickerFragment = TimePickerFragment.newInstance();
                    timePickerFragment.setTargetFragment(UploadFragment.this, REQUEST_TIME);
                    timePickerFragment.show(fragmentManager, DIALOG_TIME);
                }
            }
        });

        categoryTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View popUpView = inflater.inflate(R.layout.plain_radiobutton_view, frameContainer, false);
                RadioGroup radioGroup = popUpView.findViewById(R.id.category_radio_group);
                for(PostableItem.CATEGORY catgr : categories){
                    RadioButton radioButton = new RadioButton(getActivity());
                    radioButton.setText(catgr.toString());
                    radioButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            category = catgr;
                            categoryTextView.setText(catgr.toString());
                            categoryErrorTextView.setVisibility(View.GONE);
                        }
                    });
                    radioGroup.addView(radioButton);
                }
                AlertDialog popUp = new AlertDialog.Builder(getActivity())
                        .setView(popUpView)
                        .setTitle(R.string.select_category)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                popUp.show();
            }
        });

        return v;
    }

    private void showHelpDialog(String title, String content){
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.notification_dialog_view,
                frameContainer, false);
        TextView titleTextView = view.findViewById(R.id.title_text_view);
        TextView contentTextView = view.findViewById(R.id.content_text_view);
        Button actionButton = view.findViewById(R.id.notification_action_view);
        actionButton.setVisibility(View.GONE);
        titleTextView.setText(title);
        contentTextView.setText(content);

        AlertDialog helpDialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        helpDialog.show();
    }

    private void uploadPostableItem(){
        postableItem = new PostableItem(itemName, description, startPrice, category);
        postableItem.owner = Storage.profile.getId();
        postableItem.startTimeStamp = refDate.getTime();
        postableItem.endTimeStamp = endDate.getTime();
        postableItem.buyPrice = reservePrice;
        topBidHolder = new TopBidHolder(postableItem.id, postableItem.price);
        Storage.profile.addUpload(postableItem.id);
        Log.i(TAG, postableItem.id);

        PhotoUploader photoUploader = new PhotoUploader();
        int uploadIndex = 0;
        for (Bitmap uploadPhoto : uploadPhotos) {
            if (uploadPhoto != null) {
                byte[] imageBytes = AppUtils.getByteArrayForBitmap(uploadPhoto);
                photoUploader.uploadBytes(imageBytes, postableItem.id, Integer.toString(uploadIndex++), StorageHandler.ITEMPATH);
                ;
            }
        }
    }

    private File getPhotoFile(){
        if(getActivity() == null){
            return null;
        }
        File externalFilesDir  = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode != Activity.RESULT_OK){
            return;
        }
        if(requestCode == REQUEST_DATE){
            endDate = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            SimpleDateFormat dateFormatter = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
            String dateString = dateFormatter.format(endDate);
            endDateView.setText(dateString);

            if(!AppUtils.isBetween(endDate, dateMin, dateMax)){
                SimpleDateFormat dft = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
                String errorMsg = "Enter date between " + dft.format(dateMin) + " and " + dft.format(dateMax) + ".";
                datePickerErrorView.setVisibility(View.VISIBLE);
                datePickerErrorView.setText(errorMsg);
                endDate = null;
            }else{
                datePickerErrorView.setVisibility(View.GONE);
            }
        }else if(requestCode == REQUEST_TIME){
            endHours = data.getIntExtra(TimePickerFragment.EXTRA_TIME_HOUR, 0);
            endMinutes = data.getIntExtra(TimePickerFragment.EXTRA_TIME_MINUTE, 0);
            String timeString =  String.format(Locale.getDefault(), "%02d:%02d", endHours, endMinutes);
            endTimeView.setText(timeString);
        }else if(requestCode == REQUEST_PHOTO){
            if(uploadPhotoFiles[photoIndex] == null || !uploadPhotoFiles[photoIndex].exists()){
                Log.i(TAG, "empty file or null");
                uploadPhotos[photoIndex] = null;
            }else{
                Log.i(TAG, "file exists");
                Bitmap bitmap = BitmapFactory.decodeFile(uploadPhotoFiles[photoIndex].getPath());
                bitmap = AppUtils.getCroppedBitmap(bitmap, 400, 400);
                uploadPhotos[photoIndex] = bitmap;
                photoAdapter.notifyDataSetChanged();
            }

            photoCounts = 0;
            for(Bitmap bitmap : uploadPhotos){
                if(bitmap != null){
                    photoCounts += 1;
                }
            }
            setPhotosErrorState();
        }
    }

    private void setPhotosErrorState(){
        if(photoCounts < AppUtils.MIN_UPLOAD_PHOTOS){
            addPhotoErrorView.setVisibility(View.VISIBLE);
            addPhotoErrorView.setText(getString(R.string.add_photo_error));
        }else{
            addPhotoErrorView.setVisibility(View.GONE);
        }
    }

    private boolean validateInput(){
        itemName = nameEditView.getText().toString();
        boolean errored = false;
        if(itemName.isEmpty()){
            nameEditView.setError(getString(R.string.empty_field));
            errored = true;
        }else if(itemName.length() < AppUtils.MIN_ITEM_NAME_LENGTH){
            nameEditView.setError(getString(R.string.longer_filename_error));
            errored = true;
        }

        String startPriceString = startPriceEditView.getText().toString();
        startPrice = 0;
        if(startPriceString.isEmpty()){
            startPriceEditView.setError(getString(R.string.empty_field));
            errored = true;
        }else{
            startPrice = Integer.parseInt(startPriceString);
            if(startPrice < AppUtils.MIN_ACCEPTED_CURRENCY_VALUE){
                startPriceEditView.setError("Enter amount higher than " + AppUtils.MIN_ACCEPTED_CURRENCY_VALUE);
                errored = true;
            }else if(!AppUtils.isValidAmount(startPrice)){
                startPriceEditView.setError("Enter a valid amount");
                errored = true;
            }
        }

        String reservePriceString = reservePriceEditView.getText().toString();
        reservePrice = 0;
        if(!reservePriceString.isEmpty()){
            reservePrice = Integer.parseInt(reservePriceString);
            if(reservePrice < AppUtils.MIN_ACCEPTED_CURRENCY_VALUE){
                reservePriceEditView.setError("Enter amount higher than " + AppUtils.MIN_ACCEPTED_CURRENCY_VALUE);
                errored = true;
            }else if(!AppUtils.isValidAmount(reservePrice)){
                reservePriceEditView.setError("Enter a valid amount");
                errored = true;
            }
        }

        description = descriptionEditView.getText().toString();
        if(description.isEmpty()){
            descriptionEditView.setError(getString(R.string.empty_field));
            errored = true;
        }

        if(endDate == null){
            SimpleDateFormat dft = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
            String errorMsg = "Enter date between " + dft.format(dateMin) + " and " + dft.format(dateMax) + " inclusive.";
            datePickerErrorView.setVisibility(View.VISIBLE);
            datePickerErrorView.setText(errorMsg);
            errored = true;
        }else{
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            calendar.set(Calendar.HOUR_OF_DAY, endHours);
            calendar.set(Calendar.MINUTE, endMinutes);
            datePickerErrorView.setVisibility(View.GONE);
        }

        if(category == null) {
            categoryErrorTextView.setText("Select a category");
            categoryErrorTextView.setVisibility(View.VISIBLE);
        }
        if(photoCounts < AppUtils.MIN_UPLOAD_PHOTOS){
            errored = true;
        }
        return !errored;
    }

    private class PostableUploader extends FSStoreValueChanger<PostableItem>{

        public PostableUploader(){
            super(AppUtils.MODEL_POSTABLE, PostableItem.class);
        }

        @Override
        public void onStartChange(){
            frameContainer.removeView(progressBar);
            frameContainer.addView(progressBar);
        }

        @Override
        public void onSucceed(){
            frameContainer.removeView(progressBar);
            if(getActivity() != null){
                //getActivity().finish();
            }
            PostableTopBidSetter postableTopBidSetter = new PostableTopBidSetter();
            postableTopBidSetter.change();
            if(Storage.profileLoaded) {
                ProfileWriter profileWriter = new ProfileWriter();
                profileWriter.addToList(Storage.profile.getId(), "uploads", postableItem.id);
            }else{
                CurrentProfileFetcher profileFetcher = new CurrentProfileFetcher();
                profileFetcher.query(null);
            }
        }

        @Override
        public void onFail(){
            continueButton.setEnabled(true);
            frameContainer.setEnabled(true);
            frameContainer.removeView(progressBar);
        }

    }

    private class PostableTopBidSetter extends RTSingleValueChanger<TopBidHolder>{

        public PostableTopBidSetter(){
            super(AppUtils.MODEL_POSTABLE, TopBidHolder.class);
        }

        @Override
        public void onStartChange(){
            frameContainer.addView(progressBar);
        }

        @Override
        public void onChange(DatabaseReference databaseReference){
            databaseReference.child(topBidHolder.id).setValue(topBidHolder);
        }

        @Override
        public void onFinish(){
            frameContainer.removeView(progressBar);
            if(!Storage.postableItems.contains(postableItem)){
                Storage.postableItems.add(postableItem);
            }
            if(getActivity() != null) {
                getActivity().finish();
            }
        }

        @Override
        public void onFail(){
            continueButton.setEnabled(true);
            frameContainer.setEnabled(true);
            frameContainer.removeView(progressBar);
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        private final ImageView photoView;
        private final ImageView addIcon;
        private final ImageView cancelIcon;
        private final Intent photoIntent;
        private int index;

        public PhotoHolder(View view){
            super(view);
            photoView = itemView.findViewById(R.id.item_photo_view);
            addIcon = itemView.findViewById(R.id.add_icon_view);
            cancelIcon = itemView.findViewById(R.id.cancel_photo_shoot_button);

            photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            boolean canTakePhoto = (photoIntent.resolveActivity(getActivity().getPackageManager()) != null);

            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
            }else{
                permissionsGranted = true;
            }

            addIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(getActivity() == null){
                        return;
                    }
                    photoIndex = index;
                    //uploadPhotoFiles[index] = getPhotoFile();
                    Context context = getActivity();
                    if(context != null) {
                        Uri uri = FileProvider.getUriForFile(context,
                                context.getApplicationContext().getPackageName() + ".provider",
                                uploadPhotoFiles[index]);
                        photoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        photoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    }

                    if(canTakePhoto && permissionsGranted){
                        startActivityForResult(photoIntent, REQUEST_PHOTO);
                    }
                }
            });

            cancelIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    photoIndex = index;
                    //uploadPhotoFiles[index] = null;
                    uploadPhotos[index] = null;
                    photoView.setImageBitmap(uploadPhotos[index]);
                    photoAdapter.notifyDataSetChanged();

                    addIcon.setVisibility(View.VISIBLE);
                    //cancelIcon.setVisibility(View.GONE);
                }
            });

        }

        public void bindHolder(int index){
            this.index = index;
            if(uploadPhotos[index] != null){
                photoView.setImageBitmap(uploadPhotos[index]);
                addIcon.setVisibility(View.GONE);
                cancelIcon.setVisibility(View.VISIBLE);
            }else{
                photoView.setImageBitmap(null);
                addIcon.setVisibility(View.VISIBLE);
                cancelIcon.setVisibility(View.GONE);
            }
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType){
            View view = LayoutInflater.from(getActivity()).inflate(
                    R.layout.upload_photo_layer, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position){
            holder.bindHolder(position);
        }

        @Override
        public int getItemCount(){
            return AppUtils.MAX_UPLOAD_PHOTOS;
        }
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
            permissionsGranted = granted;
        }
    }

    private class PhotoUploader extends StorageHandler{
        public PhotoUploader(){
            super();
        }

        @Override
        public void onStart(){
            frameContainer.removeView(progressBar);
            frameContainer.addView(progressBar);
            uploadCount = 0;
            uploading = true;
            continueButton.setEnabled(false);
            frameContainer.setEnabled(false);
        }

        @Override
        public void onSuccess(){
            frameContainer.removeView(progressBar);
            uploadCount++;
            Log.i(TAG, "finished upload -> " + uploadCount);
            if(uploadCount == photoCounts){
                PostableUploader postableUploader = new PostableUploader();
                postableUploader.set(postableItem.id, postableItem);
                Log.i(TAG, "photo upload complete");
            }
        }

        @Override
        public void onFail(){
            continueButton.setEnabled(true);
            frameContainer.setEnabled(true);
            frameContainer.removeView(progressBar);
        }

        @Override
        public void onGetDownloadUri(Uri downloadUri){
            postableItem.images.add(downloadUri.toString());
        }
    }

    private class CurrentProfileFetcher extends FSStoreFetcher<Profile> {
        private boolean found;

        public CurrentProfileFetcher(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartFetch(){
            frameContainer.removeView(progressBar);
            frameContainer.addView(progressBar);
            found = false;
        }

        @Override
        public void onFind(Profile profile){
            Storage.profile = profile;
            Storage.profileLoaded = true;
            ProfileWriter profileWriter = new ProfileWriter();
            profileWriter.addToList(Storage.profile.getId(), "uploads", postableItem.id);
            found = true;
        }

        @Override
        public void onSucceed(){
            frameContainer.removeView(progressBar);
        }

        @Override
        public void onFail(){
            continueButton.setEnabled(true);
            frameContainer.setEnabled(true);
            frameContainer.removeView(progressBar);
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

    private class ProfileWriter extends FSStoreValueChanger<Profile>{

        public ProfileWriter(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

        @Override
        public void onStartChange(){
            frameContainer.removeView(progressBar);
            frameContainer.addView(progressBar);
        }

        @Override
        public void onSucceed(){
            continueButton.setEnabled(true);
            frameContainer.setEnabled(true);
            frameContainer.removeView(progressBar);
        }

        @Override
        public void onFail(){
            frameContainer.removeView(progressBar);
        }

    }

}
