# project-13-a SOFTENG-702-Android-Cursor
[![Build Status](https://travis-ci.com/mkem114/SOFTENG-702-Android-Cursor.svg?token=4tn5PhULbqssssJGM5Gs&branch=master)](https://travis-ci.com/mkem114/SOFTENG-702-Android-Cursor)

Android library for pointing and clicking with a floated cursor using sensors

**See releases for apk download: [GitLab](http://gitlab.nectar.auckland.ac.nz/literate-chickens/project-13-a/wikis/Releases), [GitHub](https://github.com/mkem114/SOFTENG-702-Android-Cursor/releases)**

For GitLab, if the link doesn't work, go to Wiki > Releases: http://gitlab.nectar.auckland.ac.nz/literate-chickens/project-13-a/wikis/Releases

## System Requirements

- Android phone
- SDK at least 24 (Android 7.0)
    - Target SDK is 27 (Android 8.1)

## Use

### Interactions
- Volume Down: Click
- Long Press Volume Down: Dragging (e.g. drag refresh, scroll, drag and drop)
- Volume Up: Traverse through different cursors or changing sensitivity (depending on a setting).
- Long press Volume Up: Focus mode (cursor moves slowly while the button is held down).
- Volume Up & Volume Down: Calibrate to re-centre cursor at current pitch and roll angles

### Use Cases
- Single-handed use
- Phone use without occlusion of the screen
- Projecting phone screen onto a display and interacting (e.g. for demos)

## Physical Buttons

Currently, as a proof of concept, the volume up button can be configured to toggle cursor sensitivity or switch cursor. This is an example of settings the application developer using our library can provide, to give their users more fine-grained control.

_User Study app provides this setting, so users can set VOL_UP to change cursor sensitivity, and select the mode that they prefer (see Cursor Sensitivity Levels below)._

_FindFood does not provide this setting, and the developer (us) has set in the code that VOL_UP changes the cursor appearance. The user therefore cannot change the cursor sensitivity._

## Cursor Sensitivity Levels

*   DWELL: if we think you intend on dwelling on the same location, we will keep the cursor stationary from jitter
*   SMOOTHEST: lowest sensitivity to change in angles, but may appear laggy
*   NO_DWELL: same sensitivity as dwell, but won't keep cursor stationary, for fine-grained control

_Note: FindFood is set to SMOOTHEST_

## Back-Tapping to Click Functionality

In order to utilising tapping the back of the device for clicking, the βTap Service application must be installed on the device. Simply install the application from Google Play - [Download βTap Service](https://play.google.com/store/apps/details?id=com.prhlt.aemus.BoDTapService). Then, the application that uses the cursor should be able to perform the clicking function via a back tap.

## For developers

### Install
Install the library `cursor` by importing it using New -> Import Module and selecting the cursor module in master

Run `gradle clean installDebug` locally

### Customise

You want to extend the current Activity using the Cursor Activity class. This class allows your view to use the cursor.
You can bind to onSensorChanged's super method to listen to sensor changes.

- `setCursorSensitivity(CursorSensitivity sensitivity)` - sets the current cursor's sensitivity
- `setVolUpTogglesSensitivity(boolean togglesSensitivity)` - Set what vol up button does. Currently either toggles cursor sensitivity or switches cursor
- `toggleNextCursorSensitivity()` - toggles to the next cursor's preset sensitivity
- `toggleNextCursor()` - cycles between different cursor appearances
- `simulateTouchMove()` - simulate a touch move event on the current position
- `simulateTouchDown()` - simulate a touch down event on the current position
- `simulateTouchUp()` - simulate a touch release event on the current position

_See FindFood for an example of a complete app that has our library imported_

## Project Plan vs. Implementation

<table>
  <tr>
   <td><strong>Project Plan</strong>
   </td>
   <td><strong>Implementation</strong>
   </td>
   <td><strong>Is achieved</strong>
   </td>
  </tr>
  <tr>
   <td>Identify alternate ways to perform pointing without using a finger, and evaluate suitability and feasibility
   </td>
   <td>Movement (pitch and roll) of device to control cursor
   </td>
   <td>Looked at the different methods of pointing - decided on a cursor
   </td>
  </tr>
  <tr>
   <td>Identify alternate ways to perform clicking without occlusion of the screen, and evaluate suitability and feasibility
   </td>
   <td>Included back tapping and side button click (volume down button).
   </td>
   <td>Completed using multiple clicking techniques
   </td>
  </tr>
  <tr>
   <td>Identify available embedded mobile phone sensors to provide alternate input (e.g. accelerometer, gyroscope, audio, etc. and combinations), and evaluate suitability and feasibility
   </td>
   <td>Used a combination of the accelerometer and the magnetometer for cursor movement. 
<p>
For back-tapping, the service we used utilised a combination of accelerometer, gyroscope, gravity, and microphone [1].
   </td>
   <td>Combinations of sensors identified and utilised
   </td>
  </tr>
  <tr>
   <td>Identify the ways a user can input information into a smartphone device by using an accelerometer
   </td>
   <td>Pointing is achieved by a moving cursor controlled by the movement of the device. Different click methods are included through the volume down button and back tapping
   </td>
   <td>Done with a point & click method.
   </td>
  </tr>
  <tr>
   <td>Implement mapping onto phone coordinates for pointing
   </td>
   <td>Pitch and roll angles are mapped onto y and x coordinates of cursor, respectively.
   </td>
   <td>Obtaining coordinates is achieved, with a multiplier that depends on the size of the display
   </td>
  </tr>
  <tr>
   <td>Evaluate the possibility and suitability of rebinding physical buttons
   </td>
   <td><ul>

<li>Volume Down: Click
<li>Long Press Volume Down: Dragging (e.g. drag refresh, scroll, drag and drop) \
Volume Up: Traverse through different cursors or changing sensitivity (depending on a setting).
<li>Long press Volume Up: Focus mode (cursor moves slowly while the button is held down).
<li>Volume Up & Volume Down: Calibrate to re-centre cursor at current pitch and roll angles</li></ul>

   </td>
   <td>Managed to rebind physical buttons for certain tasks
   </td>
  </tr>
  <tr>
   <td>Identify thresholds of movement to detect an input or action
   </td>
   <td>Using different sensitivity values for movement and dwell detection. The cursor sensitivity level of DWELL has a threshold where if we think you intend on dwelling on the same location, we will keep the cursor stationary from jitter.
   </td>
   <td>Experimented with the different threshold values
   </td>
  </tr>
  <tr>
   <td>Investigate between having our tool as a library that can be added into applications, and adding it as a separate application that runs on the phone independent of the current application
   </td>
   <td>Made android cursor an independent library that can be utilized in other sample applications (i.e. in FindFood and Sample App).
<p>
We provide methods where they can customise parameters accordingly (e.g. in our sample apps we have settings for users to select their own button rebinding).
   </td>
   <td>Created a library so developers can use the point and click method.
<p>
We cannot be a system application to overlay other applications due to security reasons, and our cursor also can't draw over pop ups for the same reason.
   </td>
  </tr>
  <tr>
   <td>Consider and implement the toggling this tool on and off 
   </td>
   <td>Developers can choose whether to include a cursor in their app.
   </td>
   <td>Because it's imported on code level, it is up to the app developer to decide whether they want to import our library, and/or whether they want a toggle in their app to change whether or not to utilise what we provide in our library
   </td>
  </tr>
  <tr>
   <td>Consider cancellations for misinputs and reversibility
   </td>
   <td>Works exactly the same way as a tap, so just move the cursor and click whatever cancellation means is provided by the app.
   </td>
   <td>No longer applicable, as the cursor can just click the back buttons, in the same way as tapping the touch screen.
   </td>
  </tr>
  <tr>
   <td colspan="3" ><strong>Implementation Plan</strong>
   </td>
  </tr>
  <tr>
   <td>Set up architecture, repository, and testing framework
   </td>
   <td>Overall set up with the library, sample application and etc is included.
   </td>
   <td>Completed
   </td>
  </tr>
  <tr>
   <td>Research algorithms for target selection (magnetic/sticky cursor, triangulation of motion, etc.)
   </td>
   <td>We implemented mapping to coordinates, low pass filtering to smooth movement, dwelling, and other thresholds and parameters.
   </td>
   <td>Completed, with some presets that can be put into settings
   </td>
  </tr>
  <tr>
   <td>Prototype algorithms as a small demo 
   </td>
   <td>Improved filtering and parameters throughout development.
   </td>
   <td>Completed
   </td>
  </tr>
  <tr>
   <td>Calculate mapping phone sensor events to change in target selection/pointer location on the screen
   </td>
   <td>Calculation performed with parameters set based on pilot studies.
   </td>
   <td>Completed
   </td>
  </tr>
  <tr>
   <td>Simulating a click event
   </td>
   <td>The library allows for different click methods that do not require a touch on the screen. This is through the volume down button and the back taper (if configured correctly).
   </td>
   <td>By exploring alternative click methods a click event is achieved.
   </td>
  </tr>
  <tr>
   <td>Recentering/calibration 
   </td>
   <td>The cursor can be recentered according to the current pitch and roll angles. 
   </td>
   <td>Achieved
   </td>
  </tr>
  <tr>
   <td>Toggle on/off 
   </td>
   <td>Developers can decide whether to include this library in their applications.
   </td>
   <td>The app developer has the flexibility in deciding whether they want to include this technique in their application.
   </td>
  </tr>
  <tr>
   <td>Demo application
   </td>
   <td>Utilized FindFood for using the point and click method for a realistic application. Also implemented a sample application which would provide a suitable indication for the usability study
   </td>
   <td>It has been applied to standalone applications.
   </td>
  </tr>
</table>

**Note to the Marker: Teamwork and Workflow**

Since we have experience working in this team, we have a preferred workflow which includes using peer/mob programming as well as squashing and merging branches. Additionally, we have started using our implementation using GitHub before the GitLab had been set up. As a result, commit history that was on branches which got squashed and merged will not be immediately visible on GitLab, and commit numbers and commit counts may not reflect the exact amount of contribution each team member has made. Our master branch with all the final implementation and master history is on GitLab, and we have confirmed with Danielle that this is fine. We also confirm that we have made equal amounts of contribution to this project (25% each).

**References:**

[1] E. Granell, L. A. Leiva (2016). Less Is More: Efficient Back-of-Device Tap Input Detection Using Built-in Smartphone Sensors. In _Proc. ISS '16_. ACM
