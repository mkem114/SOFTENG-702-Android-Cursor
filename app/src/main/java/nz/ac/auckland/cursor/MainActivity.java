package nz.ac.auckland.cursor;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    enum VolumeBtnState {
        REST,
        VOL_DOWN_LONG,
        VOL_DOWN,
        VOL_UP_LONG,
        VOL_UP,
        BOTH,
    }

    private static final float STANDARD_SCREEN_WIDTH = 720;
    private static final int STANDARD_CURSOR_HEIGHT = 60;
    private static final int STANDARD_CURSOR_WIDTH = 70;

    private int cursorHeight = STANDARD_CURSOR_HEIGHT;
    private int cursorWidth = STANDARD_CURSOR_WIDTH;
    private List<Cursor> cursors = new ArrayList<>();

    private static final int DWELL_WINDOW = 80;
    private static final int DWELL_THRESHOLD_MULTIPLIER = 80;
    private static final float MOVEMENT_THRESHOLD_MULTIPLIER = 5;

    private float screenSizeFactor;
    private int dwellThreshold;
    private int[] xArr = new int[DWELL_WINDOW];
    private int[] yArr = new int[DWELL_WINDOW];

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
    // sensitivity too
    private float changeSensitivity = 0.08f;

    private float[] rotationMatrix = new float[9];
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] orientationValues = new float[3];

    private VolumeBtnState volumeBtnState;
    private boolean disableUpHandler = false;

    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialiseCursors();
        initialiseCoordinates();

        final TextView textView = (TextView) findViewById(R.id.value_format);

        findViewById(android.R.id.content).setOnTouchListener((view, event) -> {
            textView.setText(String.format(
                    Locale.ENGLISH,
                    "%s detected at %f, %f",
                    MotionEvent.actionToString(event.getAction()),
                    event.getX(), event.getY()
            ));
            view.performClick();
            return true;
        });
    }

    private void initialiseCursors() {
        Drawable drawable1 = Objects.requireNonNull(ContextCompat.
                getDrawable(getBaseContext(), R.drawable.cursor));
        Drawable drawable2 = Objects.requireNonNull(ContextCompat.
                getDrawable(getBaseContext(), R.drawable.cursor2));

        Cursor c1 = new Cursor(drawable1, getRealWidth(STANDARD_CURSOR_WIDTH), getRealHeight(STANDARD_CURSOR_HEIGHT), 0, 0);
        Cursor c2 = new Cursor(drawable2, getRealWidth(60), getRealHeight(60), getRealWidth(30), getRealHeight(30));

        cursors.add(c1);
        cursors.add(c2);
        cursors.get(0).updateLocation(xCentre, yCentre);
        findViewById(android.R.id.content).getOverlay().add(cursors.get(0).getDrawable());

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

    @Override
    protected void onStart() {
        super.onStart();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenSizeFactor = displayMetrics.widthPixels / STANDARD_SCREEN_WIDTH;
        dwellThreshold = (int) (screenSizeFactor * DWELL_THRESHOLD_MULTIPLIER);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
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

    public void toggleNextCursor() {
        Collections.rotate(cursors, 1);
        findViewById(android.R.id.content).getOverlay().clear();
        cursors.get(0).updateLocation(x, y);
        findViewById(android.R.id.content).getOverlay().add(cursors.get(0).getDrawable());
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (volumeBtnState != VolumeBtnState.BOTH && keyCode == KeyEvent.KEYCODE_VOLUME_UP && keyEvent.isLongPress()) {
            volumeBtnState = VolumeBtnState.VOL_UP_LONG;
            changeSensitivity = 0.01f;
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
            toggleNextCursor();
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && volumeBtnState == VolumeBtnState.VOL_UP_LONG) {
            volumeBtnState = VolumeBtnState.REST;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && volumeBtnState == VolumeBtnState.VOL_DOWN) {
            volumeBtnState = VolumeBtnState.REST;
            simulateTouchUp();
        }
        changeSensitivity = 0.08f;
        dwellThreshold = (int) (screenSizeFactor * DWELL_THRESHOLD_MULTIPLIER);
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //unused. Do nothing
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
        // TODO transform for display rotation https://google-developer-training.gitbooks.io/android-developer-advanced-course-concepts/unit-1-expand-the-user-experience/lesson-3-sensors/3-2-c-motion-and-position-sensors/3-2-c-motion-and-position-sensors.html

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
        if (volumeBtnState == VolumeBtnState.VOL_DOWN) simulateTouchMove();
        cursors.get(0).updateLocation(x, y);
    }

    private boolean isDwell(int newX, int newY) {
        for (int i = xArr.length - 1; i > 0; i--) {
            xArr[i] = xArr[i - 1];
            yArr[i] = yArr[i - 1];
        }

        xArr[0] = newX;
        yArr[0] = newY;

        IntSummaryStatistics statX = Arrays.stream(xArr).summaryStatistics();
        int diffX = statX.getMax() - statX.getMin();

        IntSummaryStatistics statY = Arrays.stream(yArr).summaryStatistics();
        int diffY = statY.getMax() - statY.getMin();

        if (diffX < dwellThreshold && diffY < dwellThreshold) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Scooooooooreeeeee!!!
     */
    public void simulateTouchDown() {
        long upTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.
                obtain(upTime, eventTime, MotionEvent.ACTION_DOWN, (float)x, (float)y, 0);
        findViewById(android.R.id.content).dispatchTouchEvent(downEvent);
        downEvent.recycle();
    }

    /**
     * All natural
     */
    public void simulateTouchUp() {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent upEvent = MotionEvent
                .obtain(downTime, eventTime, MotionEvent.ACTION_UP, (float)x, (float)y, 0);
        findViewById(android.R.id.content).dispatchTouchEvent(upEvent);
        upEvent.recycle();
    }

    /**
     * Isn't that some sort of chess rule?
     */
    public void simulateTouchMove() {
        long moveTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent moveEvent = MotionEvent
                .obtain(moveTime, eventTime, MotionEvent.ACTION_MOVE, (float)x, (float)y, 0);
        findViewById(android.R.id.content).dispatchTouchEvent(moveEvent);
        moveEvent.recycle();
    }
}
