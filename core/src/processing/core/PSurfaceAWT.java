package processing.core;

import java.applet.*;
import java.awt.*;
import java.awt.event.WindowStateListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JFrame;

import processing.event.KeyEvent;
import processing.event.MouseEvent;


public class PSurfaceAWT implements PSurface {
  // disabled on retina inside init()
  boolean useActive = true;
//  boolean useActive = false;
//  boolean useStrategy = true;
  boolean useStrategy = false;
  Canvas canvas;

  /**
   * Java AWT Image object associated with this renderer. For the 1.0 version
   * of P2D and P3D, this was be associated with their MemoryImageSource.
   * For PGraphicsJava2D, it will be the offscreen drawing buffer.
   */
  public Image image;

  PApplet sketch;

  Thread thread;

  Object pauseObject = new Object();

  /** As of release 0116, frameRate(60) is called as a default */
  protected float frameRateTarget = 60;
  protected long frameRatePeriod = 1000000000L / 60L;



  public PSurfaceAWT() {
    if (checkRetina()) {
      // The active-mode rendering seems to be 2x slower, so disable it
      // with retina. On a non-retina machine, however, useActive seems
      // the only (or best) way to handle the rendering.
      useActive = false;
    }

    thread = new AnimationThread();
    thread.start();
  }


  void initCanvas() {
    // send tab keys through to the PApplet
    canvas.setFocusTraversalKeysEnabled(false);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
//        Component c = e.getComponent();
//        //System.out.println("componentResized() " + c);
//        Rectangle bounds = c.getBounds();
//        resizeRequest = true;
//        resizeWidth = bounds.width;
//        resizeHeight = bounds.height;

        if (!looping) {
          redraw();
        }
      }
    });
  }


  /**
   * Handle grabbing the focus from the parent applet. Other renderers can
   * override this if handling needs to be different.
   */
  public void requestFocus() {
    // for 2.0a6, moving this request to the EDT
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        // Call the request focus event once the image is sure to be on
        // screen and the component is valid. The OpenGL renderer will
        // request focus for its canvas inside beginDraw().
        // http://java.sun.com/j2se/1.4.2/docs/api/java/awt/doc-files/FocusSpec.html
        // Disabling for 0185, because it causes an assertion failure on OS X
        // http://code.google.com/p/processing/issues/detail?id=258
        //        requestFocus();

        // Changing to this version for 0187
        // http://code.google.com/p/processing/issues/detail?id=279
        //requestFocusInWindow();

        // For 2.0, pass this to the renderer, to lend a hand to OpenGL
        if (parent != null) {
          parent.requestFocusInWindow();
        }
      }
    });
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  @Override
  public void update(Graphics screen) {
    paint(screen);
  }


  @Override
  public void paint(Graphics screen) {
//    int r = (int) random(10000);
//    System.out.println("into paint " + r);
    //super.paint(screen);

    // ignore the very first call to paint, since it's coming
    // from the o.s., and the applet will soon update itself anyway.
    if (frameCount == 0) {
//      println("Skipping frame");
      // paint() may be called more than once before things
      // are finally painted to the screen and the thread gets going
      return;
    }
    // without ignoring the first call, the first several frames
    // are confused because paint() gets called in the midst of
    // the initial nextFrame() call, so there are multiple
    // updates fighting with one another.

    // make sure the screen is visible and usable
    // (also prevents over-drawing when using PGraphicsOpenGL)

    /* the 1.5.x version
    if (g != null) {
      // added synchronization for 0194 because of flicker issues with JAVA2D
      // http://code.google.com/p/processing/issues/detail?id=558
      // g.image is synchronized so that draw/loop and paint don't
      // try to fight over it. this was causing a randomized slowdown
      // that would cut the frameRate into a third on macosx,
      // and is probably related to the windows sluggishness bug too
      if (g.image != null) {
        System.out.println("ui paint");
        synchronized (g.image) {
          screen.drawImage(g.image, 0, 0, null);
        }
      }
    }
*/

//    if (useActive) {
//      return;
//    }

//    if (insideDraw) {
//      new Exception().printStackTrace(System.out);
//    }
    if (!insideDraw && (g != null) && (g.image != null)) {
      if (useStrategy) {
        render();
      } else {
//        System.out.println("drawing to screen");
        //screen.drawImage(g.image, 0, 0, null);  // not retina friendly
        screen.drawImage(g.image, 0, 0, width, height, null);
      }
    } else {
      debug(insideDraw + " " + g + " " + ((g != null) ? g.image : "-"));
    }
  }


  protected synchronized void render() {
    if (canvas == null) {
      removeListeners(this);
      canvas = new Canvas();
      add(canvas);
      setIgnoreRepaint(true);
      canvas.setIgnoreRepaint(true);
      addListeners(canvas);
//      add(canvas, BorderLayout.CENTER);
//      doLayout();
    }
    canvas.setBounds(0, 0, width, height);
//    System.out.println("render(), canvas bounds are " + canvas.getBounds());
    if (canvas.getBufferStrategy() == null) {  // whole block [121222]
//      System.out.println("creating a strategy");
      canvas.createBufferStrategy(2);
    }
    BufferStrategy strategy = canvas.getBufferStrategy();
    if (strategy == null) {
      return;
    }
    // Render single frame
    do {
      // The following loop ensures that the contents of the drawing buffer
      // are consistent in case the underlying surface was recreated
      do {
        Graphics draw = strategy.getDrawGraphics();
        draw.drawImage(g.image, 0, 0, width, height, null);
        draw.dispose();

        // Repeat the rendering if the drawing buffer contents
        // were restored
//        System.out.println("restored " + strategy.contentsRestored());
      } while (strategy.contentsRestored());

      // Display the buffer
//      System.out.println("showing");
      strategy.show();

      // Repeat the rendering if the drawing buffer was lost
//      System.out.println("lost " + strategy.contentsLost());
//      System.out.println();
    } while (strategy.contentsLost());
  }


  /*
  // active paint method  (also the 1.2.1 version)
  protected void paint() {
    try {
      Graphics screen = this.getGraphics();
      if (screen != null) {
        if ((g != null) && (g.image != null)) {
          screen.drawImage(g.image, 0, 0, null);
        }
        Toolkit.getDefaultToolkit().sync();
      }
    } catch (Exception e) {
      // Seen on applet destroy, maybe can ignore?
      e.printStackTrace();

//    } finally {
//      if (g != null) {
//        g.dispose();
//      }
    }
  }


  protected void paint_1_5_1() {
    try {
      Graphics screen = getGraphics();
      if (screen != null) {
        if (g != null) {
          // added synchronization for 0194 because of flicker issues with JAVA2D
          // http://code.google.com/p/processing/issues/detail?id=558
          if (g.image != null) {
            System.out.println("active paint");
            synchronized (g.image) {
              screen.drawImage(g.image, 0, 0, null);
            }
            Toolkit.getDefaultToolkit().sync();
          }
        }
      }
    } catch (Exception e) {
      // Seen on applet destroy, maybe can ignore?
      e.printStackTrace();
    }
  }
  */


  public void blit() {
    if (useActive) {
      if (useStrategy) {
        render();
      } else {
        Graphics screen = getGraphics();
        if (screen != null) {
          screen.drawImage(g.image, 0, 0, width, height, null);
        }
      }
    } else {
      repaint();
    }
//  getToolkit().sync();  // force repaint now (proper method)
  }


  public void initFrame(int width, int height, Color backgroundColor,
                        int deviceIndex, boolean fullScreen, boolean spanDisplays) {
    GraphicsEnvironment environment =
      GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice displayDevice = null;

    GraphicsDevice devices[] = environment.getScreenDevices();
    if ((deviceIndex >= 0) && (deviceIndex < devices.length)) {
      displayDevice = devices[deviceIndex];
    } else {
      System.err.println("Display " + deviceIndex + " does not exist, " +
                         "using the default display instead.");
      for (int i = 0; i < devices.length; i++) {
        System.err.println(i + " is " + devices[i]);
      }
    }

    if (displayDevice == null) {
      displayDevice = environment.getDefaultScreenDevice();
    }

    // Need to save the window bounds at full screen,
    // because pack() will cause the bounds to go to zero.
    // http://dev.processing.org/bugs/show_bug.cgi?id=923
    Rectangle screenRect =
      displayDevice.getDefaultConfiguration().getBounds();
    // DisplayMode doesn't work here, because we can't get the upper-left
    // corner of the display, which is important for multi-display setups.

    // Sketch has already requested to be the same as the screen's
    // width and height, so let's roll with full screen mode.
    if (screenRect.width == width && screenRect.height == height) {
      fullScreen = true;
    }

    // Using a JFrame fixes a Windows problem with Present mode. This might
    // be our error, but usually this is the sort of crap we usually get from
    // OS X. It's time for a turnaround: Redmond is thinking different too!
    // https://github.com/processing/processing/issues/1955
    Frame frame = new JFrame(displayDevice.getDefaultConfiguration());
    // Default Processing gray, which will be replaced below if another
    // color is specified on the command line (i.e. in the prefs).
    ((JFrame) frame).getContentPane().setBackground(WINDOW_BGCOLOR);
    // Cannot call setResizable(false) until later due to OS X (issue #467)

    // Set the trimmings around the image
    setIconImage(frame);

    // For 0149, moving this code (up to the pack() method) before init().
    // For OpenGL (and perhaps other renderers in the future), a peer is
    // needed before a GLDrawable can be created. So pack() needs to be
    // called on the Frame before applet.init(), which itself calls size(),
    // and launches the Thread that will kick off setup().
    // http://dev.processing.org/bugs/show_bug.cgi?id=891
    // http://dev.processing.org/bugs/show_bug.cgi?id=908
    if (fullScreen) {
//      if (platform == MACOSX) {
//        // Call some native code to remove the menu bar on OS X. Not necessary
//        // on Linux and Windows, who are happy to make full screen windows.
//        japplemenubar.JAppleMenuBar.hide();
//      }

      // Tried to use this to fix the 'present' mode issue.
      // Did not help, and the screenRect setup seems to work fine.
      //frame.setExtendedState(Frame.MAXIMIZED_BOTH);

      frame.setUndecorated(true);
      if (backgroundColor != null) {
        ((JFrame) frame).getContentPane().setBackground(backgroundColor);
      }
//      if (exclusive) {
//        displayDevice.setFullScreenWindow(frame);
//        // this trashes the location of the window on os x
//        //frame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
//        fullScreenRect = frame.getBounds();
//      } else {
      frame.setBounds(screenRect);
      frame.setVisible(true);
//      }
    }
    frame.setLayout(null);
    frame.add(applet);

    if (fullScreen) {
      frame.invalidate();
    } else {
      frame.pack();
    }

    // insufficient, places the 100x100 sketches offset strangely
    //frame.validate();

    // disabling resize has to happen after pack() to avoid apparent Apple bug
    // http://code.google.com/p/processing/issues/detail?id=467
    frame.setResizable(false);
  }


  public void placeSketch() {
//  // If 'present' wasn't already set, but the applet initializes
//  // to full screen, attempt to make things full screen anyway.
//  if (!present &&
//      applet.width == screenRect.width &&
//      applet.height == screenRect.height) {
//    // bounds will be set below, but can't change to setUndecorated() now
//    present = true;
//  }
//  // Opting not to do this, because we can't remove the decorations on the
//  // window at this point. And re-opening a new winodw is a lot of mess.
//  // Better all around to just encourage the use of sketchFullScreen()
//  // or cmd/ctrl-shift-R in the PDE.

  if (present) {
//    if (platform == MACOSX) {
//      println("before");
//      println(screenRect);
//      println(frame.getBounds());
//
//      // Call some native code to remove the menu bar on OS X. Not necessary
//      // on Linux and Windows, who are happy to make full screen windows.
////      japplemenubar.JAppleMenuBar.hide();
//      toggleFullScreen(frame);
//      println("after");
//      println(screenRect);
//      println(frame.getBounds());
//
//      println(applet.width + " " + applet.height);
//    }

    // After the pack(), the screen bounds are gonna be 0s
    frame.setBounds(screenRect);
    applet.setBounds((screenRect.width - applet.width) / 2,
                     (screenRect.height - applet.height) / 2,
                     applet.width, applet.height);

    if (platform == MACOSX) {
      macosxFullScreenEnable(frame);
      macosxFullScreenToggle(frame);

//      toggleFullScreen(frame);
//      println("after");
//      println(screenRect);
//      println(frame.getBounds());
//      println(applet.width + " " + applet.height);
    }

    if (!hideStop) {
      Label label = new Label("stop");
      label.setForeground(stopColor);
      label.addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(java.awt.event.MouseEvent e) {
            applet.exit();
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

    // not always running externally when in present mode
    if (external) {
      applet.setupExternalMessages();
    }

  } else {  // if not presenting
    // can't do pack earlier cuz present mode don't like it
    // (can't go full screen with a frame after calling pack)
    //        frame.pack();

    // get insets. get more.
    Insets insets = frame.getInsets();
    int windowW = Math.max(applet.width, MIN_WINDOW_WIDTH) +
      insets.left + insets.right;
    int windowH = Math.max(applet.height, MIN_WINDOW_HEIGHT) +
      insets.top + insets.bottom;

    int contentW = Math.max(applet.width, MIN_WINDOW_WIDTH);
    int contentH = Math.max(applet.height, MIN_WINDOW_HEIGHT);

    frame.setSize(windowW, windowH);

    if (location != null) {
      // a specific location was received from the Runner
      // (applet has been run more than once, user placed window)
      frame.setLocation(location[0], location[1]);

    } else if (external && editorLocation != null) {
      int locationX = editorLocation[0] - 20;
      int locationY = editorLocation[1];

      if (locationX - windowW > 10) {
        // if it fits to the left of the window
        frame.setLocation(locationX - windowW, locationY);

      } else {  // doesn't fit
        // if it fits inside the editor window,
        // offset slightly from upper lefthand corner
        // so that it's plunked inside the text area
        locationX = editorLocation[0] + 66;
        locationY = editorLocation[1] + 66;

        if ((locationX + windowW > applet.displayWidth - 33) ||
            (locationY + windowH > applet.displayHeight - 33)) {
          // otherwise center on screen
          locationX = (applet.displayWidth - windowW) / 2;
          locationY = (applet.displayHeight - windowH) / 2;
        }
        frame.setLocation(locationX, locationY);
      }
    } else {  // just center on screen
      // Can't use frame.setLocationRelativeTo(null) because it sends the
      // frame to the main display, which undermines the --display setting.
      frame.setLocation(screenRect.x + (screenRect.width - applet.width) / 2,
                        screenRect.y + (screenRect.height - applet.height) / 2);
    }
    Point frameLoc = frame.getLocation();
    if (frameLoc.y < 0) {
      // Windows actually allows you to place frames where they can't be
      // closed. Awesome. http://dev.processing.org/bugs/show_bug.cgi?id=1508
      frame.setLocation(frameLoc.x, 30);
    }

    if (backgroundColor != null) {
//    if (backgroundColor == Color.black) {  //BLACK) {
//      // this means no bg color unless specified
//      backgroundColor = SystemColor.control;
//    }
      ((JFrame) frame).getContentPane().setBackground(backgroundColor);
    }

//    int usableWindowH = windowH - insets.top - insets.bottom;
//    applet.setBounds((windowW - applet.width)/2,
//                     insets.top + (usableWindowH - applet.height)/2,
//                     applet.width, applet.height);
    applet.setBounds((contentW - applet.width)/2,
                     (contentH - applet.height)/2,
                     applet.width, applet.height);

    if (external) {
      applet.setupExternalMessages();

    } else {  // !external
      frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(java.awt.event.WindowEvent e) {
            System.exit(0);
          }
        });
    }

    // handle frame resizing events
    applet.setupFrameResizeListener();

    // all set for rockin
    if (applet.displayable()) {
      frame.setVisible(true);

      // Linux doesn't deal with insets the same way. We get fake insets
      // earlier, and then the window manager will slap its own insets
      // onto things once the frame is realized on the screen. Awzm.
      if (platform == LINUX) {
        Insets irlInsets = frame.getInsets();
        if (!irlInsets.equals(insets)) {
          insets = irlInsets;
          windowW = Math.max(applet.width, MIN_WINDOW_WIDTH) +
            insets.left + insets.right;
          windowH = Math.max(applet.height, MIN_WINDOW_HEIGHT) +
            insets.top + insets.bottom;
          frame.setSize(windowW, windowH);
        }
      }
    }
  }
  }


  private boolean checkRetina() {
    if (PApplet.platform == PConstants.MACOSX) {
    // This should probably be reset each time there's a display change.
    // A 5-minute search didn't turn up any such event in the Java API.
    // Also, should we use the Toolkit associated with the editor window?
      final String javaVendor = System.getProperty("java.vendor");
      if (javaVendor.contains("Apple")) {
        Float prop = (Float)
          canvas.getToolkit().getDesktopProperty("apple.awt.contentScaleFactor");
        if (prop != null) {
          return prop == 2;
        }
      } else if (javaVendor.contains("Oracle")) {
        String version = System.getProperty("java.version");  // 1.7.0_40
        String[] m = PApplet.match(version, "1.(\\d).*_(\\d+)");

        // Make sure this is Oracle Java 7u40 or later
        if (m != null &&
          PApplet.parseInt(m[1]) >= 7 &&
          PApplet.parseInt(m[1]) >= 40) {
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
    }
    return false;
  }


  // get the bounds for all displays
  public Rectangle getDisplaySpan() {
    Rectangle bounds = new Rectangle();
    GraphicsEnvironment localGE = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (GraphicsDevice gd : localGE.getScreenDevices()) {
      for (GraphicsConfiguration graphicsConfiguration : gd.getConfigurations()) {
        Rectangle2D.union(bounds, graphicsConfiguration.getBounds(), bounds);
      }
    }
    return bounds;
  }


  private void checkDisplaySize() {
    if (getGraphicsConfiguration() != null) {
      GraphicsDevice displayDevice = getGraphicsConfiguration().getDevice();

      if (displayDevice != null) {
        Rectangle screenRect =
          displayDevice.getDefaultConfiguration().getBounds();

        displayWidth = screenRect.width;
        displayHeight = screenRect.height;
      }
    }
  }



  /**
   * Set this sketch to communicate its state back to the PDE.
   * <p/>
   * This uses the stderr stream to write positions of the window
   * (so that it will be saved by the PDE for the next run) and
   * notify on quit. See more notes in the Worker class.
   */
  public void setupExternalMessages() {

    frame.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
          Point where = ((Frame) e.getSource()).getLocation();
          System.err.println(PApplet.EXTERNAL_MOVE + " " +
                             where.x + " " + where.y);
          System.err.flush();  // doesn't seem to help or hurt
        }
      });

    frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
//          System.err.println(PApplet.EXTERNAL_QUIT);
//          System.err.flush();  // important
//          System.exit(0);
          exit();  // don't quit, need to just shut everything down (0133)
        }
      });
  }


  /**
   * Set up a listener that will fire proper component resize events
   * in cases where frame.setResizable(true) is called.
   */
  public void setupFrameResizeListener() {
    frame.addWindowStateListener(new WindowStateListener() {
      @Override
      // Detecting when the frame is resized in order to handle the frame
      // maximization bug in OSX:
      // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8036935
      public void windowStateChanged(WindowEvent e) {
        if (Frame.MAXIMIZED_BOTH == e.getNewState()) {
          // Supposedly, sending the frame to back and then front is a
          // workaround for this bug:
          // http://stackoverflow.com/a/23897602
          // but is not working for me...
          //frame.toBack();
          //frame.toFront();
          // but either packing the frame does!
          frame.pack();
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
              Rectangle newBounds =
                new Rectangle(0, 0, //insets.left, insets.top,
                              windowSize.width - insets.left - insets.right,
                              windowSize.height - insets.top - insets.bottom);
              Rectangle oldBounds = getBounds();
              if (!newBounds.equals(oldBounds)) {
                // the ComponentListener in PApplet will handle calling size()
                setBounds(newBounds);

                // In 0225, calling this via reflection so that we can still
                // compile in Java 1.6. This is a trap since we really need
                // to move to 1.7 and cannot support 1.6, but things like text
                // are still a little wonky on 1.7, especially on OS X.
                // This gives us a way to at least test against older VMs.
                revalidate();   // let the layout manager do its work
                /*
                if (revalidateMethod != null) {
                  try {
                    revalidateMethod.invoke(PApplet.this);
                  } catch (Exception ex) {
                    ex.printStackTrace();
                    revalidateMethod = null;
                  }
                }
                */
              }
            }
          }
        }
      });
  }


  static ArrayList<Image> iconImages;

  protected void setIconImage(Frame frame) {
    // On OS X, this only affects what shows up in the dock when minimized.
    // So replacing it is actually a step backwards. Brilliant.
    if (PApplet.platform != PConstants.MACOSX) {
      //Image image = Toolkit.getDefaultToolkit().createImage(ICON_IMAGE);
      //frame.setIconImage(image);
      try {
        if (iconImages == null) {
          iconImages = new ArrayList<Image>();
          final int[] sizes = { 16, 32, 48, 64 };

          for (int sz : sizes) {
            URL url = getClass().getResource("/icon/icon-" + sz + ".png");
            Image image = Toolkit.getDefaultToolkit().getImage(url);
            iconImages.add(image);
            //iconImages.add(Toolkit.getLibImage("icons/pde-" + sz + ".png", frame));
          }
        }
        frame.setIconImages(iconImages);
      } catch (Exception e) {
        //e.printStackTrace();  // more or less harmless; don't spew errors
      }
    }
  }


  /**
   * Use reflection to call
   * <code>com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(window, true);</code>
   */
  static private void macosxFullScreenEnable(Window window) {
    try {
      Class<?> util = Class.forName("com.apple.eawt.FullScreenUtilities");
      Class params[] = new Class[] { Window.class, Boolean.TYPE };
      Method method = util.getMethod("setWindowCanFullScreen", params);
      method.invoke(util, window, true);

    } catch (ClassNotFoundException cnfe) {
      // ignored
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Use reflection to call
   * <code>com.apple.eawt.Application.getApplication().requestToggleFullScreen(window);</code>
   */
  static private void macosxFullScreenToggle(Window window) {
    try {
      Class<?> appClass = Class.forName("com.apple.eawt.Application");

      Method getAppMethod = appClass.getMethod("getApplication");
      Object app = getAppMethod.invoke(null, new Object[0]);

      Method requestMethod =
        appClass.getMethod("requestToggleFullScreen", Window.class);
      requestMethod.invoke(app, window);

    } catch (ClassNotFoundException cnfe) {
      // ignored
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


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


  public void addListeners() {
    // lots of listeners, for all my men!

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

  public void setFrameRate(float fps) {
    frameRateTarget = fps;
    frameRatePeriod = (long) (1000000000.0 / frameRateTarget);
    //g.setFrameRate(fps);
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
      long beforeTime = System.nanoTime();
      long overSleepTime = 0L;

      int noDelays = 0;
      // Number of frames with a delay of 0 ms before the
      // animation thread yields to other running threads.
      final int NO_DELAYS_PER_YIELD = 15;

      // un-pause the sketch and get rolling
      start();

      while ((Thread.currentThread() == this) && !sketch.finished) {
        if (sketch.paused) {
          PApplet.debug("PApplet.run() paused, calling object wait...");
          synchronized (pauseObject) {
            try {
              pauseObject.wait();
              PApplet.debug("out of wait");
            } catch (InterruptedException e) {
              // waiting for this interrupt on a start() (resume) call
            }
          }
        }
        PApplet.debug("done with pause");

        // Don't resize the renderer from the EDT (i.e. from a ComponentEvent),
        // otherwise it may attempt a resize mid-render.
        if (g != null) {
          Dimension currentSize = getSize();
          if (currentSize.width != g.width || currentSize.height != g.height) {
            resizeRenderer(currentSize.width, currentSize.height);
          }
        }

        // render a single frame
        if (g != null) sketch.handleDraw();

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
      }

      sketch.dispose();  // call to shutdown libs?

      // If the user called the exit() function, the window should close,
      // rather than the sketch just halting.
      if (exitCalled) {
        exitActual();
      }
    }
  }
}