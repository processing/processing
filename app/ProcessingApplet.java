import java.applet.*;
import java.awt.*;
import java.awt.event.*;
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
    paint(this.getGraphics());
  }

  public void update(Graphics screen) {
    paint(screen);
  }

  public void paint(Graphics screen) {
    if (thread == null) {
      // kickstart my heart
      thread = new Thread(this);
      thread.start();

    } else {
      screen.drawImage(g.image, 0, 0, null);
    }
  }


  public void run() {
    while ((Thread.currentThread() == thread) && !finished) {

      // setup
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
    //calendar.setTimeInMillis(actualMillis);
    if (calendar == null) calendar = Calendar.getInstance();
    return calendar.get(Calendar.SECOND);
  }

  public int getMinute() {
    if (calendar == null) calendar = Calendar.getInstance();
    //calendar.setTimeInMillis(actualMillis);
    return calendar.get(Calendar.MINUTE);
  }

  public int getHour() {
    if (calendar == null) calendar = Calendar.getInstance();
    //calendar.setTimeInMillis(actualMillis);
    return calendar.get(Calendar.HOUR_OF_DAY);
  }

  public int getMonth() {
    if (calendar == null) calendar = Calendar.getInstance();
    //calendar.setTimeInMillis(actualMillis);
    return calendar.get(Calendar.MONTH);
  }

  public int getYear() {
    if (calendar == null) calendar = Calendar.getInstance();
    //calendar.setTimeInMillis(actualMillis);
    return calendar.get(Calendar.YEAR);
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

  public void cube(float size) {  // radius*2
    g.cube(size);
  }

  public void box(float x1, float y1, float z1,
		  float x2, float y2, float z2) {
    g.box(x1, y1, z1, x2, y2, z2);
  }

  public void sphere(float radius) {
    g.sphere(radius);
  }

  public void potato() {  // my teapot equivalent
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

  public void noBackground() {
    g.noBackground();
  }

  public void background(float gray) {
    g.background(gray);
  }

  public void background(float x, float y, float z) {
    g.background(x, y, z);
  }

  public void message(int level, String message) {
    g.message(level, message);
  }

  public void message(int level, String message, Exception e) {
    g.message(level, message, e);
  }

}
