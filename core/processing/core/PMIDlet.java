package processing.core;

import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/**
 *
 * @author  Francis Li
 */
public abstract class PMIDlet extends MIDlet implements Runnable, CommandListener {    
    public static final int CENTER          = 0;
    public static final int CENTER_RADIUS   = 1;
    public static final int CORNER          = 2;
    public static final int CORNERS         = 3;
    
    public static final int POINTS          = 0;
    public static final int LINES           = 1;
    public static final int LINE_STRIP      = 2;
    public static final int LINE_LOOP       = 3;
    public static final int TRIANGLES       = 4;
    public static final int TRIANGLE_STRIP  = 5;
    public static final int QUADS           = 6;
    public static final int QUAD_STRIP      = 7;
    public static final int POLYGON         = 8;
    
    public static final int UP              = Canvas.UP;
    public static final int DOWN            = Canvas.DOWN;
    public static final int LEFT            = Canvas.LEFT;
    public static final int RIGHT           = Canvas.RIGHT;
    public static final int FIRE            = Canvas.FIRE;    
    public static final int GAME_A          = Canvas.GAME_A;
    public static final int GAME_B          = Canvas.GAME_B;
    public static final int GAME_C          = Canvas.GAME_C;
    public static final int GAME_D          = Canvas.GAME_D;

    public static final int RGB             = 0;
    public static final int HSB             = 1;
    
    protected int       width;
    protected int       height;
    
    protected char      key;
    protected int       keyCode;
    protected boolean   keyPressed;
    
    protected int       framerate;
    protected int       frameCount;
    
    protected PCanvas   canvas;    
    protected Display   display;
    protected Command   cmdExit;
    protected Command   cmdCustom;
    
    private Thread      thread;
    private boolean     running;
    private boolean     redraw;
    private long        startTime;
    private long        lastFrameTime;
    private int         msPerFrame;
    
    private Calendar    calendar;
    private Random      random;
    
    /** Creates a new instance of PMIDlet */
    public PMIDlet() {
        display = Display.getDisplay(this);
    }
    
    public final void commandAction(Command c, Displayable d) {
        if (c == cmdExit) {
            exit();
        } else if (c == cmdCustom) {
            softkeyPressed(c.getLabel());
        }
    }
    
    public void softkeyPressed(String softkey) {
    }
    
    public final void softkey(String softkey) {
        if (cmdCustom != null) {
            canvas.removeCommand(cmdCustom);
        }
        if (softkey != null) {
            cmdCustom = new Command(softkey, Command.SCREEN, 2);
            canvas.addCommand(cmdCustom);
        }
    }
    
    public final void exit() {
        try {
            destroyApp(true);
            notifyDestroyed();
        } catch (MIDletStateChangeException msce) {                
        }
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
            cmdExit = new Command("Exit", Command.EXIT, 1);
            
            canvas = new PCanvas(this);            
            canvas.addCommand(cmdExit);
            canvas.setCommandListener(this);
            
            width = canvas.getWidth();
            height = canvas.getHeight();
            
            startTime = System.currentTimeMillis();
            msPerFrame = 1;
            
            setup();
            
            lastFrameTime = startTime - msPerFrame;
        }
        display.setCurrent(canvas);        
        thread = new Thread(this);
        thread.start();
    }
    
    public final void run() {
        do {
            long currentTime = System.currentTimeMillis();
            int elapsed = (int) (currentTime - lastFrameTime);
            if (redraw || (elapsed >= msPerFrame)) {
                canvas.resetMatrix();
                draw();
                canvas.repaint();
                canvas.serviceRepaints();
                lastFrameTime = currentTime;
                framerate = 1000 / elapsed;
                frameCount++;
                
                redraw = false;
            }
        } while (running);
        thread = null;
    }
    
    public void setup() {
    }
    
    public void draw() {      
    }

    public void keyPressed() {
        
    }
    
    public void keyReleased() {
        
    }
    
    public final void redraw() {
        redraw = true;
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }
    
    public final void loop() {
        running = true;
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }
    
    public final void noLoop() {
        running = false;
    }    
    
    public final void size(int width, int height) {        
    }
    
    public final void framerate(int fps) {        
        msPerFrame = 1000 / fps;
        if (msPerFrame <= 0) {
            msPerFrame = 1;
        }
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
    
    public final void bezierVertex(int x1, int y1, int x2, int y2, int x3, int y3) {
        canvas.bezierVertex(x1, y1, x2, y2, x3, y3);
    }
    
    public final void translate(int x, int y) {
        canvas.translate(x, y);
    }
    
    public final void pushMatrix() {
        canvas.pushMatrix();
    }
    
    public final void popMatrix() {
        canvas.popMatrix();
    }
    
    public final void resetMatrix() {
        canvas.resetMatrix();
    }
    
    public final void background(int gray) {
        canvas.background(gray);
    }
    
    public final void background(int value1, int value2, int value3) {
        canvas.background(value1, value2, value3);
    }
    
    public final void background(PImage img) {
        canvas.background(img);
    }
    
    public final void colorMode(int mode) {
        canvas.colorMode(mode);
    }
    
    public final void colorMode(int mode, int range) {
        colorMode(mode, range, range, range);
    }
    
    public final void colorMode(int mode, int range1, int range2, int range3) {
        canvas.colorMode(mode, range1, range2, range3);
    }
    
    public final void colorMode(int mode, int range1, int range2, int range3, int range4) {
        canvas.colorMode(mode, range1, range2, range3, range4);
    }
    
    public final int color(int gray) {
        return canvas.color(gray);
    }
    
    public final int color(int gray, int alpha) {
        return canvas.color(gray, alpha);
    }
    
    public final int color(int value1, int value2, int value3) {
        return canvas.color(value1, value2, value3);
    }
    
    public final int color(int value1, int value2, int value3, int alpha) {
        return canvas.color(value1, value2, value3, alpha);
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
        
    public final PImage loadImage(String filename) {
        try {
            Image peer = Image.createImage("/" + filename);
            return new PImage(peer);
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public final void image(PImage img, int x, int y) {
        canvas.image(img,x,y);
    }
    
    public final PFont loadFont(String fontname, int color, int bgcolor) {
        try {
            return new PFont(getClass().getResourceAsStream("/" + fontname), color, bgcolor);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public final PFont loadFont(String fontname, int color) {
        return loadFont(fontname, color, 0xffffff);
    }
    
    public final PFont loadFont(String fontname) {
        return loadFont(fontname, 0);
    }
    
    public final void textFont(PFont font) {
        canvas.textFont(font);
    }
    
    public final void textAlign(int MODE) {
        canvas.textAlign(MODE);
    }
    
    public final void text(String data, int x, int y) {
        canvas.text(data, x, y);
    }
    
    public final int textWidth(String data) {
        return canvas.textWidth(data);
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
    
    public final byte[] loadBytes(String filename) {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream(filename);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[1024];
            int bytesRead = is.read(buffer);
            while (bytesRead >= 0) {
                baos.write(buffer, 0, bytesRead);
                bytesRead = is.read(buffer);
            }
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {                    
                }
            }
        }
    }
    
    public final String[] loadStrings(String filename) {
        Vector v = new Vector();
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream(filename);
            Reader r = new InputStreamReader(is);
            
            int numStrings = 0;
            
            StringBuffer buffer = new StringBuffer();
            int input = r.read();
            while (true) {
                if ((input < 0) || (input == '\n')) {
                    String s = buffer.toString().trim();
                    if (s.length() > 0) {
                        numStrings++;
                        v.addElement(s);
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
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {                    
                }
            }
        }
        String[] strings = new String[v.size()];
        v.copyInto(strings);
        
        return strings;
    }
    
    public final void print(String data) {
        System.out.print(data);
    }
    
    public final void println(String data) {
        System.out.println(data);
    }
    
    public final String join(String[] anyArray, String separator) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0, length = anyArray.length; i < length; i++) {
            buffer.append(anyArray[i]);
            if (i < (length - 1)) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }
    
    public final String join(int[] anyArray, String separator) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0, length = anyArray.length; i < length; i++) {
            buffer.append(anyArray[i]);
            if (i < (length - 1)) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }
    
    public final String join(int[] intArray, String separator, int digits) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0, length = intArray.length; i < length; i++) {
            buffer.append(nf(intArray[i], digits));
            if (i < (length - 1)) {
                buffer.append(separator);
            }
        }
        return buffer.toString();        
    }
    
    public final String nf(int intValue, int digits) {
        StringBuffer buffer = new StringBuffer();
        for (int j = Integer.toString(intValue).length(); j < digits; j++) {
            buffer.append("0");
        }        
        buffer.append(intValue);
        return buffer.toString();
    }
    
    public final String nfp(int intValue, int digits) {
        StringBuffer buffer = new StringBuffer();
        if (intValue < 0) {
            buffer.append("-");
        } else {
            buffer.append("+");
        }
        buffer.append(nf(intValue, digits));        
        return buffer.toString();
    }
    
    public final String nfs(int intValue, int digits) {
        StringBuffer buffer = new StringBuffer();
        if (intValue < 0) {
            buffer.append("-");
        } else {
            buffer.append(" ");
        }
        buffer.append(nf(intValue, digits));        
        return buffer.toString();
    }
    
    public final String[] split(String str) {
        Vector v = new Vector();
        StringBuffer buffer = new StringBuffer();
        char c;
        boolean whitespace = false;
        for (int i = 0, length = str.length(); i < length; i++ ) {
            c = str.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                case '\f':
                case '\t':
                case ' ':
                    whitespace = true;
                    break;
                default:
                    if (whitespace) {
                        v.addElement(buffer.toString());
                        buffer.delete(0, buffer.length());
                        
                        whitespace = false;
                    }
                    buffer.append(c);
            }
        }        
        if (buffer.length() > 0) {
            v.addElement(buffer.toString());
        }
        
        String[] tokens = new String[v.size()];
        v.copyInto(tokens);
        
        return tokens;
    }
    
    public final String[] split(String str, String delim) {
        Vector v = new Vector();
        int prevIndex = 0;
        int nextIndex = str.indexOf(delim, prevIndex);
        while (nextIndex >= 0) {
            v.addElement(str.substring(prevIndex, nextIndex));
            prevIndex = nextIndex + 1;
            nextIndex = str.indexOf(delim, prevIndex);
        }
        if (prevIndex < str.length()) {
            v.addElement(str.substring(prevIndex));
        }
        
        String[] tokens = new String[v.size()];
        v.copyInto(tokens);
        
        return tokens;
    }
    
    public final String trim(String str) {
        //// deal with unicode nbsp later
        return str.trim();
    }
    
    public final String[] append(String[] array, String element) {
        String[] old = array;
        int length = old.length;
        array = new String[length + 1];
        System.arraycopy(old, 0, array, 0, length);
        array[length] = element;
        return array;        
    }
    
    public final boolean[] append(boolean[] array, boolean element) {
        boolean[] old = array;
        int length = old.length;
        array = new boolean[length + 1];
        System.arraycopy(old, 0, array, 0, length);
        array[length] = element;
        return array;        
    }
    
    public final byte[] append(byte[] array, byte element) {
        byte[] old = array;
        int length = old.length;
        array = new byte[length + 1];
        System.arraycopy(old, 0, array, 0, length);
        array[length] = element;
        return array;        
    }
    
    public final char[] append(char[] array, char element) {
        char[] old = array;
        int length = old.length;
        array = new char[length + 1];
        System.arraycopy(old, 0, array, 0, length);
        array[length] = element;
        return array;        
    }
    
    public final int[] append(int[] array, int element) {
        int[] old = array;
        int length = old.length;
        array = new int[length + 1];
        System.arraycopy(old, 0, array, 0, length);
        array[length] = element;
        return array;        
    }
    
    public final void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }
    
    public final String[] concat(String[] array1, String[] array2) {
        int length1 = array1.length;
        int length2 = array2.length;
        String[] array = new String[length1 + length2];
        System.arraycopy(array1, 0, array, 0, length1);
        System.arraycopy(array2, 0, array, length1, length2);
        return array;
    }
    
    public final boolean[] concat(boolean[] array1, boolean[] array2) {
        int length1 = array1.length;
        int length2 = array2.length;
        boolean[] array = new boolean[length1 + length2];
        System.arraycopy(array1, 0, array, 0, length1);
        System.arraycopy(array2, 0, array, length1, length2);
        return array;
    }
    
    public final byte[] concat(byte[] array1, byte[] array2) {
        int length1 = array1.length;
        int length2 = array2.length;
        byte[] array = new byte[length1 + length2];
        System.arraycopy(array1, 0, array, 0, length1);
        System.arraycopy(array2, 0, array, length1, length2);
        return array;
    }
    
    public final char[] concat(char[] array1, char[] array2) {
        int length1 = array1.length;
        int length2 = array2.length;
        char[] array = new char[length1 + length2];
        System.arraycopy(array1, 0, array, 0, length1);
        System.arraycopy(array2, 0, array, length1, length2);
        return array;
    }
    
    public final int[] concat(int[] array1, int[] array2) {
        int length1 = array1.length;
        int length2 = array2.length;
        int[] array = new int[length1 + length2];
        System.arraycopy(array1, 0, array, 0, length1);
        System.arraycopy(array2, 0, array, length1, length2);
        return array;
    }
    
    public final String[] contract(String[] array, int newSize) {
        int length = array.length;
        if (length > newSize) {
            String[] old = array;
            array = new String[newSize];
            System.arraycopy(old, 0, array, 0, newSize);
        }
        return array;
    }
    
    public final String[] expand(String[] array) {
        return expand(array, array.length * 2);
    }
    
    public final String[] expand(String[] array, int newSize) {
        int length = array.length;
        if (length < newSize) {
            String[] old = array;
            array = new String[newSize];
            System.arraycopy(old, 0, array, 0, length);
        }
        return array;
    }
    
    public final String[] reverse(String[] array) {
        int length = array.length;
        String[] reversed = new String[length];
        for (int i = length - 1; i >= 0; i--) {
            reversed[i] = array[length - i - 1];
        }
        return reversed;
    }
    
    public final String[] shorten(String[] array) {
        String[] old = array;
        int length = old.length - 1;
        array = new String[length];
        System.arraycopy(old, 0, array, 0, length);
        return array;
    }
    
    public final String[] slice(String[] array, int offset) {
        return slice(array, offset, array.length - offset);
    }
    
    public final String[] slice(String[] array, int offset, int length) {
        String[] slice = new String[length];
        System.arraycopy(array, offset, slice, 0, length);
        return slice;
    }
    
    public final String[] splice(String[] array, String value, int index) {
        int length = array.length;
        String[] splice = new String[length + 1];
        System.arraycopy(array, 0, splice, 0, index);
        splice[index] = value;
        System.arraycopy(array, index, splice, index + 1, length - index);
        return splice;
    }
    
    public final String[] splice(String[] array, String[] array2, int index) {
        int length = array.length;
        int length2 = array2.length;
        String[] splice = new String[length + length2];
        System.arraycopy(array, 0, splice, 0, index);
        System.arraycopy(array2, 0, splice, index, length2);
        System.arraycopy(array, index, splice, index + length2, length - index);
        return splice;
    }
    
    //// casting 
    
    public static final char toChar(boolean val) {
        return (val ? 't' : 'f');
    }
    
    public static final char toChar(byte val) {
        return (char) (val & 0xff);
    }
    
    public static final char toChar(int val) {
        return (char) val;
    }
    
    public static final char[] toChar(String val) {
        return val.toCharArray();
    }
    
    public static final char[] toChar(boolean[] val) {
        char[] result = new char[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = val[i] ? 't' : 'f';
        }
        return result;
    }
    
    public static final char[] toChar(byte[] val) {
        char[] result = new char[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = (char) (val[i] & 0xff);
        }
        return result;
    }
    
    public static final char[] toChar(int[] val) {
        char[] result = new char[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = (char) val[i];
        }
        return result;
    }
    
    public static final char[][] toChar(String[] val) {
        char[][] result = new char[val.length][];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = val[i].toCharArray();
        }
        return result;
    }
    
    public static final int toInt(boolean val) {
        return (val ? 1 : 0);
    }
    
    public static final int toInt(byte val) {
        return (val & 0xff);
    }
    
    public static final int toInt(char val) {
        return val;
    }
    
    public static final int toInt(String val) {
        int result = 0;
        try {
            result = Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
        }
        return result;
    }
    
    public static final int[] toInt(boolean[] val) {
        int[] result = new int[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = val[i] ? 't' : 'f';
        }
        return result;
    }
    
    public static final int[] toInt(byte[] val) {
        int[] result = new int[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = (int) (val[i] & 0xff);
        }
        return result;
    }
    
    public static final int[] toInt(char[] val) {
        int[] result = new int[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = (int) val[i];
        }
        return result;
    }
    
    public static final int[] toInt(String[] val) {
        int[] result = new int[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            try {
                result[i] = Integer.parseInt(val[i]);
            } catch (NumberFormatException nfe) {
            }
        }
        return result;
    }
    
    public static final String str(boolean val) {
        return String.valueOf(val);
    }
    
    public static final String str(byte val) {
        return String.valueOf(val);
    }
    
    public static final String str(char val) {
        return String.valueOf(val);
    }
    
    public static final String str(int val) {
        return String.valueOf(val);
    }
    
    public static final String[] str(boolean[] val) {
        String[] result = new String[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = String.valueOf(val[i]);
        }
        return result;
    }
    
    public static final String[] str(byte[] val) {
        String[] result = new String[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = String.valueOf(val[i]);
        }
        return result;
    }
    
    public static final String[] str(char[] val) {
        String[] result = new String[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = String.valueOf(val[i]);
        }
        return result;
    }
    
    public static final String[] str(int[] val) {
        String[] result = new String[val.length];
        for (int i = val.length - 1; i >= 0; i--) {
            result[i] = String.valueOf(val[i]);
        }
        return result;
    }
    
    //// Experimental fixed point math routines here
    
    /** Precision, in number of bits for the fractional part. */
    public static final int FP_PRECISION    = 8;
    /** Convenience constant of the value 1 in fixed point. */
    public static final int ONE             = 1 << FP_PRECISION;
    /** Convenience constant of the value of pi in fixed point. */
    public static final int PI              = (int) ((3.14159265358979323846f) * ONE);
    /** Convenience constant of the value of 2*pi in fixed point. */
    public static final int TWO_PI          = 2 * PI;
    /** Convenience constant of the value of pi/2 in fixed point. */
    public static final int HALF_PI         = PI / 2;
    
    /** Multiplies two fixed point values and returns a fixed point value. */
    public final int mul(int value1, int value2) {
        return (value1 * value2) >> FP_PRECISION;
    }
    
    /** Returns the fixed point quotient from dividing the fixed point dividend by the fixed point divisor. */
    public final int div(int dividend, int divisor) {
        return (dividend << FP_PRECISION) / divisor;
    }
    
    /** Returns the fixed point representation of the specified integer value. */
    public final int itofp(int value1) {
        return value1 << FP_PRECISION;
    }
    
    /** Returns the integer less than or equal to the fixed point value. */
    public final int fptoi(int value1) {
        if (value1 < 0) {
            value1 += ONE - 1;
        }
        return value1 >> FP_PRECISION;
    }
        
    /** Returns the fixed-point square root of a fixed-point value, approximated using Newton's method. */
    public final int sqrt(int value_fp) {
        int prev_fp, next_fp, error_fp, prev;        
        //// initialize previous result
        prev_fp = value_fp;        
        next_fp = 0;
        do {
            prev = prev_fp >> FP_PRECISION;
            if (prev == 0) {                
                break;
            }
            //// calculate a new approximation
            next_fp = (prev_fp + value_fp / prev) / 2;
            if (prev_fp > next_fp) {
                error_fp = prev_fp - next_fp;
            } else {
                error_fp = next_fp - prev_fp;
            }
            prev_fp = next_fp;
        } while (error_fp > ONE);      
        
        return next_fp;
    }
    
    public final int dist(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return sqrt((dx * dx + dy * dy) << FP_PRECISION);
    }
    
    public final int dist_fp(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return sqrt(((dx * dx) >> FP_PRECISION) + ((dy * dy) >> FP_PRECISION));
    }
    
    /** Returns the closest integer fixed-point value less than or equal to the specified fixed point value. */
    public final int floor(int value1) {
        return (value1 >> FP_PRECISION) << FP_PRECISION;
    }
    
    /** Returns the closest integer fixed-point value greater than or equal to the specified fixed point value. */
    public final int ceil(int value1) {
        return ((value1 + ONE - 1) >> FP_PRECISION) << FP_PRECISION;
    }
    
    /** Returns the nearest integer fixed-point value to the specified fixed point value. */
    public final int round(int value1) {
        //// return result
        return ((value1 + (ONE >> 1)) >> FP_PRECISION) << FP_PRECISION;
    }
    
    /** Returns the fixed point radian equivalent to the specified fixed point degree value. */
    public final int radians(int angle) {
        return angle * PI / (180 << FP_PRECISION);
    }
    
    /** Returns the sin of the specified fixed-point radian angle as a fixed point value. */
    public final int sin(int rad) {
        //// convert to degrees
        int index = rad * 180 / PI % 360;
        if (index < 0) {
            index += 360;
        }
        return sin[index];
    }
    
    /** Returns the cos of the specified fixed-point radian angle as a fixed point value. */
    public final int cos(int rad) {
        //// convert to degrees
        int index = (rad * 180 / PI + 90) % 360;
        if (index < 0) {
            index += 360;
        }
        return sin[index];
    }
    
    public final int atan(int value1) {
        int result;
        if (value1 <= ONE) {
            result = div(value1, ONE + mul(((int) (0.28f * ONE)), mul(value1, value1)));
        } else {
            result = HALF_PI - div(value1, (mul(value1, value1) + ((int) (0.28f * ONE))));
        }
        return result;
    }
    
    public final int atan2(int y, int x) {
        int result;
        if ((y == 0) && (x == 0)) {
            result = 0;
        } else if (x > 0) {
            result = atan(div(y, x));
        } else if (x < 0) {
            if (y < 0) {
                result = -(PI - atan(div(-y, -x)));
            } else {
                result = PI - atan(div(y, -x));
            }
        } else {
            if (y < 0) {
                result = -HALF_PI;
            } else {
                result = HALF_PI;
            }
        }
        return result;
    }
    
    /** Lookup table for sin function, indexed by degrees. */
    public static final int[] sin = {
        (int) (0f * ONE),
        (int) (0.0174524064372835f * ONE),
        (int) (0.034899496702501f * ONE),
        (int) (0.0523359562429438f * ONE),
        (int) (0.0697564737441253f * ONE),
        (int) (0.0871557427476582f * ONE),
        (int) (0.104528463267653f * ONE),
        (int) (0.121869343405147f * ONE),
        (int) (0.139173100960065f * ONE),
        (int) (0.156434465040231f * ONE),
        (int) (0.17364817766693f * ONE),
        (int) (0.190808995376545f * ONE),
        (int) (0.207911690817759f * ONE),
        (int) (0.224951054343865f * ONE),
        (int) (0.241921895599668f * ONE),
        (int) (0.258819045102521f * ONE),
        (int) (0.275637355816999f * ONE),
        (int) (0.292371704722737f * ONE),
        (int) (0.309016994374947f * ONE),
        (int) (0.325568154457157f * ONE),
        (int) (0.342020143325669f * ONE),
        (int) (0.3583679495453f * ONE),
        (int) (0.374606593415912f * ONE),
        (int) (0.390731128489274f * ONE),
        (int) (0.4067366430758f * ONE),
        (int) (0.422618261740699f * ONE),
        (int) (0.438371146789077f * ONE),
        (int) (0.453990499739547f * ONE),
        (int) (0.469471562785891f * ONE),
        (int) (0.484809620246337f * ONE),
        (int) (0.5f * ONE),
        (int) (0.515038074910054f * ONE),
        (int) (0.529919264233205f * ONE),
        (int) (0.544639035015027f * ONE),
        (int) (0.559192903470747f * ONE),
        (int) (0.573576436351046f * ONE),
        (int) (0.587785252292473f * ONE),
        (int) (0.601815023152048f * ONE),
        (int) (0.615661475325658f * ONE),
        (int) (0.629320391049837f * ONE),
        (int) (0.642787609686539f * ONE),
        (int) (0.656059028990507f * ONE),
        (int) (0.669130606358858f * ONE),
        (int) (0.681998360062498f * ONE),
        (int) (0.694658370458997f * ONE),
        (int) (0.707106781186547f * ONE),
        (int) (0.719339800338651f * ONE),
        (int) (0.73135370161917f * ONE),
        (int) (0.743144825477394f * ONE),
        (int) (0.754709580222772f * ONE),
        (int) (0.766044443118978f * ONE),
        (int) (0.777145961456971f * ONE),
        (int) (0.788010753606722f * ONE),
        (int) (0.798635510047293f * ONE),
        (int) (0.809016994374947f * ONE),
        (int) (0.819152044288992f * ONE),
        (int) (0.829037572555042f * ONE),
        (int) (0.838670567945424f * ONE),
        (int) (0.848048096156426f * ONE),
        (int) (0.857167300702112f * ONE),
        (int) (0.866025403784439f * ONE),
        (int) (0.874619707139396f * ONE),
        (int) (0.882947592858927f * ONE),
        (int) (0.891006524188368f * ONE),
        (int) (0.898794046299167f * ONE),
        (int) (0.90630778703665f * ONE),
        (int) (0.913545457642601f * ONE),
        (int) (0.92050485345244f * ONE),
        (int) (0.927183854566787f * ONE),
        (int) (0.933580426497202f * ONE),
        (int) (0.939692620785908f * ONE),
        (int) (0.945518575599317f * ONE),
        (int) (0.951056516295154f * ONE),
        (int) (0.956304755963035f * ONE),
        (int) (0.961261695938319f * ONE),
        (int) (0.965925826289068f * ONE),
        (int) (0.970295726275996f * ONE),
        (int) (0.974370064785235f * ONE),
        (int) (0.978147600733806f * ONE),
        (int) (0.981627183447664f * ONE),
        (int) (0.984807753012208f * ONE),
        (int) (0.987688340595138f * ONE),
        (int) (0.99026806874157f * ONE),
        (int) (0.992546151641322f * ONE),
        (int) (0.994521895368273f * ONE),
        (int) (0.996194698091746f * ONE),
        (int) (0.997564050259824f * ONE),
        (int) (0.998629534754574f * ONE),
        (int) (0.999390827019096f * ONE),
        (int) (0.999847695156391f * ONE),
        (int) (1f * ONE),
        (int) (0.999847695156391f * ONE),
        (int) (0.999390827019096f * ONE),
        (int) (0.998629534754574f * ONE),
        (int) (0.997564050259824f * ONE),
        (int) (0.996194698091746f * ONE),
        (int) (0.994521895368273f * ONE),
        (int) (0.992546151641322f * ONE),
        (int) (0.99026806874157f * ONE),
        (int) (0.987688340595138f * ONE),
        (int) (0.984807753012208f * ONE),
        (int) (0.981627183447664f * ONE),
        (int) (0.978147600733806f * ONE),
        (int) (0.974370064785235f * ONE),
        (int) (0.970295726275996f * ONE),
        (int) (0.965925826289068f * ONE),
        (int) (0.961261695938319f * ONE),
        (int) (0.956304755963036f * ONE),
        (int) (0.951056516295154f * ONE),
        (int) (0.945518575599317f * ONE),
        (int) (0.939692620785908f * ONE),
        (int) (0.933580426497202f * ONE),
        (int) (0.927183854566787f * ONE),
        (int) (0.92050485345244f * ONE),
        (int) (0.913545457642601f * ONE),
        (int) (0.90630778703665f * ONE),
        (int) (0.898794046299167f * ONE),
        (int) (0.891006524188368f * ONE),
        (int) (0.882947592858927f * ONE),
        (int) (0.874619707139396f * ONE),
        (int) (0.866025403784439f * ONE),
        (int) (0.857167300702112f * ONE),
        (int) (0.848048096156426f * ONE),
        (int) (0.838670567945424f * ONE),
        (int) (0.829037572555042f * ONE),
        (int) (0.819152044288992f * ONE),
        (int) (0.809016994374947f * ONE),
        (int) (0.798635510047293f * ONE),
        (int) (0.788010753606722f * ONE),
        (int) (0.777145961456971f * ONE),
        (int) (0.766044443118978f * ONE),
        (int) (0.754709580222772f * ONE),
        (int) (0.743144825477394f * ONE),
        (int) (0.731353701619171f * ONE),
        (int) (0.719339800338651f * ONE),
        (int) (0.707106781186548f * ONE),
        (int) (0.694658370458997f * ONE),
        (int) (0.681998360062499f * ONE),
        (int) (0.669130606358858f * ONE),
        (int) (0.656059028990507f * ONE),
        (int) (0.642787609686539f * ONE),
        (int) (0.629320391049838f * ONE),
        (int) (0.615661475325658f * ONE),
        (int) (0.601815023152048f * ONE),
        (int) (0.587785252292473f * ONE),
        (int) (0.573576436351046f * ONE),
        (int) (0.559192903470747f * ONE),
        (int) (0.544639035015027f * ONE),
        (int) (0.529919264233205f * ONE),
        (int) (0.515038074910054f * ONE),
        (int) (0.5f * ONE),
        (int) (0.484809620246337f * ONE),
        (int) (0.469471562785891f * ONE),
        (int) (0.453990499739547f * ONE),
        (int) (0.438371146789077f * ONE),
        (int) (0.422618261740699f * ONE),
        (int) (0.4067366430758f * ONE),
        (int) (0.390731128489274f * ONE),
        (int) (0.374606593415912f * ONE),
        (int) (0.3583679495453f * ONE),
        (int) (0.342020143325669f * ONE),
        (int) (0.325568154457157f * ONE),
        (int) (0.309016994374948f * ONE),
        (int) (0.292371704722737f * ONE),
        (int) (0.275637355817f * ONE),
        (int) (0.258819045102521f * ONE),
        (int) (0.241921895599668f * ONE),
        (int) (0.224951054343865f * ONE),
        (int) (0.207911690817759f * ONE),
        (int) (0.190808995376545f * ONE),
        (int) (0.17364817766693f * ONE),
        (int) (0.156434465040231f * ONE),
        (int) (0.139173100960066f * ONE),
        (int) (0.121869343405148f * ONE),
        (int) (0.104528463267654f * ONE),
        (int) (0.0871557427476586f * ONE),
        (int) (0.0697564737441255f * ONE),
        (int) (0.0523359562429438f * ONE),
        (int) (0.0348994967025007f * ONE),
        (int) (0.0174524064372834f * ONE),
        (int) (1.22514845490862E-16f * ONE),
        (int) (-0.0174524064372832f * ONE),
        (int) (-0.0348994967025009f * ONE),
        (int) (-0.0523359562429436f * ONE),
        (int) (-0.0697564737441248f * ONE),
        (int) (-0.0871557427476579f * ONE),
        (int) (-0.104528463267653f * ONE),
        (int) (-0.121869343405148f * ONE),
        (int) (-0.139173100960066f * ONE),
        (int) (-0.156434465040231f * ONE),
        (int) (-0.17364817766693f * ONE),
        (int) (-0.190808995376545f * ONE),
        (int) (-0.207911690817759f * ONE),
        (int) (-0.224951054343865f * ONE),
        (int) (-0.241921895599668f * ONE),
        (int) (-0.25881904510252f * ONE),
        (int) (-0.275637355816999f * ONE),
        (int) (-0.292371704722736f * ONE),
        (int) (-0.309016994374948f * ONE),
        (int) (-0.325568154457157f * ONE),
        (int) (-0.342020143325669f * ONE),
        (int) (-0.3583679495453f * ONE),
        (int) (-0.374606593415912f * ONE),
        (int) (-0.390731128489274f * ONE),
        (int) (-0.4067366430758f * ONE),
        (int) (-0.422618261740699f * ONE),
        (int) (-0.438371146789077f * ONE),
        (int) (-0.453990499739546f * ONE),
        (int) (-0.469471562785891f * ONE),
        (int) (-0.484809620246337f * ONE),
        (int) (-0.5f * ONE),
        (int) (-0.515038074910054f * ONE),
        (int) (-0.529919264233205f * ONE),
        (int) (-0.544639035015027f * ONE),
        (int) (-0.559192903470747f * ONE),
        (int) (-0.573576436351046f * ONE),
        (int) (-0.587785252292473f * ONE),
        (int) (-0.601815023152048f * ONE),
        (int) (-0.615661475325658f * ONE),
        (int) (-0.629320391049838f * ONE),
        (int) (-0.642787609686539f * ONE),
        (int) (-0.656059028990507f * ONE),
        (int) (-0.669130606358858f * ONE),
        (int) (-0.681998360062498f * ONE),
        (int) (-0.694658370458997f * ONE),
        (int) (-0.707106781186547f * ONE),
        (int) (-0.719339800338651f * ONE),
        (int) (-0.73135370161917f * ONE),
        (int) (-0.743144825477394f * ONE),
        (int) (-0.754709580222772f * ONE),
        (int) (-0.766044443118978f * ONE),
        (int) (-0.777145961456971f * ONE),
        (int) (-0.788010753606722f * ONE),
        (int) (-0.798635510047293f * ONE),
        (int) (-0.809016994374947f * ONE),
        (int) (-0.819152044288992f * ONE),
        (int) (-0.829037572555041f * ONE),
        (int) (-0.838670567945424f * ONE),
        (int) (-0.848048096156426f * ONE),
        (int) (-0.857167300702112f * ONE),
        (int) (-0.866025403784438f * ONE),
        (int) (-0.874619707139396f * ONE),
        (int) (-0.882947592858927f * ONE),
        (int) (-0.891006524188368f * ONE),
        (int) (-0.898794046299167f * ONE),
        (int) (-0.90630778703665f * ONE),
        (int) (-0.913545457642601f * ONE),
        (int) (-0.92050485345244f * ONE),
        (int) (-0.927183854566787f * ONE),
        (int) (-0.933580426497202f * ONE),
        (int) (-0.939692620785908f * ONE),
        (int) (-0.945518575599317f * ONE),
        (int) (-0.951056516295154f * ONE),
        (int) (-0.956304755963035f * ONE),
        (int) (-0.961261695938319f * ONE),
        (int) (-0.965925826289068f * ONE),
        (int) (-0.970295726275996f * ONE),
        (int) (-0.974370064785235f * ONE),
        (int) (-0.978147600733806f * ONE),
        (int) (-0.981627183447664f * ONE),
        (int) (-0.984807753012208f * ONE),
        (int) (-0.987688340595138f * ONE),
        (int) (-0.99026806874157f * ONE),
        (int) (-0.992546151641322f * ONE),
        (int) (-0.994521895368273f * ONE),
        (int) (-0.996194698091746f * ONE),
        (int) (-0.997564050259824f * ONE),
        (int) (-0.998629534754574f * ONE),
        (int) (-0.999390827019096f * ONE),
        (int) (-0.999847695156391f * ONE),
        (int) (-1f * ONE),
        (int) (-0.999847695156391f * ONE),
        (int) (-0.999390827019096f * ONE),
        (int) (-0.998629534754574f * ONE),
        (int) (-0.997564050259824f * ONE),
        (int) (-0.996194698091746f * ONE),
        (int) (-0.994521895368273f * ONE),
        (int) (-0.992546151641322f * ONE),
        (int) (-0.99026806874157f * ONE),
        (int) (-0.987688340595138f * ONE),
        (int) (-0.984807753012208f * ONE),
        (int) (-0.981627183447664f * ONE),
        (int) (-0.978147600733806f * ONE),
        (int) (-0.974370064785235f * ONE),
        (int) (-0.970295726275997f * ONE),
        (int) (-0.965925826289068f * ONE),
        (int) (-0.961261695938319f * ONE),
        (int) (-0.956304755963035f * ONE),
        (int) (-0.951056516295154f * ONE),
        (int) (-0.945518575599317f * ONE),
        (int) (-0.939692620785909f * ONE),
        (int) (-0.933580426497202f * ONE),
        (int) (-0.927183854566787f * ONE),
        (int) (-0.92050485345244f * ONE),
        (int) (-0.913545457642601f * ONE),
        (int) (-0.90630778703665f * ONE),
        (int) (-0.898794046299167f * ONE),
        (int) (-0.891006524188368f * ONE),
        (int) (-0.882947592858927f * ONE),
        (int) (-0.874619707139396f * ONE),
        (int) (-0.866025403784439f * ONE),
        (int) (-0.857167300702112f * ONE),
        (int) (-0.848048096156426f * ONE),
        (int) (-0.838670567945424f * ONE),
        (int) (-0.829037572555042f * ONE),
        (int) (-0.819152044288992f * ONE),
        (int) (-0.809016994374948f * ONE),
        (int) (-0.798635510047293f * ONE),
        (int) (-0.788010753606722f * ONE),
        (int) (-0.777145961456971f * ONE),
        (int) (-0.766044443118978f * ONE),
        (int) (-0.754709580222772f * ONE),
        (int) (-0.743144825477395f * ONE),
        (int) (-0.731353701619171f * ONE),
        (int) (-0.719339800338652f * ONE),
        (int) (-0.707106781186548f * ONE),
        (int) (-0.694658370458998f * ONE),
        (int) (-0.681998360062498f * ONE),
        (int) (-0.669130606358858f * ONE),
        (int) (-0.656059028990507f * ONE),
        (int) (-0.64278760968654f * ONE),
        (int) (-0.629320391049838f * ONE),
        (int) (-0.615661475325659f * ONE),
        (int) (-0.601815023152048f * ONE),
        (int) (-0.587785252292473f * ONE),
        (int) (-0.573576436351046f * ONE),
        (int) (-0.559192903470747f * ONE),
        (int) (-0.544639035015027f * ONE),
        (int) (-0.529919264233206f * ONE),
        (int) (-0.515038074910054f * ONE),
        (int) (-0.5f * ONE),
        (int) (-0.484809620246337f * ONE),
        (int) (-0.469471562785891f * ONE),
        (int) (-0.453990499739547f * ONE),
        (int) (-0.438371146789077f * ONE),
        (int) (-0.4226182617407f * ONE),
        (int) (-0.4067366430758f * ONE),
        (int) (-0.390731128489275f * ONE),
        (int) (-0.374606593415912f * ONE),
        (int) (-0.358367949545301f * ONE),
        (int) (-0.342020143325669f * ONE),
        (int) (-0.325568154457158f * ONE),
        (int) (-0.309016994374948f * ONE),
        (int) (-0.292371704722736f * ONE),
        (int) (-0.275637355817f * ONE),
        (int) (-0.258819045102521f * ONE),
        (int) (-0.241921895599668f * ONE),
        (int) (-0.224951054343865f * ONE),
        (int) (-0.20791169081776f * ONE),
        (int) (-0.190808995376545f * ONE),
        (int) (-0.173648177666931f * ONE),
        (int) (-0.156434465040231f * ONE),
        (int) (-0.139173100960066f * ONE),
        (int) (-0.121869343405148f * ONE),
        (int) (-0.104528463267653f * ONE),
        (int) (-0.0871557427476583f * ONE),
        (int) (-0.0697564737441248f * ONE),
        (int) (-0.0523359562429444f * ONE),
        (int) (-0.0348994967025008f * ONE),
        (int) (-0.0174524064372844f * ONE),
    };
    
    /** Table of CRCs of all 8-bit messages. */
    private static int crc_table[];
   
    /** Make the table for a fast CRC. */
    private static void make_crc_table() {
        crc_table = new int[256];
        long c;
        int n, k;
   
        for (n = 0; n < 256; n++) {
            c = n;
            for (k = 0; k < 8; k++) {
                if ((c & 1) != 0)
                    c = 0xedb88320L ^ (c >> 1);
                else
                    c = c >> 1;
            }
            crc_table[n] = (int) c;
        }
    }
   
    /** Update a running CRC with the bytes buf[0..len-1]--the CRC
     * should be initialized to all 1's, and the transmitted value
     * is the 1's complement of the final running CRC (see the
     * crc() routine below)). */
    private static int update_crc(long crc, byte[] buf, int offset, int len) {
        long c = crc;
        int n, end;
   
        if (crc_table == null)
            make_crc_table();
        
        for (n = offset, end = offset + len; n < end; n++) {
            c = (((long) crc_table[(int) ((c ^ buf[n]) & 0xff)]) & 0xffffffffL) ^ (c >> 8);
        }
        return (int) c;
    }
   
    /** Return the CRC of the bytes buf[0..len-1]. */
    public static int crc(byte[] buf, int offset, int len) {
        return update_crc(0xffffffffL, buf, offset, len) ^ 0xffffffff;
    }
}
