/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2014-15 The Processing Foundation

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

package processing.awt;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PSurfaceNone;
import processing.event.KeyEvent;
import processing.event.MouseEvent;


public class PSurfaceAWT extends PSurfaceNone {
  GraphicsDevice displayDevice;

  // used for canvas to determine whether resizable or not
//  boolean resizable;  // default is false

  // Internally, we know it's always a JFrame (not just a Frame)
//  JFrame frame;
  // Trying Frame again with a11 to see if this avoids some Swing nastiness.
  // In the past, AWT Frames caused some problems on Windows and Linux,
  // but those may not be a problem for our reworked PSurfaceAWT class.
  Frame frame;

  // Note that x and y may not be zero, depending on the display configuration
  Rectangle screenRect;

  // 3.0a5 didn't use strategy, and active was shut off during init() w/ retina
//  boolean useStrategy = true;

  Canvas canvas;
//  Component canvas;

//  PGraphics graphics;  // moved to PSurfaceNone

  int sketchWidth;
  int sketchHeight;


  public PSurfaceAWT(PGraphics graphics) {
    //this.graphics = graphics;
    super(graphics);

    /*
    if (checkRetina()) {
//      System.out.println("retina in use");

      // The active-mode rendering seems to be 2x slower, so disable it
      // with retina. On a non-retina machine, however, useActive seems
      // the only (or best) way to handle the rendering.
//      useActive = false;
//      canvas = new JPanel(true) {
//        @Override
//        public void paint(Graphics screen) {
////          if (!sketch.insideDraw) {
//          screen.drawImage(PSurfaceAWT.this.graphics.image, 0, 0, sketchWidth, sketchHeight, null);
////          }
//        }
//      };
      // Under 1.8 and the current 3.0a6 threading regime, active mode w/o
      // strategy is far faster, but perhaps only because it's blitting with
      // flicker--pushing pixels out before the screen has finished rendering.
//      useStrategy = false;
    }
    */
    canvas = new SmoothCanvas();
//    if (useStrategy) {
    //canvas.setIgnoreRepaint(true);
//    }

    // Pass tab key to the sketch, rather than moving between components
    canvas.setFocusTraversalKeysEnabled(false);

    canvas.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        if (!sketch.isLooping()) {
          // make sure this is a real resize event, not just initial setup
          // https://github.com/processing/processing/issues/3310
          Dimension canvasSize = canvas.getSize();
          if (canvasSize.width != sketch.sketchWidth() ||
              canvasSize.height != sketch.sketchHeight()) {
            sketch.redraw();
          }
        }
      }
    });
    addListeners();
  }


//  /**
//   * Handle grabbing the focus on startup. Other renderers can override this
//   * if handling needs to be different. For the AWT, the request is invoked
//   * later on the EDT. Other implementations may not require that, so the
//   * invokeLater() happens in here rather than requiring the caller to wrap it.
//   */
//  @Override
//  void requestFocus() {
////    System.out.println("requesFocus() outer " + EventQueue.isDispatchThread());
//    // for 2.0a6, moving this request to the EDT
//    EventQueue.invokeLater(new Runnable() {
//      public void run() {
//        // Call the request focus event once the image is sure to be on
//        // screen and the component is valid. The OpenGL renderer will
//        // request focus for its canvas inside beginDraw().
//        // http://java.sun.com/j2se/1.4.2/docs/api/java/awt/doc-files/FocusSpec.html
//        // Disabling for 0185, because it causes an assertion failure on OS X
//        // http://code.google.com/p/processing/issues/detail?id=258
//        //        requestFocus();
//
//        // Changing to this version for 0187
//        // http://code.google.com/p/processing/issues/detail?id=279
//        //requestFocusInWindow();
//
//        // For 3.0, just call this directly on the Canvas object
//        if (canvas != null) {
//          //System.out.println("requesting focus " + EventQueue.isDispatchThread());
//          //System.out.println("requesting focus " + frame.isVisible());
//          //canvas.requestFocusInWindow();
//          canvas.requestFocus();
//        }
//      }
//    });
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public class SmoothCanvas extends Canvas {
    private Dimension oldSize = new Dimension(0, 0);
    private Dimension newSize = new Dimension(0, 0);


    // Turns out getParent() returns a JPanel on a JFrame. Yech.
    public Frame getFrame() {
      return frame;
    }


    @Override
    public Dimension getPreferredSize() {
      return new Dimension(sketchWidth, sketchHeight);
    }


    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }


    @Override
    public Dimension getMaximumSize() {
      //return resizable ? super.getMaximumSize() : getPreferredSize();
      return frame.isResizable() ? super.getMaximumSize() : getPreferredSize();
    }


    @Override
    public void validate() {
      super.validate();
      newSize.width = getWidth();
      newSize.height = getHeight();
//      if (oldSize.equals(newSize)) {
////        System.out.println("validate() return " + oldSize);
//        return;
//      } else {
      if (!oldSize.equals(newSize)) {
//        System.out.println("validate() render old=" + oldSize + " -> new=" + newSize);
        oldSize = newSize;
        sketch.setSize(newSize.width, newSize.height);
//        try {
        render();
//        } catch (IllegalStateException ise) {
//          System.out.println(ise.getMessage());
//        }
      }
    }


    @Override
    public void update(Graphics g) {
//      System.out.println("updating");
      paint(g);
    }


    @Override
    public void paint(Graphics screen) {
//      System.out.println("painting");
//      if (useStrategy) {
      render();
      /*
      if (graphics != null) {
        System.out.println("drawing to screen " + canvas);
        screen.drawImage(graphics.image, 0, 0, sketchWidth, sketchHeight, null);
      }
      */

//      } else {
////        new Exception("painting").printStackTrace(System.out);
////        if (graphics.image != null) { // && !sketch.insideDraw) {
//        if (onscreen != null) {
////          synchronized (graphics.image) {
//          // Needs the width/height to be set so that retina images are properly scaled down
////          screen.drawImage(graphics.image, 0, 0, sketchWidth, sketchHeight, null);
//          synchronized (offscreenLock) {
//            screen.drawImage(onscreen, 0, 0, sketchWidth, sketchHeight, null);
//          }
//        }
//      }
    }
  }

    /*
    @Override
    public void addNotify() {
//      System.out.println("adding notify");
      super.addNotify();
      // prior to Java 7 on OS X, this no longer works [121222]
//    createBufferStrategy(2);
    }
    */


  synchronized protected void render() {
    if (canvas.isDisplayable() &&
        graphics.image != null) {
      if (canvas.getBufferStrategy() == null) {
        canvas.createBufferStrategy(2);
      }
      BufferStrategy strategy = canvas.getBufferStrategy();
      if (strategy != null) {
        // Render single frame
//        try {
        do {
          // The following loop ensures that the contents of the drawing buffer
          // are consistent in case the underlying surface was recreated
          do {
            Graphics2D draw = (Graphics2D) strategy.getDrawGraphics();
            // draw to width/height, since this may be a 2x image
            draw.drawImage(graphics.image, 0, 0, sketchWidth, sketchHeight, null);
            draw.dispose();
          } while (strategy.contentsRestored());

          // Display the buffer
          strategy.show();

          // Repeat the rendering if the drawing buffer was lost
        } while (strategy.contentsLost());
      }
    }
  }


  /*
  protected void blit() {
    // Other folks that call render() (i.e. paint()) are already on the EDT.
    // We need to be using the EDT since we're messing with the Canvas
    // object and BufferStrategy and friends.
    //EventQueue.invokeLater(new Runnable() {
    //public void run() {
    //((SmoothCanvas) canvas).render();
    //}
    //});

    if (useStrategy) {
      // Not necessary to be on the EDT to update BufferStrategy
      //((SmoothCanvas) canvas).render();
      render();
    } else {
      if (graphics.image != null) {
        BufferedImage graphicsImage = (BufferedImage) graphics.image;
        if (offscreen == null ||
          offscreen.getWidth() != graphicsImage.getWidth() ||
          offscreen.getHeight() != graphicsImage.getHeight()) {
          System.out.println("creating new image");
          offscreen = (BufferedImage)
            canvas.createImage(graphicsImage.getWidth(),
                               graphicsImage.getHeight());
//          off = offscreen.getGraphics();
        }
//        synchronized (offscreen) {
        Graphics2D off = (Graphics2D) offscreen.getGraphics();
//        off.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
        off.drawImage(graphicsImage, 0, 0, null);
//        }
        off.dispose();
        synchronized (offscreenLock) {
          BufferedImage temp = onscreen;
          onscreen = offscreen;
          offscreen = temp;
        }
        canvas.repaint();
      }
    }
  }
  */


  // what needs to happen here?
  @Override
  public void initOffscreen(PApplet sketch) {
    this.sketch = sketch;
  }

  /*
  public Frame initOffscreen() {
    Frame dummy = new Frame();
    dummy.pack();  // get legit AWT graphics
    // but don't show it
    return dummy;
  }
  */

  /*
  @Override
  public Component initComponent(PApplet sketch) {
    this.sketch = sketch;

    // needed for getPreferredSize() et al
    sketchWidth = sketch.sketchWidth();
    sketchHeight = sketch.sketchHeight();

    return canvas;
  }
  */


  @Override
  public void initFrame(final PApplet sketch) {/*, int backgroundColor,
                        int deviceIndex, boolean fullScreen, boolean spanDisplays) {*/
    this.sketch = sketch;

    GraphicsEnvironment environment =
      GraphicsEnvironment.getLocalGraphicsEnvironment();

    int displayNum = sketch.sketchDisplay();
//    System.out.println("display from sketch is " + displayNum);
    if (displayNum > 0) {  // if -1, use the default device
      GraphicsDevice[] devices = environment.getScreenDevices();
      if (displayNum <= devices.length) {
        displayDevice = devices[displayNum - 1];
      } else {
        System.err.format("Display %d does not exist, " +
          "using the default display instead.%n", displayNum);
        for (int i = 0; i < devices.length; i++) {
          System.err.format("Display %d is %s%n", (i+1), devices[i]);
        }
      }
    }
    if (displayDevice == null) {
      displayDevice = environment.getDefaultScreenDevice();
    }

    // Need to save the window bounds at full screen,
    // because pack() will cause the bounds to go to zero.
    // http://dev.processing.org/bugs/show_bug.cgi?id=923
    boolean spanDisplays = sketch.sketchDisplay() == PConstants.SPAN;
    screenRect = spanDisplays ? getDisplaySpan() :
      displayDevice.getDefaultConfiguration().getBounds();
    // DisplayMode doesn't work here, because we can't get the upper-left
    // corner of the display, which is important for multi-display setups.

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
      sketch.fullScreen();  // won't change the renderer
    }
    */

    if (fullScreen || spanDisplays) {
      sketchWidth = screenRect.width;
      sketchHeight = screenRect.height;
    }

    // Using a JFrame fixes a Windows problem with Present mode. This might
    // be our error, but usually this is the sort of crap we usually get from
    // OS X. It's time for a turnaround: Redmond is thinking different too!
    // https://github.com/processing/processing/issues/1955
    frame = new JFrame(displayDevice.getDefaultConfiguration());
//    frame = new Frame(displayDevice.getDefaultConfiguration());
//    // Default Processing gray, which will be replaced below if another
//    // color is specified on the command line (i.e. in the prefs).
//    ((JFrame) frame).getContentPane().setBackground(WINDOW_BGCOLOR);
//    // Cannot call setResizable(false) until later due to OS X (issue #467)

//    // Removed code above, also removed from what's now in the placeXxxx()
//    // methods. Not sure why it was being double-set; hopefully anachronistic.
//    if (backgroundColor == 0) {
//      backgroundColor = WINDOW_BGCOLOR;
//    }
    final Color windowColor = new Color(sketch.sketchWindowColor(), false);
    if (frame instanceof JFrame) {
      ((JFrame) frame).getContentPane().setBackground(windowColor);
    } else {
      frame.setBackground(windowColor);
    }

    // Put the p5 logo in the Frame's corner to override the Java coffee cup.
    setProcessingIcon(frame);

    // For 0149, moving this code (up to the pack() method) before init().
    // For OpenGL (and perhaps other renderers in the future), a peer is
    // needed before a GLDrawable can be created. So pack() needs to be
    // called on the Frame before applet.init(), which itself calls size(),
    // and launches the Thread that will kick off setup().
    // http://dev.processing.org/bugs/show_bug.cgi?id=891
    // http://dev.processing.org/bugs/show_bug.cgi?id=908

    frame.add(canvas);
    setSize(sketchWidth, sketchHeight);

    /*
    if (fullScreen) {
      // Called here because the graphics device is needed before we can
      // determine whether the sketch wants size(displayWidth, displayHeight),
      // and getting the graphics device will be PSurface-specific.
      PApplet.hideMenuBar();

      // Tried to use this to fix the 'present' mode issue.
      // Did not help, and the screenRect setup seems to work fine.
      //frame.setExtendedState(Frame.MAXIMIZED_BOTH);

      // https://github.com/processing/processing/pull/3162
      frame.dispose();  // release native resources, allows setUndecorated()
      frame.setUndecorated(true);
      // another duplicate?
//      if (backgroundColor != null) {
//        frame.getContentPane().setBackground(backgroundColor);
//      }
      // this may be the bounds of all screens
      frame.setBounds(screenRect);
      // will be set visible in placeWindow() [3.0a10]
      //frame.setVisible(true);  // re-add native resources
    }
    */
    frame.setLayout(null);
    //frame.add(applet);

    // Need to pass back our new sketchWidth/Height here, because it may have
    // been overridden by numbers we calculated above if fullScreen and/or
    // spanScreens was in use.
//    pg = sketch.makePrimaryGraphics(sketchWidth, sketchHeight);
//    pg = sketch.makePrimaryGraphics();

    // resize sketch to sketchWidth/sketchHeight here

    if (fullScreen) {
      frame.invalidate();
    } else {
//      frame.pack();
    }

    // insufficient, places the 100x100 sketches offset strangely
    //frame.validate();

    // disabling resize has to happen after pack() to avoid apparent Apple bug
    // http://code.google.com/p/processing/issues/detail?id=467
    frame.setResizable(false);

    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        sketch.exit();  // don't quit, need to just shut everything down (0133)
      }
    });

//    sketch.setFrame(frame);
  }


  @Override
  public Object getNative() {
    return canvas;
  }


//  public Toolkit getToolkit() {
//    return canvas.getToolkit();
//  }


  /** Set the window (and dock, or whatever necessary) title. */
  @Override
  public void setTitle(String title) {
    frame.setTitle(title);
    // Workaround for apparent Java bug on OS X?
    // https://github.com/processing/processing/issues/3472
    if (cursorVisible &&
        (PApplet.platform == PConstants.MACOSX) &&
        (cursorType != PConstants.ARROW)) {
      hideCursor();
      showCursor();
    }
  }


  /** Set true if we want to resize things (default is not resizable) */
  @Override
  public void setResizable(boolean resizable) {
    //this.resizable = resizable;  // really only used for canvas

    if (frame != null) {
      frame.setResizable(resizable);
    }
  }


  @Override
  public void setIcon(PImage image) {
    Image awtImage = (Image) image.getNative();

    if (PApplet.platform != PConstants.MACOSX) {
      frame.setIconImage(awtImage);

    } else {
      try {
        final String td = "processing.core.ThinkDifferent";
        Class<?> thinkDifferent =
          Thread.currentThread().getContextClassLoader().loadClass(td);
        Method method =
          thinkDifferent.getMethod("setIconImage", new Class[] { java.awt.Image.class });
        method.invoke(null, new Object[] { awtImage });
      } catch (Exception e) {
        e.printStackTrace();  // That's unfortunate
      }
    }
  }


  @Override
  public void setAlwaysOnTop(boolean always) {
    frame.setAlwaysOnTop(always);
  }


  List<Image> iconImages;

  protected void setProcessingIcon(Frame frame) {
    // On OS X, this only affects what shows up in the dock when minimized.
    // So replacing it is actually a step backwards. Brilliant.
    if (PApplet.platform != PConstants.MACOSX) {
      //Image image = Toolkit.getDefaultToolkit().createImage(ICON_IMAGE);
      //frame.setIconImage(image);
      try {
        if (iconImages == null) {
          iconImages = new ArrayList<Image>();
          final int[] sizes = { 16, 32, 48, 64, 128, 256, 512 };

          for (int sz : sizes) {
            //URL url = getClass().getResource("/icon/icon-" + sz + ".png");
            URL url = PApplet.class.getResource("/icon/icon-" + sz + ".png");
            Image image = Toolkit.getDefaultToolkit().getImage(url);
            iconImages.add(image);
            //iconImages.add(Toolkit.getLibImage("icons/pde-" + sz + ".png", frame));
          }
        }
        frame.setIconImages(iconImages);

      } catch (Exception e) { }  // harmless; keep this to ourselves

    } else {  // handle OS X differently
      if (!dockIconSpecified()) {  // don't override existing -Xdock param
        // On OS X, set this for AWT surfaces, which handles the dock image
        // as well as the cmd-tab image that's shown. Just one size, I guess.
        URL url = PApplet.class.getResource("/icon/icon-512.png");
        // Seems dangerous to have this in code instead of using reflection, no?
        //ThinkDifferent.setIconImage(Toolkit.getDefaultToolkit().getImage(url));
        try {
          final String td = "processing.core.ThinkDifferent";
          Class<?> thinkDifferent =
            Thread.currentThread().getContextClassLoader().loadClass(td);
          Method method =
            thinkDifferent.getMethod("setIconImage", new Class[] { java.awt.Image.class });
          method.invoke(null, new Object[] { Toolkit.getDefaultToolkit().getImage(url) });
        } catch (Exception e) {
          e.printStackTrace();  // That's unfortunate
        }
      }
    }
  }


  /**
   * @return true if -Xdock:icon was specified on the command line
   */
  private boolean dockIconSpecified() {
    // TODO This is incomplete... Haven't yet found a way to figure out if
    //      the app has an icns file specified already. Help?
    List<String> jvmArgs =
      ManagementFactory.getRuntimeMXBean().getInputArguments();
    for (String arg : jvmArgs) {
      if (arg.startsWith("-Xdock:icon")) {
        return true;  // dock image already set
      }
    }
    return false;
  }


  @Override
  public void setVisible(boolean visible) {
    frame.setVisible(visible);

    // Generally useful whenever setting the frame visible
    if (canvas != null) {
      //canvas.requestFocusInWindow();
      canvas.requestFocus();
    }

    // removing per https://github.com/processing/processing/pull/3162
    // can remove the code below once 3.0a6 is tested and behaving
/*
    if (visible && PApplet.platform == PConstants.LINUX) {
      // Linux doesn't deal with insets the same way. We get fake insets
      // earlier, and then the window manager will slap its own insets
      // onto things once the frame is realized on the screen. Awzm.
      if (PApplet.platform == PConstants.LINUX) {
        Insets insets = frame.getInsets();
        frame.setSize(Math.max(sketchWidth, MIN_WINDOW_WIDTH) +
                      insets.left + insets.right,
                      Math.max(sketchHeight, MIN_WINDOW_HEIGHT) +
                      insets.top + insets.bottom);
      }
    }
*/
  }


  //public void placeFullScreen(boolean hideStop) {
  @Override
  public void placePresent(int stopColor) {
    setFullFrame();

    // After the pack(), the screen bounds are gonna be 0s
//    frame.setBounds(screenRect);  // already called in setFullFrame()
    canvas.setBounds((screenRect.width - sketchWidth) / 2,
                     (screenRect.height - sketchHeight) / 2,
                     sketchWidth, sketchHeight);

//    if (PApplet.platform == PConstants.MACOSX) {
//      macosxFullScreenEnable(frame);
//      macosxFullScreenToggle(frame);
//    }

    if (stopColor != 0) {
      Label label = new Label("stop");
      label.setForeground(new Color(stopColor, false));
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

//    if (sketch.getGraphics().displayable()) {
//      setVisible(true);
//    }
  }


  /*
  @Override
  public void placeWindow(int[] location) {
    setFrameSize(); //sketchWidth, sketchHeight);

    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      frame.setLocation(location[0], location[1]);

    } else {  // just center on screen
      // Can't use frame.setLocationRelativeTo(null) because it sends the
      // frame to the main display, which undermines the --display setting.
      frame.setLocation(screenRect.x + (screenRect.width - sketchWidth) / 2,
                        screenRect.y + (screenRect.height - sketchHeight) / 2);
    }
    Point frameLoc = frame.getLocation();
    if (frameLoc.y < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      frame.setLocation(frameLoc.x, 30);
    }

//    if (backgroundColor != null) {
//      ((JFrame) frame).getContentPane().setBackground(backgroundColor);
//    }

    setCanvasSize(); //sketchWidth, sketchHeight);

    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    // handle frame resizing events
    setupFrameResizeListener();

    // all set for rockin
    if (sketch.getGraphics().displayable()) {
      frame.setVisible(true);
    }
  }
  */


  private void setCanvasSize() {
//    System.out.format("setting canvas size %d %d%n", sketchWidth, sketchHeight);
//    new Exception().printStackTrace(System.out);
    int contentW = Math.max(sketchWidth, MIN_WINDOW_WIDTH);
    int contentH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT);

    canvas.setBounds((contentW - sketchWidth)/2,
                     (contentH - sketchHeight)/2,
                     sketchWidth, sketchHeight);
  }


  /** Resize frame for these sketch (canvas) dimensions. */
  private Dimension setFrameSize() {  //int sketchWidth, int sketchHeight) {
    // https://github.com/processing/processing/pull/3162
    frame.addNotify();  // using instead of show() to add the peer [fry]

//    System.out.format("setting frame size %d %d %n", sketchWidth, sketchHeight);
//    new Exception().printStackTrace(System.out);
    Insets insets = frame.getInsets();
    int windowW = Math.max(sketchWidth, MIN_WINDOW_WIDTH) +
      insets.left + insets.right;
    int windowH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT) +
      insets.top + insets.bottom;
    frame.setSize(windowW, windowH);
    return new Dimension(windowW, windowH);
  }


  private void setFrameCentered() {
    // Can't use frame.setLocationRelativeTo(null) because it sends the
    // frame to the main display, which undermines the --display setting.
    frame.setLocation(screenRect.x + (screenRect.width - sketchWidth) / 2,
                      screenRect.y + (screenRect.height - sketchHeight) / 2);
  }


  /** Hide the menu bar, make the Frame undecorated, set it to screenRect. */
  private void setFullFrame() {
    // Called here because the graphics device is needed before we can
    // determine whether the sketch wants size(displayWidth, displayHeight),
    // and getting the graphics device will be PSurface-specific.
    PApplet.hideMenuBar();

    // Tried to use this to fix the 'present' mode issue.
    // Did not help, and the screenRect setup seems to work fine.
    //frame.setExtendedState(Frame.MAXIMIZED_BOTH);

    // https://github.com/processing/processing/pull/3162
    //frame.dispose();  // release native resources, allows setUndecorated()
    frame.removeNotify();
    frame.setUndecorated(true);
    frame.addNotify();

    // this may be the bounds of all screens
    frame.setBounds(screenRect);
    // will be set visible in placeWindow() [3.0a10]
    //frame.setVisible(true);  // re-add native resources
  }


  @Override
  public void placeWindow(int[] location, int[] editorLocation) {
    //Dimension window = setFrameSize(sketchWidth, sketchHeight);
    Dimension window = setFrameSize(); //sketchWidth, sketchHeight);

    int contentW = Math.max(sketchWidth, MIN_WINDOW_WIDTH);
    int contentH = Math.max(sketchHeight, MIN_WINDOW_HEIGHT);

    if (sketch.sketchFullScreen()) {
      setFullFrame();
    }

    // Ignore placement of previous window and editor when full screen
    if (!sketch.sketchFullScreen()) {
      if (location != null) {
        // a specific location was received from the Runner
        // (applet has been run more than once, user placed window)
        frame.setLocation(location[0], location[1]);

      } else if (editorLocation != null) {
        int locationX = editorLocation[0] - 20;
        int locationY = editorLocation[1];

        if (locationX - window.width > 10) {
          // if it fits to the left of the window
          frame.setLocation(locationX - window.width, locationY);

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
          frame.setLocation(locationX, locationY);
        }
      } else {  // just center on screen
        setFrameCentered();
      }
      Point frameLoc = frame.getLocation();
      if (frameLoc.y < 0) {
        // Windows actually allows you to place frames where they can't be
        // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
        frame.setLocation(frameLoc.x, 30);
      }
    }

    canvas.setBounds((contentW - sketchWidth)/2,
                     (contentH - sketchHeight)/2,
                     sketchWidth, sketchHeight);

    // handle frame resizing events
    setupFrameResizeListener();

    /*
    // If displayable() is false, then PSurfaceNone should be used, but...
    if (sketch.getGraphics().displayable()) {
      frame.setVisible(true);
//      System.out.println("setting visible on EDT? " + EventQueue.isDispatchThread());
      //requestFocus();
//      if (canvas != null) {
//        //canvas.requestFocusInWindow();
//        canvas.requestFocus();
//      }
    }
    */
//    if (sketch.getGraphics().displayable()) {
//      setVisible(true);
//    }
  }


  // needs to resize the frame, which will resize the canvas, and so on...
  @Override
  public void setSize(int wide, int high) {
//    if (PApplet.DEBUG) {
//      //System.out.format("frame visible %b, setSize(%d, %d) %n", frame.isVisible(), wide, high);
//      new Exception(String.format("setSize(%d, %d)", wide, high)).printStackTrace(System.out);
//    }

    //if (wide == sketchWidth && high == sketchHeight) {  // doesn't work on launch
    if (wide == sketch.width && high == sketch.height) {
//      if (PApplet.DEBUG) {
//        new Exception("w/h unchanged " + wide + " " + high).printStackTrace(System.out);
//      }
      return;  // unchanged, don't rebuild everything
    }

    sketchWidth = wide;
    sketchHeight = high;

//    canvas.setSize(wide, high);
//    frame.setSize(wide, high);
    if (frame != null) {  // skip if just a canvas
      setFrameSize(); //wide, high);
    }
    setCanvasSize();
//    if (frame != null) {
//      frame.setLocationRelativeTo(null);
//    }

    //initImage(graphics, wide, high);

    //throw new RuntimeException("implement me, see readme.md");
    sketch.setSize(wide, high);
//    sketch.width = wide;
//    sketch.height = high;

    // set PGraphics variables for width/height/pixelWidth/pixelHeight
    graphics.setSize(wide, high);
//    System.out.println("out of setSize()");
  }


  //public void initImage(PGraphics gr, int wide, int high) {
  /*
  @Override
  public void initImage(PGraphics graphics) {
    GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
    // If not realized (off-screen, i.e the Color Selector Tool), gc will be null.
    if (gc == null) {
      System.err.println("GraphicsConfiguration null in initImage()");
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
    }

    // Formerly this was broken into separate versions based on offscreen or
    // not, but we may as well create a compatible image; it won't hurt, right?
    int wide = graphics.width * graphics.pixelFactor;
    int high = graphics.height * graphics.pixelFactor;
    graphics.image = gc.createCompatibleImage(wide, high);
  }
  */


//  @Override
//  public Component getComponent() {
//    return canvas;
//  }


//  @Override
//  public void setSmooth(int level) {
//  }


  /*
  private boolean checkRetina() {
    if (PApplet.platform == PConstants.MACOSX) {
      // This should probably be reset each time there's a display change.
      // A 5-minute search didn't turn up any such event in the Java 7 API.
      // Also, should we use the Toolkit associated with the editor window?
      final String javaVendor = System.getProperty("java.vendor");
      if (javaVendor.contains("Oracle")) {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();

        try {
          Field field = device.getClass().getDeclaredField("scale");
          if (field != null) {
            field.setAccessible(true);
            Object scale = field.get(device);

            if (scale instanceof Integer && ((Integer)scale).intValue() == 2) {
              return true;
            }
          }
        } catch (Exception ignore) { }
      }
    }
    return false;
  }
  */


  /** Get the bounds rectangle for all displays. */
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


  /*
  private void checkDisplaySize() {
    if (canvas.getGraphicsConfiguration() != null) {
      GraphicsDevice displayDevice = getGraphicsConfiguration().getDevice();

      if (displayDevice != null) {
        Rectangle screenRect =
          displayDevice.getDefaultConfiguration().getBounds();

        displayWidth = screenRect.width;
        displayHeight = screenRect.height;
      }
    }
  }
  */


  /**
   * Set this sketch to communicate its state back to the PDE.
   * <p/>
   * This uses the stderr stream to write positions of the window
   * (so that it will be saved by the PDE for the next run) and
   * notify on quit. See more notes in the Worker class.
   */
  @Override
  public void setupExternalMessages() {
    frame.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentMoved(ComponentEvent e) {
        Point where = ((Frame) e.getSource()).getLocation();
        sketch.frameMoved(where.x, where.y);
      }
    });
  }


  /**
   * Set up a listener that will fire proper component resize events
   * in cases where frame.setResizable(true) is called.
   */
  private void setupFrameResizeListener() {
    frame.addWindowStateListener(new WindowStateListener() {
      @Override
      // Detecting when the frame is resized in order to handle the frame
      // maximization bug in OSX:
      // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8036935
      public void windowStateChanged(WindowEvent e) {
        // This seems to be firing when dragging the window on OS X
        // https://github.com/processing/processing/issues/3092
        if (Frame.MAXIMIZED_BOTH == e.getNewState()) {
          // Supposedly, sending the frame to back and then front is a
          // workaround for this bug:
          // http://stackoverflow.com/a/23897602
          // but is not working for me...
          //frame.toBack();
          //frame.toFront();
          // Packing the frame works, but that causes the window to collapse
          // on OS X when the window is dragged. Changing to addNotify() for
          // https://github.com/processing/processing/issues/3092
          //frame.pack();
          frame.addNotify();
        }
      }
    });

    frame.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        // Ignore bad resize events fired during setup to fix
        // http://dev.processing.org/bugs/show_bug.cgi?id=341
        // This should also fix the blank screen on Linux bug
        // http://dev.processing.org/bugs/show_bug.cgi?id=282
        if (frame.isResizable()) {
          // might be multiple resize calls before visible (i.e. first
          // when pack() is called, then when it's resized for use).
          // ignore them because it's not the user resizing things.
          Frame farm = (Frame) e.getComponent();
          if (farm.isVisible()) {
            Insets insets = farm.getInsets();
            Dimension windowSize = farm.getSize();

            // JFrame (unlike java.awt.Frame) doesn't include the left/top
            // insets for placement (though it does seem to need them for
            // overall size of the window. Perhaps JFrame sets its coord
            // system so that (0, 0) is always the upper-left of the content
            // area. Which seems nice, but breaks any f*ing AWT-based code.
            setSize(windowSize.width - insets.left - insets.right,
                    windowSize.height - insets.top - insets.bottom);
          }
        }
      }
    });
  }


//  /**
//   * (No longer in use) Use reflection to call
//   * <code>com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(window, true);</code>
//   */
//  static void macosxFullScreenEnable(Window window) {
//    try {
//      Class<?> util = Class.forName("com.apple.eawt.FullScreenUtilities");
//      Class params[] = new Class[] { Window.class, Boolean.TYPE };
//      Method method = util.getMethod("setWindowCanFullScreen", params);
//      method.invoke(util, window, true);
//
//    } catch (ClassNotFoundException cnfe) {
//      // ignored
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//
//  /**
//   * (No longer in use) Use reflection to call
//   * <code>com.apple.eawt.Application.getApplication().requestToggleFullScreen(window);</code>
//   */
//  static void macosxFullScreenToggle(Window window) {
//    try {
//      Class<?> appClass = Class.forName("com.apple.eawt.Application");
//
//      Method getAppMethod = appClass.getMethod("getApplication");
//      Object app = getAppMethod.invoke(null, new Object[0]);
//
//      Method requestMethod =
//        appClass.getMethod("requestToggleFullScreen", Window.class);
//      requestMethod.invoke(app, window);
//
//    } catch (ClassNotFoundException cnfe) {
//      // ignored
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }


  //////////////////////////////////////////////////////////////


  /*
  // disabling for now; requires Java 1.7 and "precise" semantics are odd...
  // returns 0.1 for tick-by-tick scrolling on OS X, but it's not a matter of
  // calling ceil() on the value: 1.5 goes to 1, but 2.3 goes to 2.
  // "precise" is a whole different animal, so add later API to shore that up.
  static protected Method preciseWheelMethod;
  static {
    try {
      preciseWheelMethod = MouseWheelEvent.class.getMethod("getPreciseWheelRotation", new Class[] { });
    } catch (Exception e) {
      // ignored, the method will just be set to null
    }
  }
  */


  /**
   * Figure out how to process a mouse event. When loop() has been
   * called, the events will be queued up until drawing is complete.
   * If noLoop() has been called, then events will happen immediately.
   */
  protected void nativeMouseEvent(java.awt.event.MouseEvent nativeEvent) {
    // the 'amount' is the number of button clicks for a click event,
    // or the number of steps/clicks on the wheel for a mouse wheel event.
    int peCount = nativeEvent.getClickCount();

    int peAction = 0;
    switch (nativeEvent.getID()) {
    case java.awt.event.MouseEvent.MOUSE_PRESSED:
      peAction = MouseEvent.PRESS;
      break;
    case java.awt.event.MouseEvent.MOUSE_RELEASED:
      peAction = MouseEvent.RELEASE;
      break;
    case java.awt.event.MouseEvent.MOUSE_CLICKED:
      peAction = MouseEvent.CLICK;
      break;
    case java.awt.event.MouseEvent.MOUSE_DRAGGED:
      peAction = MouseEvent.DRAG;
      break;
    case java.awt.event.MouseEvent.MOUSE_MOVED:
      peAction = MouseEvent.MOVE;
      break;
    case java.awt.event.MouseEvent.MOUSE_ENTERED:
      peAction = MouseEvent.ENTER;
      break;
    case java.awt.event.MouseEvent.MOUSE_EXITED:
      peAction = MouseEvent.EXIT;
      break;
    //case java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL:
    case java.awt.event.MouseEvent.MOUSE_WHEEL:
      peAction = MouseEvent.WHEEL;
      /*
      if (preciseWheelMethod != null) {
        try {
          peAmount = ((Double) preciseWheelMethod.invoke(nativeEvent, (Object[]) null)).floatValue();
        } catch (Exception e) {
          preciseWheelMethod = null;
        }
      }
      */
      peCount = ((MouseWheelEvent) nativeEvent).getWheelRotation();
      break;
    }

    //System.out.println(nativeEvent);
    //int modifiers = nativeEvent.getModifiersEx();
    // If using getModifiersEx(), the regular modifiers don't set properly.
    int modifiers = nativeEvent.getModifiers();

    int peModifiers = modifiers &
      (InputEvent.SHIFT_MASK |
       InputEvent.CTRL_MASK |
       InputEvent.META_MASK |
       InputEvent.ALT_MASK);

    // Windows and OS X seem to disagree on how to handle this. Windows only
    // sets BUTTON1_DOWN_MASK, while OS X seems to set BUTTON1_MASK.
    // This is an issue in particular with mouse release events:
    // http://code.google.com/p/processing/issues/detail?id=1294
    // The fix for which led to a regression (fixed here by checking both):
    // http://code.google.com/p/processing/issues/detail?id=1332
    int peButton = 0;
//    if ((modifiers & InputEvent.BUTTON1_MASK) != 0 ||
//        (modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) {
//      peButton = LEFT;
//    } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0 ||
//               (modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) {
//      peButton = CENTER;
//    } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0 ||
//               (modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0) {
//      peButton = RIGHT;
//    }
    if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
      peButton = PConstants.LEFT;
    } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
      peButton = PConstants.CENTER;
    } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
      peButton = PConstants.RIGHT;
    }

    // If running on Mac OS, allow ctrl-click as right mouse. Prior to 0215,
    // this used isPopupTrigger() on the native event, but that doesn't work
    // for mouseClicked and mouseReleased (or others).
    if (PApplet.platform == PConstants.MACOSX) {
      //if (nativeEvent.isPopupTrigger()) {
      if ((modifiers & InputEvent.CTRL_MASK) != 0) {
        peButton = PConstants.RIGHT;
      }
    }

    sketch.postEvent(new MouseEvent(nativeEvent, nativeEvent.getWhen(),
                                    peAction, peModifiers,
                                    nativeEvent.getX(), nativeEvent.getY(),
                                    peButton,
                                    peCount));
  }


  protected void nativeKeyEvent(java.awt.event.KeyEvent event) {
    int peAction = 0;
    switch (event.getID()) {
    case java.awt.event.KeyEvent.KEY_PRESSED:
      peAction = KeyEvent.PRESS;
      break;
    case java.awt.event.KeyEvent.KEY_RELEASED:
      peAction = KeyEvent.RELEASE;
      break;
    case java.awt.event.KeyEvent.KEY_TYPED:
      peAction = KeyEvent.TYPE;
      break;
    }

//    int peModifiers = event.getModifiersEx() &
//      (InputEvent.SHIFT_DOWN_MASK |
//       InputEvent.CTRL_DOWN_MASK |
//       InputEvent.META_DOWN_MASK |
//       InputEvent.ALT_DOWN_MASK);
    int peModifiers = event.getModifiers() &
      (InputEvent.SHIFT_MASK |
       InputEvent.CTRL_MASK |
       InputEvent.META_MASK |
       InputEvent.ALT_MASK);

    sketch.postEvent(new KeyEvent(event, event.getWhen(),
                                  peAction, peModifiers,
                                  event.getKeyChar(), event.getKeyCode()));
  }


  // listeners, for all my men!
  protected void addListeners() {

    canvas.addMouseListener(new MouseListener() {

      public void mousePressed(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseReleased(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseClicked(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseEntered(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseExited(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }
    });

    canvas.addMouseMotionListener(new MouseMotionListener() {

      public void mouseDragged(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }

      public void mouseMoved(java.awt.event.MouseEvent e) {
        nativeMouseEvent(e);
      }
    });

    canvas.addMouseWheelListener(new MouseWheelListener() {

      public void mouseWheelMoved(MouseWheelEvent e) {
        nativeMouseEvent(e);
      }
    });

    canvas.addKeyListener(new KeyListener() {

      public void keyPressed(java.awt.event.KeyEvent e) {
        nativeKeyEvent(e);
      }


      public void keyReleased(java.awt.event.KeyEvent e) {
        nativeKeyEvent(e);
      }


      public void keyTyped(java.awt.event.KeyEvent e) {
        nativeKeyEvent(e);
      }
    });

    canvas.addFocusListener(new FocusListener() {

      public void focusGained(FocusEvent e) {
        sketch.focused = true;
        sketch.focusGained();
      }

      public void focusLost(FocusEvent e) {
        sketch.focused = false;
        sketch.focusLost();
      }
    });
  }


  /*
  public void addListeners(Component comp) {
    comp.addMouseListener(this);
    comp.addMouseWheelListener(this);
    comp.addMouseMotionListener(this);
    comp.addKeyListener(this);
    comp.addFocusListener(this);
  }


  public void removeListeners(Component comp) {
    comp.removeMouseListener(this);
    comp.removeMouseWheelListener(this);
    comp.removeMouseMotionListener(this);
    comp.removeKeyListener(this);
    comp.removeFocusListener(this);
  }
  */


//  /**
//   * Call to remove, then add, listeners to a component.
//   * Avoids issues with double-adding.
//   */
//  public void updateListeners(Component comp) {
//    removeListeners(comp);
//    addListeners(comp);
//  }



  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  int cursorType = PConstants.ARROW;
  boolean cursorVisible = true;
  Cursor invisibleCursor;


  @Override
  public void setCursor(int kind) {
    // Swap the HAND cursor because MOVE doesn't seem to be available on OS X
    // https://github.com/processing/processing/issues/2358
    if (PApplet.platform == PConstants.MACOSX && kind == PConstants.MOVE) {
      kind = PConstants.HAND;
    }
    canvas.setCursor(Cursor.getPredefinedCursor(kind));
    cursorVisible = true;
    this.cursorType = kind;
  }


  @Override
  public void setCursor(PImage img, int x, int y) {
    // Don't set cursorType, instead use cursorType to save the last
    // regular cursor type used for when cursor() is called.
    //cursor_type = Cursor.CUSTOM_CURSOR;
    Cursor cursor =
      canvas.getToolkit().createCustomCursor((Image) img.getNative(),
                                             new Point(x, y),
                                             "custom");
    canvas.setCursor(cursor);
    cursorVisible = true;
  }


  @Override
  public void showCursor() {
    // Maybe should always set here? Seems dangerous, since it's likely that
    // Java will set the cursor to something else on its own, and the sketch
    // will be stuck b/c p5 thinks the cursor is set to one particular thing.
    if (!cursorVisible) {
      cursorVisible = true;
      canvas.setCursor(Cursor.getPredefinedCursor(cursorType));
    }
  }


  @Override
  public void hideCursor() {
    // Because the OS may have shown the cursor on its own,
    // don't return if 'cursorVisible' is set to true. [rev 0216]

    if (invisibleCursor == null) {
      BufferedImage cursorImg =
        new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
      invisibleCursor =
        canvas.getToolkit().createCustomCursor(cursorImg, new Point(8, 8), "blank");
    }
    canvas.setCursor(invisibleCursor);
    cursorVisible = false;
  }


  @Override
  public Thread createThread() {
    return new AnimationThread() {
      @Override
      public void callDraw() {
        sketch.handleDraw();
        render();
      }
    };
  }


  void debug(String format, Object ... args) {
    System.out.format(format + "%n", args);
  }
}
