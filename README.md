# project-13-a SOFTENG-702-Android-Cursor
[![Build Status](https://travis-ci.com/mkem114/SOFTENG-702-Android-Cursor.svg?token=4tn5PhULbqssssJGM5Gs&branch=master)](https://travis-ci.com/mkem114/SOFTENG-702-Android-Cursor)

Pointing and clicking with accelerometers

## Use

- Volume Down: Click
- Long Press Volume Down: Dragging (e.g. drag refresh, scroll, drag and drop)
- Volume Up: Traverse through different cursors or changing sensitivity (depending on a setting).
- Long press Volume Up: Focus mode (cursor moves slowly while the button is held down).
- Volume Up & Volume Down: Calibrate to re-centre cursor at current pitch and roll angles


## Install

Run `gradle clean installDebug` locally

## For developers

Install the library `cursor` by importing it using New -> Import Module and selecting the cursor module in master

## Customization

You want to extend the current Activity using the Cursor Activity class. This class allows your view to use the cursor.
You can bind to onSensorChanged's super method to listen to sensor changes.

- `setCursorSensitivity(CursorSensitivity sensitivity)` - sets the current cursor's sensitivity
- `setVolUpTogglesSensitivity(boolean togglesSensitivity)` - Set what vol up button does. Currently either toggles cursor sensitivity or switches cursor
- `toggleNextCursorSensitivity()` - toggles to the next cursor's preset sensitivity
- `toggleNextCursor()` - cycles between different cursor appearances
- `simulateTouchMove()` - simulate a touch move event on the current position
- `simulateTouchDown()` - simulate a touch down event on the current position
- `simulateTouchUp()` - simulate a touch release event on the current position

See FindFood for an example of a complete app that has our library imported
