package util;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;

import database.RTDataFetcher;
import model.Message;
import model.MessageBin;

// listens to setUp Badge
public class MessageRoomListener {
    private volatile boolean loading;
    private volatile int loadedBins = 0;
    private volatile int totalBins;
    private final ArrayList<String> roomIds = new ArrayList<>();
    private final ArrayList<MessageBin> messageBins = new ArrayList<>();
    private ViewGroup container;
    private ProgressBar progressBar;
    private MessageNodesFetcher nodesFetcher = null;

    public MessageRoomListener(ViewGroup container, ProgressBar progressBar){
        this.container = container;
        this.progressBar = progressBar;
    }

    public synchronized boolean isLoading(){
        return loading;
    }

    public synchronized void setContainer(ViewGroup container){
        this.container = container;
    }

    public synchronized  void setProgressBar(ProgressBar progressBar){
        this.progressBar = progressBar;
    }

    public void onFinishFetch(ArrayList<MessageBin> messageBins){
    }

    public synchronized void fetchMessages(){
        if(Storage.profile == null || loading){
            return;
        }
        if(nodesFetcher != null) {
            nodesFetcher.stopListening();
        }
        nodesFetcher = new MessageNodesFetcher(Storage.profile.getId());
        nodesFetcher.listen(null);
    }

    public void stopListening(){
        if(nodesFetcher != null) {
            nodesFetcher.stopListening();
        }
    }

    // fetch node 0
    private class MessageBinLoader extends RTDataFetcher<Message> {
        private final String roomId;
        private MessageBin messageBin;
        private boolean foundTopMessage;

        public MessageBinLoader(String roomId){
            super(AppUtils.MODEL_MESSAGE, Message.class);
            this.roomId = roomId;
        }


        @Override
        public void onStartFetch(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
            this.foundTopMessage = false;
            messageBin = new MessageBin(null, roomId);
            setDatabaseRef(getRef().child(roomId));
        }

        public void onFind(Message message){
            String keyId = getCurrentSnapshot().getKey() ;
            if(keyId != null && keyId.equals("0")){
                messageBin.addMessage(message);
                this.foundTopMessage = true;
            }
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            loading = false;
        }

        @Override
        public boolean validateCondition(Message message){
            return !foundTopMessage;
        }

        @Override
        public synchronized void onFinish() {
            if (container != null && progressBar != null){
                container.removeView(progressBar);
            }
            loadedBins++;
            if(loadedBins >= totalBins){
                loading = false;
                onFinishFetch(messageBins);
            }
            for(MessageBin bin : messageBins){ // correctory code
                if(bin.getId().equals(messageBin.getId())){
                    messageBins.remove(bin);
                    break;
                }
            }
            messageBins.add(messageBin);
        }
    }

    private class MessageNodesFetcher extends RTDataFetcher<Object>{
        private final String userId;

        public MessageNodesFetcher(String userId){
            super(AppUtils.MODEL_MESSAGE, Object.class);
            this.userId = userId;
        }

        @Override
        public void onStartFetch(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
                container.addView(progressBar);
            }
            loading = true;
            loadedBins = 0;
            totalBins = 0;
            roomIds.clear();
        }

        @Override
        public boolean validateCondition(Object e){
            if(getCurrentSnapshot() == null){
                return false;
            }
            if(getCurrentSnapshot().getKey() == null){
                return false;
            }
            String key = "_" + userId + "_";
            String roomId = getCurrentSnapshot().getKey();
            if(roomId.contains(key)){
                roomIds.add(roomId);
                return true;
            }
            return false;
        }

        @Override
        public void onFail(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            loading = false;
        }

        @Override
        public void onFinish(){
            if(container != null && progressBar != null) {
                container.removeView(progressBar);
            }
            totalBins = roomIds.size();
            messageBins.clear();
            if(totalBins == 0){
                onFinishFetch(messageBins);
            }
            for(String roomId : roomIds){
                MessageBinLoader binLoader = new MessageBinLoader(roomId);
                binLoader.fetch(null);
            }

        }
    }
}
