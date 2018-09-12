package nz.ac.auckland.cursor;

import android.content.Context;
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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final float STANDARD_SCREEN_WIDTH = 720;
    private static final int STANDARD_CURSOR_HEIGHT = 60;
    private static final int STANDARD_CURSOR_WIDTH = 70;

    private int cursorHeight = STANDARD_CURSOR_HEIGHT;
    private int cursorWidth = STANDARD_CURSOR_WIDTH;
    private Drawable cursor;

    private float screenSizeFactor;
    private int dwellThreshold;
    private int[] xArr = new int[80];
    private int[] yArr = new int[80];

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
        cursor.setBounds(new Rect(xCentre, yCentre, xCentre + cursorWidth,
                yCentre + cursorHeight));

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

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenSizeFactor = displayMetrics.widthPixels / STANDARD_SCREEN_WIDTH;
        cursorHeight = (int) (screenSizeFactor * STANDARD_CURSOR_HEIGHT);
        cursorWidth = (int) (screenSizeFactor * STANDARD_CURSOR_WIDTH);
        dwellThreshold = (int) (80 * screenSizeFactor);

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
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUp = true;
            calibrate();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDown = true;

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
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUp = false;
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDown = false;
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

        Arrays.fill(xArr, xCentre);
        Arrays.fill(yArr, yCentre);

        pitchMultiplier = maxY * 2;
        rollMultiplier = maxX * 2;

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

        float dpitch = pitch - pitchOffset;
        float droll = roll - rollOffset;

        mapToPointer(dpitch, droll);
    }

    private float[] lowPass(float[] input, float[] previousOutput) {
        if (previousOutput == null) {
            return input;
        }

        for (int i = 0; i < input.length; i++) {
            previousOutput[i] = previousOutput[i] + 0.08f * (input[i] - previousOutput[i]);
        }

        return previousOutput;
    }

    private void mapToPointer(float dpitch, float droll) {
        int minPixelChange = (int) (5 * screenSizeFactor);
        int tempdx = (int) (Math.tan(droll) * rollMultiplier);
        int tempdy = (int) (Math.tan(dpitch) * pitchMultiplier);

        if (isDwell(xCentre + tempdx, yCentre - tempdy)) {
            System.out.println("Dwell: " + dwellThreshold);
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

        Rect bounds = cursor.copyBounds();
        bounds.left = x;
        bounds.top = y;
        bounds.right = x + cursorWidth;
        bounds.bottom = y + cursorHeight;
        cursor.setBounds(bounds);
        findViewById(android.R.id.content).invalidate();
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
}
