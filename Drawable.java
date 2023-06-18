import javafx.scene.canvas.GraphicsContext;
/**
 * Basic interface for making things drawable.
 * This is kind of unnessecary(?), the specs have it though so I added it.
 *
 * @author      Zachary Sousa
 * @version     1.00
 */
public interface Drawable
{
    /**
     * Override this with drawing code. Pretty self explanatory.
     */
    public abstract void draw(GraphicsContext gc);
}
