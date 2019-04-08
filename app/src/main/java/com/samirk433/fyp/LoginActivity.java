package com.samirk433.fyp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import data.MyPrefs;
import utils.PostRequestData;

public class LoginActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText textUsername = (EditText) findViewById(R.id.login_username);
        final EditText textPassword = (EditText) findViewById(R.id.login_password);
        final TextView textForgotPass = (TextView) findViewById(R.id.login_forgotpassword);
        final TextView textNewAccount = (TextView) findViewById(R.id.login_newaccount);
        final LinearLayout layoutSettings = (LinearLayout) findViewById(R.id.settings);
        final Button btnLogin = (Button) findViewById(R.id.login_btn);

        // layout settings click-listner
        layoutSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        // login-btn action-listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = textUsername.getText().toString().trim();
                String password = textPassword.getText().toString().trim();

                // start asycn task..
                LoginTask login = new LoginTask(LoginActivity.this, username, password);
                login.execute();
            }
        });

        // create new accout action-listener
        textNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                LoginActivity.this.startActivity(intent);
                //LoginActivity.this.finish();
            }
        });

        // forgot-password-textView action-listener
        textForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Permission Denied");
                builder.setMessage("Please inform technical staff via email for password recovery");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        });

    }

/*
* ------------------------------ CLASS: LOGIN ASYNC ---------------------------
* */

    class LoginTask extends AsyncTask {
        Context mContext;
        ProgressDialog mDialog;
        String mUserMsg, mUsername, mPassword;

        public LoginTask(Context context, String username, String password) {
            this.mContext = context;
            this.mUsername = username;
            this.mPassword = password;
            mDialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            mDialog.setMessage("Logging in..");
            mDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            MyPrefs prefs = new MyPrefs(mContext);
            URL url;
            HttpURLConnection connection;
            String urlString = prefs.getServerUrl() + "/login.php";

            try {
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");

                OutputStream os = connection.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                // set pa
                // rameter values for post-request
                HashMap<String, String> param = new HashMap<String, String>();
                param.put("username", mUsername);
                param.put("password", mPassword);

                bw.write(PostRequestData.getData(param));
                bw.flush();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    return br;
                } else {
                    mUserMsg = "Server Couldn't process the request";
                }
            } catch (IOException e) {
                mUserMsg = "Please make sure that Internet connection is available," +
                        " and server IP is inserted in settings";
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            try {

                //connection isn't available or something is wrong with server address
         /*   if( != null)
                throw  new IOException();*/

                BufferedReader br = (BufferedReader) o;
                if (br == null)
                    throw new NullPointerException("BufferedReader instance couldn't be NULL");

                String data = "", line;
                while ((line = br.readLine()) != null) {
                    data += line;
                }

                if (data == null || data.equals(""))
                    throw new NullPointerException("Server response couldn't be empty");

                // convert jsonString into jsonArray and get first object
                JSONArray jsonArray = new JSONArray(data);
                JSONObject jsonObject = jsonArray.getJSONObject(0);

                // parse server response
                switch (jsonObject.getString("response")) {

                    case "success":
                        mUserMsg = null;
                        MyPrefs prefs = new MyPrefs(mContext);
                        prefs.setUserId(jsonObject.getInt("id"));
                        prefs.setUsername(mUsername);

                        // redirect to another activity from here..
                        Intent intent = new Intent(mContext, HomeActivity.class);
                        ((Activity) mContext).startActivity(intent);
                        ((Activity) mContext).finish();
                        break;

                    case "incorrect":
                        mUserMsg = "Incorrect username or password";
                        break;

                    case "error":
                        mUserMsg = "Something went wrong on server";
                        break;
                }
            } catch (JSONException e) {
                mUserMsg = "Incorrect formatted data from server";
                e.printStackTrace();
            } catch (IOException e) {
                //if connection was available via connecting but
                //we can't get data from server..
                if (mUserMsg == null)
                    mUserMsg = "Please check connection";
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
                mUserMsg = e.getMessage();
            } finally {
                if (mUserMsg != null)
                    Toast.makeText(mContext, mUserMsg, Toast.LENGTH_SHORT).show();
            }
            // hide the progressDialog
            mDialog.hide();

            super.onPostExecute(o);
        }
    }
}