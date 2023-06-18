import hsafx.*;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.event.ActionEvent;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import java.util.ArrayList;

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

import javafx.scene.input.MouseEvent;

/**
 * Homemade budget version of Microsoft Paint. Sorta.
 *
 * @author      Zachary Sousa
 * @version     1.00
 */
public class MacroHardSketch extends Application {
    private Vector mousePos1 = null;
    private Vector mousePos2 = null;
    Alert alert = new Alert(Alert.AlertType.NONE);
    private GUI gui;
    private ArrayList<Shape> shapes = new ArrayList<Shape>();
    private Canvas canvas;
    
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
        Scene scene = new Scene(root, 900, 700);
        canvas = new Canvas(scene.getWidth(), scene.getHeight()); // Set canvas Size in Pixels
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().addAll(canvas);

        stage.setTitle("Macrohard Sketch Project"); // set the window title here
        stage.setScene(scene);
        
        gui = new GUI(canvas, root, this, Color.GREY);
        gui.draw(gc);
        
        //drawing behaviors
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, mouse -> {
            //get points for the shape to be drawn
            if(mouse.getY() < gui.getPos().getY()) {
                if(mousePos1 == null) mousePos1 = new Vector(mouse.getX(), mouse.getY());
                else mousePos2 = new Vector(mouse.getX(), mouse.getY());
            }
            
            //add shape and reset points
            if(mousePos1 != null && mousePos2 != null) {
                Vector origin   = new Vector(0, 0);
                Vector siz      = Vector.sub(mousePos2, mousePos1);
                if(siz.getX() > 0 && siz.getY() > 0) {
                    siz = new Vector(Math.abs(siz.getX()), Math.abs(siz.getY()));
                    origin = new Vector(mousePos1.getX(), mousePos1.getY());
                    if(gui.getShape().equals("rect")) {
                        shapes.add(new Rectangle(siz, gui.getCol(), gui.getStroke(), origin, gui.getStrokeWidth()));
                    } else if(gui.getShape().equals("elli")) {
                        shapes.add(new Ellipse(siz, gui.getCol(), gui.getStroke(), origin, gui.getStrokeWidth()));
                    }
                } else if(siz.getX() < 0 && siz.getY() < 0) {
                    siz = new Vector(Math.abs(siz.getX()), Math.abs(siz.getY()));
                    origin = new Vector(mousePos1.getX() - siz.getX(), mousePos1.getY() - siz.getY());
                    if(gui.getShape().equals("rect")) {
                        shapes.add(new Rectangle(siz, gui.getCol(), gui.getStroke(), origin, gui.getStrokeWidth()));
                    } else if(gui.getShape().equals("elli")) {
                        shapes.add(new Ellipse(siz, gui.getCol(), gui.getStroke(), origin, gui.getStrokeWidth()));
                    }
                } else if(siz.getX() < 0 && siz.getY() > 0) {
                    siz = new Vector(Math.abs(siz.getX()), Math.abs(siz.getY()));
                    origin = new Vector(mousePos1.getX() - siz.getX(), mousePos1.getY());
                    if(gui.getShape().equals("rect")) {
                        shapes.add(new Rectangle(siz, gui.getCol(), gui.getStroke(), origin, gui.getStrokeWidth()));
                    } else if(gui.getShape().equals("elli")) {
                        shapes.add(new Ellipse(siz, gui.getCol(), gui.getStroke(), origin, gui.getStrokeWidth()));
                    }
                } else if(siz.getX() > 0 && siz.getY() < 0) {
                    siz = new Vector(Math.abs(siz.getX()), Math.abs(siz.getY()));
                    origin = new Vector(mousePos1.getX(), mousePos1.getY() - siz.getY());
                    if(gui.getShape().equals("rect")) {
                        shapes.add(new Rectangle(siz, gui.getCol(), gui.getStroke(), origin, gui.getStrokeWidth()));
                    } else if(gui.getShape().equals("elli")) {
                        shapes.add(new Ellipse(siz, gui.getCol(), gui.getStroke(), origin, gui.getStrokeWidth()));
                    }
                } else {
                    alert.setAlertType(Alert.AlertType.WARNING);
                    alert.setContentText("Invalid shape dimensions.");
                    alert.show();
                }
                drawEverything(gc);
                
                mousePos1 = null;
                mousePos2 = null;
            }
            
        });
        
        stage.show();
    }
    
    /**
     * @return      shapes ArrayList
     */
    public ArrayList getShapes() {
        return shapes;
    }
    
    /**
     * draws everything
     */
    public void drawEverything(GraphicsContext gc) {
        new Rectangle(new Vector(canvas.getWidth(), canvas.getHeight()), Color.WHITE, Color.WHITE, new Vector(0, 0), gui.getStrokeWidth()).draw(gc);
        for(Shape s : shapes) {
            s.draw(gc);
        }
        gui.draw(gc);
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
