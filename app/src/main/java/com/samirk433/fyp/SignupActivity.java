package com.samirk433.fyp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

import data.MyPrefs;
import utils.PostRequestData;

public class SignupActivity extends AppCompatActivity {
    EditText mTextUsername, mTextEmail, mTextPassword, mTextPassConfirm;
    TextView mTextPasswordMatch, mTextUsernameUnique;
    LinearLayout mLayoutPasswordMatch, mLayoutUsernameUnique;
    ImageView mImgPasswordMatch, mImgUsernameUnique;
    Button mBtnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mTextUsername = (EditText) findViewById(R.id.signup_username);
        mTextEmail = (EditText) findViewById(R.id.signup_email);
        mTextPassword = (EditText) findViewById(R.id.signup_password);
        mTextPassConfirm = (EditText) findViewById(R.id.signup_confirmpassword);
        mBtnSignup = (Button) findViewById(R.id.signup_btn);
        mTextPasswordMatch = (TextView) findViewById(R.id.text_pass_matches);
        mTextUsernameUnique = (TextView) findViewById(R.id.text_us_unique);
        mImgPasswordMatch = (ImageView) findViewById(R.id.img_pass_matches);
        mImgUsernameUnique = (ImageView) findViewById(R.id.img_us_unique);
        mLayoutPasswordMatch = (LinearLayout) findViewById(R.id.layout_pass_matches);
        mLayoutUsernameUnique = (LinearLayout) findViewById(R.id.layout_us_unique);


        // check if the username already taken
        mTextUsername.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                if (!focused) {
                    mLayoutUsernameUnique.setVisibility(View.GONE);

                    String username = mTextUsername.getText().toString().trim();
                    CheckUsernameTask checkUsername = new CheckUsernameTask(SignupActivity.this, username);
                    checkUsername.execute();
                }
            }
        });

        // check whether password and confirm-password matches or not
        mTextPassConfirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                if (!focused) {
                    String password = mTextPassword.getText().toString();
                    String confirm = mTextPassConfirm.getText().toString();
                    mLayoutPasswordMatch.setVisibility(View.VISIBLE);

                    // if matches display icon plus message green else display in red
                    if (confirm.equals(password)) {
                        mImgPasswordMatch.setImageDrawable(getResources().getDrawable(R.drawable.ic_done));
                        mTextPasswordMatch.setText(R.string.password_matches);
                        mTextPasswordMatch.setTextColor(getResources().getColor(R.color.green));
                    } else {
                        mImgPasswordMatch.setImageDrawable(getResources().getDrawable(R.drawable.ic_clear));
                        mTextPasswordMatch.setText(R.string.password_not_match);
                        mTextPasswordMatch.setTextColor(getResources().getColor(R.color.red));
                    }
                }
            }
        });

        // sign-up btn action-listener
        mBtnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // validate user input
                if (validate()) {
                    String username = mTextUsername.getText().toString().trim();
                    String email = mTextEmail.getText().toString().trim();
                    String password = mTextPassword.getText().toString().trim();

                    // server request for new account
                    SignupTask signup = new SignupTask(SignupActivity.this, username, email, password);
                    signup.execute();
                }
            }
        });
    }

    // method: validate user input
    private boolean validate() {
        String username = mTextUsername.getText().toString().trim();
        String email = mTextEmail.getText().toString().trim();
        String password = mTextPassword.getText().toString().trim();
        String confirm = mTextPassConfirm.getText().toString().trim();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        String dialogMsg = null;

        // validate Username, email and password
        if (username.length() < 3) {
            dialogMsg = "Username should be larger than 2 characters";
        } else if (email.length() < 3) {
            dialogMsg = "Email should be larger than 2 characters";
        } else if (password.length() < 3) {
            dialogMsg = "Password should be larger than 2 characters";
        } else if (!password.equals(confirm)) {
            dialogMsg = getResources().getString(R.string.password_not_match);
        } else if (mTextUsernameUnique.getText().toString().equals(
                getResources().getString(R.string.username_taken))) {
            dialogMsg = getResources().getString(R.string.username_taken);
        }


        // show alert-dialog if error exists..
        if (dialogMsg != null) {
            builder.setMessage(dialogMsg);
            builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            builder.show();
            return false;
        }
        return true;
    }


/*
* ------------------------------ CLASS: CHECK USERNAME ASYNC  ---------------------------
* */

    class CheckUsernameTask extends AsyncTask {
        private Context mContext;
        private String mUsername;

        public CheckUsernameTask(Context context, String username) {
            this.mContext = context;
            this.mUsername = username;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            MyPrefs prefs = new MyPrefs(mContext);
            URL url;
            HttpURLConnection connection;
            String urlString = prefs.getServerUrl() + "/user_exist.php";

            try {
                urlString = urlString + "?user=" + URLEncoder.encode(mUsername, "UTF-8");
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    return br;
                }
            } catch (MalformedURLException e) {
                //error: incorrect format of URL
                e.printStackTrace();
            } catch (IOException e) {
                //error: server not found/connection not available
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            BufferedReader br = (BufferedReader) o;
            try {
                if (br == null)
                    throw new NullPointerException("Response couldn't be NULL");

                String line, data = "";
                while ((line = br.readLine()) != null) {
                    data = data + line;
                }

                if (data == null || data.equals(""))
                    throw new NullPointerException("Response couldn't be NULL");

                // convert jsonString to jsonArray
                JSONArray jsonArray = new JSONArray(data);
                JSONObject jsonObject = jsonArray.getJSONObject(0);

                TextView textUsernameUnique = (TextView) ((Activity) mContext).findViewById(R.id.text_us_unique);
                ImageView imgUsernameUnique = (ImageView) ((Activity) mContext).findViewById(R.id.img_us_unique);
                LinearLayout layoutUsernameUnique = (LinearLayout)
                        ((Activity) mContext).findViewById(R.id.layout_us_unique);

                switch (jsonObject.getString("response")) {
                    case "exist":
                        layoutUsernameUnique.setVisibility(View.VISIBLE);
                        imgUsernameUnique.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_done));
                        textUsernameUnique.setText(R.string.username_exists);
                        textUsernameUnique.setTextColor(mContext.getResources().getColor(R.color.green));
                        break;
                    case "taken":
                        layoutUsernameUnique.setVisibility(View.VISIBLE);
                        imgUsernameUnique.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_clear));
                        textUsernameUnique.setText(R.string.username_taken);
                        textUsernameUnique.setTextColor(mContext.getResources().getColor(R.color.red));
                        break;
                }

            } catch (NullPointerException e) {
                //error: server resposne NULL
                e.printStackTrace();
            } catch (IOException e) {
                //error: incorrect format error
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(o);
        }
    }   // end of Class:check-username

/*
* ------------------------------ CLASS: SIGN UP ASYNC  ---------------------------
* */

    class SignupTask extends AsyncTask {
        Context mContext;
        String mUsername, mPassword, mEmail;
        ProgressDialog mProgressDialog;
        String mUserMsg;

        public SignupTask(Context context, String username, String email, String password) {
            this.mContext = context;
            this.mUsername = username;
            this.mEmail = email;
            this.mPassword = password;
            mProgressDialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.setMessage("Please wait..");
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            MyPrefs prefs = new MyPrefs(mContext);
            URL url;
            HttpURLConnection connection;

            String urlString = prefs.getServerUrl() + "/add_user.php";

            try {
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(1000);

                OutputStreamWriter sw = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
                BufferedWriter bw = new BufferedWriter(sw);

                // set parameter values for POST request
                HashMap<String, String> param = new HashMap<String, String>();
                param.put("username", mUsername);
                param.put("email", mEmail);
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
                mUserMsg = "Please make sure that Internet Connection is available," +
                        " and server IP is inserted in settings";
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            try {

                //connection isn't available or something is wrong with server address
                if (mUserMsg != null)
                    throw new IOException();

                BufferedReader br = (BufferedReader) o;
                if (br == null)
                    throw new NullPointerException("BufferedReader instance couldn't be NULL");

                String data = "", line;
                while ((line = br.readLine()) != null) {
                    data += line;
                }

                if (data == null || data.equals(""))
                    throw new NullPointerException("Server response couldn't be empty");

                // json-string to json-array then get response from first object
                JSONArray jsonArray = new JSONArray(data);
                JSONObject jsonObject = jsonArray.getJSONObject(0);

                // parse server response
                switch (jsonObject.getString("response")) {
                    case "success":
                        mUserMsg = "Account has been successfully created";

                        //back to login activity
                        ((Activity) mContext).finish();
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
                mUserMsg = e.getMessage();
                e.printStackTrace();
            } finally {
                if (mUserMsg != null)
                    Toast.makeText(mContext, mUserMsg, Toast.LENGTH_SHORT).show();
            }

            // hide the progressDialog
            mProgressDialog.hide();

            super.onPostExecute(o);
        }
    }   // end of Sign up-Async Class

}