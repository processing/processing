/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PApplet - applet base class for the bagel engine
  Part of the Processing project - http://processing.org

  Copyright (c) 2004- Ben Fry and Casey Reas
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
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;


public class PApplet extends Applet
  implements PConstants, Runnable,
             MouseListener, MouseMotionListener, KeyListener, FocusListener
{
  // jdkVersionStr = "1.3" or "1.1" or whatever
  public static final String jdkVersionStr =
    System.getProperty("java.version").substring(0,3);
  public static final double jdkVersion =
    new Double(jdkVersionStr).doubleValue();
    //toFloat(System.getProperty("java.version").substring(0,3));

  public PGraphics g;
  public Frame frame;

  /** Command line options passed in from main() */
  public String args[];

  /** Path to sketch folder */
  public String folder; // = System.getProperty("user.dir");

  /** When debugging headaches */
  static final boolean THREAD_DEBUG = false;

  public int pixels[];

  public int mouseX, mouseY;

  public int pmouseX, pmouseY;

  /**
   * previous mouseX/Y for the draw loop, separated out because this is
   * separate from the pmouseX/Y when inside the mouse event handlers.
   */
  protected int dmouseX, dmouseY;

  /**
   * pmouseX/Y for the event handlers (mousePressed(), mouseDragged() etc)
   * these are different because mouse events are queued to the end of
   * draw, so the previous position has to be updated on each event,
   * as opposed to the pmouseX/Y that's used inside draw, which is expected
   * to be updated once per trip through draw().
   */
  protected int emouseX, emouseY;

  /**
   * used to set pmouseX/Y to mouseX/Y the first time mouseX/Y are used,
   * otherwise pmouseX/Y are always zero, causing a nasty jump. just using
   * (frameCount == 0) won't work since mouseXxxxx() may not be called
   * until a couple frames into things.
   */
  public boolean firstMouse;

  public boolean mousePressed;
  public MouseEvent mouseEvent;

  /**
   * Last key pressed. If it's a coded key
   * (arrows or ctrl/shift/alt, this will be set to 0xffff or 65535).
   */
  public char key;

  /**
   * If the key is a coded key, i.e. up/down/ctrl/shift/alt
   * the 'key' comes through as 0xffff (65535)
   */
  public int keyCode;

  public boolean keyPressed;
  public KeyEvent keyEvent;

  /**
   * Gets set to true/false as the applet gains/loses focus.
   */
  public boolean focused = false;

  /**
   * Is the applet online or not? This can be used to test how the
   * applet should behave since online situations are different.
   */
  public boolean online = false;

  long millisOffset;

  // getting the frame rate
  protected float fps = 10;
  protected long fpsLastMillis = 0;

  // setting the frame rate
  protected long fpsLastDelayTime = 0;
  protected float fpsTarget = 0;

  //boolean drawMethod;
  //boolean loopMethod;
  protected boolean looping;
  protected boolean redraw;

  // true if inside the loop method
  //boolean insideLoop;

  // used for mouse tracking so that pmouseX doesn't get
  // updated too many times while still inside the loop
  // instead, it's updated only before/after the loop()
  //int qmouseX, qmouseY;

  // queue for whether to call the simple mouseDragged or
  // mouseMoved functions. these are called after beginFrame
  // but before loop() is called itself, to avoid problems
  // in synchronization.
  //boolean qmouseDragged;
  //boolean qmouseMoved;

  //boolean firstFrame;

  // current frame number (could this be used to replace firstFrame?)
  public int frameCount;

  // true if the feller has spun down
  public boolean finished;

  //boolean drawn;
  Thread thread;

  public Exception exception; // the last exception thrown

  static public final int DEFAULT_WIDTH = 100;
  static public final int DEFAULT_HEIGHT = 100;
  public int width, height;

  protected RegisteredMethods sizeMethods;
  protected RegisteredMethods preMethods, drawMethods, postMethods;
  protected RegisteredMethods mouseEventMethods, keyEventMethods;
  protected RegisteredMethods disposeMethods;

  //protected int libraryCount;
  //protected PLibrary libraries[];
  //protected boolean libraryCalls[][];
  //int setupCount = 0;

  // this text isn't seen unless PApplet is used on its
  // own and someone takes advantage of leechErr.. not likely
  static public final String LEECH_WAKEUP = "Error while running applet.";
  public PrintStream leechErr;

  // message to send if attached as an external vm
  //static public final String EXTERNAL_FLAG = "--external=";
  //static public final char EXTERNAL_EXACT_LOCATION = 'e';
  static public final String EXT_LOCATION = "--location=";
  static public final String EXT_EXACT_LOCATION = "--exact-location=";
  static public final String EXT_SKETCH_FOLDER = "--sketch-folder=";

  static public final char EXTERNAL_STOP = 's';
  static public final String EXTERNAL_QUIT = "__QUIT__";
  static public final String EXTERNAL_MOVE = "__MOVE__";
  //boolean externalRuntime;

  //static boolean setupComplete = false;

  public void init() {
    //checkParams();

    // can/may be resized later
    //g = new PGraphics(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    initGraphics();

    // send tab keys through to the PApplet
    try {
      if (jdkVersion >= 1.4) {
        //setFocusTraversalKeysEnabled(false);  // 1.4-only function
        Method defocus =
          Component.class.getMethod("setFocusTraversalKeysEnabled",
                                    new Class[] { Boolean.TYPE });
        defocus.invoke(this, new Object[] { Boolean.FALSE });
      }
    } catch (Exception e) { }  // oh well

    millisOffset = System.currentTimeMillis();

    finished = false; // just for clarity

    // this will be cleared by loop() if it is not overridden
    looping = true;
    redraw = true;  // draw this guy once
    firstMouse = true;

    // these need to be inited before setup
    sizeMethods = new RegisteredMethods();
    preMethods = new RegisteredMethods();
    drawMethods = new RegisteredMethods();
    postMethods = new RegisteredMethods();
    mouseEventMethods = new RegisteredMethods();
    keyEventMethods = new RegisteredMethods();
    disposeMethods = new RegisteredMethods();

    try {
      getAppletContext();
      online = true;
    } catch (NullPointerException e) {
      online = false;
    }

    start();
  }


  // override for subclasses (i.e. opengl)
  // so that init() doesn't have to be replicated
  public void initGraphics() {
    // 0073: moved here so that can be overridden for PAppletGL
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
    addFocusListener(this);
  }


  public void createGraphics() {
    if (PApplet.jdkVersion >= 1.3) {
      g = new PGraphics2(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    } else {
      g = new PGraphics(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
  }


  public void depth() {
    // OPT if PGraphics already exists, pass in its pixels[]
    //     buffer so as not to re-allocate all that memory again
    if (g.width != 0) {
      g = new PGraphics3(g.width, g.height);
    } else {
      g = new PGraphics3(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    // it's ok to call this, because depth() is only getting called
    // at least inside of setup, so things can be drawn just
    // fine since it's post-beginFrame.
    g.defaults();
  }


  /**
   * Called via the first call to PApplet.paint(),
   * because PAppletGL needs to have a usable screen
   * before getting things rolling.
   */
  public void start() {
    if (thread != null) return;

    thread = new Thread(this);
    thread.start();
  }


  // maybe start should also be used as the method for kicking
  // the thread on, instead of doing it inside paint()
  public void stop() {
    //finished = true;  // why did i comment this out?

    // don't run stop and disposers twice
    if (thread == null) return;
    thread = null;

    disposeMethods.handle();
  }


  /**
   * This also calls stop(), in case there was an inadvertent
   * override of the stop() function by a user.
   *
   * destroy() supposedly gets called as the applet viewer
   * is shutting down the applet. stop() is called
   * first, and then destroy() to really get rid of things.
   * no guarantees on when they're run (on browser quit, or
   * when moving between pages), though.
   */
  public void destroy() {
    stop();
  }


  public Dimension getPreferredSize() {
    return new Dimension(width, height);
  }


  // ------------------------------------------------------------


  public class RegisteredMethods {
    int count;
    Object objects[];
    Method methods[];


    // convenience version for no args
    public void handle() {
      handle(new Object[] { });
    }

    public void handle(Object args[]) {
      for (int i = 0; i < count; i++) {
        try {
          //System.out.println(objects[i] + " " + args);
          methods[i].invoke(objects[i], args);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void add(Object object, Method method) {
      if (objects == null) {
        objects = new Object[5];
        methods = new Method[5];
      }
      if (count == objects.length) {
        Object otemp[] = new Object[count << 1];
        System.arraycopy(objects, 0, otemp, 0, count);
        objects = otemp;
        Method mtemp[] = new Method[count << 1];
        System.arraycopy(methods, 0, mtemp, 0, count);
        methods = mtemp;
      }
      objects[count] = object;
      methods[count] = method;
      count++;
    }
  }


  public void registerSize(Object o) {
    Class methodArgs[] = new Class[] { Integer.TYPE, Integer.TYPE };
    registerWithArgs(preMethods, "size", o, methodArgs);
  }

  public void registerPre(Object o) {
    registerNoArgs(preMethods, "pre", o);
  }

  public void registerDraw(Object o) {
    registerNoArgs(drawMethods, "draw", o);
  }

  public void registerPost(Object o) {
    registerNoArgs(postMethods, "post", o);
  }

  public void registerMouseEvent(Object o) {
    Class methodArgs[] = new Class[] { MouseEvent.class };
    registerWithArgs(mouseEventMethods, "mouseEvent", o, methodArgs);
  }


  public void registerKeyEvent(Object o) {
    Class methodArgs[] = new Class[] { KeyEvent.class };
    registerWithArgs(keyEventMethods, "keyEvent", o, methodArgs);
  }

  public void registerDispose(Object o) {
    registerNoArgs(disposeMethods, "dispose", o);
  }


  protected void registerNoArgs(RegisteredMethods meth,
                                String name, Object o) {
    Class c = o.getClass();
    try {
      Method method = c.getMethod(name, new Class[] {});
      meth.add(o, method);

    } catch (Exception e) {
      die("Could not register " + name + " + () for " + o, e);
    }
  }


  protected void registerWithArgs(RegisteredMethods meth,
                                  String name, Object o, Class args[]) {
    Class c = o.getClass();
    try {
      Method method = c.getMethod(name, args);
      meth.add(o, method);

    } catch (Exception e) {
      die("Could not register " + name + " + () for " + o, e);
    }
  }


  // ------------------------------------------------------------


  public void setup() {
  }


  public void draw() {
    finished = true;  // if no draw method, then...
  }


  public void redraw() {
    if (!looping) {
      redraw = true;
      if (thread != null) {
        thread.interrupt();  // wake from sleep
      }
    }
  }


  public void loop() {
    if (!looping) {
      looping = true;
      if (thread != null) {
        thread.interrupt();  // wake from sleep
      }
    }
  }


  public void noLoop() {
    if (looping) {
      looping = false;

      // reset framerate delay times
      fpsLastDelayTime = 0;
      fpsLastMillis = 0;

      if (thread != null) {
        thread.interrupt();  // wake from sleep
      }
    }
  }


  // ------------------------------------------------------------


  public void size(int iwidth, int iheight) {
    if (g == null) return;
    g.resize(iwidth, iheight);

    this.pixels = g.pixels;
    this.width = g.width;
    this.height = g.height;

    Object methodArgs[] =
      new Object[] { new Integer(width), new Integer(height) };
    sizeMethods.handle(methodArgs);

    if (frame != null) {
      Insets insets = frame.getInsets();

      // msft windows has a limited minimum size for frames
      int minW = 120;
      int minH = 120;
      int winW = Math.max(width, minW) + insets.left + insets.right;
      int winH = Math.max(height, minH) + insets.top + insets.bottom;
      frame.setSize(winW, winH);

      setBounds((winW - width)/2,
                insets.top + ((winH - insets.top - insets.bottom) - height)/2,
                winW, winH);
    } else {
      //System.out.println("frame was null");
      setBounds(0, 0, width, height);
    }
  }


  public void update(Graphics screen) {
    //System.out.println("PApplet.update()");
    if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                              "    4 update() external");
    paint(screen);
  }

  //synchronized public void paint(Graphics screen) {
  public void paint(Graphics screen) {
    //System.out.println("PApplet.paint()");
    if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                              "     5a enter paint");

    // ignore the very first call to paint, since it's coming
    // from the o.s., and the applet will soon update itself anyway.
    //if (firstFrame) return;
    if (frameCount == 0) {
      // paint() may be called more than once before things
      // are finally painted to the screen and the thread gets going
      /*
      if (thread == null) {
        initGraphics();
        start();
      }
      */
      return;
    }

    // without ignoring the first call, the first several frames
    // are confused because paint() gets called in the midst of
    // the initial nextFrame() call, so there are multiple
    // updates fighting with one another.

    // g.image is synchronized so that draw/loop and paint don't
    // try to fight over it. this was causing a randomized slowdown
    // that would cut the framerate into a third on macosx,
    // and is probably related to the windows sluggishness bug too
    if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                              "     5b enter paint sync");

    synchronized (g) {
      if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                "     5c inside paint sync");
      //System.out.println("5b paint has sync");
      //Exception e = new Exception();
      //e.printStackTrace();

      // moving this into PGraphics caused weird sluggishness on win2k
      //g.mis.newPixels(pixels, g.cm, 0, width); // must call this

      // make sure the screen is visible and usable
      if (g != null) {
        screen.drawImage(g.image, 0, 0, null);
      }
      //if (THREAD_DEBUG) println("notifying all");
      //notifyAll();
      //thread.notify();
      //System.out.println("      6 exit paint");
    }
    if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                              "      6 exit paint");
    //updated = true;
  }


  public void run() {
    try {
      /*
      // first time around, call the applet's setup method
      setup();

      // these are the same things as get run inside a call to size()
      this.pixels = g.pixels;
      this.width = g.width;
      this.height = g.height;
      */

      while ((Thread.currentThread() == thread) && !finished) {
      //while (!finished) {
        //updated = false;

        if (PApplet.THREAD_DEBUG) println(Thread.currentThread().getName() +
                                          " formerly nextFrame()");
        //if (looping || redraw) nextFrame();
        if (looping || redraw) {
          if (fpsTarget != 0) framerate_delay();

          if (frameCount == 0) {  // needed here for the sync
            createGraphics();
          }

          // g may be rebuilt inside here, so turning of the sync
          //synchronized (g) {
            if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                      " 1a beginFrame");
            g.beginFrame();
            if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                      " 1b draw");

            if (frameCount == 0) {
              //initGraphics();
              //createGraphics();
              defaults();
              setup();
              // if depth() is called inside setup, pixels/width/height
              // will be ok by the time it's back out again

              this.pixels = g.pixels;
              this.width = g.width;
              this.height = g.height;

            } else {
              preMethods.handle();

              pmouseX = dmouseX;
              pmouseY = dmouseY;

              draw();

              // dmouseX/Y is updated only once per frame
              dmouseX = mouseX;
              dmouseY = mouseY;

              // these are called *after* loop so that valid
              // drawing commands can be run inside them. it can't
              // be before, since a call to background() would wipe
              // out anything that had been drawn so far.
              dequeueMouseEvents();

              dequeueKeyEvents();
              if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                        " 2b endFrame");

              drawMethods.handle();
              //for (int i = 0; i < libraryCount; i++) {
              //if (libraryCalls[i][PLibrary.DRAW]) libraries[i].draw();
              //}
            }

            g.endFrame();
            //}  // end sync

            //update();
            // formerly 'update'
            //if (firstFrame) firstFrame = false;
            // internal frame counter
            frameCount++;
            if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                      "   3a calling repaint() " + frameCount);
            repaint();
            if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                      "   3b calling Toolkit.sync " + frameCount);
            getToolkit().sync();  // force repaint now (proper method)
            if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                    "   3c done " + frameCount);
            //if (THREAD_DEBUG) println("   3d waiting");
            //wait();
            //if (THREAD_DEBUG) println("   3d out of wait");
            //frameCount++;

            postMethods.handle();
            //for (int i = 0; i < libraryCount; i++) {
            //if (libraryCalls[i][PLibrary.POST]) libraries[i].post();
            //}
          //}  // temporarily disabling the synchronize
        }
        redraw = false;  // unset 'redraw' flag in case it was set

        // moving this to update() (for 0069+) for linux sync problems
        //if (firstFrame) firstFrame = false;

        // wait for update & paint to happen before drawing next frame
        // this is necessary since the drawing is sometimes in a
        // separate thread, meaning that the next frame will start
        // before the update/paint is completed
        //while (!updated) {
        try {
          if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                      " " + looping + " " + redraw);
          //Thread.yield();
          // windows doesn't like 'yield', so have to sleep at least
          // for some small amount of time.
          if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                    " gonna sleep");
          // can't remember when/why i changed that to '1'..
          // i have a feeling that some applets aren't gonna like that
          Thread.sleep(looping ? 1 : 10000);  // sleep to make OS happy
          if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                                    " outta sleep");
        } catch (InterruptedException e) { }
        //}
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
    if (THREAD_DEBUG) println(Thread.currentThread().getName() +
                              " thread finished");
    stop();  // call to shutdown libs?
  }


  // ------------------------------------------------------------


  MouseEvent mouseEventQueue[] = new MouseEvent[10];
  int mouseEventCount;

  protected void enqueueMouseEvent(MouseEvent e) {
    synchronized (mouseEventQueue) {
      if (mouseEventCount == mouseEventQueue.length) {
        MouseEvent temp[] = new MouseEvent[mouseEventCount << 1];
        System.arraycopy(mouseEventQueue, 0, temp, 0, mouseEventCount);
        mouseEventQueue = temp;
      }
      mouseEventQueue[mouseEventCount++] = e;
    }
  }

  protected void dequeueMouseEvents() {
    synchronized (mouseEventQueue) {
      for (int i = 0; i < mouseEventCount; i++) {
        mouseEvent = mouseEventQueue[i];
        handleMouseEvent(mouseEvent);
      }
      mouseEventCount = 0;
    }
  }


  /**
   * Actually take action based on a mouse event.
   * Internally updates mouseX, mouseY, mousePressed, and mouseEvent.
   * Then it calls the event type with no params,
   * i.e. mousePressed() or mouseReleased() that the user may have
   * overloaded to do something more useful.
   */
  protected void handleMouseEvent(MouseEvent event) {
    pmouseX = emouseX;
    pmouseY = emouseY;

    mouseX = event.getX();
    mouseY = event.getY();
    mouseEvent = event;

    mouseEventMethods.handle(new Object[] { event });
    /*
    for (int i = 0; i < libraryCount; i++) {
      if (libraryCalls[i][PLibrary.MOUSE]) {
        libraries[i].mouse(event);  // endNet/endSerial etc
      }
    }
    */

    // this used to only be called on mouseMoved and mouseDragged
    // change it back if people run into trouble
    if (firstMouse) {
      pmouseX = mouseX;
      pmouseY = mouseY;
      dmouseX = mouseX;
      dmouseY = mouseY;
      firstMouse = false;
    }

    switch (event.getID()) {
    case MouseEvent.MOUSE_PRESSED:
      mousePressed = true;
      mousePressed();
      break;
    case MouseEvent.MOUSE_RELEASED:
      mousePressed = false;
      mouseReleased();
      break;
    case MouseEvent.MOUSE_CLICKED:
      mouseClicked();
      break;
    case MouseEvent.MOUSE_DRAGGED:
      mouseDragged();
      break;
    case MouseEvent.MOUSE_MOVED:
      mouseMoved();
      break;
    }
    emouseX = mouseX;
    emouseY = mouseY;
  }


  /**
   * Figure out how to process a mouse event. When loop() has been
   * called, the events will be queued up until drawing is complete.
   * If noLoop() has been called, then events will happen immediately.
   */
  protected void checkMouseEvent(MouseEvent event) {
    if (looping) {
      enqueueMouseEvent(event);
    } else {
      handleMouseEvent(event);
    }
  }


  /**
   * If you override this or any function that takes a "MouseEvent e"
   * without calling its super.mouseXxxx() then mouseX, mouseY,
   * mousePressed, and mouseEvent will no longer be set.
   */
  public void mousePressed(MouseEvent e) {
    checkMouseEvent(e);
  }

  public void mouseReleased(MouseEvent e) {
    checkMouseEvent(e);
  }

  public void mouseClicked(MouseEvent e) {
    checkMouseEvent(e);
  }

  public void mouseEntered(MouseEvent e) {
    checkMouseEvent(e);
  }

  public void mouseExited(MouseEvent e) {
    checkMouseEvent(e);
  }

  public void mouseDragged(MouseEvent e) {
    checkMouseEvent(e);
  }

  public void mouseMoved(MouseEvent e) {
    checkMouseEvent(e);
  }


  /**
   * Mouse has been pressed, and should be considered "down"
   * until mouseReleased() is called. If you must, use
   * int button = mouseEvent.getButton();
   * to figure out which button was clicked. It will be one of:
   * MouseEvent.BUTTON1, MouseEvent.BUTTON2, MouseEvent.BUTTON3
   * Note, however, that this is completely inconsistent across
   * platforms.
   */
  public void mousePressed() { }

  /**
   * Mouse button has been released.
   */
  public void mouseReleased() { }

  /**
   * When the mouse is clicked, mousePressed() will be called,
   * then mouseReleased(), then mouseClicked(). Note that
   * mousePressed is already false inside of mouseClicked().
   */
  public void mouseClicked() { }

  /**
   * Mouse button is pressed and the mouse has been dragged.
   */
  public void mouseDragged() { }

  /**
   * Mouse button is not pressed but the mouse has changed locations.
   */
  public void mouseMoved() { }


  // ------------------------------------------------------------


  KeyEvent keyEventQueue[] = new KeyEvent[10];
  int keyEventCount;

  protected void enqueueKeyEvent(KeyEvent e) {
    synchronized (keyEventQueue) {
      if (keyEventCount == keyEventQueue.length) {
        KeyEvent temp[] = new KeyEvent[keyEventCount << 1];
        System.arraycopy(keyEventQueue, 0, temp, 0, keyEventCount);
        keyEventQueue = temp;
      }
      keyEventQueue[keyEventCount++] = e;
    }
  }

  protected void dequeueKeyEvents() {
    synchronized (keyEventQueue) {
      for (int i = 0; i < keyEventCount; i++) {
        keyEvent = keyEventQueue[i];
        handleKeyEvent(keyEvent);
      }
      keyEventCount = 0;
    }
  }


  protected void handleKeyEvent(KeyEvent event) {
    keyEvent = event;
    key = event.getKeyChar();
    keyCode = event.getKeyCode();

    keyEventMethods.handle(new Object[] { event });
    /*
    for (int i = 0; i < libraryCount; i++) {
      if (libraryCalls[i][PLibrary.KEY]) {
        libraries[i].key(event);  // endNet/endSerial etc
      }
    }
    */

    switch (event.getID()) {
    case KeyEvent.KEY_PRESSED:
      keyPressed = true;
      keyPressed();
      break;
    case KeyEvent.KEY_RELEASED:
      keyPressed = false;
      keyReleased();
      break;
    case KeyEvent.KEY_TYPED:
      keyTyped();
      break;
    }
  }


  protected void checkKeyEvent(KeyEvent event) {
    if (looping) {
      enqueueKeyEvent(event);
    } else {
      handleKeyEvent(event);
    }
  }


  /**
   * Overriding keyXxxxx(KeyEvent e) functions will cause the 'key',
   * 'keyCode', and 'keyEvent' variables to no longer work;
   * key events will no longer be queued until the end of draw();
   * and the keyPressed(), keyReleased() and keyTyped() methods
   * will no longer be called.
   */
  public void keyPressed(KeyEvent e) { checkKeyEvent(e); }
  public void keyReleased(KeyEvent e) { checkKeyEvent(e); }
  public void keyTyped(KeyEvent e) { checkKeyEvent(e); }


  /**
   * Called each time a single key on the keyboard is pressed.
   *
   * Examples for key handling:
   * (Tested on Windows XP, please notify if different on other
   * platforms, I have a feeling Mac OS and Linux may do otherwise)
   *
   * 1. Pressing 'a' on the keyboard:
   *    keyPressed  with key == 'a' and keyCode == 'A'
   *    keyTyped    with key == 'a' and keyCode ==  0
   *    keyReleased with key == 'a' and keyCode == 'A'
   *
   * 2. Pressing 'A' on the keyboard:
   *    keyPressed  with key == 'A' and keyCode == 'A'
   *    keyTyped    with key == 'A' and keyCode ==  0
   *    keyReleased with key == 'A' and keyCode == 'A'
   *
   * 3. Pressing 'shift', then 'a' on the keyboard (caps lock is off):
   *    keyPressed  with key == CODED and keyCode == SHIFT
   *    keyPressed  with key == 'A'   and keyCode == 'A'
   *    keyTyped    with key == 'A'   and keyCode == 0
   *    keyReleased with key == 'A'   and keyCode == 'A'
   *    keyReleased with key == CODED and keyCode == SHIFT
   *
   * 4. Holding down the 'a' key.
   *    The following will happen several times,
   *    depending on your machine's "key repeat rate" settings:
   *    keyPressed  with key == 'a' and keyCode == 'A'
   *    keyTyped    with key == 'a' and keyCode ==  0
   *    When you finally let go, you'll get:
   *    keyReleased with key == 'a' and keyCode == 'A'
   *
   * 5. Pressing and releasing the 'shift' key
   *    keyPressed  with key == CODED and keyCode == SHIFT
   *    keyReleased with key == CODED and keyCode == SHIFT
   *    (note there is no keyTyped)
   *
   * 6. Pressing the tab key in an applet with Java 1.4 will
   *    normally do nothing, but PApplet dynamically shuts
   *    this behavior off if Java 1.4 is in use (tested 1.4.2_05 Windows).
   *    Java 1.1 (Microsoft VM) passes the TAB key through normally.
   *    Not tested on other platforms or for 1.3.
   */
  public void keyPressed() { }


  /**
   * See keyPressed().
   */
  public void keyReleased() { }


  /**
   * Only called for "regular" keys like letters,
   * see keyPressed() for full documentation.
   */
  public void keyTyped() { }


  // ------------------------------------------------------------

  // i am focused man, and i'm not afraid of death.
  // and i'm going all out. i circle the vultures in a van
  // and i run the block.


  public void focusGained() { }

  public void focusGained(FocusEvent e) {
    focused = true;
    focusGained();
  }


  public void focusLost() { }

  public void focusLost(FocusEvent e) {
    focused = false;
    focusLost();
  }


  // ------------------------------------------------------------

  // getting the time


  /** Get the number of milliseconds since the applet started. */
  public int millis() {
    return (int) (System.currentTimeMillis() - millisOffset);
  }

  /** Seconds position of the current time. */
  static public int second() {
    return Calendar.getInstance().get(Calendar.SECOND);
  }

  /** Minutes position of the current time. */
  static public int minute() {
    return Calendar.getInstance().get(Calendar.MINUTE);
  }

  /** Hour position of the current time. */
  static public int hour() {
    return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
  }

  /**
   * Get the current day of the month (1 through 31).
   * If you're looking for the day of the week (M-F or whatever)
   * or day of the year (1..365) then use java's Calendar.get()
   */
  static public int day() {
    return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
  }

  /**
   * Get the current month in range 1 through 12.
   */
  static public int month() {
    // months are number 0..11 so change to colloquial 1..12
    return Calendar.getInstance().get(Calendar.MONTH) + 1;
  }

  /**
   * Get the current year.
   */
  static public int year() {
    return Calendar.getInstance().get(Calendar.YEAR);
  }


  // ------------------------------------------------------------

  // controlling time and playing god


  /**
   * I'm not sure if this is even helpful anymore.
   */
  public void delay(int napTime) {
    if (frameCount == 0) return;
    if (napTime > 0) {
      try {
        Thread.sleep(napTime);
      } catch (InterruptedException e) { }
    }
  }


  /**
   * Get the current framerate. The initial value will be 10 fps,
   * and will be updated with each frame thereafter. The value is not
   * instantaneous (since that wouldn't be very useful since it would
   * jump around so much), but is instead averaged (integrated)
   * over roughly the last 10 frames.
   */
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


  /**
   * Set a target framerate. This will cause delay() to be called
   * after each frame to allow for a specific rate to be set.
   */
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
   * Get a param from the web page, or (eventually)
   * from a properties file.
   */
  public String param(String what) {
    if (online) {
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
    if (online) {
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
  public void link(String here) {
    if (!online) {
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

  public void link(String here, String there) {
    if (!online) {
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


  // ------------------------------------------------------------


  /**
   * Function for an applet/application to kill itself and
   * display an error. Mostly this is here to be improved later.
   */
  public void die(String what) {
    stop();

    if (online) {
      System.err.println("i'm dead.. " + what);

    } else {
      System.err.println(what);
      System.exit(1);
    }
  }


  /**
   * Same as above but with an exception. Also needs work.
   */
  public void die(String what, Exception e) {
    e.printStackTrace();
    die(what);
  }


  /**
   * Explicitly exit the applet. Inserted as a call for static
   * mode apps, but is generally necessary because apps no longer
   * have draw/loop separation.
   */
  public void exit() {
    stop();
    // TODO if not running as an applet, do a System.exit() here
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
    if (online) {
      System.err.println("Can't use saveFrame() when running in a browser.");
      return;
    }

    //File file = new File(folder, "screen-" + nf(frame, 4) + ".tif");
    save(savePath("screen-" + nf(frameCount, 4) + ".tif"));
    //save("screen-" + nf(frame, 4) + ".tif");
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
    if (online) {
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

      //File file = new File(folder, prefix + nf(frame, count) + suffix);
      // in case the user tries to make subdirs with the filename
      //new File(file.getParent()).mkdirs();
      //save(file.getAbsolutePath());
      save(savePath(prefix + nf(frameCount, count) + suffix));
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
  public void cursor(int _cursor_type) {
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
  public void cursor(PImage image, int hotspotX, int hotspotY) {
    //if (!isOneTwoOrBetter()) {
    if (jdkVersion < 1.2) {
      System.err.println("cursor() error: Java 1.2 or higher is " +
                         "required to set cursors");
      System.err.println("                (You're using version " +
                         jdkVersionStr + ")");
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
  public void cursor() {
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
  public void noCursor() {
    if (!cursor_visible) return;  // don't hide if already hidden.

    if (invisible_cursor == null) {
      //invisible_cursor = new PImage(new int[32*32], 32, 32, RGBA);
      invisible_cursor = new PImage(new int[16*16], 16, 16, ARGB);
    }
    // was formerly 16x16, but the 0x0 was added by jdf as a fix
    // for macosx, which didn't wasn't honoring the invisible cursor
    cursor(invisible_cursor, 0, 0);
    cursor_visible = false;
  }


  // ------------------------------------------------------------


  static public void print(byte what) {
    System.out.print(what);
    System.out.flush();
  }

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

  static public void print(Object what) {
    System.out.print(what.toString());
    System.out.flush();
  }

  //

  static public void println() {
    System.out.println();
  }

  //

  static public void println(byte what) {
    print(what); System.out.println();
  }

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

  static public void println(Object what) {
    System.out.println(what.toString());
  }

  //

  static public void printarr(byte what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void printarr(boolean what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void printarr(char what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void printarr(int what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void printarr(float what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void printarr(double what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void printarr(String what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  static public void printarr(Object what[]) {
    for (int i = 0; i < what.length; i++) System.out.println(what[i]);
    System.out.flush();
  }

  //

  /*
  public void printvar(String name) {
    try {
      Field field = getClass().getDeclaredField(name);
      println(name + " = " + field.get(this));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  */


  //////////////////////////////////////////////////////////////

  // MATH

  // lots of convenience methods for math with floats.
  // doubles are overkill for processing applets, and casting
  // things all the time is annoying, thus the functions below.


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

  static public final float log(float a) {
    return (float)Math.log(a);
  }

  static public final float exp(float a) {
    return (float)Math.exp(a);
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


  static public final float lerp(float a, float b, float amt) {
    return a + (b-a) * amt;
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


  public final float sin(float angle) {
    if ((g != null) && (g.angleMode == DEGREES)) angle *= DEG_TO_RAD;
    return (float)Math.sin(angle);
  }

  public final float cos(float angle) {
    if ((g != null) && (g.angleMode == DEGREES)) angle *= DEG_TO_RAD;
    return (float)Math.cos(angle);
  }

  public final float tan(float angle) {
    if ((g != null) && (g.angleMode == DEGREES)) angle *= DEG_TO_RAD;
    return (float)Math.tan(angle);
  }


  public final float asin(float value) {
    return ((g != null) && (g.angleMode == DEGREES)) ?
      ((float)Math.asin(value) * RAD_TO_DEG) : (float)Math.asin(value);
  }

  public final float acos(float value) {
    return ((g != null) && (g.angleMode == DEGREES)) ?
      ((float)Math.acos(value) * RAD_TO_DEG) : (float)Math.acos(value);
  }

  public final float atan(float value) {
    return ((g != null) && (g.angleMode == DEGREES)) ?
      ((float)Math.atan(value) * RAD_TO_DEG) : (float)Math.atan(value);
  }

  public final float atan2(float a, float b) {
    return ((g != null) && (g.angleMode == DEGREES)) ?
      ((float)Math.atan2(a, b) * RAD_TO_DEG) : (float)Math.atan2(a, b);
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


  static public final float mag(float a, float b) {
    return (float)Math.sqrt(a*a + b*b);
  }

  static public final float mag(float a, float b, float c) {
    return (float)Math.sqrt(a*a + b*b + c*c);
  }


  static public final float dist(float x1, float y1, float x2, float y2) {
    return sqrt(sq(x2-x1) + sq(y2-y1));
  }

  static public final float dist(float x1, float y1, float z1,
                                 float x2, float y2, float z2) {
    return sqrt(sq(x2-x1) + sq(y2-y1) + sq(z2-z1));
  }



  //////////////////////////////////////////////////////////////

  // RANDOM NUMBERS


  Random internalRandom;

  /**
   * Return a random number in the range [0, howbig)
   * (0 is inclusive, non-inclusive of howbig)
   */
  public final float random(float howbig) {
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
  public final float random(float howsmall, float howbig) {
    if (howsmall >= howbig) return howsmall;
    float diff = howbig - howsmall;
    return random(diff) + howsmall;
  }


  public final void randomSeed(long what) {
    // internal random number object
    if (internalRandom == null) internalRandom = new Random();
    internalRandom.setSeed(what);
  }



  //////////////////////////////////////////////////////////////

  // PERLIN NOISE

  // [toxi 040903]
  // octaves and amplitude amount per octave are now user controlled
  // via the noiseDetail() function.

  // [toxi 030902]
  // cleaned up code and now using bagel's cosine table to speed up

  // [toxi 030901]
  // implementation by the german demo group farbrausch
  // as used in their demo "art": http://www.farb-rausch.de/fr010src.zip

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

  Random perlinRandom;


  /**
   * Computes the Perlin noise function value at the point (x, y, z).
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   * @return the noise function value at (x, y, z)
   */
  public float noise(float x) {
    // is this legit? it's a dumb way to do it (but repair it later)
    return noise(x, 0f, 0f);
  }

  public float noise(float x, float y) {
    return noise(x, y, 0f);
  }

  public float noise(float x, float y, float z) {
    if (perlin == null) {
      if (perlinRandom == null) {
        perlinRandom = new Random();
      }
      perlin = new float[PERLIN_SIZE + 1];
      for (int i = 0; i < PERLIN_SIZE + 1; i++) {
        perlin[i] = perlinRandom.nextFloat(); //(float)Math.random();
      }
      // [toxi 031112]
      // noise broke due to recent change of cos table in PGraphics
      // this will take care of it
      perlin_cosTable = PGraphics.cosLUT;
      perlin_TWOPI = perlin_PI = PGraphics.SINCOS_LENGTH;
      perlin_PI >>= 1;
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

  public void noiseSeed(long what) {
    if (perlinRandom == null) perlinRandom = new Random();
    perlinRandom.setSeed(what);
  }



  //////////////////////////////////////////////////////////////

  // SOUND I/O


  public PSound loadSound(String filename) {
    if (PApplet.jdkVersion >= 1.3) {
      return new PSound2(this, openStream(filename));
    }
    return new PSound(this, openStream(filename));
  }



  //////////////////////////////////////////////////////////////

  // IMAGE I/O


  public PImage loadImage(String filename) {
    if (filename.toLowerCase().endsWith(".tga")) {
      return loadImageTGA(filename);
    }
    return loadImage(filename, true);
  }


  // returns null if no image of that name is found
  public PImage loadImage(String filename, boolean force) {
    Image awtImage =
      Toolkit.getDefaultToolkit().createImage(loadBytes(filename));

    MediaTracker tracker = new MediaTracker(this);
    tracker.addImage(awtImage, 0);
    try {
      tracker.waitForAll();
    } catch (InterruptedException e) {
      e.printStackTrace();  // non-fatal, right?
    }

    PImage image = new PImage(awtImage);

    // if it's a .gif image, test to see if it has transparency
    if (filename.toLowerCase().endsWith(".gif")) {
      for (int i = 0; i < image.pixels.length; i++) {
        // since transparency is often at corners, hopefully this
        // will find a non-transparent pixel quickly and exit
        if ((image.pixels[i] & 0xff000000) != 0xff000000) {
          image.format = ARGB;
        }
      }
    }
    return image;
  }


  /**
   * [toxi 040304] Targa bitmap loader for 24/32bit RGB(A)
   *
   * [fry] this could be optimized to not use loadBytes
   * which would help out memory situations with large images
   */
  protected PImage loadImageTGA(String filename) {
    // load image file as byte array
    byte[] buffer = loadBytes(filename);

    // check if it's a TGA and has 8bits/colour channel
    if (buffer[2] == 2 && buffer[17] == 8) {
      // get image dimensions
      //int w=(b2i(buffer[13])<<8) + b2i(buffer[12]);
      int w = ((buffer[13] & 0xff) << 8) + (buffer[12] & 0xff);
      //int h=(b2i(buffer[15])<<8) + b2i(buffer[14]);
      int h = ((buffer[15] & 0xff) << 8) + (buffer[14] & 0xff);
      // check if image has alpha
      boolean hasAlpha=(buffer[16] == 32);

      // setup new image object
      PImage img = new PImage(w,h);
      img.format = (hasAlpha ? ARGB : RGB);

      // targa's are written upside down, so we need to parse it in reverse
      int index = (h-1) * w;
      // actual bitmap data starts at byte 18
      int offset = 18;

      // read out line by line
      for (int y = h-1; y >= 0; y--) {
        for (int x = 0; x < w; x++) {
          img.pixels[index + x] =
            (buffer[offset++] & 0xff) |
            ((buffer[offset++] & 0xff) << 8) |
            ((buffer[offset++] & 0xff) << 16) |
            (hasAlpha ? ((buffer[offset++] & 0xff) << 24) : 0xff000000);
        }
        index -= w;
      }
      return img;
    }
    die("loadImage(): bad targa image format");
    return null;
  }



  //////////////////////////////////////////////////////////////

  // FONT I/O


  public PFont loadFont(String filename) {
    try {
      String lower = filename.toLowerCase();
      InputStream input = openStream(filename);

      if (lower.endsWith(".vlw.gz")) {
        input = new GZIPInputStream(input);

      } else if (!lower.endsWith(".vlw")) {
        throw new IOException("I don't know how to load a font named " +
                              filename);
      }
      return new PFont(input);

    } catch (Exception e) {
      die("Could not load font " + filename + "\n" +
          "Make sure that the font has been copied\n" +
          "to the data folder of your sketch.", e);
    }
    return null;
  }



  //////////////////////////////////////////////////////////////

  // FILE INPUT


  public File inputFile() {
    return inputFile("Select a file...");
  }

  public File inputFile(String prompt) {
    Frame parentFrame = null;
    Component comp = getParent();
    while (comp != null) {
      if (comp instanceof Frame) {
        parentFrame = (Frame) comp;
        break;
      }
      comp = comp.getParent();
    }
    //System.out.println("found frame " + frame);
    if (parentFrame == null) parentFrame = new Frame();
    FileDialog fd = new FileDialog(parentFrame, prompt, FileDialog.LOAD);
    fd.show();

    String directory = fd.getDirectory();
    String filename = fd.getFile();
    if (filename == null) return null;
    return new File(directory, filename);
  }


  public File outputFile() {
    return outputFile("Save as...");
  }

  public File outputFile(String prompt) {
    Frame parentFrame = null;
    Component comp = getParent();
    //System.out.println(comp + " " + comp.getClass());
    while (comp != null) {
      System.out.println(comp + " " + comp.getClass());
      if (comp instanceof Frame) {
        parentFrame = (Frame) comp;
        break;
      }
      comp = comp.getParent();
    }
    //System.out.println("found frame " + frame);
    if (parentFrame == null) parentFrame = new Frame();
    FileDialog fd = new FileDialog(parentFrame, prompt, FileDialog.SAVE);
    fd.show();

    String directory = fd.getDirectory();
    String filename = fd.getFile();
    if (filename == null) return null;
    return new File(directory, filename);
  }


  /**
   * I want to read lines from a file. I have RSI from typing these
   * eight lines of code so many times.
   */
  public BufferedReader reader(String filename) {
    try {
      return reader(openStream(filename));

    } catch (Exception e) {
      if (filename == null) {
        die("Filename passed to reader() was null", e);
      } else {
        die("Couldn't create a reader for " + filename, e);
      }
    }
    return null;
  }


  /**
   * I want to read lines from a file. And I'm still annoyed.
   */
  public BufferedReader reader(File file) {
    try {
      return reader(new FileInputStream(file));

    } catch (Exception e) {
      if (file == null) {
        die("File object passed to reader() was null", e);
      } else {
        die("Couldn't create a reader for " + file.getAbsolutePath(), e);
      }
    }
    return null;
  }


  /**
   * I want to read lines from a stream. If I have to type the
   * following lines any more I'm gonna send Sun my medical bills.
   */
  public BufferedReader reader(InputStream input) {
    //try {
    InputStreamReader isr = new InputStreamReader(input);
    return new BufferedReader(isr);
    //} catch (IOException e) {
    //die("Couldn't create reader()", e);
    //}
    //return null;
  }


  /**
   * decode a gzip input stream
   */
  public InputStream gzipInput(InputStream input) {
    try {
      return new GZIPInputStream(input);
    } catch (IOException e) {
      die("Problem with gzip input", e);
    }
    return null;
  }

  /**
   * decode a gzip output stream
   */
  public OutputStream gzipOutput(OutputStream output) {
    try {
      return new GZIPOutputStream(output);
    } catch (IOException e) {
      die("Problem with gzip output", e);
    }
    return null;
  }


  /**
   * I want to print lines to a file. Why can't I?
   */
  public PrintWriter writer(String filename) {
    try {
      return writer(new FileOutputStream(savePath(filename)));

    } catch (Exception e) {
      if (filename == null) {
        die("Filename passed to writer() was null", e);
      } else {
        die("Couldn't create a writer for " + filename, e);
      }
    }
    return null;
  }

  /**
   * I want to print lines to a file. I have RSI from typing these
   * eight lines of code so many times.
   */
  public PrintWriter writer(File file) {
    try {
      return writer(new FileOutputStream(file));

    } catch (Exception e) {
      if (file == null) {
        die("File object passed to writer() was null", e);
      } else {
        die("Couldn't create a writer for " + file.getAbsolutePath(), e);
      }
    }
    return null;
  }

  /**
   * I want to print lines to a file. Why am I always explaining myself?
   * It's the JavaSoft API engineers who need to explain themselves.
   */
  public PrintWriter writer(OutputStream output) {
    //try {
    OutputStreamWriter osw = new OutputStreamWriter(output);
    return new PrintWriter(osw);
    //} catch (IOException e) {
    //die("Couldn't create writer()", e);
    //}
  }


  public InputStream openStream(File file) {
    try {
      return new FileInputStream(file);

    } catch (IOException e) {
      if (file == null) {
        die("File passed to openStream() was null", e);
      } else {
        die("Couldn't openStream() for " + file.getAbsolutePath());
      }
    }
    return null;
  }


  public InputStream openStream(String filename) {
    try {
      InputStream stream = null;

      if (filename.startsWith("http://")) {
        try {
          URL url = new URL(filename);
          stream = url.openStream();
          return stream;

        } catch (MalformedURLException e) {
          e.printStackTrace();
          return null;
        }
      }

      stream = getClass().getResourceAsStream(filename);
      if (stream != null) return stream;

      stream = getClass().getResourceAsStream("data/" + filename);
      if (stream != null) return stream;

      try {
        try {
          String location = folder + File.separator + "data";
          File file = new File(location, filename);
          stream = new FileInputStream(file);
          if (stream != null) return stream;

        } catch (Exception e) { }  // ignored

        try {
          File file = new File(folder, filename);
          stream = new FileInputStream(file);
          if (stream != null) return stream;

        } catch (Exception e) { }  // ignored

        try {
          stream = new FileInputStream(new File("data", filename));
          if (stream != null) return stream;
        } catch (IOException e2) { }

        try {
          stream = new FileInputStream(filename);
          if (stream != null) return stream;
        } catch (IOException e1) { }

      } catch (SecurityException se) { }  // online, whups

      if (stream == null) {
        throw new IOException("openStream() could not open " + filename);
      }
    } catch (Exception e) {
      die(e.getMessage(), e);
    }
    return null;  // #$(*@ compiler
  }


  public byte[] loadBytes(String filename) {
    return loadBytes(openStream(filename));
  }

  public byte[] loadBytes(InputStream input) {
    try {
      BufferedInputStream bis = new BufferedInputStream(input);
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      int c = bis.read();
      while (c != -1) {
        out.write(c);
        c = bis.read();
      }
      return out.toByteArray();

    } catch (IOException e) {
      die("Couldn't load bytes from stream", e);
    }
    return null;
  }


  public String[] loadStrings(File file) {
    InputStream is = openStream(file);
    if (is != null) return loadStrings(is);

    die("Couldn't open " + file.getAbsolutePath());
    return null;
  }

  public String[] loadStrings(String filename) {
    InputStream is = openStream(filename);
    if (is != null) return loadStrings(is);

    die("Couldn't open " + filename);
    return null;
  }

  public String[] loadStrings(InputStream input) {
    try {
      BufferedReader reader =
        new BufferedReader(new InputStreamReader(input));

      String lines[] = new String[100];
      int lineCount = 0;
      String line = null;
      while ((line = reader.readLine()) != null) {
        if (lineCount == lines.length) {
          String temp[] = new String[lineCount << 1];
          System.arraycopy(lines, 0, temp, 0, lineCount);
          lines = temp;
        }
        lines[lineCount++] = line;
      }
      reader.close();

      if (lineCount == lines.length) {
        return lines;
      }

      // resize array to appropraite amount for these lines
      String output[] = new String[lineCount];
      System.arraycopy(lines, 0, output, 0, lineCount);
      return output;

    } catch (IOException e) {
      die("Error inside loadStrings()", e);
    }
    return null;
  }



  //////////////////////////////////////////////////////////////

  // FILE OUTPUT


  /**
   * Saves bytes to a file to inside the sketch folder.
   * The filename can be a relative path, i.e. "poo/bytefun.txt"
   * would save to a file named "bytefun.txt" to a subfolder
   * called 'poo' inside the sketch folder. If the in-between
   * subfolders don't exist, they'll be created.
   */
  public void saveBytes(String filename, byte buffer[]) {
    try {
      String location = savePath(filename);
      FileOutputStream fos = new FileOutputStream(location);
      saveBytes(fos, buffer);
      fos.close();

    } catch (IOException e) {
      System.err.println("error saving bytes to " + filename);
      e.printStackTrace();
    }
  }

  /**
   * Saves bytes to a specific File location specified by the user.
   */
  public void saveBytes(File file, byte buffer[]) {
    try {
      String filename = file.getAbsolutePath();
      createPath(filename);
      FileOutputStream fos = new FileOutputStream(file);
      saveBytes(fos, buffer);
      fos.close();

    } catch (IOException e) {
      System.err.println("error saving bytes to " + file);
      e.printStackTrace();
    }
  }

  /**
   * Spews a buffer of bytes to an OutputStream.
   */
  public void saveBytes(OutputStream output, byte buffer[]) {
    try {
      //BufferedOutputStream bos = new BufferedOutputStream(output);
      output.write(buffer);
      output.flush();

    } catch (IOException e) {
      System.err.println("error while saving bytes");
      e.printStackTrace();
    }
  }

  //

  public void saveStrings(String filename, String strings[]) {
    try {
      String location = savePath(filename);
      FileOutputStream fos = new FileOutputStream(location);
      saveStrings(fos, strings);
      fos.close();

    } catch (IOException e) {
      System.err.println("error while saving strings");
      e.printStackTrace();
    }
  }


  public void saveStrings(File file, String strings[]) {
    try {
      String location = file.getAbsolutePath();
      createPath(location);
      FileOutputStream fos = new FileOutputStream(location);
      saveStrings(fos, strings);
      fos.close();

    } catch (IOException e) {
      System.err.println("error while saving strings");
      e.printStackTrace();
    }
  }

  public void saveStrings(OutputStream output, String strings[]) {
    PrintWriter writer =
      new PrintWriter(new OutputStreamWriter(output));
    for (int i = 0; i < strings.length; i++) {
      writer.println(strings[i]);
    }
    writer.flush();
  }


  //


  /**
   * Figures out the full path for where to save things.
   * Can be used by external libraries to save to the sketch folder.
   */
  public String savePath(String where) {
    String filename = folder + File.separator + where;
    createPath(filename);
    return filename;
  }

  /**
   * Creates in-between folders if they don't already exist.
   */
  static public void createPath(String filename) {
    File file = new File(filename);
    String parent = file.getParent();
    if (parent != null) {
      File unit = new File(parent);
      if (!unit.exists()) unit.mkdirs();
    }
  }


  //////////////////////////////////////////////////////////////

  // SORT

  int sort_mode;

  static final int BYTES   = 1;
  static final int CHARS   = 2;
  static final int INTS    = 3;
  static final int FLOATS  = 4;
  static final int STRINGS = 5;

  byte sort_bytes[];
  char sort_chars[];
  int sort_ints[];
  float sort_floats[];
  String sort_strings[];


  public byte[] sort(byte what[]) {
    return sort(what, what.length);
  }

  public char[] sort(char what[]) {
    return sort(what, what.length);
  }

  public int[] sort(int what[]) {
    return sort(what, what.length);
  }

  public float[] sort(float what[]) {
    return sort(what, what.length);
  }

  public String[] sort(String what[]) {
    return sort(what, what.length);
  }

  //

  public byte[] sort(byte what[], int count) {
    if (count == 0) return null;
    sort_mode = BYTES;
    sort_bytes = new byte[count];
    System.arraycopy(what, 0, sort_bytes, 0, count);
    sort_internal(0, count-1);
    return sort_bytes;
  }

  public char[] sort(char what[], int count) {
    if (count == 0) return null;
    sort_mode = CHARS;
    sort_chars = new char[count];
    System.arraycopy(what, 0, sort_chars, 0, count);
    sort_internal(0, count-1);
    return sort_chars;
  }

  public int[] sort(int what[], int count) {
    if (count == 0) return null;
    sort_mode = INTS;
    sort_ints = new int[count];
    System.arraycopy(what, 0, sort_ints, 0, count);
    sort_internal(0, count-1);
    return sort_ints;
  }

  public float[] sort(float what[], int count) {
    if (count == 0) return null;
    sort_mode = FLOATS;
    sort_floats = new float[count];
    System.arraycopy(what, 0, sort_floats, 0, count);
    sort_internal(0, count-1);
    return sort_floats;
  }

  public String[] sort(String what[], int count) {
    if (count == 0) return null;
    sort_mode = STRINGS;
    sort_strings = new String[count];
    System.arraycopy(what, 0, sort_strings, 0, count);
    sort_internal(0, count-1);
    return sort_strings;
  }

  //

  protected void sort_internal(int i, int j) {
    int pivotIndex = (i+j)/2;
    sort_swap(pivotIndex, j);
    int k = sort_partition(i-1, j);
    sort_swap(k, j);
    if ((k-i) > 1) sort_internal(i, k-1);
    if ((j-k) > 1) sort_internal(k+1, j);
  }


  protected int sort_partition(int left, int right) {
    int pivot = right;
    do {
      while (sort_compare(++left, pivot) < 0) { }
      while ((right != 0) && (sort_compare(--right, pivot) > 0)) { }
      sort_swap(left, right);
    } while (left < right);
    sort_swap(left, right);
    return left;
  }


  protected void sort_swap(int a, int b) {
    switch (sort_mode) {
    case BYTES:
      byte btemp = sort_bytes[a];
      sort_bytes[a] = sort_bytes[b];
      sort_bytes[b] = btemp;
      break;
    case CHARS:
      char ctemp = sort_chars[a];
      sort_chars[a] = sort_chars[b];
      sort_chars[b] = ctemp;
      break;
    case INTS:
      int itemp = sort_ints[a];
      sort_ints[a] = sort_ints[b];
      sort_ints[b] = itemp;
      break;
    case FLOATS:
      float ftemp = sort_floats[a];
      sort_floats[a] = sort_floats[b];
      sort_floats[b] = ftemp;
      break;
    case STRINGS:
      String stemp = sort_strings[a];
      sort_strings[a] = sort_strings[b];
      sort_strings[b] = stemp;
      break;
    }
  }

  protected int sort_compare(int a, int b) {
    switch (sort_mode) {
    case BYTES:
      return sort_bytes[a] - sort_bytes[b];
      //if (sort_bytes[a] < sort_bytes[b]) return -1;
      //return (sort_bytes[a] == sort_bytes[b]) ? 0 : 1;
    case CHARS:
      return sort_chars[a] - sort_chars[b];
      //if (sort_chars[a] < sort_chars[b]) return -1;
      //return (sort_chars[a] == sort_chars[b]) ? 0 : 1;
    case INTS:
      return sort_ints[a] - sort_ints[b];
      //if (sort_ints[a] < sort_ints[b]) return -1;
      //return (sort_ints[a] == sort_ints[b]) ? 0 : 1;
    case FLOATS:
      if (sort_floats[a] < sort_floats[b]) return -1;
      return (sort_floats[a] == sort_floats[b]) ? 0 : 1;
    case STRINGS:
      return sort_strings[a].compareTo(sort_strings[b]);
    }
    return 0;
  }



  //////////////////////////////////////////////////////////////

  // ARRAY UTILITIES


  static public boolean[] expand(boolean list[]) {
    return expand(list, list.length << 1);
  }

  static public boolean[] expand(boolean list[], int newSize) {
    boolean temp[] = new boolean[newSize];
    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
    return temp;
  }


  static public byte[] expand(byte list[]) {
    return expand(list, list.length << 1);
  }

  static public byte[] expand(byte list[], int newSize) {
    byte temp[] = new byte[newSize];
    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
    return temp;
  }


  static public char[] expand(char list[]) {
    return expand(list, list.length << 1);
  }

  static public char[] expand(char list[], int newSize) {
    char temp[] = new char[newSize];
    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
    return temp;
  }


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

  //

  static public boolean[] contract(boolean list[], int newSize) {
    return expand(list, newSize);
  }

  static public byte[] contract(byte list[], int newSize) {
    return expand(list, newSize);
  }

  static public char[] contract(char list[], int newSize) {
    return expand(list, newSize);
  }

  static public int[] contract(int list[], int newSize) {
    return expand(list, newSize);
  }

  static public float[] contract(float list[], int newSize) {
    return expand(list, newSize);
  }

  static public String[] contract(String list[], int newSize) {
    return expand(list, newSize);
  }

  //

  static public byte[] append(byte b[], byte value) {
    b = expand(b, b.length + 1);
    b[b.length-1] = value;
    return b;
  }

  static public char[] append(char b[], char value) {
    b = expand(b, b.length + 1);
    b[b.length-1] = value;
    return b;
  }

  static public int[] append(int b[], int value) {
    b = expand(b, b.length + 1);
    b[b.length-1] = value;
    return b;
  }

  static public float[] append(float b[], float value) {
    b = expand(b, b.length + 1);
    b[b.length-1] = value;
    return b;
  }

  static public String[] append(String b[], String value) {
    b = expand(b, b.length + 1);
    b[b.length-1] = value;
    return b;
  }

  //

  static public boolean[] shorten(boolean list[]) {
    return contract(list, list.length-1);
  }

  static public byte[] shorten(byte list[]) {
    return contract(list, list.length-1);
  }

  static public char[] shorten(char list[]) {
    return contract(list, list.length-1);
  }

  static public int[] shorten(int list[]) {
    return contract(list, list.length-1);
  }

  static public float[] shorten(float list[]) {
    return contract(list, list.length-1);
  }

  static public String[] shorten(String list[]) {
    return contract(list, list.length-1);
  }

  //

  static final public boolean[] splice(boolean list[],
                                       boolean v, int index) {
    boolean outgoing[] = new boolean[list.length + 1];
    System.arraycopy(list, 0, outgoing, 0, index);
    outgoing[index] = v;
    System.arraycopy(list, index, outgoing, index + 1,
                     list.length - index);
    return outgoing;
  }

  static final public boolean[] splice(boolean list[],
                                       boolean v[], int index) {
    boolean outgoing[] = new boolean[list.length + v.length];
    System.arraycopy(list, 0, outgoing, 0, index);
    System.arraycopy(v, 0, outgoing, index, v.length);
    System.arraycopy(list, index, outgoing, index + v.length,
                     list.length - index);
    return outgoing;
  }


  static final public byte[] splice(byte list[],
                                    byte v, int index) {
    byte outgoing[] = new byte[list.length + 1];
    System.arraycopy(list, 0, outgoing, 0, index);
    outgoing[index] = v;
    System.arraycopy(list, index, outgoing, index + 1,
                     list.length - index);
    return outgoing;
  }

  static final public byte[] splice(byte list[],
                                    byte v[], int index) {
    byte outgoing[] = new byte[list.length + v.length];
    System.arraycopy(list, 0, outgoing, 0, index);
    System.arraycopy(v, 0, outgoing, index, v.length);
    System.arraycopy(list, index, outgoing, index + v.length,
                     list.length - index);
    return outgoing;
  }


  static final public char[] splice(char list[],
                                    char v, int index) {
    char outgoing[] = new char[list.length + 1];
    System.arraycopy(list, 0, outgoing, 0, index);
    outgoing[index] = v;
    System.arraycopy(list, index, outgoing, index + 1,
                     list.length - index);
    return outgoing;
  }

  static final public char[] splice(char list[],
                                    char v[], int index) {
    char outgoing[] = new char[list.length + v.length];
    System.arraycopy(list, 0, outgoing, 0, index);
    System.arraycopy(v, 0, outgoing, index, v.length);
    System.arraycopy(list, index, outgoing, index + v.length,
                     list.length - index);
    return outgoing;
  }


  static final public int[] splice(int list[],
                                   int v, int index) {
    int outgoing[] = new int[list.length + 1];
    System.arraycopy(list, 0, outgoing, 0, index);
    outgoing[index] = v;
    System.arraycopy(list, index, outgoing, index + 1,
                     list.length - index);
    return outgoing;
  }

  static final public int[] splice(int list[],
                                   int v[], int index) {
    int outgoing[] = new int[list.length + v.length];
    System.arraycopy(list, 0, outgoing, 0, index);
    System.arraycopy(v, 0, outgoing, index, v.length);
    System.arraycopy(list, index, outgoing, index + v.length,
                     list.length - index);
    return outgoing;
  }


  static final public float[] splice(float list[],
                                     float v, int index) {
    float outgoing[] = new float[list.length + 1];
    System.arraycopy(list, 0, outgoing, 0, index);
    outgoing[index] = v;
    System.arraycopy(list, index, outgoing, index + 1,
                     list.length - index);
    return outgoing;
  }

  static final public float[] splice(float list[],
                                     float v[], int index) {
    float outgoing[] = new float[list.length + v.length];
    System.arraycopy(list, 0, outgoing, 0, index);
    System.arraycopy(v, 0, outgoing, index, v.length);
    System.arraycopy(list, index, outgoing, index + v.length,
                     list.length - index);
    return outgoing;
  }


  static final public String[] splice(String list[],
                                      String v, int index) {
    String outgoing[] = new String[list.length + 1];
    System.arraycopy(list, 0, outgoing, 0, index);
    outgoing[index] = v;
    System.arraycopy(list, index, outgoing, index + 1,
                     list.length - index);
    return outgoing;
  }

  static final public String[] splice(String list[],
                                      String v[], int index) {
    String outgoing[] = new String[list.length + v.length];
    System.arraycopy(list, 0, outgoing, 0, index);
    System.arraycopy(v, 0, outgoing, index, v.length);
    System.arraycopy(list, index, outgoing, index + v.length,
                     list.length - index);
    return outgoing;
  }

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

  //

  static public boolean[] concat(boolean a[], boolean b[]) {
    boolean c[] = new boolean[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public byte[] concat(byte a[], byte b[]) {
    byte c[] = new byte[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public char[] concat(char a[], char b[]) {
    char c[] = new char[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public int[] concat(int a[], int b[]) {
    int c[] = new int[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public float[] concat(float a[], float b[]) {
    float c[] = new float[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public String[] concat(String a[], String b[]) {
    String c[] = new String[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  //

  static public boolean[] reverse(boolean list[]) {
    boolean outgoing[] = new boolean[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public byte[] reverse(byte list[]) {
    byte outgoing[] = new byte[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public char[] reverse(char list[]) {
    char outgoing[] = new char[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public int[] reverse(int list[]) {
    int outgoing[] = new int[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public float[] reverse(float list[]) {
    float outgoing[] = new float[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public String[] reverse(String list[]) {
    String outgoing[] = new String[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }


  //////////////////////////////////////////////////////////////

  // STRINGS


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


  //////////////////////////////////////////////////////////////

  // CASTING FUNCTIONS, INSERTED BY PREPROC


  static final public boolean toBoolean(char what) {
    return ((what == 't') || (what == 'T') || (what == '1'));
  }

  static final public boolean toBoolean(int what) {  // this will cover byte
    return (what != 0);
  }

  static final public boolean toBoolean(float what) {
    return (what != 0);
  }

  static final public boolean toBoolean(String what) {
    return new Boolean(what).booleanValue();
  }

  //

  static final public boolean[] toBoolean(char what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] =
        ((what[i] == 't') || (what[i] == 'T') || (what[i] == '1'));
    }
    return outgoing;
  }

  static final public boolean[] toBoolean(byte what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (what[i] != 0);
    }
    return outgoing;
  }

  static final public boolean[] toBoolean(float what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (what[i] != 0);
    }
    return outgoing;
  }

  static final public boolean[] toBoolean(String what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = new Boolean(what[i]).booleanValue();
    }
    return outgoing;
  }

  //

  static final public byte toByte(boolean what) {
    return what ? (byte)1 : 0;
  }

  static final public byte toByte(char what) {
    return (byte) what;
  }

  static final public byte toByte(int what) {
    return (byte) what;
  }

  static final public byte toByte(float what) {  // nonsensical
    return (byte) what;
  }

  static final public byte[] toByte(String what) {  // note: array[]
    return what.getBytes();
  }

  //

  static final public byte[] toByte(boolean what[]) {
    byte outgoing[] = new byte[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = what[i] ? (byte)1 : 0;
    }
    return outgoing;
  }

  static final public byte[] toByte(char what[]) {
    byte outgoing[] = new byte[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (byte) what[i];
    }
    return outgoing;
  }

  static final public byte[] toByte(int what[]) {
    byte outgoing[] = new byte[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (byte) what[i];
    }
    return outgoing;
  }

  static final public byte[] toByte(float what[]) {  // nonsensical
    byte outgoing[] = new byte[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (byte) what[i];
    }
    return outgoing;
  }

  static final public byte[][] toByte(String what[]) {  // note: array[][]
    byte outgoing[][] = new byte[what.length][];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = what[i].getBytes();
    }
    return outgoing;
  }

  //

  static final public char toChar(boolean what) {  // 0/1 or T/F ?
    return what ? 't' : 'f';
  }

  static final public char toChar(byte what) {
    return (char) (what & 0xff);
  }

  static final public char toChar(int what) {
    return (char) what;
  }

  static final public char toChar(float what) {  // nonsensical
    return (char) what;
  }

  static final public char[] toChar(String what) {  // note: array[]
    return what.toCharArray();
  }

  //

  static final public char[] toChar(boolean what[]) {  // 0/1 or T/F ?
    char outgoing[] = new char[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = what[i] ? 't' : 'f';
    }
    return outgoing;
  }

  static final public char[] toChar(int what[]) {
    char outgoing[] = new char[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (char) what[i];
    }
    return outgoing;
  }

  static final public char[] toChar(byte what[]) {
    char outgoing[] = new char[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (char) (what[i] & 0xff);
    }
    return outgoing;
  }

  static final public char[] toChar(float what[]) {  // nonsensical
    char outgoing[] = new char[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (char) what[i];
    }
    return outgoing;
  }

  static final public char[][] toChar(String what[]) {  // note: array[][]
    char outgoing[][] = new char[what.length][];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = what[i].toCharArray();
    }
    return outgoing;
  }

  //

  static final public int toInt(boolean what) {
    return what ? 1 : 0;
  }

  static final public int toInt(byte what) {  // note this unsigns
    return what & 0xff;
  }

  static final public int toInt(char what) {
    return what;
  }

  static final public int toInt(float what) {
    return (int) what;
  }

  static final public int toInt(String what) {
    try {
      return Integer.parseInt(what);
    } catch (NumberFormatException e) { }
    return 0;
  }

  static final public int toInt(String what, int otherwise) {
    try {
      return Integer.parseInt(what);
    } catch (NumberFormatException e) { }

    return otherwise;
  }

  //

  static final public int[] toInt(boolean what[]) {
    int list[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      list[i] = what[i] ? 1 : 0;
    }
    return list;
  }

  static final public int[] toInt(byte what[]) {  // note this unsigns
    int list[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      list[i] = (what[i] & 0xff);
    }
    return list;
  }

  static final public int[] toInt(char what[]) {
    int list[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      list[i] = what[i];
    }
    return list;
  }

  static public int[] toInt(float what[]) {
    int inties[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      inties[i] = (int)what[i];
    }
    return inties;
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

  //

  static final public float toFloat(boolean what) {
    return what ? 1 : 0;
  }

  static final public float toFloat(int what) {
    return (float)what;
  }

  static final public float toFloat(String what) {
    //return new Float(what).floatValue();
    return toFloat(what, Float.NaN);
  }

  static final public float toFloat(String what, float otherwise) {
    try {
      return new Float(what).floatValue();
    } catch (NumberFormatException e) { }

    return otherwise;
  }

  //

  static final public float[] toFloat(boolean what[]) {
    float floaties[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      floaties[i] = what[i] ? 1 : 0;
    }
    return floaties;
  }

  static final public float[] toFloat(char what[]) {
    float floaties[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      floaties[i] = (char) what[i];
    }
    return floaties;
  }

  static final public float[] toFloat(int what[]) {
    float floaties[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      floaties[i] = what[i];
    }
    return floaties;
  }

  static final public float[] toFloat(String what[]) {
    return toFloat(what, 0);
  }

  static final public float[] toFloat(String what[], float missing) {
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

  //

  static final public String str(boolean x) { return String.valueOf(x); }
  static final public String str(byte x)    { return String.valueOf(x); }
  static final public String str(char x)    { return String.valueOf(x); }
  static final public String str(short x)   { return String.valueOf(x); }
  static final public String str(int x)     { return String.valueOf(x); }
  static final public String str(float x)   { return String.valueOf(x); }
  static final public String str(long x)    { return String.valueOf(x); }
  static final public String str(double x)  { return String.valueOf(x); }

  //

  static final public String[] str(boolean x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static final public String[] str(byte x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static final public String[] str(char x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static final public String[] str(short x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static final public String[] str(int x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static final public String[] str(float x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static final public String[] str(long x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }

  static final public String[] str(double x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x);
    return s;
  }



  //////////////////////////////////////////////////////////////

  // INT NUMBER FORMATTING


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



  //////////////////////////////////////////////////////////////

  // FLOAT NUMBER FORMATTING


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



  //////////////////////////////////////////////////////////////

  // HEX/BINARY CONVERSION


  static final public String hex(byte what) {
    return hex(what, 2);
  }

  static final public String hex(char what) {
    return hex(what, 4);
  }

  static final public String hex(int what) {
    return hex(what, 8);
  }

  static final public String hex(int what, int digits) {
    String stuff = Integer.toHexString(what).toUpperCase();

    int length = stuff.length();
    if (length > digits) {
      return stuff.substring(length - digits);

    } else if (length < digits) {
      return "00000000".substring(8 - (digits-length)) + stuff;
    }
    return stuff;
  }

  static final int unhex(String what) {
    return Integer.parseInt(what, 16);
  }

  //

  /**
   * Returns a String that contains the binary value of a byte.
   * The returned value will always have 8 digits.
   */
  static final public String binary(byte what) {
    return binary(what, 8);
  }

  /**
   * Returns a String that contains the binary value of a char.
   * The returned value will always have 16 digits because chars
   * are two bytes long.
   */
  static final public String binary(char what) {
    return binary(what, 16);
  }

  /**
   * Returns a String that contains the binary value of an int.
   * The length depends on the size of the number itself.
   * An int can be up to 32 binary digits, but that seems like
   * overkill for almost any situation, so this function just
   * auto-sizes. If you want a specific number of digits (like all 32)
   * use binary(int what, int digits) to specify how many digits.
   */
  static final public String binary(int what) {
    return Integer.toBinaryString(what);
    //return binary(what, 32);
  }

  /**
   * Returns a String that contains the binary value of an int.
   * The digits parameter determines how many digits will be used.
   */
  static final public String binary(int what, int digits) {
    String stuff = Integer.toBinaryString(what);

    int length = stuff.length();
    if (length > digits) {
      return stuff.substring(length - digits);

    } else if (length < digits) {
      int offset = 32 - (digits-length);
      return "00000000000000000000000000000000".substring(offset) + stuff;
    }
    return stuff;
  }


  /**
   * Unpack a binary String into an int.
   * i.e. unbinary("00001000") would return 8.
   */
  static final int unbinary(String what) {
    return Integer.parseInt(what, 2);
  }



  //////////////////////////////////////////////////////////////

  // COLOR FUNCTIONS

  // moved here so that they can work without
  // the graphics actually being instantiated (outside setup)


  public final int color(int gray) {
    if (g == null) {
      if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
      return 0xff000000 | (gray << 16) | (gray << 8) | gray;
    }
    return g.color(gray);
  }


  public final int color(float fgray) {
    if (g == null) {
      int gray = (int) fgray;
      if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
      return 0xff000000 | (gray << 16) | (gray << 8) | gray;
    }
    return g.color(fgray);
  }


  public final int color(int gray, int alpha) {
    if (g == null) {
      if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
      if (alpha > 255) alpha = 255; else if (alpha < 0) alpha = 0;
      return (alpha << 24) | (gray << 16) | (gray << 8) | gray;
    }
    return g.color(gray, alpha);
  }


  public final int color(float fgray, float falpha) {
    if (g == null) {
      int gray = (int) fgray;
      int alpha = (int) falpha;
      if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
      if (alpha > 255) alpha = 255; else if (alpha < 0) alpha = 0;
      return 0xff000000 | (gray << 16) | (gray << 8) | gray;
    }
    return g.color(fgray, falpha);
  }


  public final int color(int x, int y, int z) {
    if (g == null) {
      if (x > 255) x = 255; else if (x < 0) x = 0;
      if (y > 255) y = 255; else if (y < 0) y = 0;
      if (z > 255) z = 255; else if (z < 0) z = 0;

      return 0xff000000 | (x << 16) | (y << 8) | z;
    }
    return g.color(x, y, z);
  }


  public final int color(float x, float y, float z) {
    if (g == null) {
      if (x > 255) x = 255; else if (x < 0) x = 0;
      if (y > 255) y = 255; else if (y < 0) y = 0;
      if (z > 255) z = 255; else if (z < 0) z = 0;

      return 0xff000000 | ((int)x << 16) | ((int)y << 8) | (int)z;
    }
    return g.color(x, y, z);
  }


  public final int color(int x, int y, int z, int a) {
    if (g == null) {
      if (a > 255) a = 255; else if (a < 0) a = 0;
      if (x > 255) x = 255; else if (x < 0) x = 0;
      if (y > 255) y = 255; else if (y < 0) y = 0;
      if (z > 255) z = 255; else if (z < 0) z = 0;

      return (a << 24) | (x << 16) | (y << 8) | z;
    }
    return g.color(x, y, z, a);
  }


  public final int color(float x, float y, float z, float a) {
    if (g == null) {
      if (a > 255) a = 255; else if (a < 0) a = 0;
      if (x > 255) x = 255; else if (x < 0) x = 0;
      if (y > 255) y = 255; else if (y < 0) y = 0;
      if (z > 255) z = 255; else if (z < 0) z = 0;

      return ((int)a << 24) | ((int)x << 16) | ((int)y << 8) | (int)z;
    }
    return g.color(x, y, z, a);
  }



  //////////////////////////////////////////////////////////////

  // MAIN


  private static class WorkerVar {
    private Thread thread;
    WorkerVar(Thread t) { thread = t; }
    synchronized Thread get() { return thread; }
    synchronized void clear() { thread = null; }
  }

  class Worker {
    private Object value;
    private WorkerVar workerVar;

    protected synchronized Object getValue() {
      return value;
    }

    private synchronized void setValue(Object x) {
      value = x;
    }

    public Object construct() {
      try {
        int anything = System.in.read();
        if (anything == EXTERNAL_STOP) {

          // adding this for 0073.. need to stop libraries
          // when the stop button is hit.
          PApplet.this.stop();
          finished = true;
        }
      } catch (IOException e) {
        finished = true;
      }
      try {
        Thread.sleep(250);
        //Thread.sleep(100);  // kick up latency for 0075?
      } catch (InterruptedException e) { }
      return null;
    }

    // removing this from SwingWorker
    //public void finished() { }

    public void interrupt() {
      Thread t = workerVar.get();
      if (t != null) {
        t.interrupt();
      }
      workerVar.clear();
    }

    public Object get() {
      while (true) {
        Thread t = workerVar.get();
        if (t == null) {
          return getValue();
        }
        try {
          t.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt(); // propagate
          return null;
        }
      }
    }

    public Worker() {
      // removing this from SwingWorker
      //final Runnable doFinished = new Runnable() {
      //    public void run() { finished(); }
      //  };

      Runnable doConstruct = new Runnable() {
          public void run() {
            try {
              setValue(construct());

            } finally {
              workerVar.clear();
            }
            // removing this from SwingWorker to avoid swing
            //javax.swing.SwingUtilities.invokeLater(doFinished);
          }
        };

      Thread t = new Thread(doConstruct);
      workerVar = new WorkerVar(t);
    }

    public void start() {
      Thread t = workerVar.get();
      if (t != null) t.start();
    }
  }


  public void setupExternal(Frame parentFrame) {
    //externalRuntime = true;

    /*
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          //while ((Thread.currentThread() == this) && !finished) {
          try {
            // is this what's causing all the trouble?
            int anything = System.in.read();
            if (anything == EXTERNAL_STOP) {
              //System.out.println("********** STOPPING");

              // adding this for 0073.. need to stop libraries
              // when the stop button is hit.
v              PApplet.this.stop();

              //System.out.println("********** REALLY");
              finished = true;
            }
          } catch (IOException e) {
            // not tested (needed?) but seems correct
            //stop();
            finished = true;
            //thread = null;
          }
          try {
            Thread.sleep(250);
            //Thread.sleep(100);  // kick up latency for 0075?
          } catch (InterruptedException e) { }
        }
      });
    */

    /*
    Thread ethread = new Thread() {  //new Runnable() {
        public void run() {
          // this fixes the "code folder hanging bug" (mostly)
          setPriority(Thread.MIN_PRIORITY);
    */
    final Worker worker = new Worker();

    /*
    final SwingWorker worker = new SwingWorker() {
        public Object construct() {
          //while ((Thread.currentThread() == this) && !finished) {
          try {
            // is this what's causing all the trouble?
            int anything = System.in.read();
            if (anything == EXTERNAL_STOP) {
              //System.out.println("********** STOPPING");

              // adding this for 0073.. need to stop libraries
              // when the stop button is hit.
              PApplet.this.stop();

              //System.out.println("********** REALLY");
              finished = true;
            }
          } catch (IOException e) {
            // not tested (needed?) but seems correct
            //stop();
            finished = true;
            //thread = null;
          }
          try {
            Thread.sleep(250);
            //Thread.sleep(100);  // kick up latency for 0075?
          } catch (InterruptedException e) { }
          return null;
        }
      };
    //ethread.start();
    */

    parentFrame.addComponentListener(new ComponentAdapter() {
        public void componentMoved(ComponentEvent e) {
          //System.out.println(e);
          Point where = ((Frame) e.getSource()).getLocation();
          //System.out.println(e);
          System.err.println(PApplet.EXTERNAL_MOVE + " " +
                             where.x + " " + where.y);
          System.err.flush();
        }
      });

    parentFrame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.err.println(PApplet.EXTERNAL_QUIT);
          System.err.flush();  // important
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
      boolean external = false;
      int location[] = null;
      //int locationX, locationY;
      boolean exactLocation = false;
      String folder = System.getProperty("user.dir");
      //if (args[0].indexOf(EXTERNAL_FLAG) == 0) external = true;
      String name = null;

      int argIndex = 0;
      while (argIndex < args.length) {
        if (args[argIndex].indexOf(EXT_LOCATION) == 0) {
          external = true;
          String locationStr =
            args[argIndex].substring(EXT_LOCATION.length());
          location = toInt(split(locationStr, ','));
          //locationX = location[0] - 20;
          //locationY = location[1];

        } else if (args[argIndex].indexOf(EXT_EXACT_LOCATION) == 0) {
          external = true;
          String locationStr =
            args[argIndex].substring(EXT_EXACT_LOCATION.length());
          location = toInt(split(locationStr, ','));
          exactLocation = true;

        } else if (args[argIndex].indexOf(EXT_SKETCH_FOLDER) == 0) {
          folder = args[argIndex].substring(EXT_SKETCH_FOLDER.length());

        } else {
          name = args[argIndex];
          break;
        }
        argIndex++;
      }

      Frame frame = new Frame();
      frame.setResizable(false);  // remove the grow box
      frame.pack();  // get insets. get more.
      //frame.show();  // gl hack
      Class c = Class.forName(name);
      PApplet applet = (PApplet) c.newInstance();
      applet.frame = frame;

      // these are needed before init/start
      applet.folder = folder;
      int argc = args.length - (argIndex+1);
      applet.args = new String[argc];
      System.arraycopy(args, argc, applet.args, 0, argc);

      //System.out.println("calling applet.init");
      applet.init();
      //applet.start();
      //System.out.println("done calling applet.init");

      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

      if (external) {
        Insets insets = frame.getInsets();  // does pack() first above
        //System.out.println(insets);

        int locationX = location[0] - 20;
        int locationY = location[1];

        // applet.width and .height are zero here
        /*
        int minW = 120;
        int minH = 120;
        int windowW =
          Math.max(applet.width, minW) + insets.left + insets.right;
        int windowH =
          Math.max(applet.height, minH) + insets.top + insets.bottom;
        */
        int windowW = 120 + insets.left + insets.right;
        int windowH = 120 + insets.top + insets.bottom;
        frame.setSize(windowW, windowH);

        if (exactLocation) {
          frame.setLocation(location[0], location[1]);

        } else {
          if (locationX - windowW > 10) {
            // if it fits to the left of the window
            frame.setLocation(locationX - windowW, locationY);

          } else {
            // if it fits inside the editor window,
            // offset slightly from upper lefthand corner
            // so that it's plunked inside the text area
            locationX = location[0] + 66;
            locationY = location[1] + 66;

            if ((locationX + windowW > screen.width - 33) ||
                (locationY + windowH > screen.height - 33)) {
              // otherwise center on screen
              locationX = (screen.width - windowW) / 2;
              locationY = (screen.height - windowH) / 2;
            }
            frame.setLocation(locationX, locationY);
          }
        }
        //System.out.println("applet izzat: " + applet.width + " " +
        //                 applet.height);

        frame.setLayout(null);
        frame.add(applet);
        frame.setBackground(SystemColor.control);
        /*
        applet.setBounds((windowW - applet.width)/2,
                         insets.top + ((windowH - insets.top - insets.bottom) -
                                       applet.height)/2,
                         windowW, windowH);
        */
        applet.setBounds((windowW - 100) / 2,
                         insets.top + ((windowH - insets.top - insets.bottom) -
                                       100)/2,
                         windowW, windowH);

        applet.setupExternal(frame);

      } else {  // !external
        //System.out.println("applet not external");

        // remove applet name from args passed in
        applet.args = new String[args.length - 1];
        System.arraycopy(args, 1, applet.args, 0, args.length - 1);

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
      //System.out.println("showing frame");

      frame.show();
      //System.out.println("applet requesting focus");
      applet.requestFocus(); // ask for keydowns
      //System.out.println("exiting main()");

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }



  //////////////////////////////////////////////////////////////

  // everything below this line is automatically generated. no touch.
  // public functions for processing.core


  public void imageMode(int mode) {
     g.imageMode(mode);
  }


  public void smooth() {
     g.smooth();
  }


  public void noSmooth() {
     g.noSmooth();
  }


  public void loadPixels() {
     g.loadPixels();
  }


  public void updatePixels() {
     g.updatePixels();
  }


  public void updatePixels(int x1, int y1, int x2, int y2) {
     g.updatePixels(x1, y1, x2, y2);
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


  public void alpha(int alpha[]) {
     g.alpha(alpha);
  }


  static public void alpha(PImage image, int alpha[]) {
     PGraphics.alpha(image, alpha);
  }


  public void alpha(PImage alpha) {
     g.alpha(alpha);
  }


  static public void alpha(PImage image, PImage alpha) {
     PGraphics.alpha(image, alpha);
  }


  public void filter(int kind) {
     g.filter(kind);
  }


  public void filter(int kind, float param) {
     g.filter(kind, param);
  }


  public void copy(PImage src, int dx, int dy) {
     g.copy(src, dx, dy);
  }


  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
     g.copy(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
  }


  public void copy(PImage src,
                   int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
     g.copy(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
  }


  static public int blend(int c1, int c2, int mode) {
    return PGraphics.blend(c1, c2, mode);
  }


  public void blend(int sx, int sy, int dx, int dy, int mode) {
     g.blend(sx, sy, dx, dy, mode);
  }


  public void blend(PImage src,
                    int sx, int sy, int dx, int dy, int mode) {
     g.blend(src, sx, sy, dx, dy, mode);
  }


  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
     g.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  }


  public void blend(PImage src,
                    int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
     g.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  }


  static public boolean saveHeaderTIF(OutputStream output,
                                      int width, int height) {
    return PGraphics.saveHeaderTIF(output, width, height);
  }


  static public boolean saveTIF(OutputStream output, int pixels[],
                                int width, int height) {
    return PGraphics.saveTIF(output, pixels, width, height);
  }


  static public boolean saveHeaderTGA(OutputStream output,
                                      int width, int height) {
    return PGraphics.saveHeaderTGA(output, width, height);
  }


  static public boolean saveTGA(OutputStream output, int pixels[],
                                int width, int height) {
    return PGraphics.saveTGA(output, pixels, width, height);
  }


  public void save(String filename) {
     g.save(filename);
  }


  public void beginFrame() {
     g.beginFrame();
  }


  public void endFrame() {
     g.endFrame();
  }


  public void defaults() {
     g.defaults();
  }


  public void hint(int which) {
     g.hint(which);
  }


  public void unhint(int which) {
     g.unhint(which);
  }


  public void beginShape() {
     g.beginShape();
  }


  public void beginShape(int kind) {
     g.beginShape(kind);
  }


  public void normal(float nx, float ny, float nz) {
     g.normal(nx, ny, nz);
  }


  public void textureMode(int mode) {
     g.textureMode(mode);
  }


  public void texture(PImage image) {
     g.texture(image);
  }


  public void vertex(float x, float y) {
     g.vertex(x, y);
  }


  public void vertex(float x, float y, float z) {
     g.vertex(x, y, z);
  }


  public void vertex(float x, float y, float u, float v) {
     g.vertex(x, y, u, v);
  }


  public void vertex(float x, float y, float z, float u, float v) {
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


  public void ellipse(float a, float b, float c, float d) {
     g.ellipse(a, b, c, d);
  }


  public void arc(float a, float b, float c, float d,
                  float start, float stop) {
     g.arc(a, b, c, d, start, stop);
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


  public float bezierPoint(float a, float b, float c, float d, float t) {
    return g.bezierPoint(a, b, c, d, t);
  }


  public float bezierTangent(float a, float b, float c, float d, float t) {
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


  public float curvePoint(float a, float b, float c, float d, float t) {
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


  public void image(PImage image, float x, float y) {
     g.image(image, x, y);
  }


  public void image(PImage image,
                    float x, float y, float c, float d) {
     g.image(image, x, y, c, d);
  }


  public void image(PImage image,
                    float a, float b, float c, float d,
                    int u1, int v1, int u2, int v2) {
     g.image(image, a, b, c, d, u1, v1, u2, v2);
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


  public void text(String s, float x1, float y1, float x2, float y2) {
     g.text(s, x1, y1, x2, y2);
  }


  public void text(String s, float x1, float y1, float z, float x2, float y2) {
     g.text(s, x1, y1, z, x2, y2);
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


  public void translate(float tx, float ty) {
     g.translate(tx, ty);
  }


  public void translate(float tx, float ty, float tz) {
     g.translate(tx, ty, tz);
  }


  public void angleMode(int mode) {
     g.angleMode(mode);
  }


  public void rotate(float angle) {
     g.rotate(angle);
  }


  public void rotateX(float angle) {
     g.rotateX(angle);
  }


  public void rotateY(float angle) {
     g.rotateY(angle);
  }


  public void rotateZ(float angle) {
     g.rotateZ(angle);
  }


  public void rotate(float angle, float vx, float vy, float vz) {
     g.rotate(angle, vx, vy, vz);
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


  public void push() {
     g.push();
  }


  public void pop() {
     g.pop();
  }


  public void resetMatrix() {
     g.resetMatrix();
  }


  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
     g.applyMatrix(n00, n01, n02, n10, n11, n12);
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


  public void cameraMode(int mode) {
     g.cameraMode(mode);
  }


  public void beginCamera() {
     g.beginCamera();
  }


  public void endCamera() {
     g.endCamera();
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


  public void printCamera() {
     g.printCamera();
  }


  public float screenX(float x, float y) {
    return g.screenX(x, y);
  }


  public float screenY(float x, float y) {
    return g.screenY(x, y);
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


  public void lights() {
     g.lights();
  }


  public void noLights() {
     g.noLights();
  }


  public void light(int num, float x, float y, float z,
                    float red, float green, float blue) {
     g.light(num, x, y, z, red, green, blue);
  }


  public void lightEnable(int num) {
     g.lightEnable(num);
  }


  public void lightDisable(int num) {
     g.lightDisable(num);
  }


  public void lightPosition(int num, float x, float y, float z) {
     g.lightPosition(num, x, y, z);
  }


  public void lightAmbient(int num, float x, float y, float z) {
     g.lightAmbient(num, x, y, z);
  }


  public void lightDiffuse(int num, float x, float y, float z) {
     g.lightDiffuse(num, x, y, z);
  }


  public void lightSpecular(int num, float x, float y, float z) {
     g.lightSpecular(num, x, y, z);
  }


  public void colorMode(int mode) {
     g.colorMode(mode);
  }


  public void colorMode(int mode, float max) {
     g.colorMode(mode, max);
  }


  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ) {
     g.colorMode(mode, maxX, maxY, maxZ);
  }


  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ, float maxA) {
     g.colorMode(mode, maxX, maxY, maxZ, maxA);
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


  public void strokeCap(int cap) {
     g.strokeCap(cap);
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


  public void mask(int alpha[]) {
     g.mask(alpha);
  }


  public void mask(PImage alpha) {
     g.mask(alpha);
  }
}
