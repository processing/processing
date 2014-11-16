package processing.opengl;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.media.nativewindow.ScalableSurface;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PSurface;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

public class PSurfaceNEWT implements PSurface {
  /** Selected GL profile */
  public static GLProfile profile;

  PJOGL pgl;

  GLWindow window;
  Frame frame;
  FPSAnimator animator;
  Rectangle screenRect;

  PApplet sketch;
  PGraphics graphics;

  int sketchWidth;
  int sketchHeight;

  MonitorDevice displayDevice;

  public PSurfaceNEWT(PGraphics graphics) {
    this.graphics = graphics;
    this.pgl = (PJOGL) ((PGraphicsOpenGL)graphics).pgl;
  }

  public void initOffscreen() {
    // TODO Auto-generated method stub

  }

  public Frame initFrame(PApplet sketch, Color backgroundColor,
                         int deviceIndex, boolean fullScreen,
                         boolean spanDisplays) {
    this.sketch = sketch;

    Display display = NewtFactory.createDisplay(null);
    display.addReference();
    Screen screen = NewtFactory.createScreen(display, 0);
    screen.addReference();

    ArrayList<MonitorDevice> monitors = new ArrayList<MonitorDevice>();
    for (int i = 0; i < screen.getMonitorDevices().size(); i++) {
      MonitorDevice monitor = screen.getMonitorDevices().get(i);
      System.out.println("Monitor " + monitor.getId() + " ************");
      System.out.println(monitor.toString());
      System.out.println(monitor.getViewportInWindowUnits());
      System.out.println(monitor.getViewport());

      monitors.add(monitor);
    }
    System.out.println("*******************************");

    if (deviceIndex >= 0) {  // if -1, use the default device
      if (deviceIndex < monitors.size()) {
        displayDevice = monitors.get(deviceIndex);
      } else {
        System.err.format("Display %d does not exist, " +
          "using the default display instead.", deviceIndex);
        for (int i = 0; i < monitors.size(); i++) {
          System.err.format("Display %d is %s\n", i, monitors.get(i));
        }
      }
    }

    if (profile == null) {
      if (PJOGL.PROFILE == 2) {
        try {
          profile = GLProfile.getGL2ES1();
        } catch (GLException ex) {
          profile = GLProfile.getMaxFixedFunc(true);
        }
      } else if (PJOGL.PROFILE == 3) {
        try {
          profile = GLProfile.getGL2GL3();
        } catch (GLException ex) {
          profile = GLProfile.getMaxProgrammable(true);
        }
        if (!profile.isGL3()) {
          PGraphics.showWarning("Requested profile GL3 but is not available, got: " + profile);
        }
      } else if (PJOGL.PROFILE == 4) {
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

    caps.setPBuffer(false);
    caps.setFBO(true);

    caps.setSampleBuffers(true);
    caps.setNumSamples(2);
    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);
    pgl.capabilities = caps;
    window = GLWindow.create(screen, caps);


    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();

    if (displayDevice == null) {
      displayDevice = window.getMainMonitor();
    }
    int sketchX = displayDevice.getViewportInWindowUnits().getX();
    int sketchY = displayDevice.getViewportInWindowUnits().getY();

    int screenWidth = screen.getWidth();
    int screenHeight = screen.getHeight();

    screenRect = spanDisplays ? new Rectangle(0, 0, screen.getWidth(), screen.getHeight()) :
                                new Rectangle(0, 0, displayDevice.getViewportInWindowUnits().getWidth(),
                                                    displayDevice.getViewportInWindowUnits().getHeight());

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

//    window..setBackground(new Color(backgroundColor, true));
    window.setPosition(sketchX, sketchY);
    window.setSize(sketchWidth, sketchHeight);

    System.out.println("deviceIndex: " + deviceIndex);
    System.out.println(displayDevice);
    System.out.println("Screen res " + screenWidth + "x" + screenHeight);

    // This example could be useful:
    // com.jogamp.opengl.test.junit.newt.mm.TestScreenMode01cNEWT
    if (fullScreen) {
      if (spanDisplays) {
        window.setFullscreen(monitors);
      } else {
        window.setFullscreen(true);
      }
    }

    window.setVisible(true);

    // Retina
//    int[] reqSurfacePixelScale = new int[] { ScalableSurface.AUTOMAX_PIXELSCALE,
//                                             ScalableSurface.AUTOMAX_PIXELSCALE };
//    pgl.pixel_scale = 2;

    // Non-retina
    int[] reqSurfacePixelScale = new int[] { ScalableSurface.IDENTITY_PIXELSCALE,
                                             ScalableSurface.IDENTITY_PIXELSCALE };
    window.setSurfaceScale(reqSurfacePixelScale);
    pgl.pixel_scale = 1;




    NEWTMouseListener mouseListener = new NEWTMouseListener();
    window.addMouseListener(mouseListener);
    NEWTKeyListener keyListener = new NEWTKeyListener();
    window.addKeyListener(keyListener);
    NEWTWindowListener winListener = new NEWTWindowListener();
    window.addWindowListener(winListener);

    DrawListener drawlistener = new DrawListener();
    window.addGLEventListener(drawlistener);

    animator = new FPSAnimator(window, 60);


    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowDestroyNotify(final WindowEvent e) {
        animator.stop();
      }
    });

    frame = new DummyFrame();
    return frame;
  }

  class DummyFrame extends Frame {

    public DummyFrame() {
      super();
//      setVisible(false);
    }

    @Override
    public void setResizable(boolean resizable) {
      super.setResizable(resizable);

      // call NEWT function to make the window resizable
    }

    @Override
    public void setVisible(boolean visible) {
      // don't call super.setVisible()
      // make the NEWT window visible/invisible
      window.setVisible(visible);
    }

    @Override
    public void setTitle(String title) {
      window.setTitle(title);
    }
  }


  public void setTitle(String title) {
    window.setTitle(title);
  }

  public void setVisible(boolean visible) {
    window.setVisible(visible);
  }

  public void setResizable(boolean resizable) {
    // TODO Auto-generated method stub

  }

  public void placeWindow(int[] location) {
    // TODO Auto-generated method stub

  }

  public void placeWindow(int[] location, int[] editorLocation) {
    // TODO Auto-generated method stub

  }

  public void placePresent(Color stopColor) {
    // TODO Auto-generated method stub

  }

  public void setupExternalMessages() {
    // TODO Auto-generated method stub

  }

  public void startThread() {
    animator.start();
  }

  public void pauseThread() {
    animator.pause();
  }

  public void resumeThread() {
    animator.resume();
  }

  public boolean stopThread() {
    return animator.stop();
  }

  public boolean isStopped() {
    return !animator.isAnimating();
  }

  public void setSize(int width, int height) {
    sketchWidth = sketch.width = width;
    sketchHeight = sketch.height = height;
//    System.err.println("resize to " + width + ", " + height);
  }

  public void setFrameRate(float fps) {
    animator.setFPS((int)fps);
  }

  public void requestFocus() {
    window.requestFocus();

  }

  public void blit() {
    // TODO Auto-generated method stub

  }





  class DrawListener implements GLEventListener {
    public void display(GLAutoDrawable drawable) {
      pgl.getGL(drawable);
      pgl.getBuffers(window);
      sketch.handleDraw();
      if (sketch.frameCount == 1) {
        requestFocus();
      }
    }
    public void dispose(GLAutoDrawable drawable) {
      pgl.getGL(drawable);
      sketch.dispose();
//      if (sketch.exitCalled) {
//        sketch.exitActual();
//      }
    }
    public void init(GLAutoDrawable drawable) {
      pgl.getGL(drawable);
      sketch.start();
    }
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
      pgl.getGL(drawable);
      if (animator.isStarted()) {
        setSize(w, h);
      }
    }
  }

  protected class NEWTWindowListener implements com.jogamp.newt.event.WindowListener {
    public NEWTWindowListener() {
      super();
    }
    @Override
    public void windowGainedFocus(com.jogamp.newt.event.WindowEvent arg0) {
//      pg.parent.focusGained(null);
    }

    @Override
    public void windowLostFocus(com.jogamp.newt.event.WindowEvent arg0) {
//      pg.parent.focusLost(null);
    }

    @Override
    public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent arg0) {
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
    public void windowResized(com.jogamp.newt.event.WindowEvent arg0) { }
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
      nativeMouseEvent(e, MouseEvent.ENTER);
    }
    @Override
    public void mouseExited(com.jogamp.newt.event.MouseEvent e) {
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

    MouseEvent me = new MouseEvent(nativeEvent, nativeEvent.getWhen(),
                                   peAction, peModifiers,
                                   nativeEvent.getX(), nativeEvent.getY(),
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

    char keyChar;
    if (nativeEvent.getKeyChar() == 0) {
      keyChar = PConstants.CODED;
    } else {
      keyChar = nativeEvent.getKeyChar();
    }

    KeyEvent ke = new KeyEvent(nativeEvent, nativeEvent.getWhen(),
                               peAction, peModifiers,
                               keyChar,
                               nativeEvent.getKeyCode());

    sketch.postEvent(ke);
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  public void setCursor(int kind) {
    // TODO Auto-generated method stub

  }

  public void setCursor(PImage image, int hotspotX, int hotspotY) {
    // TODO Auto-generated method stub

  }

  public void showCursor() {
    window.setPointerVisible(true);
  }

  public void hideCursor() {
    window.setPointerVisible(false);
  }
}
