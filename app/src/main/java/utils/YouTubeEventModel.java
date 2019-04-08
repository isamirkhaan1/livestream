package utils;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveStream;

/**
 * Created by root on 8/13/17.
 */

public class YouTubeEventModel {

    public static YouTube youtube;

    public static LiveBroadcast liveBroadcast;
    public static LiveStream liveStream;

    public static String liveBroadcastId;
    public static String liveStreamId;
    public static String liveStreamName;
    public static String rtmpUrl;
    public static String liveUrl;

    public static String streamTitle;
    public static String description;
    public static String latitude = "0";
    public static String longitude = "0";
    public static String location = "Peshawar";

}
