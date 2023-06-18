package hsafx;

import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.image.Image;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;

import javafx.application.Platform;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import javafx.scene.text.Text;
import javafx.geometry.Bounds;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * This is a re-implementation of the old hsa console by Holt Software
 * Associates in order to use Java FX, and to present an API consistent with the
 * GraphicsContext and Scanner classes.
 * <p>
 * UPDATE HISTORY: Some update history details have been lost. Notably the
 * addition of mouse routines (Josh Gray?), anti-aliasing (Josh Gray?), and
 * keyboard listener routines (Sam Scott, date unknown)
 * <p>
 * Update September 2014: Fixed bug in getRow() and getColumn(); they now report
 * the current cursor position without requiring a print() first.
 * <p>
 * Update August 2012: Changed synchronization to synchronize on the associated
 * Console object. Now application writers can also synchronize on the Console
 * object to kill the last remaining cases of screen flicker.
 * <p>
 * Update April 30, 2010: Re-done from scratch in Swing with much code imported
 * from the old hsa console. The main goals were to reduce screen flicker during
 * animations and eliminate a couple of small bugs in the input routines.
 *
 * @author Sam Scott (conversion to Swing, conversion to FX)
 * @author Michael Harwood (minor text printing bug fix)
 * @author Josh Gray (getRow()/getColumn() bug fix)
 * @author Tom West (original hsa code)
 * @version 6.0 May 24, 2018
 */
public final class Console extends Canvas implements ActionListener {

    /**
     * Window title *
     */
    private final String title;
    /**
     * Container of this object *
     */
    private final ConsoleView container;
    /**
     *
     */
    private final GraphicsContext g;

    // ***** Screen variables *****
    private Color strokeColor = Color.BLACK;
    private Color fillColor = Color.BLACK;
    private Color backgroundColor = Color.WHITE;
    /**
     * Screen size *
     */
    private final double width, height;
    /**
     * Font for drawString *
     */
    private Font drawStringFont = new Font("sansserif", 12);
    /**
     * Timer object for flashing cursor *
     */
    private final Timer timer;

    /* lineWidth */
    private int lineWidth = 1;

    private boolean autoRefresh = true;

    // ***** Text input/output variables *****
    private final Font textFont;
    private final int fontHeight;
    private final double fontBase, fontWidth;
    private int cursorRow = 0, cursorCol = 0;
    private boolean cursorFlashing = false;
    private final int flashSpeed = 20; // speed is in frames (see framesPerSecond above)
    private int flashCount = 0;
    private boolean cursorVisible = false;

    // ***** Text output variables - adapted from original hsa package *****
    private static final int MARGIN = 3;
    private int currentRow = 0, currentCol = 0;
    private int actualRow = 0, actualCol = 0;
    private int startCol = 0, startRow = 0;
    private int maxRow = 0, maxCol = 0;
    private final static int TAB_SIZE = 8;

    // ***** Keyboard Buffer & Input Variables - adapted from original hsa package *****
    private static final int BUFFER_SIZE = 2048;
    private static final int EMPTY_BUFFER = -1;
    private final char[] kbdBuffer = new char[BUFFER_SIZE];
    private int kbdBufferHead = 0, kbdBufferTail = 0;
    private final char[] lineBuffer = new char[BUFFER_SIZE];
    private int lineBufferHead = 0, lineBufferTail = 0;
    private int ungotChar = EMPTY_BUFFER;
    private final boolean echoOn = true;
    //private boolean clearToEOL = true;

    // New Keyboard variables
    /**
     * Code for key currently held down *
     */
    private KeyCode currentKeyCode = null; //TTD GraphicsConsole.VK_UNDEFINED;
    /**
     * Code for last key pressed *
     */
    private KeyCode lastKeyCode = currentKeyCode;
    /**
     * Character currently held down *
     */
    private char currentKeyChar = (char) 0; // TTD GraphicsConsole.VK_UNDEFINED;
    /**
     * Last character pressed *
     */
    private char lastKeyChar = currentKeyChar;
    /**
     * Size of keysDown array *
     */
    private final int numKeyCodes = 256;
    /**
     * Array of booleans representing characters currently held down *
     */
    private HashSet<KeyCode> keysDown = new HashSet<>();

    /**
     * flashes per second
     */
    private final int framesPerSecond = 60;

    private final GraphicsContext liveCanvasGraphics, glassPaneGraphics;

    // Mouse Variables
    private final boolean mouseButton[] = {false, false, false};
    private double mouseX = 0, mouseY = 0;
    private int mouseClick = 0;
    private boolean mouseDrag = false;
    private Point startDrag, endDrag;

    /**
     * Creates a new console.
     *
     * @param width     The width in pixels
     * @param height    The height in pixels
     * @param fontSize  The font size for print/println
     * @param title     The title of the window
     * @param container The ConsoleView object container
     */
    public Console(double width, double height, int fontSize, String title, ConsoleView container) {
        super(width, height);

        this.height = height;
        this.width = width;
        this.container = container;
        this.title = title;

        Canvas liveCanvas = container.getLiveCanvas();
        this.liveCanvasGraphics = liveCanvas.getGraphicsContext2D();
        liveCanvas.setWidth(width);
        liveCanvas.setHeight(height);

        Canvas glassPane = container.getGlassPane();
        this.glassPaneGraphics = glassPane.getGraphicsContext2D();
        glassPane.setWidth(width);
        glassPane.setHeight(height);

        container.setTitle(title + " - Running");
        container.show();

        this.g = this.getGraphicsContext2D();

        // text i/o system
        this.textFont = new Font("Courier New", fontSize);
        FontMetrics fm = Toolkit.getToolkit().getFontLoader().getFontMetrics(this.textFont);
        this.fontHeight = (int) (fm.getLineHeight() + 0.5);
        this.fontBase = fm.getDescent();
        this.fontWidth = reportSize("A", this.textFont);
        this.maxCol = (int) ((width - 2 * Console.MARGIN) / this.fontWidth - 1);
        this.maxRow = (int) ((height - 2 * Console.MARGIN) / this.fontHeight - 1);

        // clear to background color
        clear();

        // key listeners
        liveCanvas.setOnKeyPressed(this::keyPressed);
        liveCanvas.setOnKeyReleased(this::keyReleased);
        liveCanvas.setOnKeyTyped(this::keyTyped);

        // make sure live canvas has focus
        liveCanvas.setFocusTraversable(true);
        Platform.runLater(() -> {
                liveCanvas.requestFocus();
            });

        // mouse listeners
        glassPane.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        glassPane.addEventHandler(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
        glassPane.addEventHandler(MouseEvent.MOUSE_MOVED, this::mouseMoved);
        glassPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
        glassPane.addEventHandler(MouseEvent.MOUSE_ENTERED, this::mouseEntered);
        glassPane.addEventHandler(MouseEvent.MOUSE_EXITED, this::mouseExited);

        // start flash timer
        timer = new Timer(1000 / framesPerSecond, this);
        timer.start();
    }

    // from stack overflow - get width of string
    public double reportSize(String s, Font myFont) {
        Text text = new Text(s);
        text.setFont(myFont);
        Bounds tb = text.getBoundsInLocal();
        Rectangle stencil = new Rectangle(
                tb.getMinX(), tb.getMinY(), tb.getWidth(), tb.getHeight()
            );

        Shape intersection = Shape.intersect(text, stencil);

        Bounds ib = intersection.getBoundsInLocal();
        return ib.getWidth(); // kludge Math.ceil
    }

    /**
     * Terminates the program.
     */
    public void close() {
        Platform.runLater(()-> {
                Platform.exit();
                System.exit(0);
            });
    }

    // ***************************
    // *** PUBLIC GRAPHICS METHODS
    // ***************************

    /**
     * Clears the console to the background color
     */
    public void clear() {
        Platform.runLater(() -> {
                g.setFill(backgroundColor);
                g.fillRect(0, 0, width + MARGIN * 2, height + MARGIN * 2);
            });
        setCursor(0, 0);
    }

    /**
     * Clears a rectangle to the background color
     *
     * @param x      left coordinate in pixels
     * @param y      top coordinate in pixels
     * @param width  width of rectangle
     * @param height height of rectangle
     */
    public void clearRect(double x, double y, double width, double height) {
        Platform.runLater(() -> {
                g.setFill(backgroundColor);
                g.fillRect(x, y, width, height);
            });
    }

    /**
     * Sets the color to use for the fill commands as well as print/println.
     *
     * @param color Any legal CSS color string (color name, hex color, rgb,
     *              rgba, etc.)(color
     */
    public void setFill(String color) {
        Platform.runLater(() -> {
                fillColor = Color.web(color);
            });
    }

    public void setFill(int r, int g, int b) {
        Platform.runLater(() -> {
                fillColor = Color.rgb(r, g, b);
            });
    }

    public void setFill(int r, int g, int b, double a) {
        Platform.runLater(() -> {
                fillColor = Color.rgb(r, g, b, a);
            });
    }

    /**
     * Sets the color to use for the stroke commands
     *
     * @param color Any legal CSS color string (color name, hex color, rgb,
     *              rgba, etc.)(color
     */
    public void setStroke(String color) {
        Platform.runLater(() -> {
                strokeColor = Color.web(color);
            });
    }

    public void setStroke(int r, int g, int b) {
        Platform.runLater(() -> {
                strokeColor = Color.rgb(r, g, b);
            });
    }

    public void setStroke(int r, int g, int b, double a) {
        Platform.runLater(() -> {
                strokeColor = Color.rgb(r, g, b, a);
            });
    }

    /**
     * Sets the color to use for the stroke commands
     *
     * @param color Any legal CSS color string (color name, hex color, rgb,
     *              rgba, etc.)(color
     */
    public void setBackground(String color) {
        Platform.runLater(() -> {
                backgroundColor = Color.web(color);
            });
    }

    public void setBackground(int r, int g, int b) {
        Platform.runLater(() -> {
                backgroundColor = Color.rgb(r, g, b);
            });
    }

    public void setBackground(int r, int g, int b, double a) {
        Platform.runLater(() -> {
                backgroundColor = Color.rgb(r, g, b, a);
            });
    }

    /**
     * Sets the line width for stroke commands
     *
     * @param width A line width in pixels
     */
    public void setLineWidth(int width) {
        Platform.runLater(() -> {
                lineWidth = width;
            });
    }

    /**
     * Draws a filled rectangle
     *
     * @param x      left coordinate in pixels
     * @param y      top coordinate in pixels
     * @param width  width of rectangle
     * @param height height of rectangle
     */
    public void fillRect(double x, double y, double width, double height) {
        Platform.runLater(() -> {
                g.setFill(fillColor);
                g.setLineWidth(lineWidth);
                g.fillRect(x, y, width, height);
            });
    }

    /**
     * Draws a rectangle outline
     *
     * @param x      left coordinate in pixels
     * @param y      top coordinate in pixels
     * @param width  width of rectangle
     * @param height height of rectangle
     */
    public void strokeRect(double x, double y, double width, double height) {
        Platform.runLater(() -> {
                g.setStroke(strokeColor);
                g.setLineWidth(lineWidth);
                g.strokeRect(x, y, width, height);
            });
    }

    /**
     * Draws a filled oval, inscribed inside the rectangle defined by the
     * arguments provided.
     *
     * @param x      left coordinate in pixels
     * @param y      top coordinate in pixels
     * @param width  width of rectangle
     * @param height height of rectangle
     */
    public void fillOval(double x, double y, double width, double height) {
        Platform.runLater(() -> {
                g.setFill(fillColor);
                g.fillOval(x, y, width, height);
            });
    }

    /**
     * Draws an outline of an oval, inscribed inside the rectangle defined by
     * the arguments provided.
     *
     * @param x      left coordinate in pixels
     * @param y      top coordinate in pixels
     * @param width  width of rectangle
     * @param height height of rectangle
     */
    public void strokeOval(double x, double y, double width, double height) {
        Platform.runLater(() -> {
                g.setStroke(strokeColor);
                g.setLineWidth(lineWidth);
                g.strokeOval(x, y, width, height);
            });
    }

    /**
     * Draws a line.
     *
     * @param x1 X coordinate of first endpoint
     * @param y1 Y coordinate of first endpoint
     * @param x2 X coordinate of second endpoint
     * @param y2 Y coordinate of second endpoint
     */
    public void strokeLine(double x1, double y1, double x2, double y2) {
        Platform.runLater(() -> {
                g.setStroke(strokeColor);
                g.setLineWidth(lineWidth);
                g.strokeLine(x1, y1, x2, y2);
            });
    }

    /**
     * Draws a polygon outline.
     *
     * @param xs  An array of x coordinates for each vertex
     * @param ys  An array of y coordinates for each vertex
     * @param num The number of vertices in the polygon
     */
    public void strokePolygon(double[] xs, double[] ys, int num) {
        Platform.runLater(() -> {
                g.setStroke(strokeColor);
                g.setLineWidth(lineWidth);
                g.strokePolygon(xs, ys, num);
            });
    }

    /**
     * Draws a filled polygon.
     *
     * @param xs  An array of x coordinates for each vertex
     * @param ys  An array of y coordinates for each vertex
     * @param num The number of vertices in the polygon
     */
    public void fillPolygon(double[] xs, double[] ys, int num) {
        Platform.runLater(() -> {
                g.setFill(fillColor);
                g.fillPolygon(xs, ys, num);
            });
    }

    /**
     * Draws an outline of a portion of an oval, inscribed inside the rectangle
     * defined by the arguments provided.
     *
     * @param x          left coordinate in pixels
     * @param y          top coordinate in pixels
     * @param width      width of rectangle
     * @param height     height of rectangle
     * @param startAngle angle in degrees to start drawing it (0 = east, 90 =
     *                   north, 180 = west, 270 = south, etc.)
     * @param arcAngle   number of degrees of ark to draw (counter-clockwise)
     */
    public void strokeArc(double x, double y, double width, double height, double startAngle, double arcAngle) {
        Platform.runLater(() -> {
                g.setStroke(strokeColor);
                g.setLineWidth(lineWidth);
                g.strokeArc(x, y, width, height, startAngle, arcAngle, ArcType.ROUND);
            });
    }

    /**
     * Draws an filled in portion of an oval, inscribed inside the rectangle
     * defined by the arguments provided.
     *
     * @param x          left coordinate in pixels
     * @param y          top coordinate in pixels
     * @param width      width of rectangle
     * @param height     height of rectangle
     * @param startAngle angle in degrees to start drawing it (0 = east, 90 =
     *                   north, 180 = west, 270 = south, etc.)
     * @param arcAngle   number of degrees of ark to draw (counter-clockwise)
     */
    public void fillArc(double x, double y, double width, double height, double startAngle, double arcAngle) {
        Platform.runLater(() -> {
                g.setFill(fillColor);
                g.fillArc(x, y, width, height, startAngle, arcAngle, ArcType.ROUND);
            });
    }

    /**
     * Draws the outline of a rectangle with rounded corners.
     *
     * @param x       left coordinate in pixels
     * @param y       top coordinate in pixels
     * @param width   width of rectangle
     * @param height  height of rectangle
     * @param xRadius The width of a rounded corner
     * @param yRadius The height of a rounded corner
     */
    public void strokeRoundRect(double x, double y, double width, double height, double xRadius, double yRadius) {
        Platform.runLater(() -> {
                g.setStroke(strokeColor);
                g.setLineWidth(lineWidth);
                g.strokeRoundRect(x, y, width, height, xRadius, yRadius);
            });
    }

    /**
     * Draws a filled rectangle with rounded corners.
     *
     * @param x       left coordinate in pixels
     * @param y       top coordinate in pixels
     * @param width   width of rectangle
     * @param height  height of rectangle
     * @param xRadius The width of a rounded corner
     * @param yRadius The height of a rounded corner
     */
    public void fillRoundRect(double x, double y, double width, double height, double xRadius, double yRadius) {
        Platform.runLater(() -> {
                g.setFill(fillColor);
                g.fillRoundRect(x, y, width, height, xRadius, yRadius);
            });
    }

    /**
     * Draws text on the console.
     *
     * @param str The text to draw
     * @param x   The x location of the left edge of the text
     * @param y   The y location of the bottom of the text
     */
    public void fillText(String str, double x, double y) {
        Platform.runLater(() -> {
                g.setFill(fillColor);
                g.setFont(drawStringFont);
                g.fillText(str, x, y);
            });
    }

    /**
     * Draws outlined text on the console.
     *
     * @param str The text to draw
     * @param x   The x location of the left edge of the text
     * @param y   The y location of the bottom of the text
     */
    public void strokeText(String str, double x, double y) {
        Platform.runLater(() -> {
                g.setStroke(strokeColor);
                g.setLineWidth(lineWidth);
                g.setFont(drawStringFont);
                g.strokeText(str, x, y);
            });
    }

    /**
     * Sets the font to use for fillText and strokeText.
     *
     * @param fname A legal font name
     * @param fsize The size of the font in pixels
     */
    public void setFont(String fname, double fsize) {
        Platform.runLater(() -> {
                drawStringFont = new Font(fname, fsize);
            });
    }

    /**
     * Draws an image on the console.
     *
     * @param img The Image object to draw
     * @param x   The location of the left side
     * @param y   The location of the top of the image
     */
    public void drawImage(Image img, double x, double y) {
        Platform.runLater(() -> {
                g.drawImage(img, x, y);
            });
    }

    /**
     * Draws an image on the console, stretched to a given height and width.
     *
     * @param img    The Image object to draw
     * @param x      The location of the left side
     * @param y      The location of the top of the image
     * @param width  The width of the image in pixels
     * @param height The height of the image in pixels
     */
    public void drawImage(Image img, double x, double y, double width, double height) {
        Platform.runLater(() -> {
                g.drawImage(img, x, y, width, height);
            });
    }

    /**
     * Draws a star outline on the console from (x, y) to (x + width, y +
     * width). Adapted from hsa.
     *
     * @param x      The x coordinate of the top left corner of the rectangle that
     *               the star is inscribed in.
     * @param y      The y coordinate of the top left corner of the rectangle that
     *               the star is inscribed in.
     * @param width  The width of the rectangle that the star is inscribed in.
     * @param height The height of the rectangle that the star is inscribed in.
     */
    public void strokeStar(double x, double y, double width, double height) {
        double[] xPoints, yPoints;
        double rx, ry, xc, yc;

        rx = width;
        ry = height;
        xc = x + rx / 2;
        yc = y + height;

        xPoints = new double[11];
        yPoints = new double[11];
        xPoints[0] = xc;
        yPoints[0] = (yc - ry);
        xPoints[1] = (xc + rx * 0.118034);
        yPoints[1] = (yc - ry * 0.618560);
        xPoints[2] = (xc + rx * 0.500000);
        yPoints[2] = yPoints[1];
        xPoints[3] = (xc + rx * 0.190983);
        yPoints[3] = (yc - ry * 0.381759);
        xPoints[4] = (xc + rx * 0.309017);
        yPoints[4] = yc;
        xPoints[5] = xc;
        yPoints[5] = (yc - ry * 0.236068);
        xPoints[6] = (xc - rx * 0.309017);
        yPoints[6] = yPoints[4];
        xPoints[7] = (xc - rx * 0.190983);
        yPoints[7] = yPoints[3];
        xPoints[8] = (xc - rx * 0.500000);
        yPoints[8] = yPoints[2];
        xPoints[9] = (xc - rx * 0.118034);
        yPoints[9] = yPoints[1];
        xPoints[10] = xPoints[0];
        yPoints[10] = yPoints[0];
        strokePolygon(xPoints, yPoints, 11);
    }

    /**
     * Draws a filled star on the console from (x, y) to (x + width, y + width).
     * Adapted from hsa.
     *
     * @param x      The x coordinate of the top left corner of the rectangle that
     *               the star is inscribed in.
     * @param y      The y coordinate of the top left corner of the rectangle that
     *               the star is inscribed in.
     * @param width  The width of the rectangle that the star is inscribed in.
     * @param height The height of the rectangle that the star is inscribed in.
     */
    public void fillStar(double x, double y, double width, double height) {
        double[] xPoints, yPoints;
        double rx, ry, xc, yc;

        rx = width;
        ry = height;
        xc = x + rx / 2;
        yc = y + height;

        xPoints = new double[11];
        yPoints = new double[11];
        xPoints[0] = xc;
        yPoints[0] = (yc - ry);
        xPoints[1] = (xc + rx * 0.118034);
        yPoints[1] = (yc - ry * 0.618560);
        xPoints[2] = (xc + rx * 0.500000);
        yPoints[2] = yPoints[1];
        xPoints[3] = (xc + rx * 0.190983);
        yPoints[3] = (yc - ry * 0.381759);
        xPoints[4] = (xc + rx * 0.309017);
        yPoints[4] = yc;
        xPoints[5] = xc;
        yPoints[5] = (yc - ry * 0.236068);
        xPoints[6] = (xc - rx * 0.309017);
        yPoints[6] = yPoints[4];
        xPoints[7] = (xc - rx * 0.190983);
        yPoints[7] = yPoints[3];
        xPoints[8] = (xc - rx * 0.500000);
        yPoints[8] = yPoints[2];
        xPoints[9] = (xc - rx * 0.118034);
        yPoints[9] = yPoints[1];
        xPoints[10] = xPoints[0];
        yPoints[10] = yPoints[0];
        fillPolygon(xPoints, yPoints, 11);
    }

    /**
     * Draws a maple leaf outline on the console from (x, y) to (x + width, y +
     * width). Adapted from hsa.
     *
     * @param x      The x coordinate of the top left corner of the rectangle that
     *               the maple leaf is inscribed in.
     * @param y      The y coordinate of the top left corner of the rectangle that
     *               the maple leaf is inscribed in.
     * @param width  The width of the rectangle that the maple leaf is inscribed
     *               in.
     * @param height The height of the rectangle that the maple leaf is
     *               inscribed in.
     */
    public void strokeMapleLeaf(double x, double y, double width, double height) {
        double[] xPoints, yPoints;
        double rx, ry, xc, yc;

        rx = width;
        ry = height;
        xc = x + rx / 2;
        yc = y + height;

        xPoints = new double[26];
        yPoints = new double[26];
        xPoints[0] = (xc + rx * 0.021423);
        yPoints[0] = (yc - ry * 0.215686);
        xPoints[1] = (xc + rx * 0.270780);
        yPoints[1] = (yc - ry * 0.203804);
        xPoints[2] = (xc + rx * 0.271820);
        yPoints[2] = (yc - ry * 0.295752);
        xPoints[3] = (xc + rx * 0.482015);
        yPoints[3] = (yc - ry * 0.411765);
        xPoints[4] = (xc + rx * 0.443046);
        yPoints[4] = (yc - ry * 0.483267);
        xPoints[5] = (xc + rx * 0.500000);
        yPoints[5] = (yc - ry * 0.587435);
        xPoints[6] = (xc + rx * 0.363353);
        yPoints[6] = (yc - ry * 0.619576);
        xPoints[7] = (xc + rx * 0.342287);
        yPoints[7] = (yc - ry * 0.693849);
        xPoints[8] = (xc + rx * 0.153596);
        yPoints[8] = (yc - ry * 0.612537);
        xPoints[9] = (xc + rx * 0.201601);
        yPoints[9] = (yc - ry * 0.918462);
        xPoints[10] = (xc + rx * 0.093001);
        yPoints[10] = (yc - ry * 0.894514);
        xPoints[11] = xc;
        yPoints[11] = (yc - ry);
        xPoints[12] = (xc - rx * 0.093001);
        yPoints[12] = yPoints[10];
        xPoints[13] = (xc - rx * 0.201601);
        yPoints[13] = yPoints[9];
        xPoints[14] = (xc - rx * 0.153596);
        yPoints[14] = yPoints[8];
        xPoints[15] = (xc - rx * 0.342287);
        yPoints[15] = yPoints[7];
        xPoints[16] = (xc - rx * 0.363353);
        yPoints[16] = yPoints[6];
        xPoints[17] = (xc - rx * 0.500000);
        yPoints[17] = yPoints[5];
        xPoints[18] = (xc - rx * 0.443046);
        yPoints[18] = yPoints[4];
        xPoints[19] = (xc - rx * 0.482015);
        yPoints[19] = yPoints[3];
        xPoints[20] = (xc - rx * 0.271820);
        yPoints[20] = yPoints[2];
        xPoints[21] = (xc - rx * .2707796);
        yPoints[21] = yPoints[1];
        xPoints[22] = (xc - rx * 0.021423);
        yPoints[22] = yPoints[0];
        xPoints[23] = xPoints[22];
        yPoints[23] = yc;
        xPoints[24] = xPoints[0];
        yPoints[24] = yPoints[23];
        xPoints[25] = xPoints[0];
        yPoints[25] = yPoints[0];
        strokePolygon(xPoints, yPoints, 26);
    }

    /**
     * Draws a filled maple leaf on the console from (x, y) to (x + width, y +
     * width). Adapted from hsa.
     *
     * @param x      int The x coordinate of the top left corner of the rectangle
     *               that the maple leaf is inscribed in.
     * @param y      int The y coordinate of the top left corner of the rectangle
     *               that the maple leaf is inscribed in.
     * @param width  int The width of the rectangle that the maple leaf is
     *               inscribed in.
     * @param height int The height of the rectangle that the maple leaf is
     *               inscribed in.
     */
    public void fillMapleLeaf(double x, double y, double width, double height) {
        double[] xPoints, yPoints;
        double rx, ry, xc, yc;

        rx = width;
        ry = height;
        xc = x + rx / 2;
        yc = y + height;

        xPoints = new double[26];
        yPoints = new double[26];
        xPoints[0] = (xc + rx * 0.021423);
        yPoints[0] = (yc - ry * 0.215686);
        xPoints[1] = (xc + rx * 0.270780);
        yPoints[1] = (yc - ry * 0.203804);
        xPoints[2] = (xc + rx * 0.271820);
        yPoints[2] = (yc - ry * 0.295752);
        xPoints[3] = (xc + rx * 0.482015);
        yPoints[3] = (yc - ry * 0.411765);
        xPoints[4] = (xc + rx * 0.443046);
        yPoints[4] = (yc - ry * 0.483267);
        xPoints[5] = (xc + rx * 0.500000);
        yPoints[5] = (yc - ry * 0.587435);
        xPoints[6] = (xc + rx * 0.363353);
        yPoints[6] = (yc - ry * 0.619576);
        xPoints[7] = (xc + rx * 0.342287);
        yPoints[7] = (yc - ry * 0.693849);
        xPoints[8] = (xc + rx * 0.153596);
        yPoints[8] = (yc - ry * 0.612537);
        xPoints[9] = (xc + rx * 0.201601);
        yPoints[9] = (yc - ry * 0.918462);
        xPoints[10] = (xc + rx * 0.093001);
        yPoints[10] = (yc - ry * 0.894514);
        xPoints[11] = xc;
        yPoints[11] = (yc - ry);
        xPoints[12] = (xc - rx * 0.093001);
        yPoints[12] = yPoints[10];
        xPoints[13] = (xc - rx * 0.201601);
        yPoints[13] = yPoints[9];
        xPoints[14] = (xc - rx * 0.153596);
        yPoints[14] = yPoints[8];
        xPoints[15] = (xc - rx * 0.342287);
        yPoints[15] = yPoints[7];
        xPoints[16] = (xc - rx * 0.363353);
        yPoints[16] = yPoints[6];
        xPoints[17] = (xc - rx * 0.500000);
        yPoints[17] = yPoints[5];
        xPoints[18] = (xc - rx * 0.443046);
        yPoints[18] = yPoints[4];
        xPoints[19] = (xc - rx * 0.482015);
        yPoints[19] = yPoints[3];
        xPoints[20] = (xc - rx * 0.271820);
        yPoints[20] = yPoints[2];
        xPoints[21] = (xc - rx * .2707796);
        yPoints[21] = yPoints[1];
        xPoints[22] = (xc - rx * 0.021423);
        yPoints[22] = yPoints[0];
        xPoints[23] = xPoints[22];
        yPoints[23] = yc;
        xPoints[24] = xPoints[0];
        yPoints[24] = yPoints[23];
        xPoints[25] = xPoints[0];
        yPoints[25] = yPoints[0];
        fillPolygon(xPoints, yPoints, 26);
    }

    // ***********************
    // *** PUBLIC TEXT METHODS
    // ***********************

    /**
     * Set the row and column for print/println.
     *
     * @param row The row number (0 = top)
     * @param col The column number (0 = left)
     */
    public void setCursor(int row, int col) {
        currentRow = row;
        currentCol = col;
        actualRow = row;
        actualCol = col;
        setCursorPos(row, col);
    }

    /**
     * Retrieve the current column for print/println.
     *
     * @return The current column
     */
    public int getColumn() {
        return cursorCol;
    }

    /**
     * Retrieve the current row for print/println.
     *
     * @return The current row
     */
    public int getRow() {
        return cursorRow;
    }

    /**
     * Get the total number of columns on the console for print/println.
     *
     * @return The number of columns available
     */
    public int getNumColumns() {
        return maxCol + 1;
    }

    /**
     * Get the total number of rows on the console for print/println.
     *
     * @return The number of rows available
     */
    public int getNumRows() {
        return maxRow + 1;
    }

    /**
     * Write a string to the Console. Adapted from hsa.
     *
     * @param text The string to be written to the Console
     */
    public void print(String text) {
        // Convert the printing of null to a printable string.
        if (text == null) {
            text = "<null>";
        }

        int index;
        int len = text.length();
        int start = 0;

        while (true) {
            index = start;
            if (index == len) {
                setCursorPos(actualRow, actualCol);
                return;
            }

            while ((index < len) && (text.charAt(index) != '\n')
            && (text.charAt(index) != '\t')
            && (text.charAt(index) != '\r')
            && (index - start < maxCol - currentCol)) {
                index++;
            }
            if (start != index) {
                // Draw what we have so far
                drawText(currentRow, currentCol, text.substring(start, index));
                currentCol += index - start;
                actualCol = currentCol;
            }
            if (index == len) {
                setCursorPos(actualRow, actualCol);
                return;
            }
            if (text.charAt(index) == '\n' && text.charAt(index) != '\t') {
                if ((currentRow <= maxRow) && (currentCol <= maxCol)) {
                    clearToEOL(currentRow, currentCol);
                }
                if (currentRow < maxRow) {
                    currentCol = 0;
                    currentRow++;
                    actualCol = currentCol;
                    actualRow = currentRow;
                } else {
                    scrollUpALine();
                    startRow--;
                    currentCol = 0;
                    actualCol = currentCol;
                }
            } else if (text.charAt(index) == '\t') {
                int numSpaces = TAB_SIZE - ((currentCol) % TAB_SIZE);
                // If the next tab position is off the end of the screen,
                // scroll down a line and place the cursor at the beginning
                // of the line.
                if (currentCol + numSpaces > maxCol) {
                    print("\n");
                } else {
                    print("        ".substring(0, numSpaces));
                }
            } else if (currentCol <= maxCol) {
                drawText(currentRow, currentCol, text.substring(index, index + 1));
                if (currentCol < maxCol) {
                    currentCol++;
                    actualCol = currentCol;
                } else if (currentRow < maxRow) {
                    currentCol = 0; // converted from ++ by sam
                    actualCol = 0;
                    actualRow++;
                    currentRow++; // added by sam
                } else {
                    currentCol++;
                }
            } else {
                if (currentRow < maxRow) {
                    currentRow++;
                } else {
                    scrollUpALine();
                    startRow--;
                }
                //FIX for obscure bug in that if you try to print a single character off-screen to the right, the character gets printed twice on the following line instead.
                //drawText (currentRow, 1, text.substring (index, index + 1));
                drawText(currentRow, 0, text.substring(index, index + 1));
                currentCol = 0;
                actualCol = currentCol;
                actualRow = currentRow;
                index--; //kludge
            }
            start = index + 1;
        }
    }

    /**
     * Writes a newline to the GraphicsConsole. Adapted from hsa.
     */
    public void println() {
        print("\n");
    }

    /**
     * Writes the text representation of an 8-bit integer (a "byte") to the
     * GraphicsConsole followed by a newline. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void println(byte number) {
        print(number);
        print("\n");
    }

    /**
     * Writes the text representation of an 8-bit integer (a "byte") to the
     * GraphicsConsole with a specified field size followed by a newline.
     * Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void println(byte number, int fieldSize) {
        print(number, fieldSize);
        print("\n");
    }

    /**
     * Writes a character to the GraphicsConsole followed by a newline. Adapted
     * from hsa.
     *
     * @param ch The character to be written to the GraphicsConsole.
     */
    public void println(char ch) {
        print(ch);
        print("\n");
    }

    /**
     * Writes a character to the GraphicsConsole with a specified field size.
     * Adapted from hsa.
     *
     * @param ch        The character to be written to the GraphicsConsole.
     * @param fieldSize The field width that the character is to be written in.
     */
    public void println(char ch, int fieldSize) {
        print(ch, fieldSize);
        print("\n");
    }

    /**
     * Writes a double precision floating point number (a "double") to the
     * GraphicsConsole followed by a newline. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void println(double number) {
        print(number);
        print("\n");
    }

    /**
     * Writes a double precision floating point number (a "double") to the
     * GraphicsConsole with a specified field size followed by a newline.
     * Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void println(double number, int fieldSize) {
        print(number, fieldSize);
        print("\n");
    }

    /**
     * Writes a double precision floating point number (a "double") to the
     * GraphicsConsole with a specified field size and a specified number of
     * decimal places followed by a newline. Adapted from hsa.
     *
     * @param number        The number to be written to the GraphicsConsole.
     * @param fieldSize     The field width that the number is to be written in.
     * @param decimalPlaces The number of decimal places of the number to be
     *                      displayed.
     */
    public void println(double number, int fieldSize, int decimalPlaces) {
        print(number, fieldSize, decimalPlaces);
        print("\n");
    }

    /**
     * Writes a floating point number (a "float") to the GraphicsConsole
     * followed by a newline. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void println(float number) {
        print(number);
        print("\n");
    }

    /**
     * Writes a floating point number (a "float") to the GraphicsConsole with a
     * specified field size followed by a newline. Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void println(float number, int fieldSize) {
        print(number, fieldSize);
        print("\n");
    }

    /**
     * Writes a floating point number (a "double") to the GraphicsConsole with a
     * specified field size and a specified number of decimal places followed by
     * a newline. Adapted from hsa.
     *
     * @param number        The number to be written to the GraphicsConsole.
     * @param fieldSize     The field width that the number is to be written in.
     * @param decimalPlaces The number of decimal places of the number to be
     *                      displayed.
     */
    public void println(float number, int fieldSize, int decimalPlaces) {
        print(number, fieldSize, decimalPlaces);
        print("\n");
    }

    /**
     * Writes the text representation of an 32-bit integer (an "int") to the
     * GraphicsConsole followed by a newline. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void println(int number) {
        print(number);
        print("\n");
    }

    /**
     * Writes the text representation of an 32-bit integer (an "int") to the
     * GraphicsConsole with a specified field size followed by a newline.
     * Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void println(int number, int fieldSize) {
        print(number, fieldSize);
        print("\n");
    }

    /**
     * Writes the text representation of an 64-bit integer (a "long") to the
     * GraphicsConsole followed by a newline. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void println(long number) {
        print(number);
        print("\n");
    }

    /**
     * Writes the text representation of an 8-bit integer (a "byte") to the
     * GraphicsConsole. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void print(byte number) {
        print((int) number);
    }

    /**
     * Writes the text representation of an 8-bit integer (a "byte") to the
     * GraphicsConsole with a specified field size. Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void print(byte number, int fieldSize) {
        print((int) number, fieldSize);
    }

    /**
     * Writes a character to the GraphicsConsole. Adapted from hsa.
     *
     * @param ch The character to be written to the GraphicsConsole.
     */
    public void print(char ch) {
        print(String.valueOf(ch));
    }

    /**
     * Writes a character to the GraphicsConsole with a specified field size.
     * Adapted from hsa.
     *
     * @param ch        The character to be written to the GraphicsConsole.
     * @param fieldSize The field width that the character is to be written in.
     */
    public void print(char ch, int fieldSize) {
        String charStr = String.valueOf(ch);
        StringBuffer padding = new StringBuffer();

        for (int cnt = 0; cnt < fieldSize - charStr.length(); cnt++) {
            padding.append(' ');
        }
        print(charStr + padding);
    }

    /**
     * Writes a double precision floating point number (a "double") to the
     * GraphicsConsole. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void print(double number) {
        print(String.valueOf(number));
    }

    /**
     * Writes a double precision floating point number (a "double") to the
     * GraphicsConsole with a specified field size. Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void print(double number, int fieldSize) {
        double posValue = Math.abs(number);
        int placesRemaining = fieldSize;
        String format = null, numStr;
        StringBuffer padding = new StringBuffer();

        if (number < 0) {
            placesRemaining--;                 // Space for the minus sign
        }
        if (posValue < 10.0) {
            format = "0";
        } else if (posValue < 100.0) {
            format = "00";
        } else if (posValue < 1000.0) {
            format = "000";
        } else if (posValue < 10000.0) {
            format = "0000";
        } else if (posValue < 100000.0) {
            format = "00000";
        } else if (posValue < 1000000.0) {
            format = "000000";
        } else if (posValue < 10000000.0) {
            format = "0000000";
        } else if (posValue < 100000000.0) {
            format = "00000000";
        }

        if (format == null) {
            // We're using scientific notation
            numStr = String.valueOf(number);
        } else {
            // Add a decimal point, if there's room
            placesRemaining -= format.length();
            if (placesRemaining > 0) {
                format = format + ".";
                placesRemaining--;
            }

            // For any addition room, add decimal places
            for (int cnt = 0; cnt < placesRemaining; cnt++) {
                format = format + "#";
            }

            // Convert the number
            NumberFormat form = new DecimalFormat(format);
            numStr = form.format(number);
        }

        // If the number is not long enough, pad with spaces
        for (int cnt = 0; cnt < fieldSize - numStr.length(); cnt++) {
            padding.append(' ');
        }
        print(padding + numStr);
    }

    /**
     * Writes a double precision floating point number (a "double") to the
     * GraphicsConsole with a specified field size and a specified number of
     * decimal places. Adapted from hsa.
     *
     * @param number        The number to be written to the GraphicsConsole.
     * @param fieldSize     The field width that the number is to be written in.
     * @param decimalPlaces The number of decimal places of the number to be
     *                      displayed.
     */
    public void print(double number, int fieldSize, int decimalPlaces) {
        double posValue = Math.abs(number);
        //int placesRemaining = fieldSize;
        String format, numStr;
        StringBuffer padding = new StringBuffer();

        if (Math.abs(number) >= 100000000.0) {
            // We're using scientific notation
            numStr = String.valueOf(number);
        } else {
            format = "0.";

            // For any addition room, add decimal places
            for (int cnt = 0; cnt < decimalPlaces; cnt++) {
                format = format + "0";
            }

            // Convert the number
            NumberFormat form = new DecimalFormat(format);
            form.setMinimumIntegerDigits(1);
            numStr = form.format(number);
        }

        // If the number is not long enough, pad with spaces
        for (int cnt = 0; cnt < fieldSize - numStr.length(); cnt++) {
            padding.append(' ');
        }
        print(padding + numStr);
    }

    /**
     * Writes a floating point number (a "float") to the GraphicsConsole.
     * Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void print(float number) {
        print(String.valueOf(number));
    }

    /**
     * Writes a floating point number (a "float") to the GraphicsConsole with a
     * specified field size.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void print(float number, int fieldSize) {
        print((double) number, fieldSize);
    }

    /**
     * Writes a floating point number (a "double") to the GraphicsConsole with a
     * specified field size and a specified number of decimal places. Adapted
     * from hsa.
     *
     * @param number        The number to be written to the GraphicsConsole.
     * @param fieldSize     The field width that the number is to be written in.
     * @param decimalPlaces The number of decimal places of the number to be
     *                      displayed.
     */
    public void print(float number, int fieldSize, int decimalPlaces) {
        print((double) number, fieldSize, decimalPlaces);
    }

    /**
     * Writes the text representation of an 32-bit integer (an "int") to the
     * GraphicsConsole. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void print(int number) {
        print(String.valueOf(number));
    }

    /**
     * Writes the text representation of an 32-bit integer (an "int") to the
     * GraphicsConsole with a specified field size. Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void print(int number, int fieldSize) {
        String numStr = String.valueOf(number);
        StringBuffer padding = new StringBuffer();

        for (int cnt = 0; cnt < fieldSize - numStr.length(); cnt++) {
            padding.append(' ');
        }
        print(padding + numStr);
    }

    /**
     * Writes the text representation of an 64-bit integer (a "long") to the
     * GraphicsConsole. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void print(long number) {
        print(String.valueOf(number));
    }

    /**
     * Writes the text representation of an 64-bit integer (a "long") to the
     * GraphicsConsole with a specified field size. Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void print(long number, int fieldSize) {
        String numStr = String.valueOf(number);
        StringBuffer padding = new StringBuffer();

        for (int cnt = 0; cnt < fieldSize - numStr.length(); cnt++) {
            padding.append(' ');
        }
        print(padding + numStr);
    }

    /**
     * Writes a string to the GraphicsConsole with a specified field size.
     * Adapted from hsa.
     *
     * @param text      The string to be written to the GraphicsConsole.
     * @param fieldSize The field width that the string is to be written in.
     */
    public void print(String text, int fieldSize) {
        StringBuffer padding = new StringBuffer();

        for (int cnt = 0; cnt < fieldSize - text.length(); cnt++) {
            padding.append(' ');
        }
        print(text + padding);
    }

    /**
     * Writes the text representation of an 16-bit integer (a "short") to the
     * GraphicsConsole. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void print(short number) {
        print((int) number);
    }

    /**
     * Writes the text representation of an 16-bit integer (a "short") to the
     * GraphicsConsole with a specified field size. Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void print(short number, int fieldSize) {
        print((int) number, fieldSize);
    }

    /**
     * Writes the text representation of a boolean to the GraphicsConsole.
     * Adapted from hsa.
     *
     * @param value The boolean to be written to the GraphicsConsole.
     */
    public void print(boolean value) {
        print(String.valueOf(value));
    }

    /**
     * Writes the text representation of a boolean to the GraphicsConsole with a
     * specified field size. Adapted from hsa.
     *
     * @param value     The boolean to be written to the GraphicsConsole.
     * @param fieldSize The field width that the boolean is to be written in.
     */
    public void print(boolean value, int fieldSize) {
        String boolStr = String.valueOf(value);
        StringBuffer padding = new StringBuffer();

        for (int cnt = 0; cnt < fieldSize - boolStr.length(); cnt++) {
            padding.append(' ');
        }
        print(boolStr + padding);
    }

    /**
     * Writes the text representation of an 64-bit integer (a "long") to the
     * GraphicsConsole with a specified field size followed by a newline.
     * Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void println(long number, int fieldSize) {
        print(number, fieldSize);
        print("\n");
    }

    /**
     * Writes a string to the GraphicsConsole followed by a newline. Adapted
     * from hsa.
     *
     * @param text The string to be written to the GraphicsConsole.
     */
    public void println(String text) {
        print(text);
        print("\n");
    }

    /**
     * Writes a string to the GraphicsConsole with a specified field size
     * followed by a newline. Adapted from hsa.
     *
     * @param text      The string to be written to the GraphicsConsole.
     * @param fieldSize The field width that the string is to be written in.
     */
    public void println(String text, int fieldSize) {
        print(text, fieldSize);
        print("\n");
    }

    /**
     * Writes the text representation of an 16-bit integer (a "short") to the
     * GraphicsConsole followed by a newline. Adapted from hsa.
     *
     * @param number The number to be written to the GraphicsConsole.
     */
    public void println(short number) {
        print(number);
        print("\n");
    }

    /**
     * Writes the text representation of an 16-bit integer (a "short") to the
     * GraphicsConsole with a specified field size followed by a newline.
     * Adapted from hsa.
     *
     * @param number    The number to be written to the GraphicsConsole.
     * @param fieldSize The field width that the number is to be written in.
     */
    public void println(short number, int fieldSize) {
        print(number, fieldSize);
        print("\n");
    }

    /**
     * Writes the text representation of a boolean to the GraphicsConsole
     * followed by a newline. Adapted from hsa.
     *
     * @param value The boolean to be written to the GraphicsConsole.
     */
    public void println(boolean value) {
        print(value);
        print("\n");
    }

    /**
     * Writes the text representation of a boolean to the GraphicsConsole with a
     * specified field size followed by a newline. Adapted from hsa.
     *
     * @param value     The boolean to be written to the GraphicsConsole.
     * @param fieldSize The field width that the boolean is to be written in.
     */
    public void println(boolean value, int fieldSize) {
        print(value, fieldSize);
        print("\n");
    }

    public void println(Object o) {
        print(o.toString());
        print("\n");
    }

    public void print(Object o) {
        print(o.toString());
    }
    
    // ************************
    // *** PUBLIC INPUT METHODS
    // ************************

    /**
     * Pauses to read a character from the GraphicsConsole without showing the
     * cursor.
     *
     * @return the character read.
     */
    public char getChar() {
        return getChar(false);
    }

    /**
     * Pauses to read the next character entered on the keyboard. Ignores
     * characters currently in the line buffer.
     *
     * @param cursor T/F to indicate whether the cursor is displayed.
     * @return The next character entered on the keyboard.
     */
    public synchronized char getChar(boolean cursor) {
        while (kbdBufferHead == kbdBufferTail) {
            try {
                Platform.runLater(() -> {
                        container.setTitle(title + " - Waiting for input");
                    });

                if (cursor) {
                    cursorOn();
                } else {
                    cursorOff();
                }
                wait();
                if (cursor) {
                    cursorOff();
                }
                Platform.runLater(() -> {
                        container.setTitle(title + " - Running");
                    });

            } catch (InterruptedException e) {
            }
        }

        char ch = kbdBuffer[kbdBufferTail];
        kbdBufferTail = (kbdBufferTail + 1) % kbdBuffer.length;

        return ch;
    }

    /**
     * Reads a single character from the Console. Note that this discards any
     * whitespace. If you want to get every character on the line, use the
     * nextLine() method.
     *
     * @return The character read from the Console
     */
    public synchronized char nextChar() {
        char result, ch;

        if (ungotChar != EMPTY_BUFFER) {
            result = (char) ungotChar;
            ungotChar = EMPTY_BUFFER;
            return (result);
        }

        if (lineBufferHead != lineBufferTail) {
            result = lineBuffer[lineBufferTail];
            lineBufferTail = (lineBufferTail + 1) % lineBuffer.length;
            return (result);
        }

        startRow = currentRow;
        startCol = currentCol;
        if (currentRow > maxRow) {
            startRow++;
            currentCol = 0;
        }

        // Turn cursor on if necessary
        //cursorOn ();
        // Wait for a character to be entered
        while (true) {
            ch = getChar(true);
            if (ch == '\n' || ch == '\r') {
                //clearToEOL = false;
                if (echoOn) {
                    print("\n");
                }
                //clearToEOL = true;
                lineBuffer[lineBufferHead] = '\n';
                lineBufferHead = (lineBufferHead + 1) % lineBuffer.length;
                break;
            }
            switch (ch) {
                // if backspace
                case '\b':
                if (lineBufferHead == lineBufferTail) {
                    invertScreen();
                } else {
                    int chToErase;

                    lineBufferHead = (lineBufferHead + lineBuffer.length - 1)
                    % lineBuffer.length;
                    chToErase = lineBuffer[lineBufferHead];
                    if (echoOn) {
                        if (chToErase != '\t') {
                            erasePreviousChar();
                        } else {
                            int cnt;
                            eraseLineOfInput();
                            cnt = lineBufferTail;
                            while (cnt != lineBufferHead) {
                                print(lineBuffer[cnt]);
                                cnt = (cnt + 1) % lineBuffer.length;
                            }
                        }
                    }
                }
                break;
                case '\025':
                if (echoOn) {
                    eraseLineOfInput();
                }
                lineBufferHead = lineBufferTail;
                break;
                default:
                if (echoOn) {
                    print(ch);
                    //System.out.println(currentCol+" "+actualCol+" "+cursorCol);
                }
                lineBuffer[lineBufferHead] = ch;
                lineBufferHead = (lineBufferHead + 1) % lineBuffer.length;
                break;
            }
        }

        result = lineBuffer[lineBufferTail];
        lineBufferTail = (lineBufferTail + 1) % lineBuffer.length;

        // Turn cursor off if necessary
        //cursorOff ();
        return (result);
    }

    /**
     * Reads a boolean from the GraphicsConsole. The actual text in the
     * GraphicsConsole must be either "true" or "false" although case is
     * irrelevant.
     *
     * @return The boolean value read from the GraphicsConsole.
     */
    public boolean nextBoolean() {
        String s;

        s = next().toLowerCase();
        switch (s) {
            case "true":
            return (true);
            case "false":
            return (false);
            default:
            ErrorReporting.fatalError("Unable to convert \"" + s + "\" to a boolean");
        }
        return (false);
    }

    /**
     * Reads an 8-bit integer (a "byte") from the GraphicsConsole. The actual
     * text in the GraphicsConsole must be a number from -128 to 127.
     *
     * @return The byte value read from the GraphicsConsole.
     */
    public byte nextByte() {
        String s = next();

        try {
            return (Byte.parseByte(s));
        } catch (NumberFormatException e) {
            ErrorReporting.fatalError("Unable to convert \"" + s + "\" to a byte");
            // Never reaches here
        }
        return (0);
    }

    /**
     * Reads a double precision floating point number (a "double") from the
     * GraphicsConsole.
     *
     * @return The double value read from the GraphicsConsole.
     */
    public double nextDouble() {
        Double d;
        String s;

        s = next();
        try {
            d = Double.valueOf(s);
            //System.out.println(d);
            return d;
        } catch (NumberFormatException e) {
            ErrorReporting.fatalError("Unable to convert \"" + s + "\" to a double");
            // Never reaches here
        }
        return (0.0);
    }

    /**
     * Reads a floating point number (a "float") from the GraphicsConsole.
     *
     * @return The float value read from the GraphicsConsole.
     */
    public float nextFloat() {
        Float f;
        String s;

        s = next();
        try {
            f = Float.valueOf(s);
            return f;
        } catch (NumberFormatException e) {
            ErrorReporting.fatalError("Unable to convert \"" + s + "\" to a float");
            // Never reaches here
        }
        return ((float) 0.0);
    }

    /**
     * Reads a 32-bit integer (an "int") from the GraphicsConsole. The actual
     * text in the GraphicsConsole must be a number from -2147483648 to
     * 2147483647.
     *
     * @return The int value read from the GraphicsConsole.
     */
    public int nextInt() {
        String s = next();

        try {
            return (Integer.parseInt(s));
        } catch (NumberFormatException e) {
            ErrorReporting.fatalError("Unable to convert \"" + s + "\" to a int");
            // Never reaches here
        }
        return (0);
    }

    /**
     * Reads a full line of text from the GraphicsConsole.
     *
     * @return The line of text read from the GraphicsConsole.
     */
    public String nextLine() {
        char ch;                                // The character being read in
        String s = "";          // The string typed in

        // Skip whitespace up tio the first newline
        do {
            ch = nextChar();
        } while (ch == ' ');

        if (ch == '\n' || (ch == '\r')) {
            ch = nextChar();
        }

        while (ch != '\n' && ch != '\r') {
            s = s + ch;
            ch = nextChar();
        }

        return (s);
    }

    /**
     * Reads a 64-bit integer (a "long") from the GraphicsConsole.
     *
     * @return The long value read from the GraphicsConsole.
     */
    public long nextLong() {
        String s = next();                        // The token read in

        try {
            return (Long.parseLong(s));
        } catch (NumberFormatException e) {
            ErrorReporting.fatalError("Unable to convert \"" + s + "\" to a long");
            // Never reaches here
        }
        return (0);
    }

    /**
     * Reads a 16-bit integer (a "short") from the GraphicsConsole. The actual
     * text in the GraphicsConsole must be a number from -32768 to 32767.
     *
     * @return The short value read from the GraphicsConsole.
     */
    public short nextShort() {
        String s = next();

        try {
            return (Short.parseShort(s));
        } catch (NumberFormatException e) {
            ErrorReporting.fatalError("Unable to convert \"" + s + "\" to a short");
            // Never reaches here
        }
        return (0);
    }

    /**
     * Reads in input from the keyboard buffer until it hits a whitespace, which
     * indicates the end of a token.
     *
     * @return the string read.
     */
    public String next() {
        char ch;

        StringBuffer sb = new StringBuffer();

        // Skip white space
        do {
            ch = nextChar();
        } while ((ch == ' ') || (ch == '\n') || (ch == '\r') || (ch == '\t'));

        if (ch == '"') {
            // Read until close quote
            ch = nextChar();
            while (ch != '"') {
                sb.append(ch);
                ch = nextChar();
                if (ch == '\n' || (ch == '\r')) {
                    ErrorReporting.fatalError("No terminating quote for quoted string");
                    // Never reaches here.
                }
            }

            // Read the character following the close quote.
            ch = nextChar();
        } else {
            do {
                sb.append(ch);
                ch = nextChar();
            } while ((ch != ' ') && (ch != '\n') && (ch != '\r') && (ch != '\t'));
        }

        // Lastly, skip any whitespace until the end of line
        while ((ch == ' ') || (ch == '\t')) {
            ch = nextChar();
        }

        if (ch != '\n' && ch != '\r') {
            ungotChar = (int) ch;
        }

        return (new String(sb));
    }

    // *****************************
    // * PUBLIC KEY LISTENER INTERFACE
    // *****************************

    /**
     * Returns the code for the key currently held down.
     *
     * @return A String value representing the keycode
     */
    public synchronized String getKeyCode() {
        kbdBufferHead = kbdBufferTail;
        return currentKeyCode == null ? "null" : currentKeyCode.getName();
    }

    /**
     * Returns the char for the key currently held down.
     *
     * @return the char that was typed
     */
    public synchronized char getKeyChar() {
        kbdBufferHead = kbdBufferTail;
        return currentKeyChar;
    }

    /**
     * Returns the code for the last key pressed.
     *
     * @return A String value representing the keycode
     */
    public synchronized String getLastKeyCode() {
        kbdBufferHead = kbdBufferTail;
        return lastKeyCode == null ? "null" : lastKeyCode.getName();
    }

    /**
     * Returns the char for the last key that was pressed.
     *
     * @return the char that was typed
     */
    public synchronized char getLastKeyChar() {
        kbdBufferHead = kbdBufferTail;
        return lastKeyChar;
    }

    /**
     * Call this to find out whether or not a particular key is being held down.
     *
     * @param key The String keycode for the key you are interested in (the same
     *            one returned by getKeyCode())
     * @return True if the key is currently held down, false otherwise
     */
    public synchronized boolean isKeyDown(String key) {
        kbdBufferHead = kbdBufferTail;
        return keysDown.contains(KeyCode.getKeyCode(key));
    }

    /**
     * Call this to find out whether or not a particular character is being held
     * down.
     *
     * @param key The character code for the key
     * @return True if the key is currently held down, false otherwise
     */
    public synchronized boolean isKeyDown(char key) {
        kbdBufferHead = kbdBufferTail;
        return keysDown.contains(KeyCode.getKeyCode(("" + key).toUpperCase()));
    }

    // ************************
    // *** OTHER PUBLIC METHODS
    // ************************

    /**
     * A simplified sleep function handles the try/catch or "throws
     * InterruptedException" that Thread.sleep() produces.
     *
     * @param milliSeconds the time to sleep in ms.
     */
    public void sleep(long milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Quits the program.
     */
    public void quit() {
        System.exit(0);
    }

    /**
     * Refreshes the screen. This will be called automatically unless you turn
     * auto-refresh off.
     */
    public void refresh() {
        Platform.runLater(() -> {
                WritableImage w = new WritableImage((int) getWidth() + 1, (int) getHeight() + 1);
                snapshot(null, w);
                liveCanvasGraphics.drawImage(w, 0, 0);
            });
    }

    /**
     * Turns on auto-refresh (refreshes the screen 60 times per second)
     */
    public void autoRefreshOn() {
        autoRefresh = true;
    }

    /**
     * Turns off auto-refresh. With auto-refresh off, you must call refresh()
     * yourself to see the results of any drawing. Use this if you are doing
     * animation or coding a game loop. Do all the drawing first, then call
     * refresh(), then sleep(), then repeat.
     */
    public void autoRefreshOff() {
        autoRefresh = false;
    }

    /**
     * Shows a popup dialog using Swing JOptionPane. It does not display an icon
     *
     * @param message to display
     * @param title   for the popup message box
     */
    public void showDialog(String message, String title) {
        //MH. June 2017
        //clear all keys that are being pressed down
        clearKeysDown();
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * This uses Swing JOptionPane to get text from the user. The text can be
     * from one letter, or a word, to a whole line. The text is terminated with
     * Enter. It displays the Question icon.
     * <p>
     * Sample code:
     * <pre>
     * name = c.showInputDialog("What is your name?", "");
     * //handle CANCEL option
     * if(name == null){
     * System.out.println("Cancel pressed");
     * System.exit(0); //or do something else
     * }
     * //handle OK option with no text
     * if (name.equals("")) {
     * name = "No Name";
     * }</pre>
     *
     * @param message to display
     * @param title   for the popup message box
     * @return The string typed in. If the Cancel button is pressed, the return
     * value is equal to null. If OK is pressed without anything typed in, the
     * return value is a zero length string.
     */
    public String showInputDialog(String message, String title) {
        //MH. June 2017
        //clear all keys that are being pressed down
        clearKeysDown();
        return JOptionPane.showInputDialog(null, message, title, JOptionPane.QUESTION_MESSAGE);
    }

    // **********************************
    // * PUBLIC MOUSE METHODS
    // **********************************

    /**
     * Returns true if the specified button is pressed, false otherwise.
     * <p>
     * Buttons are numbered 0, 1 or 2.
     *
     * @param buttonNum mouse button number (0,1,2)
     * @return T/F if that button has been pressed.
     */
    public boolean getMouseButton(int buttonNum) {

        if ((buttonNum >= 0) && (buttonNum < mouseButton.length)) {
            return mouseButton[buttonNum];
        } else {
            return false;
        }
    }

    /**
     * Returns non-zero if the mouse has been clicked since the last time the
     * click was queried, zero if the mouse was not clicked.
     * <p>
     * 1 = single click 2 = double click 3 = triple click etc.
     *
     * @return number of clicks
     */
    public int getMouseClick() {
        int toReturn = mouseClick;
        mouseClick = 0;
        return toReturn;
    }

    /**
     * Returns the X coordinate of the mouse pointer position within the drawing
     * area.
     *
     * @return x coordinate of mouse pointer position
     */
    public double getMouseX() {
        return mouseX;
    }

    /**
     * Returns the Y coordinate of the mouse pointer position within the drawing
     * area.
     *
     * @return y coordinate of mouse pointer position
     */
    public double getMouseY() {
        return mouseY;
    }

    /**
     * Returns true of mouse is being dragged (button down and mouse moving
     * triggers a drag event)
     *
     * @return
     */
    public boolean isMouseDragged() {
        return mouseDrag;
    }

    /**
     * Returns the distance dragged in the x-axis (positive or negative)
     *
     * @return distance in pixels dragged in x-direction
     */
    public double getMouseDX() {
        if (endDrag == null || startDrag == null) {
            return 0;
        }
        return endDrag.x - startDrag.x;
    }

    /**
     * Returns the distance dragged in the y-axis (positive or negative)
     *
     * @return distance in pixels dragged in y-direction
     */
    public double getMouseDY() {
        if (endDrag == null || startDrag == null) {
            return 0;
        }
        return endDrag.y - startDrag.y;
    }

    // **********************
    // *** NON-PUBLIC METHODS
    // **********************

    /**
     * Sets the cursor to the specified row and column.Adapted from hsa.
     *
     * @param row the row to position the cursor on
     * @param col the column to position the cursor on
     */
    private void setCursorPos(int row, int col) {
        if (cursorFlashing) {
            cursorOff();
        }
        cursorRow = row;
        cursorCol = col;
        if (cursorFlashing) {
            cursorOn();
        }
    }

    //MH. June 2017. Needed for showDialog() in the middle of a game. Not public.
    private synchronized void clearKeysDown() {
        keysDown = new HashSet<>();
        currentKeyChar = (char) 0;//TTD GraphicsConsole.VK_UNDEFINED;
        currentKeyCode = null;//TTD GraphicsConsole.VK_UNDEFINED;
    }

    /**
     * Places a keystroke in the keyboard buffer. It is synchronized so that
     * there can't be a problem with input being taken off the keyboard buffer
     * and placed on the keyboard buffer at the same time. Adapted from hsa.
     * Modified by Sam to record the current key held down.
     *
     * @param e
     */
    private synchronized void keyPressed(KeyEvent e) {
        currentKeyCode = e.getCode();
        lastKeyCode = currentKeyCode;
        keysDown.add(currentKeyCode);
    }

    /**
     * Set current key to the null code
     *
     * @param e
     */
    private void keyReleased(KeyEvent e) {
        currentKeyCode = null;//TTD GraphicsConsole.VK_UNDEFINED;
        currentKeyChar = 0;//TTD (char) GraphicsConsole.VK_UNDEFINED;
        keysDown.remove(e.getCode());
    }

    /**
     * Does nothing. Called by the system when a key is typed.
     *
     * @param e
     */
    private void keyTyped(KeyEvent e) {
        currentKeyChar = e.getCharacter().length() == 0 ? 0 : e.getCharacter().charAt(0);
        lastKeyChar = currentKeyChar;

        char ch = currentKeyChar;
        // Handle standard keystrokes including backspace, newline and
        // Ctrl+U to delete a line of input.
        if (((' ' <= ch) && (ch <= '~')) || (ch == '\b')
        || (ch == '\t') || (ch == '\n') || (ch == '\r') || (ch == '\025')) {
            // Place the keystroke into the keyboard buffer.
            kbdBuffer[kbdBufferHead] = currentKeyChar;
            kbdBufferHead = (kbdBufferHead + 1) % kbdBuffer.length;

            // The following statements wakes up any processes that are
            // sleeping while waiting for keyboard input.
            synchronized (this) {
                notify();
            }
        } // Handle Ctrl+V to paste.
        //        else if (ch == '\026') {
        //            Transferable clipData
        //                    = Toolkit.getToolkit().getSystemClipboard()..getContents(this);
        //
        //            try {
        //                String s = (String) (clipData.getTransferData(DataFlavor.stringFlavor));
        //                int bufferUsed = (kbdBufferHead - kbdBufferTail + kbdBuffer.length) % kbdBuffer.length;
        //                if (s.length() > kbdBuffer.length - bufferUsed) {
        //                    // Current keyboard buffer isn't big enough.
        //                    invertScreen();
        //                } else {
        //                    for (int cnt = 0; cnt < s.length(); cnt++) {
        //                        // Place the keystroke into the keyboard buffer.
        //                        ch = s.charAt(cnt);
        //
        //                        // Some systems seem to mix up CR and LF.
        //                        if (((' ' <= ch) && (ch <= '~')) || (ch == '\n') || (ch == '\r')) {
        //                            kbdBuffer[kbdBufferHead] = ch;
        //                            kbdBufferHead = (kbdBufferHead + 1) % kbdBuffer.length;
        //                        }
        //                    }
        //                    synchronized (this) {
        //                        notify();
        //                    }
        //                }
        //            } catch (UnsupportedFlavorException | IOException exception) {
        //                invertScreen();
        //            }
        //        }
    }

    private void cursorOff() {
        cursorFlashing = false;
        if (cursorVisible) {
            toggleVisibleCursor();
        }
    }

    private void cursorOn() {
        cursorFlashing = true;
    }

    private void toggleVisibleCursor() {
        cursorVisible = !cursorVisible;
        if (cursorVisible) {
            Platform.runLater(() -> {
                    glassPaneGraphics.setStroke(fillColor);
                    glassPaneGraphics.strokeRect(actualCol * fontWidth + MARGIN, actualRow * fontHeight + MARGIN, fontWidth, fontHeight);
                });
        } else {
            Platform.runLater(() -> {
                    glassPaneGraphics.clearRect(0, 0, width, height);
                });
        }
    }

    /**
     * Draws the specified text to the screen at the specified row and column
     * using the specified foreground and background colours. Adapted from hsa.
     * This is imitating the System.out.print() command to use colours on the
     * HSA graphics console.
     *
     * @param row  the row that the text will be printed
     * @param col  the column that he text starts in
     * @param text the text to print
     */
    private void drawText(int row, int col, String text) {
        double x = col * fontWidth;
        double y = row * fontHeight;

        // Erase the area that the image will appear on.
        Platform.runLater(() -> {
                g.setFill(backgroundColor);
                g.fillRect(x + MARGIN, y + MARGIN, fontWidth * text.length(), fontHeight);
                g.setFill(fillColor);
                g.setFont(textFont);
                for (int i=0; i<text.length(); i++)
                    g.fillText(""+text.charAt(i), x + MARGIN + i * fontWidth, y + MARGIN + fontHeight - fontBase);
            });
    }

    /**
     * Clears a rectangle on console canvas from the specified row and column to
     * the end of line. Adapted from hsa.
     *
     * @param row the row specified
     * @param col the column specified
     */
    private void clearToEOL(int row, int col) {
        double x = col * fontWidth;
        double y = row * fontHeight;
        double len = width - x;

        // First clear the rectangle on the offscreen image.
        Platform.runLater(() -> {
                g.setFill(backgroundColor);
                g.fillRect(x + MARGIN, y + MARGIN, len, fontHeight);
            });
    }

    /**
     * Scrolls up the entire ConsoleCanvas a single line. The blank space at the
     * bottom is filled in the specified colour. Adapted from hsa.
     */
    private void scrollUpALine() {
        WritableImage w = new WritableImage((int) getWidth(), (int) getHeight());
        Platform.runLater(() -> {
                snapshot(null, w);
                g.setFill(backgroundColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.drawImage(w, 0, -fontHeight);
            });
    }

    /* This is the action performed for the Swing Timer that is started in the constructor */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (cursorFlashing) {
            flashCount = (flashCount + 1) % flashSpeed;
            if (flashCount == 0) {
                toggleVisibleCursor();
            }
        }
        if (autoRefresh) {
            refresh();
        }
    }

    private void invertScreen() {
        //TODO - fill this in, maybe
        /* Graphics g = getGraphics();
         *
        g.translate(MARGIN, MARGIN);
        g.setColor(Color.white);
        g.setXORMode(Color.black);

        // Invert the screen
        g.fillRect(0, 0, numXPixels, numYPixels);
        Toolkit.getDefaultToolkit().sync();

        // Wait 50 milliseconds
        try {
        Thread.sleep(50);
        } catch (Exception e) {
        }

        // Restore the screen
        g.fillRect(0, 0, numXPixels, numYPixels);
        Toolkit.getDefaultToolkit().sync();

        g.setPaintMode();*/
    }

    /**
     * Erases the previous character in a line of input. Called when the user
     * presses backspace when typing. Adapted from hsa.
     */
    private void erasePreviousChar() {
        if (currentCol > 0) {
            currentCol--;
        } else if (currentRow > 0) {
            currentRow--;
            currentCol = maxCol;
        }
        actualRow = currentRow;
        actualCol = currentCol;

        drawText(currentRow, currentCol, " ");
        setCursorPos(currentRow, currentCol);

        if ((currentCol == -1) && (currentRow != startRow)) {
            currentCol = maxCol + 1;
            currentRow--;
        }
    }

    /**
     * Erases the entire line of input. Called when the user presses Ctrl+U when
     * typing. Adapted from hsa.
     */
    private void eraseLineOfInput() {
        int numChars, cnt;

        numChars = (actualCol - startCol) + (maxCol + 1) * (actualRow - startRow);
        currentRow = startRow;
        currentCol = startCol;
        actualRow = startRow;
        actualCol = startCol;
        for (cnt = 0; cnt < numChars; cnt++) {
            print(" ");
        }
        currentRow = startRow;
        currentCol = startCol;
        actualRow = startRow;
        actualCol = startCol;
        setCursorPos(currentRow, currentCol);
    } // eraseLineOfInput (void)

    /* *********************************************
     * MOUSE LISTENER EVENTS
     * "Background" mouse methods
     * (i.e., don't try to invoke these directly!)
     * *********************************************/
    private void mouseClicked(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        //mouseClick = e.getClickCount(); //moved to mouseReleased for faster response
    }

    private void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        endDrag = new Point(e.getX(), e.getY());
        mouseDrag = true;
    }

    private void mouseEntered(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    private void mouseExited(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    private void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (null != e.getButton()) {
            switch (e.getButton()) {
                case PRIMARY:
                mouseButton[0] = true;
                break;
                case SECONDARY:
                mouseButton[1] = true;
                break;
                case MIDDLE:
                mouseButton[2] = true;
                break;
                default:
                break;
            }
        }

        startDrag = new Point(e.getX(), e.getY());
        endDrag = startDrag;
    }

    private void mouseReleased(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        mouseClick = e.getClickCount(); //for better response when using gc.getMouseClick()

        if (null != e.getButton()) {
            switch (e.getButton()) {
                case PRIMARY:
                mouseButton[0] = false;
                break;
                case SECONDARY:
                mouseButton[1] = false;
                break;
                case MIDDLE:
                mouseButton[2] = false;
                break;
                default:
                break;
            }
        }

        mouseDrag = false;
        startDrag = endDrag = null;
    }
}

class Point {

    double x, y;

    Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

}
