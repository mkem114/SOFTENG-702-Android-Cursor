package nz.ac.auckland.cursor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class CursorOverlay extends AppCompatActivity implements SensorEventListener {
    private static final float STANDARD_SCREEN_WIDTH = 720;
    private static final int STANDARD_CURSOR_HEIGHT = 60;
    private static final int STANDARD_CURSOR_WIDTH = 70;

    private static final int DWELL_WINDOW = 80;
    private static final int DWELL_THRESHOLD_MULTIPLIER = 80;
    private static final float MOVEMENT_THRESHOLD_MULTIPLIER = 5;

    //depends on display size
    private int maxX;
    private int maxY;
    private int xCentre;
    private int yCentre;
    private float screenSizeFactor;

    //depends on calibrated pitch and roll
    private float pitchOffset;
    private float rollOffset;

    //physical buttons and tapping
    private VolumeBtnState volumeBtnState;
    private boolean disableUpHandler = false;
    private boolean volUpTogglesSensitivity;
    public Handler messageHandler = new MessageHandler();
    protected ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    //cursor
    private List<Cursor> cursors = new ArrayList<>();
    private CursorSensitivity currentCursorSensitivity;
    private float changeSensitivity;
    private int[] xArr = new int[DWELL_WINDOW];
    private int[] yArr = new int[DWELL_WINDOW];
    private int dwellThreshold;

    //sensors
    private SensorManager sensorManager;
    private float[] rotationMatrix = new float[9];
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] orientationValues = new float[3];

    private float pitch;
    private float roll;
    private double pitchMultiplier;
    private double rollMultiplier;
    private int x;
    private int y;
    private int dy = 0;
    private int dx = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initialiseCursors();
        initialiseCoordinates();
    }

    @Override
    protected void onStart() {
        super.onStart();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenSizeFactor = displayMetrics.widthPixels / STANDARD_SCREEN_WIDTH;
        setCursorSensitivity(CursorSensitivity.NO_DWELL);
        setVolUpTogglesSensitivity(false);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);

        initialiseBackTapService();
    }


    private void initialiseCursors() {
        Drawable drawableArrow = Objects.requireNonNull(ContextCompat.
                getDrawable(getBaseContext(), R.drawable.cursor));
        Drawable drawableCrosshair = Objects.requireNonNull(ContextCompat.
                getDrawable(getBaseContext(), R.drawable.cursor2));

        Cursor arrow = new Cursor(drawableArrow, getRealWidth(STANDARD_CURSOR_WIDTH), getRealHeight(STANDARD_CURSOR_HEIGHT), 0, 0);
        Cursor smallArrow = new Cursor(drawableArrow, getRealWidth(STANDARD_CURSOR_WIDTH / 2), getRealHeight(STANDARD_CURSOR_WIDTH / 2), 0, 0);
        Cursor crosshair = new Cursor(drawableCrosshair, getRealWidth(60), getRealHeight(60), getRealWidth(30), getRealHeight(30));
        Cursor smallCrosshair = new Cursor(drawableCrosshair, getRealWidth(30), getRealHeight(30), getRealWidth(15), getRealHeight(15));

        cursors.add(arrow);
        cursors.add(smallArrow);
        cursors.add(crosshair);
        cursors.add(smallCrosshair);

        cursors.get(0).updateLocation(xCentre, yCentre);
        findViewById(android.R.id.content).getOverlay().add(cursors.get(0).getDrawable());
    }

    private int getRealHeight(int height) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenSizeFactor = displayMetrics.widthPixels / STANDARD_SCREEN_WIDTH;
        return (int) (screenSizeFactor * height);
    }

    private int getRealWidth(int width) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenSizeFactor = displayMetrics.widthPixels / STANDARD_SCREEN_WIDTH;
        return (int) (screenSizeFactor * width);
    }


    private void initialiseCoordinates() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        maxX = displaySize.x;
        maxY = displaySize.y;
        xCentre = maxX / 2;
        yCentre = maxY / 2;

        Arrays.fill(xArr, xCentre);
        Arrays.fill(yArr, yCentre);
        x = xCentre;
        y = yCentre;

        pitchMultiplier = maxY * 2;
        rollMultiplier = maxX * 2;

        pitchOffset = 0;
        rollOffset = 0;
    }


    private void initialiseBackTapService() {
        ComponentName componentName = new ComponentName("com.prhlt.aemus.BoDTapService",
                "com.prhlt.aemus.BoDTapService.BoDTapService");

        Intent intent = new Intent();
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        intent.setComponent(componentName);

        getApplication().bindService(intent, mServerConn, Context.BIND_AUTO_CREATE);
        getApplication().startService(intent);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
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
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (volumeBtnState != VolumeBtnState.BOTH && keyCode == KeyEvent.KEYCODE_VOLUME_UP && keyEvent.isLongPress()) {
            volumeBtnState = VolumeBtnState.VOL_UP_LONG;
            changeSensitivity = 0.005f;
            dwellThreshold = 0;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && volumeBtnState == VolumeBtnState.VOL_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && volumeBtnState == VolumeBtnState.VOL_UP) {
            volumeBtnState = VolumeBtnState.BOTH;
            calibrate();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && volumeBtnState != VolumeBtnState.VOL_UP
                && volumeBtnState != VolumeBtnState.VOL_UP_LONG) {
            volumeBtnState = VolumeBtnState.VOL_UP;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && volumeBtnState != VolumeBtnState.VOL_DOWN
                && volumeBtnState != VolumeBtnState.VOL_DOWN_LONG) {
            volumeBtnState = VolumeBtnState.VOL_DOWN;
            simulateTouchDown();
            return true;
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
        if (disableUpHandler) {
            disableUpHandler = false;
            volumeBtnState = VolumeBtnState.REST;
        } else if (volumeBtnState == VolumeBtnState.BOTH) {
            disableUpHandler = true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && volumeBtnState == VolumeBtnState.VOL_UP) {
            volumeBtnState = VolumeBtnState.REST;
            if (volUpTogglesSensitivity) {
                toggleNextCursorSensitivity();
            } else {
                toggleNextCursor();
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && volumeBtnState == VolumeBtnState.VOL_UP_LONG) {
            volumeBtnState = VolumeBtnState.REST;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && volumeBtnState == VolumeBtnState.VOL_DOWN) {
            volumeBtnState = VolumeBtnState.REST;
            simulateTouchUp();
        }
        setCursorSensitivity(currentCursorSensitivity);
        return true;
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

        float dpitch = pitch - pitchOffset;
        float droll = roll - rollOffset;

        mapToPointer(dpitch, droll);
    }

    private float[] lowPass(float[] input, float[] previousOutput) {
        if (previousOutput == null) {
            return input;
        }

        for (int i = 0; i < input.length; i++) {
            previousOutput[i] = previousOutput[i] + changeSensitivity * (input[i] - previousOutput[i]);
        }

        return previousOutput;
    }

    private void mapToPointer(float dpitch, float droll) {
        int minPixelChange = (int) (MOVEMENT_THRESHOLD_MULTIPLIER * screenSizeFactor);
        int tempdx = (int) (Math.tan(droll) * rollMultiplier);
        int tempdy = (int) (Math.tan(dpitch) * pitchMultiplier);

        if (isDwell(xCentre + tempdx, yCentre - tempdy)) {
            return;
        }

        if (Math.abs(dy - tempdy) > minPixelChange) {
            dy = tempdy;
            y = yArr[0];
            y = y > maxY ? maxY : y;
            y = y < 0 ? 0 : y;
        }
        if (Math.abs(dx - tempdx) > minPixelChange) {
            dx = tempdx;
            x = xArr[0];
            x = x > maxX ? maxX : x;
            x = x < 0 ? 0 : x;
        }

        if (volumeBtnState == VolumeBtnState.VOL_DOWN) {
            simulateTouchMove();
        }
        cursors.get(0).updateLocation(x, y);
    }

    private boolean isDwell(int newX, int newY) {
        for (int i = xArr.length - 1; i > 0; i--) {
            xArr[i] = xArr[i - 1];
            yArr[i] = yArr[i - 1];
        }

        xArr[0] = newX;
        yArr[0] = newY;

        int diffX = getMaxInArray(xArr) - getMinInArray(xArr);
        int diffY = getMinInArray(yArr) - getMaxInArray(yArr);

        if (diffX < dwellThreshold && diffY < dwellThreshold) {
            return true;
        } else {
            return false;
        }
    }

    private int getMaxInArray(int[] arr) {
        if (arr.length == 0) {
            return 0;
        }

        int max = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }
        return max;
    }

    private int getMinInArray(int[] arr) {
        if (arr.length == 0) {
            return 0;
        }

        int min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }
        return min;
    }

    /**
     * Set sensitivity
     * toggleNextCursorSensitivity() cycles between these levels
     *
     * @param sensitivity DWELL: if we think you intend on dwelling on the same location, we will keep the cursor
     *                    stationary from jitter
     *                    SMOOTHEST: lowest sensitivity to change in angles, but may appear laggy
     *                    NO_DWELL: same sensitivity as dwell, but won't keep cursor stationary,
     *                    for fine-grained control
     */
    public void setCursorSensitivity(CursorSensitivity sensitivity) {
        currentCursorSensitivity = sensitivity;

        switch (sensitivity) {
            case DWELL:
                dwellThreshold = (int) (screenSizeFactor * DWELL_THRESHOLD_MULTIPLIER);
                changeSensitivity = 0.08f;
                break;
            case SMOOTHEST:
                dwellThreshold = 10;
                changeSensitivity = 0.015f;
                break;
            case NO_DWELL:
                dwellThreshold = 0;
                changeSensitivity = 0.08f;
                break;
            default:
                break;
        }
    }

    /**
     * Set what vol up button does
     * Currently either toggles cursor sensitivity or switches cursor
     *
     * @param togglesSensitivity
     */
    public void setVolUpTogglesSensitivity(boolean togglesSensitivity) {
        volUpTogglesSensitivity = togglesSensitivity;
    }

    /**
     * Cycles between different cursor preset sensitivity levels
     */
    public void toggleNextCursorSensitivity() {
        int nextOrdinal = (currentCursorSensitivity.ordinal() + 1) % CursorSensitivity.values().length;
        currentCursorSensitivity = CursorSensitivity.values()[nextOrdinal];
    }

    /**
     * Cycles between different cursor appearances
     */
    public void toggleNextCursor() {
        Collections.rotate(cursors, -1);
        findViewById(android.R.id.content).getOverlay().clear();
        cursors.get(0).updateLocation(x, y);
        findViewById(android.R.id.content).getOverlay().add(cursors.get(0).getDrawable());
    }

    /**
     * Simulates touch down at current cursor location
     */
    public void simulateTouchDown() {
        long upTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.
                obtain(upTime, eventTime, MotionEvent.ACTION_DOWN, (float) x, (float) y, 0);
        findViewById(android.R.id.content).dispatchTouchEvent(downEvent);
        downEvent.recycle();
    }

    /**
     * Simulates touch up at current cursor location
     */
    public void simulateTouchUp() {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent upEvent = MotionEvent
                .obtain(downTime, eventTime, MotionEvent.ACTION_UP, (float) x, (float) y, 0);
        findViewById(android.R.id.content).dispatchTouchEvent(upEvent);
        upEvent.recycle();
    }

    /**
     * Simulates a drag event holding down the cursor
     */
    public void simulateTouchMove() {
        long moveTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent moveEvent = MotionEvent
                .obtain(moveTime, eventTime, MotionEvent.ACTION_MOVE, (float) x, (float) y, 0);
        findViewById(android.R.id.content).dispatchTouchEvent(moveEvent);
        moveEvent.recycle();
    }


    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            JSONObject info = null;

            try {
                info = new JSONObject(message.getData().getString("data"));
                int tap;
                tap = info.getInt("tap");

                if (tap == 1 || tap == 2) {
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
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
