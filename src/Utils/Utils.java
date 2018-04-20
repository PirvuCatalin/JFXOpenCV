package Utils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;

import org.opencv.core.Mat;
import com.openalpr.jni.*;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;

import sample.Controller;

/**
 * Provide general purpose methods for handling OpenCV-JavaFX data conversion.
 * Moreover, expose some "low level" methods for matching few JavaFX behavior.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a>
 * @version 1.0 (2016-09-17)
 * @since 1.0
 *
 */

public final class Utils
{
    /**
     * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
     *
     * @param frame
     *            the {@link Mat} representing the current frame
     * @return the {@link Image} to show
     */

    public static Image mat2Image(Mat frame)
    {
        try
        {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        }
        catch (Exception e)
        {
            System.err.println("Cannot convert the Mat obejct: " + e);

            return null;
        }
    }

    /**
     * Generic method for putting element running on a non-JavaFX thread on the
     * JavaFX thread, to properly update the UI
     *
     * @param property
     *            a {@link ObjectProperty}
     * @param value
     *            the value to set for the given {@link ObjectProperty}
     */

    public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
    {
        Platform.runLater(() -> property.set(value));
    }

    /**
     * Support for the {@link mat2image()} method
     *
     * @param original
     *            the {@link Mat} object in BGR or grayscale
     * @return the corresponding {@link BufferedImage}
     */

    public static BufferedImage matToBufferedImage(Mat original)
    {
        // init
        BufferedImage image;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1)
        {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        }
        else
        {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }

        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    public static String ScanForCar()throws Exception {
        // country: eu for Europe, us for USA
        // configfile: the location of openalpr.conf
        // runtimeDataDir: the location of runtime_data
        String country = "eu",
                configfile = "openalpr.conf",
                runtimeDataDir = "runtime_data";

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


            Mat original = Controller.grabFrame();
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
            //String licensePlate = "test_images\\test1 ("+(1+currentFrameToDetect)+").jpg";
            //AlprResults results = alpr.recognize(licensePlate);
            //
            /////////////////////////////////////////////////////////////////////////////////////

            // Debug Only: to make the scan results look better
            //System.out.println("Plate Number:     Confidence:");

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
            // make sure to call this to release memory.
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

    public static int platePosition(String finalPlate) throws Exception{
        // country: eu for Europe, us for USA
        // configfile: the location of openalpr.conf
        // runtimeDataDir: the location of runtime_data
        String country = "eu",
               configfile = "openalpr.conf",
                runtimeDataDir = "runtime_data";

        // detector initialization. do this once and don't mess with the files (parameters)
        Alpr alpr = new Alpr(country, configfile, runtimeDataDir);

        // set pattern to Romania (runtime_data/post_process/eu.patterns)
        // a.k.a. searches first for this kind of plates to decrease detection time
        alpr.setDefaultRegion("ro");

        // set the number of plates to return PER EACH SCAN
        // i.e. after each frame is scanned it returns a maximum of numberOfCandidates plates
        int numberOfCandidates = 4;
        alpr.setTopN(numberOfCandidates);

        // recognition
        Mat original = Controller.grabFrame();
        if(original.empty()){
            System.err.println("Camera error. Please check the connection!");
            return 0;
        }

        BufferedImage img;
        img = Utils.matToBufferedImage(original);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();

        AlprResults results = alpr.recognize(imageInByte);

        for (AlprPlateResult result : results.getPlates()) {

            // The following for statement declares "plate" object and then passes
            // each value in the "result" to it
            for (AlprPlate plate : result.getTopNPlates()) {
                if( finalPlate.equals(plate.getCharacters()) ){
                    return 1;
                }
            }
        }

        return 0;
    }
}