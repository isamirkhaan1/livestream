package utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

/**
 * Created by samirk433 on 10/24/2017.
 */

public class MyPermissions {

    public static boolean checkPermissions(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS)
                        == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    public static void showPermissionDialog(Context context) {
        ActivityCompat.requestPermissions(((Activity) context),
                new String[]{android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.WRITE_CONTACTS,
                        android.Manifest.permission.ACCOUNT_MANAGER,
                        android.Manifest.permission.GET_ACCOUNTS,
                        android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.WAKE_LOCK},
                101);
    }
}
