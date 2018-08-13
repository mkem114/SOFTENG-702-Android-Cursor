package nz.ac.auckland.bocaj;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private int x;
    private int y;

    //depends on display size
    private int xCentre;
    private int yCentre;

    //depends on calibrated pitch and roll
    private float pitchOffset;
    private float rollOffset;

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

        initialiseCalibration();
    }

    private void initialiseCalibration() {
        Display mdisp = getWindowManager().getDefaultDisplay();
        Point mdispSize = new Point();
        mdisp.getSize(mdispSize);
        int maxX = mdispSize.x;
        int maxY = mdispSize.y;
        xCentre = maxX / 2;
        yCentre = maxY / 2;

        pitchOffset = 0;
        rollOffset = 0;
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

        float dpitch = pitch - pitchOffset;
        float droll = roll - rollOffset;

        TextView textView = findViewById(R.id.value_format);
        textView.setText("raw pitch: " + pitch + ", dpitch: " + dpitch + ", raw roll: " + roll + ", droll: " + droll);

        mapToPointer(dpitch, droll);
    }

    private void mapToPointer(float dpitch, float droll) {
        //TODO magic maths happens here
        int dy = (int) dpitch;
        int dx = (int) droll;

        x = xCentre + dx;
        y = yCentre + dy;
    }

    public Point getClickCoordinates() {
        return new Point(x, y);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //unused. Do nothing
    }
}
