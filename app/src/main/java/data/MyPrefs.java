package data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by root on 8/13/17.
 */

public class MyPrefs {
    private static final String TAG = "MyPrefs";
    private static final String FILE = "com.samirk433.fyp.file";
    private static final String ACCOUNT_NAME = "accountName";
    private static final String USER_ID = "userId";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String SERVER_URL = "serverUrl";


    Context mContext;

    SharedPreferences mPreferences;
    SharedPreferences.Editor mEditor;

    public MyPrefs(Context context){
        Log.d(TAG, " MyPrefs()");

        this.mContext = context;
        mPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        mEditor = mPreferences.edit();
    }

    public void setAccountName(String accountName){
        mEditor.putString(ACCOUNT_NAME, accountName);
        mEditor.apply();
    }
    public String getAccountName(){
        return mPreferences.getString(ACCOUNT_NAME, null);
    }


    public void setUserId(int userID){
        mEditor.putInt(USER_ID, userID);
        mEditor.apply();
    }
    public int getUserId(){
        return mPreferences.getInt(USER_ID, -1);
    }

    public void setUsername(String username){
        mEditor.putString(USERNAME, username);
        mEditor.apply();
    }

    public String getUsername(){
        return mPreferences.getString(USERNAME, null);
    }

    public void setPassword(String password){
        mEditor.putString(PASSWORD, password);
        mEditor.apply();
    }
    public String getPassword(){
        return mPreferences.getString(PASSWORD, null);
    }

    public void setServerUrl(String url){
        mEditor.putString(SERVER_URL, url);
        mEditor.apply();
    }
    public String getServerUrl(){
        return mPreferences.getString(SERVER_URL, "");
    }
}
