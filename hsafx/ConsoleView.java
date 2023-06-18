package hsafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * This is a re-implementation of the old hsa console by Holt Software
 * Associates, in order to use Java FX, and to present an API consistent with
 * the GraphicsContext and Scanner classes.
 *
 * For more details and version history, see Console.java.
 *
 * @author Sam Scott
 *
 * @version 6.0, May 24, 2018
 */
public abstract class ConsoleView extends Application {

    // private component vars
    private Stage stage;
    private Canvas liveCanvas;
    private Canvas glassPane;

    /**
     * start method sets up the window
     *
     * @param stage The FX stage to draw on
     */
    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root);
        this.stage = stage;
        liveCanvas = new Canvas(100, 100);
        glassPane = new Canvas(100, 100);
        root.getChildren().addAll(liveCanvas, glassPane);
        stage.setScene(scene);
        stage.setOnCloseRequest((WindowEvent t) -> {
            Platform.exit();
            System.exit(0);
        });
        Thread t = new Thread(() -> run());
        t.start();
    }

    public abstract void run();

    void setTitle(String title) {
        Platform.runLater(() -> {
            stage.setTitle(title);
        });
    }

    void show() {
        Platform.runLater(() -> {
            stage.show();
        });
    }

    Canvas getLiveCanvas() {
        return liveCanvas;
    }

    Canvas getGlassPane() {
        return glassPane;
    }
}
