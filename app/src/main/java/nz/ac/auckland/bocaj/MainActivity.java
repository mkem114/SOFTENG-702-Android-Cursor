package nz.ac.auckland.bocaj;

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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private TextView textView;
    private String centre;
    private String radPitchRoll;
    private String degPitchRoll = "";
    private String coordinates;
    private String textToDisplay;

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


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final double x = fab.getX();
        final double y = fab.getY();

        cursor = Objects.requireNonNull(ContextCompat.getDrawable(getBaseContext(), R.drawable.cursor));
        cursor.setBounds(new Rect(xCentre, yCentre, xCentre + CURSOR_WIDTH, yCentre + CURSOR_HEIGHT));

        findViewById(android.R.id.content).getOverlay().add(cursor);

        findViewById(android.R.id.content).setOnTouchListener((view, event) -> {
            Snackbar.make(view, "Click detected at " + event.getX() + "," + event.getY(), Snackbar.LENGTH_SHORT)
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

            MotionEvent downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, (int)x, (int)y, 0);
            MotionEvent upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, (int)x, (int)y, 0);
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
        centre = "Centre: (" + xCentre + ", " + yCentre + ")";
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

        sensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
        sensorManager.getOrientation(rotationMatrix, orientationValues); //use transformedRotationMatrix
        pitch = orientationValues[1];
        roll = orientationValues[2];

        pitch = Math.round(pitch * 100) / (100f);
        roll = Math.round(roll * 100) / (100f);
        float dpitch = pitch - pitchOffset;
        float droll = roll - rollOffset;

        radPitchRoll = "RAD: raw pitch: " + pitch + ", raw roll: " + roll;

        mapToPointer(dpitch, droll);

        displayText();
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
        double pitchDeg = Math.toDegrees(dpitch);
        double rollDeg = Math.toDegrees(droll);

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

        degPitchRoll = "dpitch: " + (int) pitchDeg + "deg, dy: " + dy + "px, droll: " + (int) rollDeg + "deg, dx: " + dx + "px";
        coordinates = "Point: (" + x + ", " + y + ")";

        Rect bounds = cursor.copyBounds();
        bounds.left = x;
        bounds.top = y;
        bounds.right = x + CURSOR_WIDTH;
        bounds.bottom = y + CURSOR_HEIGHT;
        cursor.setBounds(bounds);
        findViewById(android.R.id.content).invalidate();
    }

    private void displayText() {
        textView = findViewById(R.id.value_format);
        textToDisplay = centre + "\n"
                + radPitchRoll + "\n"
                + degPitchRoll + "\n"
                + coordinates;
        textView.setText(textToDisplay);
    }

    public Point getClickCoordinates() {
        return new Point(x, y);
    }
}
