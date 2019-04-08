package com.samirk433.fyp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import data.MyPrefs;
import data.PostModel;

public class YoutubePlayerActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {
    // api key:
    /* AIzaSyDv4i0n756mYtFhI-TMl7cij4l4KGP4zEs */

    YouTubePlayerView mPlayerView;
    LinearLayout mLayoutUpvote, mLayoutDownvote;
    TextView mTextTitle, mTextDesc, mTextDate, mTextUpvotes, mTextDownvotes;

    MyPrefs mPrefs;
    int mId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);

        mId = getIntent().getIntExtra("id", -1);
        if (mId == -1 || HomeActivity.mData == null) {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show();
            finish();
        }

        mPrefs = new MyPrefs(this);

        mTextTitle = findViewById(R.id.text_title);
        mTextDesc = findViewById(R.id.text_desc);
        mTextDate = findViewById(R.id.text_date);
        mTextUpvotes = findViewById(R.id.text_upvotes);
        mTextDownvotes = findViewById(R.id.text_downvotes);

        mTextTitle.setText(HomeActivity.mData.get(mId).title);
        mTextDesc.setText(HomeActivity.mData.get(mId).descrip);
        mTextDate.setText(HomeActivity.mData.get(mId).date);
        mTextUpvotes.setText(HomeActivity.mData.get(mId).upvotes + "");
        mTextDownvotes.setText(HomeActivity.mData.get(mId).downvotes + "");

        mPlayerView = findViewById(R.id.player_youtube);
        mPlayerView.initialize(getResources().getString(R.string.youtube_player_api_id), this);

        if (HomeActivity.mData.get(mId).vote == 1) {
            mTextUpvotes.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else if (HomeActivity.mData.get(mId).vote == -1) {
            mTextDownvotes.setTextColor(getResources().getColor(R.color.colorPrimary));
        }

        mLayoutUpvote = findViewById(R.id.layout_like);
        mLayoutUpvote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                castVote(true);
            }
        });

        mLayoutDownvote = findViewById(R.id.layout_dislike);
        mLayoutDownvote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                castVote(false);
            }
        });

    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean b) {
        if (!b) {

            // loadVideo() will auto play video
            // Use cueVideo() method, if you don't want to play it automatically
            if (HomeActivity.mData == null) {
                Toast.makeText(this, "Error 1 while playing video", Toast.LENGTH_SHORT).show();
                finish();
            }

            if (mId < 0) {
                Toast.makeText(this, "Error 2 while playing video", Toast.LENGTH_SHORT).show();
                finish();
            }
            String videoId = HomeActivity.mData.get(mId).videoId;
            player.loadVideo(videoId);


            // Hiding player controls
            player.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, 100).show();
        } else {
            String errorMessage = String.format("Error", errorReason.toString());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void castVote(boolean like) {

        new VoteTask(like ? 1 : 0).execute();

        if (like) {
            if (mTextUpvotes.getTextColors().getDefaultColor() == getResources().getColor(R.color.colorPrimary)) {
                return; /* already upvoted */
            } else if (mTextDownvotes.getTextColors().getDefaultColor() == getResources().getColor(R.color.colorPrimary)) {
                /* already downvoted */
                mTextDownvotes.setTextColor(getResources().getColor(R.color.hr));
                mTextUpvotes.setTextColor(getResources().getColor(R.color.hr));
                HomeActivity.mData.get(mId).downvotes--;
            } else {
                /* upvote */
                mTextUpvotes.setTextColor(getResources().getColor(R.color.colorPrimary));
                HomeActivity.mData.get(mId).upvotes++;
            }

        } else {

            if (mTextDownvotes.getTextColors().getDefaultColor() == getResources().getColor(R.color.colorPrimary)) {
                return; /* already upvoted */
            } else if (mTextUpvotes.getTextColors().getDefaultColor() == getResources().getColor(R.color.colorPrimary)) {
                /* already downvoted */
                mTextDownvotes.setTextColor(getResources().getColor(R.color.hr));
                mTextUpvotes.setTextColor(getResources().getColor(R.color.hr));
                HomeActivity.mData.get(mId).upvotes--;
            } else {
                /* upvote */
                mTextDownvotes.setTextColor(getResources().getColor(R.color.colorPrimary));
                HomeActivity.mData.get(mId).downvotes++;
            }
        }

        mTextUpvotes.setText(HomeActivity.mData.get(mId).upvotes + "");
        mTextDownvotes.setText(HomeActivity.mData.get(mId).downvotes + "");
    }


    class VoteTask extends AsyncTask {
        int mVote;
        String mUserMsg;

        public VoteTask(int vote) {
            this.mVote = vote;
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            URL url;
            HttpURLConnection connection;
            String urlString = mPrefs.getServerUrl() + "/upvote.php";
            String userId = mPrefs.getUserId() + "";

            try {

                // add parameters to GET request
                urlString = String.format(urlString + "?vote=%s&user_id=%s&post_id=%s",
                        URLEncoder.encode(mVote + "", "UTF8"),
                        URLEncoder.encode(userId + "", "UTF8"), URLEncoder.encode(HomeActivity.mData.get(mId).postId + "", "UTF8"));

                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);

                InputStream inputStream = connection.getInputStream();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder sb = new StringBuilder("");
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    JSONArray jsonArray = new JSONArray(sb.toString());
                    final JSONObject jsonObject = jsonArray.getJSONObject(0);

                    switch (jsonObject.getString("response")) {
                        case "success":
                            Drawable upvoteDrawable = null, downvoteDrawable = null;
                            String totalUpvotes = jsonObject.getString("upvotes");
                            String totalDownvotes = jsonObject.getString("downvotes");

                            HomeActivity.mData.get(mId).upvotes = Integer.parseInt(totalUpvotes);
                            HomeActivity.mData.get(mId).downvotes = Integer.parseInt(totalDownvotes);

                            break;
                        default:
                            //if there is any SEVER error while updating upvote & downvote
                            mUserMsg = "Error occured while upvoting/downvoting the post";
                            break;
                    }
                } else {
                    mUserMsg = "Sorry, server Couldn't process the request";
                }
            } catch (MalformedURLException e) {
                mUserMsg = "Cannot upvote/downvote, incorrect format of URL";
                e.printStackTrace();
            } catch (IOException e) {
                mUserMsg = "Please make sure the connection is available";
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {

           /* try {
                BufferedReader br = (BufferedReader) o;
                if (br == null)
                    throw new NullPointerException("Please make sure the connection is available");

                StringBuilder sb = new StringBuilder("");
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                JSONArray jsonArray = new JSONArray(sb.toString());
                final JSONObject jsonObject = jsonArray.getJSONObject(0);

                switch (jsonObject.getString("response")) {
                    case "success":
                        Drawable upvoteDrawable = null, downvoteDrawable = null;
                        String totalUpvotes = jsonObject.getString("upvotes");
                        String totalDownvotes = jsonObject.getString("downvotes");

                        HomeActivity.mData.get(mId).upvotes = Integer.parseInt(totalUpvotes);
                        HomeActivity.mData.get(mId).downvotes = Integer.parseInt(totalDownvotes);

                        break;
                    default:
                        //if there is any SEVER error while updating upvote & downvote
                        mUserMsg = "Error occured while upvoting/downvoting the post";
                        break;
                }
            } catch (JSONException e) {
                mUserMsg = "Error occured while upvoting/downvoting the post";
                e.printStackTrace();
            } catch (IOException e) {
                mUserMsg = "Please make sure the connection is available";
                e.printStackTrace();
            } catch (NullPointerException e) {
                mUserMsg = e.getMessage();
                e.printStackTrace();
            }*/

            if (mUserMsg != null) {
                Toast.makeText(YoutubePlayerActivity.this, mUserMsg, Toast.LENGTH_SHORT).show();
            }

            super.onPostExecute(o);
        }
    }

}
