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

/*import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private Drawable cursor;
    private static final int CURSOR_WIDTH = 140;
    private static final int CURSOR_HEIGHT = 120;

    //pixel coordinates
    private float pitch;
    private float roll;
    private int x;
    private int y;
    private int dy = 0;
    private int dx = 0;

    //depends on display size
    private int maxX;
    private int maxY;
    private int xCentre;
    private int yCentre;

    //depends on calibrated pitch and roll
    private float pitchOffset;
    private float rollOffset;

    //multiplier (z height) for mapping pitch/roll to up/right
    private double pitchMultiplier;
    private double rollMultiplier;

    private float[] rotationMatrix = new float[9];
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] orientationValues = new float[3];

    private boolean volumeDown = false;
    private boolean volumeUp = false;

    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock the orientation to portrait (for now)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        cursor = Objects.requireNonNull(ContextCompat.
                getDrawable(getBaseContext(), R.drawable.cursor));
        cursor.setBounds(new Rect(xCentre, yCentre, xCentre + CURSOR_WIDTH,
                yCentre + CURSOR_HEIGHT));

        findViewById(android.R.id.content).getOverlay().add(cursor);

        findViewById(android.R.id.content).setOnTouchListener((view, event) -> {
            Snackbar.make(view, "Click detected at " + event.getX() + "," + event.getY(),
                    Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            view.performClick();
            return true;
        });

        initialise();
    }

    @Override
    protected void onStart() {
        super.onStart();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) volumeUp = true;
            else volumeDown = true;
            if (volumeDown && volumeUp) {
                calibrate();
            }
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            MotionEvent downEvent = MotionEvent.
                    obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
            MotionEvent upEvent = MotionEvent
                    .obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
            findViewById(android.R.id.content).dispatchTouchEvent(downEvent);
            findViewById(android.R.id.content).dispatchTouchEvent(upEvent);
            downEvent.recycle();
            upEvent.recycle();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) volumeUp = false;
            else volumeDown = false;
            return true;
        }
        return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //unused. Do nothing
    }

    private void initialise() {
        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        maxX = displaySize.x;
        maxY = displaySize.y;
        xCentre = maxX / 2;
        yCentre = maxY / 2;

        x = xCentre;
        y = yCentre;

        pitchMultiplier = maxY;
        rollMultiplier = maxX;

        pitchOffset = 0;
        rollOffset = 0;
    }

    private void calibrate() {
        pitchOffset = pitch;
        rollOffset = roll;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = lowPass(event.values.clone(), gravity);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = lowPass(event.values.clone(), geomagnetic);
                break;
            default:
                return;
        }

        SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
        SensorManager.getOrientation(rotationMatrix, orientationValues);
        pitch = orientationValues[1];
        roll = orientationValues[2];

        pitch = Math.round(pitch * 100) / (100f);
        roll = Math.round(roll * 100) / (100f);
        float dpitch = pitch - pitchOffset;
        float droll = roll - rollOffset;

        mapToPointer(dpitch, droll);
    }

    private float[] lowPass(float[] input, float[] previousOutput) {
        if (previousOutput == null) {
            return input;
        }

        for (int i = 0; i < input.length; i++) {
            previousOutput[i] = previousOutput[i] + 0.1f * (input[i] - previousOutput[i]);
        }

        return previousOutput;
    }

    private void mapToPointer(float dpitch, float droll) {
        int tempdy = (int) (Math.tan(dpitch) * pitchMultiplier);
        if (Math.abs(dy - tempdy) > 15) {
            dy = tempdy;
            y = yCentre - dy;
            y = y > maxY ? maxY : y;
            y = y < 0 ? 0 : y;
        }

        int tempdx = (int) (Math.tan(droll) * rollMultiplier);
        if (Math.abs(dx - tempdx) > 15) {
            dx = tempdx;
            x = xCentre + dx;
            x = x > maxX ? maxX : x;
            x = x < 0 ? 0 : x;
        }

        Rect bounds = cursor.copyBounds();
        bounds.left = x;
        bounds.top = y;
        bounds.right = x + CURSOR_WIDTH;
        bounds.bottom = y + CURSOR_HEIGHT;
        cursor.setBounds(bounds);
        findViewById(android.R.id.content).invalidate();
    }
}*/
