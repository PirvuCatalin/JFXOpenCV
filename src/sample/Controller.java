package sample;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import Utils.Utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 ** The controller for our application, where the application logic is
 ** implemented. It handles the button for starting/stopping the camera and the
 ** acquired video stream.
 **/

public class Controller
{

    // the FXML button
    @FXML
    private Button button;

    // the FXML image view
    @FXML
    private ImageView currentFrame;

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;

    // the OpenCV object that realizes the video capture
    private static VideoCapture capture = new VideoCapture();

    // a flag to change the button behavior
    private boolean cameraActive = false;

    // the id of the camera to be used
    private static int cameraId = 0;

     /** The action triggered by pushing the button on the GUI
     ** @param event the push button event
     **/

    // this is used to check if it's the time to scan for motion
    private int currentMotionTime = 0;
    // this is used to wait before scanning again for the car
    private int currentPlateTime = 0;
    // this is used to wait before checking where is the license plate on the frame
    private int currentPlatePoisitionTime = 0;

    // used to keep the value of the license plate after deciding if it's the final one
    private String finalLicensePlate = "NULL";

    // Mat used to check for motion
    private Mat frameDelta = new Mat();

    // variables used to determine the final licence plate
    private int i = 0;
    private int k = 0;

    // used in the scanner algorithm. initially it will scan for motion
    private String modifier = "motion";

    @FXML
    protected void startCamera (ActionEvent event)
    {
        // loads openALPR libraries
        try {
            System.loadLibrary("liblept170");
            System.loadLibrary("opencv_world300");
            System.loadLibrary("openalpr");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
        if (!this.cameraActive)
        {
            // start the video capture
            capture.open(cameraId);

            // is the video stream available?
            if (capture.isOpened())
            {
                this.cameraActive = true;

                // set this to schedule when to scan for motion and other timers
                // we should adjust these timers after we test this LIVE
                final int timeToMotionScan = 20;
                final int timeToPlateScan = 60;
                final int timeToPlatePositionScan = 60;

                // takes the first frame after camera is opened
                // to prevent errors
                frameDelta = grabFrame();

                // string[] used to determine the final licence
                String[] s = new String[5];
                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {
                    @Override
                    public void run() {
                        // effectively grab and process a single frame
                        Mat frame = grabFrame();

                        // convert and show the frame
                        Image imageToShow = Utils.mat2Image(frame);
                        updateImageView(currentFrame, imageToShow);

                        // updates times
                        currentPlateTime++;
                        currentMotionTime++;
                        currentPlatePoisitionTime++;

                        // Checks if it should scan for motion
                        if (modifier.equals("motion") && currentMotionTime == timeToMotionScan){
                            currentMotionTime = 0;
                            currentPlateTime = 0;
                            currentPlatePoisitionTime = 0;

                            Mat firstFrame = frameDelta;
                            List<MatOfPoint> contours = new ArrayList();

                            // this is the minimum area to consider it a motion
                            double maxArea = 50;

                            Imgproc.GaussianBlur(firstFrame, firstFrame, new Size(3, 3), 0);
                            Imgproc.GaussianBlur(frame, frame, new Size(3, 3), 0);

                            Core.subtract(firstFrame, frame, frameDelta);

                            Imgproc.cvtColor(frameDelta, frameDelta, Imgproc.COLOR_BGR2GRAY);

                            // modify third parameter (thresh) to be more/less sensitive
                            // if thresh is low -> the scanner is more sensible
                            Imgproc.threshold(frameDelta, frameDelta, 50, 255, Imgproc.THRESH_BINARY);

                            Mat v = new Mat();
                            Mat vv = frameDelta.clone();

                            Imgproc.findContours(vv, contours, v, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);


                            for (int idx = 0; idx < contours.size(); idx++) {
                                Mat contour = contours.get(idx);
                                double contourarea = Imgproc.contourArea(contour);
                                if (contourarea > maxArea) {

                                    // HERE IS WHAT HAPPENS IF MOVEMENT HAS BEEN FOUND
                                    modifier = "scan";
                                    try {
                                        s[i] = Utils.ScanForCar();
                                        System.out.println("Plate number returned by the method: " + s[i]);
                                        i++;

                                    }
                                    catch (Exception e) {
                                        System.err.print("Error at the entrance in the plate number scanner method " + e);
                                    }

                                    break;
                                }
                            }

                            // takes a frame to compare on future scans
                            frameDelta = grabFrame();
                        }

                        if(currentPlateTime == timeToPlateScan && modifier.equals("scan")){
                            currentPlateTime = 0;
                            currentMotionTime = 0;
                            currentPlatePoisitionTime = 0;

                            try{
                                s[i] = Utils.ScanForCar();
                                System.out.println("Plate number returned by the method: " + s[i]);
                                i++;
                                if (i == 5){
                                    int a[] = {0, 0, 0, 0, 0};
                                    for (int j = 0; j < 5; j++) {
                                        for (int k = 0; k < 5; k++) {
                                            if (s[k].equals(s[j])) {
                                                a[j]++;
                                            }
                                        }
                                    }
                                    int max = a[0];
                                    int varMax = 0;
                                    for (int k = 1; k < 5; k++) {
                                        if (max < a[k]) {
                                            max = a[k];
                                            varMax = k;
                                        }
                                    }
                                    i = 0;
                                    if(max>=3 && !s[varMax].equals("NULL")){
                                        finalLicensePlate = s[varMax];
                                        modifier = "platePosition";

                                        //
                                        //
          ////////////////////////////////// This is what happens when a new car comes to the station
                                        //      finalLicensePlate is the card license plate
                                        //
                                        System.out.println("\nThe final license plate:" + finalLicensePlate);
                                    }
                                    else {
                                        System.out.println("The scan failed to return a valid license plate.\nGoing back to the basic scanning.");
                                        modifier = "motion";
                                    }
                                }
                            }
                            catch (Exception e) {
                                System.err.print("Error at the entrance in the plate number scanner method " + e);
                                i = 0;
                                modifier = "motion";
                            }
                            frameDelta = grabFrame();
                        }

                        if(currentPlatePoisitionTime == timeToPlatePositionScan && modifier.equals("platePosition")){
                            currentMotionTime = 0;
                            currentPlateTime = 0;
                            currentPlatePoisitionTime = 0;
                            try{
                                if(1 == Utils.platePosition(finalLicensePlate)){
                                    k++;
                                }
                                i++;
                                if(i == 5){
                                    if(k < 2){
                                        //
                                        //
          ////////////////////////////////// This is what happens when the car left the station
                                        //
                                        //
                                        System.out.println("THE CAR LEFT!");
                                        modifier = "motion";
                                    }

                                    i = 0;
                                    k = 0;
                                }
                            }
                            catch (Exception e) {
                                System.err.print("Error at the entrance in the plate position scanner method " + e);
                                i = 0;
                                k = 0;
                                modifier = "motion";
                            }

                            frameDelta = grabFrame();
                        }


                    }

                };
                this.timer = Executors.newSingleThreadScheduledExecutor();

                // modify third argument to increase/decrease the time to wait for grabbing a new frame
                // 33 means 30FPS (Math!!!)
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                // update the button content
                this.button.setText("Stop Camera");
            }
            else
            {
                // log the error
                System.err.println("Impossible to open the camera connection...");
            }
        }
        else
        {
            // the camera is not active at this point
            this.cameraActive = false;

            // update again the button content
            this.button.setText("Start Camera");

            // stop the timer
            this.stopAcquisition();
        }
    }
    /** Get a frame from the opened video stream (if any)
     ** @return the {@link Mat} to show
     **/

    public static Mat grabFrame()
    {
        // init everything
        Mat frame = new Mat();

        // check if the capture is open
        if (capture.isOpened())
        {
            try
            {
                // read the current frame
                capture.read(frame);
            }
            catch (Exception e)
            {
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }
        return frame;
    }

    /**
     ** Stop the acquisition from the camera and release all the resources
     **/

    private void stopAcquisition()
    {
        if (this.timer!=null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (capture.isOpened())
        {
            // release the camera
            capture.release();
        }
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view
     *            the {@link ImageView} to update
     * @param image
     *            the {@link Image} to show
     */

    private void updateImageView(ImageView view, Image image)
    {
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * On application close, stop the acquisition from the camera
     */

    protected void setClosed()
    {
        this.stopAcquisition();
    }
}