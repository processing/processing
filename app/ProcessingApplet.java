import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;


public class ProcessingApplet extends Applet 
  implements BagelConstants, Runnable, 
	     MouseListener, MouseMotionListener, KeyListener {
  public Bagel g;

  public int mouseX, mouseY;
  public boolean mousePressed;
  boolean mousePressedBriefly;

  public int key;
  public boolean keyPressed;
  boolean keyPressedBriefly;

  boolean timing;
  public int millis;
  long actualMillis;
  long millisOffset;
  Calendar calendar;

  boolean drawMethod;
  boolean loopMethod;

  boolean finished;
  boolean drawn;
  Thread thread;

  int width, height;

  //PrintStream errStream;


  public void init() {
    addMouse();  // set basic params
    addKeyboard();
    addTime();

    finished = false; // just for clarity
    drawn = false;

    // this will be cleared by loop() if it is not overridden
    drawMethod = true;
    loopMethod = true;

    // call setup for changed params
    setup();

    // do actual setup calls
    if (g == null) {
      // if programmer hasn't added a special graphics
      // object, then setup a standard 320x240 one
      size(320, 240);
    }
  }


  public void start() {
    thread = new Thread(this);
    thread.start();
  }


  // maybe start should also be used as the method for kicking
  // the thread on, instead of doing it inside paint()
  public void stop() {
    if (thread != null) {
      thread.stop();
      thread = null;
    }

    // kill off any associated threads
    Thread threads[] = new Thread[Thread.activeCount()];
    Thread.enumerate(threads);
    for (int i = 0; i < threads.length; i++) {
      if (threads[i].getName().indexOf("Thread-") == 0) {
	//System.out.println("stopping " + threads[i].getName());
	threads[i].stop();
      }
    }
  }


  // ------------------------------------------------------------


  void setup() {
  }


  void draw() {
    drawMethod = false;
  }


  void loop() {
    loopMethod = false;
  }


  // ------------------------------------------------------------


  // this is where screen grab could attach itself
  public void update() {
    Graphics g = this.getGraphics();
    if (g != null) paint(g);
  }

  public void update(Graphics screen) {
    paint(screen);
  }

  public void paint(Graphics screen) {
    /*
    if ((thread == null) && !finished) {
      // kickstart my heart
      thread = new Thread(this);
      thread.start();

    } else {
      if (screen == null) System.out.println("screen is null");
      if (g == null) System.out.println("g is null");
      screen.drawImage(g.image, 0, 0, null);
    }
    */
    if (screen == null) 
      System.out.println("ProcessinApplet.paint screen is null");
    if (g == null) 
      System.out.println("ProcessinApplet.paint g is null");
    screen.drawImage(g.image, 0, 0, null);
  }


  public void run() {
    while ((Thread.currentThread() == thread) && !finished) {

      if (timing) {
	actualMillis = System.currentTimeMillis();
	calendar = null;
      }

      // attempt to draw a static image using draw()
      if (!drawn) {
	// always do this once. empty if not overridden
	g.beginFrame();
	draw();
	if (!drawMethod) {
	  // that frame was bogus, mark it as such
	  // before ending the frame so that it doesn't get
	  // saved to a quicktime movie or whatever

	  // might be as simple as not calling endFrame?
	}
	if (drawMethod) {
	  g.endFrame();
	  update();
	  finished = true;
	}
	drawn = true;
      }

      // if not a static app, run the loop
      if (!drawMethod) {
	g.beginFrame();
	loop();
	g.endFrame();
	update();
      }

      // takedown
      if (!loopMethod) {
	finished = true;
      }

      if (mousePressedBriefly) {
	mousePressedBriefly = false;
	mousePressed = false;
      }

      if (keyPressedBriefly) {
	keyPressedBriefly = false;
	keyPressed = false;
      }

      /*
	// with any luck, kjcengine will be listening
	// and slurp this right up
	} catch (Exception e) {  
	//System.out.println("exception caught in run");
	//System.err.println("i'm here in err");
	if (errStream != null) {
	errStream.print("MAKE WAY");
	e.printStackTrace(errStream);
	} else {
	e.printStackTrace();
	}
	}
      */

      // sleep to make OS happy
      try {
	thread.sleep(5);
      } catch (InterruptedException e) { }
    }
  }


  // ------------------------------------------------------------


  public void size(int width, int height) {
    if (g != null) return; // would this ever happen?

    this.width = width;
    this.height = height;

    g = new Bagel(width, height);

    // do all the defaults down here, because
    // subclasses need to go through this function
    g.lighting = false;
  }


  // ------------------------------------------------------------


  public void addMouse() {
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public void mouseClicked(MouseEvent e) { 
    mousePressedBriefly = true;
    mousePressed = true;
  } 

  public void mousePressed(MouseEvent e) { 
    mousePressedBriefly = false;
    mousePressed = true;
  }

  public void mouseReleased(MouseEvent e) { 
    mousePressed = false;
  }

  public void mouseEntered(MouseEvent e) { }

  public void mouseExited(MouseEvent e) { }

  public void mouseDragged(MouseEvent e) { 
    mouseX = e.getX(); mouseY = e.getY();
    mousePressed = true;
  }

  public void mouseMoved(MouseEvent e) { 
    mouseX = e.getX(); mouseY = e.getY();
    mousePressed = false;
  }


  // ------------------------------------------------------------


  public void addKeyboard() {
    addKeyListener(this);
  }

  public void keyTyped(KeyEvent e) { 
    keyPressed = true;
    keyPressedBriefly = true;
    key = e.getKeyChar();
  }

  public void keyPressed(KeyEvent e) { 
    keyPressed = true;
    keyPressedBriefly = false;
    key = e.getKeyChar();
  }

  public void keyReleased(KeyEvent e) { 
    keyPressed = false;
    key = e.getKeyChar();
  }


  // ------------------------------------------------------------


  public void addTime() {
    timing = true;
    //calendar = Calendar.getInstance();
    //calendar.setTimeZone(TimeZone.getDefault());
    millisOffset = System.currentTimeMillis();
  }

  // at the expense of dealing with longs.. hmm..
  public int getMillis() {
    return (int) (actualMillis - millisOffset);
  }

  public int getSecond() {
    if (calendar == null) calendar = Calendar.getInstance();
    return calendar.get(Calendar.SECOND);
  }

  public int getMinute() {
    if (calendar == null) calendar = Calendar.getInstance();
    return calendar.get(Calendar.MINUTE);
  }

  public int getHour() {
    if (calendar == null) calendar = Calendar.getInstance();
    return calendar.get(Calendar.HOUR_OF_DAY);
  }

  // if users want day of week or day of year,
  // they can add their own functions
  public int getDay() {
    if (calendar == null) calendar = Calendar.getInstance();
    return calendar.get(Calendar.DAY_OF_MONTH);    
  }

  public int getMonth() {
    if (calendar == null) calendar = Calendar.getInstance();
    // months are number 0..11 so change to colloquial 1..12
    return calendar.get(Calendar.MONTH) + 1;
  }

  public int getYear() {
    if (calendar == null) calendar = Calendar.getInstance();
    //calendar.setTimeInMillis(actualMillis);
    return calendar.get(Calendar.YEAR);
  }

  public void delay(int howlong) {
    long stop = System.currentTimeMillis() + (long)howlong;
    while (System.currentTimeMillis() < stop) { }
    return;
  }


  // ------------------------------------------------------------


  public void print(boolean what) {
    System.out.print(what);
  }

  public void print(int what) {
    System.out.print(what);
  }

  public void print(float what) {
    System.out.print(what);
  }

  public void print(String what) {
    System.out.print(what);
  }

  public void println(boolean what) {
    print(what); System.out.println();
  }

  public void println(int what) {
    print(what); System.out.println();
  }

  public void println(float what) {
    print(what); System.out.println();
  }

  public void println(String what) {
    print(what); System.out.println();
  }

  public void println() {
    System.out.println();
  }

  // would be nice to have a way to write messages to the
  // console (whether in the browser or the environment)
  // this might be part of adding an AppletContext to the
  // environment. 


  // ------------------------------------------------------------


  // these functions are really slow, but easy to use
  // if folks are advanced enough to want something faster, 
  // they can write it themselves (not difficult)

  public float getRed(int x, int y) {
    if ((x < 0) || (x > g.width1) || (y < 0) || (y > g.height1)) {
      return (g.backR * g.colorMaxX);
    }
    int r1 = (g.pixels[y*width + x] >> 16) & 0xff;
    return g.colorMaxX * ((float)r1 / 255.0f);
  }

  public float getGreen(int x, int y) {
    if ((x < 0) || (x > g.width1) || (y < 0) || (y > g.height1)) {
      return (g.backR * g.colorMaxX);
    }
    int g1 = (g.pixels[y*width + x] >> 8) & 0xff;
    return g.colorMaxX * ((float)g1 / 255.0f);
  }

  public float getBlue(int x, int y) {
    if ((x < 0) || (x > g.width1) || (y < 0) || (y > g.height1)) {
      return (g.backR * g.colorMaxX);
    }
    int b1 = (g.pixels[y*width + x]) & 0xff;
    return g.colorMaxX * ((float)b1 / 255.0f);
  }


  public void setRed(int x, int y, float value) {
    if ((x < 0) || (x > g.width1) || (y < 0) || (y > g.height1)) {
      return;
    }
    if (value < 0) value = 0;
    if (value > g.colorMaxX) value = g.colorMaxX;

    int masked = (g.pixels[y*width + x]) & 0xff00ffff;
    g.pixels[y*width + x] = masked |
      (((int) (255.0f * value / g.colorMaxX)) << 16);
  }

  public void setGreen(int x, int y, float value) {
    if ((x < 0) || (x > g.width1) || (y < 0) || (y > g.height1)) {
      return;
    }
    if (value < 0) value = 0;
    if (value > g.colorMaxY) value = g.colorMaxY;

    int masked = (g.pixels[y*width + x]) & 0xffff00ff;
    g.pixels[y*width + x] = masked |
      (((int) (255.0f * value / g.colorMaxY)) << 8);
  }

  public void setBlue(int x, int y, float value) {
    if ((x < 0) || (x > g.width1) || (y < 0) || (y > g.height1)) {
      return;
    }
    if (value < 0) value = 0;
    if (value > g.colorMaxZ) value = g.colorMaxZ;

    int masked = (g.pixels[y*width + x]) & 0xffffff00;
    g.pixels[y*width + x] = masked |
      ((int) (255.0f * value / g.colorMaxZ));
  }


  /*
  public float getHue(int x, int y) {
  }

  public float getSaturation(int x, int y) {
  }

  public float getBrightness(int x, int y) {
  }


  public void setHue(int x, int y, float h) {
  }

  public void setSaturation(int x, int y, float s) {
  }

  public void setBrightness(int x, int y, float b) {
  }


  public float getGray(int x, int y) {
  }

  public void setGray(int x, int y, float value) {
  }
  */


  // ------------------------------------------------------------


  public void rotateX(float angle) {
    g.rotate(angle, 1, 0, 0);
  }

  public void rotateY(float angle) {
    g.rotate(angle, 0, 1, 0);
  }

  public void rotateZ(float angle) {
    g.rotate(angle, 0, 0, 1);
  }


  // ------------------------------------------------------------

  // math stuff for convenience

  
  public final float sin(float angle) {
    return (float)Math.sin(angle);
  }

  public final float cos(float angle) {
    return (float)Math.cos(angle);
  }

  public final float tan(float angle) {
    return (float)Math.tan(angle);
  }

  public final float atan2(float a, float b) {
    return (float)Math.atan2(a, b);
  }


  public final float sq(float a) {
    return a*a;
  }

  public final float sqrt(float a) {
    return (float)Math.sqrt(a);
  }

  public final float pow(float a, float b) {
    return (float)Math.pow(a, b);
  }

  public final float abs(float n) {
    return (n < 0) ? -n : n;
  }


  public final float max(float a, float b) {
    return Math.max(a, b);
  }

  public final float max(float a, float b, float c) {
    return Math.max(a, Math.max(b, c));
  }

  public final float min(float a, float b) {
    return Math.min(a, b);
  }

  public final float min(float a, float b, float c) {
    return Math.min(a, Math.min(b, c));
  }


  public final float random() {
    return (float)Math.random();
  }


  // ------------------------------------------------------------


  public void beginFrame() {
    g.beginFrame();
  }

  public void endFrame() {
    g.endFrame();
  }

  public void beginShape(int kind) {
    g.beginShape(kind);
  }

  public void textureImage(BagelImage image) {
    g.textureImage(image);
  }

  public void vertexTexture(float u, float v) {
    g.vertexTexture(u, v);
  }

  public void vertexNormal(float nx, float ny, float nz) {
    g.vertexNormal(nx, ny, nz);
  }

  public void vertex(float x, float y) {
    g.vertex(x, y);
  }

  public void vertex(float x, float y, float z) {
    g.vertex(x, y, z);
  }

  public void endShape() {
    g.endShape();
  }

  public void point(float x, float y) {
    g.point(x, y);
  }

  public void point(float x, float y, float z) {
    g.point(x, y, z);
  }

  public void line(float x1, float y1, float x2, float y2) {
    g.line(x1, y1, x2, y2);
  }

  public void line(float x1, float y1, float z1, 
		   float x2, float y2, float z2) {
    g.line(x1, y1, z1, x2, y2, z2);
  }

  public void bezierCurve(float x1, float y1, float x2, float y2, 
			  float x3, float y3, float x4, float y4) {
    g.bezierCurve(x1, y1, x2, y2, x3, y3, x4, y4);
  }

  public void drawBezier(float inX[], float inY[]) {
    g.drawBezier(inX, inY);
  }

  public void catmullRomCurve(float x1, float y1, 
			      float cx1, float cy1,
			      float cx2, float cy2, 
			      float x2, float y2) {
    g.catmullRomCurve(x1, y1, cx1, cy1, cx2, cy2, x2, y2);
  }

  public void rect(float x1, float y1, float x2, float y2) {
    g.rect(x1, y1, x2, y2);
  }

  public void cube(float size) {
    g.cube(size);
  }

  public void box(float x1, float y1, float z1,
		  float x2, float y2, float z2) {
    g.box(x1, y1, z1, x2, y2, z2);
  }

  public void circle(float x, float y, float radius) {
    g.circle(x, y, radius);
  }

  public void sphere(float radius) {
    g.sphere(radius);
  }

  public void potato() {
    g.potato();
  }

  public void imageRect(float x1, float y1, float x2, float y2,
			BagelImage image) {
    g.imageRect(x1, y1, x2, y2, image);
  }

  public void imageRect(float x1, float y1, float x2, float y2,
			BagelImage image, float maxU, float maxV) {
    g.imageRect(x1, y1, x2, y2, image, maxU, maxV);
  }

  public void loadIdentityMatrix() {
    g.loadIdentityMatrix();
  }

  public void push() {
    g.push();
  }

  public void pop() {
    g.pop();
  }

  public void multMatrix(float n00, float n01, float n02, float n03,
			 float n10, float n11, float n12, float n13,
			 float n20, float n21, float n22, float n23,
			 float n30, float n31, float n32, float n33) {
    g.multMatrix(n00, n01, n02, n03, n10, n11, n12, n13, n20, n21, n22, n23, n30, n31, n32, n33);
  }

  public void setupProjection(int projectionMode) {
    g.setupProjection(projectionMode);
  }

  public void projectPoint(float x, float y, float z) {
    g.projectPoint(x, y, z);
  }

  public void translate(float tx, float ty, float tz) {
    g.translate(tx, ty, tz);
  }

  public void rotate(float angle, float v0, float v1, float v2) {
    g.rotate(angle, v0, v1, v2);
  }

  public void scale(float xyz) {
    g.scale(xyz);
  }

  public void scale(float x, float y, float z) {
    g.scale(x, y, z);
  }

  public void multWithPerspective() {
    g.multWithPerspective();
  }

  public void colorMode(int colorMode) {
    g.colorMode(colorMode);
  }

  public void colorMode(int colorMode, int max) {
    g.colorMode(colorMode, max);
  }

  public void colorMode(int colorMode, 
			int maxX, int maxY, int maxZ, int maxA) {
    g.colorMode(colorMode, maxX, maxY, maxZ, maxA);
  }

  public void colorScale(int max) {
    g.colorScale(max);
  }

  public void colorScale(int maxX, int maxY, int maxZ, int maxA) {
    g.colorScale(maxX, maxY, maxZ, maxA);
  }

  public void noFill() {
    g.noFill();
  }

  public void fill(float gray) {
    g.fill(gray);
  }

  public void fill(float gray, float alpha) {
    g.fill(gray, alpha);
  }

  public void fill(float x, float y, float z) {
    g.fill(x, y, z);
  }

  public void fill(float x, float y, float z, float a) {
    g.fill(x, y, z, a);
  }

  public void strokeWidth(float strokeWidth) {
    g.strokeWidth(strokeWidth);
  }

  public void noStroke() {
    g.noStroke();
  }

  public void stroke(float gray) {
    g.stroke(gray);
  }

  public void stroke(float gray, float alpha) {
    g.stroke(gray, alpha);
  }

  public void stroke(float x, float y, float z) {
    g.stroke(x, y, z);
  }

  public void stroke(float x, float y, float z, float a) {
    g.stroke(x, y, z, a);
  }

  public void clear() {
    g.clear();
  }

  public void noBackground() {
    g.noBackground();
  }

  public void background(float gray) {
    g.background(gray);
  }

  public void background(float x, float y, float z) {
    g.background(x, y, z);
  }

  public void lightsOn() {
    g.lightsOn();
  }

  public void lightsOff() {
    g.lightsOff();
  }

  public void message(int level, String message) {
    g.message(level, message);
  }

  public void message(int level, String message, Exception e) {
    g.message(level, message, e);
  }

}
