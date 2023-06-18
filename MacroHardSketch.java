import hsafx.*;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.event.ActionEvent;
import javafx.scene.canvas.*;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;
import static javafx.application.Application.launch;

/**
 * Homemade budget version of Microsoft Paint. Sorta.
 *
 * @author      Zachary Sousa
 * @version     1.00
 */
public class MacroHardSketch extends Application {

    // TODO: Instance Variables for View Components and Model
    // TODO: Private Event Handlers and Helper Methods
    /**
     * This is where you create your components and the model and add event
     * handlers.
     *
     * @param stage The main stage
     * @throws Exception
     */
    @Override
    public void start(Stage stage) throws Exception {
        Pane root = new Pane();
        Scene scene = new Scene(root);
        Canvas canvas = new Canvas(400, 300); // Set canvas Size in Pixels
        GraphicsContext gc = canvas.getGraphicsContext2D();

        stage.setTitle("Macrohard Sketch Project"); // set the window title here
        stage.setScene(scene);
        // TODO: Add your GUI-building code here

        // 1. Create the model
        // 2. Create the GUI components
        // 3. Add components to the root
        // 4. Configure the components (colors, fonts, size, location)
        // 5. Add Event Handlers and do final setup
        // 6. Show the stage
        stage.show();
    }

    /**
     * Make no changes here.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        launch(args);
    }
}
