package util;

import android.content.Context;
import android.preference.PreferenceManager;

import model.Profile;


public class PreferenceUtils {
    private static final String TAG = "PreferenceUtils";
    private static final String REMEMBER_ME = "rememberMe";
    private static final String PREF_PROFILE_FNAME = "profile.fname";
    private static final String PREF_PROFILE_LNAME = "profile.lname";
    private static final String PREF_PROFILE_EMAIL = "profile.email";
    private static final String PREF_PROFILE_TELEPHONE = "profile.telephone";
    private static final String PREF_PROFILE_PASSWORD = "profile.password";
    private static final String PREF_PROFILE_IMAGEPATH = "profile.image.path";
    private static final String PREF_CUSTOMER_NAV_ID = "customer.nav.id";
    private static final String PREF_MAIN_SEARCH_VALUE = "searchFragment.searchValue";

    public static void setRememberStats(Context context, boolean rememberMe){
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(REMEMBER_ME, rememberMe)
                .apply();
    }

    public static boolean getRememberState(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(REMEMBER_ME, false);
    }

    public static void writeProfile(Context context, Profile profile){
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_PROFILE_FNAME, profile.getFName())
                .putString(PREF_PROFILE_LNAME, profile.getLName())
                .putString(PREF_PROFILE_EMAIL, profile.getEmail())
                .putString(PREF_PROFILE_TELEPHONE, profile.getTelephone())
                .putString(PREF_PROFILE_PASSWORD, profile.getPassword())
                .putString(PREF_PROFILE_IMAGEPATH, profile.imagePath)
                .apply();
    }

    public static Profile readProfile(Context context){
        Profile profile = new Profile();
        profile.setFName(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PROFILE_FNAME, null));
        profile.setLName(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PROFILE_LNAME, null));
        profile.setEmail(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PROFILE_EMAIL, null));
        profile.setTelephone(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PROFILE_TELEPHONE, null));
        profile.setPassword(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PROFILE_PASSWORD, null));
        profile.imagePath = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PROFILE_IMAGEPATH, null);
        return profile;
    }

    public static void clearPreferences(Context context){
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();
    }

    public static void setCustomerNavigationId(Context context, int id){
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(PREF_CUSTOMER_NAV_ID, id)
                .apply();
    }

    public static int getCustomerNavigationId(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_CUSTOMER_NAV_ID, 0);
    }

    public static void setMainSearchValue(Context context, String searchValue){
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_MAIN_SEARCH_VALUE, searchValue)
                .apply();
    }

    public static String getMainSearchValue(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_MAIN_SEARCH_VALUE, null);
    }

}
