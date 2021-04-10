package com.lak.pi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.toolbox.ImageLoader;
import com.lak.pi.app.App;
import com.lak.pi.common.ActivityBase;
import com.lak.pi.util.AudioPlayer;
import com.lak.pi.util.Helper;
import com.mikhaellopez.circularimageview.CircularImageView;

import org.json.JSONException;
import org.json.JSONObject;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class VideoChatViewActivity extends ActivityBase {
    public static final String BROADCAST_ACTION_RINGING = "ringing";
    public static final String BROADCAST_ACTION_DISCONNECTING = "disconnecting";
    public static final String BROADCAST_ACTION_ACCEPT = "accept";
    private static final String TAG = VideoChatViewActivity.class.getSimpleName();

    private static final int PERMISSION_REQ_ID = 22;

    // Permission WRITE_EXTERNAL_STORAGE is not mandatory
    // for Agora RTC SDK, just in case if you wanna save
    // logs to external sdcard.
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private RtcEngine mRtcEngine;
    private boolean mCallEnd;
    private boolean mMuted;

    private FrameLayout mLocalContainer;
    private RelativeLayout mRemoteContainer;
    private SurfaceView mLocalView;
    private SurfaceView mRemoteView;
    private boolean is_accepted = false;
    private ImageView mCallBtn;
    private ImageView mMuteBtn;
    private ImageView mSwitchCameraBtn;
    private String requestFrom;
    private boolean isConnected = false;

    // Customized logger view
    //private LoggerRecyclerView mLogView;

    /**
     * Event handler registered into RTC engine for RTC callbacks.
     * Note that UI operations needs to be in UI thread because RTC
     * engine deals with the events in a separate thread.
     */
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        /**
         * Occurs when the local user joins a specified channel.
         * The channel name assignment is based on channelName specified in the joinChannel method.
         * If the uid is not specified when joinChannel is called, the server automatically assigns a uid.
         *
         * @param channel Channel name.
         * @param uid User ID.
         * @param elapsed Time elapsed (ms) from the user calling joinChannel until this callback is triggered.
         */
        @Override
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   Log.d("Join channel suc" ," ");
                    isConnected = true;
                    mRtcEngine.setEnableSpeakerphone(true);
                    int volume = 400;
// Sets the volume of the recorded signal as 200% of the original volume.
                    mRtcEngine.adjustRecordingSignalVolume(volume);


                }
            });
        }

        /**
         * Occurs when the first remote video frame is received and decoded.
         * This callback is triggered in either of the following scenarios:
         *
         *     The remote user joins the channel and sends the video stream.
         *     The remote user stops sending the video stream and re-sends it after 15 seconds. Possible reasons include:
         *         The remote user leaves channel.
         *         The remote user drops offline.
         *         The remote user calls the muteLocalVideoStream method.
         *         The remote user calls the disableVideo method.
         *
         * @param uid User ID of the remote user sending the video streams.
         * @param width Width (pixels) of the video stream.
         * @param height Height (pixels) of the video stream.
         * @param elapsed Time elapsed (ms) from the local user calling the joinChannel method until this callback is triggered.
         */
        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                   Log.d("First remote video dec" ," ");
                    setupRemoteVideo(uid);
                }
            });
        }

        /**
         * Occurs when a remote user (Communication)/host (Live Broadcast) leaves the channel.
         *
         * There are two reasons for users to become offline:
         *
         *     Leave the channel: When the user/host leaves the channel, the user/host sends a
         *     goodbye message. When this message is received, the SDK determines that the
         *     user/host leaves the channel.
         *
         *     Drop offline: When no data packet of the user or host is received for a certain
         *     period of time (20 seconds for the communication profile, and more for the live
         *     broadcast profile), the SDK assumes that the user/host drops offline. A poor
         *     network connection may lead to false detections, so we recommend using the
         *     Agora RTM SDK for reliable offline detection.
         *
         * @param uid ID of the user or host who leaves the channel or goes offline.
         * @param reason Reason why the user goes offline:
         *
         *     USER_OFFLINE_QUIT(0): The user left the current channel.
         *     USER_OFFLINE_DROPPED(1): The SDK timed out and the user dropped offline because no data packet was received within a certain period of time. If a user quits the call and the message is not passed to the SDK (due to an unreliable channel), the SDK assumes the user dropped offline.
         *     USER_OFFLINE_BECOME_AUDIENCE(2): (Live broadcast only.) The client role switched from the host to the audience.
         */
        @Override
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   Log.d("User offline, uid: " ," ");
                    onRemoteUserLeft();
                }
            });
        }
    };
    private String channelid;
    private BroadcastReceiver br_ringin = null;
    private boolean isRinging = false;
    private CountDownTimer countdown;
    private CountDownTimer countdown_recieving;
    private BroadcastReceiver br_disconnect;
    private String accountId;
    private BroadcastReceiver br_accept;
    private String agoraKey;
    private String with_user_photo_url;
    private String with_user_fullname;
    ImageLoader imageLoader = App.getInstance().getImageLoader();
    private AudioPlayer mAudioPlayer;
    private TextView tv_calling;

    private void setupRemoteVideo(int uid) {
        // Only one remote video view is available for this
        // tutorial. Here we check if there exists a surface
        // view tagged as this uid.
        int count = mRemoteContainer.getChildCount();
        View view = null;
        for (int i = 0; i < count; i++) {
            View v = mRemoteContainer.getChildAt(i);
            if (v.getTag() instanceof Integer && ((int) v.getTag()) == uid) {
                view = v;
            }
        }

        if (view != null) {
            return;
        }

        /*
          Creates the video renderer view.
          CreateRendererView returns the SurfaceView type. The operation and layout of the view
          are managed by the app, and the Agora SDK renders the view provided by the app.
          The video display view must be created using this method instead of directly
          calling SurfaceView.
         */
        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        mRemoteContainer.addView(mRemoteView);
        // Initializes the video view of a remote user.
        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        mRemoteView.setTag(uid);


    }

    private void onRemoteUserLeft() {
        removeRemoteVideo();
    }

    private void removeRemoteVideo() {
        if (mRemoteView != null) {
            mRemoteContainer.removeView(mRemoteView);
        }
        // Destroys remote view
        mRemoteView = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat_view);
        initUI();
        agoraKey = App.getInstance().getAgoraApiKey();
        Intent intent = getIntent();
        String from_activity = intent.getStringExtra("from_activity");
        CircularImageView iv_userImage = findViewById(R.id.iv_userImage);
        TextView tv_fullname = findViewById(R.id.tv_fullname);
         tv_calling = findViewById(R.id.tv_calling);

        if(from_activity.equals("chat")){
            String channel_id = intent.getStringExtra("channel_id");
            this.channelid = channel_id;
            requestFrom = intent.getStringExtra("requestFromId");
            accountId = intent.getStringExtra("accountId");
            with_user_photo_url = intent.getStringExtra("with_user_photo_url");
            with_user_fullname = intent.getStringExtra("with_user_fullname");
            imageLoader.get(with_user_photo_url, ImageLoader.getImageListener(iv_userImage, R.drawable.profile_default_photo, R.drawable.profile_default_photo));
            tv_fullname.setText(with_user_fullname);
            tv_calling.setText(getString(R.string.calling));
            initializeTone();
            setCalling();
        }else{
            String data = intent.getStringExtra("data");
            try {
                JSONObject object = new JSONObject(data);
                String channelId = object.getString("channelId");
                this.channelid = channelId;
                requestFrom = object.getString("requestFromId");
                accountId = object.getString("accountId");
                setRecieving();
                initEngineAndJoinChannel();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }






        // Ask for permissions at runtime.
        // This is just an example set of permissions. Other permissions
        // may be needed, and please refer to our online documents.
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {

        }
    }

    private void initializeTone() {
        mAudioPlayer = new AudioPlayer(this);
        setVolumeControlStream(AudioManager.STREAM_RING);
        mAudioPlayer.playRingtone();
    }

    private void setRecieving() {
        is_accepted = true;
        countdown_recieving = new CountDownTimer(16000, 1000) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
               // Toast.makeText(VideoChatViewActivity.this, "User not answering", Toast.LENGTH_SHORT).show();

                //finish();
            }
        }.start();

        br_disconnect = new BroadcastReceiver() {



            public void onReceive(Context context, Intent intent) {
                if(is_accepted)
                    Toast.makeText(context, "User hangup the call", Toast.LENGTH_SHORT).show();
                    else
                Toast.makeText(context, "User declined the call", Toast.LENGTH_SHORT).show();
                finish();

            }
        };

        IntentFilter intFilt5 = new IntentFilter(BROADCAST_ACTION_DISCONNECTING);
        registerReceiver(br_disconnect, intFilt5);


    }

    private void setCalling() {

        br_ringin = new BroadcastReceiver() {



            public void onReceive(Context context, Intent intent) {

                isRinging = true;

            }
        };

        IntentFilter intFilt4 = new IntentFilter(BROADCAST_ACTION_RINGING);
        registerReceiver(br_ringin, intFilt4);


        br_disconnect = new BroadcastReceiver() {



            public void onReceive(Context context, Intent intent) {

                if(is_accepted)
                    Toast.makeText(context, "User hangup the call", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context, "User declined the call", Toast.LENGTH_SHORT).show();
                finish();

            }
        };

        IntentFilter intFilt5 = new IntentFilter(BROADCAST_ACTION_DISCONNECTING);
        registerReceiver(br_disconnect, intFilt5);

        br_accept = new BroadcastReceiver() {


            public void onReceive(Context context, Intent intent) {
                is_accepted = true;
                tv_calling.setText("");
                if(mAudioPlayer != null)
                    mAudioPlayer.stopRingtone();

                initEngineAndJoinChannel();

            }
        };

        IntentFilter intFilt6 = new IntentFilter(BROADCAST_ACTION_ACCEPT);
        registerReceiver(br_accept, intFilt6);

        countdown = new CountDownTimer(16000, 1000) {

            public void onTick(long millisUntilFinished) {
                if(millisUntilFinished> 10000){
                    if(!isRinging){
                     //   countdown.cancel();
                  //            Toast.makeText(VideoChatViewActivity.this, "User is Offline", Toast.LENGTH_SHORT).show();
                        //finish();
                    }else{
                     //   countdown.cancel();
                    }
                }
            }

            public void onFinish() {
                if(!is_accepted ) {
                    Helper helper = new Helper(getApplication());
                    helper.sendNotifyVideoCall(GCM_NOTIFY_MISSEDCALL, requestFrom, accountId);
                    Toast.makeText(VideoChatViewActivity.this, "User not answering", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isRinging){
                    countdown.cancel();
                    Toast.makeText(VideoChatViewActivity.this, "Kullanıcı Çevrimdışı", Toast.LENGTH_SHORT).show();
                    finish();
                    finish();
                }else{
                    //countdown.cancel();
                }
            }
        },7000);


    }

    private void initUI() {
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);

        mCallBtn = findViewById(R.id.btn_call);
        mMuteBtn = findViewById(R.id.btn_mute);
        mSwitchCameraBtn = findViewById(R.id.btn_switch_camera);

        //mLogView = findViewById(R.id.log_recycler_view);

        // Sample logs are optional.
        showSampleLogs();
    }

    private void showSampleLogs() {
//        mLogView.logI("Welcome to Agora 1v1 video call");
//        mLogView.logW("You will see custom logs here");
//        mLogView.logE("You can also use this to show errors");
    }

    private boolean checkSelfPermission(String permission, int requestCode) {


        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                showLongToast("Need permissions " + Manifest.permission.RECORD_AUDIO +
                        "/" + Manifest.permission.CAMERA + "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                finish();
                return;
            }

            // Here we continue only if all permissions are granted.
            // The permissions can also be granted in the system settings manually.
            initEngineAndJoinChannel();
        }
    }

    private void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initEngineAndJoinChannel() {
        // This is our usual steps for joining
        // a channel and starting a call.
        initializeEngine();
        setupVideoConfig();
       setupLocalVideo();
        joinChannel();
    }

    private void initializeEngine() {
        Helper helper = new Helper(this);
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), App.getInstance().getAgoraApiKey(), mRtcEventHandler);

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            helper.sendNotifyVideoCall(GCM_NOTIFY_DISCONNECTING, requestFrom, accountId);
            Toast.makeText(VideoChatViewActivity.this, "Invalid Apid key", Toast.LENGTH_SHORT).show();
           // throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoConfig() {
        // In simple use cases, we only need to enable video capturing
        // and rendering once at the initialization step.
        // Note: audio recording and playing is enabled by default.
        mRtcEngine.enableVideo();

        // Please go to this page for detailed explanation
        // https://docs.agora.io/en/Video/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_rtc_engine.html#af5f4de754e2c1f493096641c5c5c1d8f
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {
        // This is used to set a local preview.
        // The steps setting local and remote view are very similar.
        // But note that if the local user do not have a uid or do
        // not care what the uid is, he can set his uid as ZERO.
        // Our server will assign one and return the uid via the event
        // handler callback function (onJoinChannelSuccess) after
        // joining the channel successfully.
        mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        mLocalView.setZOrderMediaOverlay(true);
        mLocalContainer.addView(mLocalView);
        // Initializes the local video view.
        // RENDER_MODE_HIDDEN: Uniformly scale the video until it fills the visible boundaries. One dimension of the video may have clipped contents.
        mRtcEngine.setupLocalVideo(new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void joinChannel() {
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name that
        // you use to generate this token.
        String token = getString(R.string.agora_access_token);
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null; // default, no token
        }
        mRtcEngine.joinChannel(token, this.channelid, "Extra Optional Data", 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mAudioPlayer != null)
            mAudioPlayer.stopRingtone();
        if (!mCallEnd) {
            leaveChannel();
        }
        if (br_ringin != null)
        unregisterReceiver(br_ringin);
        if(br_disconnect != null)
        unregisterReceiver(br_disconnect);
        if(br_accept != null)
        unregisterReceiver(br_accept);

        /*
          Destroys the RtcEngine instance and releases all resources used by the Agora SDK.

          This method is useful for apps that occasionally make voice or video calls,
          to free up resources for other operations when not making calls.
         */
        RtcEngine.destroy();
    }

    private void leaveChannel() {
        if(mRtcEngine != null)
        mRtcEngine.leaveChannel();
    }

    public void onLocalAudioMuteClicked(View view) {
        if(mRtcEngine == null )
            return;
        mMuted = !mMuted;
        // Stops/Resumes sending the local audio stream.
        mRtcEngine.muteLocalAudioStream(mMuted);
        int res = mMuted ? R.drawable.btn_mute : R.drawable.btn_unmute;
        mMuteBtn.setImageResource(res);
    }

    public void onSwitchCameraClicked(View view) {
        if(mRtcEngine == null )
            return;
        // Switches between front and rear cameras.
        mRtcEngine.switchCamera();
    }

    public void onCallClicked(View view) {
        Helper helper = new Helper(getApplication());

         helper = new Helper(getApplication());
        helper.sendNotifyVideoCall(GCM_NOTIFY_DISCONNECTING, requestFrom,accountId);

        if(!is_accepted) {

            helper.sendNotifyVideoCall(GCM_NOTIFY_MISSEDCALL, requestFrom, accountId);
            Toast.makeText(VideoChatViewActivity.this, "User not answering", Toast.LENGTH_SHORT).show();

        }
        if (mCallEnd) {
            startCall();
            mCallEnd = false;

            mCallBtn.setImageResource(R.drawable.btn_endcall);
            finish();
        } else {
            endCall();
            mCallEnd = true;
            mCallBtn.setImageResource(R.drawable.btn_startcall);
            finish();
        }

        showButtons(!mCallEnd);
    }

    private void startCall() {
        setupLocalVideo();
        joinChannel();
    }

    private void endCall() {
        removeLocalVideo();
        removeRemoteVideo();
        leaveChannel();
    }

    private void removeLocalVideo() {
        if (mLocalView != null) {
            mLocalContainer.removeView(mLocalView);
        }
        mLocalView = null;
    }

    private void showButtons(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        mMuteBtn.setVisibility(visibility);
        mSwitchCameraBtn.setVisibility(visibility);
    }
}
