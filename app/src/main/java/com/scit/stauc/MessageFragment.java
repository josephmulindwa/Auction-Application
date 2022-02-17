package com.scit.stauc;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import database.FSStoreFetcher;
import database.RTDataFetcher;
import database.RTSingleValueChanger;
import model.Message;
import model.MessageBin;
import model.Profile;
import util.AppUtils;
import util.Storage;

public class MessageFragment extends Fragment {
    private static final String TAG = "MessageFragment";
    private static final String EXTRA_RECEIPIENT = "MessageFragment.Receipient";
    private String receipient = null;
    private String infoText = null;
    private ViewGroup container;
    private ProgressBar progressBar;
    private RecyclerView messageRecycler;
    private TextView infoTextView;
    private EditText messageEditView;
    private MessageBinLoader binLoader;
    private MessageBin mMessageBin;

    public static MessageFragment newInstance(String receipient, String infoText){
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_RECEIPIENT, receipient);
        if(infoText != null) {
            bundle.putString(MessageActivity.EXTRA_INFO_TEXT, infoText);
        }
        MessageFragment messageFragment = new MessageFragment();
        messageFragment.setArguments(bundle);
        return messageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            receipient = getArguments().getString(EXTRA_RECEIPIENT);
            infoText = getArguments().getString(MessageActivity.EXTRA_INFO_TEXT);
        }
        if(mMessageBin == null){
            mMessageBin = new MessageBin(receipient, null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_message, parent, false);
        container = v.findViewById(R.id.frame_container);
        progressBar = (ProgressBar) inflater.inflate(R.layout.progress_bar_layer, parent, false);
        messageRecycler = v.findViewById(R.id.messages_recycler);
        messageRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        infoTextView = v.findViewById(R.id.info_text_view);
        messageEditView = v.findViewById(R.id.message_edit_view);
        View sendMessageView = v.findViewById(R.id.send_message_view);

        infoTextView.setText(infoText);
        if(infoText == null){
            infoTextView.setVisibility(View.GONE);
        }

        AppUtils.ToolbarChanger toolbarChanger = (AppUtils.ToolbarChanger) getActivity();
        if(toolbarChanger != null){
            toolbarChanger.setTitleText(getString(R.string.messages));
        }

        if(Storage.profile == null){
            Intent i = LoginActivity.newIntent(getActivity(), true);
            startActivity(i);
        }

        sendMessageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Storage.profile == null){
                    return;
                }
                String text = messageEditView.getText().toString().trim();
                if(text.isEmpty()){
                    return;
                }
                Message msg = new Message(text, Storage.profile.getId());
                msg.setSender(Storage.profile.getId());
                MessageUploader messageUploader = new MessageUploader(msg);
                messageUploader.change();
            }
        });

        return v;
    }

    private String getRoomId(){
        if(receipient == null || Storage.profile == null){
            return null;
        }
        String start = receipient, end=Storage.profile.getId();
        if(start.equals(end)){
            return null;
        }
        if(receipient.compareTo(end) > 0){
            start = Storage.profile.getId();
            end = receipient;
        }
        return "_" + start + "_" + end + "_";
    }

    private class MessageHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{
        private final LinearLayout encasing;
        private final TextView bodyTextView;
        private final TextView dateTextView;
        private final View backgroundView;
        private int senderColor = R.color.customblue;
        private int receipientColor = R.color.white;
        private Message mMessage;

        public MessageHolder(View view){
            super(view);
            encasing = itemView.findViewById(R.id.frame_encasing);
            bodyTextView = itemView.findViewById(R.id.message_body_view);
            dateTextView = itemView.findViewById(R.id.message_time_view);
            backgroundView = itemView.findViewById(R.id.background_view);
            itemView.setOnLongClickListener(this);
        }

        public void bindMessage(Message message){
            mMessage = message;
            bodyTextView.setText(message.getContent());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            String dateString = dateFormat.format(new Date(message.getTimeStamp()));
            dateTextView.setText(dateString);
            if(Storage.profile != null){
                boolean isSender = Storage.profile.getId().equals(message.getSender());
                backgroundView.setBackgroundColor(getResources().getColor(isSender ? senderColor : receipientColor));
                encasing.setGravity(isSender ? Gravity.END : Gravity.START);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if(mMessage != null && mMessage.getSender().equals(Storage.profile.getId())){
                View deletePopUp = LayoutInflater.from(getActivity()).inflate(
                        R.layout.delete_view, container, false);
                TextView deleteMessageView = deletePopUp.findViewById(R.id.content_text_view);
                deleteMessageView.setText("Delete this message? \nThis operation cannot be undone.");
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setView(deletePopUp)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MessageDeleter messageDeleter = new MessageDeleter(mMessage);
                                messageDeleter.change();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                alertDialog.show();
            }
            return false;
        }
    }

    private void updateView(){
        if(!mMessageBin.getMessages().isEmpty()){
            messageRecycler.setVisibility(View.VISIBLE);
            MessageAdapter messageAdapter = new MessageAdapter();
            messageRecycler.setAdapter(messageAdapter);
            messageRecycler.scrollToPosition(mMessageBin.getMessages().size()-1);
        }else{
            messageRecycler.setVisibility(View.GONE);
        }
    }

    private void updateMessagesAsRead(){
        if(mMessageBin == null || mMessageBin.getMessages() == null || Storage.profile == null){
            return;
        }
        for(Message message : mMessageBin.getMessages()){
            if(!message.getSender().equals(Storage.profile.getId()) && !message.getRead()){
                message.setRead(true);
                MessageUploader messageUploader = new MessageUploader(message);
                messageUploader.change();
            }
        }
    }

    public class MessageAdapter extends RecyclerView.Adapter<MessageHolder>{

        @NonNull
        @Override
        public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType){
            View view = LayoutInflater.from(getActivity()).inflate(
                    R.layout.message_holder_view, parent, false);
            return new MessageHolder(view);
        }

        @Override
        public void onBindViewHolder(MessageHolder holder, int position){
            holder.bindMessage(mMessageBin.getMessages().get(position));
        }

        @Override
        public int getItemCount(){
            return mMessageBin.getMessages().size();
        }
    }

    private class MessageUploader extends RTSingleValueChanger<Message> {
        private final Message message;

        public MessageUploader(Message message){
            super(AppUtils.MODEL_MESSAGE, Message.class);
            this.message = message;
        }

        @Override
        public void onStartChange(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onChange(DatabaseReference dRef){
            if(getRoomId() == null){
                return;
            }
            String timeId = String.format("%d", message.getTimeStamp());
            // senderid + selfid
            //String roomId =
            dRef.child(getRoomId()).child(timeId).setValue(message);
            dRef.child(getRoomId()).child("0").setValue(message);
        }

        @Override
        protected void onFinish() {
            Log.i(TAG, "message upload finished");
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            // Storage.messageBin.addMessage(message); // handled at listen
            messageEditView.setText(null);
            // notifyDatasetChanged() // handled at listen
        }

        @Override
        protected void onFail() {
            Log.i(TAG, "message upload failed");
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
        }
    }

    private class MessageDeleter extends RTSingleValueChanger<Message>{
        private final Message message;

        public MessageDeleter(Message message){
            super(AppUtils.MODEL_MESSAGE, Message.class);
            this.message = message;
        }

        @Override
        public void onStartChange(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
        }

        @Override
        public void onChange(DatabaseReference dRef){
            if(getRoomId() == null){
                return;
            }
            String id = String.format("%d", message.getTimeStamp());
            dRef.child(getRoomId()).child(id).removeValue();
            if(mMessageBin != null){
                if(mMessageBin.getMessages().size() == 1) {
                    dRef.child(getRoomId()).child("0").removeValue();
                    updateView();
                }
                // if message deleted is last msg
                if( message.equals(mMessageBin.getTopMessage())){
                    mMessageBin.deleteMessage(message);
                    if(mMessageBin.getTopMessage() != null) {
                        dRef.child(getRoomId()).child("0").setValue(mMessageBin.getTopMessage());
                    }
                }
            }
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
        }

        @Override
        public void onFinish(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
        }

    }

    private class ProfileNameFetcher extends FSStoreFetcher<Profile> {
        private boolean found;
        private final String id;

        public ProfileNameFetcher(String id){
            super(AppUtils.MODEL_PROFILE, Profile.class);
            this.id = id;
        }

        @Override
        public void onStartFetch(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
            found = false;
        }

        @Override
        public void onFind(Profile profile){
            // fill name
            if(getActivity() != null){
                AppUtils.ToolbarChanger toolbarChanger = (AppUtils.ToolbarChanger) getActivity();
                toolbarChanger.setTitleText(profile.getName());
            }
            found = true;
        }

        @Override
        public void onSucceed(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
        }

        @Override
        public boolean validateCondition(Profile profile){ return profile.getId().equals(id); }

        @Override
        public boolean endFetchCondition(){ return found; }
    }

    private class MessageBinLoader extends RTDataFetcher<Message> {
        private MessageBin messageBin;
        private final String nodeKey;

        public MessageBinLoader(String node){
            super(AppUtils.MODEL_MESSAGE, Message.class);
            nodeKey = node;
        }

        @Override
        public void onStartFetch(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
            messageBin = new MessageBin(null, null);
            setDatabaseRef(getRef().child(nodeKey));
        }

        public void onFind(Message message){
            if(getCurrentSnapshot() != null && getCurrentSnapshot().getKey() != null
                    && !getCurrentSnapshot().getKey().equals("0")){
                messageBin.addMessage(message);
            }
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
        }

        @Override
        public void onFinish(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            mMessageBin = messageBin;
            updateMessagesAsRead();
            updateView();
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        if(Storage.profile != null && getRoomId() != null) {
            ProfileNameFetcher profileNameFetcher = new ProfileNameFetcher(receipient);
            profileNameFetcher.query(null);
            binLoader = new MessageBinLoader(getRoomId());
            binLoader.listen(null);
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if(binLoader != null){
            binLoader.stopListening();
        }
    }
}
