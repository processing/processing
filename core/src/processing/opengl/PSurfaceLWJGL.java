package processing.opengl;

//import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
//import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.lang.reflect.Field;

import javax.swing.JFrame;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PSurface;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

public class PSurfaceLWJGL implements PSurface {
  GraphicsDevice displayDevice;
  PApplet sketch;
  PGraphics graphics;

  int sketchWidth;
  int sketchHeight;

  Frame frame;
  // Note that x and y may not be zero, depending on the display configuration
  Rectangle screenRect;

  PLWJGL pgl;

  boolean fullScreenRequested;

  int cursorType = PConstants.ARROW; // cursor type
  boolean cursorVisible = true; // cursor visibility flag
  Cursor invisibleCursor;
  Cursor currentCursor;

  // ........................................................

  // Event handling

  boolean externalMessages = false;

  /** Poller threads to get the keyboard/mouse events from LWJGL */
  protected static KeyPoller keyPoller;
  protected static MousePoller mousePoller;

  protected static DisplayResarter restarter;

  Thread thread;
  boolean paused;
  Object pauseObject = new Object();

  /** As of release 0116, frameRate(60) is called as a default */
  protected float frameRateTarget = 60;
//  protected long frameRatePeriod = 1000000000L / 60L;

  PSurfaceLWJGL(PGraphics graphics) {
    this.graphics = graphics;
    this.pgl = (PLWJGL) ((PGraphicsOpenGL)graphics).pgl;
  }

  @Override
  public void initOffscreen() {
  }

  @Override
  public Canvas initCanvas(PApplet sketch) {
    this.sketch = sketch;

    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();

    Canvas canvas = new Canvas();
    canvas.setFocusable(true);
    canvas.requestFocus();
    canvas.setBounds(0, 0, sketchWidth, sketchHeight);
    try {
      Display.setParent(canvas);
      return canvas;
    } catch (LWJGLException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Frame initFrame(PApplet sketch, Color backgroundColor,
                         int deviceIndex, boolean fullScreen, boolean spanDisplays) {
    this.sketch = sketch;

    GraphicsEnvironment environment =
        GraphicsEnvironment.getLocalGraphicsEnvironment();

      DisplayMode desktopMode = Display.getDesktopDisplayMode();
      PApplet.println("DESKTOP MODES");
      PApplet.println(desktopMode);
      PApplet.println("ALL MODES");
      try {
        DisplayMode[] allModes = Display.getAvailableDisplayModes();
        for (DisplayMode mode: allModes) {
          PApplet.println(mode);
        }
      } catch (LWJGLException e) {
        e.printStackTrace();
      }

      if (deviceIndex >= 0) {  // if -1, use the default device
        GraphicsDevice[] devices = environment.getScreenDevices();
        if (deviceIndex < devices.length) {
          displayDevice = devices[deviceIndex];
        } else {
          System.err.format("Display %d does not exist, " +
            "using the default display instead.", deviceIndex);
          for (int i = 0; i < devices.length; i++) {
            System.err.format("Display %d is %s\n", i, devices[i]);
          }
        }
      }
      if (displayDevice == null) {
        displayDevice = environment.getDefaultScreenDevice();
      }

      // Need to save the window bounds at full screen,
      // because pack() will cause the bounds to go to zero.
      // http://dev.processing.org/bugs/show_bug.cgi?id=923
      screenRect = spanDisplays ? getDisplaySpan() :
        displayDevice.getDefaultConfiguration().getBounds();

    // Set the displayWidth/Height variables inside PApplet, so that they're
    // usable and can even be returned by the sketchWidth()/Height() methods.
    sketch.displayWidth = screenRect.width;
    sketch.displayHeight = screenRect.height;

    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();

    // Sketch has already requested to be the same as the screen's
    // width and height, so let's roll with full screen mode.
    if (screenRect.width == sketchWidth &&
        screenRect.height == sketchHeight) {
      fullScreen = true;
    }

//    if (fullScreen || spanDisplays) {
    if (spanDisplays) {
      sketchWidth = screenRect.width;
      sketchHeight = screenRect.height;
    }

    if (fullScreen) {
      // Called here because the graphics device is needed before we can
      // determine whether the sketch wants size(displayWidth, displayHeight),
      // and getting the graphics device will be PSurface-specific.
      PApplet.hideMenuBar();

      // Useful hidden switches:
      // http://wiki.lwjgl.org/index.php?title=LWJGL_Hidden_Switches
      System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
      fullScreenRequested = true;
    }

    if (graphics.is2X()) {
      // http://forum.lwjgl.org/index.php?topic=5084.0
      System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "true");
//      pgl.pixel_scale = 2;
    }

    pgl.reqNumSamples = graphics.quality;

    System.err.println("DISPLAY PARENT: " + Display.getParent());
    System.err.println("DISPLAY SMOOTH: " + pgl.reqNumSamples);

    try {
      int argb = graphics.backgroundColor;
      float r = ((argb >> 16) & 0xff) / 255.0f;
      float g = ((argb >> 8) & 0xff) / 255.0f;
      float b = ((argb) & 0xff) / 255.0f;
      Display.setInitialBackground(r, g, b);
      if (fullScreen) {
        Display.setDisplayMode(new DisplayMode(screenRect.width, screenRect.height));
      } else {
        Display.setDisplayMode(new DisplayMode(sketchWidth, sketchHeight));
      }

      System.err.println(sketchWidth + " " + sketchHeight);
      if (fullScreenRequested) {
        Display.setFullscreen(true);
      }
    } catch (LWJGLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    restarter = new DisplayResarter();
    restarter.start();


//    sketchWidth = sketch.width = sketch.sketchWidth();
//    sketchHeight = sketch.height = sketch.sketchHeight();

    frame = new DummyFrame();
    return frame;
  }

  // get the bounds for all displays
  static Rectangle getDisplaySpan() {
    Rectangle bounds = new Rectangle();
    GraphicsEnvironment environment =
      GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (GraphicsDevice device : environment.getScreenDevices()) {
      for (GraphicsConfiguration config : device.getConfigurations()) {
        Rectangle2D.union(bounds, config.getBounds(), bounds);
      }
    }
    return bounds;
  }

  @Override
  public void setTitle(String title) {
    Display.setTitle(title);

  }

  @Override
  public void setVisible(boolean visible) {
    // Apparently not possible:
    // http://forum.lwjgl.org/index.php?topic=5388.0
    System.err.println("Sorry, cannot set visibility of window in OpenGL");
  }

  @Override
  public void setResizable(boolean resizable) {
    Display.setResizable(resizable);
  }

  @Override
  public void placeWindow(int[] location) {
    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      Display.setLocation(location[0], location[1]);
    } else {  // just center on screen
      // Can't use frame.setLocationRelativeTo(null) because it sends the
      // frame to the main display, which undermines the --display setting.
      setFrameCentered();
    }

    if (Display.getY() < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      Display.setLocation(Display.getX(), 30);
    }
  }

  @Override
  public void placeWindow(int[] location, int[] editorLocation) {
    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      frame.setLocation(location[0], location[1]);
    } else if (editorLocation != null) {
      Dimension window = new Dimension(sketchWidth, sketchHeight);

      int locationX = editorLocation[0] - 20;
      int locationY = editorLocation[1];

      if (locationX - window.width > 10) {
        // if it fits to the left of the window
        Display.setLocation(locationX - window.width, locationY);

      } else {  // doesn't fit
        // if it fits inside the editor window,
        // offset slightly from upper lefthand corner
        // so that it's plunked inside the text area
        locationX = editorLocation[0] + 66;
        locationY = editorLocation[1] + 66;

        if ((locationX + window.width > sketch.displayWidth - 33) ||
            (locationY + window.height > sketch.displayHeight - 33)) {
          // otherwise center on screen
          locationX = (sketch.displayWidth - window.width) / 2;
          locationY = (sketch.displayHeight - window.height) / 2;
        }
        Display.setLocation(locationX, locationY);
      }
    } else {  // just center on screen
      setFrameCentered();
    }

    if (Display.getY() < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      Display.setLocation(Display.getX(), 30);
    }
  }

  JFrame presentFrame;
  Canvas presentCanvas;

  boolean presentMode = false;
  float offsetX;
  float offsetY;
  @Override
  public void placePresent(Color stopColor) {

    if (sketchWidth < screenRect.width || sketchHeight < screenRect.height) {
      System.err.println("WILL USE FBO");

     presentMode = pgl.presentMode = true;
     offsetX = pgl.offsetX = 0.5f * (screenRect.width - sketchWidth);
     offsetY = pgl.offsetY = 0.5f * (screenRect.height - sketchHeight);
     pgl.requestFBOLayer();



//     try {
////      Display.setFullscreen(true);
//       Display.setDisplayMode(new DisplayMode(screenRect.width, screenRect.height));
//    } catch (LWJGLException e1) {
//      // TODO Auto-generated catch block
//      e1.printStackTrace();
//    }

     /*
      presentFrame = new JFrame(displayDevice.getDefaultConfiguration());
      presentFrame.getContentPane().setBackground(WINDOW_BGCOLOR);


//      presentCanvas = new Canvas();
//      presentCanvas.setFocusable(true);
//      presentCanvas.requestFocus();
//      presentCanvas.setBounds(0, 0, sketchWidth, sketchHeight);
//
//      Insets insets = presentFrame.getInsets();
//      int windowW = Math.max(sketchWidth, MIN_WINDOW_WIDTH) +
//        insets.left + insets.right;
//      int windowH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT) +
//        insets.top + insets.bottom;
//      presentFrame.setSize(windowW, windowH);
//      presentFrame.setLayout(new BorderLayout());
//      presentFrame.add(presentCanvas, BorderLayout.CENTER);
//
      presentFrame.setUndecorated(true);
      presentFrame.setBounds(screenRect);
      presentFrame.setVisible(true);
      presentFrame.setLayout(null);
      presentFrame.invalidate();
      presentFrame.setResizable(false);
      presentFrame.setAlwaysOnTop(false);
//      presentFrame.toBack();



      if (stopColor != null) {
        Label label = new Label("stop");
        label.setForeground(stopColor);
        label.addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(java.awt.event.MouseEvent e) {
            sketch.exit();
          }
        });
        presentFrame.add(label);

        Dimension labelSize = label.getPreferredSize();
        // sometimes shows up truncated on mac
        //System.out.println("label width is " + labelSize.width);
        labelSize = new Dimension(100, labelSize.height);
        label.setSize(labelSize);
        label.setLocation(20, screenRect.height - labelSize.height - 20);
      }
      */
    }





    /*
    // After the pack(), the screen bounds are gonna be 0s
    frame.setBounds(screenRect);
    canvas.setBounds((screenRect.width - sketchWidth) / 2,
                     (screenRect.height - sketchHeight) / 2,
                     sketchWidth, sketchHeight);

    if (stopColor != null) {
      Label label = new Label("stop");
      label.setForeground(stopColor);
      label.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
          sketch.exit();
        }
      });
      frame.add(label);

      Dimension labelSize = label.getPreferredSize();
      // sometimes shows up truncated on mac
      //System.out.println("label width is " + labelSize.width);
      labelSize = new Dimension(100, labelSize.height);
      label.setSize(labelSize);
      label.setLocation(20, screenRect.height - labelSize.height - 20);
    }
    */
  }

  @Override
  public void setupExternalMessages() {
    externalMessages = true;
  }

  private void setFrameCentered() {
    // Can't use frame.setLocationRelativeTo(null) because it sends the
    // frame to the main display, which undermines the --display setting.
    Display.setLocation(screenRect.x + (screenRect.width - sketchWidth) / 2,
                        screenRect.y + (screenRect.height - sketchHeight) / 2);
  }

  @Override
  public void startThread() {
    if (thread == null) {
      thread = new AnimationThread();
      thread.start();
    } else {
      throw new IllegalStateException("Thread already started in PSurfaceLWJGL");
    }
  }


  @Override
  public void pauseThread() {
    PApplet.debug("PApplet.run() paused, calling object wait...");
    paused = true;
  }

  // halts the animation thread if the pause flag is set
  protected void checkPause() {
    if (paused) {
      synchronized (pauseObject) {
        try {
          pauseObject.wait();
//          PApplet.debug("out of wait");
        } catch (InterruptedException e) {
          // waiting for this interrupt on a start() (resume) call
        }
      }
    }
//    PApplet.debug("done with pause");
  }


  @Override
  public void resumeThread() {
    paused = false;
    synchronized (pauseObject) {
      pauseObject.notifyAll();  // wake up the animation thread
    }
  }


  @Override
  public boolean stopThread() {
    if (thread == null) {
      return false;
    }
    thread = null;
    return true;

  }


  @Override
  public boolean isStopped() {
    return thread == null;
  }


  @Override
  public void setSize(int width, int height) {
    if (frame != null) {
      sketchWidth = sketch.width = width;
      sketchHeight = sketch.height = height;
      graphics.setSize(width, height);
    }
  }


  boolean forceExit = false;
  @Override
  public void setSmooth(int level) {
    System.err.println("set smooth " + level);
    pgl.reqNumSamples = level;
    restarter.changeRequested = true;
  }


  @Override
  public void setFrameRate(float fps) {
    frameRateTarget = fps;
    if (60 < fps) {
      // Disables v-sync
      System.err.println("Disabling VSync");
      Display.setVSyncEnabled(false);
    } else  {
      Display.setVSyncEnabled(true);
    }
  }


  @Override
  public void requestFocus() {
    // seems there is no way of request focus on the LWJGL Display, unless
    // it is parented inside a Canvas:
    // http://www.java-gaming.org/index.php?topic=31158.0
  }


  @Override
  public void blit() {
    // Nothing to do here
  }

  @Override
  public void setCursor(int kind) {
    System.err.println("Sorry, cursor types not supported in OpenGL, provide your cursor image");
  }

  @Override
  public void setCursor(PImage image, int hotspotX, int hotspotY) {
    BufferedImage jimg = (BufferedImage)image.getNative();
    IntBuffer buf = IntBuffer.wrap(jimg.getRGB(0, 0, jimg.getWidth(), jimg.getHeight(),
                                               null, 0, jimg.getWidth()));
    try {
      currentCursor = new Cursor(jimg.getWidth(), jimg.getHeight(),
                                 hotspotX, hotspotY, 1, buf, null);
      Mouse.setNativeCursor(currentCursor);
      cursorVisible = true;
    } catch (LWJGLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void showCursor() {
    if (!cursorVisible) {
      try {
        Mouse.setNativeCursor(currentCursor);
        cursorVisible = true;
      } catch (LWJGLException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void hideCursor() {
    if (invisibleCursor == null) {
      try {
        invisibleCursor = new Cursor(1, 1, 0, 0, 1, BufferUtils.createIntBuffer(1), null);
      } catch (LWJGLException e1) {
        e1.printStackTrace();
      }
    }
    try {
      Mouse.setNativeCursor(invisibleCursor);
      cursorVisible = false;
    } catch (LWJGLException e) {
      e.printStackTrace();
    }
  }

  class AnimationThread extends Thread {

    public AnimationThread() {
      super("Animation Thread");
    }

    /**
     * Main method for the primary animation thread.
     * <A HREF="http://java.sun.com/products/jfc/tsc/articles/painting/">Painting in AWT and Swing</A>
     */
    @Override
    public void run() {  // not good to make this synchronized, locks things up
      try {
//        DisplayMode[] modes = Display.getAvailableDisplayModes();
//        for (DisplayMode mode: modes) {
//          System.err.println(mode.toString());
//        }
//        Display.setDisplayMode(Display.getDesktopDisplayMode());
//
//        Display.create();
//        Display.destroy();
        System.err.println("CREATE THE DISPLAY");
        PixelFormat format = new PixelFormat(PGL.REQUESTED_ALPHA_BITS,
                                             PGL.REQUESTED_DEPTH_BITS,
                                             PGL.REQUESTED_STENCIL_BITS, pgl.reqNumSamples);
        Display.create(format);
      } catch (LWJGLException e) {
        e.printStackTrace();
        System.exit(0);
      }

      boolean deinit = true;

      keyPoller = new KeyPoller(sketch);
      keyPoller.start();

      mousePoller = new MousePoller(sketch);
      mousePoller.start();

//      System.err.println(Mouse.getNativeCursor());

//      long beforeTime = System.nanoTime();
//      long overSleepTime = 0L;

//      int noDelays = 0;
      // Number of frames with a delay of 0 ms before the
      // animation thread yields to other running threads.
//      final int NO_DELAYS_PER_YIELD = 15;

      // If size un-initialized, might be a Canvas. Call setSize() here since
      // we now have a parent object that this Canvas can use as a peer.
//      if (graphics.image == null) {
//        System.out.format("it's null, sketchW/H already set to %d %d%n", sketchWidth, sketchHeight);
      setSize(sketchWidth, sketchHeight);
//        System.out.format("  but now, sketchW/H changed to %d %d%n", sketchWidth, sketchHeight);
//      }

      // un-pause the sketch and get rolling
      sketch.start();

      int x0 = Display.getX();
      int y0 = Display.getY();
      while ((Thread.currentThread() == thread) && !sketch.finished) {
        if (Display.wasResized()) {
          setSize(Display.getWidth(), Display.getHeight());
        }
        pgl.setThread(thread);
        checkPause();



        // Don't resize the renderer from the EDT (i.e. from a ComponentEvent),
        // otherwise it may attempt a resize mid-render.
//        Dimension currentSize = canvas.getSize();
//        if (currentSize.width != sketchWidth || currentSize.height != sketchHeight) {
//          System.err.format("need to resize from %s to %d, %d%n", currentSize, sketchWidth, sketchHeight);
//        }

        // render a single frame
//        if (g != null) {
        sketch.handleDraw();
        Display.update();
//        }

        if (sketch.frameCount == 1) {
          requestFocus();
        }

        // wait for update & paint to happen before drawing next frame
        // this is necessary since the drawing is sometimes in a
        // separate thread, meaning that the next frame will start
        // before the update/paint is completed
//        long afterTime = System.nanoTime();
//        long timeDiff = afterTime - beforeTime;
//        //System.out.println("time diff is " + timeDiff);
//        long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;
//
//        if (sleepTime > 0) {  // some time left in this cycle
//          try {
//            Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
//            noDelays = 0;  // Got some sleep, not delaying anymore
//          } catch (InterruptedException ex) { }
//
//          overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
//
//        } else {    // sleepTime <= 0; the frame took longer than the period
//          overSleepTime = 0L;
//          noDelays++;
//
//          if (noDelays > NO_DELAYS_PER_YIELD) {
//            Thread.yield();   // give another thread a chance to run
//            noDelays = 0;
//          }
//        }
//        beforeTime = System.nanoTime();
        Display.sync((int)frameRateTarget);


        int x = Display.getX();
        int y = Display.getY();
        if (externalMessages && (x != x0 || y0 != y)) {
          System.err.println(PApplet.EXTERNAL_MOVE + " " + x + " " + y);
          System.err.flush();  // doesn't seem to help or hurt
        }
        x0 = x;
        y0 = y;

        if (Display.isCloseRequested()) {
//          if (externalMessages) {
//            System.err.println(PApplet.EXTERNAL_QUIT);
//            System.err.flush();  // important
//          }
          sketch.exit();
          break;
        }
        if (forceExit) {
          System.err.println("QUIT");
          deinit = false;
          break;
        }
//        System.err.println(Display.isActive());
      }

      if (deinit) {
//        if (externalMessages) {
//          sketch.exit();  // don't quit, need to just shut everything down (0133)
//        }

        System.err.println("DESTROY");
        keyPoller.requestStop();
        mousePoller.requestStop();
        sketch.dispose();  // call to shutdown libs?
        Display.destroy();
        frame.dispose();

        // If the user called the exit() function, the window should close,
        // rather than the sketch just halting.
        if (sketch.exitCalled()) {
          sketch.exitActual();
        }
      }


    }
  }

  @SuppressWarnings("serial")
  class DummyFrame extends Frame {

    public DummyFrame() {
      super();
    }

    @Override
    public void setResizable(boolean resizable) {
      Display.setResizable(resizable);
    }

//    @Override
//    public void setLocation(int x, int y) {
//      Display.setLocation(x, y);
//    }

    @Override
    public void setVisible(boolean visible) {
//      Display.isVisible()
//      Display.setVisible(visible);
      System.err.println("Sorry, cannot set visibility of window in OpenGL");
    }

    @Override
    public void setTitle(String title) {
      Display.setTitle(title);
    }
  }

  ///////////////////////////////////////////////////////////

  // LWJGL event handling


  protected class KeyPoller extends Thread {
    protected PApplet parent;
    protected boolean stopRequested;
    protected boolean[] pressedKeys;
    protected char[] charCheys;

    KeyPoller(PApplet parent) {
      this.parent = parent;
      stopRequested = false;
      try {
        Keyboard.create();
      } catch (LWJGLException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void run() {
      pressedKeys = new boolean[256];
      charCheys = new char[256];
      Keyboard.enableRepeatEvents(true);
      while (true) {
        if (stopRequested) break;

        Keyboard.poll();
        while (Keyboard.next()) {
          if (stopRequested) break;

          long millis = Keyboard.getEventNanoseconds() / 1000000L;
          char keyChar = Keyboard.getEventCharacter();
          int keyCode = Keyboard.getEventKey();

          if (keyCode >= pressedKeys.length) continue;

          int modifiers = 0;
          if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
              Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers |= Event.SHIFT;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
              Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers |= Event.CTRL;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) ||
              Keyboard.isKeyDown(Keyboard.KEY_RMETA)) {
            modifiers |= Event.META;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) ||
              Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            // LWJGL maps the menu key and the alt key to the same value.
            modifiers |= Event.ALT;
          }

          int keyPCode = LWJGLtoAWTCode(keyCode);
          if (keyChar == 0) {
            keyChar = PConstants.CODED;
          }

          int action = 0;
          if (Keyboard.getEventKeyState()) {
            action = KeyEvent.PRESS;
            KeyEvent ke = new KeyEvent(null, millis,
                                       action, modifiers,
                                       keyChar, keyPCode);
            parent.postEvent(ke);
            pressedKeys[keyCode] = true;
            charCheys[keyCode] = keyChar;
            keyPCode = 0;
            action = KeyEvent.TYPE;
          } else if (pressedKeys[keyCode]) {
            keyChar = charCheys[keyCode];
            pressedKeys[keyCode] = false;
            action = KeyEvent.RELEASE;
          }

          KeyEvent ke = new KeyEvent(null, millis,
                                     action, modifiers,
                                     keyChar, keyPCode);
          parent.postEvent(ke);
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          // http://stackoverflow.com/questions/1024651/do-i-have-to-worry-about-interruptedexceptions-if-i-dont-interrupt-anything-mys/1024719#1024719
//          e.printStackTrace();
          Thread.currentThread().interrupt(); // restore interrupted status
          break;
        }
      }
    }

    public void requestStop() {
      stopRequested = true;
    }
  }

  protected class DisplayResarter extends Thread {
    boolean changeRequested = false;

    @Override
    public void run() {
      while (true) {
        if (changeRequested) {
          forceExit = true;
          thread.interrupt();
          while (thread.isAlive()) {
//            System.err.println("alive");
            Thread.yield();
          }
          thread = null;

          keyPoller.requestStop();
          keyPoller.interrupt();
          while (keyPoller.isAlive()) {
            Thread.yield();
          }
          mousePoller.requestStop();
          mousePoller.interrupt();
          while (mousePoller.isAlive()) {
            Thread.yield();
          }

          /*
          System.err.println("will destroy surface");
          Display.destroy();
          try {
            Display.setDisplayMode(new DisplayMode(sketchWidth, sketchHeight));
          } catch (LWJGLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
*/
          startThread();



          changeRequested = false;
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
//          e.printStackTrace();
          Thread.currentThread().interrupt(); // restore interrupted status
          break;
        }
      }
    }
  }


  protected class MousePoller extends Thread {
    protected PApplet parent;
    protected boolean stopRequested;
    protected boolean pressed;
    protected boolean inside;
    protected long startedClickTime;
    protected int startedClickButton;

    MousePoller(PApplet parent) {
      this.parent = parent;
      stopRequested = false;
      try {
        Mouse.create();
      } catch (LWJGLException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void run() {
      while (true) {
        if (stopRequested) break;

        Mouse.poll();
        while (Mouse.next()) {
          if (stopRequested) break;

          long millis = Mouse.getEventNanoseconds() / 1000000L;

          int modifiers = 0;
          if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
              Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers |= Event.SHIFT;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
              Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers |= Event.CTRL;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) ||
              Keyboard.isKeyDown(Keyboard.KEY_RMETA)) {
            modifiers |= Event.META;
          }
          if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) ||
              Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            // LWJGL maps the menu key and the alt key to the same value.
            modifiers |= Event.ALT;
          }

//          PApplet.println(Mouse.getX(), Mouse.getY(), offsetX, offsetY);
          int x = Mouse.getX() - (int)offsetX;
          int y = sketchHeight - (Mouse.getY() - (int)offsetY);
          int button = 0;
          if (Mouse.isButtonDown(0)) {
            button = PConstants.LEFT;
          } else if (Mouse.isButtonDown(1)) {
            button = PConstants.RIGHT;
          } else if (Mouse.isButtonDown(2)) {
            button = PConstants.CENTER;
          }

          int action = 0;
          if (button != 0) {
            if (pressed) {
              action = MouseEvent.DRAG;
            } else {
              action = MouseEvent.PRESS;
              pressed = true;
            }
          } else if (pressed) {
            action = MouseEvent.RELEASE;

            if (presentMode) {
              if (20 < Mouse.getX() && Mouse.getX() < 20 + 100 &&
                  20 < Mouse.getY() && Mouse.getY() < 20 + 50) {
                System.err.println("clicked on exit button");
//              if (externalMessages) {
//                System.err.println(PApplet.EXTERNAL_QUIT);
//                System.err.flush();  // important
//              }
                sketch.exit();
              }
            }

            pressed = false;
          } else {
            action = MouseEvent.MOVE;
          }

          if (inside) {
            if (!Mouse.isInsideWindow()) {
              inside = false;
              action = MouseEvent.EXIT;
            }
          } else {
            if (Mouse.isInsideWindow()) {
              inside = true;
              action = MouseEvent.ENTER;
            }
          }

          int count = 0;
          if (Mouse.getEventButtonState()) {
            startedClickTime = millis;
            startedClickButton = button;
          } else {
            if (action == MouseEvent.RELEASE) {
              boolean clickDetected = millis - startedClickTime < 500;
              if (clickDetected) {
                // post a RELEASE event, in addition to the CLICK event.
                MouseEvent me = new MouseEvent(null, millis, action, modifiers,
                                               x, y, button, count);
                parent.postEvent(me);
                action = MouseEvent.CLICK;
                count = 1;
              }
            }
          }

          MouseEvent me = new MouseEvent(null, millis, action, modifiers,
                                         x, y, button, count);
          parent.postEvent(me);
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
//          e.printStackTrace();
          Thread.currentThread().interrupt(); // restore interrupted status
          break;
        }
      }
    }

    public void requestStop() {
      stopRequested = true;
    }
  }
  /**
   * AWT to LWJGL key constants conversion.
   */
  protected static final int[] LWJGL_KEY_CONVERSION;
  /**
   * Conversion LWJGL -> AWT keycode. Taken from GTGE library
   * https://code.google.com/p/gtge/
   *
   */
  static {
    // LWJGL -> AWT conversion
    // used for keypressed and keyreleased
    // mapping Keyboard.KEY_ -> KeyEvent.VK_
    LWJGL_KEY_CONVERSION = new int[Keyboard.KEYBOARD_SIZE];

    // loops through all of the registered keys in KeyEvent
    Field[] keys = KeyEvent.class.getFields();
    for (int i = 0; i < keys.length; i++) {

      try {
        // Converts the KeyEvent constant name to the LWJGL constant
        // name
        String field = "KEY_" + keys[i].getName().substring(3);
        Field lwjglKey = Keyboard.class.getField(field);

        // print key mapping
        // System.out.println(field + " " + lwjglKey.getInt(null) + "="
        // + keys[i].getInt(null));

        // Sets LWJGL index to be the KeyCode value
        LWJGL_KEY_CONVERSION[lwjglKey.getInt(null)] = keys[i].getInt(null);

      } catch (Exception e) { }
    }

    try {
      LWJGL_KEY_CONVERSION[Keyboard.KEY_BACK] = java.awt.event.KeyEvent.VK_BACK_SPACE;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_LBRACKET] = java.awt.event.KeyEvent.VK_BRACELEFT;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_RBRACKET] = java.awt.event.KeyEvent.VK_BRACERIGHT;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_APOSTROPHE] = java.awt.event.KeyEvent.VK_QUOTE;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_GRAVE] = java.awt.event.KeyEvent.VK_BACK_QUOTE;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_BACKSLASH] = java.awt.event.KeyEvent.VK_BACK_SLASH;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_CAPITAL] = java.awt.event.KeyEvent.VK_CAPS_LOCK;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_NUMLOCK] = java.awt.event.KeyEvent.VK_NUM_LOCK;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_SCROLL] = java.awt.event.KeyEvent.VK_SCROLL_LOCK;

      // two to one buttons mapping
      LWJGL_KEY_CONVERSION[Keyboard.KEY_RETURN] = java.awt.event.KeyEvent.VK_ENTER;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_NUMPADENTER] = java.awt.event.KeyEvent.VK_ENTER;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_LCONTROL] = java.awt.event.KeyEvent.VK_CONTROL;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_RCONTROL] = java.awt.event.KeyEvent.VK_CONTROL;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_LSHIFT] = java.awt.event.KeyEvent.VK_SHIFT;
      LWJGL_KEY_CONVERSION[Keyboard.KEY_RSHIFT] = java.awt.event.KeyEvent.VK_SHIFT;
    }
    catch (Exception e) {
    }
  }


  protected int LWJGLtoAWTCode(int code) {
    try {
      return LWJGL_KEY_CONVERSION[code];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("ERROR: Invalid LWJGL KeyCode " + code);
      return -1;
    }
  }
}
