package org.firstinspires.ftc.teamcode;

import android.os.Environment;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.vuforia.HINT;
import com.vuforia.Vuforia;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

@Autonomous(name = "Vuforia Demo", group = "Test")
public class SkystoneIdentification extends LinearOpMode {

    // Used to convert between inches and millimeters ([value in in.] * mmPerInch = [value in mm.])
    private static final float mmPerInch = 25.4f;

    // Fields for Vuforia
    private VuforiaTrackables visionTargets;
    private VuforiaLocalizer vuforiaLocalizer;
    private VuforiaLocalizer.Parameters parameters;

    // Stores all the initialized VuforiaTrackables to check for detected markers
    private ArrayList<VuforiaTrackable> allTrackables;

    // The last known location of the bot, obtained from Vuforia
    // The phone's offset from the center of the bot with the long side along the y axis and screen up
    private OpenGLMatrix lastKnownLocation, phoneLocation;

    private float robotX = 0, robotY = 0, robotAngle = 0;

    public void runOpMode() throws InterruptedException {
        setupVuforia();

        // We don't know where the robot is, so set it to the origin
        // If we don't include this, it would be null, which would cause errors later on
        lastKnownLocation = originMatrix();

        telemetry.addLine("Ready!");
        telemetry.update();

        waitForStart();

        // Start tracking the targets
        visionTargets.activate();

        while (opModeIsActive()) {
            for (VuforiaTrackable trackable : allTrackables) {
                VuforiaTrackableDefaultListener listener = (VuforiaTrackableDefaultListener) trackable.getListener();

                if (listener.isVisible()) {
                    // Ask the listener for the latest information on where the robot is
                    OpenGLMatrix latestLocation = listener.getUpdatedRobotLocation();

                    // The listener will sometimes return null, so we check for that to prevent errors
                    if (latestLocation != null)
                        lastKnownLocation = latestLocation;

                    float[] coordinates = lastKnownLocation.getTranslation().getData();

                    robotX = coordinates[0];
                    robotY = coordinates[1];
                    robotAngle = Orientation.getOrientation(lastKnownLocation, AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES).thirdAngle;

                    // Send information about whether the target is visible, and where the robot is
                    telemetry.addData("Tracking " + trackable.getName(), listener.isVisible());
                    telemetry.addData("Last Known Location", formatMatrix(lastKnownLocation));
                    break;
                }
            }

            // Send telemetry and idle to let hardware catch up
            telemetry.update();
            idle();
        }
    }

    private void setupVuforia() throws InterruptedException {
        // Read VUFORIA_KEY from SD Card file for the security of the new developer key
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File file = new File(sdcard, "FIRST/Vuforia_Key_2019_2020.txt");

        //Read text from file
        StringBuilder vuforiaKey = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                vuforiaKey.append(line);
                vuforiaKey.append('\n');
            }
            br.close();
        } catch (IOException e) {
            throw new InterruptedException("Please store the Vuforia key on the phone's SD card in the FIRST folder and make sure the file name reflects the one used in the code!");
        }

        telemetry.addLine("NOTICE: If no camera view is visible on the robot controller, the Vuforia key is missing from the SD card!");
        telemetry.update();

        // Setup parameters to create localizer
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);
        parameters.vuforiaLicenseKey = vuforiaKey.toString();
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        parameters.useExtendedTracking = false;
        vuforiaLocalizer = ClassFactory.getInstance().createVuforia(parameters);

        // These are the vision targets that we want to use
        visionTargets = vuforiaLocalizer.loadTrackablesFromFile("/sdcard/FIRST/Skystone");
        Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 4);


        // Set phone location on robot (from center with screen facing up)
        phoneLocation = createMatrix(0, 0, 0, 90, 0, 0);

        // Initialize the VuforiaTrackable objects and stores them in allTrackables
        allTrackables = new ArrayList<>();

        initTrackable(0, "Stone Target", originMatrix());
        initTrackable(1, "Blue Rear Bridge", originMatrix());
        initTrackable(2, "Red Rear Bridge", originMatrix());
        initTrackable(3, "Red Front Bridge", originMatrix());
        initTrackable(4, "Blue Front Bridge", originMatrix());
        initTrackable(5, "Red Perimeter 1", originMatrix());
        initTrackable(6, "Red Perimeter 2", originMatrix());
        initTrackable(7, "Front Perimeter 1", originMatrix());
        initTrackable(8, "Front Perimeter 2", originMatrix());
        initTrackable(9, "Blue Perimeter 1", createMatrix(0, 36 * mmPerInch, 0, 90, 0, 90));
        initTrackable(10, "Blue Perimeter 2", originMatrix());
        initTrackable(12, "Rear Perimeter 1", originMatrix());
        initTrackable(11, "Rear Perimeter 2", originMatrix());
    }

    /* Creates a matrix for determining the locations and orientations of objects
    // Units are millimeters for x, y, and z, and degrees for u, v, and w
    // u is rotation on the x axis, v on the y axis, and w on the z axis
    // The right hand rule should be utilized in keeping dimensions in check
    // The rotations are applied in the following order: x, y, z axes
    // This is crucial to know because the order can greatly alter your final result
    // Once again, use the right hand rule and visualize the rotation of the object
    */
    private OpenGLMatrix createMatrix(float x, float y, float z, float u, float v, float w) {
        return OpenGLMatrix.translation(x, y, z).
                multiplied(Orientation.getRotationMatrix(
                        AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES, u, v, w));
    }

    // Creates a matrix with 0 for all values
    // On the field, this represents the bottom left corner of the field from the audience's view
    // This is the corner with the red depot (the red square) by the human player station
    private OpenGLMatrix originMatrix() {
        return createMatrix(0, 0, 0, 0, 0, 0);
    }

    // Formats a matrix into a readable string
    private String formatMatrix(OpenGLMatrix matrix) {
        return matrix.formatAsTransform();
    }

    private void initTrackable(int targetIndex, String name, OpenGLMatrix location) {
        VuforiaTrackable trackable = visionTargets.get(targetIndex);
        trackable.setName(name);
        trackable.setLocation(location);

        // Setup listener and inform it of phone information
        ((VuforiaTrackableDefaultListener) trackable.getListener()).setPhoneInformation(phoneLocation, parameters.cameraDirection);

        allTrackables.add(trackable);
    }
}