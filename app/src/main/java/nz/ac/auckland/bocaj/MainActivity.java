package nz.ac.auckland.bocaj;

import android.app.AlertDialog;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.app.Instrumentation;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

    }

}
