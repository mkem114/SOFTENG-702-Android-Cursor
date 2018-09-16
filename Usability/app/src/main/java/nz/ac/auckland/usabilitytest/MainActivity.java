package nz.ac.auckland.usabilitytest;

import android.Manifest;
import android.content.DialogInterface;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import nz.ac.auckland.cursor.CursorOverlay;

public class MainActivity extends CursorOverlay implements SensorEventListener {
    public static final int NUM_TO_PRESS = 20;

    private String logFileName = "/didntLoadYet.txt";
    private Button[] buttons = {};
    private int count = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get permissions to write to logs
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);

        // Get time
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        logFileName = "/SE702-" + mdformat.format(calendar.getTime()) + ".txt";

        setContentView(R.layout.activity_main);

        buttons = new Button[]{
                findViewById(R.id.buttontl),
                findViewById(R.id.buttontr),
                findViewById(R.id.buttonbl),
                findViewById(R.id.buttonbr),
                findViewById(R.id.buttonmtl),
                findViewById(R.id.buttonmtr),
                findViewById(R.id.buttonmbl),
                findViewById(R.id.buttonmbr)};
        printToLogs("Buttons to IDs respectively: top left, top right, bottom left, " +
                "bottom right, mid-top left, mid-top right, mid-bottom left, mid-bottom right");

        for (Button button : buttons) {
            button.setOnClickListener(this::onClick);
            printToLogs("" + button.getId());
        }

        RadioGroup radioGroup = findViewById(R.id.radios);
        radioGroup.setOnCheckedChangeListener((RadioGroup rGroup, int checkedId) -> {
            RadioButton cursorsRB = findViewById(R.id.Vcursor);
            if (cursorsRB.isChecked()) {
                setVolUpTogglesSensitivity(false);
            } else {
                setVolUpTogglesSensitivity(true);
            }
        });
    }

    private void onClick(View v) {
        int clickedButton = v.getId();

        printToLogs((count + 1) + ". User clicked button:" +
                clickedButton + " @ " + System.currentTimeMillis());

        int nextButtonId = v.getId();
        Button nextButton = buttons[0];
        if (buttons.length > 1) {
            while (clickedButton == nextButtonId) {
                nextButton = buttons[new Random().ints(
                        1,
                        0,
                        buttons.length - 1)
                        .sum()];
                nextButtonId = nextButton.getId();
            }
        } else if (buttons.length < 1) {
            throw new RuntimeException("Where are the buttons?");
        }

        for (Button button : buttons) {
            if (count == NUM_TO_PRESS - 1 || button == nextButton) {
                button.setEnabled(true);
            } else {
                button.setEnabled(false);
            }
        }

        count++;

        if (count >= NUM_TO_PRESS) {
            count = 0;
        }
    }

    private void printToLogs(String message) {
        File backupPath = Environment.getExternalStorageDirectory();
        backupPath = new File(backupPath.getPath() +
                "/Android/data/nz.ac.auckland.usabilitytest/logs");
        if (!backupPath.exists()) {
            if (!backupPath.mkdirs()) {
                AlertDialog.Builder delmessagebuilder = new AlertDialog.Builder(this);
                delmessagebuilder.setCancelable(false);
                delmessagebuilder.setMessage("Couldn't make directory for logs");
                delmessagebuilder.setNeutralButton("Okay",
                        ((DialogInterface dialog, int id) -> dialog.dismiss()));
                delmessagebuilder.create().show();
            }
        }
        FileOutputStream fos;

        try {
            fos = new FileOutputStream(backupPath.getPath() + logFileName, true);
            fos.write(message.getBytes());
            fos.write("\n".getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            AlertDialog.Builder delmessagebuilder = new AlertDialog.Builder(this);
            delmessagebuilder.setCancelable(false);
            delmessagebuilder.setMessage("Couldn't write to logs");
            delmessagebuilder.setNeutralButton("Okay",
                    ((DialogInterface dialog, int id) -> dialog.dismiss()));
            delmessagebuilder.create().show();
        }
    }
}
