package sample;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import javax.imageio.ImageIO;

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

import com.openalpr.jni.Alpr;
import com.openalpr.jni.AlprPlate;
import com.openalpr.jni.AlprPlateResult;
import com.openalpr.jni.AlprResults;

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
    private VideoCapture capture = new VideoCapture();

    // a flag to change the button behavior
    private boolean cameraActive = false;

    // the id of the camera to be used
    private static int cameraId = 0;

     /** The action triggered by pushing the button on the GUI
     ** @param eventthe push button event
     **/

    // this is used to check if it's the time to scan for motion
    private int currentMotionTime = 0;

    //this is used to wait before scanning again for the car
    private int currentPlateTime = 0;
    // Mat used to check for motion
    private Mat frameDelta = new Mat();

    // variable used to determine the final licence plate
    private int i = 0;

    // used in the scanner algorithm
    private int modifier = 0;
    private String ScanForCar()throws Exception {
        // country: eu for Europe, us for USA
        // configfile: the location of openalpr.conf
        // runtimeDataDir: the location of runtime_data
        // licensePlate: the location of the license plate
        String country = "eu",
                configfile = "openalpr.conf",
                runtimeDataDir = "runtime_data",
                licensePlate;
        // detector initialization. do this once and don't mess with the files (parameters)
        Alpr alpr = new Alpr(country, configfile, runtimeDataDir);

        // set pattern to Romania (runtime_data/post_process/eu.patterns)
        // a.k.a. searches first for this kind of plates to decrease detection time
        alpr.setDefaultRegion("ro");

        // set the number of plates to return PER EACH SCAN
        // i.e. after each frame is scanned it returns a maximum of numberOfCandidates plates
        int numberOfCandidates = 3;
        alpr.setTopN(numberOfCandidates);

        // this is the number of frames to scan
        int numberOfFramesToDetect = 4;

        // these strings+floats are used in order to keep the values after each scan for future processing
        String[][] plateName = new String[numberOfFramesToDetect][numberOfCandidates];
        float[][] plateConfidence = new float[numberOfFramesToDetect][numberOfCandidates];
        // variable j used to populate plateName and plateConfidence vectors
        int j;

        // this is where i'll add individual processing times
        float totalProcessingTime = 0;

        // entering the loop that scans EACH frame
        for(int currentFrameToDetect = 0; currentFrameToDetect < numberOfFramesToDetect; currentFrameToDetect++) {

            ////////////////////////////////////////////////////////////////////////////////
            // CAMERA METHOD - RECOMMENDED FOR FINAL TESTING
            // Use the following code to make the application work directly with your camera
            // I recommend to use the alternative method for debugging
            //
            // How it works: Grabs a frame, then makes it a byte of array in order to make
            // the openalpr's "recognize" work with it.


            Mat original = grabFrame();
            if(original.empty()){
                System.err.println("Camera error. Please check the connection!");
                return "NULL";
            }
            BufferedImage img;
            img = Utils.matToBufferedImage(original);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();
            AlprResults results = alpr.recognize(imageInByte);


            /////////////////////////////////////////////////////////////////////////////////


            /////////////////////////////////////////////////////////////////////////////////////
            // ALTERNATIVE METHOD - RECOMMENDED FOR DEBUGGING
            // Warning: it is at least 10x slower that working directly with frames from the camera
            // "licensePlate" is the location of the image to test
            // !!!If you want to use another test images, update the number of frames too!
            //
            //  How it works: due to the facts that openalpr's "recognize" is projected to work with
            // local files, you just give the location of the file and it scans that file.
            //
            //licensePlate = "test_images\\test1 ("+(1+currentFrameToDetect)+").jpg";
            //AlprResults results = alpr.recognize(licensePlate);
            //
            /////////////////////////////////////////////////////////////////////////////////////

            // Debug Only: to make the scan results look better
        //    System.out.println("Plate Number:     Confidence:");

            // The following for statement initializes "result" with the first AND ONLY object in the
            // "results" list. If no object is present, this statement is skipped.
            for (AlprPlateResult result : results.getPlates()) {
                j = 0;

                // The following for statement declares "plate" object and then passes
                // each value in the "result" to it
                for (AlprPlate plate : result.getTopNPlates()) {

                    // there we store the name and confidence for each plate
                    plateName[currentFrameToDetect][j] = plate.getCharacters();
                    plateConfidence[currentFrameToDetect][j] = plate.getOverallConfidence();

                    // Debug Only: printing current scan results
       //             System.out.println(
       //                     plateName[currentFrameToDetect][j] +
       //                             "               " +
       //                             plateConfidence[currentFrameToDetect][j]
       //             );

                    j++;
                }
            }

            // add current frame processing time to total processing time
            totalProcessingTime += results.getTotalProcessingTimeMs();
        }

        // prints total processing time
        System.out.println(totalProcessingTime);

        // we will use "x" in the rest of the code to mark the reference frame (see next comment)
        // we will also use "j" as a variable
        int x = 0;

        // The reference frame is the frame that we will use to find the plate
        // with the highest confidence rate. First, we will find a frame that
        // has "numberOfCandidates" plates. If there aren't that many plates, it
        // checks for the frame that has "numberOfCandidates-1" plates and so on.
        // If we won't do this, there could be ignored plates and we don't want this
        for(j = numberOfCandidates - 1;  j >= 0; j--){
            while(plateName[x][j] == null) {
                if(x < numberOfFramesToDetect - 1){
                    x++;
                }
                else break;
            }
            if(x == numberOfFramesToDetect-1 && j!=0) {
                x = 0;
            }
            else {
                break;
            }
        }

        //this is the maximum number of plates found in a frame
        int actualNumberOfCandidates = j + 1;

        // if this statement is true then there are no plates detected in any frame
        if((x == numberOfCandidates && j == 0)||(x == 0 && j == 0)){
            //System.err.println("No plate detected in the frames");
            alpr.unload();
            return "NULL";
        }

        // This is where i'll add the confidence rate found in all frames
        // at the confidence rate of the reference frame
        for(j = 0; j < numberOfCandidates; j++) {

            // Testing that we're adding the confidence rate to the same plate
            // found in the reference plate and also skipping to add the same
            // confidence rate twice "x!=i"
            for (int i = 0; i < numberOfFramesToDetect; i++){

                for(int k = 0; k < numberOfCandidates; k++){
                    if (plateName[i][k] != null)
                    if (((plateName[i][k]).equals(plateName[x][j])) && x!=i ) {
                        plateConfidence[x][j] += plateConfidence[i][k];
                    }
                }

            }

        }

        // Finding the maximum confidence rate a.k.a. finding the actual plate number
        // and the actual confidence rate (TOTAL NOT AVERAGE)
        float maximumRate = plateConfidence[x][0];
        int actualPlateNumber = 0;
        for(int i = 1; i < actualNumberOfCandidates; i++){
            if(plateConfidence[x][i] > maximumRate){
                maximumRate = plateConfidence[x][i];
                actualPlateNumber = i;
            }
        }

        // Debug Only: printing plate number and the confidence rate
        //System.out.println("Total confidence rate(debug only):"+plateConfidence[x][actualPlateNumber]);
        //System.out.println("Plate number found:"+plateName[x][actualPlateNumber]);


        // make sure to call this to release memory.
        alpr.unload();
        return plateName[x][actualPlateNumber];
    }

    @FXML
    protected void startCamera (ActionEvent event)
    {
        // loads openalpr libraries
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
            this.capture.open(cameraId);

            // is the video stream available?
            if (this.capture.isOpened())
            {
                this.cameraActive = true;

                // set this to schedule when to scan for motion
                final int timeToMotionScan = 20;
                final int timeToPlateScan = 60;
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

                        // Checks if it should scan for motion
                        if (modifier == 0 && currentMotionTime == timeToMotionScan){
                            currentMotionTime = 0;
                            currentPlateTime = 0;

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
                                    modifier = 1;
                                    try {
                                        s[i] = ScanForCar();
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
                        else if(currentPlateTime == timeToPlateScan && modifier == 1){
                            currentPlateTime = 0;
                            currentMotionTime = 0;
                            try{
                                s[i] = ScanForCar();
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
                                    if(max>=3 && !s[varMax].equals("NULL")){
                                        System.out.println("\nThe final license plate:" + s[varMax]);
                                    }
                                    else {
                                        System.out.println("The scan failed to return a valid license plate.\nGoing back to the basic scanning.");
                                    }
                                    modifier = 0;
                                    i = 0;
                                }
                            }
                            catch (Exception e) {
                                System.err.print("Error at the entrance in the plate number scanner method " + e);
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

    private Mat grabFrame()
    {
        // init everything
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened())
        {
            try
            {
                // read the current frame
                this.capture.read(frame);
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

        if (this.capture.isOpened())
        {
            // release the camera
            this.capture.release();
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