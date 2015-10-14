/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.opengl;

import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.IOUtil.ClassResources;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PSurface;
import processing.event.KeyEvent;
import processing.event.MouseEvent;


public class PSurfaceJOGL implements PSurface {
  /** Selected GL profile */
  public static GLProfile profile;

  public PJOGL pgl;

  protected GLWindow window;
  protected FPSAnimator animator;
  protected Rectangle screenRect;

  protected PApplet sketch;
  protected PGraphics graphics;

  protected int sketchX;
  protected int sketchY;
  protected int sketchWidth0;
  protected int sketchHeight0;
  protected int sketchWidth;
  protected int sketchHeight;

  protected Display display;
  protected Screen screen;
  protected List<MonitorDevice> monitors;
  protected MonitorDevice displayDevice;
  protected Throwable drawException;
  protected Object waitObject = new Object();

  protected NewtCanvasAWT canvas;

  protected float[] currentPixelScale = {0, 0};

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


  public void initFrame(PApplet sketch) {
    this.sketch = sketch;
    initIcons();
    initDisplay();
    initGL();
    initWindow();
    initListeners();
    initAnimator();
  }


  public Object getNative() {
    return window;
  }


  protected void initDisplay() {
    Display tmpDisplay = NewtFactory.createDisplay(null);
    tmpDisplay.addReference();
    Screen tmpScreen = NewtFactory.createScreen(tmpDisplay, 0);
    tmpScreen.addReference();

    monitors = new ArrayList<MonitorDevice>();
    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] awtDevices = environment.getScreenDevices();
    List<MonitorDevice> newtDevices = tmpScreen.getMonitorDevices();

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
    } else if (PApplet.platform == PConstants.WINDOWS) {
      // NEWT display id is == (adapterId << 8 | monitorId),
      // should be in the same order as AWT
      monitors.addAll(newtDevices);
    } else { // MAC OSX and others
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

    displayDevice = null;
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
    } else if (0 < monitors.size()) {
      displayDevice = monitors.get(0);
    }

    if (displayDevice != null) {
      screen = displayDevice.getScreen();
      display = screen.getDisplay();
    } else {
      screen = tmpScreen;
      display = tmpDisplay;
      displayDevice = screen.getPrimaryMonitor();
    }
  }


  protected void initGL() {
//  System.out.println("*******************************");
    if (profile == null) {
      if (PJOGL.profile == 1) {
        try {
          profile = GLProfile.getGL2ES1();
        } catch (GLException ex) {
          profile = GLProfile.getMaxFixedFunc(true);
        }
      } else if (PJOGL.profile == 2) {
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

//    pgl.reqNumSamples = PGL.smoothToSamples(graphics.smooth);
    caps.setSampleBuffers(true);
    caps.setNumSamples(PGL.smoothToSamples(graphics.smooth));
    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);
    pgl.setCaps(caps);
  }


  protected void initWindow() {
    window = GLWindow.create(screen, pgl.getCaps());

//    if (displayDevice == null) {
//
//
//    } else {
//      window = GLWindow.create(displayDevice.getScreen(), pgl.getCaps());
//    }


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

    sketchWidth0 = sketch.sketchWidth();
    sketchHeight0 = sketch.sketchHeight();

    /*
    // Trying to fix
    // https://github.com/processing/processing/issues/3401
    if (sketch.displayWidth < sketch.width ||
      sketch.displayHeight < sketch.height) {
      int w = sketch.width;
      int h = sketch.height;
      if (sketch.displayWidth < w) {
        w = sketch.displayWidth;
      }
      if (sketch.displayHeight < h) {
        h = sketch.displayHeight;
      }
//      sketch.setSize(w, h - 22 - 22);
//      graphics.setSize(w, h - 22 - 22);
      System.err.println("setting width/height to " + w + " "  + h);
    }
    */

    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();
//    System.out.println("init: " + sketchWidth + " " + sketchHeight);

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
    window.setResizable(false);
    setSize(sketchWidth, sketchHeight);
    sketchX = displayDevice.getViewportInWindowUnits().getX();
    sketchY = displayDevice.getViewportInWindowUnits().getY();
    if (fullScreen) {
      PApplet.hideMenuBar();
      window.setTopLevelPosition(sketchX, sketchY);
      if (spanDisplays) {
        window.setFullscreen(monitors);
      } else {
        List<MonitorDevice> display = Collections.singletonList(displayDevice);
        window.setFullscreen(display);
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
    animator = new FPSAnimator(window, 60, true);
    drawException = null;
    animator.setUncaughtExceptionHandler(new GLAnimatorControl.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final GLAnimatorControl animator,
                                    final GLAutoDrawable drawable,
                                    final Throwable cause) {
        synchronized (waitObject) {
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
            } else if (cause instanceof UnsatisfiedLinkError) {
              throw new UnsatisfiedLinkError(cause.getMessage());
            } else {
              throw new RuntimeException(cause);
            }
          }
        }
      }
    }).start();
  }


  @Override
  public void setTitle(final String title) {
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.setTitle(title);
      }
    });
  }


  @Override
  public void setVisible(final boolean visible) {
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.setVisible(visible);
      }
    });
  }


  @Override
  public void setResizable(final boolean resizable) {
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.setResizable(resizable);
      }
    });
  }


  public void setIcon(PImage icon) {
    // TODO Auto-generated method stub
  }


  @Override
  public void setAlwaysOnTop(final boolean always) {
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.setAlwaysOnTop(always);
      }
    });
  }


  protected void initIcons() {
    final int[] sizes = { 16, 32, 48, 64, 128, 256, 512 };
    String[] iconImages = new String[sizes.length];
    for (int i = 0; i < sizes.length; i++) {
      iconImages[i] = "/icon/icon-" + sizes[i] + ".png";
    }
    IOUtil.ClassResources res = new ClassResources(iconImages,
                                                   PApplet.class.getClassLoader(),
                                                   PApplet.class);
    NewtFactory.setWindowIcons(res);
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
        // center on screen, below the editor's window bar
        locationX = (sketch.displayWidth - w) / 2;
        locationY = editorLocation[1] + 66;
        window.setTopLevelPosition(locationX, locationY);
      }
    } else {  // just center on screen
      // Can't use frame.setLocationRelativeTo(null) because it sends the
      // frame to the main display, which undermines the --display setting.
      int sketchX = displayDevice.getViewportInWindowUnits().getX();
      int sketchY = displayDevice.getViewportInWindowUnits().getY();
      window.setTopLevelPosition(sketchX + screenRect.x + (screenRect.width - sketchWidth) / 2,
                                 sketchY + screenRect.y + (screenRect.height - sketchHeight) / 2);
    }

    Point frameLoc = new Point(x, y);
    if (frameLoc.y < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      window.setTopLevelPosition(frameLoc.x, 30);
    }
  }


  public void placePresent(int stopColor) {
    pgl.initPresentMode(0.5f * (screenRect.width - sketchWidth),
                        0.5f * (screenRect.height - sketchHeight));
    window.setSize(screenRect.width, screenRect.height);
    PApplet.hideMenuBar();
    window.setTopLevelPosition(sketchX + screenRect.x,
                               sketchY + screenRect.y);
    window.setFullscreen(true);
  }


  public void setupExternalMessages() {
    // TODO Auto-generated method stub

  }


  public void startThread() {
    if (animator != null) {
      animator.start();
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


  public void setLocation(final int x, final int y) {
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.setTopLevelPosition(x, y);
      }
    });
  }


  public void setSize(final int width, final int height) {
    if (width == sketch.width && height == sketch.height) {
      return;
    }

    if (!pgl.presentMode()) {
      sketch.setSize(width, height);
      sketchWidth = width;
      sketchHeight = height;
      graphics.setSize(width, height);
      window.setSize(width, height);
    }
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
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.requestFocus();
      }
    });
  }


  class DrawListener implements GLEventListener {
    public void display(GLAutoDrawable drawable) {
      if (display.getEDTUtil().isCurrentThreadEDT()) {
        // For some reason, the first two frames of the animator are run on the
        // EDT, skipping rendering Processing's frame in that case.
        return;
      }

      if (sketch.frameCount == 0) {
        if (sketchWidth < sketchWidth0 || sketchHeight < sketchHeight0) {
          PGraphics.showWarning("The sketch has been automatically resized to fit the screen resolution");
        }
//        System.out.println("display: " + window.getWidth() + " "+ window.getHeight() + " - " + sketchWidth + " " + sketchHeight);
        requestFocus();
      }

      pgl.getGL(drawable);
      int pframeCount = sketch.frameCount;
      sketch.handleDraw();
      if (pframeCount == sketch.frameCount) {
        // This hack allows the FBO layer to be swapped normally even if
        // the sketch is no looping, otherwise background artifacts will occur.
        pgl.beginRender();
        pgl.endRender(sketch.sketchWindowColor());
      }

      PGraphicsOpenGL.completeFinishedPixelTransfers();

      if (sketch.exitCalled()) {
        PGraphicsOpenGL.completeAllPixelTransfers();

        sketch.dispose(); // calls stopThread(), which stops the animator.
        sketch.exitActual();
      }
    }
    public void dispose(GLAutoDrawable drawable) {
      sketch.dispose();
    }
    public void init(GLAutoDrawable drawable) {
      pgl.getGL(drawable);
      pgl.init(drawable);
      sketch.start();

      int c = graphics.backgroundColor;
      pgl.clearColor(((c >> 16) & 0xff) / 255f,
                     ((c >>  8) & 0xff) / 255f,
                     ((c >>  0) & 0xff) / 255f,
                     ((c >> 24) & 0xff) / 255f);
      pgl.clear(PGL.COLOR_BUFFER_BIT);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
//      int c = graphics.backgroundColor;
//      pgl.clearColor(((c >> 16) & 0xff) / 255f,
//                     ((c >>  8) & 0xff) / 255f,
//                     ((c >>  0) & 0xff) / 255f,
//                     ((c >> 24) & 0xff) / 255f);
//      pgl.clear(PGL.COLOR_BUFFER_BIT);
      pgl.resetFBOLayer();
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
      sketch.focused = true;
      sketch.focusGained();
    }

    @Override
    public void windowLostFocus(com.jogamp.newt.event.WindowEvent arg0) {
      sketch.focused = false;
      sketch.focusLost();
    }

    @Override
    public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent arg0) {
      sketch.dispose();
      sketch.exitActual();
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
      // Invert wheel rotation count so it matches JAVA2D's
      // https://github.com/processing/processing/issues/3840
      peCount = -(nativeEvent.isShiftDown() ? (int)nativeEvent.getRotation()[0]:
                                              (int)nativeEvent.getRotation()[1]);
    } else {
      peCount = nativeEvent.getClickCount();
    }

    window.getCurrentSurfaceScale(currentPixelScale);
    int sx = (int)(nativeEvent.getX()/currentPixelScale[0]);
    int sy = (int)(nativeEvent.getY()/currentPixelScale[1]);
    int mx = sx;
    int my = sy;

    if (pgl.presentMode()) {
      mx -= (int)pgl.presentX;
      my -= (int)pgl.presentY;
      if (peAction == KeyEvent.RELEASE &&
          pgl.insideCloseButton(sx, sy - screenRect.height)) {
        sketch.exit();
      }
      if (mx < 0 || sketchWidth < mx || my < 0 || sketchHeight < my) {
        return;
      }
    }

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
      // we can return only one char for ENTER, let it be \n everywhere
      keyCode = code == com.jogamp.newt.event.KeyEvent.VK_ENTER ?
          PConstants.ENTER : code;
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
                               keyCode,
                               nativeEvent.isAutoRepeat());

    sketch.postEvent(ke);

    if (!isPCodedKey(code) && !isHackyKey(code)) {
      if (peAction == KeyEvent.PRESS) {
        // Create key typed event
        // TODO: combine dead keys with the following key
        KeyEvent tke = new KeyEvent(nativeEvent, nativeEvent.getWhen(),
                                    KeyEvent.TYPE, peModifiers,
                                    keyChar,
                                    0,
                                    nativeEvent.isAutoRepeat());

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
    switch (code) {
      case com.jogamp.newt.event.KeyEvent.VK_UP:
        return PConstants.UP;
      case com.jogamp.newt.event.KeyEvent.VK_DOWN:
        return PConstants.DOWN;
      case com.jogamp.newt.event.KeyEvent.VK_LEFT:
        return PConstants.LEFT;
      case com.jogamp.newt.event.KeyEvent.VK_RIGHT:
        return PConstants.RIGHT;
      case com.jogamp.newt.event.KeyEvent.VK_ALT:
        return PConstants.ALT;
      case com.jogamp.newt.event.KeyEvent.VK_CONTROL:
        return PConstants.CONTROL;
      case com.jogamp.newt.event.KeyEvent.VK_SHIFT:
        return PConstants.SHIFT;
      case com.jogamp.newt.event.KeyEvent.VK_WINDOWS:
        return java.awt.event.KeyEvent.VK_META;
      default:
        return code;
    }
  }


  private static boolean isHackyKey(short code) {
    switch (code) {
      case com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE:
      case com.jogamp.newt.event.KeyEvent.VK_TAB:
      case com.jogamp.newt.event.KeyEvent.VK_ENTER:
      case com.jogamp.newt.event.KeyEvent.VK_ESCAPE:
      case com.jogamp.newt.event.KeyEvent.VK_DELETE:
        return true;
    }
    return false;
  }


  private static char hackToChar(short code, char def) {
    switch (code) {
      case com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE:
        return PConstants.BACKSPACE;
      case com.jogamp.newt.event.KeyEvent.VK_TAB:
        return PConstants.TAB;
      case com.jogamp.newt.event.KeyEvent.VK_ENTER:
        return PConstants.ENTER;
      case com.jogamp.newt.event.KeyEvent.VK_ESCAPE:
        return PConstants.ESC;
      case com.jogamp.newt.event.KeyEvent.VK_DELETE:
        return PConstants.DELETE;
    }
    return def;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class CursorInfo {
    PImage image;
    int x, y;

    CursorInfo(PImage image, int x, int y) {
      this.image = image;
      this.x = x;
      this.y = y;
    }

    void set() {
      setCursor(image, x, y);
    }
  }

  static Map<Integer, CursorInfo> cursors = new HashMap<>();
  static Map<Integer, String> cursorNames = new HashMap<Integer, String>();
  static {
    cursorNames.put(PConstants.ARROW, "arrow");
    cursorNames.put(PConstants.CROSS, "cross");
    cursorNames.put(PConstants.WAIT, "wait");
    cursorNames.put(PConstants.MOVE, "move");
    cursorNames.put(PConstants.HAND, "hand");
    cursorNames.put(PConstants.TEXT, "text");
  }


  public void setCursor(int kind) {
    if (!cursorNames.containsKey(kind)) {
      PGraphics.showWarning("Unknown cursor type: " + kind);
      return;
    }
    CursorInfo cursor = cursors.get(kind);
    if (cursor == null) {
      String name = cursorNames.get(kind);
      if (name != null) {
        ImageIcon icon =
          new ImageIcon(getClass().getResource("cursors/" + name + ".png"));
        PImage img = new PImage(icon.getImage());
        // Most cursors just use the center as the hotspot...
        int x = img.width / 2;
        int y = img.height / 2;
        // ...others are more specific
        if (kind == PConstants.ARROW) {
          x = 10; y = 7;
        } else if (kind == PConstants.HAND) {
          x = 12; y = 8;
        } else if (kind == PConstants.TEXT) {
          x = 16; y = 22;
        }
        cursor = new CursorInfo(img, x, y);
        cursors.put(kind, cursor);
      }
    }
    if (cursor != null) {
      cursor.set();
    } else {
      PGraphics.showWarning("Cannot load cursor type: " + kind);
    }
  }


  public void setCursor(PImage image, int hotspotX, int hotspotY) {
    Display disp = window.getScreen().getDisplay();
    BufferedImage bimg = (BufferedImage)image.getNative();
    DataBufferInt dbuf = (DataBufferInt)bimg.getData().getDataBuffer();
    int[] ipix = dbuf.getData();
    ByteBuffer pixels = ByteBuffer.allocate(ipix.length * 4);
    pixels.asIntBuffer().put(ipix);
    PixelFormat format = PixelFormat.ARGB8888;
    final Dimension size = new Dimension(bimg.getWidth(), bimg.getHeight());
    PixelRectangle pixelrect = new PixelRectangle.GenericPixelRect(format, size, 0, false, pixels);
    final PointerIcon pi = disp.createPointerIcon(pixelrect, hotspotX, hotspotY);
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.setPointerIcon(pi);
      }
    });
  }


  public void showCursor() {
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.setPointerVisible(true);
      }
    });
  }


  public void hideCursor() {
    display.getEDTUtil().invoke(false, new Runnable() {
      @Override
      public void run() {
        window.setPointerVisible(false);
      }
    });
  }
}
