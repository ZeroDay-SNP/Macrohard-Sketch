import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.canvas.*;


/**
 * Handles all of the GUI elements. Really not needed but I felt
 * it cleaned things up a little bit, made it MOAR MODULAR!!
 *
 * @author      Zachary Sousa
 * @version     1.00
 */
public class GUI implements Drawable
{
    private Vector pos;
    private Color bgCol;
    private final int height = 150;
    private int width;
    private Rectangle backdrop;
    
    private MacroHardSketch app;
    
    private Alert alert = new Alert(Alert.AlertType.NONE);
    
    private ArrayList<Shape> shapes = new ArrayList<Shape>();
    
    private Canvas canvas;
    
    //buttons
    private ColorPicker colPicker;
    private ColorPicker strPicker;
    private Button rectangle;
    private Button ellipse;
    private Button undo;
    private TextField strokeField;
    private int strokeWidth;
    private Label instruction;
    
    private String curShape = "rect";
    
    /**
     * Constructor for GUI
     * @param canvas        the canvas
     * @param bgCol         background color for the GUI
     */
    public GUI(Canvas canvas, Pane root, MacroHardSketch app, Color bgCol) {
        pos = new Vector(0, canvas.getHeight() - this.height);
        this.canvas = canvas;
        this.app = app;
        this.bgCol = bgCol;
        this.width = (int)canvas.getWidth();
        
        colPicker   = new ColorPicker(Color.RED);
        strPicker   = new ColorPicker(Color.BLUE);
        rectangle   = new Button("Rectangle");
        ellipse     = new Button("Ellipse");
        undo        = new Button("Undo");
        strokeField = new TextField("5");
        strokeField.setPrefWidth(50);
        instruction = new Label("INSTRUCTIONS:\n-Use the buttons and text field on the left to customize your drawing tool\n-The button on the right will undo the latest shape\n-The top color picker on the left is for the fill color, the other one is for the stroke color\n-The other 2 buttons select which shape to draw with\n-The textfield represents the stroke weight\n-Click once to designate the origin of the shape, click again to set the size and draw");
        
        backdrop = new Rectangle(new Vector(width, height), bgCol, bgCol, pos, getStrokeWidth());
        
        root.getChildren().addAll(instruction, colPicker, strPicker, rectangle, ellipse, undo, strokeField);
        
        colPicker   .relocate(pos.getX() + 30, pos.getY() + 30);
        strPicker   .relocate(pos.getX() + 30, pos.getY() + 90);
        rectangle   .relocate(pos.getX() + 200, pos.getY() + 30);
        ellipse     .relocate(pos.getX() + 270, pos.getY() + 30);
        undo        .relocate(canvas.getWidth() - 100, pos.getY() + 30);
        strokeField .relocate(pos.getX() + 320, pos.getY() + 30);
        instruction .relocate(pos.getX() + 400, pos.getY() + 10);
        
        rectangle.setOnAction(event -> selectRect());
        ellipse.setOnAction(event -> selectEllipse());
        undo.setOnAction(event -> doUndo());
    }
    
    /**
     * draws the GUI
     * 
     * @param gc    GraphicsContext used in main program
     * 
     * @author      Zachary Sousa
     * @version     1.00
     */
    public void draw(GraphicsContext gc) {
        backdrop.draw(gc);
    }
    
    /**
     * called when the Rectangle button is pressed
     */
    public void selectRect() {
        curShape = "rect";
    }
    
    /**
     * called when the Ellipse button is pressed
     */
    public void selectEllipse() {
        curShape = "elli";
    }
    
    /**
     * called when the Undo button is pressed. Removes the last shape.
     */
    public void doUndo() {
        shapes = app.getShapes();
        try{
            shapes.remove(shapes.size()-1);   
            app.drawEverything(canvas.getGraphicsContext2D());
        } catch(Exception e) {
            alert.setAlertType(Alert.AlertType.WARNING);
            alert.setContentText("Nothing to undo.");
            alert.show();
        }
    }
    
    /**
     * @return      the fill color
     * @author      Zachary Sousa
     * @version     1.00
     */
    public Color getCol() {
        return colPicker.getValue();
    }
    
    /**
     * @return      the stroke color
     * @author      Zachary Sousa
     * @version     1.00
     */
    public Color getStroke() {
        return strPicker.getValue();
    }
    
    /**
     * @return      the current selected shape
     * @author      Zachary Sousa
     * @version     1.00
     */
    public String getShape() {
        return curShape;
    }
    
    /**
     * @return      the stroke width
     * @author      Zachary Sousa
     * @version     1.00
     */
    public int getStrokeWidth() {
        try {
            int i = Integer.parseInt(strokeField.getText());
            return i;
        } catch (Exception e) {
            alert.setAlertType(Alert.AlertType.WARNING);
            alert.setContentText("Stroke width invalid.");
            alert.show();
            return 0;
        }
    }
    
    /**
     * @return      the position of GUI
     * @author      Zachary Sousa
     * @version     1.00
     */
    public Vector getPos() {
        return pos;
    }
    
}
