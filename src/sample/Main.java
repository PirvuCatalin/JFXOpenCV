package sample;

import org.opencv.core.Core;
import Database.Database;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

import javax.xml.crypto.Data;

/**
 * The main class for a JavaFX application. It creates and handle the main
 * window with its resources (style, graphics, etc.).
 */

public class Main extends Application
{
    @Override
    public void start(Stage primaryStage)
    {
        try
        {
            // load the FXML resource
            FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));

            // store the root element so that the controllers can use it
            BorderPane rootElement = (BorderPane) loader.load();

            // create and style a scene
            Scene scene = new Scene(rootElement, 800, 600);

            //scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            // create the stage with the given title and the previously created
            // scene
            primaryStage.setTitle("Camera Systems");
            primaryStage.setScene(scene);

            // show the GUI
            primaryStage.show();

            // set the proper behavior on closing the application
            Controller controller = loader.getController();

            primaryStage.setOnCloseRequest((we -> controller.setClosed()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * For launching the application...
     * @param args optional params
     */

    public static void main(String[] args)
    {
        // load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        launch(args);
        Database db = new Database();

        db.getAllEntries();

    }
}
