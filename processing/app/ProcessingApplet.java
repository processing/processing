import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;


public class ProcessingApplet extends Applet 
  implements BagelConstants, Runnable, 
	     MouseListener, MouseMotionListener, KeyListener {
  Bagel g;

  int mouseX, mouseY;
  boolean mousePressed;
  boolean mousePressedBriefly;

  int key;
  boolean keyPressed;
  boolean keyPressedBriefly;

  boolean timing;
  int millis;
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


  public void setup() {
  }


  public void draw() {
    drawMethod = false;
  }


  public void loop() {
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
}
