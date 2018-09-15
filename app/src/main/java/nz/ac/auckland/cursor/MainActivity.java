package nz.ac.auckland.cursor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String TAG = "βTapModelTest";
    TextView text;
    RelativeLayout layout;
    int vumeterInc = 3000;
    int vumeterDec = 5;
    int vumeterLevel = 0;
    private ImageView vumeter;

    public Handler messageHandler = new MessageHandler();

    protected ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.i(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected");
        }
    };

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ComponentName componentName = new ComponentName("com.prhlt.aemus.BoDTapService",
                "com.prhlt.aemus.BoDTapService.BoDTapService");
        Intent intent = new Intent();
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        intent.setComponent(componentName);

        getApplication().bindService(intent,mServerConn, Context.BIND_AUTO_CREATE);
        ComponentName c = getApplication().startService(intent);

        if (c == null) {
            Toast.makeText(getApplicationContext(), "Failed to start the βTap Service", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to start the βTap Service with " + intent);
            new Thread(){
                @Override
                public void run() {
                    try {
                        Thread.sleep(4000);
                        finish();
                        System.exit(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }else{
            Toast.makeText(getApplicationContext(), "βTap Service started", Toast.LENGTH_LONG).show();
            Log.i(TAG, "βTap Service started with " + intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ComponentName componentName = new ComponentName("com.prhlt.aemus.BoDTapService",
                "com.prhlt.aemus.BoDTapService.BoDTapService");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        getApplication().stopService(intent);
        getApplication().unbindService(mServerConn);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            JSONObject info = null;

            try {
                info = new JSONObject(message.getData().getString("data"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            int tap = 0;
            try {
                tap = info.getInt("tap");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            switch (tap) {
                case 0:
               //     Log.d(TAG, "No βTAP!");
                     break;
                case 1:
                    Log.d(TAG, "βTAP_SINGLE!");
                    Toast.makeText(getApplicationContext(), "single tap here!", Toast.LENGTH_LONG).show();


                    break;
                case 2:
                    Log.d(TAG, "βTAP_DOUBLE!");
                    break;
                default:
                    Log.e(TAG, "βTAP Type not recognised!");
                    break;
            }

       }
    }
}
