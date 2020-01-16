package com.zpf.medianotificationdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    boolean openNotification;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            if (iBinder instanceof MediaNotificationService.PlayerBinder) {
                MediaNotificationService.PlayerBinder binder = (MediaNotificationService.PlayerBinder) iBinder;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = findViewById(R.id.notification_operation_btn);
        button.setText(R.string.open_notification);
        MediaNotificationService.bindService(MainActivity.this, mServiceConnection);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!openNotification) {
                    openNotification = true;
                    button.setText(R.string.close_notification);

                    Intent intent = new Intent(MainActivity.this, MediaNotificationService.class);
                    intent.putExtra(MediaNotificationService.OPEN_NOTIFICATION_KEY, true);
                    MainActivity.this.startService(intent);
                } else {
                    MediaNotificationService.stop(MainActivity.this);
                    openNotification = false;
                    button.setText(R.string.open_notification);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
            stopService(new Intent(this, MediaNotificationService.class));
        }
    }
}
