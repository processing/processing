package processing.lwjgl;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PSurface;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
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
  
  /** Poller threads to get the keyboard/mouse events from LWJGL */
  protected static KeyPoller keyPoller;
  protected static MousePoller mousePoller; 
  
  Thread thread;
  boolean paused;
  Object pauseObject = new Object();  
  
  /** As of release 0116, frameRate(60) is called as a default */
  protected float frameRateTarget = 60;
  protected long frameRatePeriod = 1000000000L / 60L;  
  
  PSurfaceLWJGL(PGraphics graphics) {
    this.graphics = graphics;
    this.pgl = (PLWJGL) ((PGraphicsOpenGL)graphics).pgl;
  }
  
  @Override
  public void initOffscreen() {
  }

  @Override
  public Canvas initCanvas(PApplet sketch) {
    Canvas canvas = new Canvas();
    canvas.setFocusable(true);
    canvas.requestFocus();    
    try {
      Display.setParent(canvas);
      return canvas;
    } catch (LWJGLException e) {
      // TODO Auto-generated catch block
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
        // TODO Auto-generated catch block
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

    if (fullScreen || spanDisplays) {
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
  }

  @Override
  public void placeWindow(int[] location, int[] editorLocation) {
    // TODO Auto-generated method stub
    
  }
  
  private void setFrameCentered() {
    // Can't use frame.setLocationRelativeTo(null) because it sends the
    // frame to the main display, which undermines the --display setting.
    Display.setLocation(screenRect.x + (screenRect.width - sketchWidth) / 2,
                        screenRect.y + (screenRect.height - sketchHeight) / 2);
  } 

  @Override
  public void placePresent(Color stopColor) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setupExternalMessages() {
    // TODO Auto-generated method stub
    
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
  

  @Override
  public void setSmooth(int level) {
    // TODO Auto-generated method stub
    
  }
  

  @Override
  public void setFrameRate(float fps) {
    frameRateTarget = fps;
    frameRatePeriod = (long) (1000000000.0 / frameRateTarget);    
  }

  
  @Override
  public void requestFocus() {
    // TODO Auto-generated method stub
    
  }
  

  @Override
  public void blit() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setCursor(int kind) {
    System.err.println("Sorry, cursor types not supported in OpenGL, provide your cursor image");
    // TODO Auto-generated method stub
//    if (PApplet.platform == PConstants.MACOSX && kind == PConstants.MOVE) {
//      kind = PConstants.HAND;
//    }
//    
//    java.awt.Cursor cursor0 = java.awt.Cursor.getPredefinedCursor(kind);
    
    
//    Cursor cursor1 = Cursor(cursor0.,
//        int height,
//        int xHotspot,
//        int yHotspot,
//        int numImages,
//        java.nio.IntBuffer images,
//        java.nio.IntBuffer delays);
    
    
//    Mouse.setNativeCursor(cursor1);
//    cursorVisible = true;
//    this.cursorType = kind;     
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
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void showCursor() {
    if (!cursorVisible) {
//      setCursor(cursorType);
//      cursorVisible = true;
//      Mouse.setCursor(Cursor.getPredefinedCursor(cursorType));
      try {
        Mouse.setNativeCursor(currentCursor);
        cursorVisible = true;
      } catch (LWJGLException e) {
        // TODO Auto-generated catch block
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
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    }    
    try {
      Mouse.setNativeCursor(invisibleCursor);
      cursorVisible = false;
    } catch (LWJGLException e) {
      // TODO Auto-generated catch block
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

          
        Display.setDisplayMode(new DisplayMode(sketchWidth, sketchHeight));
        System.err.println(sketchWidth + " " + sketchHeight);
        if (fullScreenRequested) {
          Display.setFullscreen(true);
        }
        
//        Display.setDisplayMode(Display.getDesktopDisplayMode());
//        
//        Display.create();                
        
        
        Display.create();
      } catch (LWJGLException e) {
        e.printStackTrace();
        System.exit(0);
      }    
      
      keyPoller = new KeyPoller(sketch);
      keyPoller.start();

      mousePoller = new MousePoller(sketch); 
      mousePoller.start();
      
      System.err.println(Mouse.getNativeCursor());
      
      long beforeTime = System.nanoTime();
      long overSleepTime = 0L;

      int noDelays = 0;
      // Number of frames with a delay of 0 ms before the
      // animation thread yields to other running threads.
      final int NO_DELAYS_PER_YIELD = 15;

      // If size un-initialized, might be a Canvas. Call setSize() here since
      // we now have a parent object that this Canvas can use as a peer.
//      if (graphics.image == null) {
//        System.out.format("it's null, sketchW/H already set to %d %d%n", sketchWidth, sketchHeight);
      setSize(sketchWidth, sketchHeight);
//        System.out.format("  but now, sketchW/H changed to %d %d%n", sketchWidth, sketchHeight);
//      }

      // un-pause the sketch and get rolling
      sketch.start();

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
        long afterTime = System.nanoTime();
        long timeDiff = afterTime - beforeTime;
        //System.out.println("time diff is " + timeDiff);
        long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;

        if (sleepTime > 0) {  // some time left in this cycle
          try {
            Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
            noDelays = 0;  // Got some sleep, not delaying anymore
          } catch (InterruptedException ex) { }

          overSleepTime = (System.nanoTime() - afterTime) - sleepTime;

        } else {    // sleepTime <= 0; the frame took longer than the period
          overSleepTime = 0L;
          noDelays++;

          if (noDelays > NO_DELAYS_PER_YIELD) {
            Thread.yield();   // give another thread a chance to run
            noDelays = 0;
          }
        }
        beforeTime = System.nanoTime();        
//        Display.sync((int)frameRateTarget);
        
        if (Display.isCloseRequested()) break;
      }

      keyPoller.requestStop();
      mousePoller.requestStop();
      sketch.dispose();  // call to shutdown libs?
      Display.destroy();

      // If the user called the exit() function, the window should close,
      // rather than the sketch just halting.
      if (sketch.exitCalled()) {
        sketch.exitActual();
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

    @Override
    public void setVisible(boolean visible) {
//      Display.setVisible(visible);
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
        // TODO Auto-generated catch block
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
          e.printStackTrace();
        }
      }
    }

    public void requestStop() {
      stopRequested = true;
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
//        Mouse.setNativeCursor(null);
      } catch (LWJGLException e) {
        // TODO Auto-generated catch block
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

          int x = Mouse.getX();
          int y = sketchHeight - Mouse.getY();
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
          e.printStackTrace();
        }
      }
    }

    public void requestStop() {
      stopRequested = true;
    }
  }

  // To complete later...
  // http://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html
  // http://processing.org/reference/keyCode.html
  // This might be very useful:
  // http://gtge.googlecode.com/svn/trunk/GTGE%20Add-Ons/src/com/golden/gamedev/engine/lwjgl/LWJGLInput.java
  protected int LWJGLtoAWTCode(int code) {
    switch (code) {
    case Keyboard.KEY_0:
      return java.awt.event.KeyEvent.VK_0;
    case Keyboard.KEY_1:
      return java.awt.event.KeyEvent.VK_1;
    case Keyboard.KEY_2:
      return java.awt.event.KeyEvent.VK_2;
    case Keyboard.KEY_3:
      return java.awt.event.KeyEvent.VK_3;
    case Keyboard.KEY_4:
      return java.awt.event.KeyEvent.VK_4;
    case Keyboard.KEY_5:
      return java.awt.event.KeyEvent.VK_5;
    case Keyboard.KEY_6:
      return java.awt.event.KeyEvent.VK_6;
    case Keyboard.KEY_7:
      return java.awt.event.KeyEvent.VK_7;
    case Keyboard.KEY_8:
      return java.awt.event.KeyEvent.VK_8;
    case Keyboard.KEY_9:
      return java.awt.event.KeyEvent.VK_9;
    case Keyboard.KEY_A:
      return java.awt.event.KeyEvent.VK_A;
    case Keyboard.KEY_B:
      return java.awt.event.KeyEvent.VK_B;
    case Keyboard.KEY_C:
      return java.awt.event.KeyEvent.VK_C;
    case Keyboard.KEY_D:
      return java.awt.event.KeyEvent.VK_D;
    case Keyboard.KEY_E:
      return java.awt.event.KeyEvent.VK_E;
    case Keyboard.KEY_F:
      return java.awt.event.KeyEvent.VK_F;
    case Keyboard.KEY_G:
      return java.awt.event.KeyEvent.VK_G;
    case Keyboard.KEY_H:
      return java.awt.event.KeyEvent.VK_H;
    case Keyboard.KEY_I:
      return java.awt.event.KeyEvent.VK_I;
    case Keyboard.KEY_J:
      return java.awt.event.KeyEvent.VK_J;
    case Keyboard.KEY_K:
      return java.awt.event.KeyEvent.VK_K;
    case Keyboard.KEY_L:
      return java.awt.event.KeyEvent.VK_L;
    case Keyboard.KEY_M:
      return java.awt.event.KeyEvent.VK_M;
    case Keyboard.KEY_N:
      return java.awt.event.KeyEvent.VK_N;
    case Keyboard.KEY_O:
      return java.awt.event.KeyEvent.VK_O;
    case Keyboard.KEY_P:
      return java.awt.event.KeyEvent.VK_P;
    case Keyboard.KEY_Q:
      return java.awt.event.KeyEvent.VK_Q;
    case Keyboard.KEY_R:
      return java.awt.event.KeyEvent.VK_R;
    case Keyboard.KEY_S:
      return java.awt.event.KeyEvent.VK_S;
    case Keyboard.KEY_T:
      return java.awt.event.KeyEvent.VK_T;
    case Keyboard.KEY_U:
      return java.awt.event.KeyEvent.VK_U;
    case Keyboard.KEY_V:
      return java.awt.event.KeyEvent.VK_V;
    case Keyboard.KEY_W:
      return java.awt.event.KeyEvent.VK_W;
    case Keyboard.KEY_X:
      return java.awt.event.KeyEvent.VK_X;
    case Keyboard.KEY_Y:
      return java.awt.event.KeyEvent.VK_Y;
    case Keyboard.KEY_Z:
      return java.awt.event.KeyEvent.VK_Z;
    case Keyboard.KEY_ADD:
      return java.awt.event.KeyEvent.VK_ADD;
    case Keyboard.KEY_APOSTROPHE:
      return java.awt.event.KeyEvent.VK_ASTERISK;
    case Keyboard.KEY_AT:
      return java.awt.event.KeyEvent.VK_AT;
    case Keyboard.KEY_BACK:
      return java.awt.event.KeyEvent.VK_BACK_SPACE;
    case Keyboard.KEY_BACKSLASH:
      return java.awt.event.KeyEvent.VK_BACK_SLASH;
    case Keyboard.KEY_CAPITAL:
      return java.awt.event.KeyEvent.VK_CAPS_LOCK;
    case Keyboard.KEY_CIRCUMFLEX:
      return java.awt.event.KeyEvent.VK_CIRCUMFLEX;
    case Keyboard.KEY_COLON:
      return java.awt.event.KeyEvent.VK_COLON;
    case Keyboard.KEY_COMMA:
      return java.awt.event.KeyEvent.VK_COMMA;
    case Keyboard.KEY_CONVERT:
      return java.awt.event.KeyEvent.VK_CONVERT;
    case Keyboard.KEY_DECIMAL:
      return java.awt.event.KeyEvent.VK_DECIMAL;
    case Keyboard.KEY_DELETE:
      return java.awt.event.KeyEvent.VK_DELETE;
    case Keyboard.KEY_DIVIDE:
      return java.awt.event.KeyEvent.VK_DIVIDE;
    case Keyboard.KEY_DOWN:
      return java.awt.event.KeyEvent.VK_DOWN;
    case Keyboard.KEY_END:
      return java.awt.event.KeyEvent.VK_END;
    case Keyboard.KEY_EQUALS:
      return java.awt.event.KeyEvent.VK_EQUALS;
    case Keyboard.KEY_ESCAPE:
      return java.awt.event.KeyEvent.VK_ESCAPE;
    case Keyboard.KEY_F1:
      return java.awt.event.KeyEvent.VK_F1;
    case Keyboard.KEY_F10:
      return java.awt.event.KeyEvent.VK_F10;
    case Keyboard.KEY_F11:
      return java.awt.event.KeyEvent.VK_F11;
    case Keyboard.KEY_F12:
      return java.awt.event.KeyEvent.VK_F12;
    case Keyboard.KEY_F13:
      return java.awt.event.KeyEvent.VK_F13;
    case Keyboard.KEY_F14:
      return java.awt.event.KeyEvent.VK_F14;
    case Keyboard.KEY_F15:
      return java.awt.event.KeyEvent.VK_F15;
    case Keyboard.KEY_F2:
      return java.awt.event.KeyEvent.VK_F2;
    case Keyboard.KEY_F3:
      return java.awt.event.KeyEvent.VK_F3;
    case Keyboard.KEY_F4:
      return java.awt.event.KeyEvent.VK_F4;
    case Keyboard.KEY_F5:
      return java.awt.event.KeyEvent.VK_F5;
    case Keyboard.KEY_F6:
      return java.awt.event.KeyEvent.VK_F6;
    case Keyboard.KEY_F7:
      return java.awt.event.KeyEvent.VK_F7;
    case Keyboard.KEY_F8:
      return java.awt.event.KeyEvent.VK_F8;
    case Keyboard.KEY_F9:
      return java.awt.event.KeyEvent.VK_F9;
//    case Keyboard.KEY_GRAVE:
    case Keyboard.KEY_HOME:
      return java.awt.event.KeyEvent.VK_HOME;
    case Keyboard.KEY_INSERT:
      return java.awt.event.KeyEvent.VK_INSERT;
    case Keyboard.KEY_LBRACKET:
      return java.awt.event.KeyEvent.VK_BRACELEFT;
    case Keyboard.KEY_LCONTROL:
      return java.awt.event.KeyEvent.VK_CONTROL;
    case Keyboard.KEY_LEFT:
      return java.awt.event.KeyEvent.VK_LEFT;
    case Keyboard.KEY_LMENU:
      return java.awt.event.KeyEvent.VK_ALT;
    case Keyboard.KEY_LMETA:
      return java.awt.event.KeyEvent.VK_META;
    case Keyboard.KEY_LSHIFT:
      return java.awt.event.KeyEvent.VK_SHIFT;
    case Keyboard.KEY_MINUS:
      return java.awt.event.KeyEvent.VK_MINUS;
    case Keyboard.KEY_MULTIPLY:
      return java.awt.event.KeyEvent.VK_MULTIPLY;
//    case Keyboard.KEY_NEXT:
    case Keyboard.KEY_NUMLOCK:
      return java.awt.event.KeyEvent.VK_NUM_LOCK;
    case Keyboard.KEY_NUMPAD0:
      return java.awt.event.KeyEvent.VK_NUMPAD0;
    case Keyboard.KEY_NUMPAD1:
      return java.awt.event.KeyEvent.VK_NUMPAD1;
    case Keyboard.KEY_NUMPAD2:
      return java.awt.event.KeyEvent.VK_NUMPAD2;
    case Keyboard.KEY_NUMPAD3:
      return java.awt.event.KeyEvent.VK_NUMPAD3;
    case Keyboard.KEY_NUMPAD4:
      return java.awt.event.KeyEvent.VK_NUMPAD4;
    case Keyboard.KEY_NUMPAD5:
      return java.awt.event.KeyEvent.VK_NUMPAD5;
    case Keyboard.KEY_NUMPAD6:
      return java.awt.event.KeyEvent.VK_NUMPAD6;
    case Keyboard.KEY_NUMPAD7:
      return java.awt.event.KeyEvent.VK_NUMPAD7;
    case Keyboard.KEY_NUMPAD8:
      return java.awt.event.KeyEvent.VK_NUMPAD8;
    case Keyboard.KEY_NUMPAD9:
      return java.awt.event.KeyEvent.VK_NUMPAD9;
//    case Keyboard.KEY_NUMPADCOMMA:
//    case Keyboard.KEY_NUMPADENTER:
//    case Keyboard.KEY_NUMPADEQUALS:
    case Keyboard.KEY_PAUSE:
      return java.awt.event.KeyEvent.VK_PAUSE;
    case Keyboard.KEY_PERIOD:
      return java.awt.event.KeyEvent.VK_PERIOD;
//    case Keyboard.KEY_POWER:
//    case Keyboard.KEY_PRIOR:
    case Keyboard.KEY_RBRACKET:
      return java.awt.event.KeyEvent.VK_BRACERIGHT;
    case Keyboard.KEY_RCONTROL:
      return java.awt.event.KeyEvent.VK_CONTROL;
    case Keyboard.KEY_RETURN:
      return java.awt.event.KeyEvent.VK_ENTER;
    case Keyboard.KEY_RIGHT:
      return java.awt.event.KeyEvent.VK_RIGHT;
//    case Keyboard.KEY_RMENU:
    case Keyboard.KEY_RMETA:
      return java.awt.event.KeyEvent.VK_META;
    case Keyboard.KEY_RSHIFT:
      return java.awt.event.KeyEvent.VK_SHIFT;
//    case Keyboard.KEY_SCROLL:
    case Keyboard.KEY_SEMICOLON:
      return java.awt.event.KeyEvent.VK_SEMICOLON;
    case Keyboard.KEY_SLASH:
      return java.awt.event.KeyEvent.VK_SLASH;
//    case Keyboard.KEY_SLEEP:
    case Keyboard.KEY_SPACE:
      return java.awt.event.KeyEvent.VK_SPACE;
    case Keyboard.KEY_STOP:
      return java.awt.event.KeyEvent.VK_STOP;
    case Keyboard.KEY_SUBTRACT:
      return java.awt.event.KeyEvent.VK_SUBTRACT;
    case Keyboard.KEY_TAB:
      return java.awt.event.KeyEvent.VK_TAB;
//    case Keyboard.KEY_UNDERLINE:
    case Keyboard.KEY_UP:
      return java.awt.event.KeyEvent.VK_UP;
    default:
      return 0;
    }   
  } 
}
