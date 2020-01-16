package com.zpf.medianotificationdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

public class MediaNotificationService extends Service {

    private static final int NOTIFICATION_ID = 4396;
    private static final String CHANNEL_ID = "com.zpf.medianotificationdemo.channel_id";
    private static final CharSequence CHANNEL_NAME = "my_channel";

    private Context mContext;
    private NotificationCompat.Builder mBuilder;
    private NotificationChannel mNotificationChannel;
    private NotificationManager mNotificationManager;
    private RemoteViews mRemoteViews;
    private Notification mNotification;
    public static final String OPERATION_KEY = "operation";

    private static final int LAST_AUDIO = 10000;
    private static final int NEXT_AUDIO = 10001;
    public static final int PLAY_AUDIO = 10002;
    private static final int PAUSE_AUDIO = 10003;
    private static final int CANCEL_NOTIFY = 10005;

    public static final String OPEN_NOTIFICATION_KEY = "open_audio_play_notification";
    public static final String CLOSE_NOTIFICATION_KEY = "close_audio_play_notification";

    public static final String PAUSE_AUDIO_HIDE_ALL_KEY = "pause_everything";


    public static void bindService(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, MediaNotificationService.class);
        context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    /**
     * 暂停播放
     *
     * @param context
     */
    public static void pause(Context context) {
        Intent pauseAudioIntent = new Intent(context, MediaNotificationService.class);
        pauseAudioIntent.putExtra(OPERATION_KEY, PAUSE_AUDIO);
        context.startService(pauseAudioIntent);
    }

    /**
     * 取消后台播放，隐藏通知栏
     *
     * @param context
     */
    public static void stop(Context context) {
        Intent pauseAudioIntent = new Intent(context, MediaNotificationService.class);
        pauseAudioIntent.putExtra(OPERATION_KEY, CANCEL_NOTIFY);
        context.startService(pauseAudioIntent);
    }

    public MediaNotificationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        if (mRemoteViews != null) {
            mRemoteViews.removeAllViews(R.layout.notification_video_play);
            mRemoteViews = null;
        }
        /**
         * 不支持某些布局（说的就是你约束布局）
         */
        mRemoteViews = new RemoteViews(mContext.getPackageName(), R.layout.notification_video_play);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return new PlayerBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }

        int operation = intent.getIntExtra(OPERATION_KEY, -1);

        boolean openNotification = intent.getBooleanExtra(OPEN_NOTIFICATION_KEY, false);
        boolean closeNotification = intent.getBooleanExtra(CLOSE_NOTIFICATION_KEY, false);

        boolean pauseAudioHideAll = intent.getBooleanExtra(PAUSE_AUDIO_HIDE_ALL_KEY, false);

        if (pauseAudioHideAll) {
            pause();
            hideNotification();
        }

        if (openNotification) {
            mRemoteViews.setViewVisibility(R.id.resume, View.INVISIBLE);
            mRemoteViews.setViewVisibility(R.id.pause, View.VISIBLE);
            createNotification();
        }

        if (closeNotification && mNotification != null) {
            hideNotification();
        }

        switch (operation) {
            case CANCEL_NOTIFY://取消通知的时候暂停
                pause();
                hideNotification();
                break;
            case LAST_AUDIO:
                last();
                mRemoteViews.setViewVisibility(R.id.resume, View.INVISIBLE);
                mRemoteViews.setViewVisibility(R.id.pause, View.VISIBLE);
                createNotification();
                break;
            case NEXT_AUDIO:
                next();
                mRemoteViews.setViewVisibility(R.id.resume, View.INVISIBLE);
                mRemoteViews.setViewVisibility(R.id.pause, View.VISIBLE);
                createNotification();
                break;
            case PLAY_AUDIO:
                play();
                mRemoteViews.setViewVisibility(R.id.resume, View.INVISIBLE);
                mRemoteViews.setViewVisibility(R.id.pause, View.VISIBLE);
                createNotification();
                break;
            case PAUSE_AUDIO:
                pause();
                mRemoteViews.setViewVisibility(R.id.pause, View.INVISIBLE);
                mRemoteViews.setViewVisibility(R.id.resume, View.VISIBLE);
                createNotification();
                break;
            default:
                break;
        }

        return START_STICKY;
    }

    private void createNotification() {
        if (mNotificationChannel == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                mNotificationManager.createNotificationChannel(mNotificationChannel);
            }
        }

        if (mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setCustomBigContentView(mRemoteViews)
                    .setOnlyAlertOnce(false)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    // 铃声、闪光、震动均系统默认
                    .setDefaults(Notification.DEFAULT_ALL)
                    // 设置为public后，通知栏将在锁屏界面显示
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    // 从Android4.1开始，可以通过以下方法，设置通知栏的优先级，优先级越高的通知排的越靠前，
                    // 优先级低的，不会在手机最顶部的状态栏显示图标
                    // 设置优先级为PRIORITY_MAX，将会在手机顶部显示通知栏
                    .setPriority(NotificationCompat.PRIORITY_MAX);
        }


        Intent jumpIntent = new Intent("com.zpf.medianotificationdemo.MainActivity");
        PendingIntent jumpPendingIntent = PendingIntent.getActivity(this, 0, jumpIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mRemoteViews.setOnClickPendingIntent(R.id.video_image, jumpPendingIntent);
        mRemoteViews.setOnClickPendingIntent(R.id.title, jumpPendingIntent);

        Intent lastNotifyIntent = new Intent(this, MediaNotificationService.class);
        lastNotifyIntent.putExtra(OPERATION_KEY, LAST_AUDIO);
        PendingIntent lastAudioPendingIntent = PendingIntent.getService(mContext, 1, lastNotifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.last_audio, lastAudioPendingIntent);

        Intent nextNotifyIntent = new Intent(this, MediaNotificationService.class);
        nextNotifyIntent.putExtra(OPERATION_KEY, NEXT_AUDIO);
        PendingIntent nextAudioPendingIntent = PendingIntent.getService(mContext, 2, nextNotifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.next_audio, nextAudioPendingIntent);

        Intent playAudioIntent = new Intent(this, MediaNotificationService.class);
        playAudioIntent.putExtra(OPERATION_KEY, PLAY_AUDIO);
        PendingIntent playAudioPendingIntent = PendingIntent.getService(mContext, 3, playAudioIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.resume, playAudioPendingIntent);

        Intent pauseAudioIntent = new Intent(this, MediaNotificationService.class);
        pauseAudioIntent.putExtra(OPERATION_KEY, PAUSE_AUDIO);
        PendingIntent pauseAudioPendingIntent = PendingIntent.getService(mContext, 4, pauseAudioIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.pause, pauseAudioPendingIntent);

        Intent cancelNotifyIntent = new Intent(this, MediaNotificationService.class);
        cancelNotifyIntent.putExtra(OPERATION_KEY, CANCEL_NOTIFY);
        PendingIntent cancelNotifyPendingIntent = PendingIntent.getService(mContext, 5, cancelNotifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notification_close, cancelNotifyPendingIntent);


        mNotification = mBuilder.build();

        mRemoteViews.setTextViewText(R.id.title, getResources().getText(R.string.notification_title));
        mRemoteViews.setTextViewText(R.id.sub_title, getResources().getText(R.string.notification_subtitle));

//        /**
//         * 如果使用Glide图片加载可使用如下
//         */
//        if (!TextUtils.isEmpty(imageUrl)) {
//            NotificationTarget notificationTarget = new NotificationTarget(mContext, R.id.video_image, mRemoteViews, mNotification, NOTIFICATION_ID);
//            Glide.with(mContext).asBitmap().load(imageUrl).into(notificationTarget);
//        }else {
//            startForeground(100, mNotification);
//        }

        startForeground(100, mNotification);

    }

    /**
     * 隐藏通知栏
     */
    public void hideNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mNotificationManager != null) {
                mNotificationManager.deleteNotificationChannel(CHANNEL_ID);
            }
            mNotificationChannel = null;
        } else {
            stopForeground(true);
        }
    }


    public void pause() {

    }

    public void play() {

    }

    public void last() {

    }

    public void next() {

    }

    //该方法包含关于歌曲的操作
    public class PlayerBinder extends Binder {
        /**
         * 播放
         */
        public void play() {
            MediaNotificationService.this.play();
        }

        /**
         * 暂停播放
         */
        public void pause() {
            MediaNotificationService.this.pause();
        }

        /**
         * 播放下一个资源
         */
        public void next() {
            MediaNotificationService.this.next();
        }

        /**
         * 上一个资源
         */
        public void last() {
            MediaNotificationService.this.last();
        }
    }
}
