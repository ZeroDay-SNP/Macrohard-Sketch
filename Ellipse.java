import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Ellipse.*;

/**
 * This class creates and can display an ellipse on the canvas. I reused this from the castles assignment so if this
 * breaks its past-tense me's fault.
 *
 * @author Dave Slemon, Zachary Sousa (docs, modified to use Shape class)
 * @version v100
 */
public class Ellipse extends Shape
{    
    /**
     *  Generic constructor for the Ellipse
     *  @author     Zachary Sousa
     *  @version    1.00
     */
    public Ellipse() {
        super();
    }
    
    /**
     *  Overload constructor for the Ellipse
     *  
     *  @param siz              Vector size of the Ellipse
     *  @param fillColor        the fill color of the Ellipse
     *  @param borderColor      the border color of the Ellipse
     *  @param pos              Vector position of the Ellipse
     *  @param lineWidth        stroke thickness of the Shape
     *  
     *  @author     Dave Slemon, Zachary Sousa (reworked to use Vector and Shape)
     *  @version    1.00
     */
    public Ellipse(Vector siz, Color fillColor, Color borderColor, Vector pos, int lineWidth)
    {
        super(siz, fillColor, borderColor, pos, lineWidth);
    }
    
    
    /**
     * draws the Ellipse
     * 
     * @param gc    GraphicsContext used in main program
     * 
     * @author      Dave Slemon, Zachary Sousa (docs, reworked to use Vector and Shape)
     * @version     1.00
     */
    public void draw(GraphicsContext gc)
    {
       gc.setFill(fillColor);
       gc.fillOval(pos.getX(),pos.getY(), siz.getX(), siz.getY());
       
       gc.setLineWidth(lineWidth);
       gc.setStroke(borderColor);
       gc.strokeOval(pos.getX(),pos.getY(), siz.getX(), siz.getY());
    }
    
    
    /**
     * @return      stats about the Ellipse
     * 
     * @author      Zachary Sousa 
     * @version     1.00
     */
    public String toString() {
        return "Shape #" + myId + ": " + "height = " + siz.getY() + "  width = " + siz.getX();
    }
    
    
}
