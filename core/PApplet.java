/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PApplet - applet base class for the bagel engine
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.text.*;
import java.util.*;


public class PApplet extends Applet
  implements PConstants, Runnable,
       MouseListener, MouseMotionListener, KeyListener
{
  static final double jdkVersion = 
    toDouble(System.getProperty("java.version").substring(0,3));

  public PGraphics g;

  static final boolean THREAD_DEBUG = false; //true;

  public int pixels[];

  public int mouseX, mouseY;
  public int pmouseX, pmouseY;
  boolean firstMouseEvent;

  public boolean mousePressed;
  public MouseEvent mouseEvent;

  public int key;
  //public int keyCode;
  public boolean keyPressed;
  public KeyEvent keyEvent;

  long millisOffset;

  // getting the frame rate
  protected float fps = 10;
  protected long fpsLastMillis = 0;

  // setting the frame rate
  protected long fpsLastDelayTime = 0;
  protected float fpsTarget = 0;

  boolean drawMethod;
  boolean loopMethod;

  // true if inside the loop method
  boolean insideLoop;

  // used for mouse tracking so that pmouseX doesn't get
  // updated too many times while still inside the loop
  // instead, it's updated only before/after the loop()
  int qmouseX, qmouseY;

  // queue for whether to call the simple mouseDragged or
  // mouseMoved functions. these are called after beginFrame
  // but before loop() is called itself, to avoid problems
  // in synchronization.
  boolean qmouseDragged;
  boolean qmouseMoved;

  // used to set pmouseX/Y to mouseX/Y the first time
  // mouseX/Y are used, otherwise pmouseX/Y would always
  // be zero making a big jump. yech.
  boolean firstFrame;

  // current frame number (could this be used to replace firstFrame?)
  public int frame;

  // true if the feller has spun down
  public boolean finished;

  boolean drawn;
  Thread thread;

  public Exception exception; // the last exception thrown

  static public final int DEFAULT_WIDTH = 100;
  static public final int DEFAULT_HEIGHT = 100;
  public int width, height;

  int libraryCount;
  PLibrary libraries[]; 

  // this text isn't seen unless PApplet is used on its
  // own and someone takes advantage of leechErr.. not likely
  static public final String LEECH_WAKEUP = "Error while running applet.";
  public PrintStream leechErr;

  // message to send if attached as an external vm
  static public final String EXTERNAL_FLAG = "--external=";
  static public final char EXTERNAL_EXACT_LOCATION = 'e';
  static public final char EXTERNAL_STOP = 's';
  static public final String EXTERNAL_QUIT = "__QUIT__";
  static public final String EXTERNAL_MOVE = "__MOVE__";
  boolean externalRuntime;


  public void init() {
    //checkParams();

    // can/may be resized later
    //g = new PGraphics(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    initg();

    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
    //timing = true;
    millisOffset = System.currentTimeMillis();

    finished = false; // just for clarity
    drawn = false;
    firstFrame = true;

    // this will be cleared by loop() if it is not overridden
    drawMethod = true;
    loopMethod = true;
    firstMouseEvent = true;

    /*
    // call setup for changed params
    try {
      setup();
    } catch (NullPointerException e) {
      //e.printStackTrace();
      // this is probably because someone didn't call size() first
      size(DEFAULT_WIDTH, DEFAULT_HEIGHT);
      setup();
    }

    // do actual setup calls
    if (g == null) {
      // if programmer hasn't added a special graphics
      // object, then setup a standard one
      size(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    */

    // call the applet's setup method
    setup();

    // these are the same things as get run inside a call to size()
    this.pixels = g.pixels;
    this.width = g.width;
    this.height = g.height;
    g.applet = this;
  }


  // override for subclasses (i.e. opengl)
  // so that init() doesn't have to be replicated
  public void initg() {
    g = new PGraphics(DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }


  public void start() {
    //System.out.println("PApplet.start");

    thread = new Thread(this);
    thread.start();
  }


  // maybe start should also be used as the method for kicking
  // the thread on, instead of doing it inside paint()
  public void stop() {
    if (thread != null) {
      //thread.stop();
      thread = null;
    }

    // DEPRECATED as of jdk 1.2.. ugh.. will this work?
    /*
    // kill off any associated threads
    Thread threads[] = new Thread[Thread.activeCount()];
    Thread.enumerate(threads);
    for (int i = 0; i < threads.length; i++) {
      // sometimes these get killed off before i can get 'em
      if (threads[i] == null) continue;

      if (threads[i].getName().indexOf("Thread-") == 0) {
        //System.out.println("stopping " + threads[i].getName());
        threads[i].stop();
      }
    }
    */

    for (int i = 0; i < libraryCount; i++) {
      libraries[i].stop();  // endNet/endSerial etc
    }
  }


  public Dimension getPreferredSize() {
    return new Dimension(width, height);
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


  public void size(int iwidth, int iheight) {
    //if (g != null) return; // would this ever happen? is this a good idea?
    g.resize(iwidth, iheight);

    this.pixels = g.pixels;
    this.width = g.width;
    this.height = g.height;

    // set this here, and if not inside browser, getDocumentBase()
    // will fail with a NullPointerException, and cause applet to
    // be set to null. might be a better way to deal with that, but..
    g.applet = this;
  }


  boolean updated = false;

  public void update() {
    if (firstFrame) firstFrame = false; 

    if (THREAD_DEBUG) println("   3a update() internal " + firstFrame);
    repaint();
    if (THREAD_DEBUG) println("   3b update() internal " + firstFrame);
    getToolkit().sync();  // force repaint now (proper method)
    if (THREAD_DEBUG) println("   3c update() internal " + firstFrame);
  }

  public void update(Graphics screen) {
    if (THREAD_DEBUG) println("    4 update() external");
    paint(screen);
  }

  synchronized public void paint(Graphics screen) {
    if (THREAD_DEBUG) println("     5a enter paint");

    // ignore the very first call to paint, since it's coming
    // from the o.s., and the applet will soon update itself anyway.
    if (firstFrame) return;

    // without ignoring the first call, the first several frames
    // are confused because paint() gets called in the midst of 
    // the initial nextFrame() call, so there are multiple
    // updates fighting with one another.

    // g.image is synchronized so that draw/loop and paint don't
    // try to fight over it. this was causing a randomized slowdown
    // that would cut the framerate into a third on macosx,
    // and is probably related to the windows sluggishness bug too
    if (THREAD_DEBUG) println("     5b enter paint sync");

    synchronized (g) {
      //System.out.println("5b paint has sync");
      //Exception e = new Exception();
      //e.printStackTrace();

      // moving this into PGraphics caused weird sluggishness on win2k
      //g.mis.newPixels(pixels, g.cm, 0, width); // must call this

      // make sure the screen is visible and usable
      if (g != null) {
        screen.drawImage(g.image, 0, 0, null);
      }
      //System.out.println("      6 exit paint");
    }
    updated = true;
  }


  public void run() {
    try {
      while ((Thread.currentThread() == thread) && !finished) {
        updated = false;

        if (PApplet.THREAD_DEBUG) println("nextFrame()");
        nextFrame();

        // moving this to update() (for 0069+) for linux sync problems
        //if (firstFrame) firstFrame = false; 

        // wait for update & paint to happen before drawing next frame
        // this is necessary since the drawing is sometimes in a 
        // separate thread, meaning that the next frame will start 
        // before the update/paint is completed
        while (!updated) { 
          try {
            //Thread.yield();
            // windows doesn't like 'yield', so have to sleep at least
            // for some small amount of time.
            if (PApplet.THREAD_DEBUG) System.out.println("gonna sleep");
            Thread.sleep(1);  // sleep to make OS happy
            if (PApplet.THREAD_DEBUG) System.out.println("outta sleep");
          } catch (InterruptedException e) { }
        }
      }
    } catch (Exception e) {
      // formerly in kjcapplet, now just checks to see
      // if someone wants to leech off errors

      // note that this will not catch errors inside setup()
      // those are caught by the PdeRuntime

      finished = true;

      if (leechErr != null) {
        // if draw() mode, make sure that ui stops waiting
        // and the run button quits out
        leechErr.println(LEECH_WAKEUP);
        e.printStackTrace(leechErr);

      } else {
        System.err.println(LEECH_WAKEUP);
        e.printStackTrace();
      }
    }
  }


  public void nextFrame() {
    mouseX = qmouseX;   // only if updated?
    mouseY = qmouseY;

    if (fpsTarget != 0) framerate_delay();

    // attempt to draw a static image using draw()
    if (!drawn) {
      //synchronized (g.image) {
      synchronized (g) {
        // always do this once. empty if not overridden
        g.beginFrame();
        draw();

        if (drawMethod) {
          g.endFrame();
          update();
          finished = true;
        }
        drawn = true;
      }  // end synch
    }

    // if not a static app, run the loop
    if (!drawMethod) {
      synchronized (g) {
        g.beginFrame();
        //mouseUpdated = false;
        insideLoop = true;
        loop();
        insideLoop = false;

        // these are called *after* loop so that valid
        // drawing commands can be run inside them. it can't
        // be before, since a call to background() would wipe
        // out anything that had been drawn so far.
        if (qmouseMoved) {
          mouseMoved();
          qmouseMoved = false;
        }
        if (qmouseDragged) {
          mouseDragged();
          qmouseDragged = false;
        }
        g.endFrame();
      }  // end synch
      update();
    }

    // takedown
    if (!loopMethod) {
      finished = true;
    }

    pmouseX = mouseX;
    pmouseY = mouseY;

    // internal frame counter
    frame++;
  }


  // ------------------------------------------------------------


  void mouseClicked() { }

  public void mouseClicked(MouseEvent e) {  // can this be removed?
    mouseClicked();
  }

  // needs 'public' otherwise kjc assumes private
  void mousePressed() { } // for beginners

  // this needn't set mouseX/Y
  // since mouseMoved will have already set it
  public void mousePressed(MouseEvent e) {
    mouseEvent = e;
    mousePressed = true;
    mousePressed();
  }

  void mouseReleased() { }  // for beginners

  // this needn't set mouseX/Y
  // since mouseReleased will have already set it
  public void mouseReleased(MouseEvent e) {
    mouseEvent = e;
    mousePressed = false;
    mouseReleased();
  }

  public void mouseEntered(MouseEvent e) { }

  public void mouseExited(MouseEvent e) { }

  void mouseDragged() { } // for beginners

  public void mouseDragged(MouseEvent e) {
    mouseEvent = e;
    qmouseX = e.getX();
    qmouseY = e.getY();
    if (firstMouseEvent) {
      // set valid coordinates for pmouseX/Y so that they don't
      // default to zero which would make them draw from the corner.
      pmouseX = qmouseX;
      pmouseY = qmouseY;
      firstMouseEvent = false;
    }
    mousePressed = true;

    // call mouseDragged() on next trip through loop
    qmouseDragged = true;
    //mouseDragged();
  }

  void mouseMoved() { }   // for beginners

  public void mouseMoved(MouseEvent e) {
    mouseEvent = e;
    qmouseX = e.getX();
    qmouseY = e.getY();
    if (firstMouseEvent) {
      // set valid coordinates for pmouseX/Y so that they don't
      // default to zero which would make them draw from the corner.
      pmouseX = qmouseX;
      pmouseY = qmouseY;
      firstMouseEvent = false;
    }
    mousePressed = false;

    // call mouseMoved() on next trip through loop
    qmouseMoved = true;
    //mouseMoved();
  }


  // ------------------------------------------------------------


  // if the key is a coded key, i.e. up/down/ctrl/shift/alt
  // the 'key' comes through as 0xffff (65535)
  // to make this simpler for beginners, just set the
  // value for key as the code, so that the code looks like:
  // keyPressed() {   if (key == UP) { ... }  }
  // instead of
  // keyPressed() {   if (keyCode = 0xffff) { if (key == UP) { .. } } }
  // or something else more difficult

  void keyTyped() { }

  public void keyTyped(KeyEvent e) {
    keyEvent = e;
    key = e.getKeyChar();
    if (key == 0xffff) key = e.getKeyCode();
    //keyCode = e.getKeyCode();
    keyTyped();
  }

  void keyPressed() { }

  public void keyPressed(KeyEvent e) {
    keyEvent = e;
    keyPressed = true;
    key = e.getKeyChar();
    if (key == 0xffff) key = e.getKeyCode();
    //keyCode = e.getKeyCode();
    keyPressed();
  }

  void keyReleased() { }

  public void keyReleased(KeyEvent e) {
    keyEvent = e;
    keyPressed = false;
    key = e.getKeyChar();
    if (key == 0xffff) key = e.getKeyCode();
    //keyCode = e.getKeyCode();
    keyReleased();
  }


  // ------------------------------------------------------------

  // getting the time


  public int millis() {
    return (int) (System.currentTimeMillis() - millisOffset);
  }

  static public int second() {
    return Calendar.getInstance().get(Calendar.SECOND);
  }

  static public int minute() {
    return Calendar.getInstance().get(Calendar.MINUTE);
  }

  static public int hour() {
    return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
  }

  // if users want day of week or day of year,
  // they can add their own functions
  static public int day() {
    return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
  }

  static public int month() {
    // months are number 0..11 so change to colloquial 1..12
    return Calendar.getInstance().get(Calendar.MONTH) + 1;
  }

  static public int year() {
    return Calendar.getInstance().get(Calendar.YEAR);
  }


  // ------------------------------------------------------------

  // controlling time and playing god


  /**
   * I'm not sure if this is even helpful anymore.
   */
  public void delay(int napTime) {
    if (firstFrame) return;
    if (napTime > 0) {
      try {
        Thread.sleep(napTime);
      } catch (InterruptedException e) { }
    }
  }


  public float framerate() {
    if (fpsLastMillis != 0) {
      float elapsed = (float) (System.currentTimeMillis() - fpsLastMillis);
      if (elapsed != 0) {
        fps = (fps * 0.9f) + ((1.0f / (elapsed / 1000.0f)) * 0.1f);
      }
    }
    fpsLastMillis = System.currentTimeMillis();
    return fps;
  }


  public void framerate(float fpsTarget) {
    this.fpsTarget = fpsTarget;
  }

  protected void framerate_delay() {
    if (fpsLastDelayTime == 0) {
      fpsLastDelayTime = System.currentTimeMillis();
      return;
    }

    long timeToLeave = fpsLastDelayTime + (long)(1000.0f / fpsTarget);
    int napTime = (int) (timeToLeave - System.currentTimeMillis());
    fpsLastDelayTime = timeToLeave;
    delay(napTime);
  }


  // ------------------------------------------------------------


  /**
   * Is the applet online or not? This can be used to test how the 
   * applet should behave since online situations are different.
   */
  public boolean online() {
    try {
      getAppletContext();
    } catch (NullPointerException e) { 
      return false;
    }
    return true;
  }


  /**
   * Get a param from the web page, or (eventually) 
   * from a properties file.
   */
  public String param(String what) {
    if (online()) {
      return getParameter(what);

    } else {
      System.err.println("param() only works inside a web browser");
    }
    return null;
  }


  /**
   * Show status in the status bar of a web browser, or in the
   * System.out console. Eventually this might show status in the
   * p5 environment itself, rather than relying on the console.
   */
  public void status(String what) {
    if (online()) {
      showStatus(what);

    } else {
      System.out.println(what);  // something more interesting?
    }
  }


  /**
   * Link to an external page without all the muss. Currently
   * only works for applets, but eventually should be implemented
   * for applications as well, using code from PdeBase.
   */
  void link(String here) {
    if (!online()) {
      System.err.println("Can't open " + here);
      System.err.println("link() only works inside a web browser");
      return;
    }

    try {
      getAppletContext().showDocument(new URL(here));

    } catch (Exception e) {
      System.err.println("Could not open " + here);
      e.printStackTrace();
    }
  }

  void link(String here, String there) {
    if (!online()) {
      System.err.println("Can't open " + here);
      System.err.println("link() only works inside a web browser");
      return;
    }

    try {
      getAppletContext().showDocument(new URL(here), there);

    } catch (Exception e) {
      System.err.println("Could not open " + here);
      e.printStackTrace();
    }
  }


  public void die(String what) {
    if (online()) {
      System.err.println("i'm dead.. " + what);

    } else {
      System.err.println(what);
      System.exit(1);
    }
  }


  public void die(String what, Exception e) {
    e.printStackTrace();
    die(what);
  }



  // ------------------------------------------------------------

  // SCREEN GRABASS


  /**
   * grab an image of what's currently in the drawing area.
   * best used just before endFrame() at the end of your loop().
   * only creates .tif or .tga images, so if extension isn't specified
   * it defaults to writing a tiff.
   */
  public void saveFrame() {
    if (online()) {
      System.err.println("Can't use saveFrame() when running in a browser.");
      return;
    }

    save("screen-" + nf(frame, 4) + ".tif");
  }


  /**
   * Save the current frame as a .tif or .tga image.
   *
   * The String passed in can contain a series of # signs
   * that will be replaced with the screengrab number. 
   *
   * i.e. saveFrame("blah-####.tif");
   *      // saves a numbered tiff image, replacing the 
   *      // # signs with zeros and the frame number 
   */
  public void saveFrame(String what) {
    if (online()) {
      System.err.println("Can't use saveFrame() when running in a browser.");
      return;
    }

    int first = what.indexOf('#');
    int last = what.lastIndexOf('#');

    if (first == -1) {
      save(what);

    } else {
      String prefix = what.substring(0, first);
      int count = last - first + 1;
      String suffix = what.substring(last + 1);

      save(prefix + nf(frame, count) + suffix);
    }
  }


  // ------------------------------------------------------------

  // CURSOR, base code contributed by amit pitaru


  int cursor_type = ARROW; // cursor type
  boolean cursor_visible = true; // cursor visibility flag
  PImage invisible_cursor;


  /**
   * Set the cursor type
   */
  void cursor(int _cursor_type) {
    //if (cursor_visible && _cursor_type != cursor_type) {
    setCursor(Cursor.getPredefinedCursor(_cursor_type));
    //}
    cursor_visible = true;
    cursor_type = _cursor_type;
  }


  /**
   * Set a custom cursor to an image with a specific hotspot.
   * Only works with JDK 1.2 and later.
   * Currently seems to be broken on Java 1.4 for Mac OS X
   */
  void cursor(PImage image, int hotspotX, int hotspotY) {
    //if (!isOneTwoOrBetter()) {
    if (jdkVersion < 1.2) {
      System.err.println("cursor() error: Java 1.2 or higher is " + 
                         "required to set cursors");
      System.err.println("                (You're using version " + 
                         nf((float) jdkVersion, 1, 1) + ")");
      return;
    }

    // don't set this as cursor type, instead use cursor_type
    // to save the last cursor used in case cursor() is called
    //cursor_type = Cursor.CUSTOM_CURSOR;
    Image jimage =
      createImage(new MemoryImageSource(image.width, image.height,
                                        image.pixels, 0, image.width));

    //Toolkit tk = Toolkit.getDefaultToolkit();
    Point hotspot = new Point(hotspotX, hotspotY);
    try {
      Method mCustomCursor = 
        Toolkit.class.getMethod("createCustomCursor",
                                new Class[] { Image.class, 
                                              Point.class, 
                                              String.class, });
      Cursor cursor = 
        (Cursor)mCustomCursor.invoke(Toolkit.getDefaultToolkit(),
                                     new Object[] { jimage, 
                                                    hotspot, 
                                                    "no cursor" });
      setCursor(cursor);
      cursor_visible = true;

    } catch (NoSuchMethodError e) {
      System.out.println("cursor() is not available on " + 
                         nf((float)jdkVersion, 1, 1));
    } catch (IndexOutOfBoundsException e) {
      System.err.println("cursor() error: the hotspot " + hotspot + 
                         " is out of bounds for the given image.");
    } catch (Exception e) {
      System.err.println(e);
    }
  }


  /**
   * Show the cursor after noCursor() was called.
   * Notice that the program remembers the last set cursor type
   */
  void cursor() {
    // maybe should always set here? seems dangerous, since
    // it's likely that java will set the cursor to something
    // else on its own, and the applet will be stuck b/c bagel
    // thinks that the cursor is set to one particular thing
    if (!cursor_visible) {
      cursor_visible = true;
      setCursor(Cursor.getPredefinedCursor(cursor_type));
    }
  }


  /**
   * Hide the cursor by creating a transparent image
   * and using it as a custom cursor.
   */
  void noCursor() {
    if (!cursor_visible) return;  // don't hide if already hidden.

    if (invisible_cursor == null) {
      //invisible_cursor = new PImage(new int[32*32], 32, 32, RGBA);
      invisible_cursor = new PImage(new int[16*16], 16, 16, RGBA);
    }
    // was formerly 16x16, but the 0x0 was added by jdf as a fix 
    // for macosx, which didn't wasn't honoring the invisible cursor
    cursor(invisible_cursor, 0, 0);
    cursor_visible = false;
  }


  // ------------------------------------------------------------


  static public void print(boolean what) {
    System.out.print(what);
    System.out.flush();
  }

  static public void print(char what) {
    System.out.print(what);
    System.out.flush();
  }

  static public void print(int what) {
    System.out.print(what);
    System.out.flush();
  }

  static public void print(float what) {
    System.out.print(what);
    System.out.flush();
  }

  static public void print(double what) {
    System.out.print(what);
    System.out.flush();
  }

  static public void print(String what) {
    System.out.print(what);
    System.out.flush();
  }

  //static public void print(Object what) {
  //System.out.print(what.toString());
  //System.out.flush();
  //}

  //

  static public void print(boolean what[]) {
    for (int i = 0; i < what.length; i++) System.out.print(what[i]);
    System.out.flush();
  }

  static public void print(char what[]) {
    for (int i = 0; i < what.length; i++) System.out.print(what[i]);
    System.out.flush();
  }

  static public void print(int what[]) {
    for (int i = 0; i < what.length; i++) System.out.print(what[i]);
    System.out.flush();
  }

  static public void print(float what[]) {
    for (int i = 0; i < what.length; i++) System.out.print(what[i]);
    System.out.flush();
  }

  static public void print(double what[]) {
    for (int i = 0; i < what.length; i++) System.out.print(what[i]);
    System.out.flush();
  }

  static public void print(String what[]) {
    for (int i = 0; i < what.length; i++) System.out.print(what[i]);
    System.out.flush();
  }

  //static public void print(Object what[]) {
  //for (int i = 0; i < what.length; i++) System.out.print(what[i]);
  //System.out.flush();
  //}

  //

  static public void println(boolean what) {
    print(what); System.out.println();
  }

  static public void println(char what) {
    print(what); System.out.println();
  }

  static public void println(int what) {
    print(what); System.out.println();
  }

  static public void println(float what) {
    print(what); System.out.println();
  }

  static public void println(double what) {
    print(what); System.out.println();
  }

  static public void println(String what) {
    print(what); System.out.println();
  }

  //static public void println(Object what) {
  //System.out.println(what.toString());
  //}

  static public void println() {
    System.out.println();
  }

  //

  static public void println(boolean what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void println(char what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void println(int what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void println(float what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void println(double what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void println(String what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  //static public void println(Object what[]) {
  //for (int i = 0; i < what.length; i++) System.out.println(what[i]);
  //System.out.flush();
  //}


  // ------------------------------------------------------------

  // math stuff for convenience


  static public final float abs(float n) {
    return (n < 0) ? -n : n;
  }

  static public final int abs(int n) {
    return (n < 0) ? -n : n;
  }

  static public final float sq(float a) {
    return a*a;
  }

  static public final float sqrt(float a) {
    return (float)Math.sqrt(a);
  }

  static public final float pow(float a, float b) {
    return (float)Math.pow(a, b);
  }


  static public final float max(float a, float b) {
    return Math.max(a, b);
  }

  static public final float max(float a, float b, float c) {
    return Math.max(a, Math.max(b, c));
  }

  static public final float min(float a, float b) {
    return Math.min(a, b);
  }

  static public final float min(float a, float b, float c) {
    return Math.min(a, Math.min(b, c));
  }

  static public final float constrain(float amt, float low, float high) {
    return (amt < low) ? low : ((amt > high) ? high : amt);
  }


  static public final int max(int a, int b) {
    return (a > b) ? a : b;
  }

  static public final int max(int a, int b, int c) {
    return (a > b) ? ((a > c) ? a : c) : ((b > c) ? b : c); 
  }

  static public final int min(int a, int b) {
    return (a < b) ? a : b;
  }

  static public final int min(int a, int b, int c) {
    return (a < b) ? ((a < c) ? a : c) : ((b < c) ? b : c); 
  }

  static public final int constrain(int amt, int low, int high) {
    return (amt < low) ? low : ((amt > high) ? high : amt);
  }


  static public final float sin(float angle) {
    return (float)Math.sin(angle);
  }

  static public final float cos(float angle) {
    return (float)Math.cos(angle);
  }

  static public final float tan(float angle) {
    return (float)Math.tan(angle);
  }

  static public final float atan2(float a, float b) {
    return (float)Math.atan2(a, b);
  }


  static public final float degrees(float radians) {
    return radians * RAD_TO_DEG;
  }

  static public final float radians(float degrees) {
    return degrees * DEG_TO_RAD;
  }


  static public final float ceil(float what) {
    return (float) Math.ceil(what);
  }

  static public final float floor(float what) {
    return (float) Math.floor(what);
  }

  static public final float round(float what) {
    return Math.round(what);
  }


  static public final float dist(float x1, float y1, float x2, float y2) {
    return sqrt(sq(x2-x1) + sq(y2-y1));
  }

  static public final float dist(float x1, float y1, float z1,
                          float x2, float y2, float z2) {
    return sqrt(sq(x2-x1) + sq(y2-y1) + sq(z2-z1));
  }


  static Random internalRandom;

  /**
   * Return a random number in the range [0, howbig)
   * (0 is inclusive, non-inclusive of howbig)
   */
  static public final float random(float howbig) {
    // for some reason (rounding error?) Math.random() * 3
    // can sometimes return '3' (once in ~30 million tries)
    // so a check was added to avoid the inclusion of 'howbig'

    // avoid an infinite loop
    if (howbig == 0) return 0;

    // internal random number object
    if (internalRandom == null) internalRandom = new Random();

    float value = 0;
    do {
      //value = (float)Math.random() * howbig;
      value = internalRandom.nextFloat() * howbig;
    } while (value == howbig);
    return value;
  }


  /**
   * Return a random number in the range [howsmall, howbig)
   * (inclusive of howsmall, non-inclusive of howbig)
   * If howsmall is >= howbig, howsmall will be returned,
   * meaning that random(5, 5) will return 5 (useful)
   * and random(7, 4) will return 7 (not useful.. better idea?)
   */
  static public final float random(float howsmall, float howbig) {
    if (howsmall >= howbig) return howsmall;

    float diff = howbig - howsmall;
    return random(diff) + howsmall;

    /*
    if (internalRandom == null) internalRandom = new Random();

    float diff = howbig - howsmall;
    //return howsmall + (float)Math.random() * diff;
    float value = 0;
    do {
      //value = howsmall + (float)Math.random() * diff;
      value = howsmall + internalRandom.nextFloat() * diff;
    } while (value == howbig);  // don't allow inclusive
    return value;
    */
  }


  /*
    Computes the Perlin noise function value at the point (x, y, z).

    [toxi 040903]
    octaves and amplitude amount per octave are now user controlled
    via the noiseDetail() function.

    [toxi 030902]
    cleaned up code and now using bagel's cosine table to speed up

    [toxi 030901]
    implementation by the german demo group farbrausch
    as used in their demo "art": http://www.farb-rausch.de/fr010src.zip

    @param x x coordinate
    @param y y coordinate
    @param z z coordinate
    @return the noise function value at (x, y, z)
  */

  static final int PERLIN_YWRAPB = 4;
  static final int PERLIN_YWRAP = 1<<PERLIN_YWRAPB;
  static final int PERLIN_ZWRAPB = 8;
  static final int PERLIN_ZWRAP = 1<<PERLIN_ZWRAPB;
  static final int PERLIN_SIZE = 4095;

  int perlin_octaves = 4; // default to medium smooth
  float perlin_amp_falloff = 0.5f; // 50% reduction/octave

  // [toxi 031112] 
  // new vars needed due to recent change of cos table in PGraphics
  int perlin_TWOPI, perlin_PI;
  float[] perlin_cosTable;
  float perlin[];


  //public float noise(int x) {
  //return noise((float)x, 0f, 0f);
  //}

  public float noise(float x) {
    // is this legit? it's a dumb way to do it (but repair it later)
    return noise(x, 0f, 0f);
  }

  //public float noise(int x, int y) {
  //return noise((float)x, (float)y, 0f);
  //}

  public float noise(float x, float y) {
    return noise(x, y, 0f);
  }

  //public float noise(int x, int y, int z) {
  //return noise((float)x, (float)y, (float)z);
  //}

  public float noise(float x, float y, float z) {
    if (perlin == null) {
      perlin = new float[PERLIN_SIZE + 1];
      for (int i = 0; i < PERLIN_SIZE + 1; i++) {
        perlin[i] = (float)Math.random();
      }
      // [toxi 031112] 
      // noise broke due to recent change of cos table in PGraphics
      // this will take care of it
      perlin_cosTable=g.cosLUT;
      perlin_TWOPI=perlin_PI=g.SINCOS_LENGTH;
      perlin_PI>>=1;
    }

    if (x<0) x=-x;
    if (y<0) y=-y;
    if (z<0) z=-z;

    int xi=(int)x, yi=(int)y, zi=(int)z;
    float xf = (float)(x-xi);
    float yf = (float)(y-yi);
    float zf = (float)(z-zi);
    float rxf, ryf;

    float r=0;
    float ampl=0.5f;

    float n1,n2,n3;

    for (int i=0; i<perlin_octaves; i++) {
      int of=xi+(yi<<PERLIN_YWRAPB)+(zi<<PERLIN_ZWRAPB);
		
      rxf=noise_fsc(xf);
      ryf=noise_fsc(yf);

      n1  = perlin[of&PERLIN_SIZE];
      n1 += rxf*(perlin[(of+1)&PERLIN_SIZE]-n1);
      n2  = perlin[(of+PERLIN_YWRAP)&PERLIN_SIZE];
      n2 += rxf*(perlin[(of+PERLIN_YWRAP+1)&PERLIN_SIZE]-n2);
      n1 += ryf*(n2-n1);

      of += PERLIN_ZWRAP;
      n2  = perlin[of&PERLIN_SIZE];
      n2 += rxf*(perlin[(of+1)&PERLIN_SIZE]-n2);
      n3  = perlin[(of+PERLIN_YWRAP)&PERLIN_SIZE];
      n3 += rxf*(perlin[(of+PERLIN_YWRAP+1)&PERLIN_SIZE]-n3);
      n2 += ryf*(n3-n2);

      n1 += noise_fsc(zf)*(n2-n1);

      r += n1*ampl;
      ampl *= perlin_amp_falloff;
      xi<<=1; xf*=2;
      yi<<=1; yf*=2;
      zi<<=1; zf*=2;

      if (xf>=1.0f) { xi++; xf--; }
      if (yf>=1.0f) { yi++; yf--; }
      if (zf>=1.0f) { zi++; zf--; }
    }
    return r;
  }

  // [toxi 031112] 
  // now adjusts to the size of the cosLUT used via 
  // the new variables, defined above
  private float noise_fsc(float i) {
    // using bagel's cosine table instead
    return 0.5f*(1.0f-perlin_cosTable[(int)(i*perlin_PI)%perlin_TWOPI]);
  }

  // [toxi 040903]
  // make perlin noise quality user controlled to allow 
  // for different levels of detail. lower values will produce 
  // smoother results as higher octaves are surpressed

  public void noiseDetail(int lod) {
    if (lod>0) perlin_octaves=lod;
  }

  public void noiseDetail(int lod, float falloff) {
    if (lod>0) perlin_octaves=lod;
    if (falloff>0) perlin_amp_falloff=falloff;
  }


  // ------------------------------------------------------------

  // utilities for dealing with arrays


  static public int[] expand(int list[]) {
    return expand(list, list.length << 1);
  }

  static public int[] expand(int list[], int newSize) {
    int temp[] = new int[newSize];
    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
    return temp;
  }


  static public float[] expand(float list[]) {
    return expand(list, list.length << 1);
  }

  static public float[] expand(float list[], int newSize) {
    float temp[] = new float[newSize];
    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
    return temp;
  }


  static public String[] expand(String list[]) {
    return expand(list, list.length << 1);
  }

  static public String[] expand(String list[], int newSize) {
    String temp[] = new String[newSize];
    // in case the new size is smaller than list.length
    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
    return temp;
  }


  /* 

  // this appears to be worthless, because it will always 
  // throw a ClassCastException

  // maybe this could be done with reflection?

  static public Object[] expand(Object list[]) {
    return expand(list, list.length << 1);
  }

  static public Object[] expand(Object list[], int newSize) {
    Object temp[] = new Object[newSize];
    // in case the new size is smaller than list.length
    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
    return temp;
  }

  */


  //

  static public int[] subset(int list[], int start) {
    return subset(list, start, list.length - start);
  }

  static public int[] subset(int list[], int start, int count) {
    int output[] = new int[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public float[] subset(float list[], int start) {
    return subset(list, start, list.length - start);
  }

  static public float[] subset(float list[], int start, int count) {
    float output[] = new float[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public String[] subset(String list[], int start) {
    return subset(list, start, list.length - start);
  }

  static public String[] subset(String list[], int start, int count) {
    String output[] = new String[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public Object[] subset(Object list[], int start) {
    return subset(list, start, list.length - start);
  }

  static public Object[] subset(Object list[], int start, int count) {
    Object output[] = new Object[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  // ------------------------------------------------------------

  // utilities for dealing with strings


  /**
   * Remove whitespace characters from the beginning and ending
   * of a String. Works like String.trim() but includes the
   * unicode nbsp character as well.
   */
  static public String trim(String str) {
    return str.replace('\u00A0', ' ').trim();

    /*
    int left = 0;
    int right = str.length() - 1;

    while ((left <= right) &&
           (WHITESPACE.indexOf(str.charAt(left)) != -1)) left++;
    if (left == right) return "";

    while (WHITESPACE.indexOf(str.charAt(right)) != -1) --right;

    return str.substring(left, right-left+1);
    */
  }


  /**
   * Join an array of Strings together as a single String, 
   * separated by the whatever's passed in for the separator.
   *
   * To use this on numbers, first pass the array to nf() or nfs()
   * to get a list of String objects, then use join on that.
   *
   * e.g. String stuff[] = { "apple", "bear", "cat" };
   *      String list = join(stuff, ", ");
   *      // list is now "apple, bear, cat"
   */
  static public String join(String str[], String separator) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < str.length; i++) {
      if (i != 0) buffer.append(separator);
      buffer.append(str[i]);
    }
    return buffer.toString();
  }
  

  /**
   * Split the provided String at wherever whitespace occurs. 
   * Multiple whitespace (extra spaces or tabs or whatever) 
   * between items will count as a single break.
   *
   * The whitespace characters are "\t\n\r\f", which are the defaults
   * for java.util.StringTokenizer, plus the unicode non-breaking space
   * character, which is found commonly on files created by or used
   * in conjunction with Mac OS X (character 160, or 0x00A0 in hex).
   *
   * i.e. split("a b") -> { "a", "b" }
   *      split("a    b") -> { "a", "b" }
   *      split("a\tb") -> { "a", "b" }
   *      split("a \t  b  ") -> { "a", "b" }
   */
  static public String[] split(String what) {
    return split(what, WHITESPACE);
  }


  /** 
   * Splits a string into pieces, using any of the chars in the
   * String 'delim' as separator characters. For instance, 
   * in addition to white space, you might want to treat commas 
   * as a separator. The delimeter characters won't appear in
   * the returned String array.
   *
   * i.e. split("a, b", " ,") -> { "a", "b" }
   *
   * To include all the whitespace possibilities, use the variable
   * WHITESPACE, found in PConstants:
   *
   * i.e. split("a   | b", WHITESPACE + "|");  ->  { "a", "b" }
   */
  static public String[] split(String what, String delim) {
    StringTokenizer toker = new StringTokenizer(what, delim);
    String pieces[] = new String[toker.countTokens()];

    int index = 0;
    while (toker.hasMoreTokens()) {
      pieces[index++] = toker.nextToken();
    }
    return pieces;    
  }


  /**
   * Split a string into pieces along a specific character. 
   * Most commonly used to break up a String along tab characters.
   *
   * This operates differently than the others, where the
   * single delimeter is the only breaking point, and consecutive
   * delimeters will produce an empty string (""). This way,
   * one can split on tab characters, but maintain the column
   * alignments (of say an excel file) where there are empty columns.
   */
  static public String[] split(String what, char delim) {
    // do this so that the exception occurs inside the user's 
    // program, rather than appearing to be a bug inside split()
    if (what == null) return null;
    //return split(what, String.valueOf(delim));  // huh

    char chars[] = what.toCharArray();
    int splitCount = 0; //1;
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == delim) splitCount++;
    }
    // make sure that there is something in the input string
    //if (chars.length > 0) {
      // if the last char is a delimeter, get rid of it..
      //if (chars[chars.length-1] == delim) splitCount--;
      // on second thought, i don't agree with this, will disable
    //}
    if (splitCount == 0) {
      String splits[] = new String[1];
      splits[0] = new String(what);
      return splits;
    }
    //int pieceCount = splitCount + 1;
    String splits[] = new String[splitCount + 1];
    int splitIndex = 0;
    int startIndex = 0;
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == delim) {
        splits[splitIndex++] =
          new String(chars, startIndex, i-startIndex);
        startIndex = i + 1;
      }
    }
    //if (startIndex != chars.length) {
      splits[splitIndex] =
        new String(chars, startIndex, chars.length-startIndex);
    //}
    return splits;
  }


  /**
   * Wrapper for tedious Integer.parseInt function
   */
  static public int toInt(String what) {
    try {
      return Integer.parseInt(what);
    } catch (NumberFormatException e) { }
    return 0;
  }


  /**
   * Wrapper for tedious Integer.parseInt function
   */
  static public int toInt(String what, int otherwise) {
    try {
      return Integer.parseInt(what);
    } catch (NumberFormatException e) { }
    
    return otherwise;
  }


  /**
   * Make an array of int elements from an array of String objects.
   * If the String can't be parsed as a number, it will be set to zero.
   *
   * String s[] = { "1", "300", "44" };
   * int numbers[] = toInt(s);
   *
   * numbers will contain { 1, 300, 44 }
   */ 
  static public int[] toInt(String what[]) {
    return toInt(what, 0);
  }


  /**
   * Make an array of int elements from an array of String objects.
   * If the String can't be parsed as a number, its entry in the
   * array will be set to the value of the "missing" parameter.
   *
   * String s[] = { "1", "300", "apple", "44" };
   * int numbers[] = toInt(s, 9999);
   *
   * numbers will contain { 1, 300, 9999, 44 }
   */ 
  static public int[] toInt(String what[], int missing) {
    int output[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      try {
        output[i] = Integer.parseInt(what[i]);
      } catch (NumberFormatException e) {
        output[i] = missing;
      }
    }
    return output;
  }


  /**
   * Cast a float to an int.
   */
  static public int toInt(float what) {
    return (int) what;
  }


  /**
   * Create an array of ints that correspond to an array of floats.
   */
  static public int[] toInt(float what[]) {
    int inties[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      inties[i] = (int)what[i];
    }
    return inties;
  }


  /**
   * Wrapper for tedious new Float(string).floatValue().
   */
  static public float toFloat(String what) {
    //return new Float(what).floatValue();
    return toFloat(what, Float.NaN);
  }


  /**
   * Wrapper for tedious new Float(string).floatValue().
   */
  static public float toFloat(String what, float otherwise) {
    //return new Float(what).floatValue();
    try {
      return new Float(what).floatValue();
    } catch (NumberFormatException e) { }

    return otherwise;
  }


  /**
   * Cast an int to a float.
   */
  static public float toFloat(int what) {
    return (float)what;
  }


  /**
   * Create an array of ints that correspond to an array of floats.
   */
  static public float[] toFloat(int what[]) {
    float floaties[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      //floaties[i] = (float)what[i];
      floaties[i] = what[i];
    }
    return floaties;
  }


  /**
   * Convert an array of Strings into an array of floats.
   * See the documentation for toInt().
   */
  static public float[] toFloat(String what[]) {
    return toFloat(what, 0);
  }


  /**
   * Convert an array of Strings into an array of floats.
   * See the documentation for toInt().
   */
  static public float[] toFloat(String what[], float missing) {
    float output[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      try {
        output[i] = new Float(what[i]).floatValue();
      } catch (NumberFormatException e) {
        output[i] = missing;
      }
    }
    return output;
  }


  /**
   * Wrapper for tedious Long.parseLong().
   */
  static public long toLong(String what) {
    return Long.parseLong(what);
  }


  /**
   * Convert an array of Strings into an array of longs.
   * See the documentation for toInt().
   */
  static public long[] toLong(String what[]) {
    return toLong(what, 0);
  }

  /**
   * Convert an array of Strings into an array of longs.
   * See the documentation for toInt().
   */
  static public long[] toLong(String what[], int missing) {
    long output[] = new long[what.length];
    for (int i = 0; i < what.length; i++) {
      try {
        output[i] = Long.parseLong(what[i]);
      } catch (NumberFormatException e) {
        output[i] = missing;
      }
    }
    return output;
  }


  /**
   * Wrapper for tedious new Double(string).doubleValue()
   */
  static public double toDouble(String what) {
    return new Double(what).doubleValue();
  }


  /**
   * Convert an array of Strings into an array of doubles.
   * See the documentation for toInt().
   */
  static public double[] toDouble(String what[]) {
    return toDouble(what, 0);
  }

  /**
   * Convert an array of Strings into an array of doubles.
   * See the documentation for toInt().
   */
  static public double[] toDouble(String what[], double missing) {
    double output[] = new double[what.length];
    for (int i = 0; i < what.length; i++) {
      try {
        output[i] = new Double(what[i]).doubleValue();
      } catch (NumberFormatException e) {
        output[i] = missing;
      }
    }
    return output;
  }


  // ------------------------------------------------------------

  // number formatting - floats


  static private NumberFormat float_nf;
  static private int float_nf_left, float_nf_right;

  static public String[] nf(float num[], int left, int right) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nf(num[i], left, right);
    }
    return formatted;
  }

  static public String nf(float num, int left, int right) {
    if ((float_nf != null) &&
        (float_nf_left == left) && (float_nf_right == right)) {
      return float_nf.format(num);
    }

    float_nf = NumberFormat.getInstance();
    float_nf.setGroupingUsed(false); // no commas

    if (left != 0) float_nf.setMinimumIntegerDigits(left);
    if (right != 0) {
      float_nf.setMinimumFractionDigits(right);
      float_nf.setMaximumFractionDigits(right);
    }
    float_nf_left = left;
    float_nf_right = right;
    return float_nf.format(num);
  }


  /**
   * Number formatter that takes into account whether the number
   * has a sign (positive, negative, etc) in front of it.
   */
  static public String[] nfs(float num[], int left, int right) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nfs(num[i], left, right);
    }
    return formatted;
  }

  static public String nfs(float num, int left, int right) {
    return (num < 0) ? nf(num, left, right) :  (' ' + nf(num, left, right));
  }


  static public String[] nfp(float num[], int left, int right) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nfp(num[i], left, right);
    }
    return formatted;
  }

  static public String nfp(float num, int left, int right) {
    return (num < 0) ? nf(num, left, right) :  ('+' + nf(num, left, right));
  }


  // ------------------------------------------------------------

  // number formatting - integers


  static public String str(boolean x) { return String.valueOf(x); }
  static public String str(byte x)    { return String.valueOf(x); }
  static public String str(char x)    { return String.valueOf(x); }
  static public String str(short x)   { return String.valueOf(x); }
  static public String str(int x)     { return String.valueOf(x); }
  static public String str(float x)   { return String.valueOf(x); }
  static public String str(long x)    { return String.valueOf(x); }
  static public String str(double x)  { return String.valueOf(x); }

  static public String[] str(boolean x[]) { 
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static public String[] str(byte x[]) { 
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static public String[] str(char x[]) { 
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static public String[] str(short x[]) { 
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static public String[] str(int x[]) { 
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static public String[] str(float x[]) { 
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static public String[] str(long x[]) { 
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static public String[] str(double x[]) { 
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }


  /**
   * Integer number formatter.
   */
  static private NumberFormat int_nf;
  static private int int_nf_digits;

  static public String[] nf(int num[], int digits) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nf(num[i], digits);
    }
    return formatted;    
  }

  static public String nf(int num, int digits) {
    if ((int_nf != null) && (int_nf_digits == digits)) {
      return int_nf.format(num);
    }

    int_nf = NumberFormat.getInstance();
    int_nf.setGroupingUsed(false); // no commas
    int_nf.setMinimumIntegerDigits(digits);
    int_nf_digits = digits;
    return int_nf.format(num);
  }


  /**
   * number format signed (or space)
   * Formats a number but leaves a blank space in the front
   * when it's positive so that it can be properly aligned with 
   * numbers that have a negative sign in front of them.
   */
  static public String nfs(int num, int digits) {
    return (num < 0) ? nf(num, digits) : (' ' + nf(num, digits));
  }

  static public String[] nfs(int num[], int digits) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nfs(num[i], digits);
    }
    return formatted;    
  }

  // 

  /**
   * number format positive (or plus)
   * Formats a number, always placing a - or + sign 
   * in the front when it's negative or positive. 
   */
  static public String nfp(int num, int digits) {
    return (num < 0) ? nf(num, digits) : ('+' + nf(num, digits));
  }

  static public String[] nfp(int num[], int digits) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nfp(num[i], digits);
    }
    return formatted;
  }


  // ------------------------------------------------------------

  // run as application


  public Stopper stopper;

  class Stopper implements Runnable {
    //PApplet parent;
    Thread thread;

    public Stopper() {
      //this.parent = parent;
      thread = new Thread(this);
      thread.start();
    }

    public void run() {
      while (thread != null) {
        try {
          //System.out.println("Stopper: blocking for input");
          int anything = System.in.read();
          //System.out.println("Stopper: got input " + anything);

          if (anything == EXTERNAL_STOP) {
            finished = true;
            //parent.finished = true;  // kill parent
            //parent = null;
            thread = null;  // kill self
          }
        } catch (IOException e) {
          // not tested (needed?) but seems correct
          finished = true;
          thread = null;
          //System.err.println("Stopper: error on reading");
          //e.printStackTrace();
        }

        try {
          Thread.sleep(250);
        } catch (InterruptedException e) { }
      }
    }
  }

  public void setupExternal(Frame frame) {
    externalRuntime = true;
    stopper = new Stopper();

    frame.addComponentListener(new ComponentAdapter() {
        public void componentMoved(ComponentEvent e) {
          //System.out.println(e);
          Point where = ((Frame) e.getSource()).getLocation();
          //System.out.println(e);
          System.err.println(PApplet.EXTERNAL_MOVE + " " + 
                             where.x + " " + where.y);
          System.err.flush();
        }
      });

    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          //if (externalRuntime) {
          System.err.println(PApplet.EXTERNAL_QUIT);
          System.err.flush();  // important
          //}
          System.exit(0);
        }
      });
  }


  static public void main(String args[]) {
    if (args.length < 1) {
      System.err.println("error: PApplet <appletname>");
      System.exit(1);
    }

    try {
      //boolean locationFound = false;
      //externalRuntime = false;
      boolean external = false;
      //String str = "--external=";
      if (args[0].indexOf(EXTERNAL_FLAG) == 0) external = true;

      Frame frame = new Frame();
      frame.pack();  // maybe get insets
      Class c = Class.forName(args[external ? 1 : 0]);
      PApplet applet = (PApplet) c.newInstance();
      applet.init();
      applet.start();

      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

      //String str = "--location=";

      if (external) {
        String argh = args[0].substring(EXTERNAL_FLAG.length());
        boolean exact = argh.charAt(0) == EXTERNAL_EXACT_LOCATION;
        if (exact) argh = argh.substring(1);
        int location[] = toInt(split(argh, ','));

        int x1 = location[0] - 20;
        int y1 = location[1];

        Insets insets = frame.getInsets();  // pack() first?
        //System.out.println(insets);

        int minW = 120;
        int minH = 120;
        int windowW = 
          Math.max(applet.width, minW) + insets.left + insets.right;
        int windowH = 
          Math.max(applet.height, minH) + insets.top + insets.bottom;

        if (x1 - windowW > 10) {  // if it fits to the left of the window
          frame.setBounds(x1 - windowW, y1, windowW, windowH);

        } else { 
          // if it fits inside the editor window, 
          // offset slightly from upper lefthand corner 
          // so that it's plunked inside the text area
          x1 = location[0] + 66; 
          y1 = location[1] + 66;

          if ((x1 + windowW > screen.width - 33) ||
              (y1 + windowH > screen.height - 33)) {
            // otherwise center on screen
            x1 = (screen.width - windowW) / 2;
            y1 = (screen.height - windowH) / 2;
          }
          frame.setBounds(x1, y1, windowW, windowH); //ww, wh);
        }

        if (exact) {
          // ignore the stuff above for window x y coords, but still use it
          // for the bounds, and the placement of the applet within the window
          //System.out.println("setting exact " + 
          //location[0] + " " + location[1]);
          frame.setLocation(location[0], location[1]);
        }

        frame.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
              int newX = e.getComponent().getX();
              int newY = e.getComponent().getY();
              //System.out.println(newX + " " + newY);
            }
          });

        frame.setLayout(null);
        frame.add(applet);
        frame.setBackground(SystemColor.control);
        applet.setBounds((windowW - applet.width)/2, 
                         insets.top + ((windowH - insets.top - insets.bottom) -
                                       applet.height)/2, 
                         windowW, windowH);

        applet.setupExternal(frame);

      } else {  // !external
        frame.setLayout(new BorderLayout());
        frame.add(applet, BorderLayout.CENTER);
        frame.pack();

        frame.setLocation((screen.width - applet.g.width) / 2,
                          (screen.height - applet.g.height) / 2);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
              System.exit(0);
            }
          });
      }

      frame.show();
      applet.requestFocus(); // get keydowns right away

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }


  // ------------------------------------------------------------

  // public functions for processing.core


  public void setup(int width, int height, int format) {
    g.setup(width, height, format);
  }


  public void alpha(int alpha[]) {
    g.alpha(alpha);
  }


  public void alpha(PImage alpha) {
    g.alpha(alpha);
  }


  public int blendColor(int c1, int c2, int mode) {
   return g.blendColor(c1, c2, mode);
  }


  public void filter(int kind) {
    g.filter(kind);
  }


  public void filter(int kind, float param) {
    g.filter(kind, param);
  }


  public int get(int x, int y) {
   return g.get(x, y);
  }


  public PImage get(int x, int y, int w, int h) {
   return g.get(x, y, w, h);
  }


  public void set(int x, int y, int c) {
    g.set(x, y, c);
  }


  public void copy(PImage src, int dx, int dy) {
    g.copy(src, dx, dy);
  }


  public void copy(int sx1, int sy1, int sx2, int sy2, 
                   int dx1, int dy1, int dx2, int dy2) {
    g.copy(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
  }


  public void copy(PImage src, int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    g.copy(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
  }


  public void blend(PImage src, int sx, int sy, int dx, int dy, int mode) {
    g.blend(src, sx, sy, dx, dy, mode);
  }


  public void blend(int sx, int sy, int dx, int dy, int mode) {
    g.blend(sx, sy, dx, dy, mode);
  }


  public void blend(int sx1, int sy1, int sx2, int sy2, 
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    g.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  }


  public void blend(PImage src, int sx1, int sy1, int sx2, int sy2, 
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    g.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  }


  public Object clone() throws CloneNotSupportedException {
   return g.clone();
  }


  public void save(String filename) {
    g.save(filename);
  }


  public void smooth() {
    g.smooth();
  }


  public void noSmooth() {
    g.noSmooth();
  }


  public void imageMode(int mode) {
    g.imageMode(mode);
  }


  public void defaults() {
    g.defaults();
  }


  public void beginFrame() {
    g.beginFrame();
  }


  public void endFrame() {
    g.endFrame();
  }


  public final float[] nextVertex() {
   return g.nextVertex();
  }


  public final void addTexture(PImage image) {
    g.addTexture(image);
  }


  public final void addLine(int a, int b) {
    g.addLine(a, b);
  }


  public final void addTriangle(int a, int b, int c) {
    g.addTriangle(a, b, c);
  }


  public void beginShape() {
    g.beginShape();
  }


  public void beginShape(int kind) {
    g.beginShape(kind);
  }


  public void texture(PImage image) {
    g.texture(image);
  }


  public void textureMode(int texture_mode) {
    g.textureMode(texture_mode);
  }


  public void normal(float nx, float ny, float nz) {
    g.normal(nx, ny, nz);
  }


  public void vertex(float x, float y) {
    g.vertex(x, y);
  }


  public void vertex(float x, float y, float u, float v) {
    g.vertex(x, y, u, v);
  }


  public void vertex(float x, float y, float z) {
    g.vertex(x, y, z);
  }


  public void vertex(float x, float y, float z,  
                     float u, float v) {
    g.vertex(x, y, z, u, v);
  }


  public void bezierVertex(float x, float y) {
    g.bezierVertex(x, y);
  }


  public void bezierVertex(float x, float y, float z) {
    g.bezierVertex(x, y, z);
  }


  public void curveVertex(float x, float y) {
    g.curveVertex(x, y);
  }


  public void curveVertex(float x, float y, float z) {
    g.curveVertex(x, y, z);
  }


  public void endShape_newgraphics() {
    g.endShape_newgraphics();
  }


  public void endShape() {
    g.endShape();
  }


  public void flat_image(PImage image, int sx1, int sy1) {
    g.flat_image(image, sx1, sy1);
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


  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    g.triangle(x1, y1, x2, y2, x3, y3);
  }


  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    g.quad(x1, y1, x2, y2, x3, y3, x4, y4);
  }


  public void rectMode(int mode) {
    g.rectMode(mode);
  }


  public void rect(float x1, float y1, float x2, float y2) {
    g.rect(x1, y1, x2, y2);
  }


  public void ellipseMode(int mode) {
    g.ellipseMode(mode);
  }


  public void ellipse(float x, float y, float hradius, float vradius) {
    g.ellipse(x, y, hradius, vradius);
  }


  public void box(float size) {
    g.box(size);
  }


  public void box(float w, float h, float d) {
    g.box(w, h, d);
  }


  public void sphereDetail(int res) {
    g.sphereDetail(res);
  }


  public void sphere(float r) {
    g.sphere(r);
  }


  public void sphere(float x, float y, float z, float r) {
    g.sphere(x, y, z, r);
  }


  public float bezierPoint(float a, float b, float c, float d,
                           float t) {
   return g.bezierPoint(a, b, c, d, t);
  }


  public float bezierTangent(float a, float b, float c, float d,
                             float t) {
   return g.bezierTangent(a, b, c, d, t);
  }


  public void bezier(float x1, float y1, 
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4) {
    g.bezier(x1, y1, x2, y2, x3, y3, x4, y4);
  }


  public void bezier(float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     float x4, float y4, float z4) {
    g.bezier(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
  }


  public void bezierDetail(int detail) {
    g.bezierDetail(detail);
  }


  public void curveDetail(int detail) {
    g.curveDetail(detail);
  }


  public void curveTightness(float tightness) {
    g.curveTightness(tightness);
  }


  public float curvePoint(float a, float b, float c, float d,
                          float t) {
   return g.curvePoint(a, b, c, d, t);
  }


  public float curveTangent(float a, float b, float c, float d,
                            float t) {
   return g.curveTangent(a, b, c, d, t);
  }


  public void curve(float x1, float y1, 
                    float x2, float y2,
                    float x3, float y3,
                    float x4, float y4) {
    g.curve(x1, y1, x2, y2, x3, y3, x4, y4);
  }


  public void curve(float x1, float y1, float z1,
                    float x2, float y2, float z2,
                    float x3, float y3, float z3,
                    float x4, float y4, float z4) {
    g.curve(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
  }


  public PImage loadImage(String filename) {
   return g.loadImage(filename);
  }


  public PImage loadImage(String filename, boolean force) {
   return g.loadImage(filename, force);
  }


  public void image(PImage image, float x1, float y1) {
    g.image(image, x1, y1);
  }


  public void image(PImage image, 
                    float x1, float y1, float x2, float y2) {
    g.image(image, x1, y1, x2, y2);
  }


  public void image(PImage image, 
                    float x1, float y1, float x2, float y2,
                    float u1, float v1, float u2, float v2) {
    g.image(image, x1, y1, x2, y2, u1, v1, u2, v2);
  }


  public void cache(PImage image) {
    g.cache(image);
  }


  public void cache(PImage images[]) {    
    g.cache(images);
  }


  public PFont loadFont(String name) {
   return g.loadFont(name);
  }


  public void textFont(PFont which) {
    g.textFont(which);
  }


  public void textFont(PFont which, float size) {
    g.textFont(which, size);
  }


  public void textSize(float size) {
    g.textSize(size);
  }


  public void textLeading(float leading) {
    g.textLeading(leading);
  }


  public void textMode(int mode) {
    g.textMode(mode);
  }


  public void textSpace(int space) {
    g.textSpace(space);
  }


  public void text(char c, float x, float y) {
    g.text(c, x, y);
  }


  public void text(char c, float x, float y, float z) {
    g.text(c, x, y, z);
  }


  public void text(String s, float x, float y) {
    g.text(s, x, y);
  }


  public void text(String s, float x, float y, float z) {
    g.text(s, x, y, z);
  }


  public void text(int num, float x, float y) {
    g.text(num, x, y);
  }


  public void text(int num, float x, float y, float z) {
    g.text(num, x, y, z);
  }


  public void text(float num, float x, float y) {
    g.text(num, x, y);
  }


  public void text(float num, float x, float y, float z) {
    g.text(num, x, y, z);
  }


  public void push() {
    g.push();
  }


  public void pop() {
    g.pop();
  }


  public void resetMatrix() {
    g.resetMatrix();
  }


  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    g.applyMatrix(n00, n01, n02, n03, n10, n11, n12, n13, n20, n21, n22, n23, n30, n31, n32, n33);
  }


  public void printMatrix() {
    g.printMatrix();
  }


  public void beginCamera() {
    g.beginCamera();
  }


  public void cameraMode(int icameraMode) {
    g.cameraMode(icameraMode);
  }


  public void endCamera() {
    g.endCamera();
  }


  public void printCamera() {
    g.printCamera();
  }


  public float screenX(float x, float y, float z) {
   return g.screenX(x, y, z);
  }


  public float screenY(float x, float y, float z) {
   return g.screenY(x, y, z);
  }


  public float screenZ(float x, float y, float z) {
   return g.screenZ(x, y, z);
  }


  public float objectX(float x, float y, float z) {
   return g.objectX(x, y, z);
  }


  public float objectY(float x, float y, float z) {
   return g.objectY(x, y, z);
  }


  public float objectZ(float x, float y, float z) {
   return g.objectZ(x, y, z);
  }


  public void ortho(float left, float right, 
                    float bottom, float top,
                    float near, float far) {
    g.ortho(left, right, bottom, top, near, far);
  }


  public void perspective(float fovy, float aspect, float zNear, float zFar) {
    g.perspective(fovy, aspect, zNear, zFar);
  }


  public void frustum(float left, float right, float bottom,
                      float top, float znear, float zfar) {
    g.frustum(left, right, bottom, top, znear, zfar);
  }


  public void lookat(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
    g.lookat(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
  }


  public void translate(float tx, float ty) {
    g.translate(tx, ty);
  }


  public void translate(float tx, float ty, float tz) {
    g.translate(tx, ty, tz);
  }


  public void rotateX(float angle) {
    g.rotateX(angle);
  }


  public void rotateY(float angle) {
    g.rotateY(angle);
  }


  public void rotate(float angle) {
    g.rotate(angle);
  }


  public void rotateZ(float angle) {
    g.rotateZ(angle);
  }


  public void rotate(float angle, float v0, float v1, float v2) {
    g.rotate(angle, v0, v1, v2);
  }


  public void scale(float s) {
    g.scale(s);
  }


  public void scale(float sx, float sy) {
    g.scale(sx, sy);
  }


  public void scale(float x, float y, float z) {
    g.scale(x, y, z);
  }


  public void transform(float n00, float n01, float n02, float n03,
                        float n10, float n11, float n12, float n13,
                        float n20, float n21, float n22, float n23,
                        float n30, float n31, float n32, float n33) {
    g.transform(n00, n01, n02, n03, n10, n11, n12, n13, n20, n21, n22, n23, n30, n31, n32, n33);
  }


  public void colorMode(int icolorMode) {
    g.colorMode(icolorMode);
  }


  public void colorMode(int icolorMode, float max) {
    g.colorMode(icolorMode, max);
  }


  public void colorMode(int icolorMode, 
                        float maxX, float maxY, float maxZ) {
    g.colorMode(icolorMode, maxX, maxY, maxZ);
  }


  public void colorMode(int icolorMode, 
                        float maxX, float maxY, float maxZ, float maxA) {
    g.colorMode(icolorMode, maxX, maxY, maxZ, maxA);
  }


  public void noTint() {
    g.noTint();
  }


  public void tint(int rgb) {
    g.tint(rgb);
  }


  public void tint(float gray) {
    g.tint(gray);
  }


  public void tint(float gray, float alpha) {
    g.tint(gray, alpha);
  }


  public void tint(float x, float y, float z) {
    g.tint(x, y, z);
  }


  public void tint(float x, float y, float z, float a) {
    g.tint(x, y, z, a);
  }


  public void noFill() {
    g.noFill();
  }


  public void fill(int rgb) {
    g.fill(rgb);
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


  public void strokeWeight(float weight) {
    g.strokeWeight(weight);
  }


  public void strokeJoin(int join) {
    g.strokeJoin(join);
  }


  public void strokeMiter(int miter) {
    g.strokeMiter(miter);
  }


  public void noStroke() {
    g.noStroke();
  }


  public void stroke(int rgb) {
    g.stroke(rgb);
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


  public void background(int rgb) {
    g.background(rgb);
  }


  public void background(float gray) {
    g.background(gray);
  }


  public void background(float x, float y, float z) {
    g.background(x, y, z);
  }


  public void background(PImage image) {
    g.background(image);
  }


  public void clear() {
    g.clear();
  }


  public void lights() {
    g.lights();
  }


  public void noLights() {
    g.noLights();
  }


  public void hint(int which) {
    g.hint(which);
  }


  public void unhint(int which) {
    g.unhint(which);
  }


  public void message(int level, String message) {
    g.message(level, message);
  }


  public void message(int level, String message, Exception e) {
    g.message(level, message, e);
  }


  public InputStream openStream(String filename) throws IOException {
   return g.openStream(filename);
  }


  public byte[] loadBytes(String filename) {
   return g.loadBytes(filename);
  }


  static public byte[] loadBytes(InputStream input) {
   return PGraphics.loadBytes(input);
  }


  public String[] loadStrings(String filename) {
   return g.loadStrings(filename);
  }


  static public String[] loadStrings(InputStream input) {
   return PGraphics.loadStrings(input);
  }


  public void saveBytes(String filename, byte buffer[]) {
    g.saveBytes(filename, buffer);
  }


  public void saveBytes(OutputStream output, byte buffer[]) {
    g.saveBytes(output, buffer);
  }


  public void saveStrings(String filename, String strings[]) {
    g.saveStrings(filename, strings);
  }


  public void saveStrings(OutputStream output, String strings[]) {
    g.saveStrings(output, strings);
  }


  public void sort(String what[]) {
    g.sort(what);
  }


  public void sort(String what[], Object objects[]) {
    g.sort(what, objects);
  }


  public void sort(int what[]) {
    g.sort(what);
  }


  public void sort(int what[], Object objects[]) {
    g.sort(what, objects);
  }


  public void sort(float what[]) {
    g.sort(what);
  }


  public void sort(float what[], Object objects[]) {
    g.sort(what, objects);
  }


  public void sort(double what[]) {
    g.sort(what);
  }


  public void sort(double what[], Object objects[]) {
    g.sort(what, objects);
  }


  public void sort(String what[], int count, Object objects[]) {
    g.sort(what, count, objects);
  }


  public void sort(int what[], int count, Object objects[]) {
    g.sort(what, count, objects);
  }


  public void sort(float what[], int count, Object objects[]) {
    g.sort(what, count, objects);
  }


  public void sort(double what[], int count, Object objects[]) {
    g.sort(what, count, objects);
  }


  public final int color(int x, int y, int z) {
   return g.color(x, y, z);
  }


  public final int color(float x, float y, float z) {
   return g.color(x, y, z);
  }


  public final int color(int x, int y, int z, int a) {
   return g.color(x, y, z, a);
  }


  public final int color(float x, float y, float z, float a) {
   return g.color(x, y, z, a);
  }


  public final float alpha(int what) {
   return g.alpha(what);
  }


  public final float red(int what) {
   return g.red(what);
  }


  public final float green(int what) {
   return g.green(what);
  }


  public final float blue(int what) {
   return g.blue(what);
  }


  public final float hue(int what) {
   return g.hue(what);
  }


  public final float saturation(int what) {
   return g.saturation(what);
  }


  public final float brightness(int what) {
   return g.brightness(what);
  }
}
