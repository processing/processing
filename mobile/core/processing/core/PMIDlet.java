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
    public static final int mul(int value1, int value2) {
        return (value1 * value2) >> FP_PRECISION;
    }
    
    /** Returns the fixed point quotient from dividing the fixed point dividend by the fixed point divisor. */
    public static final int div(int dividend, int divisor) {
        return (dividend << FP_PRECISION) / divisor;
    }
    
    /** Returns the fixed point representation of the specified integer value. */
    public static final int itofp(int value1) {
        return value1 << FP_PRECISION;
    }
    
    /** Returns the closest integer value less than or equal to the specified fixed point value. */
    public static final int floor(int value1) {
        return value1 >> FP_PRECISION;
    }
    
    /** Returns the sin of the specified fixed-point radian angle as a fixed point value. */
    public static final int sin(int rad) {
        //// convert to degrees
        int index = rad * 180 / PI % 360;
        return sin[index];
    }
    
    /** Returns the cos of the specified fixed-point radian angle as a fixed point value. */
    public static final int cos(int rad) {
        //// convert to degrees
        int index = (rad * 180 / PI + 90) % 360;
        return sin[index];
    }
    
    /** Lookup table for sin function, indexed by degrees. */
    private static final int[] sin = {
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
}
