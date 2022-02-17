package util;

import java.util.Date;
import java.util.Random;

import database.FSStoreFetcher;
import database.FSStoreValueChanger;
import model.Profile;

public class ForgotPasswordHandler {
    private Profile mProfile = null;
    private boolean found = false;
    private boolean loading = false;

    public void handle(){
        long tempPass = new Date().getTime() + new Random().nextInt(9999);
        // set it as password
        // send code to their phone, charges apply
        // or email
        // or message admin
    }

    private class ProfileFetcher extends FSStoreFetcher<Profile>{
        private final String id;
        private long newPasskey;

        public ProfileFetcher(String id, long newPasskey){
            super(AppUtils.MODEL_PROFILE, Profile.class);
            this.id = id;
            this.newPasskey = newPasskey;
        }

        @Override
        public void onStartFetch(){
            mProfile = null;
            found = false;
        }

        @Override
        public boolean validateCondition(Profile profile){
            return profile.getId().equals(id);
        }

        @Override
        public boolean endFetchCondition(){
            return found;
        }

        @Override
        public void onFind(Profile profile){
            found = true;
            mProfile = profile;
            mProfile.setPassword(String.format("%d", newPasskey));
        }

        @Override
        public void onSucceed(){
            ProfilePassChanger passChanger = new ProfilePassChanger();
            passChanger.setMerge(mProfile.getId(), mProfile);
        }

        @Override
        public void onFail(){

        }

    }

    private class ProfilePassChanger extends FSStoreValueChanger<Profile>{

        public ProfilePassChanger(){
            super(AppUtils.MODEL_PROFILE, Profile.class);
        }

    }

}
