# project-13-a SOFTENG-702-Android-Cursor
[![Build Status](https://travis-ci.com/mkem114/SOFTENG-702-Android-Cursor.svg?token=4tn5PhULbqssssJGM5Gs&branch=master)](https://travis-ci.com/mkem114/SOFTENG-702-Android-Cursor)

Pointing and clicking with accelerometers

## Use

- VOL_DOWN - simulate touch press (drag/drop supported!)
- VOL_UP - cycle through available cursors
- VOL_UP (Long press) - toggle focused mode (decrease sensitivity, slow down movements)
- Both VOL_UP and VOL_DOWN (short tap) - calibrate to centre

## Install

Run `gradle clean installDebug` locally

## For developers

Install the library `cursor' by importing it using New -> Import Module and selecting the cursor module in master

## Customization

You want to extend the current Activity using the Cursor Activity class. This class allows your view to use the cursor.
You can bind to onSensorChanged's super method to listen to sensor changes.

- setCursorSensitivity(CursorSensitivity sensitivity) - sets the current cursor's sensitivity
- toggleNextCursorSensitivity() - toggles to the next cursor's preset sensitivity
- simulateTouchMove() - simulate a touch move event on the current position
- simulateTouchDown() - simulate a touch down event on the current position
- simulateTouchUp() - simulate a touch release event on the current position

See FindFood for an example of a complete app that has our library imported
