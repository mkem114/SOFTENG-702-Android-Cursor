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

    //pixel coordinates
    private int x;
    private int y;

    //depends on display size
    private int maxX;
    private int maxY;
    private int xCentre;
    private int yCentre;

    //depends on calibrated pitch and roll
    private float pitchOffset;
    private float rollOffset;

    //multiplier (z height) for mapping pitch/roll to up/right
    private int pitchMultiplier;
    private int rollMultiplier;

    private float[] rotationMatrix = new float[9];
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] orientationValues = new float[3];

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
        fab.setOnClickListener(view -> {
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            MotionEvent downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 150, 900, 0);
            MotionEvent upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 150, 900, 0);
            findViewById(android.R.id.content).dispatchTouchEvent(downEvent);
            findViewById(android.R.id.content).dispatchTouchEvent(upEvent);
            downEvent.recycle();
            upEvent.recycle();
        });

        Drawable cursor = Objects.requireNonNull(ContextCompat.getDrawable(getBaseContext(), R.drawable.cursor));
        cursor.setBounds(new Rect(-40, -20, 100, 100));

        findViewById(android.R.id.content).getOverlay().add(cursor);

        findViewById(android.R.id.content).setOnTouchListener((view, event) -> {
            Snackbar.make(view, "Click detected at " + event.getX() + "," + event.getY(), Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            view.performClick();
            return true;
        });

        initialiseCalibration();
    }

    private void initialiseCalibration() {
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

        pitchOffset = 0;
        rollOffset = 0;

        // 45 degree rotation should take pointer to the edge
        pitchMultiplier = maxY / 2;
        rollMultiplier = maxX / 2;
    }

    /**
     * TODO later: updates offset if dwell for 5 seconds
     */
    private void recalibrate() {
        pitchOffset = 0; //pitch from sensor
        rollOffset = 0; //roll from sensor
    }

    @Override
    protected void onStart() {
        super.onStart();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = event.values.clone();
                break;
            default:
                return;
        }

        sensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
        /*
        TODO may need to transform
        //https://developer.android.com/reference/android/hardware/SensorManager.html#remapCoordinateSystem(float[],%20int,%20int,%20float[])
        float[] transformedRotationMatrix = new float[9];
        //eg Using the device as a mechanical compass when rotation is Surface.ROTATION_90
        sensorManager.remapCoordinateSystem(rotationMatrix, AXIS_Y, AXIS_MINUS_X, transformedRotationMatrix);
         */

        sensorManager.getOrientation(rotationMatrix, orientationValues); //use transformedRotationMatrix
        float pitch = orientationValues[1];
        float roll = orientationValues[2];
//
//        float dpitch = pitch - pitchOffset;
//        float droll = roll - rollOffset;

//        radPitchRoll = "RAD: raw pitch: " + pitch + ", dpitch: " + dpitch + ", raw roll: " + roll + ", droll: " + droll;
        radPitchRoll = "RAD: raw pitch: " + pitch + ", raw roll: " + roll;

//        mapToPointer(dpitch, droll);
        mapToPointer(pitch, roll);

        displayText();
    }

    private void mapToPointer(float pitch, float roll) {
        int dy = 0;
        int dx = 0;
        double pitchDeg = Math.toDegrees(pitch);
        double rollDeg = Math.toDegrees(roll);

        if (Math.abs(pitchDeg) > 5) {
            dy = (int) (Math.tan(pitch) * pitchMultiplier);
            y = yCentre + dy;
            y = y > maxY ? maxY : y;
            y = y < 0 ? 0 : y;
        }

        if (Math.abs(rollDeg) > 5) {
            dx = (int) (Math.tan(roll) * rollMultiplier);
            x = xCentre + dx;
            x = x > maxX ? maxX : x;
            x = x < 0 ? 0 : x;
        }

        degPitchRoll = "dpitch: " + (int) pitchDeg + "deg, dy: " + dy + "px, droll: " + (int) rollDeg + "deg, dx: " + dx + "px";
        coordinates = "Point: (" + x + ", " + y + ")";
    }

//    private void mapToPointer(float dpitchRad, float drollRad) {
//        double dpitchDeg = Math.toDegrees(dpitchRad);
//        double drollDeg = Math.toDegrees(drollRad);
//
//        int dy = (int) (Math.tan(dpitchDeg) * pitchMultiplier);
//        int dx = (int) (Math.tan(drollDeg) * rollMultiplier);
//
//        degPitchRoll = "dpitch: " + dpitchDeg + "deg, dy: " + dy + "px, droll: " + drollDeg + "deg, dx: " + "px";
//
//        y = yCentre + dy;
//        x = xCentre + dx;
//
//        coordinates = "Point: (" + x + ", " + y + ")";
//    }

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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //unused. Do nothing
    }
}
