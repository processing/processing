package processing.opengl;

import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
//import java.awt.Dimension;
import java.awt.Point;
//import java.awt.Frame;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.util.IOUtil.ClassResources;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.InputEvent;
//import com.jogamp.newt.event.WindowAdapter;
//import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PSurface;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PGL;

public class PSurfaceJOGL implements PSurface {
  /** Selected GL profile */
  public static GLProfile profile;

  PJOGL pgl;

  GLWindow window;
  FPSAnimator animator;
  Rectangle screenRect;

  PApplet sketch;
  PGraphics graphics;

  int sketchX;
  int sketchY;
  int sketchWidth;
  int sketchHeight;

  Display display;
  Screen screen;
  List<MonitorDevice> monitors;
  MonitorDevice displayDevice;
  Throwable drawException;
  Object waitObject = new Object();

  NewtCanvasAWT canvas;
  boolean placedWindow = false;
  boolean requestedStart = false;

  float[] currentPixelScale = {0, 0};

  boolean presentMode = false;
  float offsetX;
  float offsetY;


  public PSurfaceJOGL(PGraphics graphics) {
    this.graphics = graphics;
    this.pgl = (PJOGL) ((PGraphicsOpenGL)graphics).pgl;
  }


  public void initOffscreen(PApplet sketch) {
    this.sketch = sketch;

    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();

    if (window != null) {
      canvas = new NewtCanvasAWT(window);
      canvas.setBounds(0, 0, window.getWidth(), window.getHeight());
      canvas.setFocusable(true);
    }
  }


  public void initFrame(PApplet sketch) {/*, int backgroundColor,
                        int deviceIndex, boolean fullScreen,
                        boolean spanDisplays) {*/
    this.sketch = sketch;
    initIcons();
    initScreen();
    initGL();
    initWindow();
    initListeners();
    initAnimator();
  }


  public Object getNative() {
//    if (canvas == null) {
//      initOffscreen(sketch);
//    }
//    return canvas;
    return window;
  }


  protected void initScreen() {
    display = NewtFactory.createDisplay(null);
    display.addReference();
    screen = NewtFactory.createScreen(display, 0);
    screen.addReference();

    monitors = new ArrayList<MonitorDevice>();
    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] awtDevices = environment.getScreenDevices();
    List<MonitorDevice> newtDevices = screen.getMonitorDevices();

    // AWT and NEWT name devices in different ways, depending on the platform,
    // and also appear to order them in different ways. The following code
    // tries to address the differences.
    if (PApplet.platform == PConstants.LINUX) {
      for (GraphicsDevice device: awtDevices) {
        String did = device.getIDstring();
        String[] parts = did.split("\\.");
        String id1 = "";
        if (1 < parts.length) {
          id1 = parts[1].trim();
        }
        MonitorDevice monitor = null;
        int id0 = newtDevices.size() > 0 ? newtDevices.get(0).getId() : 0;
        for (int i = 0; i < newtDevices.size(); i++) {
          MonitorDevice mon = newtDevices.get(i);
          String mid = String.valueOf(mon.getId() - id0);
          if (id1.equals(mid)) {
            monitor = mon;
            break;
          }
        }
        if (monitor != null) {
          monitors.add(monitor);
        }
      }
    } else { // All the other platforms...
      for (GraphicsDevice device: awtDevices) {
        String did = device.getIDstring();
        String[] parts = did.split("Display");
        String id1 = "";
        if (1 < parts.length) {
          id1 = parts[1].trim();
        }
        MonitorDevice monitor = null;
        for (int i = 0; i < newtDevices.size(); i++) {
          MonitorDevice mon = newtDevices.get(i);
          String mid = String.valueOf(mon.getId());
          if (id1.equals(mid)) {
            monitor = mon;
            break;
          }
        }
        if (monitor == null) {
          // Didn't find a matching monitor, try using less stringent id check
          for (int i = 0; i < newtDevices.size(); i++) {
            MonitorDevice mon = newtDevices.get(i);
            String mid = String.valueOf(mon.getId());
            if (-1 < did.indexOf(mid)) {
              monitor = mon;
              break;
            }
          }
        }
        if (monitor != null) {
          monitors.add(monitor);
        }
      }
    }
  }

  protected void initGL() {
//  System.out.println("*******************************");
    if (profile == null) {
      if (PJOGL.profile == 2) {
        try {
          profile = GLProfile.getGL2ES2();
        } catch (GLException ex) {
          profile = GLProfile.getMaxProgrammable(true);
        }
      } else if (PJOGL.profile == 3) {
        try {
          profile = GLProfile.getGL2GL3();
        } catch (GLException ex) {
          profile = GLProfile.getMaxProgrammable(true);
        }
        if (!profile.isGL3()) {
          PGraphics.showWarning("Requested profile GL3 but is not available, got: " + profile);
        }
      } else if (PJOGL.profile == 4) {
        try {
          profile = GLProfile.getGL4ES3();
        } catch (GLException ex) {
          profile = GLProfile.getMaxProgrammable(true);
        }
        if (!profile.isGL4()) {
          PGraphics.showWarning("Requested profile GL4 but is not available, got: " + profile);
        }
      } else throw new RuntimeException(PGL.UNSUPPORTED_GLPROF_ERROR);
    }

    // Setting up the desired capabilities;
    GLCapabilities caps = new GLCapabilities(profile);
    caps.setAlphaBits(PGL.REQUESTED_ALPHA_BITS);
    caps.setDepthBits(PGL.REQUESTED_DEPTH_BITS);
    caps.setStencilBits(PGL.REQUESTED_STENCIL_BITS);

//  caps.setPBuffer(false);
//  caps.setFBO(false);

    pgl.reqNumSamples = PGL.smoothToSamples(graphics.smooth);
    caps.setSampleBuffers(true);
    caps.setNumSamples(pgl.reqNumSamples);
    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);
    pgl.capabilities = caps;
  }


  protected void initWindow() {
    window = GLWindow.create(screen, pgl.capabilities);
    if (displayDevice == null) {
      displayDevice = window.getMainMonitor();
    }

    int displayNum = sketch.sketchDisplay();
    if (displayNum > 0) {  // if -1, use the default device
      if (displayNum <= monitors.size()) {
        displayDevice = monitors.get(displayNum - 1);
      } else {
        System.err.format("Display %d does not exist, " +
          "using the default display instead.%n", displayNum);
        for (int i = 0; i < monitors.size(); i++) {
          System.err.format("Display %d is %s%n", i+1, monitors.get(i));
        }
      }
    }

    boolean spanDisplays = sketch.sketchDisplay() == PConstants.SPAN;
    screenRect = spanDisplays ?
      new Rectangle(0, 0, screen.getWidth(), screen.getHeight()) :
      new Rectangle(0, 0,
                    displayDevice.getViewportInWindowUnits().getWidth(),
                    displayDevice.getViewportInWindowUnits().getHeight());

    // Set the displayWidth/Height variables inside PApplet, so that they're
    // usable and can even be returned by the sketchWidth()/Height() methods.
    sketch.displayWidth = screenRect.width;
    sketch.displayHeight = screenRect.height;

    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();

    boolean fullScreen = sketch.sketchFullScreen();
    // Removing the section below because sometimes people want to do the
    // full screen size in a window, and it also breaks insideSettings().
    // With 3.x, fullScreen() is so easy, that it's just better that way.
    // https://github.com/processing/processing/issues/3545
    /*
    // Sketch has already requested to be the same as the screen's
    // width and height, so let's roll with full screen mode.
    if (screenRect.width == sketchWidth &&
        screenRect.height == sketchHeight) {
      fullScreen = true;
      sketch.fullScreen();
    }
    */

    if (fullScreen || spanDisplays) {
      sketchWidth = screenRect.width;
      sketchHeight = screenRect.height;
    }

    float[] reqSurfacePixelScale;
    if (graphics.is2X()) {
       // Retina
       reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE,
                                            ScalableSurface.AUTOMAX_PIXELSCALE };
    } else {
      // Non-retina
      reqSurfacePixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE,
                                           ScalableSurface.IDENTITY_PIXELSCALE };
    }
    window.setSurfaceScale(reqSurfacePixelScale);
    window.setSize(sketchWidth, sketchHeight);
    setSize(sketchWidth, sketchHeight);
    sketchX = displayDevice.getViewportInWindowUnits().getX();
    sketchY = displayDevice.getViewportInWindowUnits().getY();
    if (fullScreen) {
      PApplet.hideMenuBar();
      window.setTopLevelPosition(sketchX, sketchY);
      placedWindow = true;
      if (spanDisplays) {
        window.setFullscreen(monitors);
      } else {
        window.setFullscreen(true);
      }
    }
  }


  protected void initListeners() {
    NEWTMouseListener mouseListener = new NEWTMouseListener();
    window.addMouseListener(mouseListener);
    NEWTKeyListener keyListener = new NEWTKeyListener();
    window.addKeyListener(keyListener);
    NEWTWindowListener winListener = new NEWTWindowListener();
    window.addWindowListener(winListener);

    DrawListener drawlistener = new DrawListener();
    window.addGLEventListener(drawlistener);
  }


  protected void initAnimator() {
//  System.err.println("1. create animator");
    animator = new FPSAnimator(window, 60);
    drawException = null;
    animator.setUncaughtExceptionHandler(new GLAnimatorControl.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final GLAnimatorControl animator,
                                    final GLAutoDrawable drawable,
                                    final Throwable cause) {
        synchronized (waitObject) {
//        System.err.println("Caught exception: " + cause.getMessage());
          drawException = cause;
          waitObject.notify();
        }
      }
    });

    new Thread(new Runnable() {
      public void run() {
        synchronized (waitObject) {
          try {
            if (drawException == null) waitObject.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
//        System.err.println("Caught exception: " + drawException.getMessage());
          if (drawException != null) {
            Throwable cause = drawException.getCause();
            if (cause instanceof ThreadDeath) {
//            System.out.println("caught ThreadDeath");
//            throw (ThreadDeath)cause;
            } else if (cause instanceof RuntimeException) {
              throw (RuntimeException)cause;
            } else {
              throw new RuntimeException(cause);
            }
          }
        }
      }
    }).start();
  }


  @Override
  public void setTitle(String title) {
    window.setTitle(title);
  }


  @Override
  public void setVisible(boolean visible) {
    window.setVisible(visible);
  }


  @Override
  public void setResizable(boolean resizable) {
    // TODO Auto-generated method stub
  }


  public void setIcon(PImage icon) {
    // TODO Auto-generated method stub
  }


  protected void initIcons() {
    final int[] sizes = { 16, 32, 48, 64, 128, 256, 512 };
    String[] iconImages = new String[sizes.length];
    for (int i = 0; i < sizes.length; i++) {
      iconImages[i] = "/icon/icon-" + sizes[i] + ".png";
    }
    NewtFactory.setWindowIcons(new ClassResources(PApplet.class, iconImages));
  }


//  private void setFrameCentered() {
//  }


  @Override
  public void placeWindow(int[] location, int[] editorLocation) {
//    Dimension dim = new Dimension(sketchWidth, sketchHeight);
//    int contentW = Math.max(sketchWidth, MIN_WINDOW_WIDTH);
//    int contentH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT);

    int x = window.getX() - window.getInsets().getLeftWidth();
    int y = window.getY() - window.getInsets().getTopHeight();
    int w = window.getWidth() + window.getInsets().getTotalWidth();
    int h = window.getHeight() + window.getInsets().getTotalHeight();

    if (location != null) {
//      System.err.println("place window at " + location[0] + ", " + location[1]);
      window.setTopLevelPosition(location[0], location[1]);

    } else if (editorLocation != null) {
//      System.err.println("place window at editor location " + editorLocation[0] + ", " + editorLocation[1]);
      int locationX = editorLocation[0] - 20;
      int locationY = editorLocation[1];

      if (locationX - w > 10) {
        // if it fits to the left of the window
        window.setTopLevelPosition(locationX - w, locationY);

      } else {  // doesn't fit
        // if it fits inside the editor window,
        // offset slightly from upper lefthand corner
        // so that it's plunked inside the text area
        locationX = editorLocation[0] + 66;
        locationY = editorLocation[1] + 66;

        if ((locationX + w > sketch.displayWidth - 33) ||
            (locationY + h > sketch.displayHeight - 33)) {
          // otherwise center on screen
          locationX = (sketch.displayWidth - w) / 2;
          locationY = (sketch.displayHeight - h) / 2;
        }
        window.setTopLevelPosition(locationX, locationY);
      }
    } else {  // just center on screen
      // Can't use frame.setLocationRelativeTo(null) because it sends the
      // frame to the main display, which undermines the --display setting.
      int sketchX = displayDevice.getViewportInWindowUnits().getX();
      int sketchY = displayDevice.getViewportInWindowUnits().getY();
//      System.err.println("just center on the screen at " + sketchX + screenRect.x + (screenRect.width - sketchWidth) / 2 + ", " +
//                                                           sketchY + screenRect.y + (screenRect.height - sketchHeight) / 2);
  //
//      System.err.println("  Display starts at " +  sketchX + ", " + sketchY);
//      System.err.println("  Screen rect pos: " +  screenRect.x + ", " + screenRect.y);
//      System.err.println("  Screen rect w/h: " +  screenRect.width + ", " + screenRect.height);
//      System.err.println("  Sketch w/h: " +  sketchWidth + ", " + sketchHeight);

//      int w = sketchWidth;
//      int h = sketchHeight;
//      if (graphics.is2X()) {
//        w /= 2;
//        h /= 2;
//      }

      window.setTopLevelPosition(sketchX + screenRect.x + (screenRect.width - sketchWidth) / 2,
                                 sketchY + screenRect.y + (screenRect.height - sketchHeight) / 2);

    }

    Point frameLoc = new Point(x, y);
    if (frameLoc.y < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      window.setTopLevelPosition(frameLoc.x, 30);
    }

    placedWindow = true;
    if (requestedStart) startThread();
//    canvas.setBounds((contentW - sketchWidth)/2,
//                     (contentH - sketchHeight)/2,
//                     sketchWidth, sketchHeight);
  }


  public void placePresent(int stopColor) {

//    if (presentMode) {
//      System.err.println("Present mode");
//    System.err.println("WILL USE FBO");
    presentMode = pgl.presentMode = true;
    offsetX = pgl.offsetX = 0.5f * (screenRect.width - sketchWidth);
    offsetY = pgl.offsetY = 0.5f * (screenRect.height - sketchHeight);
    pgl.requestFBOLayer();

    window.setSize(screenRect.width, screenRect.height);
    PApplet.hideMenuBar();
    window.setTopLevelPosition(sketchX + screenRect.x,
                               sketchY + screenRect.y);
//    window.setTopLevelPosition(0, 0);
    window.setFullscreen(true);


    placedWindow = true;
    if (requestedStart) startThread();

//    }
  }


  public void setupExternalMessages() {
    // TODO Auto-generated method stub

  }


  public void startThread() {
    if (animator != null) {
      if (placedWindow) {
        window.setVisible(true);
        animator.start();
        requestedStart = false;
      } else {
        // The GL window is not visible until it has been placed, so we cannot
        // start the animator because it requires the window to be visible.
        requestedStart = true;
        // Need this assignment to bypass the while loop in runSketch, otherwise
        // the programs hangs waiting for defaultSize to be false, but it never
        // happens because the animation thread is not yet running to avoid showing
        // the window in the wrong place:
        // https://github.com/processing/processing/issues/3308
//      sketch.defaultSize = false;
      }
    }
  }

  public void pauseThread() {
    if (animator != null) {
      animator.pause();
    }
  }

  public void resumeThread() {
    if (animator != null) {
      animator.resume();
    }
  }

  public boolean stopThread() {
    if (animator != null) {
      return animator.stop();
    } else {
      return false;
    }
  }

  public boolean isStopped() {
    if (animator != null) {
      return !animator.isAnimating();
    } else {
      return true;
    }
  }


  public void setLocation(int x, int y) {
    if (window != null) {
      window.setTopLevelPosition(x, y);
    }
  }


  public void setSize(int width, int height) {
    if (width == sketch.width && height == sketch.height) {
      return;
    }


//    if (animator.isAnimating()) {
//      System.err.println("3. set size");

      if (!presentMode) {
//        sketch.width = width;
//        sketch.height = height;
        sketch.setSize(width, height);
        sketchWidth = width;
        sketchHeight = height;
        graphics.setSize(width, height);
        window.setSize(width, height);
      }


//    }
  }

  public float getPixelScale() {
    if (graphics.is2X()) {
      // Even if the graphics are retina, the user might have moved the window
      // into a non-retina monitor, so we need to check
      window.getCurrentSurfaceScale(currentPixelScale);
      return currentPixelScale[0];
    } else {
      return 1;
    }
  }

  public Component getComponent() {
    return canvas;
  }

  public void setSmooth(int level) {
    pgl.reqNumSamples = level;
    GLCapabilities caps = new GLCapabilities(profile);
    caps.setAlphaBits(PGL.REQUESTED_ALPHA_BITS);
    caps.setDepthBits(PGL.REQUESTED_DEPTH_BITS);
    caps.setStencilBits(PGL.REQUESTED_STENCIL_BITS);
    caps.setSampleBuffers(true);
    caps.setNumSamples(pgl.reqNumSamples);
    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);
    NativeSurface target = window.getNativeSurface();
    MutableGraphicsConfiguration config = (MutableGraphicsConfiguration) target.getGraphicsConfiguration();
    config.setChosenCapabilities(caps);
  }

  public void setFrameRate(float fps) {
    if (animator != null) {
      animator.stop();
      animator.setFPS((int)fps);
      pgl.setFps(fps);
      animator.start();
    }
  }

  public void requestFocus() {
    if (window != null) {
      window.requestFocus();
    }
  }

  class DrawListener implements GLEventListener {
    public void display(GLAutoDrawable drawable) {
      pgl.getGL(drawable);
//      System.out.println(" - " + sketch.frameCount);
      sketch.handleDraw();

      if (sketch.frameCount == 1) {
        requestFocus();
      }

      if (sketch.exitCalled()) {
//        System.out.println("exit");
        animator.stop();
        sketch.dispose();
        sketch.exitActual();
      }
    }
    public void dispose(GLAutoDrawable drawable) {
//      pgl.getGL(drawable);
//      System.out.println("dispose");
      sketch.dispose();
//      if (sketch.exitCalled()) {
//        sketch.exitActual();
//      }
    }
    public void init(GLAutoDrawable drawable) {
//      System.err.println("2. init drawable");
      pgl.getGL(drawable);
      pgl.init(drawable);
      sketch.start();
//      setSize(sketchWidth, sketchHeight);

      int c = graphics.backgroundColor;
      pgl.clearColor(((c >> 16) & 0xff) / 255f,
                     ((c >>  8) & 0xff) / 255f,
                     ((c >>  0) & 0xff) / 255f,
                     ((c >> 24) & 0xff) / 255f);
      pgl.clear(PGL.COLOR_BUFFER_BIT);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {

//      final float[] valReqSurfacePixelScale = window.getRequestedSurfaceScale(new float[2]);
      window.getCurrentSurfaceScale(currentPixelScale);
//      final float[] nativeSurfacePixelScale = window.getMaximumSurfaceScale(new float[2]);
//      System.err.println("[set PixelScale post]: "+
//                         valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
//                         hasSurfacePixelScale[0]+"x"+hasSurfacePixelScale[1]+" (has), "+
//                         nativeSurfacePixelScale[0]+"x"+nativeSurfacePixelScale[1]+" (native)");




//      System.out.println("reshape: " + w + ", " + h);
      pgl.getGL(drawable);
//      if (!graphics.is2X() && 1 < hasSurfacePixelScale[0]) {
//        setSize(w/2, h/2);
//      } else {
//        setSize(w, h);
//      }
      setSize((int)(w/currentPixelScale[0]), (int)(h/currentPixelScale[1]));
    }
  }

  protected class NEWTWindowListener implements com.jogamp.newt.event.WindowListener {
    public NEWTWindowListener() {
      super();
    }
    @Override
    public void windowGainedFocus(com.jogamp.newt.event.WindowEvent arg0) {
//      pg.parent.focusGained(null);
//      System.err.println("gain focus");
      sketch.focused = true;
      sketch.focusGained();
    }

    @Override
    public void windowLostFocus(com.jogamp.newt.event.WindowEvent arg0) {
//      pg.parent.focusLost(null);
//      System.err.println("lost focus");
      sketch.focused = false;
      sketch.focusLost();
    }

    @Override
    public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent arg0) {
//      System.err.println("bye");
      PSurfaceJOGL.this.sketch.dispose();
      PSurfaceJOGL.this.sketch.exitActual();
    }

    @Override
    public void windowDestroyed(com.jogamp.newt.event.WindowEvent arg0) {
    }

    @Override
    public void windowMoved(com.jogamp.newt.event.WindowEvent arg0) {
    }

    @Override
    public void windowRepaint(com.jogamp.newt.event.WindowUpdateEvent arg0) {
    }

    @Override
    public void windowResized(com.jogamp.newt.event.WindowEvent arg0) {
//      System.err.println("resized");
//      System.err.println(window.hasFocus());
//      window.removeMouseListener(mouseListener);
//      mouseListener = new NEWTMouseListener();
//      window.addMouseListener(mouseListener);
    }

  }

  // NEWT mouse listener
  protected class NEWTMouseListener extends com.jogamp.newt.event.MouseAdapter {
    public NEWTMouseListener() {
      super();
    }
    @Override
    public void mousePressed(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.PRESS);
    }
    @Override
    public void mouseReleased(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.RELEASE);
    }
    @Override
    public void mouseClicked(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.CLICK);
    }
    @Override
    public void mouseDragged(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.DRAG);
    }
    @Override
    public void mouseMoved(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.MOVE);
    }
    @Override
    public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent e) {
      nativeMouseEvent(e, MouseEvent.WHEEL);
    }
    @Override
    public void mouseEntered(com.jogamp.newt.event.MouseEvent e) {
//      System.out.println("enter");
      nativeMouseEvent(e, MouseEvent.ENTER);
    }
    @Override
    public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
//      System.out.println("exit");
      nativeMouseEvent(e, MouseEvent.EXIT);
    }
  }

  // NEWT key listener
  protected class NEWTKeyListener extends com.jogamp.newt.event.KeyAdapter {
    public NEWTKeyListener() {
      super();
    }
    @Override
    public void keyPressed(com.jogamp.newt.event.KeyEvent e) {
      nativeKeyEvent(e, KeyEvent.PRESS);
    }
    @Override
    public void keyReleased(com.jogamp.newt.event.KeyEvent e) {
      nativeKeyEvent(e, KeyEvent.RELEASE);
    }
    public void keyTyped(com.jogamp.newt.event.KeyEvent e)  {
      nativeKeyEvent(e, KeyEvent.TYPE);
    }
  }

  protected void nativeMouseEvent(com.jogamp.newt.event.MouseEvent nativeEvent,
                                  int peAction) {
    int modifiers = nativeEvent.getModifiers();
    int peModifiers = modifiers &
                      (InputEvent.SHIFT_MASK |
                       InputEvent.CTRL_MASK |
                       InputEvent.META_MASK |
                       InputEvent.ALT_MASK);

    int peButton = 0;
    if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
      peButton = PConstants.LEFT;
    } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
      peButton = PConstants.CENTER;
    } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
      peButton = PConstants.RIGHT;
    }

    if (PApplet.platform == PConstants.MACOSX) {
      //if (nativeEvent.isPopupTrigger()) {
      if ((modifiers & InputEvent.CTRL_MASK) != 0) {
        peButton = PConstants.RIGHT;
      }
    }

    int peCount = 0;
    if (peAction == MouseEvent.WHEEL) {
      peCount = nativeEvent.isShiftDown() ? (int)nativeEvent.getRotation()[0] :
                                            (int)nativeEvent.getRotation()[1];
    } else {
      peCount = nativeEvent.getClickCount();
    }

    window.getCurrentSurfaceScale(currentPixelScale);
    int sx = (int)(nativeEvent.getX()/currentPixelScale[0]);
    int sy = (int)(nativeEvent.getY()/currentPixelScale[1]);
    int mx = sx - (int)offsetX;
    int my = sy - (int)offsetY;

    if (presentMode) {
      if (peAction == KeyEvent.RELEASE &&
          20 < sx && sx < 20 + 100 &&
          screenRect.height - 70 < sy && sy < screenRect.height - 20) {
//        System.err.println("clicked on exit button");
//      if (externalMessages) {
//        System.err.println(PApplet.EXTERNAL_QUIT);
//        System.err.flush();  // important
//      }
//        animator.stop();
        sketch.exit();
//        window.destroy();
      }

      if (mx < 0 || sketchWidth < mx || my < 0 || sketchHeight < my) {
        return;
      }
    }

//    if (!graphics.is2X() && 1 < hasSurfacePixelScale[0]) {
//      x /= 2;
//      y /= 2;
//    }

    MouseEvent me = new MouseEvent(nativeEvent, nativeEvent.getWhen(),
                                   peAction, peModifiers,
                                   mx, my,
                                   peButton,
                                   peCount);

    sketch.postEvent(me);
  }

  protected void nativeKeyEvent(com.jogamp.newt.event.KeyEvent nativeEvent,
                                int peAction) {
    int peModifiers = nativeEvent.getModifiers() &
                      (InputEvent.SHIFT_MASK |
                       InputEvent.CTRL_MASK |
                       InputEvent.META_MASK |
                       InputEvent.ALT_MASK);

    short code = nativeEvent.getKeyCode();
    char keyChar;
    int keyCode;
    if (isPCodedKey(code)) {
      keyCode = mapToPConst(code);
      keyChar = PConstants.CODED;
    } else if (isHackyKey(code)) {
      keyCode = code;
      keyChar = hackToChar(code, nativeEvent.getKeyChar());
    } else {
      keyCode = code;
      keyChar = nativeEvent.getKeyChar();
    }

    // From http://jogamp.org/deployment/v2.1.0/javadoc/jogl/javadoc/com/jogamp/newt/event/KeyEvent.html
    // public final short getKeySymbol()
    // Returns the virtual key symbol reflecting the current keyboard layout.
    // public final short getKeyCode()
    // Returns the virtual key code using a fixed mapping to the US keyboard layout.
    // In contrast to key symbol, key code uses a fixed US keyboard layout and therefore is keyboard layout independent.
    // E.g. virtual key code VK_Y denotes the same physical key regardless whether keyboard layout QWERTY or QWERTZ is active. The key symbol of the former is VK_Y, where the latter produces VK_Y.
    KeyEvent ke = new KeyEvent(nativeEvent, nativeEvent.getWhen(),
                               peAction, peModifiers,
                               keyChar,
                               keyCode);

    sketch.postEvent(ke);

    if (!isPCodedKey(code) && !isHackyKey(code)) {
      if (peAction == KeyEvent.PRESS) {
        // Create key typed event
        // TODO: combine dead keys with the following key
        KeyEvent tke = new KeyEvent(nativeEvent, nativeEvent.getWhen(),
                                    KeyEvent.TYPE, peModifiers,
                                    keyChar,
                                    0);

        sketch.postEvent(tke);
      }
    }
  }

  private static boolean isPCodedKey(short code) {
    return code == com.jogamp.newt.event.KeyEvent.VK_UP ||
           code == com.jogamp.newt.event.KeyEvent.VK_DOWN ||
           code == com.jogamp.newt.event.KeyEvent.VK_LEFT ||
           code == com.jogamp.newt.event.KeyEvent.VK_RIGHT ||
           code == com.jogamp.newt.event.KeyEvent.VK_ALT ||
           code == com.jogamp.newt.event.KeyEvent.VK_CONTROL ||
           code == com.jogamp.newt.event.KeyEvent.VK_SHIFT ||
           code == com.jogamp.newt.event.KeyEvent.VK_WINDOWS;
  }

  // Why do we need this mapping?
  // Relevant discussion and links here:
  // http://forum.jogamp.org/Newt-wrong-keycode-for-key-td4033690.html#a4033697
  // (I don't think this is a complete solution).
  private static int mapToPConst(short code) {
    if (code == com.jogamp.newt.event.KeyEvent.VK_UP) {
      return PConstants.UP;
    } else if (code == com.jogamp.newt.event.KeyEvent.VK_DOWN) {
      return PConstants.DOWN;
    } else if (code == com.jogamp.newt.event.KeyEvent.VK_LEFT) {
      return PConstants.LEFT;
    } else if (code == com.jogamp.newt.event.KeyEvent.VK_RIGHT) {
      return PConstants.RIGHT;
    } else if (code == com.jogamp.newt.event.KeyEvent.VK_ALT) {
      return PConstants.ALT;
    } else if (code == com.jogamp.newt.event.KeyEvent.VK_CONTROL) {
      return PConstants.CONTROL;
    } else if (code == com.jogamp.newt.event.KeyEvent.VK_SHIFT) {
      return PConstants.SHIFT;
    } else if (code == com.jogamp.newt.event.KeyEvent.VK_WINDOWS) {
      return java.awt.event.KeyEvent.VK_META;
    }
    return code;
  }

  private static boolean isHackyKey(short code) {
    return code == com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE ||
           code == com.jogamp.newt.event.KeyEvent.VK_TAB;
  }

  private static char hackToChar(short code, char def) {
    if (code == com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE) {
      return '\b';
    } else if (code == com.jogamp.newt.event.KeyEvent.VK_TAB) {
      return '\t';
    }
    return def;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void setCursor(int kind) {
    PGraphics.showWarning("Cursor types not yet supported in OpenGL, provide your cursor image");
  }


  public void setCursor(PImage image, int hotspotX, int hotspotY) {
    final Display disp = window.getScreen().getDisplay();
    disp.createNative();

//    BufferedImage jimg = (BufferedImage)image.getNative();
//    IntBuffer buf = IntBuffer.wrap(jimg.getRGB(0, 0, jimg.getWidth(), jimg.getHeight(),
//                                               null, 0, jimg.getWidth()));
//
//    final PixelRectangle pixelrect = new PixelRectangle.GenericPixelRect(srcFmt, new Dimension(width, height),
//        srcStrideBytes, srcIsGLOriented, srcPixels);
//
//    PointerIcon pi = disp.createPointerIcon(PixelRectangle pixelrect,
//                                            hotspotX,
//                                            hotspotY);
//
//    window.setPointerIcon(pi);

  }

  public void showCursor() {
    if (window != null) {
      window.setPointerVisible(true);
    }
  }

  public void hideCursor() {
    if (window != null) {
      window.setPointerVisible(false);
    }
  }
}
