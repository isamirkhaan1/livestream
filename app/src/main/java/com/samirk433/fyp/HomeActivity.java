package com.samirk433.fyp;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import data.MyPrefs;
import data.PostModel;
import utils.MyPermissions;
import utils.PostRequestData;

public class HomeActivity extends AppCompatActivity {
    public static final String TAG = "HomeActivity";

    public static List<PostModel> mData;

    FloatingActionButton mFloatingBtn;
    RecyclerView mRecyclerView;
    RecyclerView.Adapter mAdapter;

    MyPrefs mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        if (!MyPermissions.checkPermissions(this)) {
            MyPermissions.showPermissionDialog(this);
        }

        mData = new ArrayList<>();
        mPrefs = new MyPrefs(this);

        new FetchTask().execute();

        mFloatingBtn = (FloatingActionButton) findViewById(R.id.floating_live);
        mFloatingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });


        if (mPrefs.getServerUrl().equals("")) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            finish();
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.list_vid);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_logout:
                mPrefs.setUserId(-1);
                mPrefs.setUsername(null);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Grant the permissions from Settings.", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    class CustomAdapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_vid, parent, false);
            return new CustomViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

            ((CustomViewHolder) holder).textTitle.setText(mData.get(position).title);
            ((CustomViewHolder) holder).textDate.setText(mData.get(position).date);
            ((CustomViewHolder) holder).textDesc.setText(mData.get(position).descrip);
            ((CustomViewHolder) holder).textUpvotes.setText(mData.get(position).upvotes + "");
            ((CustomViewHolder) holder).textDownvotes.setText(mData.get(position).downvotes + "");

            if (HomeActivity.mData.get(position).vote == 1) {
                ((CustomViewHolder) holder).textUpvotes.setTextColor(getResources().getColor(R.color.colorPrimary));
            } else if (HomeActivity.mData.get(position).vote == -1) {
                ((CustomViewHolder) holder).textDownvotes.setTextColor(getResources().getColor(R.color.colorPrimary));
            }

            Picasso.with(HomeActivity.this).load(Uri.parse(
                    "http://img.youtube.com/vi/" + mData.get(position).videoId + "/0.jpg"))
                    .resize(550,350)
                    .centerCrop()
                    .into(((CustomViewHolder) holder).imgVid);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(HomeActivity.this, YoutubePlayerActivity.class);
                    intent.putExtra("id", position);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView imgVid;
        TextView textTitle, textDate, textDesc, textUpvotes, textDownvotes;

        public CustomViewHolder(View itemView) {
            super(itemView);
            imgVid = (ImageView) itemView.findViewById(R.id.img_vid);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDate = (TextView) itemView.findViewById(R.id.text_date);
            textDesc = (TextView) itemView.findViewById(R.id.text_desc);
            textUpvotes = (TextView) itemView.findViewById(R.id.text_upvotes);
            textDownvotes = (TextView) itemView.findViewById(R.id.text_downvotes);


        }
    }

    class FetchTask extends AsyncTask<Void, String, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] objects) {
            String response = null;

            try {
                URL url = new URL(mPrefs.getServerUrl() + "/post.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);

                OutputStream os = connection.getOutputStream();

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                // set pa
                // rameter values for post-request
                HashMap<String, String> param = new HashMap<String, String>();
                param.put("user_id", "1");
                param.put("action", "get");

                bw.write(PostRequestData.getData(param));
                bw.flush();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    response = "";
                    String line;
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }

                    // json-string to json-array then get response from first object
                    JSONArray jsonArray = new JSONArray(response);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);

                    if (jsonObject.getString("response").equals("success")) {
                        response = "success";

                        PostModel post = null;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            jsonObject = jsonArray.getJSONObject(i);
                            post = new PostModel();

                            post.postId = jsonObject.getInt("id");
                            post.title = jsonObject.getString("title");
                            post.descrip = jsonObject.getString("description");
                            post.videoId = jsonObject.getString("video_id");
                            // post.votes = jsonObject.getInt("votes");
                            post.vote = jsonObject.getInt("voted");
                            post.upvotes = jsonObject.getInt("upvotes");
                            post.downvotes = jsonObject.getInt("downvotes");
                            post.location = jsonObject.getString("location");
                            post.date = jsonObject.getString("date");
                            post.modified_date = jsonObject.getString("modified_date");

                            post.userId = jsonObject.getInt("user_id");
                            post.username = jsonObject.getString("username");

                            mData.add(post);
                        }
                    } else {
                        response = "server_error_1";
                    }

                } else {
                    response = "server_error";
                }

                //todo abstract from json form

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                response = "unknown_error";
                e.printStackTrace();
            } catch (MalformedURLException e) {
                response = "incorrect_url";
                e.printStackTrace();
            } catch (IOException e) {
                response = "network_error";
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);

            if (response.equals("server_error")) {

            } else if (response.equals("server_error_1")) {

            } else if (response.equals("network_error")) {

            } else if (response.equals("incorrect_url")) {

            } else if (response.equals("unknown_error")) {

            } else if (response == null) {

            }

            if (mData != null) {
                mAdapter = new CustomAdapter();
                mRecyclerView.setAdapter(mAdapter);
            } else {
                Toast.makeText(HomeActivity.this, "mData is NULL", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
