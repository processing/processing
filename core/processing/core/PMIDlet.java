package processing.core;

import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/**
 *
 * @author  Francis Li
 */
public abstract class PMIDlet extends MIDlet implements Runnable {
    private Display     display;
    private PCanvas     canvas;
    
    private boolean     running;
    private long        startTime;
    
    private Calendar    calendar;
    private Random      random;
    
    /** Creates a new instance of PMIDlet */
    public PMIDlet() {
        display = Display.getDisplay(this);
    }
    
    protected final void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        running = false;
    }
    
    protected final void pauseApp() {
        running = false;
    }
    
    protected final void startApp() throws MIDletStateChangeException {
        running = true;
        if (canvas == null) {
            canvas = new PCanvas(this);
            width = canvas.getWidth();
            height = canvas.getHeight();
            
            startTime = System.currentTimeMillis();
            
            setup();
        }
        display.setCurrent(canvas);        
        display.callSerially(this);
    }
    
    public final void run() {
        draw();
        canvas.repaint();
        
        if (running) {
            display.callSerially(this);
        }
    }
    
    public static final int CORNER          = 0;
    public static final int CORNERS         = 1;
    public static final int CENTER          = 2;
    public static final int CENTER_RADIUS   = 3;
    
    public static final int POINTS          = 0;
    public static final int LINES           = 1;
    public static final int LINE_STRIP      = 2;
    public static final int LINE_LOOP       = 3;
    public static final int TRIANGLES       = 4;
    public static final int TRIANGLE_STRIP  = 5;
    public static final int QUADS           = 6;
    public static final int QUAD_STRIP      = 7;
    public static final int POLYGON         = 8;
    
    public static final int UP              = 0;
    public static final int DOWN            = 1;
    public static final int LEFT            = 2;
    public static final int RIGHT           = 3;
    public static final int FIRE            = 4;
    
    public static final int ALIGN_LEFT      = 0;
    public static final int ALIGN_RIGHT     = 1;
    public static final int ALIGN_CENTER    = 2;    
    
    protected int       width;
    protected int       height;
    
    protected char      key;
    protected boolean   keyPressed;
    
    public void setup() {
    }
    
    public void draw() {      
    }

    public void keyPressed() {
        
    }
    
    public void keyReleased() {
        
    }
    
    public final void redraw() {
        draw();
        canvas.repaint();
    }
    
    public final void loop() {
        running = true;
        display.callSerially(this);
    }
    
    public final void noLoop() {
        running = false;
    }    
    
    public final void size(int width, int height) {        
    }
    
    public final void framerate(int fps) {        
    }
    
    public final void point(int x1, int y1) {
        canvas.point(x1, y1);
    }
    
    public final void line(int x1, int y1, int x2, int y2) {
        canvas.line(x1, y1, x2, y2);
    }
    
    public final void triangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        canvas.triangle(x1, y1, x2, y2, x3, y3);
    }
    
    public final void quad(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        canvas.quad(x1, y1, x2, y2, x3, y3, x4, y4);
    }
    
    public final void rect(int x, int y, int width, int height) {
        canvas.rect(x, y, width, height);
    }
    
    public final void rectMode(int MODE) {     
        canvas.rectMode(MODE);
    }
    
    public final void ellipse(int x, int y, int width, int height) {
        canvas.ellipse(x, y, width, height);
    }
    
    public final void ellipseMode(int MODE) {
        canvas.ellipseMode(MODE);
    }
    
    public final void curve(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        canvas.curve(x1, y1, x2, y2, x3, y3, x4, y4);
    }
    
    public final void bezier(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        canvas.bezier(x1, y1, x2, y2, x3, y3, x4, y4);
    }
    
    public final void strokeWeight(int width) {
        canvas.strokeWeight(width);
    }
    
    public final void beginShape(int MODE) {
        canvas.beginShape(MODE);
    }
    
    public final void endShape() {
        canvas.endShape();
    }
    
    public final void vertex(int x, int y) {
        canvas.vertex(x, y);
    }
    
    public final void curveVertex(int x, int y) {
        canvas.curveVertex(x, y);
    }
    
    public final void bezierVertex(int x, int y) {
        canvas.bezierVertex(x, y);
    }
    
    public final void translate(int x, int y) {
        canvas.translate(x, y);
    }
    
    public final void background(int gray) {
        canvas.background(gray);
    }
    
    public final void background(int value1, int value2, int value3) {
        canvas.background(value1, value2, value3);
    }
    
    public final void stroke(int gray) {
        canvas.stroke(gray);
    }
    
    public final void stroke(int value1, int value2, int value3) {
        canvas.stroke(value1, value2, value3);
    }
    
    public final void noStroke() {
        canvas.noStroke();
    }
    
    public final void fill(int gray) {
        canvas.fill(gray);
    }
    
    public final void fill(int value1, int value2, int value3) {
        canvas.fill(value1, value2, value3);
    }
    
    public final void noFill() {
        canvas.noFill();
    }
    
    public final void text(String data, int x, int y) {
        canvas.text(data, x, y);
    }
    
    public final void textMode(int MODE) {
        canvas.textMode(MODE);
    }
    
    public final int millis() {
        return (int) (System.currentTimeMillis() - startTime);
    }
        
    private void checkCalendar() {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        calendar.setTime(new Date());
    }
    
    public final int second() {
        checkCalendar();
        return calendar.get(Calendar.SECOND);
    }
    
    public final int minute() {
        checkCalendar();
        return calendar.get(Calendar.MINUTE);
    }
    
    public final int hour() {
        checkCalendar();
        return calendar.get(Calendar.HOUR_OF_DAY);
    }
    
    public final int day() {
        checkCalendar();
        return calendar.get(Calendar.DAY_OF_MONTH);
    }
    
    public final int month() {
        checkCalendar();
        return calendar.get(Calendar.MONTH);
    }
    
    public final int year() {
        checkCalendar();
        return calendar.get(Calendar.YEAR);        
    }
    
    public final int abs(int value) {
        return Math.abs(value);
    }
    
    public final int max(int value1, int value2) {
        return Math.max(value1, value2);
    }
    
    public final int min(int value1, int value2) {
        return Math.min(value1, value2);
    }
    
    public final int sq(int value) {
        return value * value;
    }
    
    public final int pow(int base, int exponent) {
        int value = 1;
        for (int i = 0; i < exponent; i++) {
            value *= base;
        }
        
        return value;
    }
    
    public final int constrain(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
    
    public final int random(int value1) {
        return random(0, value1);
    }
    
    public final int random(int value1, int value2) {
        if (random == null) {
            random = new Random();
        }
        int min = Math.min(value1, value2);
        int range = Math.abs(value2 - value1) + 1;
        
        return min + Math.abs((random.nextInt() % range));
    }
    
    public final String[] loadStrings(String filename) {
        String[] strings = null;
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream(filename);
            Reader r = new InputStreamReader(is);
            
            strings = new String[8];
            int numStrings = 0;
            
            StringBuffer buffer = new StringBuffer();
            int input = r.read();
            while (true) {
                if ((input < 0) || (input == '\n')) {
                    String s = buffer.toString().trim();
                    if (s.length() > 0) {
                        numStrings++;

                        int length = strings.length;
                        if (numStrings > length) {
                            String[] old = strings;
                            strings = new String[length * 2];
                            System.arraycopy(old, 0, strings, 0, length);
                        }
                        strings[numStrings - 1] = s;
                    }
                    buffer.delete(0, Integer.MAX_VALUE);
                    
                    if (input < 0) {
                        break;
                    }
                } else {
                    buffer.append((char) input);
                }
                
                input = r.read();
            }
            //// shrink array
            if (numStrings < strings.length) {
                String[] old = strings;
                strings = new String[numStrings];
                if (numStrings > 0) {
                    System.arraycopy(old, 0, strings, 0, numStrings);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    
                }
            }
            if (strings == null) {
                strings = new String[0];
            }
        }
        
        return strings;
    }
    
    public final void print(String data) {
        System.out.print(data);
    }
    
    public final void println(String data) {
        System.out.println(data);
    }
}
