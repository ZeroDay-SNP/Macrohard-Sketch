import javafx.scene.paint.*;
import javafx.scene.canvas.GraphicsContext;

/**
 * Abstract class Shape - basic parameters for any shape.
 *
 * @author      Zachary Sousa
 * @version     1.00
 */
public abstract class Shape implements Drawable
{
    protected Vector pos;
    protected Vector siz;
    protected Color fillColor;
    protected Color borderColor;
    private static int nextId = 0;
    protected int myId;
    protected int lineWidth;
    
    /**
     *  Shape constructor.
     *  
     *  @author     Zachary Sousa
     *  @version    1.00
     */
    public Shape()
    {
        this.siz = new Vector(50, 50);
        this.fillColor = Color.RED;
        this.borderColor = Color.BLACK;
        this.pos = new Vector(0, 0);
        this.lineWidth = 1;
        
        myId = nextId;
        nextId = nextId + 1;
    }
    
    /**
     *  Shape constructor overload.
     *  
     *  @param siz              Vector size of the shape
     *  @param fillColor        the fill color of the Shape
     *  @param borderColor      the border color of the Shape
     *  @param pos              Vector position of the Shape
     *  @param lineWidth        stroke thickness of the Shape
     *  
     *  @author     Dave Slemon, Zachary Sousa (reworked to use Vector)
     *  @version    1.00
     */
    public Shape(Vector siz, Color fillColor, Color borderColor, Vector pos, int lineWidth)
    {
        this.siz = siz;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.pos = pos;
        this.lineWidth = lineWidth;
        
        myId = nextId;
        nextId = nextId + 1;
    }
    
    /**
     *  @return     the id of the shape
     *  @author     Zachary Sousa
     *  @version    1.00
     */
    public int getId()
    {
        return myId;
    }
    
    /**
     * Override this with drawing code. Pretty self explanatory.
     */
    public abstract void draw(GraphicsContext gc);
}
