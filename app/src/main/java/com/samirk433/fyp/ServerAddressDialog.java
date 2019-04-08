package com.samirk433.fyp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import data.MyPrefs;

/**
 * Created by Peekay on 10/26/2016.
 */
public class ServerAddressDialog extends DialogFragment {
    Context mContext;

    public ServerAddressDialog() {
    }

    @SuppressLint("ValidFragment")
    public ServerAddressDialog(Context context) {
        this.mContext = context;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        // inflate view to dialog
        final View v = LayoutInflater.from(mContext).inflate(R.layout.dialog_server, null);
        final EditText textUrl = (EditText) v.findViewById(R.id.editTextServerUrl);

        //  populate editText with server-address if exists
        textUrl.setText(new MyPrefs(mContext).getServerUrl());

        // set view to dialog
        builder.setView(v);

        // if dialog-btn-cancel pressed..
        builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // if dialog-btn-save pressed..
        builder.setPositiveButton(R.string.action_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = textUrl.getText().toString().trim();
                String userMsg;

                if (url.length() < 5) {
                    userMsg = "Invalid server address";
                } else {
                    userMsg = "Server address successfully saved";

                    MyPrefs preference = new MyPrefs(mContext);
                    preference.setServerUrl(url);
                }
                dialog.dismiss();
                Toast.makeText(mContext, userMsg, Toast.LENGTH_SHORT).show();
            }
        });
        return builder.create();
    }
}
