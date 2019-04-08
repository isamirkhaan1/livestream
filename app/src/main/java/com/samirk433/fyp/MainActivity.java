package com.samirk433.fyp;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.internal.zzt;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.seu.magicfilter.utils.MagicFilterType;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import data.MyPrefs;
import utils.PostRequestData;
import utils.YouTubeEventModel;
import utils.YouTubeApi;

public class MainActivity extends AppCompatActivity implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {
    private static final String TAG = MainActivity.class.getName();
    public static final String[] SCOPES = {Scopes.PROFILE, YouTubeScopes.YOUTUBE};

    public static GoogleAccountCredential mAccountCredential;

    private ImageView btnPublish;
    private ImageView btnSwitchCamera, btnShare;

    MyPrefs mPrefs;

    boolean isStreamingStart = false;
  /*  private Button btnRecord;
    private Button btnSwitchEncoder;*/

  /*  private SharedPreferences sp;
    public static String rtmpUrl = "rtmp://";*/
    //  private String recPath = Environment.getExternalStorageDirectory().getPath();

    private SrsPublisher mPublisher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(com.samirk433.fyp.R.layout.activity_main);

        // response screen rotation event
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);



        mPrefs = new MyPrefs(this);

        if(mPrefs.getAccountName() == null || mPrefs.getAccountName().length()<1) {
            loadUserAccount(savedInstanceState);
        }

        mPublisher = new SrsPublisher((SrsCameraView) findViewById(R.id.glsurfaceview_camera));
        mPublisher.setEncodeHandler(new SrsEncodeHandler(MainActivity.this));
        mPublisher.setRtmpHandler(new RtmpHandler(MainActivity.this));
        mPublisher.setRecordHandler(new SrsRecordHandler(MainActivity.this));
        mPublisher.setPreviewResolution(640, 360);
        mPublisher.setOutputResolution(360, 640);
        mPublisher.setVideoHDMode();
        mPublisher.startCamera();


        btnPublish = (ImageView) findViewById(R.id.publish);
        btnSwitchCamera = (ImageView) findViewById(R.id.swCam);
        btnShare = (ImageView) findViewById(R.id.imgShare);


        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStreamingStart) {
                   /* if (YouTubeEventModel.rtmpUrl == null) {
                        Toast.makeText(MainActivity.this, "Youtube Event isn't ready yet", Toast.LENGTH_SHORT).show();
                        return;
                    }*/

                    showNewPostDialog();

                   /* if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                        Toast.makeText(getApplicationContext(), "Use hard encoder", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Use soft encoder", Toast.LENGTH_SHORT).show();
                    }
                    btnPublish.setText("stop");
                    btnSwitchEncoder.setEnabled(false);*/


                } else {
                    new EndStreamingTask().execute();

                   /* btnPublish.setText("publish");
                    btnRecord.setText("record");
                    btnSwitchEncoder.setEnabled(true);*/
                }
            }
        });

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPublisher.switchCameraFace((mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());

            }
        });

       /* btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnRecord.getText().toString().contentEquals("record")) {
                    if (mPublisher.startRecord(recPath)) {
                        btnRecord.setText("pause");
                    }
                } else if (btnRecord.getText().toString().contentEquals("pause")) {
                    mPublisher.pauseRecord();
                    btnRecord.setText("resume");
                } else if (btnRecord.getText().toString().contentEquals("resume")) {
                    mPublisher.resumeRecord();
                    btnRecord.setText("pause");
                }
            }
        });*/

        btnShare = (ImageView) findViewById(R.id.imgShare);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Check me out on this URL " +
                        YouTubeEventModel.liveUrl);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });

       /* btnSwitchEncoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                    mPublisher.switchToSoftEncoder();
                    btnSwitchEncoder.setText("hard encoder");
                } else if (btnSwitchEncoder.getText().toString().contentEquals("hard encoder")) {
                    mPublisher.switchToHardEncoder();
                    btnSwitchEncoder.setText("soft encoder");
                }
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
       /* if (id == R.id.action_settings) {
            return true;
        } else {
            switch (id) {
                case R.id.cool_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.COOL);
                    break;
                case R.id.beauty_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
                    break;
                case R.id.early_bird_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EARLYBIRD);
                    break;
                case R.id.evergreen_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EVERGREEN);
                    break;
                case R.id.n1977_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.N1977);
                    break;
                case R.id.nostalgia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.NOSTALGIA);
                    break;
                case R.id.romance_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.ROMANCE);
                    break;
                case R.id.sunrise_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNRISE);
                    break;
                case R.id.sunset_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNSET);
                    break;
                case R.id.tender_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TENDER);
                    break;
                case R.id.toast_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TOASTER2);
                    break;
                case R.id.valencia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.VALENCIA);
                    break;
                case R.id.walden_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WALDEN);
                    break;
                case R.id.warm_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WARM);
                    break;
                case R.id.original_filter:
                default:
                    mPublisher.switchCameraFilter(MagicFilterType.NONE);
                    break;
            }
        }
        setTitle(item.getTitle());*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final ImageView btn = (ImageView) findViewById(R.id.publish);
        btn.setEnabled(true);
        mPublisher.resumeRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPublisher.pauseRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPublisher.stopPublish();
        mPublisher.stopRecord();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPublisher.stopEncode();
        mPublisher.stopRecord();
        //  btnRecord.setText("record");
        mPublisher.setScreenOrientation(newConfig.orientation);

        //todo this may cause a bug,
        if (isStreamingStart) {
            mPublisher.startEncode();
        }
        mPublisher.startCamera();
    }

    private void showJsonErrorMessage(final String json) {
        ((Activity) this).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    /*if (mEventsListFragment != null) {
                        mEventsListFragment.SetErrorMessage(jsonObject.getString("message"));
                    } else {
                        Snackbar.make(fab, jsonObject.getString("message"), Snackbar.LENGTH_SHORT).show();
                        //Toast.makeText(mContext, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                    }*/
                    Toast.makeText(MainActivity.this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

   /* private static String getRandomAlphaString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private static String getRandomAlphaDigitString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }*/

    private void handleException(Exception e) {
        try {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            btnPublish.setImageResource(R.drawable.ic_logo);
           /* btnRecord.setText("record");
            btnSwitchEncoder.setEnabled(true);*/
        } catch (Exception e1) {
            //
        }
    }

    // Implementation of SrsRtmpListener.

    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoStreaming() {
    }

    @Override
    public void onRtmpAudioStreaming() {
    }

    @Override
    public void onRtmpStopped() {
        Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.i(TAG, String.format("Output Fps: %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Audio bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    // Implementation of SrsRecordHandler.

    @Override
    public void onRecordPause() {
        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    // Implementation of SrsEncodeHandler.

    @Override
    public void onNetworkWeak() {
        Toast.makeText(getApplicationContext(), "Network weak", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNetworkResume() {
        Toast.makeText(getApplicationContext(), "Network resume", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            loadUserAccount(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case 1:
                if(data.getExtras() == null)
                    return;

                String accountName = data.getExtras().getString(
                        AccountManager.KEY_ACCOUNT_NAME);

                if (accountName != null) {
                    try {
                        mAccountCredential.setSelectedAccountName(accountName);
                        mPrefs.setAccountName(accountName);

                        Toast.makeText(this, "Account " + accountName + " has been selected"
                                , Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Log.d(TAG, e.toString());
                    }
                }
                break;
            case 3:
                selectAccount();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void loadUserAccount(Bundle savedInstanceState) {
        try {
            if (MainActivity.mAccountCredential == null ||
                    MainActivity.mAccountCredential.getSelectedAccountName() == null) {

                MainActivity.mAccountCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(MainActivity.SCOPES));

                // set exponential backoff policy
                MainActivity.mAccountCredential.setBackOff(new ExponentialBackOff());
            }

            if (mPrefs.getAccountName() == null || mPrefs.getAccountName().length() < 1) {
                selectAccount();
            } else {
                MainActivity.mAccountCredential.setSelectedAccountName(mPrefs.getAccountName());
                Toast.makeText(this, "Account " + MainActivity.mAccountCredential.getSelectedAccountName()
                                + " has already been selected"
                        , Toast.LENGTH_SHORT).show();
            }


        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void selectAccount() {
        startActivityForResult(MainActivity.mAccountCredential.newChooseAccountIntent(),
                1);
    }


    public void showNewPostDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_post, null);
        if (v == null)
            return;

        final EditText etTitle = v.findViewById(R.id.text_title);
        final EditText etDesc = v.findViewById(R.id.text_desc);
        final EditText etLocation = v.findViewById(R.id.text_loc);
        builder.setView(v);

        builder.setPositiveButton("Start Streaming", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                String title = "";
                String desc = "";

                if (etTitle.getText().toString().trim().length() < 1) {
                    Toast.makeText(MainActivity.this, "Cannot Start without Streaming Title",
                            Toast.LENGTH_SHORT).show();
                    return;
                } else {

                    if (etDesc.getText().toString().trim().length() < 1) {
                        desc = null;
                    }

                    if(etLocation.getText().toString().trim().length()>1){
                        YouTubeEventModel.location = etLocation.getText().toString().trim();
                    }
                    title = etTitle.getText().toString().trim();
                    desc = etDesc.getText().toString().trim();
                    YouTubeEventModel.streamTitle = title;
                    YouTubeEventModel.description = desc;


                    new CreateLiveEventTask().execute(title, desc);
                }
            }
        });


        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }


    private class CreateLiveEventTask extends
            AsyncTask<String, Void, String> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
//            progressDialog = ProgressDialog.show(MainActivity.this, null, "Creating new event...");
        }

        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, "CreateLiveEventTask . doInBackground()");

            String title = null, desc = null;
            if (params == null) {
                title = "Testing";
                desc = "No Description ";
            } else {
                title = params[0];
                desc = params[1];
            }

            if (MainActivity.mAccountCredential == null) {
                loadUserAccount(null);
            } else {
                MainActivity.mAccountCredential.setSelectedAccountName(mPrefs.getAccountName());
            }

            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = new GsonFactory();

            YouTube youtube = new YouTube.Builder(httpTransport, jsonFactory,
                    MainActivity.mAccountCredential)
                    .setApplicationName(YouTubeApi.APP_NAME)
                    .build();

            try {
                String date = new Date().toString();
                boolean success = YouTubeApi.CreateLiveEvent(youtube, title,
                        desc);
                if (success) {
                    return "success";
                } else {
                    return "error";
                }
            } catch (UserRecoverableAuthIOException e) {
                Log.e(TAG, e.toString());
                ((Activity) MainActivity.this).startActivityForResult(e.getIntent(), 3);
            } catch (GoogleJsonResponseException e) {
                Log.e(TAG, e.toString());

            } catch (Exception e) {
                Log.e(TAG, e.toString());
                selectAccount();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String st) {

//            Button buttonCreateEvent = (Button) findViewById(R.id.create_button);
//            buttonCreateEvent.setEnabled(true);
//            progressDialog.dismiss();

            String userMsg = "Cannot create a Live Event";
            if (st == null) {
                userMsg = "Undefined Error";
            } else if (st.equals("error")) {
                userMsg = "Error while creating event, please try again";
            } else if (st.equals("success")) {
                userMsg = "Live Event has been created successfully";

                isStreamingStart = true;
                    /*SharedPreferences.Editor editor = sp.edit();
                    editor.putString("rtmpUrl", YouTubeEventModel.rtmpUrl);
                    editor.apply();*/

                mPublisher.switchToSoftEncoder();
                mPublisher.startPublish(YouTubeEventModel.rtmpUrl);
                mPublisher.startCamera();
                btnPublish.setImageResource(R.drawable.ic_logo_light);

                new StartStreamingTask().execute();
            }

            Toast.makeText(MainActivity.this, userMsg, Toast.LENGTH_SHORT).show();

        }
    }

    private class StartStreamingTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null, "Starting Streaming..");
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = new GsonFactory();

            YouTube youtube = new YouTube.Builder(httpTransport, jsonFactory,
                    MainActivity.mAccountCredential)
                    .setApplicationName(YouTubeApi.APP_NAME)
                    .build();


            if (YouTubeEventModel.liveStream == null)
                YouTubeEventModel.liveStream = YouTubeApi.GetLiveStream(youtube, YouTubeEventModel.liveStreamId);

            String status = YouTubeEventModel.liveStream.getStatus().getStreamStatus();

            int i = 0;
            while (!status.equals("active")) {
                Log.d(TAG, i + " " + status);
                try {

                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    Log.d(TAG, e.toString());
                }

                YouTubeEventModel.liveStream = YouTubeApi.GetLiveStream(youtube, YouTubeEventModel.liveStreamId);
                status = YouTubeEventModel.liveStream.getStatus().getStreamStatus();

                ++i;
            }

            Log.d(TAG, "final status : " + status);
            if (status.equals("active"))
                YouTubeApi.StartEvent(youtube, YouTubeEventModel.liveBroadcastId);
            return "success";

        }

        @Override
        protected void onPostExecute(String st) {
            progressDialog.dismiss();

            String userMsg = "Cannot create a Live Event";
            if (st == null) {

            } else if (st.equals("success")) {
                userMsg = "Live Event has been started successfully";
               new  NewPostTask().execute();
            }

            Toast.makeText(MainActivity.this, userMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private class EndStreamingTask extends AsyncTask<Void, Void, Boolean> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null, "Finishing Streaming..");
        }

        @Override
        protected Boolean doInBackground(Void... objects) {

            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = new GsonFactory();

            try {
                YouTube youtube = new YouTube.Builder(httpTransport, jsonFactory,
                        MainActivity.mAccountCredential)
                        .setApplicationName(YouTubeApi.APP_NAME)
                        .build();


                boolean result = false;
                if (YouTubeEventModel.liveStream != null)
                    result = YouTubeApi.EndEvent(youtube, YouTubeEventModel.liveBroadcastId);

                return result;
            } catch (Exception e) {

            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean o) {
            super.onPostExecute(o);

            progressDialog.cancel();
            if (o) {
                mPublisher.stopPublish();
                mPublisher.stopRecord();
                btnPublish.setImageResource(R.drawable.ic_logo);
            } else {
                Toast.makeText(MainActivity.this, "Error while stopping streaming, please try again",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    class NewPostTask extends AsyncTask {
        String mUserMsg;



        @Override
        protected Object doInBackground(Object[] params) {
            URL url;
            HttpURLConnection connection;
            String urlString = mPrefs.getServerUrl() + "/post.php";
            try {
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(1000);

                OutputStreamWriter sw = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
                BufferedWriter bw = new BufferedWriter(sw);

                // add parameters to POST request
                HashMap<String, String> param = new HashMap<String, String>();
                param.put("action", "add");
                param.put("title",  YouTubeEventModel.streamTitle);
                param.put("description", YouTubeEventModel.description);


                param.put("video_id", YouTubeEventModel.liveUrl.split("v=")[1]);
                param.put("latitude", YouTubeEventModel.latitude);
                param.put("longitude", YouTubeEventModel.longitude);
                param.put("location", YouTubeEventModel.location);
                param.put("user_id", mPrefs.getUserId() + "");


                bw.write(PostRequestData.getData(param));
                bw.flush();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    String data = "", line;
                    while ((line = br.readLine()) != null) {
                        data += line;
                    }

                    // json-string to json-array then get response from that
                    JSONArray jsonArray = new JSONArray(data);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);

                    switch (jsonObject.getString("response")) {
                        case "success":
                            //todo: do something...
                            break;

                        case "error":
                            mUserMsg = "Something went wrong on server";
                            break;
                    }
                } else {
                    mUserMsg = "Server Couldn't process the request";
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                mUserMsg = "Please make sure that Internet Connection is available," +
                        " and server IP is inserted in settings";
                e.printStackTrace();
            } catch (JSONException e) {
                mUserMsg = "Incorrect formatted data from server";
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            if (mUserMsg != null)
                Toast.makeText(MainActivity.this, mUserMsg, Toast.LENGTH_SHORT).show();

        }
    }
}
