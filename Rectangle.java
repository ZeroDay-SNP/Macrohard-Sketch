import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Rectangle.*;

/**
 * This class creates and can display a rectangle on the canvas. I reused this from the castles assignment so if this
 * breaks its past-tense me's fault.
 *
 * @author Dave Slemon, Zachary Sousa (docs, modified to use Shape class)
 * @version v100
 */
public class Rectangle extends Shape
{    
    /**
     *  Generic constructor for the Rectangle
     *  @author     Zachary Sousa
     *  @version    1.00
     */
    public Rectangle() {
        super();
    }
    
    /**
     *  Overload constructor for the Rectangle
     *  
     *  @param siz              Vector size of the Rectangle
     *  @param fillColor        the fill color of the Rectangle
     *  @param borderColor      the border color of the Rectangle
     *  @param pos              Vector position of the Rectangle
     *  
     *  @author     Dave Slemon, Zachary Sousa (reworked to use Vector and Shape)
     *  @version    1.00
     */
    public Rectangle(Vector siz, Color fillColor, Color borderColor, Vector pos)
    {
        super(siz, fillColor, borderColor, pos);
    }
    
    
    /**
     * draws the Rectangle
     * 
     * @param gc    GraphicsContext used in main program
     * 
     * @author      Dave Slemon, Zachary Sousa (docs, reworked to use Vector and Shape)
     * @version     1.00
     */
    public void draw(GraphicsContext gc)
    {
       gc.setFill(fillColor);
       gc.fillRect(pos.getX(),pos.getY(), siz.getX(), siz.getY());
       
       
       gc.setStroke(borderColor);
       gc.strokeRect(pos.getX(),pos.getY(), siz.getX(), siz.getY());
    }
    
    
    /**
     * @return      stats about the Rectangle
     * 
     * @author      Zachary Sousa 
     * @version     1.00
     */
    public String toString() {
        return "Shape #" + myId + ": " + "height = " + siz.getY() + "  width = " + siz.getX();
    }
    
    
}
