/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package utils;

import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.LiveBroadcasts.Transition;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.IngestionInfo;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastContentDetails;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamListResponse;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.LiveStreamStatus;
import com.google.api.services.youtube.model.MonitorStreamInfo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class YouTubeApi {
    public static final String TAG = YouTubeApi.class.getName();
    public static final String APP_NAME = "483330351700-2abbdmptm4ti25hi1o19tg87d7014tr9.apps.googleusercontent.com";
    private static final int FUTURE_DATE_OFFSET_MILLIS = 8 * 1000;


    //todo find this UserRecoverableAuthIOException
    public static boolean CreateLiveEvent(YouTube youtube, String name, String description) throws Exception, UserRecoverableAuthIOException {

             /* We need a date that's in the proper ISO format and is in the future,
                 since the API won't
                create events that start in the past.*/

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        long futureDateMillis = System.currentTimeMillis() + FUTURE_DATE_OFFSET_MILLIS;
        Date futureDate = new Date();
        futureDate.setTime(futureDateMillis);
        String date = dateFormat.format(futureDate);

        Log.i(TAG, String.format(
                "Creating event: name='%s', description='%s', date='%s'.",
                name, description, date));

        if (description == null) {
            description = "This video is published on from Final Year Project" + date;
        } else {
            description += "\n Uploaded via Final Year Project";
        }

        LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
        broadcastSnippet.setTitle(name);
        broadcastSnippet.setDescription(description);
        broadcastSnippet.setScheduledStartTime(new DateTime(futureDate));

        LiveBroadcastContentDetails contentDetails = new LiveBroadcastContentDetails();
        MonitorStreamInfo monitorStream = new MonitorStreamInfo();
        monitorStream.setEnableMonitorStream(false);
        contentDetails.setMonitorStream(monitorStream);
        contentDetails.setEnableLowLatency(true);
        contentDetails.setEnableDvr(true);
        contentDetails.setRecordFromStart(true);

        // Set the broadcast's privacy status to "private". See:
        // https://developers.google.com/youtube/v3/live/docs/liveBroadcasts#status.privacyStatus
        LiveBroadcastStatus status = new LiveBroadcastStatus();
        status.setPrivacyStatus("public");

        LiveBroadcast broadcast = new LiveBroadcast();
        broadcast.setKind("youtube#liveBroadcast");
        broadcast.setSnippet(broadcastSnippet);
        broadcast.setStatus(status);
        broadcast.setContentDetails(contentDetails);

        // Create the insert request
        YouTube.LiveBroadcasts.Insert liveBroadcastInsert = youtube
                .liveBroadcasts().insert("snippet,status,contentDetails",
                        broadcast);

        // Request is executed and inserted broadcast is returned
        LiveBroadcast returnedBroadcast = liveBroadcastInsert.execute();

        // Create a snippet with title.
        LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
        streamSnippet.setTitle(name);

        // Define the content distribution network settings for the
        // video stream. The settings specify the stream's format and
        // ingestion type. See:
        // https://developers.google.com/youtube/v3/live/docs/liveStatus
        CdnSettings cdn = new CdnSettings();
        cdn.setFormat("360p");
        cdn.setIngestionType("rtmp");


        LiveStream stream = new LiveStream();
        stream.setKind("youtube#liveStream");
        stream.setSnippet(streamSnippet);
        stream.setCdn(cdn);

        // Create the insert request
        YouTube.LiveStreams.Insert liveStreamInsert = youtube.liveStreams()
                .insert("snippet,cdn", stream);

        // Request is executed and inserted stream is returned
        LiveStream returnedStream = liveStreamInsert.execute();

        LiveStreamStatus liveStreamStatus = new LiveStreamStatus();
        liveStreamStatus.setStreamStatus("active");
        returnedStream.setStatus(liveStreamStatus);


        // Create the bind request
        YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtube
                .liveBroadcasts().bind(returnedBroadcast.getId(),
                        "id,contentDetails");

        // Set stream id to bind
        liveBroadcastBind.setStreamId(returnedStream.getId());

        // Request is executed and bound broadcast is returned
        liveBroadcastBind.execute();
        // String sId = liveBroadcastBind.getStreamId();

        LiveBroadcast currBroadcast = getLastLiveEvent(youtube);
        if (currBroadcast != null)
            return true;

        return false;
    }

    public static LiveBroadcast getLastLiveEvent(YouTube youtube) throws IOException {
        Log.i(TAG, "Get Live Events()");

        YouTube.LiveBroadcasts.List liveBroadcastRequest = youtube
                .liveBroadcasts().list("id,snippet,contentDetails,status");

        liveBroadcastRequest.setBroadcastStatus("upcoming");

        // List request is executed and list of broadcasts are returned
        LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();

        // Get the list of broadcasts associated with the user.
        List<LiveBroadcast> returnedList = returnedListResponse.getItems();

        if (returnedList == null)
            return null;

        LiveBroadcast currBroadcast = null;
        LiveStream currStream;

        if (returnedList.size() > 0)
            currBroadcast = returnedList.get(returnedList.size() - 1);
        else
            return null;

        //current stream id
        String currStreamId = currBroadcast.getContentDetails().getBoundStreamId();
        if (currStreamId != null) {
            currStream = GetLiveStream(youtube, currStreamId);
            if (currStream != null) {
                //YouTubeEventModel.youtube = youtube;
                YouTubeEventModel.liveBroadcast = currBroadcast;
                YouTubeEventModel.liveBroadcastId = currBroadcast.getId();
                YouTubeEventModel.liveStream = currStream;
                YouTubeEventModel.liveStreamId = currStreamId;
                //fuckingB.getContentDetails().getBoundStreamId();

                YouTubeEventModel.liveUrl = "http://www.youtube.com/watch?v=" + currBroadcast.getId();

                IngestionInfo ingestionInfo = currStream.getCdn().getIngestionInfo();
                String streamName = ingestionInfo.getStreamName();
                YouTubeEventModel.rtmpUrl = ingestionInfo.getIngestionAddress() + "/" + streamName;

                        /*String backupAddress = ingestionInfo.getBackupIngestionAddress() + "/" + streamName;
                        YouTubeEventModel.rtmpUrl_B = backupAddress;*/
            }

        }
        return currBroadcast;
    }

    public static boolean StartEvent(YouTube youtube, String broadcastId) {

        try {
            Thread.sleep(1000 * 7); //7 sec

            Transition transitionRequest = youtube.liveBroadcasts().transition(
                    "live", broadcastId, "status");
            transitionRequest.execute();

            return true;
        } catch (InterruptedException e) {
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "Cannot start event" + e.toString());
        }
        return false;

    }

    public static boolean EndEvent(YouTube youtube, String broadcastId){
        try {

            Transition transitionRequest = youtube.liveBroadcasts().transition(
                    "complete", broadcastId, "status");
            transitionRequest.execute();

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Cannot End event" + e.toString());
        }
        return false;

    }

    public static LiveStream GetLiveStream(YouTube youTube, String streamId) {
        Log.e(TAG, "Getting Live Stream");
        try {
            YouTube.LiveStreams.List liveStreamRequest = youTube.liveStreams()
                    .list("id,snippet,cdn,status");
            liveStreamRequest.setId(streamId);
            LiveStreamListResponse returnedStream = liveStreamRequest.execute();
            List<LiveStream> streamList = returnedStream.getItems();
            if (streamList.isEmpty()) {
                return null;
            }

            Log.d(TAG, "this is asly status : " + streamList.get(0).getStatus().getStreamStatus());
            return streamList.get(0);
        } catch (Exception e) {
            Log.e(TAG, "Error while gettingLiveStreams" + e.toString());
        }
        return null;
    }

    public static String GetIngestionAddress(YouTube youtube, String streamId)
            throws IOException {
        YouTube.LiveStreams.List liveStreamRequest = youtube.liveStreams()
                .list("cdn,status");
        liveStreamRequest.setId(streamId);
        LiveStreamListResponse returnedStream = liveStreamRequest.execute();

        List<LiveStream> streamList = returnedStream.getItems();
        if (streamList.isEmpty()) {
            return null;
        }

        IngestionInfo ingestionInfo = streamList.get(0).getCdn().getIngestionInfo();
        return ingestionInfo.getIngestionAddress() + "/"
                + ingestionInfo.getStreamName();
    }

  /*  public static boolean DeleteEvent(YouTube youTube, YouTubeEventData eventData) {
        try {
            // delete stream
//            String streamId = eventData.GetEvent().getContentDetails().getBoundStreamId();
//            YouTubeRequest deleteStream = youTube.liveStreams().delete(streamId);
//            deleteStream.execute();
            // delete broadcast
            YouTubeRequest delete = youTube.liveBroadcasts().delete(eventData.GetId());
            delete.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }*/

    /* public static void UpdatePrivacyStatus(YouTube youTube, YouTubeEventData eventData, String status) throws Exception {

         LiveBroadcastStatus broadcastStatus = new LiveBroadcastStatus();
         broadcastStatus.setPrivacyStatus(status);

         LiveBroadcast broadcast = new LiveBroadcast();
         broadcast.setId(eventData.GetEvent().getId());
         broadcast.setStatus(broadcastStatus);

         //broadcast.setStatus(broadcastStatus);
         YouTubeRequest update = youTube.liveBroadcasts().update("status", broadcast);
         update.execute();

         // update current data
         eventData.GetEvent().getStatus().setPrivacyStatus(status);

     }
 */
    private static String GetEventDate(long offsetMilliseconds) {
        String eventDate = "";
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            long futureDateMillis = System.currentTimeMillis() + offsetMilliseconds;
            Date futureDate = new Date();
            futureDate.setTime(futureDateMillis);
            eventDate = dateFormat.format(futureDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return eventDate;
    }

   /* public static void UpdateTitle(YouTube youTube, YouTubeEventData eventData, String title) throws Exception {

        String startDate = GetEventDate(30 * 1000);
        String endDate = GetEventDate(600 * 1000);
        LiveBroadcastSnippet snippet = new LiveBroadcastSnippet();
        snippet.setTitle(title);
        snippet.setScheduledStartTime(new DateTime(startDate));
        snippet.setScheduledEndTime(new DateTime(endDate));

        LiveBroadcast broadcast = new LiveBroadcast();
        broadcast.setId(eventData.GetEvent().getId());
        broadcast.setSnippet(snippet);


        //broadcast.setStatus(broadcastStatus);
        YouTubeRequest update = youTube.liveBroadcasts().update("snippet", broadcast);
        update.execute();

        // update current data
        eventData.GetEvent().getSnippet().setTitle(title);

    }*/

    public static boolean rtmpConnectionSuccess = false;
    public static final String youtube = "a.rtmp.youtube.com";
    public static final String youtubeBackup = "b.rtmp.youtube.com";
    public static final String primaryServer = "rtmp://a.rtmp.youtube.com/live2";
    public static final String backupServer = "rtmp://b.rtmp.youtube.com/live2?backup=1";

    public static boolean CheckRtmpConnection() {
        rtmpConnectionSuccess = false;

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                rtmpConnectionSuccess = Helper.CheckTcpPortOpen2(youtube, 1935, 4000);
//            }
//        });
//        thread.start();
        //  rtmpConnectionSuccess = Helper.CheckTcpPortOpen2(youtube, 1935, 4000);

        return rtmpConnectionSuccess;
    }
}
