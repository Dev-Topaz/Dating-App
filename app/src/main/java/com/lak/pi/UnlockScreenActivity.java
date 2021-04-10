package com.lak.pi;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.toolbox.ImageLoader;
import com.lak.pi.app.App;
import com.lak.pi.common.ActivityBase;
import com.lak.pi.util.AudioPlayer;
import com.lak.pi.util.Helper;
import com.lak.pi.util.Listner;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static com.lak.pi.VideoChatViewActivity.BROADCAST_ACTION_DISCONNECTING;

public class UnlockScreenActivity extends ActivityBase {

    private static final String TAG = "unlock";
    private static final int WITHOUT_NOTIFY = 1;
    private static final int WITH_NOTIFY = 0;
    private AudioPlayer mAudioPlayer;
    private String data;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isOnCall;
    private String ACTION_SNOOZE = "snooze";
    private boolean disableNotification;
    private boolean disableAllNotification;
    private Object tag_json_obj = "unlockscreen";
    private final int interval = 1000; // 1 Second
    private Handler handler = new Handler();
    ImageLoader imageLoader = App.getInstance().getImageLoader();
    private Runnable runnable = new Runnable(){
        public void run() {

        }
    };
    private CountDownTimer countdown;
    private Helper helper;
    private String requestFrom;
    private String accountId;
    private BroadcastReceiver br_disconnect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window=getWindow();
      //  window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      //  getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        handler.postAtTime(runnable, System.currentTimeMillis()+interval);
        handler.postDelayed(runnable, interval);
        Log.d(TAG, "isOnCall ??: "+isCallActive(getApplicationContext()));
        // keep screen awake during call
        startTimer();
        isOnCall = isCallActive(getApplicationContext());

        helper = new Helper(getApplicationContext());
        setContentView(R.layout.activity_unlock_screen2);
        NotificationManager nMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(223);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        nMgr.deleteNotificationChannel("com.channel");
        nMgr.cancelAll();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "aman:wakeup");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //WritableMap params = Arguments.createMap(); // add here the data you want to send
        //params.putString("event", "ActivityonStop"); // <- example
        data = getIntent().getStringExtra("data");
        TextView iv_name = (TextView)findViewById(R.id.iv_name);
        ImageView iv_image = (ImageView)findViewById(R.id.iv_image);
        JSONObject object = null;
        try {
            object = new JSONObject(data);
             requestFrom = object.getString("requestFromId");
            accountId = object.getString("accountId");
            JSONObject object_profile = object.getJSONObject("extraData");
            String lowPhotoUrl = object_profile.getString("lowPhotoUrl");
            String fullName = object_profile.getString("fullname");
            iv_name.setText(fullName);
            imageLoader.get(lowPhotoUrl, ImageLoader.getImageListener(iv_image, R.drawable.profile_default_photo, R.drawable.profile_default_photo));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        br_disconnect = new BroadcastReceiver() {



            public void onReceive(Context context, Intent intent) {

                Toast.makeText(context, "User declined the call", Toast.LENGTH_SHORT).show();
                if(mAudioPlayer != null)
                mAudioPlayer.stopRingtone();
                finish();

            }
        };

        IntentFilter intFilt5 = new IntentFilter(BROADCAST_ACTION_DISCONNECTING);
        registerReceiver(br_disconnect, intFilt5);





        Button answer = (Button) findViewById(R.id.answerButton);
        answer.setOnClickListener(mClickListener);
        Button decline = (Button) findViewById(R.id.declineButton);
        decline.setOnClickListener(mClickListener);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                answer.setEnabled(true);
                decline.setEnabled(true);
            }
        },3000);
        mAudioPlayer = new AudioPlayer(this);
        setVolumeControlStream(AudioManager.STREAM_RING);
        mAudioPlayer.playRingtone();
        doSomethingMemoryIntensive();
        brodCastReciever();
    }
    public static String toDisplayCase(String s) {

        final String ACTIONABLE_DELIMITERS = " '-/"; // these cause the character following
        // to be capitalized

        StringBuilder sb = new StringBuilder();
        boolean capNext = true;

        for (char c : s.toCharArray()) {
            c = (capNext)
                    ? Character.toUpperCase(c)
                    : Character.toLowerCase(c);
            sb.append(c);
            capNext = (ACTIONABLE_DELIMITERS.indexOf((int) c) >= 0); // explicit cast not needed
        }
        return sb.toString();
    }
    private void startTimer() {
        countdown = new CountDownTimer(16000, 1000) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
               // helper.sendNotifyVideoCall(GCM_NOTIFY_DISCONNECTING, requestFrom, accountId);
                mAudioPlayer.stopRingtone();
              finish();
            }
        }.start();

    }
     private static String toTitleCase(String str) {
        
        if(str == null || str.isEmpty())
            return "";
        
        if(str.length() == 1)
            return str.toUpperCase();
        
        String[] parts = str.split(" ");
        
        StringBuilder sb = new StringBuilder( str.length() );
        
        for(String part : parts){
 
            char[] charArray = part.toLowerCase().toCharArray();
            charArray[0] = Character.toUpperCase( charArray[0] );
            
            sb.append( new String(charArray) ).append(" ");
        }
        
        return sb.toString().trim();
    }

    public boolean isCallActive(Context context){
        AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(manager.getMode()==AudioManager.MODE_IN_CALL){
            return true;
        }
        else{
            return false;
        }
    }
    @Override
    public void onResume() {

        // by doing this, the activity will be notified each time a new message arrives
//        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
//                new IntentFilter(AppConstants.PUSH_NOTIFICATION));

        super.onResume();
    }

    @Override
    public void onPause() {

       // LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        if(!disableAllNotification) {

        }
        super.onPause();
    }

    private void showCallingNotification(String title, String message) {
        String CHANNEL_ID = "sample_channel";
        String CHANNEL_NAME = "Notification";
        Calendar cal = Calendar.getInstance();
        Intent snoozeIntent = new Intent(this, UnlockScreenActivity.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        PendingIntent snoozePendingIntent =
                PendingIntent.getBroadcast(this, 0, snoozeIntent, 0);
        // I removed one of the semi-colons in the next line of code
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        inboxStyle.addLine(message);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // I would suggest that you use IMPORTANCE_DEFAULT instead of IMPORTANCE_HIGH
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            channel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/" + R.drawable.app_logo),
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build());
            //channel.canShowBadge();
            // Did you mean to set the property to enable Show Badge?
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setVibrate(new long[]{0, 100})
                .setPriority(Notification.PRIORITY_MAX)
                .setLights(Color.BLUE, 3000, 3000)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setWhen(cal.getTimeInMillis())
                .setContentIntent(snoozePendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(inboxStyle)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.app_logo))
                .setContentText(message);
        // Removed .build() since you use it below...no need to build it twice

        // Don't forget to set the ChannelID!!
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationBuilder.setChannelId(CHANNEL_ID);
        }

        notificationManager.notify(CHANNEL_ID, 1, notificationBuilder.build());
    }

    private void showSmallNotification(  String title, String message){
        String CHANNEL_ID = "sample_channel";
        String CHANNEL_NAME = "Notification";
        Calendar cal = Calendar.getInstance();
        // I removed one of the semi-colons in the next line of code
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        inboxStyle.addLine(message);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // I would suggest that you use IMPORTANCE_DEFAULT instead of IMPORTANCE_HIGH
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            channel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/" + R.drawable.app_logo),
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build());
            //channel.canShowBadge();
            // Did you mean to set the property to enable Show Badge?
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setVibrate(new long[]{0, 100})
                .setPriority(Notification.PRIORITY_MAX)
                .setLights(Color.BLUE, 3000, 3000)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setWhen(cal.getTimeInMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(inboxStyle)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.app_logo))
                .setContentText(message);
        // Removed .build() since you use it below...no need to build it twice

        // Don't forget to set the ChannelID!!
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationBuilder.setChannelId(CHANNEL_ID);
        }

        notificationManager.notify(CHANNEL_ID, 1, notificationBuilder.build());
    }
    private void brodCastReciever() {
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
    }




    private void wakeupScreen() {
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = Build.VERSION.SDK_INT >= 20 ? pm.isInteractive() : pm.isScreenOn(); // check if screen is on
        if (!isScreenOn) {

            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "aman:wakeup");
            wl.acquire(3000); //set your time in milliseconds
            wl.release();
        }
    }

    private void answerClicked() {
        // ask permission then only recieve
        wakeupScreen();
        countdown.cancel();
        disableAllNotification = true;
        isOnCall = isCallActive(getApplicationContext());
        if(isOnCall) {
            Toast.makeText(this, "Please disconnect current call before answering", Toast.LENGTH_LONG).show();
            return;
        }

        mAudioPlayer.stopRingtone();


        new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(UnlockScreenActivity.this, VideoChatViewActivity.class);
                            intent.putExtra("data",data);
                            intent.putExtra("from_activity","unlock");
                            startActivity(intent);
                            finish();
                        }
                    },500);





    }

    private void declineClicked(int notifyType) {


            helper.sendNotifyVideoCall(GCM_NOTIFY_DISCONNECTING, requestFrom, accountId);
            countdown.cancel();

        mAudioPlayer.stopRingtone();
        finish();
    }
    public class DownLoadImageTask extends AsyncTask<String,Void,Bitmap> {
        ImageView imageView;

        public DownLoadImageTask(ImageView imageView){
            this.imageView = imageView;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */
        protected Bitmap doInBackground(String...urls){
            String urlOfImage = urls[0];
            Bitmap logo = null;
            try{
                InputStream is = new URL(urlOfImage).openStream();
                /*
                    decodeStream(InputStream is)
                        Decode an input stream into a bitmap.
                 */
                logo = BitmapFactory.decodeStream(is);
            }catch(Exception e){ // Catch the download exception
                e.printStackTrace();
            }
            return logo;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        protected void onPostExecute(Bitmap result){
            imageView.setImageBitmap(result);
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.answerButton:

                    Helper.AskForPermission(UnlockScreenActivity.this, new Listner.OnPermissionGranted() {
                        @Override
                        public void onPermissionGranted(int isGranted) {
                            if(isGranted == Helper.ALL_PERMISSION_GIVEN){
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        answerClicked();
                                        helper.sendNotifyVideoCall(GCM_NOTIFY_ACCEPT_CALL, requestFrom, accountId);
                                    }
                                },800);

                            }else if(isGranted == Helper.SOME_PERMISSION_TEMPORARY_NOT_GIVEN){
                                Toast.makeText(UnlockScreenActivity.this, "You have to give all permission in order to procced", Toast.LENGTH_LONG).show();
                            }else if(isGranted == Helper.SOME_PERMISSION_PERMANENTLY_NOT_GIVEN){
                                Toast.makeText(UnlockScreenActivity.this, "You have to give all permission from settings in order to procced", Toast.LENGTH_LONG).show();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        declineClicked(WITH_NOTIFY);
                                    }
                                },2000);

                            }
                        }
                    });


                    break;
                case R.id.declineButton:
                    disableNotification = true;
                    declineClicked(WITH_NOTIFY);
                    break;
            }
        }
    };


    public void doSomethingMemoryIntensive() {

        // Before doing something that requires a lot of memory,
        // check to see whether the device is in a low memory state.
        ActivityManager.MemoryInfo memoryInfo = getAvailableMemory();
        if (!memoryInfo.lowMemory) {
            // Do memory intensive work ...
        //    Log.d(TAG, "doSomethingMemoryIntensive: ");
        }else {
         //   Log.d(TAG, "doSomethingMemoryIntensive: ");
        }
    }

    // Get a MemoryInfo object for the device's current memory status.
    private ActivityManager.MemoryInfo getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(br_disconnect);
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {

    }
}