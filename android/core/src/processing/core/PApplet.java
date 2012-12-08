/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

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

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.util.*;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;

import processing.data.*;
import processing.event.*;
import processing.opengl.*;


public class PApplet extends Activity implements PConstants, Runnable {
  /** The PGraphics renderer associated with this PApplet */
  public PGraphics g;

//  static final public boolean DEBUG = true;
  static final public boolean DEBUG = false;

  /** The frame containing this applet (if any) */
//  public Frame frame;

  /**
   * The screen size when the sketch was started. This is initialized inside
   * onCreate().
   * <p>
   * Note that this won't update if you change the resolution
   * of your screen once the the applet is running.
   * <p>
   * This variable is not static because in the desktop version of Processing,
   * not all instances of PApplet will necessarily be started on a screen of
   * the same size.
   */
  public int displayWidth, displayHeight;

  /**
   * Command line options passed in from main().
   * <P>
   * This does not include the arguments passed in to PApplet itself.
   */
//  public String[] args;

  /**
   * Path to where sketch can read/write files (read-only).
   * Android: This is the writable area for the Activity, which is correct
   * for purposes of how sketchPath is used in practice from a sketch,
   * even though it's technically different than the desktop version.
   */
  public String sketchPath; //folder;

  /** When debugging headaches */
//  static final boolean THREAD_DEBUG = false;

  /** Default width and height for applet when not specified */
//  static public final int DEFAULT_WIDTH = 100;
//  static public final int DEFAULT_HEIGHT = 100;

  /**
   * Minimum dimensions for the window holding an applet.
   * This varies between platforms, Mac OS X 10.3 can do any height
   * but requires at least 128 pixels width. Windows XP has another
   * set of limitations. And for all I know, Linux probably lets you
   * make windows with negative sizes.
   */
//  static public final int MIN_WINDOW_WIDTH = 128;
//  static public final int MIN_WINDOW_HEIGHT = 128;

  /**
   * Exception thrown when size() is called the first time.
   * <P>
   * This is used internally so that setup() is forced to run twice
   * when the renderer is changed. This is the only way for us to handle
   * invoking the new renderer while also in the midst of rendering.
   */
  static public class RendererChangeException extends RuntimeException { }

  protected boolean surfaceReady;

  /**
   * Set true when the surface dimensions have changed, so that the PGraphics
   * object can be resized on the next trip through handleDraw().
   */
  protected boolean surfaceChanged;

  /**
   * true if no size() command has been executed. This is used to wait until
   * a size has been set before placing in the window and showing it.
   */
//  public boolean defaultSize;

//  volatile boolean resizeRequest;
//  volatile int resizeWidth;
//  volatile int resizeHeight;

  /**
   * Pixel buffer from this applet's PGraphics.
   * <P>
   * When used with OpenGL or Java2D, this value will
   * be null until loadPixels() has been called.
   */
  public int[] pixels;

  /** width of this applet's associated PGraphics */
  public int width;

  /** height of this applet's associated PGraphics */
  public int height;

  // can't call this because causes an ex, but could set elsewhere
  //final float screenDensity = getResources().getDisplayMetrics().density;

  /** absolute x position of input on screen */
  public int mouseX;

  /** absolute x position of input on screen */
  public int mouseY;

//  /** current x position of motion (relative to start of motion) */
//  public float motionX;
//
//  /** current y position of the mouse (relative to start of motion) */
//  public float motionY;
//
//  /** Last reported pressure of the current motion event */
//  public float motionPressure;
//
//  /** Last reported positions and pressures for all pointers */
//  protected int numPointers;
//  protected int pnumPointers;
//
//  protected float[] ppointersX = {0};
//  protected float[] ppointersY = {0};
//  protected float[] ppointersPressure = {0};
//
//  protected float[] pointersX = {0};
//  protected float[] pointersY = {0};
//  protected float[] pointersPressure = {0};
//
//  protected int downMillis;
//  protected float downX, downY;
//  protected boolean onePointerGesture = false;
//  protected boolean twoPointerGesture = true;
//
//  protected final int MIN_SWIPE_LENGTH = 150;       // Minimum length (in pixels) of a swipe event
//  protected final int MAX_SWIPE_DURATION = 2000;    // Maximum duration (in millis) of a swipe event
//  protected final int MAX_TAP_DISP = 20;            // Maximum displacement (in pixels) during a tap event
//  protected final int MAX_TAP_DURATION = 1000;      // Maximum duration (in millis) of a tap event


  /**
   * Previous x/y position of the mouse. This will be a different value
   * when inside a mouse handler (like the mouseMoved() method) versus
   * when inside draw(). Inside draw(), pmouseX is updated once each
   * frame, but inside mousePressed() and friends, it's updated each time
   * an event comes through. Be sure to use only one or the other type of
   * means for tracking pmouseX and pmouseY within your sketch, otherwise
   * you're gonna run into trouble.
   */
  public int pmouseX, pmouseY;
//  public float pmotionX, pmotionY;

  /**
   * previous mouseX/Y for the draw loop, separated out because this is
   * separate from the pmouseX/Y when inside the mouse event handlers.
   */
  protected int dmouseX, dmouseY;
//  protected float dmotionX, dmotionY;

  /**
   * pmotionX/Y for the event handlers (motionPressed(), motionDragged() etc)
   * these are different because motion events are queued to the end of
   * draw, so the previous position has to be updated on each event,
   * as opposed to the pmotionX/Y that's used inside draw, which is expected
   * to be updated once per trip through draw().
   */
  protected int emouseX, emouseY;
//  protected float emotionX, emotionY;

//  /**
//   * Used to set pmotionX/Y to motionX/Y the first time motionX/Y are used,
//   * otherwise pmotionX/Y are always zero, causing a nasty jump.
//   * <P>
//   * Just using (frameCount == 0) won't work since motionXxxxx()
//   * may not be called until a couple frames into things.
//   */
//  public boolean firstMotion;

//  public int mouseButton;

  public boolean mousePressed;

  public MouseEvent mouseEvent;

//  public MotionEvent motionEvent;

  /** Post events to the main thread that created the Activity */
  Handler handler;

  /**
   * Last key pressed.
   * <P>
   * If it's a coded key, i.e. UP/DOWN/CTRL/SHIFT/ALT,
   * this will be set to CODED (0xffff or 65535).
   */
  public char key;

  /**
   * When "key" is set to CODED, this will contain a Java key code.
   * <P>
   * For the arrow keys, keyCode will be one of UP, DOWN, LEFT and RIGHT.
   * Also available are ALT, CONTROL and SHIFT. A full set of constants
   * can be obtained from java.awt.event.KeyEvent, from the VK_XXXX variables.
   */
  public int keyCode;

  /**
   * true if the mouse is currently pressed.
   */
  public boolean keyPressed;

  /**
   * the last KeyEvent object passed into a mouse function.
   */
  public KeyEvent keyEvent;

  /**
   * Gets set to true/false as the applet gains/loses focus.
   */
  public boolean focused = false;

  protected boolean windowFocused = false;
  protected boolean viewFocused = false;

  /**
   * true if the applet is online.
   * <P>
   * This can be used to test how the applet should behave
   * since online situations are different (no file writing, etc).
   */
//  public boolean online = false;

  /**
   * Time in milliseconds when the applet was started.
   * <P>
   * Used by the millis() function.
   */
  long millisOffset = System.currentTimeMillis();

  /**
   * The current value of frames per second.
   * <P>
   * The initial value will be 10 fps, and will be updated with each
   * frame thereafter. The value is not instantaneous (since that
   * wouldn't be very useful since it would jump around so much),
   * but is instead averaged (integrated) over several frames.
   * As such, this value won't be valid until after 5-10 frames.
   */
  public float frameRate = 10;
  /** Last time in nanoseconds that frameRate was checked */
  protected long frameRateLastNanos = 0;

  /** As of release 0116, frameRate(60) is called as a default */
  protected float frameRateTarget = 60;
  protected long frameRatePeriod = 1000000000L / 60L;

  protected boolean looping;

  /** flag set to true when a redraw is asked for by the user */
  protected boolean redraw;

  /**
   * How many frames have been displayed since the applet started.
   * <P>
   * This value is read-only <EM>do not</EM> attempt to set it,
   * otherwise bad things will happen.
   * <P>
   * Inside setup(), frameCount is 0.
   * For the first iteration of draw(), frameCount will equal 1.
   */
  public int frameCount;

  /**
   * true if this applet has had it.
   */
  public boolean finished;

  /**
   * For Android, true if the activity has been paused.
   */
  protected boolean paused;

  protected SurfaceView surfaceView;

  /**
   * The Window object for Android.
   */
//  protected Window window;

  /**
   * true if exit() has been called so that things shut down
   * once the main thread kicks off.
   */
  protected boolean exitCalled;

  Thread thread;

  // messages to send if attached as an external vm

  /**
   * Position of the upper-lefthand corner of the editor window
   * that launched this applet.
   */
  static public final String ARGS_EDITOR_LOCATION = "--editor-location";

  /**
   * Location for where to position the applet window on screen.
   * <P>
   * This is used by the editor to when saving the previous applet
   * location, or could be used by other classes to launch at a
   * specific position on-screen.
   */
  static public final String ARGS_EXTERNAL = "--external";

  static public final String ARGS_LOCATION = "--location";

  static public final String ARGS_DISPLAY = "--display";

  static public final String ARGS_BGCOLOR = "--bgcolor";

  static public final String ARGS_PRESENT = "--present";

  static public final String ARGS_EXCLUSIVE = "--exclusive";

  static public final String ARGS_STOP_COLOR = "--stop-color";

  static public final String ARGS_HIDE_STOP = "--hide-stop";

  /**
   * Allows the user or PdeEditor to set a specific sketch folder path.
   * <P>
   * Used by PdeEditor to pass in the location where saveFrame()
   * and all that stuff should write things.
   */
  static public final String ARGS_SKETCH_FOLDER = "--sketch-path";

  /**
   * When run externally to a PdeEditor,
   * this is sent by the applet when it quits.
   */
  //static public final String EXTERNAL_QUIT = "__QUIT__";
  static public final String EXTERNAL_STOP = "__STOP__";

  /**
   * When run externally to a PDE Editor, this is sent by the applet
   * whenever the window is moved.
   * <P>
   * This is used so that the editor can re-open the sketch window
   * in the same position as the user last left it.
   */
  static public final String EXTERNAL_MOVE = "__MOVE__";

  /** true if this sketch is being run by the PDE */
  boolean external = false;

  static final String ERROR_MIN_MAX =
    "Cannot use min() or max() on an empty array.";


  //////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////


  /** Called with the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
//    println("PApplet.onCreate()");

    if (DEBUG) println("onCreate() happening here: " + Thread.currentThread().getName());

    Window window = getWindow();

    // Take up as much area as possible
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

    // This does the actual full screen work
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

    DisplayMetrics dm = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(dm);
    displayWidth = dm.widthPixels;
    displayHeight = dm.heightPixels;
//    println("density is " + dm.density);
//    println("densityDpi is " + dm.densityDpi);
    if (DEBUG) println("display metrics: " + dm);

    //println("screen size is " + screenWidth + "x" + screenHeight);

//    LinearLayout layout = new LinearLayout(this);
//    layout.setOrientation(LinearLayout.VERTICAL | LinearLayout.HORIZONTAL);
//    viewGroup = new ViewGroup();
//    surfaceView.setLayoutParams();
//    viewGroup.setLayoutParams(LayoutParams.)
//    RelativeLayout layout = new RelativeLayout(this);
//    RelativeLayout overallLayout = new RelativeLayout(this);
//    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.FILL_PARENT);
//lp.addRule(RelativeLayout.RIGHT_OF, tv1.getId());
//    layout.setGravity(RelativeLayout.CENTER_IN_PARENT);

    int sw = sketchWidth();
    int sh = sketchHeight();

    if (sketchRenderer().equals(JAVA2D)) {
      surfaceView = new SketchSurfaceView(this, sw, sh);
    } else if (sketchRenderer().equals(P2D) || sketchRenderer().equals(P3D)) {
      surfaceView = new SketchSurfaceViewGL(this, sw, sh, sketchRenderer().equals(P3D));
    }
//    g = ((SketchSurfaceView) surfaceView).getGraphics();

//    surfaceView.setLayoutParams(new LayoutParams(sketchWidth(), sketchHeight()));

//    layout.addView(surfaceView);
//    surfaceView.setVisibility(1);
//    println("visibility " + surfaceView.getVisibility() + " " + SurfaceView.VISIBLE);
//    layout.addView(surfaceView);
//    AttributeSet as = new AttributeSet();
//    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(layout, as);

//    lp.addRule(android.R.styleable.ViewGroup_Layout_layout_height,
//    layout.add
    //lp.addRule(, arg1)
    //layout.addView(surfaceView, sketchWidth(), sketchHeight());

//      new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
//        RelativeLayout.LayoutParams.FILL_PARENT);

    if (sw == displayWidth && sh == displayHeight) {
      // If using the full screen, don't embed inside other layouts
      window.setContentView(surfaceView);
    } else {
      // If not using full screen, setup awkward view-inside-a-view so that
      // the sketch can be centered on screen. (If anyone has a more efficient
      // way to do this, please file an issue on Google Code, otherwise you
      // can keep your "talentless hack" comments to yourself. Ahem.)
      RelativeLayout overallLayout = new RelativeLayout(this);
      RelativeLayout.LayoutParams lp =
        new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                        LayoutParams.WRAP_CONTENT);
      lp.addRule(RelativeLayout.CENTER_IN_PARENT);

      LinearLayout layout = new LinearLayout(this);
      layout.addView(surfaceView, sketchWidth(), sketchHeight());
      overallLayout.addView(layout, lp);
      window.setContentView(overallLayout);
    }

    /*
    // Here we use Honeycomb API (11+) to hide (in reality, just make the status icons into small dots)
    // the status bar. Since the core is still built against API 7 (2.1), we use introspection to get
    // the setSystemUiVisibility() method from the view class.
    Method visibilityMethod = null;
    try {
      visibilityMethod = surfaceView.getClass().getMethod("setSystemUiVisibility", new Class[] { int.class});
    } catch (NoSuchMethodException e) {
      // Nothing to do. This means that we are running with a version of Android previous to Honeycomb.
    }
    if (visibilityMethod != null) {
      try {
        // This is equivalent to calling:
        //surfaceView.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        // The value of View.STATUS_BAR_HIDDEN is 1.
        visibilityMethod.invoke(surfaceView, new Object[] { 1 });
      } catch (InvocationTargetException e) {
      } catch (IllegalAccessException e) {
      }
    }
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
    */


//    layout.addView(surfaceView, lp);
//    surfaceView.setLayoutParams(new LayoutParams(sketchWidth(), sketchHeight()));

//    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams()
//    layout.addView(surfaceView, new LayoutParams(arg0)

    // TODO probably don't want to set these here, can't we wait for surfaceChanged()?
    // removing this in 0187
//    width = screenWidth;
//    height = screenHeight;

//    int left = (screenWidth - iwidth) / 2;
//    int right = screenWidth - (left + iwidth);
//    int top = (screenHeight - iheight) / 2;
//    int bottom = screenHeight - (top + iheight);
//    surfaceView.setPadding(left, top, right, bottom);
    // android:layout_width

//    window.setContentView(surfaceView);  // set full screen

    // code below here formerly from init()

    //millisOffset = System.currentTimeMillis(); // moved to the variable declaration

    finished = false; // just for clarity

    // this will be cleared by draw() if it is not overridden
    looping = true;
    redraw = true;  // draw this guy once
//    firstMotion = true;

    Context context = getApplicationContext();
    sketchPath = context.getFilesDir().getAbsolutePath();

//    Looper.prepare();
    handler = new Handler();
//    println("calling loop()");
//    Looper.loop();
//    println("done with loop() call, will continue...");

    start();
  }


  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    if (DEBUG) System.out.println("configuration changed: " + newConfig);
    super.onConfigurationChanged(newConfig);
  }


  @Override
  protected void onResume() {
    super.onResume();

    // TODO need to bring back app state here!
//    surfaceView.onResume();
    if (DEBUG) System.out.println("PApplet.onResume() called");
    paused = false;
    handleMethods("resume");
    //start();  // kick the thread back on
    resume();
//    surfaceView.onResume();
  }


  @Override
  protected void onPause() {
    super.onPause();

    // TODO need to save all application state here!
//    System.out.println("PApplet.onPause() called");
    paused = true;
    handleMethods("pause");
    pause();  // handler for others to write
//  synchronized (this) {
//  paused = true;
//}
//    surfaceView.onPause();
  }


  /**
   * Developers can override here to save state. The 'paused' variable will be
   * set before this function is called.
   */
  public void pause() {
  }


  /**
   * Developers can override here to restore state. The 'paused' variable
   * will be cleared before this function is called.
   */
  public void resume() {
  }


  @Override
  public void onDestroy() {
//    stop();
    dispose();
    if (PApplet.DEBUG) {
      System.out.println("PApplet.onDestroy() called");
    }
    super.onDestroy();
    //finish();
  }



  //////////////////////////////////////////////////////////////

  // ANDROID SURFACE VIEW


  // TODO this is only used by A2D, when finishing up a draw. but if the
  // surfaceview has changed, then it might belong to an a3d surfaceview. hrm.
  public SurfaceHolder getSurfaceHolder() {
    return surfaceView.getHolder();
//    return surfaceHolder;
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  public interface SketchSurfaceView {
//    public PGraphics getGraphics();
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public class SketchSurfaceView extends SurfaceView
  implements /*SketchSurfaceView,*/ SurfaceHolder.Callback {
    PGraphicsAndroid2D g2;
    SurfaceHolder surfaceHolder;


    public SketchSurfaceView(Context context, int wide, int high) {
      super(context);

//      println("surface holder");
      // Install a SurfaceHolder.Callback so we get notified when the
      // underlying surface is created and destroyed
      surfaceHolder = getHolder();
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

//      println("creating graphics");
      g2 = new PGraphicsAndroid2D();
      // Set semi-arbitrary size; will be set properly when surfaceChanged() called
      g2.setSize(wide, high);
//      newGraphics.setSize(getWidth(), getHeight());
      g2.setParent(PApplet.this);
      g2.setPrimary(true);
      // Set the value for 'g' once everything is ready (otherwise rendering
      // may attempt before setSize(), setParent() etc)
//      g = newGraphics;
      g = g2;  // assign the g object for the PApplet

//      println("setting focusable, requesting focus");
      setFocusable(true);
      setFocusableInTouchMode(true);
      requestFocus();
//      println("done making surface view");
    }


//    public PGraphics getGraphics() {
//      return g2;
//    }


    // part of SurfaceHolder.Callback
    public void surfaceCreated(SurfaceHolder holder) {
    }


    // part of SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder holder) {
      //g2.dispose();
    }


    // part of SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
      if (DEBUG) {
        System.out.println("SketchSurfaceView2D.surfaceChanged() " + w + " " + h);
      }
      surfaceChanged = true;

//      width = w;
//      height = h;
//
//      g.setSize(w, h);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
      surfaceWindowFocusChanged(hasFocus);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
      return surfaceTouchEvent(event);
    }


    @Override
    public boolean onKeyDown(int code, android.view.KeyEvent event) {
      return surfaceKeyDown(code, event);
    }


    @Override
    public boolean onKeyUp(int code, android.view.KeyEvent event) {
      return surfaceKeyUp(code, event);
    }


    // don't think i want to call stop() from here, since it might be swapping renderers
//    @Override
//    protected void onDetachedFromWindow() {
//      super.onDetachedFromWindow();
//      stop();
//    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public class SketchSurfaceViewGL extends GLSurfaceView /*implements SketchSurfaceView*/ {
    PGraphicsOpenGL g3;
    SurfaceHolder surfaceHolder;


    public SketchSurfaceViewGL(Context context, int wide, int high, boolean is3D) {
      super(context);

      // Check if the system supports OpenGL ES 2.0.
      final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
      final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
      final boolean supportsGLES2 = configurationInfo.reqGlEsVersion >= 0x20000;

      if (!supportsGLES2) {
        throw new RuntimeException("OpenGL ES 2.0 is not supported by this device.");
      }

      surfaceHolder = getHolder();
      // are these two needed?
      surfaceHolder.addCallback(this);
      //surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

      // The PGraphics object needs to be created here so the renderer is not
      // null. This is required because PApplet.onResume events (which call
      // this.onResume() and thus require a valid renderer) are triggered
      // before surfaceChanged() is ever called.
      if (is3D) {
        g3 = new PGraphics3D();
      } else {
        g3 = new PGraphics2D();
      }
      g3.setParent(PApplet.this);
      g3.setPrimary(true);
      // Set semi-arbitrary size; will be set properly when surfaceChanged() called
      g3.setSize(wide, high);

      // Tells the default EGLContextFactory and EGLConfigChooser to create an GLES2 context.
      setEGLContextClientVersion(2);

      // The renderer can be set only once.
      setRenderer(g3.pgl.getRenderer());
      setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

      // assign this g to the PApplet
      g = g3;

      setFocusable(true);
      setFocusableInTouchMode(true);
      requestFocus();
    }


    public PGraphics getGraphics() {
      return g3;
    }


    // part of SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      super.surfaceCreated(holder);
      if (DEBUG) {
        System.out.println("surfaceCreated()");
      }
    }


    // part of SurfaceHolder.Callback
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      super.surfaceDestroyed(holder);
      if (DEBUG) {
        System.out.println("surfaceDestroyed()");
      }

      /*
      // TODO: Check how to make sure of calling g3.dispose() when this call to
      // surfaceDestoryed corresponds to the sketch being shut down instead of just
      // taken to the background.

      // For instance, something like this would be ok?
      // The sketch is being stopped, so we dispose the resources.
      if (!paused) {
        g3.dispose();
      }
      */
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
      super.surfaceChanged(holder, format, w, h);

      if (DEBUG) {
        System.out.println("SketchSurfaceView3D.surfaceChanged() " + w + " " + h);
      }
      surfaceChanged = true;
//      width = w;
//      height = h;
//      g.setSize(w, h);

      // No need to call g.setSize(width, height) b/c super.surfaceChanged()
      // will trigger onSurfaceChanged in the renderer, which calls setSize().
      // -- apparently not true? (100110)
    }


    /**
     * Inform the view that the window focus has changed.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
      surfaceWindowFocusChanged(hasFocus);
//      super.onWindowFocusChanged(hasFocus);
//      focused = hasFocus;
//      if (focused) {
////        println("got focus");
//        focusGained();
//      } else {
////        println("lost focus");
//        focusLost();
//      }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
      return surfaceTouchEvent(event);
    }


    @Override
    public boolean onKeyDown(int code, android.view.KeyEvent event) {
      return surfaceKeyDown(code, event);
    }


    @Override
    public boolean onKeyUp(int code, android.view.KeyEvent event) {
      return surfaceKeyUp(code, event);
    }


    // don't think i want to call stop() from here, since it might be swapping renderers
//    @Override
//    protected void onDetachedFromWindow() {
//      super.onDetachedFromWindow();
//      stop();
//    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /** Holy crap this was an ugly one. Need to fix. */
  public void andresNeedsBetterAPI() {
    if (looping) { // This "if" is needed to avoid flickering when looping is disabled.
      ((GLSurfaceView) surfaceView).requestRender();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Called by the sketch surface view, thought it could conceivably be called
   * by Android as well.
   */
  public void surfaceWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    focused = hasFocus;
    if (focused) {
      focusGained();
    } else {
      focusLost();
    }
  }


  /**
   * If you override this function without calling super.onTouchEvent(),
   * then motionX, motionY, motionPressed, and motionEvent will not be set.
   */
  public boolean surfaceTouchEvent(MotionEvent event) {
//    println(event);
    nativeMotionEvent(event);
//    return super.onTouchEvent(event);
    return true;
  }


  public boolean surfaceKeyDown(int code, android.view.KeyEvent event) {
    //  System.out.println("got onKeyDown for " + code + " " + event);
    nativeKeyEvent(event);
    return super.onKeyDown(code, event);
  }


  public boolean surfaceKeyUp(int code, android.view.KeyEvent event) {
    //  System.out.println("got onKeyUp for " + code + " " + event);
    nativeKeyEvent(event);
    return super.onKeyUp(code, event);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public int sketchQuality() {
    return 1;
  }


  public int sketchWidth() {
    return displayWidth;
  }


  public int sketchHeight() {
    return displayHeight;
  }


  public String sketchRenderer() {
    return JAVA2D;
  }


  public void orientation(int which) {
    if (which == PORTRAIT) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } else if (which == LANDSCAPE) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
  }


//  public int sketchOrientation() {
//    return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
//    //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Called by the browser or applet viewer to inform this applet that it
   * should start its execution. It is called after the init method and
   * each time the applet is revisited in a Web page.
   * <p/>
   * Called explicitly via the first call to PApplet.paint(), because
   * PAppletGL needs to have a usable screen before getting things rolling.
   */
  public void start() {
    finished = false;
    paused = false; // unpause the thread

    if (thread == null) {
      thread = new Thread(this, "Animation Thread");
      thread.start();
    }
  }


  /**
   * Called by the browser or applet viewer to inform
   * this applet that it should stop its execution.
   * <p/>
   * Unfortunately, there are no guarantees from the Java spec
   * when or if stop() will be called (i.e. on browser quit,
   * or when moving between web pages), and it's not always called.
   */
  public void stop() {
    // this used to shut down the sketch, but that code has
    // been moved to dispose()

    paused = true; // sleep the animation thread

    //TODO listeners
  }


  /**
   * Called by the browser or applet viewer to inform this applet
   * that it is being reclaimed and that it should destroy
   * any resources that it has allocated.
   * <p/>
   * This also attempts to call PApplet.stop(), in case there
   * was an inadvertent override of the stop() function by a user.
   * <p/>
   * destroy() supposedly gets called as the applet viewer
   * is shutting down the applet. stop() is called
   * first, and then destroy() to really get rid of things.
   * no guarantees on when they're run (on browser quit, or
   * when moving between pages), though.
   */
  public void destroy() {
    ((PApplet)this).exit();
  }


  /**
   * This returns the last width and height specified by the user
   * via the size() command.
   */
//  public Dimension getPreferredSize() {
//    return new Dimension(width, height);
//  }


//  public void addNotify() {
//    super.addNotify();
//    println("addNotify()");
//  }


  //////////////////////////////////////////////////////////////


  /** Map of registered methods, stored by name. */
  HashMap<String, RegisteredMethods> registerMap =
    new HashMap<String, PApplet.RegisteredMethods>();


  class RegisteredMethods {
    int count;
    Object[] objects;
    // Because the Method comes from the class being called,
    // it will be unique for most, if not all, objects.
    Method[] methods;
    Object[] emptyArgs = new Object[] { };


    void handle() {
      handle(emptyArgs);
    }


    void handle(Object[] args) {
      for (int i = 0; i < count; i++) {
        try {
          methods[i].invoke(objects[i], args);
        } catch (Exception e) {
          // check for wrapped exception, get root exception
          Throwable t;
          if (e instanceof InvocationTargetException) {
            InvocationTargetException ite = (InvocationTargetException) e;
            t = ite.getCause();
          } else {
            t = e;
          }
          // check for RuntimeException, and allow to bubble up
          if (t instanceof RuntimeException) {
            // re-throw exception
            throw (RuntimeException) t;
          } else {
            // trap and print as usual
            t.printStackTrace();
          }
        }
      }
    }


    void add(Object object, Method method) {
      if (findIndex(object) == -1) {
        if (objects == null) {
          objects = new Object[5];
          methods = new Method[5];

        } else if (count == objects.length) {
          objects = (Object[]) PApplet.expand(objects);
          methods = (Method[]) PApplet.expand(methods);
        }
        objects[count] = object;
        methods[count] = method;
        count++;
      } else {
        die(method.getName() + "() already added for this instance of " +
            object.getClass().getName());
      }
    }


    /**
     * Removes first object/method pair matched (and only the first,
     * must be called multiple times if object is registered multiple times).
     * Does not shrink array afterwards, silently returns if method not found.
     */
//    public void remove(Object object, Method method) {
//      int index = findIndex(object, method);
    public void remove(Object object) {
      int index = findIndex(object);
      if (index != -1) {
        // shift remaining methods by one to preserve ordering
        count--;
        for (int i = index; i < count; i++) {
          objects[i] = objects[i+1];
          methods[i] = methods[i+1];
        }
        // clean things out for the gc's sake
        objects[count] = null;
        methods[count] = null;
      }
    }


//    protected int findIndex(Object object, Method method) {
    protected int findIndex(Object object) {
      for (int i = 0; i < count; i++) {
        if (objects[i] == object) {
//        if (objects[i] == object && methods[i].equals(method)) {
          //objects[i].equals() might be overridden, so use == for safety
          // since here we do care about actual object identity
          //methods[i]==method is never true even for same method, so must use
          // equals(), this should be safe because of object identity
          return i;
        }
      }
      return -1;
    }
  }


  /**
   * Register a built-in event so that it can be fired for libraries, etc.
   * Supported events include:
   * <ul>
   * <li>pre – at the very top of the draw() method (safe to draw)
   * <li>draw – at the end of the draw() method (safe to draw)
   * <li>post – after draw() has exited (not safe to draw)
   * <li>pause – called when the sketch is paused
   * <li>resume – called when the sketch is resumed
   * <li>dispose – when the sketch is shutting down (definitely not safe to draw)
   * <ul>
   * In addition, the new (for 2.0) processing.event classes are passed to
   * the following event types:
   * <ul>
   * <li>mouseEvent
   * <li>keyEvent
   * <li>touchEvent
   * </ul>
   * The older java.awt events are no longer supported.
   * See the Library Wiki page for more details.
   * @param methodName name of the method to be called
   * @param target the target object that should receive the event
   */
  public void registerMethod(String methodName, Object target) {
    if (methodName.equals("mouseEvent")) {
      registerWithArgs("mouseEvent", target, new Class[] { processing.event.MouseEvent.class });

    } else if (methodName.equals("keyEvent")) {
      registerWithArgs("keyEvent", target, new Class[] { processing.event.KeyEvent.class });

    } else if (methodName.equals("touchEvent")) {
      registerWithArgs("touchEvent", target, new Class[] { processing.event.TouchEvent.class });

    } else {
      registerNoArgs(methodName, target);
    }
  }


  private void registerNoArgs(String name, Object o) {
    RegisteredMethods meth = registerMap.get(name);
    if (meth == null) {
      meth = new RegisteredMethods();
      registerMap.put(name, meth);
    }
    Class<?> c = o.getClass();
    try {
      Method method = c.getMethod(name, new Class[] {});
      meth.add(o, method);

    } catch (NoSuchMethodException nsme) {
      die("There is no public " + name + "() method in the class " +
          o.getClass().getName());

    } catch (Exception e) {
      die("Could not register " + name + " + () for " + o, e);
    }
  }


  private void registerWithArgs(String name, Object o, Class<?> cargs[]) {
    RegisteredMethods meth = registerMap.get(name);
    if (meth == null) {
      meth = new RegisteredMethods();
      registerMap.put(name, meth);
    }
    Class<?> c = o.getClass();
    try {
      Method method = c.getMethod(name, cargs);
      meth.add(o, method);

    } catch (NoSuchMethodException nsme) {
      die("There is no public " + name + "() method in the class " +
          o.getClass().getName());

    } catch (Exception e) {
      die("Could not register " + name + " + () for " + o, e);
    }
  }


//  public void registerMethod(String methodName, Object target, Object... args) {
//    registerWithArgs(methodName, target, args);
//  }


  public void unregisterMethod(String name, Object target) {
    RegisteredMethods meth = registerMap.get(name);
    if (meth == null) {
      die("No registered methods with the name " + name + "() were found.");
    }
    try {
//      Method method = o.getClass().getMethod(name, new Class[] {});
//      meth.remove(o, method);
      meth.remove(target);
    } catch (Exception e) {
      die("Could not unregister " + name + "() for " + target, e);
    }
  }


  protected void handleMethods(String methodName) {
    RegisteredMethods meth = registerMap.get(methodName);
    if (meth != null) {
      meth.handle();
    }
  }


  protected void handleMethods(String methodName, Object[] args) {
    RegisteredMethods meth = registerMap.get(methodName);
    if (meth != null) {
      meth.handle(args);
    }
  }


  @Deprecated
  public void registerSize(Object o) {
    System.err.println("The registerSize() command is no longer supported.");
//    Class<?> methodArgs[] = new Class[] { Integer.TYPE, Integer.TYPE };
//    registerWithArgs(sizeMethods, "size", o, methodArgs);
  }


  @Deprecated
  public void registerPre(Object o) {
    registerNoArgs("pre", o);
  }


  @Deprecated
  public void registerDraw(Object o) {
    registerNoArgs("draw", o);
  }


  @Deprecated
  public void registerPost(Object o) {
    registerNoArgs("post", o);
  }


  @Deprecated
  public void registerDispose(Object o) {
    registerNoArgs("dispose", o);
  }


  @Deprecated
  public void unregisterSize(Object o) {
    System.err.println("The unregisterSize() command is no longer supported.");
//    Class<?> methodArgs[] = new Class[] { Integer.TYPE, Integer.TYPE };
//    unregisterWithArgs(sizeMethods, "size", o, methodArgs);
  }


  @Deprecated
  public void unregisterPre(Object o) {
    unregisterMethod("pre", o);
  }


  @Deprecated
  public void unregisterDraw(Object o) {
    unregisterMethod("draw", o);
  }


  @Deprecated
  public void unregisterPost(Object o) {
    unregisterMethod("post", o);
  }


  @Deprecated
  public void unregisterDispose(Object o) {
    unregisterMethod("dispose", o);
  }


  //////////////////////////////////////////////////////////////


  public void setup() {
  }


  public void draw() {
    // if no draw method, then shut things down
    //System.out.println("no draw method, goodbye");
    finished = true;
  }


  //////////////////////////////////////////////////////////////


//  protected void resizeRenderer(int iwidth, int iheight) {
////    println("resizeRenderer request for " + iwidth + " " + iheight);
//    if (width != iwidth || height != iheight) {
////      int left = (screenWidth - iwidth) / 2;
////      int right = screenWidth - (left + iwidth);
////      int top = (screenHeight - iheight) / 2;
////      int bottom = screenHeight - (top + iheight);
////      surfaceView.setPadding(left, top, right, bottom);
//
//      g.setSize(iwidth, iheight);
//      width = iwidth;
//      height = iheight;
//      overallLayout.invalidate();
//      layout.invalidate();
//    }
//  }


  /**
   * Starts up and creates a two-dimensional drawing surface, or resizes the
   * current drawing surface.
   * <P>
   * This should be the first thing called inside of setup().
   * <P>
   * If called once a renderer has already been set, this will use the
   * previous renderer and simply resize it.
   */
  public void size(int iwidth, int iheight) {
    size(iwidth, iheight, P2D, null);
  }


  public void size(int iwidth, int iheight, String irenderer) {
    size(iwidth, iheight, irenderer, null);
  }


  // not finished yet--will swap the renderer at a bad time
  /*
  public void renderer(String name) {
    if (name.equals(A2D)) {
      if (!(surfaceView instanceof SketchSurfaceView2D)) {
        surfaceView = new SketchSurfaceView2D(this);
        getWindow().setContentView(surfaceView);  // set full screen
      }
    } else if (name.equals(A3D)) {
      if (!(surfaceView instanceof SketchSurfaceView3D)) {
        surfaceView = new SketchSurfaceView3D(this);
        getWindow().setContentView(surfaceView);  // set full screen
      }
    }
  }
  */


  /**
   * Creates a new PGraphics object and sets it to the specified size.
   *
   * Note that you cannot change the renderer once outside of setup().
   * In most cases, you can call size() to give it a new size,
   * but you need to always ask for the same renderer, otherwise
   * you're gonna run into trouble.
   *
   * The size() method should *only* be called from inside the setup() or
   * draw() methods, so that it is properly run on the main animation thread.
   * To change the size of a PApplet externally, use setSize(), which will
   * update the component size, and queue a resize of the renderer as well.
   */
  public void size(final int iwidth, final int iheight,
                   final String irenderer, final String ipath) {
    System.out.println("This size() method is ignored on Android.");
    System.out.println("See http://wiki.processing.org/w/Android for more information.");

    /*
//    Looper.prepare();
    // Run this from the EDT, just cuz it's AWT stuff (or maybe later Swing)
//    new Handler().post(new Runnable() {
    handler.post(new Runnable() {
      public void run() {
        println("Handler is on thread " + Thread.currentThread().getName());
//        // Set the preferred size so that the layout managers can handle it
////        setPreferredSize(new Dimension(iwidth, iheight));
////        setSize(iwidth, iheight);
//        g.setSize(iwidth, iheight);
//      }
//    });

    // ensure that this is an absolute path
//    if (ipath != null) ipath = savePath(ipath);
        // no path renderers supported yet

    println("I'm the ole thread " + Thread.currentThread().getName());
    String currentRenderer = g.getClass().getName();
    if (currentRenderer.equals(irenderer)) {
      println("resizing renderer inside size(....)");
      // Avoid infinite loop of throwing exception to reset renderer
      resizeRenderer(iwidth, iheight);
      //redraw();  // will only be called insize draw()

    } else {  // renderer is being changed
      println("changing renderer inside size(....)");

      // otherwise ok to fall through and create renderer below
      // the renderer is changing, so need to create a new object
//      g = makeGraphics(iwidth, iheight, irenderer, ipath, true);
//      width = iwidth;
//      height = iheight;
//      if ()

      // Remove the old view from the layout
      layout.removeView(surfaceView);

      if (irenderer.equals(A2D)) {
        surfaceView = new SketchSurfaceView2D(PApplet.this, iwidth, iheight);
      } else if (irenderer.equals(A3D)) {
        surfaceView = new SketchSurfaceView3D(PApplet.this, iwidth, iheight);
      }
      g = ((SketchSurfaceView) surfaceView).getGraphics();

      // these don't seem like a good idea
//      width = screenWidth;
//      height = screenHeight;

//      println("getting window");
//      Window window = getWindow();
//      window.setContentView(surfaceView);  // set full screen
//      layout.removeAllViews();
      layout.addView(surfaceView);

      // fire resize event to make sure the applet is the proper size
//      setSize(iwidth, iheight);
      // this is the function that will run if the user does their own
      // size() command inside setup, so set defaultSize to false.
//      defaultSize = false;

      // throw an exception so that setup() is called again
      // but with a properly sized render
      // this is for opengl, which needs a valid, properly sized
      // display before calling anything inside setup().
//      throw new RendererChangeException();
      println("interrupting animation thread");
      thread.interrupt();
    }
      }
    });
*/
  }


  public PGraphics createGraphics(int iwidth, int iheight) {
    return createGraphics(iwidth, iheight, JAVA2D);
  }


  /**
   * Create an offscreen PGraphics object for drawing. This can be used
   * for bitmap or vector images drawing or rendering.
   * <UL>
   * <LI>Do not use "new PGraphicsXxxx()", use this method. This method
   * ensures that internal variables are set up properly that tie the
   * new graphics context back to its parent PApplet.
   * <LI>The basic way to create bitmap images is to use the <A
   * HREF="http://processing.org/reference/saveFrame_.html">saveFrame()</A>
   * function.
   * <LI>If you want to create a really large scene and write that,
   * first make sure that you've allocated a lot of memory in the Preferences.
   * <LI>If you want to create images that are larger than the screen,
   * you should create your own PGraphics object, draw to that, and use
   * <A HREF="http://processing.org/reference/save_.html">save()</A>.
   * For now, it's best to use <A HREF="http://dev.processing.org/reference/everything/javadoc/processing/core/PGraphics3D.html">P3D</A> in this scenario.
   * P2D is currently disabled, and the JAVA2D default will give mixed
   * results. An example of using P3D:
   * <PRE>
   *
   * PGraphics big;
   *
   * void setup() {
   *   big = createGraphics(3000, 3000, P3D);
   *
   *   big.beginDraw();
   *   big.background(128);
   *   big.line(20, 1800, 1800, 900);
   *   // etc..
   *   big.endDraw();
   *
   *   // make sure the file is written to the sketch folder
   *   big.save("big.tif");
   * }
   *
   * </PRE>
   * <LI>It's important to always wrap drawing to createGraphics() with
   * beginDraw() and endDraw() (beginFrame() and endFrame() prior to
   * revision 0115). The reason is that the renderer needs to know when
   * drawing has stopped, so that it can update itself internally.
   * This also handles calling the defaults() method, for people familiar
   * with that.
   * <LI>It's not possible to use createGraphics() with the OPENGL renderer,
   * because it doesn't allow offscreen use.
   * <LI>With Processing 0115 and later, it's possible to write images in
   * formats other than the default .tga and .tiff. The exact formats and
   * background information can be found in the developer's reference for
   * <A HREF="http://dev.processing.org/reference/core/javadoc/processing/core/PImage.html#save(java.lang.String)">PImage.save()</A>.
   * </UL>
   */
  public PGraphics createGraphics(int iwidth, int iheight, String irenderer) {
    PGraphics pg = null;
    if (irenderer.equals(JAVA2D)) {
      pg = new PGraphicsAndroid2D();
    } else if (irenderer.equals(P2D)) {
      if (!g.isGL()) {
        throw new RuntimeException("createGraphics() with P2D requires size() to use P2D or P3D");
      }
      pg = new PGraphics2D();
    } else if (irenderer.equals(P3D)) {
      if (!g.isGL()) {
        throw new RuntimeException("createGraphics() with P3D or OPENGL requires size() to use P2D or P3D");
      }
      pg = new PGraphics3D();
    } else {
      Class<?> rendererClass = null;
      Constructor<?> constructor = null;
      try {
        // The context class loader doesn't work:
        //rendererClass = Thread.currentThread().getContextClassLoader().loadClass(irenderer);
        // even though it should, according to this discussion:
        // http://code.google.com/p/android/issues/detail?id=11101
        // While the method that is not supposed to work, using the class loader, does:
        rendererClass = this.getClass().getClassLoader().loadClass(irenderer);
      } catch (ClassNotFoundException cnfe) {
        throw new RuntimeException("Missing renderer class");
      }

      if (rendererClass != null) {
        try {
          constructor = rendererClass.getConstructor(new Class[] { });
        } catch (NoSuchMethodException nsme) {
          throw new RuntimeException("Missing renderer constructor");
        }

        if (constructor != null) {
          try {
            pg = (PGraphics) constructor.newInstance();
          } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
          } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
          }
        }
      }
    }

    pg.setParent(this);
    pg.setPrimary(false);
    pg.setSize(iwidth, iheight);

    return pg;
  }


  /**
   * Create an offscreen graphics surface for drawing, in this case
   * for a renderer that writes to a file (such as PDF or DXF).
   * @param ipath can be an absolute or relative path
   */
//  public PGraphics createGraphics(int iwidth, int iheight,
//                                  String irenderer, String ipath) {
//    if (ipath != null) {
//      ipath = savePath(ipath);
//    }
//    PGraphics pg = makeGraphics(iwidth, iheight, irenderer, ipath, false);
//    pg.parent = this;  // make save() work
//    return pg;
//  }


  /**
   * Version of createGraphics() used internally.
   *
   * @param ipath must be an absolute path, usually set via savePath()
   * @oaram applet the parent applet object, this should only be non-null
   *               in cases where this is the main drawing surface object.
   */
  /*
  protected PGraphics makeGraphics(int iwidth, int iheight,
                                   String irenderer, String ipath,
                                   boolean iprimary) {
    try {
      Class<?> rendererClass =
        Thread.currentThread().getContextClassLoader().loadClass(irenderer);

      Constructor<?> constructor = rendererClass.getConstructor(new Class[] { });
      PGraphics pg = (PGraphics) constructor.newInstance();

      pg.setParent(this);
      pg.setPrimary(iprimary);
      if (ipath != null) pg.setPath(ipath);
      pg.setSize(iwidth, iheight);

      // everything worked, return it
      return pg;

    } catch (InvocationTargetException ite) {
      ite.getTargetException().printStackTrace();
      Throwable target = ite.getTargetException();
      throw new RuntimeException(target.getMessage());

    } catch (ClassNotFoundException cnfe) {
      throw new RuntimeException("You need to use \"Import Library\" " +
                                 "to add " + irenderer + " to your sketch.");
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }
  */


  /**
   * Creates a new PImage (the datatype for storing images). This provides a fresh buffer of pixels to play with. Set the size of the buffer with the <b>width</b> and <b>height</b> parameters. The <b>format</b> parameter defines how the pixels are stored. See the PImage reference for more information.
   */
  public PImage createImage(int wide, int high, int format) {
//    return createImage(wide, high, format, null);
//  }
//
//
//  /**
//   * Preferred method of creating new PImage objects, ensures that a
//   * reference to the parent PApplet is included, which makes save() work
//   * without needing an absolute path.
//   */
//  public PImage createImage(int wide, int high, int format, Object params) {
    PImage image = new PImage(wide, high, format);
//    if (params != null) {
//      image.setParams(g, params);
//    }
    image.parent = this;  // make save() work
    return image;
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // not necessary, ja?
//  public void update(Graphics screen) {
//    paint(screen);
//  }


  /*
  //synchronized public void paint(Graphics screen) {  // shutting off for 0146
  public void paint(Graphics screen) {
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

    // g.image is synchronized so that draw/loop and paint don't
    // try to fight over it. this was causing a randomized slowdown
    // that would cut the frameRate into a third on macosx,
    // and is probably related to the windows sluggishness bug too

    // make sure the screen is visible and usable
    // (also prevents over-drawing when using PGraphicsOpenGL)
    if ((g != null) && (g.image != null)) {
//      println("inside paint(), screen.drawImage()");
      screen.drawImage(g.image, 0, 0, null);
    }
  }
  */


  // active paint method
  /*
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

    } finally {
      if (g != null) {
        g.dispose();
      }
    }
  }
  */


  //////////////////////////////////////////////////////////////


  /**
   * Main method for the primary animation thread.
   */
  public void run() {  // not good to make this synchronized, locks things up
    long beforeTime = System.nanoTime();
    long overSleepTime = 0L;

    int noDelays = 0;
    // Number of frames with a delay of 0 ms before the
    // animation thread yields to other running threads.
    final int NO_DELAYS_PER_YIELD = 15;

    while ((Thread.currentThread() == thread) && !finished) {

      while (paused) {
        try{
          Thread.sleep(100L);
        } catch (InterruptedException e) {
          //ignore?
        }
      }

      // Don't resize the renderer from the EDT (i.e. from a ComponentEvent),
      // otherwise it may attempt a resize mid-render.
//      if (resizeRequest) {
//        resizeRenderer(resizeWidth, resizeHeight);
//        resizeRequest = false;
//      }

      // render a single frame
      if (g != null) g.requestDraw();
//      g.requestDraw();
//      surfaceView.requestDraw();

      // removed in android
//      if (frameCount == 1) {
//        // Call the request focus event once the image is sure to be on
//        // screen and the component is valid. The OpenGL renderer will
//        // request focus for its canvas inside beginDraw().
//        // http://java.sun.com/j2se/1.4.2/docs/api/java/awt/doc-files/FocusSpec.html
//        //println("requesting focus");
//        requestFocus();
//      }

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
//          Thread.sleep(sleepTime / 1000000L);  // nanoseconds -> milliseconds
          Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
          noDelays = 0;  // Got some sleep, not delaying anymore
        } catch (InterruptedException ex) { }

        overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
        //System.out.println("  oversleep is " + overSleepTime);

      } else {    // sleepTime <= 0; the frame took longer than the period
//        excess -= sleepTime;  // store excess time value
        overSleepTime = 0L;

        if (noDelays > NO_DELAYS_PER_YIELD) {
          Thread.yield();   // give another thread a chance to run
          noDelays = 0;
        }
      }

      beforeTime = System.nanoTime();
    }

    // if this isn't just a pause, shut it all down
    if (!paused) {
      stop();  // call to shutdown libs?

      // If the user called the exit() function, the window should close,
      // rather than the sketch just halting.
      if (exitCalled) {
        exit2();
      }
    }
  }


  public void handleDraw() {
    if (DEBUG) {
      println("inside handleDraw() " + millis() +
              " changed=" + surfaceChanged +
              " ready=" + surfaceReady +
              " paused=" + paused +
              " looping=" + looping +
              " redraw=" + redraw);
    }
    if (surfaceChanged) {
      int newWidth = surfaceView.getWidth();
      int newHeight = surfaceView.getHeight();
      if (newWidth != width || newHeight != height) {
        width = newWidth;
        height = newHeight;
        g.setSize(width, height);
      }
      surfaceChanged = false;
      surfaceReady = true;
      if (DEBUG) {
        println("surfaceChanged true, resized to " + width + "x" + height);
      }
    }

//    if (surfaceView.isShown()) {
//      println("surface view not visible, getting out");
//      return;
//    } else {
//      println("surface set to go.");
//    }

    // don't start drawing (e.g. don't call setup) until there's a legitimate
    // width and height that have been set by surfaceChanged().
//    boolean validSize = width != 0 && height != 0;
//    println("valid size = " + validSize + " (" + width + "x" + height + ")");
    if (g != null && surfaceReady && !paused && (looping || redraw)) {
//      if (!g.canDraw()) {
//        // Don't draw if the renderer is not yet ready.
//        // (e.g. OpenGL has to wait for a peer to be on screen)
//        return;
//      }

      g.beginDraw();

      long now = System.nanoTime();

      if (frameCount == 0) {
        try {
          //println("Calling setup()");
          setup();
          //println("Done with setup()");

        } catch (RendererChangeException e) {
          // Give up, instead set the new renderer and re-attempt setup()
          return;
        }
//        this.defaultSize = false;

      } else {  // frameCount > 0, meaning an actual draw()
        // update the current frameRate
        double rate = 1000000.0 / ((now - frameRateLastNanos) / 1000000.0);
        float instantaneousRate = (float) rate / 1000.0f;
        frameRate = (frameRate * 0.9f) + (instantaneousRate * 0.1f);

        if (frameCount != 0) {
          handleMethods("pre");
        }

        // use dmouseX/Y as previous mouse pos, since this is the
        // last position the mouse was in during the previous draw.
        pmouseX = dmouseX;
        pmouseY = dmouseY;
//        pmotionX = dmotionX;
//        pmotionY = dmotionY;

        //println("Calling draw()");
        draw();
        //println("Done calling draw()");

        // dmouseX/Y is updated only once per frame (unlike emouseX/Y)
        dmouseX = mouseX;
        dmouseY = mouseY;
//        dmotionX = motionX;
//        dmotionY = motionY;

        // these are called *after* loop so that valid
        // drawing commands can be run inside them. it can't
        // be before, since a call to background() would wipe
        // out anything that had been drawn so far.
//        dequeueMotionEvents();
//        dequeueKeyEvents();
        dequeueEvents();

        handleMethods("draw");

        redraw = false;  // unset 'redraw' flag in case it was set
        // (only do this once draw() has run, not just setup())
      }
      g.endDraw();

      if (frameCount != 0) {
        handleMethods("post");
      }

      frameRateLastNanos = now;
      frameCount++;
    }
  }


  //////////////////////////////////////////////////////////////



  synchronized public void redraw() {
    if (!looping) {
      redraw = true;
//      if (thread != null) {
//        // wake from sleep (necessary otherwise it'll be
//        // up to 10 seconds before update)
//        if (CRUSTY_THREADS) {
//          thread.interrupt();
//        } else {
//          synchronized (blocker) {
//            blocker.notifyAll();
//          }
//        }
//      }
    }
  }


  synchronized public void loop() {
    if (!looping) {
      looping = true;
    }
  }


  synchronized public void noLoop() {
    if (looping) {
      looping = false;
    }
  }


  //////////////////////////////////////////////////////////////


  // all these are handled in SurfaceView, which is a listener for all of em
//  public void addListeners() {
//    addMouseListener(this);
//    addMouseMotionListener(this);
//    addKeyListener(this);
//    addFocusListener(this);
//
//    addComponentListener(new ComponentAdapter() {
//      public void componentResized(ComponentEvent e) {
//        Component c = e.getComponent();
//        //System.out.println("componentResized() " + c);
//        Rectangle bounds = c.getBounds();
//        resizeRequest = true;
//        resizeWidth = bounds.width;
//        resizeHeight = bounds.height;
//      }
//    });
//  }


  //////////////////////////////////////////////////////////////


  InternalEventQueue eventQueue = new InternalEventQueue();


  class InternalEventQueue {
    protected Event queue[] = new Event[10];
    protected int offset;
    protected int count;

    synchronized void add(Event e) {
      if (count == queue.length) {
        queue = (Event[]) expand(queue);
      }
      queue[count++] = e;
    }

    synchronized Event remove() {
      if (offset == count) {
        throw new RuntimeException("Nothing left on the event queue.");
      }
      Event outgoing = queue[offset++];
      if (offset == count) {
        // All done, time to reset
        offset = 0;
        count = 0;
      }
      return outgoing;
    }

    synchronized boolean available() {
      return count != 0;
    }
  }

  /**
   * Add an event to the internal event queue, or process it immediately if
   * the sketch is not currently looping.
   */
  public void postEvent(processing.event.Event pe) {
    eventQueue.add(pe);

    if (!looping) {
      dequeueEvents();
    }
  }


  protected void dequeueEvents() {
    while (eventQueue.available()) {
      Event e = eventQueue.remove();

      switch (e.getFlavor()) {
//      case Event.TOUCH:
//        handleTouchEvent((TouchEvent) e);
//        break;
      case Event.MOUSE:
        handleMouseEvent((MouseEvent) e);
        break;
      case Event.KEY:
        handleKeyEvent((KeyEvent) e);
        break;
      }
    }
  }


  //////////////////////////////////////////////////////////////


  protected void handleMouseEvent(MouseEvent event) {
    mouseEvent = event;

    // http://dev.processing.org/bugs/show_bug.cgi?id=170
    // also prevents mouseExited() on the mac from hosing the mouse
    // position, because x/y are bizarre values on the exit event.
    // see also the id check below.. both of these go together
//  if ((id == java.awt.event.MouseEvent.MOUSE_DRAGGED) ||
//      (id == java.awt.event.MouseEvent.MOUSE_MOVED)) {
    if (event.getAction() == MouseEvent.DRAG ||
        event.getAction() == MouseEvent.MOVE) {
      pmouseX = emouseX;
      pmouseY = emouseY;
      mouseX = event.getX();
      mouseY = event.getY();
    }

    // Because we only get DRAGGED (and no MOVED) events, pmouseX/Y will make
    // things jump because they aren't updated while a finger isn't on the
    // screen. This makes for weirdness with the continuous lines example,
    // causing it to jump. Since we're emulating the mouse here, do the right
    // thing for mouse events. It breaks the situation where random taps/clicks
    // to the screen won't show up as 'previous' values, but that's probably
    // less common.
//    if (event.getAction() == MouseEvent.PRESS) {
//      System.out.println("resetting");
////      pmouseX = event.getX();
////      pmouseY = event.getY();
//      firstMotion = true;
//    }

    // Get the (already processed) button code
//    mouseButton = event.getButton();

    // Added in 0215 (2.0b7) so that pmouseX/Y behave more like one would
    // expect from the desktop. This makes the ContinousLines example behave.
    if (event.getAction() == MouseEvent.PRESS) {
      mouseX = event.getX();
      mouseY = event.getY();
      pmouseX = mouseX;
      pmouseY = mouseY;
      dmouseX = mouseX;
      dmouseY = mouseY;
    }

//    if (event.getAction() == MouseEvent.RELEASE) {
//      mouseX = event.getX();
//      mouseY = event.getY();
//    }

//    mouseEvent = event;

    // Do this up here in case a registered method relies on the
    // boolean for mousePressed.

    switch (event.getAction()) {
    case MouseEvent.PRESS:
      mousePressed = true;
      break;
    case MouseEvent.RELEASE:
      mousePressed = false;
      break;
    }

    handleMethods("mouseEvent", new Object[] { event });

    switch (event.getAction()) {
    case MouseEvent.PRESS:
      mousePressed();
      break;
    case MouseEvent.RELEASE:
      mouseReleased();
      break;
    case MouseEvent.CLICK:
      mouseClicked();
      break;
    case MouseEvent.DRAG:
      mouseDragged();
      break;
    case MouseEvent.MOVE:
      mouseMoved();
      break;
    case MouseEvent.ENTER:
      mouseEntered();
      break;
    case MouseEvent.EXIT:
      mouseExited();
      break;
    }

    if ((event.getAction() == MouseEvent.DRAG) ||
        (event.getAction() == MouseEvent.MOVE)) {
      emouseX = mouseX;
      emouseY = mouseY;
    }
    if (event.getAction() == MouseEvent.PRESS) {  // Android-only
      emouseX = mouseX;
      emouseY = mouseY;
    }
//    if (event.getAction() == MouseEvent.RELEASE) {  // Android-only
//      emouseX = mouseX;
//      emouseY = mouseY;
//    }
  }


  /*
  // ******************** NOT CURRENTLY IN USE ********************
  // this class is hiding inside PApplet for now,
  // until I have a chance to get the API right inside TouchEvent.
  class AndroidTouchEvent extends TouchEvent {
    int action;
    int numPointers;
    float[] motionX, motionY;
    float[] motionPressure;
    int[] mouseX, mouseY;

    AndroidTouchEvent(Object nativeObject, long millis, int action, int modifiers) {
      super(nativeObject, millis, action, modifiers);
    }

//    void setAction(int action) {
//      this.action = action;
//    }

    void setNumPointers(int n) {
      numPointers = n;
      motionX = new float[n];
      motionY = new float[n];
      motionPressure = new float[n];
      mouseX = new int[n];
      mouseY = new int[n];
    }

    void setPointers(MotionEvent event) {
      for (int ptIdx = 0; ptIdx < numPointers; ptIdx++) {
        motionX[ptIdx] = event.getX(ptIdx);
        motionY[ptIdx] = event.getY(ptIdx);
        motionPressure[ptIdx] = event.getPressure(ptIdx);  // should this be constrained?
        mouseX[ptIdx] = (int) motionX[ptIdx];  //event.getRawX();
        mouseY[ptIdx] = (int) motionY[ptIdx];  //event.getRawY();
      }
    }

    // Sets the pointers for the historical event histIdx
    void setPointers(MotionEvent event, int hisIdx) {
      for (int ptIdx = 0; ptIdx < numPointers; ptIdx++) {
        motionX[ptIdx] = event.getHistoricalX(ptIdx, hisIdx);
        motionY[ptIdx] = event.getHistoricalY(ptIdx, hisIdx);
        motionPressure[ptIdx] = event.getHistoricalPressure(ptIdx, hisIdx);  // should this be constrained?
        mouseX[ptIdx] = (int) motionX[ptIdx];  //event.getRawX();
        mouseY[ptIdx] = (int) motionY[ptIdx];  //event.getRawY();
      }
    }
  }

//  Object motionLock = new Object();
//  AndroidTouchEvent[] motionEventQueue;
//  int motionEventCount;
//
//  protected void enqueueMotionEvent(MotionEvent event) {
//    synchronized (motionLock) {
//      // on first run, allocate array for motion events
//      if (motionEventQueue == null) {
//        motionEventQueue = new AndroidTouchEvent[20];
//        for (int i = 0; i < motionEventQueue.length; i++) {
//          motionEventQueue[i] = new AndroidTouchEvent();
//        }
//      }
//      // allocate more PMotionEvent objects if we're out
//      int historyCount = event.getHistorySize();
////      println("motion: " + motionEventCount + " " + historyCount + " " + motionEventQueue.length);
//      if (motionEventCount + historyCount >= motionEventQueue.length) {
//        int atLeast = motionEventCount + historyCount + 1;
//        AndroidTouchEvent[] temp = new AndroidTouchEvent[max(atLeast, motionEventCount << 1)];
//        if (PApplet.DEBUG) {
//          println("motion: " + motionEventCount + " " + historyCount + " " + motionEventQueue.length);
//          println("allocating " + temp.length + " entries for motion events");
//        }
//        System.arraycopy(motionEventQueue, 0, temp, 0, motionEventCount);
//        motionEventQueue = temp;
//        for (int i = motionEventCount; i < motionEventQueue.length; i++) {
//          motionEventQueue[i] = new AndroidTouchEvent();
//        }
//      }
//
//      // this will be the last event in the list
//      AndroidTouchEvent pme = motionEventQueue[motionEventCount + historyCount];
//      pme.setAction(event.getAction());
//      pme.setNumPointers(event.getPointerCount());
//      pme.setPointers(event);
//
//      // historical events happen before the 'current' values
//      if (pme.action == MotionEvent.ACTION_MOVE && historyCount > 0) {
//        for (int i = 0; i < historyCount; i++) {
//          AndroidTouchEvent hist = motionEventQueue[motionEventCount++];
//          hist.setAction(event.getAction());
//          hist.setNumPointers(event.getPointerCount());
//          hist.setPointers(event, i);
//        }
//      }
//
//      // now step over the last one that we used to assign 'pme'
//      // if historyCount is 0, this just steps over the last
//      motionEventCount++;
//    }
//  }
//
//
//  protected void dequeueMotionEvents() {
//    synchronized (motionLock) {
//      for (int i = 0; i < motionEventCount; i++) {
//        handleMotionEvent(motionEventQueue[i]);
//      }
//      motionEventCount = 0;
//    }
//  }


   // ******************** NOT CURRENTLY IN USE ********************
   // Take action based on a motion event.
   // Internally updates mouseX, mouseY, mousePressed, and mouseEvent.
   // Then it calls the event type with no params,
   // i.e. mousePressed() or mouseReleased() that the user may have
   // overloaded to do something more useful.
  protected void handleMotionEvent(AndroidTouchEvent pme) {
    pmotionX = emotionX;
    pmotionY = emotionY;
    motionX = pme.motionX[0];
    motionY = pme.motionY[0];
    motionPressure = pme.motionPressure[0];

    // replace previous mouseX/Y with the last from the event handlers
    pmouseX = emouseX;
    pmouseY = emouseY;
    mouseX = pme.mouseX[0];
    mouseY = pme.mouseY[0];

    // *** because removed from PApplet
    boolean firstMotion = false;
    // this used to only be called on mouseMoved and mouseDragged
    // change it back if people run into trouble
    if (firstMotion) {
      pmouseX = mouseX;
      pmouseY = mouseY;
      dmouseX = mouseX;  // set it as the first value to be used inside draw() too
      dmouseY = mouseY;

      pmotionX = motionX;
      pmotionY = motionY;
      dmotionX = motionX;
      dmotionY = motionY;

      firstMotion = false;
    }

    // TODO implement method handling for registry of motion/mouse events
//    MouseEvent me = new MouseEvent(nativeObject, millis, action, modifiers, x, y, button, clickCount);
//    handleMethods("mouseEvent", new Object[] { mouseEvent });
//    handleMethods("motionEvent", new Object[] { motionEvent });

    if (ppointersX.length < numPointers) {
      ppointersX = new float[numPointers];
      ppointersY = new float[numPointers];
      ppointersPressure = new float[numPointers];
    }
    arrayCopy(pointersX, ppointersX);
    arrayCopy(pointersY, ppointersY);
    arrayCopy(pointersPressure, ppointersPressure);

    numPointers = pme.numPointers;
    if (pointersX.length < numPointers) {
      pointersX = new float[numPointers];
      pointersY = new float[numPointers];
      pointersPressure = new float[numPointers];
    }
    arrayCopy(pme.motionX, pointersX);
    arrayCopy(pme.motionY, pointersY);
    arrayCopy(pme.motionPressure, pointersPressure);

    // Triggering the appropriate event methods
    if (pme.action == MotionEvent.ACTION_DOWN || (!mousePressed && numPointers == 1)) {
      // First pointer is down
      mousePressed = true;
      onePointerGesture = true;
      twoPointerGesture = false;
      downMillis = millis();
      downX = pointersX[0];
      downY = pointersY[0];

      mousePressed();
      pressEvent();

    } else if ((pme.action == MotionEvent.ACTION_POINTER_DOWN  && numPointers == 2) ||
               (pme.action == MotionEvent.ACTION_POINTER_2_DOWN) || // 2.3 seems to use this action constant (supposedly deprecated) instead of ACTION_POINTER_DOWN
               (pnumPointers == 1 && numPointers == 2)) {           // 2.1 just uses MOVE as the action constant, so the only way to know we have a new pointer is to compare the counters.

      // An additional pointer is down (we keep track of multitouch only for 2 pointers)
      onePointerGesture = false;
      twoPointerGesture = true;

    } else if ((pme.action == MotionEvent.ACTION_POINTER_UP && numPointers == 2) ||
               (pme.action == MotionEvent.ACTION_POINTER_2_UP) || // 2.1 doesn't use the ACTION_POINTER_UP constant, but this one, apparently deprecated in newer versions of the SDK.
               (twoPointerGesture && numPointers < 2)) {          // Sometimes it seems that it doesn't generate the up event.
      // A previously detected pointer is up

      twoPointerGesture = false; // Not doing a 2-pointer gesture anymore, but neither a 1-pointer.

    } else if (pme.action == MotionEvent.ACTION_MOVE) {
      // Pointer motion

      if (onePointerGesture) {
        if (mousePressed) {
          mouseDragged();
          dragEvent();
        } else {  // TODO is this physically possible? (perhaps with alt input devices...)
          mouseMoved();
          moveEvent();
        }
      } else if (twoPointerGesture) {
        float d0 = PApplet.dist(ppointersX[0], ppointersY[0], ppointersX[1], ppointersY[1]);
        float d1 = PApplet.dist(pointersX[0], pointersY[0], pointersX[1], pointersY[1]);

        if (0 < d0 && 0 < d1) {
          float centerX = 0.5f * (pointersX[0] + pointersX[1]);
          float centerY = 0.5f * (pointersY[0] + pointersY[1]);

          zoomEvent(centerX, centerY, d0, d1);
        }
      }

    } else if (pme.action == MotionEvent.ACTION_UP) {
      // Final pointer is up
      mousePressed = false;

      float upX = pointersX[0];
      float upY = pointersY[0];
      float gestureLength = PApplet.dist(downX, downY, upX, upY);

      int upMillis = millis();
      int gestureTime = upMillis - downMillis;

      if (onePointerGesture) {
        // First, lets determine if this 1-pointer event is a tap
        boolean tap = gestureLength <= MAX_TAP_DISP && gestureTime <= MAX_TAP_DURATION;
        if (tap) {
          mouseClicked();
          tapEvent(downX, downY);
        } else if (MIN_SWIPE_LENGTH <= gestureLength && gestureTime <= MAX_SWIPE_DURATION) {
          mouseReleased();
          swipeEvent(downX, downY, upX, upY);
        } else {
          mouseReleased();
          releaseEvent();
        }

      } else {
        mouseReleased();
        releaseEvent();
      }
      onePointerGesture = twoPointerGesture = false;

    } else if (pme.action == MotionEvent.ACTION_CANCEL) {
      // Current gesture is canceled.
      onePointerGesture = twoPointerGesture = false;
      mousePressed = false;
      mouseReleased();
      releaseEvent();

    } else {
      //System.out.println("Unknown MotionEvent action: " + action);
    }

    pnumPointers = numPointers;

    if (pme.action == MotionEvent.ACTION_MOVE) {
      emotionX = motionX;
      emotionY = motionY;
      emouseX = mouseX;
      emouseY = mouseY;
    }
  }
  */


  // Added in API 11, so defined here: Constant Value: 4096 (0x00001000)
  static final int META_CTRL_ON = 4096;
  // Added in API 11, so defined here: 65536 (0x00010000)
  static final int META_META_ON = 65536;

  int motionPointerId;


  /**
   * Figure out how to process a mouse event. When loop() has been
   * called, the events will be queued up until drawing is complete.
   * If noLoop() has been called, then events will happen immediately.
   */
  protected void nativeMotionEvent(android.view.MotionEvent motionEvent) {
//    enqueueMotionEvent(event);
//
//    // this will be the last event in the list
//    AndroidTouchEvent pme = motionEventQueue[motionEventCount + historyCount];
//    pme.setAction(event.getAction());
//    pme.setNumPointers(event.getPointerCount());
//    pme.setPointers(event);
//
//    // historical events happen before the 'current' values
//    if (pme.action == MotionEvent.ACTION_MOVE && historyCount > 0) {
//      for (int i = 0; i < historyCount; i++) {
//        AndroidTouchEvent hist = motionEventQueue[motionEventCount++];
//        hist.setAction(event.getAction());
//        hist.setNumPointers(event.getPointerCount());
//        hist.setPointers(event, i);
//      }
//    }

    // ACTION_HOVER_ENTER and ACTION_HOVER_EXIT are passed into
    // onGenericMotionEvent(android.view.MotionEvent)
    // if we want to implement mouseEntered/Exited

    // http://developer.android.com/reference/android/view/MotionEvent.html
    // http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
    // http://www.techrepublic.com/blog/app-builder/use-androids-gesture-detector-to-translate-a-swipe-into-an-event/1577

    int metaState = motionEvent.getMetaState();
    int modifiers = 0;
    if ((metaState & android.view.KeyEvent.META_SHIFT_ON) != 0) {
      modifiers |= Event.SHIFT;
    }
    if ((metaState & META_CTRL_ON) != 0) {
      modifiers |= Event.CTRL;
    }
    if ((metaState & META_META_ON) != 0) {
      modifiers |= Event.META;
    }
    if ((metaState & android.view.KeyEvent.META_ALT_ON) != 0) {
      modifiers |= Event.ALT;
    }

    int clickCount = 1;  // not really set... (i.e. not catching double taps)
    int index;

    // MotionEvent.html -> getButtonState() does BUTTON_PRIMARY, SECONDARY, TERTIARY
    //   use this for left/right/etc
    switch (motionEvent.getAction()) {
    case MotionEvent.ACTION_DOWN:
      motionPointerId = motionEvent.getPointerId(0);
      postEvent(new MouseEvent(motionEvent, motionEvent.getEventTime(),
                               MouseEvent.PRESS, modifiers,
                               (int) motionEvent.getX(), (int) motionEvent.getY(),
                               LEFT, clickCount));
      break;
    case MotionEvent.ACTION_MOVE:
//      int historySize = motionEvent.getHistorySize();
      index = motionEvent.findPointerIndex(motionPointerId);
      if (index != -1) {
        postEvent(new MouseEvent(motionEvent, motionEvent.getEventTime(),
                                 MouseEvent.DRAG, modifiers,
                                 (int) motionEvent.getX(index), (int) motionEvent.getY(index),
                                 LEFT, clickCount));
      }
      break;
    case MotionEvent.ACTION_UP:
      index = motionEvent.findPointerIndex(motionPointerId);
      if (index != -1) {
        postEvent(new MouseEvent(motionEvent, motionEvent.getEventTime(),
                                 MouseEvent.RELEASE, modifiers,
                                 (int) motionEvent.getX(index), (int) motionEvent.getY(index),
                                 LEFT, clickCount));
      }
      break;
    }
    //postEvent(pme);
  }


  public void mousePressed() { }

  public void mouseReleased() { }

  public void mouseClicked() { }

  public void mouseDragged() { }

  public void mouseMoved() { }

  public void mouseEntered() { }

  public void mouseExited() { }



  //////////////////////////////////////////////////////////////

  // unfinished API, do not use


  protected void pressEvent() { }

  protected void dragEvent() { }

  protected void moveEvent() { }

  protected void releaseEvent() { }

  protected void zoomEvent(float x, float y, float d0, float d1) { }

  protected void tapEvent(float x, float y) { }

  protected void swipeEvent(float x0, float y0, float x1, float y1) { }


  //////////////////////////////////////////////////////////////


//  KeyEvent[] keyEventQueue = new KeyEvent[10];
//  int keyEventCount;
//
//  protected void enqueueKeyEvent(KeyEvent e) {
//    synchronized (keyEventQueue) {
//      if (keyEventCount == keyEventQueue.length) {
//        keyEventQueue = (KeyEvent[]) expand(keyEventQueue);
//      }
//      keyEventQueue[keyEventCount++] = e;
//    }
//  }
//
//  protected void dequeueKeyEvents() {
//    synchronized (keyEventQueue) {
//      for (int i = 0; i < keyEventCount; i++) {
//        handleKeyEvent(keyEventQueue[i]);
//      }
//      keyEventCount = 0;
//    }
//  }


  protected void handleKeyEvent(KeyEvent event) {
    keyEvent = event;
    key = event.getKey();
    keyCode = event.getKeyCode();

    switch (event.getAction()) {
    case KeyEvent.PRESS:
      keyPressed = true;
      keyPressed();
      break;
    case KeyEvent.RELEASE:
      keyPressed = false;
      keyReleased();
      break;
    }

    handleMethods("keyEvent", new Object[] { event });

    // if someone else wants to intercept the key, they should
    // set key to zero (or something besides the "ESC").
    if (event.getAction() == KeyEvent.PRESS &&
        event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
      exit();
    }
  }


  protected void nativeKeyEvent(android.view.KeyEvent event) {
    // event.isPrintingKey() returns false for whitespace and others,
    // which is a problem if the space bar or tab key are used.
    char key = (char) event.getUnicodeChar();
    // if not mappable to a unicode character, instead mark as coded key
    if (key == 0 || key == 0xFFFF) {
      key = CODED;
    }

    int keyCode = event.getKeyCode();

    int keAction = 0;
    int action = event.getAction();
    if (action == android.view.KeyEvent.ACTION_DOWN) {
      keAction = KeyEvent.PRESS;
    } else if (action == android.view.KeyEvent.ACTION_UP) {
      keAction = KeyEvent.RELEASE;
    }

    // TODO set up proper key modifier handling
    int keModifiers = 0;

    KeyEvent ke = new KeyEvent(event, event.getEventTime(),
                               keAction, keModifiers, key, keyCode);

    // if someone else wants to intercept the key, they should
    // set key to zero (or something besides the "ESC").
    if (action == android.view.KeyEvent.ACTION_DOWN) {
      if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
        exit();
      }
    }

    postEvent(ke);
  }


  /**
   * Overriding keyXxxxx(KeyEvent e) functions will cause the 'key',
   * 'keyCode', and 'keyEvent' variables to no longer work;
   * key events will no longer be queued until the end of draw();
   * and the keyPressed(), keyReleased() and keyTyped() methods
   * will no longer be called.
   */
//  public void keyPressed(KeyEvent e) { checkKeyEvent(e); }
//  public void keyReleased(KeyEvent e) { checkKeyEvent(e); }
//  public void keyTyped(KeyEvent e) { checkKeyEvent(e); }


  /**
   * Called each time a single key on the keyboard is pressed.
   * Because of how operating systems handle key repeats, holding
   * down a key will cause multiple calls to keyPressed(), because
   * the OS repeat takes over.
   * <P>
   * Examples for key handling:
   * (Tested on Windows XP, please notify if different on other
   * platforms, I have a feeling Mac OS and Linux may do otherwise)
   * <PRE>
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
   * </PRE>
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


  //////////////////////////////////////////////////////////////


  public void focusGained() { }

//  public void focusGained(FocusEvent e) {
//    focused = true;
//    focusGained();
//  }


  public void focusLost() { }

//  public void focusLost(FocusEvent e) {
//    focused = false;
//    focusLost();
//  }


  //////////////////////////////////////////////////////////////

  // getting the time


  static protected Time time = new Time();

  /**
   * Get the number of milliseconds since the applet started.
   * <P>
   * This is a function, rather than a variable, because it may
   * change multiple times per frame.
   */
  public int millis() {
    return (int) (System.currentTimeMillis() - millisOffset);
  }

  /** Seconds position of the current time. */
  static public int second() {
    //return Calendar.getInstance().get(Calendar.SECOND);
    time.setToNow();
    return time.second;
  }

  /** Minutes position of the current time. */
  static public int minute() {
    //return Calendar.getInstance().get(Calendar.MINUTE);
    time.setToNow();
    return time.minute;
  }

  /**
   * Hour position of the current time in international format (0-23).
   * <P>
   * To convert this value to American time: <BR>
   * <PRE>int yankeeHour = (hour() % 12);
   * if (yankeeHour == 0) yankeeHour = 12;</PRE>
   */
  static public int hour() {
    //return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    time.setToNow();
    return time.hour;
  }

  /**
   * Get the current day of the month (1 through 31).
   * <P>
   * If you're looking for the day of the week (M-F or whatever)
   * or day of the year (1..365) then use java's Calendar.get()
   */
  static public int day() {
    //return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    time.setToNow();
    return time.monthDay;
  }

  /**
   * Get the current month in range 1 through 12.
   */
  static public int month() {
    // months are number 0..11 so change to colloquial 1..12
    //return Calendar.getInstance().get(Calendar.MONTH) + 1;
    time.setToNow();
    return time.month + 1;
  }

  /**
   * Get the current year.
   */
  static public int year() {
    //return Calendar.getInstance().get(Calendar.YEAR);
    time.setToNow();
    return time.year;
  }


  //////////////////////////////////////////////////////////////

  // controlling time (playing god)


  /**
   * The delay() function causes the program to halt for a specified time.
   * Delay times are specified in thousandths of a second. For example,
   * running delay(3000) will stop the program for three seconds and
   * delay(500) will stop the program for a half-second.
   *
   * The screen only updates when the end of draw() is reached, so delay()
   * cannot be used to slow down drawing. For instance, you cannot use delay()
   * to control the timing of an animation.
   *
   * The delay() function should only be used for pausing scripts (i.e.
   * a script that needs to pause a few seconds before attempting a download,
   * or a sketch that needs to wait a few milliseconds before reading from
   * the serial port).
   */
  public void delay(int napTime) {
    //if (frameCount != 0) {
    //if (napTime > 0) {
    try {
      Thread.sleep(napTime);
    } catch (InterruptedException e) { }
    //}
    //}
  }


  /**
   * ( begin auto-generated from frameRate.xml )
   *
   * Specifies the number of frames to be displayed every second. If the
   * processor is not fast enough to maintain the specified rate, it will not
   * be achieved. For example, the function call <b>frameRate(30)</b> will
   * attempt to refresh 30 times a second. It is recommended to set the frame
   * rate within <b>setup()</b>. The default rate is 60 frames per second.
   *
   * ( end auto-generated )
   */
  public void frameRate(float newRateTarget) {
    frameRateTarget = newRateTarget;
    frameRatePeriod = (long) (1000000000.0 / frameRateTarget);
    g.setFrameRate(newRateTarget);
  }


  //////////////////////////////////////////////////////////////


  /**
   * Get a param from the web page, or (eventually)
   * from a properties file.
   */
//  public String param(String what) {
//    if (online) {
//      return getParameter(what);
//
//    } else {
//      System.err.println("param() only works inside a web browser");
//    }
//    return null;
//  }


  /**
   * Show status in the status bar of a web browser, or in the
   * System.out console. Eventually this might show status in the
   * p5 environment itself, rather than relying on the console.
   */
//  public void status(String what) {
//    if (online) {
//      showStatus(what);
//
//    } else {
//      System.out.println(what);  // something more interesting?
//    }
//  }


  public void link(String here) {
    link(here, null);
  }


  /**
   * Link to an external page without all the muss.
   * <P>
   * When run with an applet, uses the browser to open the url,
   * for applications, attempts to launch a browser with the url.
   * <P>
   * Works on Mac OS X and Windows. For Linux, use:
   * <PRE>open(new String[] { "firefox", url });</PRE>
   * or whatever you want as your browser, since Linux doesn't
   * yet have a standard method for launching URLs.
   */
  public void link(String url, String frameTitle) {
    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
    startActivity(viewIntent);
  }


  /**
   * Attempt to open a file using the platform's shell.
   */
  static public void open(String filename) {
    open(new String[] { filename });
  }


  /**
   * Launch a process using a platforms shell. This version uses an array
   * to make it easier to deal with spaces in the individual elements.
   * (This avoids the situation of trying to put single or double quotes
   * around different bits).
   */
  static public Process open(String argv[]) {
    return exec(argv);
  }


  static public Process exec(String[] argv) {
    try {
      return Runtime.getRuntime().exec(argv);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Could not open " + join(argv, ' '));
    }
  }


  //////////////////////////////////////////////////////////////


  /**
   * Function for an applet/application to kill itself and
   * display an error. Mostly this is here to be improved later.
   */
  public void die(String what) {
    stop();
    throw new RuntimeException(what);
  }


  /**
   * Same as above but with an exception. Also needs work.
   */
  public void die(String what, Exception e) {
    if (e != null) e.printStackTrace();
    die(what);
  }


  /**
   * Call to safely exit the sketch when finished. For instance,
   * to render a single frame, save it, and quit.
   */
  public void exit() {
//    println("exit() called");
    if (thread == null) {
      // exit immediately, stop() has already been called,
      // meaning that the main thread has long since exited
      exit2();

    } else if (looping) {
      // stop() will be called as the thread exits
      finished = true;
      // tell the code to call exit2() to do a System.exit()
      // once the next draw() has completed
      exitCalled = true;

    } else if (!looping) {
      // if not looping, shut down things explicitly,
      // because the main thread will be sleeping
      dispose();

      // now get out
      exit2();
    }
  }


  void exit2() {
    try {
      System.exit(0);
    } catch (SecurityException e) {
      // don't care about applet security exceptions
    }
  }

  /**
   * Called to dispose of resources and shut down the sketch.
   * Destroys the thread, dispose the renderer, and notify listeners.
   * <p>
   * Not to be called or overriden by users. If called multiple times,
   * will only notify listeners once. Register a dispose listener instead.
   */
  final public void dispose() {
    // moved here from stop()
    finished = true;  // let the sketch know it is shut down time

    // don't run stop and disposers twice
    if (thread == null) return;
    thread = null;

    // call to shut down renderer, in case it needs it (pdf does)
    if (g != null) g.dispose();

    handleMethods("dispose");
  }



  //////////////////////////////////////////////////////////////


  /**
   * Call a method in the current class based on its name.
   * <p/>
   * Note that the function being called must be public. Inside the PDE,
   * 'public' is automatically added, but when used without the preprocessor,
   * (like from Eclipse) you'll have to do it yourself.
   */
  public void method(String name) {
    try {
      Method method = getClass().getMethod(name, new Class[] {});
      method.invoke(this, new Object[] { });

    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.getTargetException().printStackTrace();
    } catch (NoSuchMethodException nsme) {
      System.err.println("There is no public " + name + "() method " +
                         "in the class " + getClass().getName());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Launch a new thread and call the specified function from that new thread.
   * This is a very simple way to do a thread without needing to get into
   * classes, runnables, etc.
   * <p/>
   * Note that the function being called must be public. Inside the PDE,
   * 'public' is automatically added, but when used without the preprocessor,
   * (like from Eclipse) you'll have to do it yourself.
   */
  public void thread(final String name) {
    Thread later = new Thread() {
      @Override
      public void run() {
        method(name);
      }
    };
    later.start();
  }



  //////////////////////////////////////////////////////////////

  // SCREEN GRABASS


  /**
   * Intercepts any relative paths to make them absolute (relative
   * to the sketch folder) before passing to save() in PImage.
   * (Changed in 0100)
   */
  public void save(String filename) {
    g.save(savePath(filename));
  }


  /**
   * Grab an image of what's currently in the drawing area and save it
   * as a .tif or .tga file.
   * <P>
   * Best used just before endDraw() at the end of your draw().
   * This can only create .tif or .tga images, so if neither extension
   * is specified it defaults to writing a tiff and adds a .tif suffix.
   */
  public void saveFrame() {
    try {
      g.save(savePath("screen-" + nf(frameCount, 4) + ".tif"));
    } catch (SecurityException se) {
      System.err.println("Can't use saveFrame() when running in a browser, " +
                         "unless using a signed applet.");
    }
  }


  /**
   * Save the current frame as a .tif or .tga image.
   * <P>
   * The String passed in can contain a series of # signs
   * that will be replaced with the screengrab number.
   * <PRE>
   * i.e. saveFrame("blah-####.tif");
   *      // saves a numbered tiff image, replacing the
   *      // #### signs with zeros and the frame number </PRE>
   */
  public void saveFrame(String what) {
    try {
      g.save(savePath(insertFrame(what)));
    } catch (SecurityException se) {
      System.err.println("Can't use saveFrame() when running in a browser, " +
                         "unless using a signed applet.");
    }
  }


  /**
   * Check a string for #### signs to see if the frame number should be
   * inserted. Used for functions like saveFrame() and beginRecord() to
   * replace the # marks with the frame number. If only one # is used,
   * it will be ignored, under the assumption that it's probably not
   * intended to be the frame number.
   */
  protected String insertFrame(String what) {
    int first = what.indexOf('#');
    int last = what.lastIndexOf('#');

    if ((first != -1) && (last - first > 0)) {
      String prefix = what.substring(0, first);
      int count = last - first + 1;
      String suffix = what.substring(last + 1);
      return prefix + nf(frameCount, count) + suffix;
    }
    return what;  // no change
  }



  //////////////////////////////////////////////////////////////

  // CURSOR

  // Removed, this doesn't make sense in a touch interface.


//  int cursorType = ARROW; // cursor type
//  boolean cursorVisible = true; // cursor visibility flag
//  PImage invisibleCursor;


  /**
   * Set the cursor type
   */
//  public void cursor(int cursorType) {
//    setCursor(Cursor.getPredefinedCursor(cursorType));
//    cursorVisible = true;
//    this.cursorType = cursorType;
//  }


  /**
   * Replace the cursor with the specified PImage. The x- and y-
   * coordinate of the center will be the center of the image.
   */
//  public void cursor(PImage image) {
//    cursor(image, image.width/2, image.height/2);
//  }


  /**
   * Set a custom cursor to an image with a specific hotspot.
   * Only works with JDK 1.2 and later.
   * Currently seems to be broken on Java 1.4 for Mac OS X
   * <P>
   * Based on code contributed by Amit Pitaru, plus additional
   * code to handle Java versions via reflection by Jonathan Feinberg.
   * Reflection removed for release 0128 and later.
   */
//  public void cursor(PImage image, int hotspotX, int hotspotY) {
//    // don't set this as cursor type, instead use cursor_type
//    // to save the last cursor used in case cursor() is called
//    //cursor_type = Cursor.CUSTOM_CURSOR;
//    Image jimage =
//      createImage(new MemoryImageSource(image.width, image.height,
//                                        image.pixels, 0, image.width));
//    Point hotspot = new Point(hotspotX, hotspotY);
//    Toolkit tk = Toolkit.getDefaultToolkit();
//    Cursor cursor = tk.createCustomCursor(jimage, hotspot, "Custom Cursor");
//    setCursor(cursor);
//    cursorVisible = true;
//  }


  /**
   * Show the cursor after noCursor() was called.
   * Notice that the program remembers the last set cursor type
   */
//  public void cursor() {
//    // maybe should always set here? seems dangerous, since
//    // it's likely that java will set the cursor to something
//    // else on its own, and the applet will be stuck b/c bagel
//    // thinks that the cursor is set to one particular thing
//    if (!cursorVisible) {
//      cursorVisible = true;
//      setCursor(Cursor.getPredefinedCursor(cursorType));
//    }
//  }


  /**
   * Hide the cursor by creating a transparent image
   * and using it as a custom cursor.
   */
//  public void noCursor() {
//    if (!cursorVisible) return;  // don't hide if already hidden.
//
//    if (invisibleCursor == null) {
//      invisibleCursor = new PImage(16, 16, ARGB);
//    }
//    // was formerly 16x16, but the 0x0 was added by jdf as a fix
//    // for macosx, which wasn't honoring the invisible cursor
//    cursor(invisibleCursor, 8, 8);
//    cursorVisible = false;
//  }


  //////////////////////////////////////////////////////////////


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

  static public void print(String what) {
    System.out.print(what);
    System.out.flush();
  }

  static public void print(Object what) {
    if (what == null) {
      // special case since this does fuggly things on > 1.1
      System.out.print("null");
    } else {
      System.out.println(what.toString());
    }
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

  static public void println(String what) {
    print(what); System.out.println();
  }

  static public void println(Object what) {
    if (what == null) {
      // special case since this does fuggly things on > 1.1
      System.out.println("null");

    } else {
      String name = what.getClass().getName();
      if (name.charAt(0) == '[') {
        switch (name.charAt(1)) {
        case '[':
          // don't even mess with multi-dimensional arrays (case '[')
          // or anything else that's not int, float, boolean, char
          System.out.println(what);
          break;

        case 'L':
          // print a 1D array of objects as individual elements
          Object poo[] = (Object[]) what;
          for (int i = 0; i < poo.length; i++) {
            if (poo[i] instanceof String) {
              System.out.println("[" + i + "] \"" + poo[i] + "\"");
            } else {
              System.out.println("[" + i + "] " + poo[i]);
            }
          }
          break;

        case 'Z':  // boolean
          boolean zz[] = (boolean[]) what;
          for (int i = 0; i < zz.length; i++) {
            System.out.println("[" + i + "] " + zz[i]);
          }
          break;

        case 'B':  // byte
          byte bb[] = (byte[]) what;
          for (int i = 0; i < bb.length; i++) {
            System.out.println("[" + i + "] " + bb[i]);
          }
          break;

        case 'C':  // char
          char cc[] = (char[]) what;
          for (int i = 0; i < cc.length; i++) {
            System.out.println("[" + i + "] '" + cc[i] + "'");
          }
          break;

        case 'I':  // int
          int ii[] = (int[]) what;
          for (int i = 0; i < ii.length; i++) {
            System.out.println("[" + i + "] " + ii[i]);
          }
          break;

        case 'F':  // float
          float ff[] = (float[]) what;
          for (int i = 0; i < ff.length; i++) {
            System.out.println("[" + i + "] " + ff[i]);
          }
          break;

          /*
        case 'D':  // double
          double dd[] = (double[]) what;
          for (int i = 0; i < dd.length; i++) {
            System.out.println("[" + i + "] " + dd[i]);
          }
          break;
          */

        default:
          System.out.println(what);
        }
      } else {  // not an array
        System.out.println(what);
      }
    }
  }

  //

  /*
  // not very useful, because it only works for public (and protected?)
  // fields of a class, not local variables to methods
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


  static public final int max(int a, int b) {
    return (a > b) ? a : b;
  }

  static public final float max(float a, float b) {
    return (a > b) ? a : b;
  }


  static public final int max(int a, int b, int c) {
    return (a > b) ? ((a > c) ? a : c) : ((b > c) ? b : c);
  }

  static public final float max(float a, float b, float c) {
    return (a > b) ? ((a > c) ? a : c) : ((b > c) ? b : c);
  }


  /**
   * Find the maximum value in an array.
   * Throws an ArrayIndexOutOfBoundsException if the array is length 0.
   * @param list the source array
   * @return The maximum value
   */
  static public final int max(int[] list) {
    if (list.length == 0) {
      throw new ArrayIndexOutOfBoundsException(ERROR_MIN_MAX);
    }
    int max = list[0];
    for (int i = 1; i < list.length; i++) {
      if (list[i] > max) max = list[i];
    }
    return max;
  }

  /**
   * Find the maximum value in an array.
   * Throws an ArrayIndexOutOfBoundsException if the array is length 0.
   * @param list the source array
   * @return The maximum value
   */
  static public final float max(float[] list) {
    if (list.length == 0) {
      throw new ArrayIndexOutOfBoundsException(ERROR_MIN_MAX);
    }
    float max = list[0];
    for (int i = 1; i < list.length; i++) {
      if (list[i] > max) max = list[i];
    }
    return max;
  }


  static public final int min(int a, int b) {
    return (a < b) ? a : b;
  }

  static public final float min(float a, float b) {
    return (a < b) ? a : b;
  }


  static public final int min(int a, int b, int c) {
    return (a < b) ? ((a < c) ? a : c) : ((b < c) ? b : c);
  }

  static public final float min(float a, float b, float c) {
    return (a < b) ? ((a < c) ? a : c) : ((b < c) ? b : c);
  }


  /**
   * Find the minimum value in an array.
   * Throws an ArrayIndexOutOfBoundsException if the array is length 0.
   * @param list the source array
   * @return The minimum value
   */
  static public final int min(int[] list) {
    if (list.length == 0) {
      throw new ArrayIndexOutOfBoundsException(ERROR_MIN_MAX);
    }
    int min = list[0];
    for (int i = 1; i < list.length; i++) {
      if (list[i] < min) min = list[i];
    }
    return min;
  }
  /**
   * Find the minimum value in an array.
   * Throws an ArrayIndexOutOfBoundsException if the array is length 0.
   * @param list the source array
   * @return The minimum value
   */
  static public final float min(float[] list) {
    if (list.length == 0) {
      throw new ArrayIndexOutOfBoundsException(ERROR_MIN_MAX);
    }
    float min = list[0];
    for (int i = 1; i < list.length; i++) {
      if (list[i] < min) min = list[i];
    }
    return min;
  }


  static public final int constrain(int amt, int low, int high) {
    return (amt < low) ? low : ((amt > high) ? high : amt);
  }

  static public final float constrain(float amt, float low, float high) {
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


  static public final float asin(float value) {
    return (float)Math.asin(value);
  }

  static public final float acos(float value) {
    return (float)Math.acos(value);
  }

  static public final float atan(float value) {
    return (float)Math.atan(value);
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


  static public final int ceil(float what) {
    return (int) Math.ceil(what);
  }

  static public final int floor(float what) {
    return (int) Math.floor(what);
  }

  static public final int round(float what) {
    return (int) Math.round(what);
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


  static public final float lerp(float start, float stop, float amt) {
    return start + (stop-start) * amt;
  }

  /**
   * Normalize a value to exist between 0 and 1 (inclusive).
   * Mathematically the opposite of lerp(), figures out what proportion
   * a particular value is relative to start and stop coordinates.
   */
  static public final float norm(float value, float start, float stop) {
    return (value - start) / (stop - start);
  }

  /**
   * Convenience function to map a variable from one coordinate space
   * to another. Equivalent to unlerp() followed by lerp().
   */
  static public final float map(float value,
                                float istart, float istop,
                                float ostart, float ostop) {
    return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
  }



  //////////////////////////////////////////////////////////////

  // RANDOM NUMBERS


  Random internalRandom;

  /**
   * Return a random number in the range [0, howbig).
   * <P>
   * The number returned will range from zero up to
   * (but not including) 'howbig'.
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
   * Return a random number in the range [howsmall, howbig).
   * <P>
   * The number returned will range from 'howsmall' up to
   * (but not including 'howbig'.
   * <P>
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
  float[] perlin;

  Random perlinRandom;


  /**
   * Computes the Perlin noise function value at point x.
   */
  public float noise(float x) {
    // is this legit? it's a dumb way to do it (but repair it later)
    return noise(x, 0f, 0f);
  }

  /**
   * Computes the Perlin noise function value at the point x, y.
   */
  public float noise(float x, float y) {
    return noise(x, y, 0f);
  }

  /**
   * Computes the Perlin noise function value at x, y, z.
   */
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
    // force table reset after changing the random number seed [0122]
    perlin = null;
  }



  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  protected String[] loadImageFormats;


//  public PImage loadImage(String filename) {
//    return loadImage(filename, null);
//  }


  public PImage loadImage(String filename) { //, Object params) {
//    return loadImage(filename, null);
    InputStream stream = createInput(filename);
    if (stream == null) {
      System.err.println("Could not find the image " + filename + ".");
      return null;
    }
//    long t = System.currentTimeMillis();
    Bitmap bitmap = null;
    try {
      bitmap = BitmapFactory.decodeStream(stream);
    } finally {
      try {
        stream.close();
        stream = null;
      } catch (IOException e) { }
    }
//    int much = (int) (System.currentTimeMillis() - t);
//    println("loadImage(" + filename + ") was " + nfc(much));
    PImage image = new PImage(bitmap);
    image.parent = this;
//    if (params != null) {
//      image.setParams(g, params);
//    }
    return image;
  }


  /*
  public PImage loadImage(String filename, String extension) {
    if (extension == null) {
      String lower = filename.toLowerCase();
      int dot = filename.lastIndexOf('.');
      if (dot == -1) {
        extension = "unknown";  // no extension found
      }
      extension = lower.substring(dot + 1);

      // check for, and strip any parameters on the url, i.e.
      // filename.jpg?blah=blah&something=that
      int question = extension.indexOf('?');
      if (question != -1) {
        extension = extension.substring(0, question);
      }
    }

    // just in case. them users will try anything!
    extension = extension.toLowerCase();

    if (extension.equals("tga")) {
      try {
        return loadImageTGA(filename);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    if (extension.equals("tif") || extension.equals("tiff")) {
      byte bytes[] = loadBytes(filename);
      return (bytes == null) ? null : PImage.loadTIFF(bytes);
    }

    // For jpeg, gif, and png, load them using createImage(),
    // because the javax.imageio code was found to be much slower, see
    // <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=392">Bug 392</A>.
    try {
      if (extension.equals("jpg") || extension.equals("jpeg") ||
          extension.equals("gif") || extension.equals("png") ||
          extension.equals("unknown")) {
        byte bytes[] = loadBytes(filename);
        if (bytes == null) {
          return null;
        } else {
          Image awtImage = Toolkit.getDefaultToolkit().createImage(bytes);
          PImage image = loadImageMT(awtImage);
          if (image.width == -1) {
            System.err.println("The file " + filename +
                               " contains bad image data, or may not be an image.");
          }
          // if it's a .gif image, test to see if it has transparency
          if (extension.equals("gif") || extension.equals("png")) {
            image.checkAlpha();
          }
          return image;
        }
      }
    } catch (Exception e) {
      // show error, but move on to the stuff below, see if it'll work
      e.printStackTrace();
    }

    if (loadImageFormats == null) {
      loadImageFormats = ImageIO.getReaderFormatNames();
    }
    if (loadImageFormats != null) {
      for (int i = 0; i < loadImageFormats.length; i++) {
        if (extension.equals(loadImageFormats[i])) {
          return loadImageIO(filename);
        }
      }
    }

    // failed, could not load image after all those attempts
    System.err.println("Could not find a method to load " + filename);
    return null;
  }
  */


  public PImage requestImage(String filename) {
    PImage vessel = createImage(0, 0, ARGB);
    AsyncImageLoader ail = new AsyncImageLoader(filename, vessel);
    ail.start();
    return vessel;
  }


  /**
   * By trial and error, four image loading threads seem to work best when
   * loading images from online. This is consistent with the number of open
   * connections that web browsers will maintain. The variable is made public
   * (however no accessor has been added since it's esoteric) if you really
   * want to have control over the value used. For instance, when loading local
   * files, it might be better to only have a single thread (or two) loading
   * images so that you're disk isn't simply jumping around.
   */
  public int requestImageMax = 4;
  volatile int requestImageCount;

  // Removed 'extension' from the android version. If the extension is needed
  // later, re-copy this from the original PApplet code.
  class AsyncImageLoader extends Thread {
    String filename;
    PImage vessel;

    public AsyncImageLoader(String filename, PImage vessel) {
      this.filename = filename;
      this.vessel = vessel;
    }

    @Override
    public void run() {
      while (requestImageCount == requestImageMax) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) { }
      }
      requestImageCount++;

      PImage actual = loadImage(filename);

      // An error message should have already printed
      if (actual == null) {
        vessel.width = -1;
        vessel.height = -1;

      } else {
        vessel.width = actual.width;
        vessel.height = actual.height;
        vessel.format = actual.format;
        vessel.pixels = actual.pixels;
        // an android, pixels[] will probably be null, we want this one
        vessel.bitmap = actual.bitmap;
      }
      requestImageCount--;
    }
  }



  //////////////////////////////////////////////////////////////

  // DATA I/O


  public XML loadXML(String filename) {
    try {
      return new XML(this, filename);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  static public XML loadXML(File file) {
    try {
      return new XML(file);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  public Table loadTable(String filename) {
    return new Table(this, filename);
  }


  static public Table loadTable(File file) {
    return new Table(file);
  }


  //////////////////////////////////////////////////////////////

  // FONT I/O


  public PFont loadFont(String filename) {
    try {
      InputStream input = createInput(filename);
      return new PFont(input);

    } catch (Exception e) {
      die("Could not load font " + filename + ". " +
          "Make sure that the font has been copied " +
          "to the data folder of your sketch.", e);
    }
    return null;
  }


  /**
   * Used by PGraphics to remove the requirement for loading a font!
   */
  protected PFont createDefaultFont(float size) {
    return createFont("SansSerif", size, true, null);
  }


  public PFont createFont(String name, float size) {
    return createFont(name, size, true, null);
  }


  public PFont createFont(String name, float size, boolean smooth) {
    return createFont(name, size, smooth, null);
  }


  /**
   * Create a bitmap font on the fly from either a font name that's
   * installed on the system, or from a .ttf or .otf that's inside
   * the data folder of this sketch.
   * <P/>
   * Use 'null' for the charset if you want to dynamically create
   * character bitmaps only as they're needed.
   */
  public PFont createFont(String name, float size,
                          boolean smooth, char[] charset) {
    String lowerName = name.toLowerCase();
    Typeface baseFont = null;

    if (lowerName.endsWith(".otf") || lowerName.endsWith(".ttf")) {
      AssetManager assets = getBaseContext().getAssets();
      baseFont = Typeface.createFromAsset(assets, name);
    } else {
      baseFont = (Typeface) PFont.findNative(name);
    }
    return new PFont(baseFont, round(size), smooth, charset);
  }


  //////////////////////////////////////////////////////////////

  // FILE/FOLDER SELECTION

  // Doesn't appear to be implemented by Android, but this article might help:
  // http://linuxdevices.com/articles/AT6247038002.html

//  public File selectedFile;
//  protected Frame parentFrame;
//
//
//  protected void checkParentFrame() {
//    if (parentFrame == null) {
//      Component comp = getParent();
//      while (comp != null) {
//        if (comp instanceof Frame) {
//          parentFrame = (Frame) comp;
//          break;
//        }
//        comp = comp.getParent();
//      }
//      // Who you callin' a hack?
//      if (parentFrame == null) {
//        parentFrame = new Frame();
//      }
//    }
//  }
//
//
//  /**
//   * Open a platform-specific file chooser dialog to select a file for input.
//   * @return full path to the selected file, or null if no selection.
//   */
//  public String selectInput() {
//    return selectInput("Select a file...");
//  }
//
//
//  /**
//   * Open a platform-specific file chooser dialog to select a file for input.
//   * @param prompt Mesage to show the user when prompting for a file.
//   * @return full path to the selected file, or null if canceled.
//   */
//  public String selectInput(String prompt) {
//    return selectFileImpl(prompt, FileDialog.LOAD);
//  }
//
//
//  /**
//   * Open a platform-specific file save dialog to select a file for output.
//   * @return full path to the file entered, or null if canceled.
//   */
//  public String selectOutput() {
//    return selectOutput("Save as...");
//  }
//
//
//  /**
//   * Open a platform-specific file save dialog to select a file for output.
//   * @param prompt Mesage to show the user when prompting for a file.
//   * @return full path to the file entered, or null if canceled.
//   */
//  public String selectOutput(String prompt) {
//    return selectFileImpl(prompt, FileDialog.SAVE);
//  }
//
//
//  protected String selectFileImpl(final String prompt, final int mode) {
//    checkParentFrame();
//
//    try {
//      SwingUtilities.invokeAndWait(new Runnable() {
//        public void run() {
//          FileDialog fileDialog =
//            new FileDialog(parentFrame, prompt, mode);
//          fileDialog.setVisible(true);
//          String directory = fileDialog.getDirectory();
//          String filename = fileDialog.getFile();
//          selectedFile =
//            (filename == null) ? null : new File(directory, filename);
//        }
//      });
//      return (selectedFile == null) ? null : selectedFile.getAbsolutePath();
//
//    } catch (Exception e) {
//      e.printStackTrace();
//      return null;
//    }
//  }
//
//
//  /**
//   * Open a platform-specific folder chooser dialog.
//   * @return full path to the selected folder, or null if no selection.
//   */
//  public String selectFolder() {
//    return selectFolder("Select a folder...");
//  }
//
//
//  /**
//   * Open a platform-specific folder chooser dialog.
//   * @param prompt Mesage to show the user when prompting for a file.
//   * @return full path to the selected folder, or null if no selection.
//   */
//  public String selectFolder(final String prompt) {
//    checkParentFrame();
//
//    try {
//      SwingUtilities.invokeAndWait(new Runnable() {
//        public void run() {
//          if (platform == MACOSX) {
//            FileDialog fileDialog =
//              new FileDialog(parentFrame, prompt, FileDialog.LOAD);
//            System.setProperty("apple.awt.fileDialogForDirectories", "true");
//            fileDialog.setVisible(true);
//            System.setProperty("apple.awt.fileDialogForDirectories", "false");
//            String filename = fileDialog.getFile();
//            selectedFile = (filename == null) ? null :
//              new File(fileDialog.getDirectory(), fileDialog.getFile());
//          } else {
//            JFileChooser fileChooser = new JFileChooser();
//            fileChooser.setDialogTitle(prompt);
//            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//
//            int returned = fileChooser.showOpenDialog(parentFrame);
//            System.out.println(returned);
//            if (returned == JFileChooser.CANCEL_OPTION) {
//              selectedFile = null;
//            } else {
//              selectedFile = fileChooser.getSelectedFile();
//            }
//          }
//        }
//      });
//      return (selectedFile == null) ? null : selectedFile.getAbsolutePath();
//
//    } catch (Exception e) {
//      e.printStackTrace();
//      return null;
//    }
//  }



  //////////////////////////////////////////////////////////////

  // READERS AND WRITERS


  /**
   * I want to read lines from a file. I have RSI from typing these
   * eight lines of code so many times.
   */
  public BufferedReader createReader(String filename) {
    try {
      InputStream is = createInput(filename);
      if (is == null) {
        System.err.println(filename + " does not exist or could not be read");
        return null;
      }
      return createReader(is);

    } catch (Exception e) {
      if (filename == null) {
        System.err.println("Filename passed to reader() was null");
      } else {
        System.err.println("Couldn't create a reader for " + filename);
      }
    }
    return null;
  }


  /**
   * I want to read lines from a file. And I'm still annoyed.
   */
  static public BufferedReader createReader(File file) {
    try {
      InputStream is = new FileInputStream(file);
      if (file.getName().toLowerCase().endsWith(".gz")) {
        is = new GZIPInputStream(is);
      }
      return createReader(is);

    } catch (Exception e) {
      if (file == null) {
        throw new RuntimeException("File passed to createReader() was null");
      } else {
        e.printStackTrace();
        throw new RuntimeException("Couldn't create a reader for " +
                                   file.getAbsolutePath());
      }
    }
    //return null;
  }


  /**
   * I want to read lines from a stream. If I have to type the
   * following lines any more I'm gonna send Sun my medical bills.
   */
  static public BufferedReader createReader(InputStream input) {
    InputStreamReader isr = null;
    try {
      isr = new InputStreamReader(input, "UTF-8");
    } catch (UnsupportedEncodingException e) { }  // not gonna happen
    return new BufferedReader(isr);
  }


  /**
   * I want to print lines to a file. Why can't I?
   */
  public PrintWriter createWriter(String filename) {
    return createWriter(saveFile(filename));
  }


  /**
   * I want to print lines to a file. I have RSI from typing these
   * eight lines of code so many times.
   */
  static public PrintWriter createWriter(File file) {
    try {
      OutputStream output = new FileOutputStream(file);
      if (file.getName().toLowerCase().endsWith(".gz")) {
        output = new GZIPOutputStream(output);
      }
      return createWriter(output);

    } catch (Exception e) {
      if (file == null) {
        throw new RuntimeException("File passed to createWriter() was null");
      } else {
        e.printStackTrace();
        throw new RuntimeException("Couldn't create a writer for " +
                                   file.getAbsolutePath());
      }
    }
    //return null;
  }


  /**
   * I want to print lines to a file. Why am I always explaining myself?
   * It's the JavaSoft API engineers who need to explain themselves.
   */
  static public PrintWriter createWriter(OutputStream output) {
    try {
      BufferedOutputStream bos = new BufferedOutputStream(output, 8192);
      OutputStreamWriter osw = new OutputStreamWriter(bos, "UTF-8");
      return new PrintWriter(osw);
    } catch (UnsupportedEncodingException e) { }  // not gonna happen
    return null;
  }


  //////////////////////////////////////////////////////////////

  // FILE INPUT


  /**
   * Simplified method to open a Java InputStream.
   * <P>
   * This method is useful if you want to use the facilities provided
   * by PApplet to easily open things from the data folder or from a URL,
   * but want an InputStream object so that you can use other Java
   * methods to take more control of how the stream is read.
   * <P>
   * If the requested item doesn't exist, null is returned.
   * (Prior to 0096, die() would be called, killing the applet)
   * <P>
   * For 0096+, the "data" folder is exported intact with subfolders,
   * and openStream() properly handles subdirectories from the data folder
   * <P>
   * If not online, this will also check to see if the user is asking
   * for a file whose name isn't properly capitalized. This helps prevent
   * issues when a sketch is exported to the web, where case sensitivity
   * matters, as opposed to Windows and the Mac OS default where
   * case sensitivity is preserved but ignored.
   * <P>
   * It is strongly recommended that libraries use this method to open
   * data files, so that the loading sequence is handled in the same way
   * as functions like loadBytes(), loadImage(), etc.
   * <P>
   * The filename passed in can be:
   * <UL>
   * <LI>A URL, for instance openStream("http://processing.org/");
   * <LI>A file in the sketch's data folder
   * <LI>Another file to be opened locally (when running as an application)
   * </UL>
   */
  public InputStream createInput(String filename) {
    InputStream input = createInputRaw(filename);
    if ((input != null) && filename.toLowerCase().endsWith(".gz")) {
      try {
        return new GZIPInputStream(input);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }
    return input;
  }


  /**
   * Call createInput() without automatic gzip decompression.
   */
  public InputStream createInputRaw(String filename) {
    // Additional considerations for Android version:
    // http://developer.android.com/guide/topics/resources/resources-i18n.html
    InputStream stream = null;

    if (filename == null) return null;

    if (filename.length() == 0) {
      // an error will be called by the parent function
      //System.err.println("The filename passed to openStream() was empty.");
      return null;
    }

    // safe to check for this as a url first. this will prevent online
    // access logs from being spammed with GET /sketchfolder/http://blahblah
    if (filename.indexOf(":") != -1) {  // at least smells like URL
      try {
        // Workaround for Android bug 6066
        // http://code.google.com/p/android/issues/detail?id=6066
        // http://code.google.com/p/processing/issues/detail?id=629
//      URL url = new URL(filename);
//      stream = url.openStream();
//      return stream;
        HttpGet httpRequest = null;
        httpRequest = new HttpGet(URI.create(filename));
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
        // can't use BufferedHttpEntity because it may try to allocate a byte
        // buffer of the size of the download, bad when DL is 25 MB... [0200]
//        BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
//        return bufHttpEntity.getContent();

      } catch (MalformedURLException mfue) {
        // not a url, that's fine

      } catch (FileNotFoundException fnfe) {
        // Java 1.5 likes to throw this when URL not available. (fix for 0119)
        // http://dev.processing.org/bugs/show_bug.cgi?id=403

      } catch (IOException e) {
        // changed for 0117, shouldn't be throwing exception
        e.printStackTrace();
        //System.err.println("Error downloading from URL " + filename);
        return null;
        //throw new RuntimeException("Error downloading from URL " + filename);
      }
    }

    /*
    // Moved this earlier than the getResourceAsStream() checks, because
    // calling getResourceAsStream() on a directory lists its contents.
    // http://dev.processing.org/bugs/show_bug.cgi?id=716
    try {
      // First see if it's in a data folder. This may fail by throwing
      // a SecurityException. If so, this whole block will be skipped.
      File file = new File(dataPath(filename));
      if (!file.exists()) {
        // next see if it's just in the sketch folder
        file = new File(sketchPath, filename);
      }
      if (file.isDirectory()) {
        return null;
      }
      if (file.exists()) {
        try {
          // handle case sensitivity check
          String filePath = file.getCanonicalPath();
          String filenameActual = new File(filePath).getName();
          // make sure there isn't a subfolder prepended to the name
          String filenameShort = new File(filename).getName();
          // if the actual filename is the same, but capitalized
          // differently, warn the user.
          //if (filenameActual.equalsIgnoreCase(filenameShort) &&
          //!filenameActual.equals(filenameShort)) {
          if (!filenameActual.equals(filenameShort)) {
            throw new RuntimeException("This file is named " +
                                       filenameActual + " not " +
                                       filename + ". Rename the file " +
            "or change your code.");
          }
        } catch (IOException e) { }
      }

      // if this file is ok, may as well just load it
      stream = new FileInputStream(file);
      if (stream != null) return stream;

      // have to break these out because a general Exception might
      // catch the RuntimeException being thrown above
    } catch (IOException ioe) {
    } catch (SecurityException se) { }
     */

    // Using getClassLoader() prevents Java from converting dots
    // to slashes or requiring a slash at the beginning.
    // (a slash as a prefix means that it'll load from the root of
    // the jar, rather than trying to dig into the package location)

    /*
    // this works, but requires files to be stored in the src folder
    ClassLoader cl = getClass().getClassLoader();
    stream = cl.getResourceAsStream(filename);
    if (stream != null) {
      String cn = stream.getClass().getName();
      // this is an irritation of sun's java plug-in, which will return
      // a non-null stream for an object that doesn't exist. like all good
      // things, this is probably introduced in java 1.5. awesome!
      // http://dev.processing.org/bugs/show_bug.cgi?id=359
      if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
        return stream;
      }
    }
     */

    // Try the assets folder
    AssetManager assets = getAssets();
    try {
      stream = assets.open(filename);
      if (stream != null) {
        return stream;
      }
    } catch (IOException e) {
      // ignore this and move on
      //e.printStackTrace();
    }

    // Maybe this is an absolute path, didja ever think of that?
    File absFile = new File(filename);
    if (absFile.exists()) {
      try {
        stream = new FileInputStream(absFile);
        if (stream != null) {
          return stream;
        }
      } catch (FileNotFoundException fnfe) {
        //fnfe.printStackTrace();
      }
    }

    // Maybe this is a file that was written by the sketch on another occasion.
    File sketchFile = new File(sketchPath(filename));
    if (sketchFile.exists()) {
      try {
        stream = new FileInputStream(sketchFile);
        if (stream != null) {
          return stream;
        }
      } catch (FileNotFoundException fnfe) {
        //fnfe.printStackTrace();
      }
    }

    // Attempt to load the file more directly. Doesn't like paths.
    Context context = getApplicationContext();
    try {
      // MODE_PRIVATE is default, should we use something else?
      stream = context.openFileInput(filename);
      if (stream != null) {
        return stream;
      }
    } catch (FileNotFoundException e) {
      // ignore this and move on
      //e.printStackTrace();
    }

    return null;
  }


  static public InputStream createInput(File file) {
    if (file == null) {
      throw new IllegalArgumentException("File passed to createInput() was null");
    }
    try {
      InputStream input = new FileInputStream(file);
      if (file.getName().toLowerCase().endsWith(".gz")) {
        return new GZIPInputStream(input);
      }
      return input;

    } catch (IOException e) {
      System.err.println("Could not createInput() for " + file);
      e.printStackTrace();
      return null;
    }
  }


  public byte[] loadBytes(String filename) {
    InputStream is = createInput(filename);
    if (is != null) return loadBytes(is);

    System.err.println("The file \"" + filename + "\" " +
                       "is missing or inaccessible, make sure " +
                       "the URL is valid or that the file has been " +
                       "added to your sketch and is readable.");
    return null;
  }


  static public byte[] loadBytes(InputStream input) {
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
      e.printStackTrace();
      //throw new RuntimeException("Couldn't load bytes from stream");
    }
    return null;
  }


  static public byte[] loadBytes(File file) {
    InputStream is = createInput(file);
    return loadBytes(is);
  }


  static public String[] loadStrings(File file) {
    InputStream is = createInput(file);
    if (is != null) return loadStrings(is);
    return null;
  }


  /**
   * Load data from a file and shove it into a String array.
   * <P>
   * Exceptions are handled internally, when an error, occurs, an
   * exception is printed to the console and 'null' is returned,
   * but the program continues running. This is a tradeoff between
   * 1) showing the user that there was a problem but 2) not requiring
   * that all i/o code is contained in try/catch blocks, for the sake
   * of new users (or people who are just trying to get things done
   * in a "scripting" fashion. If you want to handle exceptions,
   * use Java methods for I/O.
   */
  public String[] loadStrings(String filename) {
    InputStream is = createInput(filename);
    if (is != null) return loadStrings(is);

    System.err.println("The file \"" + filename + "\" " +
                       "is missing or inaccessible, make sure " +
                       "the URL is valid or that the file has been " +
                       "added to your sketch and is readable.");
    return null;
  }


  static public String[] loadStrings(InputStream input) {
    try {
      BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, "UTF-8"));

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

      // resize array to appropriate amount for these lines
      String output[] = new String[lineCount];
      System.arraycopy(lines, 0, output, 0, lineCount);
      return output;

    } catch (IOException e) {
      e.printStackTrace();
      //throw new RuntimeException("Error inside loadStrings()");
    }
    return null;
  }



  //////////////////////////////////////////////////////////////

  // FILE OUTPUT


  /**
   * Similar to createInput() (formerly openStream), this creates a Java
   * OutputStream for a given filename or path. The file will be created in
   * the sketch folder, or in the same folder as an exported application.
   * <p/>
   * If the path does not exist, intermediate folders will be created. If an
   * exception occurs, it will be printed to the console, and null will be
   * returned.
   * <p/>
   * Future releases may also add support for handling HTTP POST via this
   * method (for better symmetry with createInput), however that's maybe a
   * little too clever (and then we'd have to add the same features to the
   * other file functions like createWriter). Who you callin' bloated?
   */
  public OutputStream createOutput(String filename) {
    try {
      // in spite of appearing to be the 'correct' option, this doesn't allow
      // for paths, so no subfolders, none of that savePath() goodness.
//      Context context = getApplicationContext();
//      // MODE_PRIVATE is default, should we use that instead?
//      return context.openFileOutput(filename, MODE_WORLD_READABLE);

      File file = new File(filename);
      if (!file.isAbsolute()) {
        file = new File(sketchPath(filename));
      }
      FileOutputStream fos = new FileOutputStream(file);
      if (file.getName().toLowerCase().endsWith(".gz")) {
        return new GZIPOutputStream(fos);
      }
      return fos;

    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }


//  static public OutputStream createOutput(File file) {
//    try {
//      FileOutputStream fos = new FileOutputStream(file);
//      if (file.getName().toLowerCase().endsWith(".gz")) {
//        return new GZIPOutputStream(fos);
//      }
//      return fos;
//
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//    return null;
//  }


  /**
   * Save the contents of a stream to a file in the sketch folder.
   * This is basically saveBytes(blah, loadBytes()), but done
   * more efficiently (and with less confusing syntax).
   */
  public boolean saveStream(String targetFilename, String sourceLocation) {
    return saveStream(saveFile(targetFilename), sourceLocation);
  }


  /**
   * Identical to the other saveStream(), but writes to a File
   * object, for greater control over the file location.
   * Note that unlike other api methods, this will not automatically
   * compress or uncompress gzip files.
   */
  public boolean saveStream(File targetFile, String sourceLocation) {
    return saveStream(targetFile, createInputRaw(sourceLocation));
  }


  public boolean saveStream(String targetFilename, InputStream sourceStream) {
    return saveStream(saveFile(targetFilename), sourceStream);
  }


  static public boolean saveStream(File targetFile, InputStream sourceStream) {
    File tempFile = null;
    try {
      File parentDir = targetFile.getParentFile();
      createPath(targetFile);
      tempFile = File.createTempFile(targetFile.getName(), null, parentDir);

      BufferedInputStream bis = new BufferedInputStream(sourceStream, 16384);
      FileOutputStream fos = new FileOutputStream(tempFile);
      BufferedOutputStream bos = new BufferedOutputStream(fos);

      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = bis.read(buffer)) != -1) {
        bos.write(buffer, 0, bytesRead);
      }

      bos.flush();
      bos.close();
      bos = null;

      if (targetFile.exists() && !targetFile.delete()) {
        System.err.println("Could not replace " +
                           targetFile.getAbsolutePath() + ".");
      }

      if (!tempFile.renameTo(targetFile)) {
        System.err.println("Could not rename temporary file " +
                           tempFile.getAbsolutePath());
        return false;
      }
      return true;

    } catch (IOException e) {
      if (tempFile != null) {
        tempFile.delete();
      }
      e.printStackTrace();
      return false;
    }
  }


  /**
   * Saves bytes to a file to inside the sketch folder.
   * The filename can be a relative path, i.e. "poo/bytefun.txt"
   * would save to a file named "bytefun.txt" to a subfolder
   * called 'poo' inside the sketch folder. If the in-between
   * subfolders don't exist, they'll be created.
   */
  public void saveBytes(String filename, byte buffer[]) {
    saveBytes(saveFile(filename), buffer);
  }


  /**
   * Saves bytes to a specific File location specified by the user.
   */
  static public void saveBytes(File file, byte buffer[]) {
    try {
      String filename = file.getAbsolutePath();
      createPath(filename);
      OutputStream output = new FileOutputStream(file);
      if (file.getName().toLowerCase().endsWith(".gz")) {
        output = new GZIPOutputStream(output);
      }
      saveBytes(output, buffer);
      output.close();

    } catch (IOException e) {
      System.err.println("error saving bytes to " + file);
      e.printStackTrace();
    }
  }


  /**
   * Spews a buffer of bytes to an OutputStream.
   */
  static public void saveBytes(OutputStream output, byte buffer[]) {
    try {
      output.write(buffer);
      output.flush();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //

  public void saveStrings(String filename, String strings[]) {
    saveStrings(saveFile(filename), strings);
  }


  static public void saveStrings(File file, String strings[]) {
    try {
      String location = file.getAbsolutePath();
      createPath(location);
      OutputStream output = new FileOutputStream(location);
      if (file.getName().toLowerCase().endsWith(".gz")) {
        output = new GZIPOutputStream(output);
      }
      saveStrings(output, strings);
      output.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  static public void saveStrings(OutputStream output, String strings[]) {
    try {
      OutputStreamWriter osw = new OutputStreamWriter(output, "UTF-8");
      PrintWriter writer = new PrintWriter(osw);
      for (int i = 0; i < strings.length; i++) {
        writer.println(strings[i]);
      }
      writer.flush();
    } catch (UnsupportedEncodingException e) { }  // will not happen
  }


  //////////////////////////////////////////////////////////////


  /**
   * Prepend the sketch folder path to the filename (or path) that is
   * passed in. External libraries should use this function to save to
   * the sketch folder.
   * <p/>
   * Note that when running as an applet inside a web browser,
   * the sketchPath will be set to null, because security restrictions
   * prevent applets from accessing that information.
   * <p/>
   * This will also cause an error if the sketch is not inited properly,
   * meaning that init() was never called on the PApplet when hosted
   * my some other main() or by other code. For proper use of init(),
   * see the examples in the main description text for PApplet.
   */
  public String sketchPath(String where) {
    if (sketchPath == null) {
      return where;
//      throw new RuntimeException("The applet was not inited properly, " +
//                                 "or security restrictions prevented " +
//                                 "it from determining its path.");
    }

    // isAbsolute() could throw an access exception, but so will writing
    // to the local disk using the sketch path, so this is safe here.
    // for 0120, added a try/catch anyways.
    try {
      if (new File(where).isAbsolute()) return where;
    } catch (Exception e) { }

    Context context = getApplicationContext();
    return context.getFileStreamPath(where).getAbsolutePath();
  }


  public File sketchFile(String where) {
    return new File(sketchPath(where));
  }


  /**
   * Returns a path inside the applet folder to save to. Like sketchPath(),
   * but creates any in-between folders so that things save properly.
   * <p/>
   * All saveXxxx() functions use the path to the sketch folder, rather than
   * its data folder. Once exported, the data folder will be found inside the
   * jar file of the exported application or applet. In this case, it's not
   * possible to save data into the jar file, because it will often be running
   * from a server, or marked in-use if running from a local file system.
   * With this in mind, saving to the data path doesn't make sense anyway.
   * If you know you're running locally, and want to save to the data folder,
   * use <TT>saveXxxx("data/blah.dat")</TT>.
   */
  public String savePath(String where) {
    if (where == null) return null;
//    System.out.println("filename before sketchpath is " + where);
    String filename = sketchPath(where);
//    System.out.println("filename after sketchpath is " + filename);
    createPath(filename);
    return filename;
  }


  /**
   * Identical to savePath(), but returns a File object.
   */
  public File saveFile(String where) {
    return new File(savePath(where));
  }


  /**
   * Return a full path to an item in the data folder.
   * <p>
   * In this method, the data path is defined not as the applet's actual
   * data path, but a folder titled "data" in the sketch's working
   * directory. When running inside the PDE, this will be the sketch's
   * "data" folder. However, when exported (as application or applet),
   * sketch's data folder is exported as part of the applications jar file,
   * and it's not possible to read/write from the jar file in a generic way.
   * If you need to read data from the jar file, you should use createInput().
   */
  public String dataPath(String where) {
    // isAbsolute() could throw an access exception, but so will writing
    // to the local disk using the sketch path, so this is safe here.
    if (new File(where).isAbsolute()) return where;

    return sketchPath + File.separator + "data" + File.separator + where;
  }


  /**
   * Return a full path to an item in the data folder as a File object.
   * See the dataPath() method for more information.
   */
  public File dataFile(String where) {
    return new File(dataPath(where));
  }


  /**
   * Takes a path and creates any in-between folders if they don't
   * already exist. Useful when trying to save to a subfolder that
   * may not actually exist.
   */
  static public void createPath(String path) {
    createPath(new File(path));
  }


  static public void createPath(File file) {
    try {
      String parent = file.getParent();
      if (parent != null) {
        File unit = new File(parent);
        if (!unit.exists()) unit.mkdirs();
      }
    } catch (SecurityException se) {
      System.err.println("You don't have permissions to create " + file.getAbsolutePath());
    }
  }


  static public String getExtension(String filename) {
    String extension;

    String lower = filename.toLowerCase();
    int dot = filename.lastIndexOf('.');
    if (dot == -1) {
      extension = "unknown";  // no extension found
    }
    extension = lower.substring(dot + 1);

    // check for, and strip any parameters on the url, i.e.
    // filename.jpg?blah=blah&something=that
    int question = extension.indexOf('?');
    if (question != -1) {
      extension = extension.substring(0, question);
    }

    return extension;
  }


  //////////////////////////////////////////////////////////////

  // URL ENCODING

  static public String urlEncode(String what) {
    try {
      return URLEncoder.encode(what, "UTF-8");
    } catch (UnsupportedEncodingException e) {  // oh c'mon
      return null;
    }
  }


  static public String urlDecode(String what) {
    try {
      return URLDecoder.decode(what, "UTF-8");
    } catch (UnsupportedEncodingException e) {  // safe per the JDK source
      return null;
    }
  }



  //////////////////////////////////////////////////////////////

  // SORT


  static public byte[] sort(byte what[]) {
    return sort(what, what.length);
  }


  static public byte[] sort(byte[] what, int count) {
    byte[] outgoing = new byte[what.length];
    System.arraycopy(what, 0, outgoing, 0, what.length);
    Arrays.sort(outgoing, 0, count);
    return outgoing;
  }


  static public char[] sort(char what[]) {
    return sort(what, what.length);
  }


  static public char[] sort(char[] what, int count) {
    char[] outgoing = new char[what.length];
    System.arraycopy(what, 0, outgoing, 0, what.length);
    Arrays.sort(outgoing, 0, count);
    return outgoing;
  }


  static public int[] sort(int what[]) {
    return sort(what, what.length);
  }


  static public int[] sort(int[] what, int count) {
    int[] outgoing = new int[what.length];
    System.arraycopy(what, 0, outgoing, 0, what.length);
    Arrays.sort(outgoing, 0, count);
    return outgoing;
  }


  static public float[] sort(float what[]) {
    return sort(what, what.length);
  }


  static public float[] sort(float[] what, int count) {
    float[] outgoing = new float[what.length];
    System.arraycopy(what, 0, outgoing, 0, what.length);
    Arrays.sort(outgoing, 0, count);
    return outgoing;
  }


  static public String[] sort(String what[]) {
    return sort(what, what.length);
  }


  static public String[] sort(String[] what, int count) {
    String[] outgoing = new String[what.length];
    System.arraycopy(what, 0, outgoing, 0, what.length);
    Arrays.sort(outgoing, 0, count);
    return outgoing;
  }



  //////////////////////////////////////////////////////////////

  // ARRAY UTILITIES


  /**
   * Calls System.arraycopy(), included here so that we can
   * avoid people needing to learn about the System object
   * before they can just copy an array.
   */
  static public void arrayCopy(Object src, int srcPosition,
                               Object dst, int dstPosition,
                               int length) {
    System.arraycopy(src, srcPosition, dst, dstPosition, length);
  }


  /**
   * Convenience method for arraycopy().
   * Identical to <CODE>arraycopy(src, 0, dst, 0, length);</CODE>
   */
  static public void arrayCopy(Object src, Object dst, int length) {
    System.arraycopy(src, 0, dst, 0, length);
  }


  /**
   * Shortcut to copy the entire contents of
   * the source into the destination array.
   * Identical to <CODE>arraycopy(src, 0, dst, 0, src.length);</CODE>
   */
  static public void arrayCopy(Object src, Object dst) {
    System.arraycopy(src, 0, dst, 0, Array.getLength(src));
  }

  //

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


  static public PImage[] expand(PImage list[]) {
    return expand(list, list.length << 1);
  }

  static public PImage[] expand(PImage list[], int newSize) {
    PImage temp[] = new PImage[newSize];
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


  static public Object expand(Object array) {
    return expand(array, Array.getLength(array) << 1);
  }

  static public Object expand(Object list, int newSize) {
    Class<?> type = list.getClass().getComponentType();
    Object temp = Array.newInstance(type, newSize);
    System.arraycopy(list, 0, temp, 0,
                     Math.min(Array.getLength(list), newSize));
    return temp;
  }

  //

  // contract() has been removed in revision 0124, use subset() instead.
  // (expand() is also functionally equivalent)

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

  static public Object append(Object b, Object value) {
    int length = Array.getLength(b);
    b = expand(b, length + 1);
    Array.set(b, length, value);
    return b;
  }

  //

  static public boolean[] shorten(boolean list[]) {
    return subset(list, 0, list.length-1);
  }

  static public byte[] shorten(byte list[]) {
    return subset(list, 0, list.length-1);
  }

  static public char[] shorten(char list[]) {
    return subset(list, 0, list.length-1);
  }

  static public int[] shorten(int list[]) {
    return subset(list, 0, list.length-1);
  }

  static public float[] shorten(float list[]) {
    return subset(list, 0, list.length-1);
  }

  static public String[] shorten(String list[]) {
    return subset(list, 0, list.length-1);
  }

  static public Object shorten(Object list) {
    int length = Array.getLength(list);
    return subset(list, 0, length - 1);
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


  static final public Object splice(Object list, Object v, int index) {
    Object[] outgoing = null;
    int length = Array.getLength(list);

    // check whether item being spliced in is an array
    if (v.getClass().getName().charAt(0) == '[') {
      int vlength = Array.getLength(v);
      outgoing = new Object[length + vlength];
      System.arraycopy(list, 0, outgoing, 0, index);
      System.arraycopy(v, 0, outgoing, index, vlength);
      System.arraycopy(list, index, outgoing, index + vlength, length - index);

    } else {
      outgoing = new Object[length + 1];
      System.arraycopy(list, 0, outgoing, 0, index);
      Array.set(outgoing, index, v);
      System.arraycopy(list, index, outgoing, index + 1, length - index);
    }
    return outgoing;
  }

  //

  static public boolean[] subset(boolean list[], int start) {
    return subset(list, start, list.length - start);
  }

  static public boolean[] subset(boolean list[], int start, int count) {
    boolean output[] = new boolean[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public byte[] subset(byte list[], int start) {
    return subset(list, start, list.length - start);
  }

  static public byte[] subset(byte list[], int start, int count) {
    byte output[] = new byte[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public char[] subset(char list[], int start) {
    return subset(list, start, list.length - start);
  }

  static public char[] subset(char list[], int start, int count) {
    char output[] = new char[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


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


  static public Object subset(Object list, int start) {
    int length = Array.getLength(list);
    return subset(list, start, length - start);
  }

  static public Object subset(Object list, int start, int count) {
    Class<?> type = list.getClass().getComponentType();
    Object outgoing = Array.newInstance(type, count);
    System.arraycopy(list, start, outgoing, 0, count);
    return outgoing;
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

  static public Object concat(Object a, Object b) {
    Class<?> type = a.getClass().getComponentType();
    int alength = Array.getLength(a);
    int blength = Array.getLength(b);
    Object outgoing = Array.newInstance(type, alength + blength);
    System.arraycopy(a, 0, outgoing, 0, alength);
    System.arraycopy(b, 0, outgoing, alength, blength);
    return outgoing;
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

  static public Object reverse(Object list) {
    Class<?> type = list.getClass().getComponentType();
    int length = Array.getLength(list);
    Object outgoing = Array.newInstance(type, length);
    for (int i = 0; i < length; i++) {
      Array.set(outgoing, i, Array.get(list, (length - 1) - i));
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
  }


  /**
   * Trim the whitespace from a String array. This returns a new
   * array and does not affect the passed-in array.
   */
  static public String[] trim(String[] array) {
    String[] outgoing = new String[array.length];
    for (int i = 0; i < array.length; i++) {
      if (array[i] != null) {
        outgoing[i] = array[i].replace('\u00A0', ' ').trim();
      }
    }
    return outgoing;
  }


  /**
   * Join an array of Strings together as a single String,
   * separated by the whatever's passed in for the separator.
   */
  static public String join(String str[], char separator) {
    return join(str, String.valueOf(separator));
  }


  /**
   * Join an array of Strings together as a single String,
   * separated by the whatever's passed in for the separator.
   * <P>
   * To use this on numbers, first pass the array to nf() or nfs()
   * to get a list of String objects, then use join on that.
   * <PRE>
   * e.g. String stuff[] = { "apple", "bear", "cat" };
   *      String list = join(stuff, ", ");
   *      // list is now "apple, bear, cat"</PRE>
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
   * <P>
   * The whitespace characters are "\t\n\r\f", which are the defaults
   * for java.util.StringTokenizer, plus the unicode non-breaking space
   * character, which is found commonly on files created by or used
   * in conjunction with Mac OS X (character 160, or 0x00A0 in hex).
   * <PRE>
   * i.e. splitTokens("a b") -> { "a", "b" }
   *      splitTokens("a    b") -> { "a", "b" }
   *      splitTokens("a\tb") -> { "a", "b" }
   *      splitTokens("a \t  b  ") -> { "a", "b" }</PRE>
   */
  static public String[] splitTokens(String what) {
    return splitTokens(what, WHITESPACE);
  }


  /**
   * Splits a string into pieces, using any of the chars in the
   * String 'delim' as separator characters. For instance,
   * in addition to white space, you might want to treat commas
   * as a separator. The delimeter characters won't appear in
   * the returned String array.
   * <PRE>
   * i.e. splitTokens("a, b", " ,") -> { "a", "b" }
   * </PRE>
   * To include all the whitespace possibilities, use the variable
   * WHITESPACE, found in PConstants:
   * <PRE>
   * i.e. splitTokens("a   | b", WHITESPACE + "|");  ->  { "a", "b" }</PRE>
   */
  static public String[] splitTokens(String what, String delim) {
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
   * Most commonly used to break up a String along a space or a tab
   * character.
   * <P>
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
   * Split a String on a specific delimiter. Unlike Java's String.split()
   * method, this does not parse the delimiter as a regexp because it's more
   * confusing than necessary, and String.split() is always available for
   * those who want regexp.
   */
  static public String[] split(String what, String delim) {
    ArrayList<String> items = new ArrayList<String>();
    int index;
    int offset = 0;
    while ((index = what.indexOf(delim, offset)) != -1) {
      items.add(what.substring(offset, index));
      offset = index + delim.length();
    }
    items.add(what.substring(offset));
    String[] outgoing = new String[items.size()];
    items.toArray(outgoing);
    return outgoing;
  }


  static protected HashMap<String, Pattern> matchPatterns;

  static Pattern matchPattern(String regexp) {
    Pattern p = null;
    if (matchPatterns == null) {
      matchPatterns = new HashMap<String, Pattern>();
    } else {
      p = matchPatterns.get(regexp);
    }
    if (p == null) {
      if (matchPatterns.size() == 10) {
        // Just clear out the match patterns here if more than 10 are being
        // used. It's not terribly efficient, but changes that you have >10
        // different match patterns are very slim, unless you're doing
        // something really tricky (like custom match() methods), in which
        // case match() won't be efficient anyway. (And you should just be
        // using your own Java code.) The alternative is using a queue here,
        // but that's a silly amount of work for negligible benefit.
        matchPatterns.clear();
      }
      p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
      matchPatterns.put(regexp, p);
    }
    return p;
  }


  /**
   * Match a string with a regular expression, and returns the match as an
   * array. The first index is the matching expression, and array elements
   * [1] and higher represent each of the groups (sequences found in parens).
   *
   * This uses multiline matching (Pattern.MULTILINE) and dotall mode
   * (Pattern.DOTALL) by default, so that ^ and $ match the beginning and
   * end of any lines found in the source, and the . operator will also
   * pick up newline characters.
   */
  static public String[] match(String what, String regexp) {
    Pattern p = matchPattern(regexp);
    Matcher m = p.matcher(what);
    if (m.find()) {
      int count = m.groupCount() + 1;
      String[] groups = new String[count];
      for (int i = 0; i < count; i++) {
        groups[i] = m.group(i);
      }
      return groups;
    }
    return null;
  }


  /**
   * Identical to match(), except that it returns an array of all matches in
   * the specified String, rather than just the first.
   */
  static public String[][] matchAll(String what, String regexp) {
    Pattern p = matchPattern(regexp);
    Matcher m = p.matcher(what);
    ArrayList<String[]> results = new ArrayList<String[]>();
    int count = m.groupCount() + 1;
    while (m.find()) {
      String[] groups = new String[count];
      for (int i = 0; i < count; i++) {
        groups[i] = m.group(i);
      }
      results.add(groups);
    }
    if (results.isEmpty()) {
      return null;
    }
    String[][] matches = new String[results.size()][count];
    for (int i = 0; i < matches.length; i++) {
      matches[i] = (String[]) results.get(i);
    }
    return matches;
  }



  //////////////////////////////////////////////////////////////

  // CASTING FUNCTIONS, INSERTED BY PREPROC


  /**
   * Convert a char to a boolean. 'T', 't', and '1' will become the
   * boolean value true, while 'F', 'f', or '0' will become false.
   */
  /*
  static final public boolean parseBoolean(char what) {
    return ((what == 't') || (what == 'T') || (what == '1'));
  }
  */

  /**
   * <p>Convert an integer to a boolean. Because of how Java handles upgrading
   * numbers, this will also cover byte and char (as they will upgrade to
   * an int without any sort of explicit cast).</p>
   * <p>The preprocessor will convert boolean(what) to parseBoolean(what).</p>
   * @return false if 0, true if any other number
   */
  static final public boolean parseBoolean(int what) {
    return (what != 0);
  }

  /*
  // removed because this makes no useful sense
  static final public boolean parseBoolean(float what) {
    return (what != 0);
  }
  */

  /**
   * Convert the string "true" or "false" to a boolean.
   * @return true if 'what' is "true" or "TRUE", false otherwise
   */
  static final public boolean parseBoolean(String what) {
    return new Boolean(what).booleanValue();
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
  // removed, no need to introduce strange syntax from other languages
  static final public boolean[] parseBoolean(char what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] =
        ((what[i] == 't') || (what[i] == 'T') || (what[i] == '1'));
    }
    return outgoing;
  }
  */

  /**
   * Convert a byte array to a boolean array. Each element will be
   * evaluated identical to the integer case, where a byte equal
   * to zero will return false, and any other value will return true.
   * @return array of boolean elements
   */
  static final public boolean[] parseBoolean(byte what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (what[i] != 0);
    }
    return outgoing;
  }

  /**
   * Convert an int array to a boolean array. An int equal
   * to zero will return false, and any other value will return true.
   * @return array of boolean elements
   */
  static final public boolean[] parseBoolean(int what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (what[i] != 0);
    }
    return outgoing;
  }

  /*
  // removed, not necessary... if necessary, convert to int array first
  static final public boolean[] parseBoolean(float what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (what[i] != 0);
    }
    return outgoing;
  }
  */

  static final public boolean[] parseBoolean(String what[]) {
    boolean outgoing[] = new boolean[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = new Boolean(what[i]).booleanValue();
    }
    return outgoing;
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  static final public byte parseByte(boolean what) {
    return what ? (byte)1 : 0;
  }

  static final public byte parseByte(char what) {
    return (byte) what;
  }

  static final public byte parseByte(int what) {
    return (byte) what;
  }

  static final public byte parseByte(float what) {
    return (byte) what;
  }

  /*
  // nixed, no precedent
  static final public byte[] parseByte(String what) {  // note: array[]
    return what.getBytes();
  }
  */

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  static final public byte[] parseByte(boolean what[]) {
    byte outgoing[] = new byte[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = what[i] ? (byte)1 : 0;
    }
    return outgoing;
  }

  static final public byte[] parseByte(char what[]) {
    byte outgoing[] = new byte[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (byte) what[i];
    }
    return outgoing;
  }

  static final public byte[] parseByte(int what[]) {
    byte outgoing[] = new byte[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (byte) what[i];
    }
    return outgoing;
  }

  static final public byte[] parseByte(float what[]) {
    byte outgoing[] = new byte[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (byte) what[i];
    }
    return outgoing;
  }

  /*
  static final public byte[][] parseByte(String what[]) {  // note: array[][]
    byte outgoing[][] = new byte[what.length][];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = what[i].getBytes();
    }
    return outgoing;
  }
  */

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
  static final public char parseChar(boolean what) {  // 0/1 or T/F ?
    return what ? 't' : 'f';
  }
  */

  static final public char parseChar(byte what) {
    return (char) (what & 0xff);
  }

  static final public char parseChar(int what) {
    return (char) what;
  }

  /*
  static final public char parseChar(float what) {  // nonsensical
    return (char) what;
  }

  static final public char[] parseChar(String what) {  // note: array[]
    return what.toCharArray();
  }
  */

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
  static final public char[] parseChar(boolean what[]) {  // 0/1 or T/F ?
    char outgoing[] = new char[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = what[i] ? 't' : 'f';
    }
    return outgoing;
  }
  */

  static final public char[] parseChar(byte what[]) {
    char outgoing[] = new char[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (char) (what[i] & 0xff);
    }
    return outgoing;
  }

  static final public char[] parseChar(int what[]) {
    char outgoing[] = new char[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (char) what[i];
    }
    return outgoing;
  }

  /*
  static final public char[] parseChar(float what[]) {  // nonsensical
    char outgoing[] = new char[what.length];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = (char) what[i];
    }
    return outgoing;
  }

  static final public char[][] parseChar(String what[]) {  // note: array[][]
    char outgoing[][] = new char[what.length][];
    for (int i = 0; i < what.length; i++) {
      outgoing[i] = what[i].toCharArray();
    }
    return outgoing;
  }
  */

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  static final public int parseInt(boolean what) {
    return what ? 1 : 0;
  }

  /**
   * Note that parseInt() will un-sign a signed byte value.
   */
  static final public int parseInt(byte what) {
    return what & 0xff;
  }

  /**
   * Note that parseInt('5') is unlike String in the sense that it
   * won't return 5, but the ascii value. This is because ((int) someChar)
   * returns the ascii value, and parseInt() is just longhand for the cast.
   */
  static final public int parseInt(char what) {
    return what;
  }

  /**
   * Same as floor(), or an (int) cast.
   */
  static final public int parseInt(float what) {
    return (int) what;
  }

  /**
   * Parse a String into an int value. Returns 0 if the value is bad.
   */
  static final public int parseInt(String what) {
    return parseInt(what, 0);
  }

  /**
   * Parse a String to an int, and provide an alternate value that
   * should be used when the number is invalid.
   */
  static final public int parseInt(String what, int otherwise) {
    try {
      int offset = what.indexOf('.');
      if (offset == -1) {
        return Integer.parseInt(what);
      } else {
        return Integer.parseInt(what.substring(0, offset));
      }
    } catch (NumberFormatException e) { }
    return otherwise;
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  static final public int[] parseInt(boolean what[]) {
    int list[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      list[i] = what[i] ? 1 : 0;
    }
    return list;
  }

  static final public int[] parseInt(byte what[]) {  // note this unsigns
    int list[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      list[i] = (what[i] & 0xff);
    }
    return list;
  }

  static final public int[] parseInt(char what[]) {
    int list[] = new int[what.length];
    for (int i = 0; i < what.length; i++) {
      list[i] = what[i];
    }
    return list;
  }

  static public int[] parseInt(float what[]) {
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
   * int numbers[] = parseInt(s);
   *
   * numbers will contain { 1, 300, 44 }
   */
  static public int[] parseInt(String what[]) {
    return parseInt(what, 0);
  }

  /**
   * Make an array of int elements from an array of String objects.
   * If the String can't be parsed as a number, its entry in the
   * array will be set to the value of the "missing" parameter.
   *
   * String s[] = { "1", "300", "apple", "44" };
   * int numbers[] = parseInt(s, 9999);
   *
   * numbers will contain { 1, 300, 9999, 44 }
   */
  static public int[] parseInt(String what[], int missing) {
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

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
  static final public float parseFloat(boolean what) {
    return what ? 1 : 0;
  }
  */

  /**
   * Convert an int to a float value. Also handles bytes because of
   * Java's rules for upgrading values.
   */
  static final public float parseFloat(int what) {  // also handles byte
    return (float)what;
  }

  static final public float parseFloat(String what) {
    return parseFloat(what, Float.NaN);
  }

  static final public float parseFloat(String what, float otherwise) {
    try {
      return new Float(what).floatValue();
    } catch (NumberFormatException e) { }

    return otherwise;
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
  static final public float[] parseFloat(boolean what[]) {
    float floaties[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      floaties[i] = what[i] ? 1 : 0;
    }
    return floaties;
  }

  static final public float[] parseFloat(char what[]) {
    float floaties[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      floaties[i] = (char) what[i];
    }
    return floaties;
  }
  */

  static final public float[] parseByte(byte what[]) {
    float floaties[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      floaties[i] = what[i];
    }
    return floaties;
  }

  static final public float[] parseFloat(int what[]) {
    float floaties[] = new float[what.length];
    for (int i = 0; i < what.length; i++) {
      floaties[i] = what[i];
    }
    return floaties;
  }

  static final public float[] parseFloat(String what[]) {
    return parseFloat(what, Float.NaN);
  }

  static final public float[] parseFloat(String what[], float missing) {
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

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  static final public String str(boolean x) {
    return String.valueOf(x);
  }

  static final public String str(byte x) {
    return String.valueOf(x);
  }

  static final public String str(char x) {
    return String.valueOf(x);
  }

  static final public String str(int x) {
    return String.valueOf(x);
  }

  static final public String str(float x) {
    return String.valueOf(x);
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  static final public String[] str(boolean x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
    return s;
  }

  static final public String[] str(byte x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
    return s;
  }

  static final public String[] str(char x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
    return s;
  }

  static final public String[] str(int x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
    return s;
  }

  static final public String[] str(float x[]) {
    String s[] = new String[x.length];
    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
    return s;
  }


  //////////////////////////////////////////////////////////////

  // INT NUMBER FORMATTING


  /**
   * Integer number formatter.
   */
  static private NumberFormat int_nf;
  static private int int_nf_digits;
  static private boolean int_nf_commas;


  static public String[] nf(int num[], int digits) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nf(num[i], digits);
    }
    return formatted;
  }


  static public String nf(int num, int digits) {
    if ((int_nf != null) &&
        (int_nf_digits == digits) &&
        !int_nf_commas) {
      return int_nf.format(num);
    }

    int_nf = NumberFormat.getInstance();
    int_nf.setGroupingUsed(false); // no commas
    int_nf_commas = false;
    int_nf.setMinimumIntegerDigits(digits);
    int_nf_digits = digits;
    return int_nf.format(num);
  }


  static public String[] nfc(int num[]) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nfc(num[i]);
    }
    return formatted;
  }


  static public String nfc(int num) {
    if ((int_nf != null) &&
        (int_nf_digits == 0) &&
        int_nf_commas) {
      return int_nf.format(num);
    }

    int_nf = NumberFormat.getInstance();
    int_nf.setGroupingUsed(true);
    int_nf_commas = true;
    int_nf.setMinimumIntegerDigits(0);
    int_nf_digits = 0;
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
  static private boolean float_nf_commas;


  static public String[] nf(float num[], int left, int right) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nf(num[i], left, right);
    }
    return formatted;
  }


  static public String nf(float num, int left, int right) {
    if ((float_nf != null) &&
        (float_nf_left == left) &&
        (float_nf_right == right) &&
        !float_nf_commas) {
      return float_nf.format(num);
    }

    float_nf = NumberFormat.getInstance();
    float_nf.setGroupingUsed(false);
    float_nf_commas = false;

    if (left != 0) float_nf.setMinimumIntegerDigits(left);
    if (right != 0) {
      float_nf.setMinimumFractionDigits(right);
      float_nf.setMaximumFractionDigits(right);
    }
    float_nf_left = left;
    float_nf_right = right;
    return float_nf.format(num);
  }


  static public String[] nfc(float num[], int right) {
    String formatted[] = new String[num.length];
    for (int i = 0; i < formatted.length; i++) {
      formatted[i] = nfc(num[i], right);
    }
    return formatted;
  }


  static public String nfc(float num, int right) {
    if ((float_nf != null) &&
        (float_nf_left == 0) &&
        (float_nf_right == right) &&
        float_nf_commas) {
      return float_nf.format(num);
    }

    float_nf = NumberFormat.getInstance();
    float_nf.setGroupingUsed(true);
    float_nf_commas = true;

    if (right != 0) {
      float_nf.setMinimumFractionDigits(right);
      float_nf.setMaximumFractionDigits(right);
    }
    float_nf_left = 0;
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


  /**
   * Convert a byte into a two digit hex string.
   */
  static final public String hex(byte what) {
    return hex(what, 2);
  }

  /**
   * Convert a Unicode character into a four digit hex string.
   */
  static final public String hex(char what) {
    return hex(what, 4);
  }

  /**
   * Convert an integer into an eight digit hex string.
   */
  static final public String hex(int what) {
    return hex(what, 8);
  }

  /**
   * Format an integer as a hex string using the specified number of digits.
   * @param what the value to format
   * @param digits the number of digits (maximum 8)
   * @return a String object with the formatted values
   */
  static final public String hex(int what, int digits) {
    String stuff = Integer.toHexString(what).toUpperCase();
    if (digits > 8) {
      digits = 8;
    }

    int length = stuff.length();
    if (length > digits) {
      return stuff.substring(length - digits);

    } else if (length < digits) {
      return "00000000".substring(8 - (digits-length)) + stuff;
    }
    return stuff;
  }

  static final public int unhex(String what) {
    // has to parse as a Long so that it'll work for numbers bigger than 2^31
    return (int) (Long.parseLong(what, 16));
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
   * Returns a String that contains the binary value of an int. The length
   * depends on the size of the number itself. If you want a specific number
   * of digits use binary(int what, int digits) to specify how many.
   */
  static final public String binary(int what) {
    return binary(what, 32);
  }

  /**
   * Returns a String that contains the binary value of an int.
   * The digits parameter determines how many digits will be used.
   */
  static final public String binary(int what, int digits) {
    String stuff = Integer.toBinaryString(what);
    if (digits > 32) {
      digits = 32;
    }

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
  static final public int unbinary(String what) {
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


  /**
   * As of 0116 this also takes color(#FF8800, alpha)
   */
  public final int color(int gray, int alpha) {
    if (g == null) {
      if (alpha > 255) alpha = 255; else if (alpha < 0) alpha = 0;
      if (gray > 255) {
        // then assume this is actually a #FF8800
        return (alpha << 24) | (gray & 0xFFFFFF);
      } else {
        //if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
        return (alpha << 24) | (gray << 16) | (gray << 8) | gray;
      }
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


  static public int blendColor(int c1, int c2, int mode) {
    return PImage.blendColor(c1, c2, mode);
  }



  //////////////////////////////////////////////////////////////

  // MAIN


  /**
   * Set this sketch to communicate its state back to the PDE.
   * <p/>
   * This uses the stderr stream to write positions of the window
   * (so that it will be saved by the PDE for the next run) and
   * notify on quit. See more notes in the Worker class.
   */
//  public void setupExternalMessages() {
//
//    frame.addComponentListener(new ComponentAdapter() {
//        public void componentMoved(ComponentEvent e) {
//          Point where = ((Frame) e.getSource()).getLocation();
//          System.err.println(PApplet.EXTERNAL_MOVE + " " +
//                             where.x + " " + where.y);
//          System.err.flush();  // doesn't seem to help or hurt
//        }
//      });
//
//    frame.addWindowListener(new WindowAdapter() {
//        public void windowClosing(WindowEvent e) {
//          exit();  // don't quit, need to just shut everything down (0133)
//        }
//      });
//  }


  /**
   * Set up a listener that will fire proper component resize events
   * in cases where frame.setResizable(true) is called.
   */
//  public void setupFrameResizeListener() {
//    frame.addComponentListener(new ComponentAdapter() {
//
//        public void componentResized(ComponentEvent e) {
//          // Ignore bad resize events fired during setup to fix
//          // http://dev.processing.org/bugs/show_bug.cgi?id=341
//          // This should also fix the blank screen on Linux bug
//          // http://dev.processing.org/bugs/show_bug.cgi?id=282
//          if (frame.isResizable()) {
//            // might be multiple resize calls before visible (i.e. first
//            // when pack() is called, then when it's resized for use).
//            // ignore them because it's not the user resizing things.
//            Frame farm = (Frame) e.getComponent();
//            if (farm.isVisible()) {
//              Insets insets = farm.getInsets();
//              Dimension windowSize = farm.getSize();
//              int usableW = windowSize.width - insets.left - insets.right;
//              int usableH = windowSize.height - insets.top - insets.bottom;
//
//              // the ComponentListener in PApplet will handle calling size()
//              setBounds(insets.left, insets.top, usableW, usableH);
//            }
//          }
//        }
//      });
//  }


  /**
   * GIF image of the Processing logo.
   */
  static public final byte[] ICON_IMAGE = {
    71, 73, 70, 56, 57, 97, 16, 0, 16, 0, -77, 0, 0, 0, 0, 0, -1, -1, -1, 12,
    12, 13, -15, -15, -14, 45, 57, 74, 54, 80, 111, 47, 71, 97, 62, 88, 117,
    1, 14, 27, 7, 41, 73, 15, 52, 85, 2, 31, 55, 4, 54, 94, 18, 69, 109, 37,
    87, 126, -1, -1, -1, 33, -7, 4, 1, 0, 0, 15, 0, 44, 0, 0, 0, 0, 16, 0, 16,
    0, 0, 4, 122, -16, -107, 114, -86, -67, 83, 30, -42, 26, -17, -100, -45,
    56, -57, -108, 48, 40, 122, -90, 104, 67, -91, -51, 32, -53, 77, -78, -100,
    47, -86, 12, 76, -110, -20, -74, -101, 97, -93, 27, 40, 20, -65, 65, 48,
    -111, 99, -20, -112, -117, -123, -47, -105, 24, 114, -112, 74, 69, 84, 25,
    93, 88, -75, 9, 46, 2, 49, 88, -116, -67, 7, -19, -83, 60, 38, 3, -34, 2,
    66, -95, 27, -98, 13, 4, -17, 55, 33, 109, 11, 11, -2, -128, 121, 123, 62,
    91, 120, -128, 127, 122, 115, 102, 2, 119, 0, -116, -113, -119, 6, 102,
    121, -108, -126, 5, 18, 6, 4, -102, -101, -100, 114, 15, 17, 0, 59
  };


  /**
   * main() method for running this class from the command line.
   * <P>
   * <B>The options shown here are not yet finalized and will be
   * changing over the next several releases.</B>
   * <P>
   * The simplest way to turn and applet into an application is to
   * add the following code to your program:
   * <PRE>static public void main(String args[]) {
   *   PApplet.main(new String[] { "YourSketchName" });
   * }</PRE>
   * This will properly launch your applet from a double-clickable
   * .jar or from the command line.
   * <PRE>
   * Parameters useful for launching or also used by the PDE:
   *
   * --location=x,y        upper-lefthand corner of where the applet
   *                       should appear on screen. if not used,
   *                       the default is to center on the main screen.
   *
   * --present             put the applet into full screen presentation
   *                       mode. requires java 1.4 or later.
   *
   * --exclusive           use full screen exclusive mode when presenting.
   *                       disables new windows or interaction with other
   *                       monitors, this is like a "game" mode.
   *
   * --hide-stop           use to hide the stop button in situations where
   *                       you don't want to allow users to exit. also
   *                       see the FAQ on information for capturing the ESC
   *                       key when running in presentation mode.
   *
   * --stop-color=#xxxxxx  color of the 'stop' text used to quit an
   *                       sketch when it's in present mode.
   *
   * --bgcolor=#xxxxxx     background color of the window.
   *
   * --sketch-path         location of where to save files from functions
   *                       like saveStrings() or saveFrame(). defaults to
   *                       the folder that the java application was
   *                       launched from, which means if this isn't set by
   *                       the pde, everything goes into the same folder
   *                       as processing.exe.
   *
   * --display=n           set what display should be used by this applet.
   *                       displays are numbered starting from 1.
   *
   * Parameters used by Processing when running via the PDE
   *
   * --external            set when the applet is being used by the PDE
   *
   * --editor-location=x,y position of the upper-lefthand corner of the
   *                       editor window, for placement of applet window
   * </PRE>
   */
  static public void main(String args[]) {
    // just do a no-op for now
  }

//  static public void main(String args[]) {
//    // Disable abyssmally slow Sun renderer on OS X 10.5.
//    if (platform == MACOSX) {
//      // Only run this on OS X otherwise it can cause a permissions error.
//      // http://dev.processing.org/bugs/show_bug.cgi?id=976
//      System.setProperty("apple.awt.graphics.UseQuartz", "true");
//    }
//
//    // This doesn't do anything.
////    if (platform == WINDOWS) {
////      // For now, disable the D3D renderer on Java 6u10 because
////      // it causes problems with Present mode.
////      // http://dev.processing.org/bugs/show_bug.cgi?id=1009
////      System.setProperty("sun.java2d.d3d", "false");
////    }
//
//    if (args.length < 1) {
//      System.err.println("Usage: PApplet <appletname>");
//      System.err.println("For additional options, " +
//                         "see the Javadoc for PApplet");
//      System.exit(1);
//    }
//
//    try {
//      boolean external = false;
//      int[] location = null;
//      int[] editorLocation = null;
//
//      String name = null;
//      boolean present = false;
//      boolean exclusive = false;
//      Color backgroundColor = Color.BLACK;
//      Color stopColor = Color.GRAY;
//      GraphicsDevice displayDevice = null;
//      boolean hideStop = false;
//
//      String param = null, value = null;
//
//      // try to get the user folder. if running under java web start,
//      // this may cause a security exception if the code is not signed.
//      // http://processing.org/discourse/yabb_beta/YaBB.cgi?board=Integrate;action=display;num=1159386274
//      String folder = null;
//      try {
//        folder = System.getProperty("user.dir");
//      } catch (Exception e) { }
//
//      int argIndex = 0;
//      while (argIndex < args.length) {
//        int equals = args[argIndex].indexOf('=');
//        if (equals != -1) {
//          param = args[argIndex].substring(0, equals);
//          value = args[argIndex].substring(equals + 1);
//
//          if (param.equals(ARGS_EDITOR_LOCATION)) {
//            external = true;
//            editorLocation = parseInt(split(value, ','));
//
//          } else if (param.equals(ARGS_DISPLAY)) {
//            int deviceIndex = Integer.parseInt(value) - 1;
//
//            //DisplayMode dm = device.getDisplayMode();
//            //if ((dm.getWidth() == 1024) && (dm.getHeight() == 768)) {
//
//            GraphicsEnvironment environment =
//              GraphicsEnvironment.getLocalGraphicsEnvironment();
//            GraphicsDevice devices[] = environment.getScreenDevices();
//            if ((deviceIndex >= 0) && (deviceIndex < devices.length)) {
//              displayDevice = devices[deviceIndex];
//            } else {
//              System.err.println("Display " + value + " does not exist, " +
//                                 "using the default display instead.");
//            }
//
//          } else if (param.equals(ARGS_BGCOLOR)) {
//            if (value.charAt(0) == '#') value = value.substring(1);
//            backgroundColor = new Color(Integer.parseInt(value, 16));
//
//          } else if (param.equals(ARGS_STOP_COLOR)) {
//            if (value.charAt(0) == '#') value = value.substring(1);
//            stopColor = new Color(Integer.parseInt(value, 16));
//
//          } else if (param.equals(ARGS_SKETCH_FOLDER)) {
//            folder = value;
//
//          } else if (param.equals(ARGS_LOCATION)) {
//            location = parseInt(split(value, ','));
//          }
//
//        } else {
//          if (args[argIndex].equals(ARGS_PRESENT)) {
//            present = true;
//
//          } else if (args[argIndex].equals(ARGS_EXCLUSIVE)) {
//            exclusive = true;
//
//          } else if (args[argIndex].equals(ARGS_HIDE_STOP)) {
//            hideStop = true;
//
//          } else if (args[argIndex].equals(ARGS_EXTERNAL)) {
//            external = true;
//
//          } else {
//            name = args[argIndex];
//            break;
//          }
//        }
//        argIndex++;
//      }
//
//      // Set this property before getting into any GUI init code
//      //System.setProperty("com.apple.mrj.application.apple.menu.about.name", name);
//      // This )*)(*@#$ Apple crap don't work no matter where you put it
//      // (static method of the class, at the top of main, wherever)
//
//      if (displayDevice == null) {
//        GraphicsEnvironment environment =
//          GraphicsEnvironment.getLocalGraphicsEnvironment();
//        displayDevice = environment.getDefaultScreenDevice();
//      }
//
//      Frame frame = new Frame(displayDevice.getDefaultConfiguration());
//      /*
//      Frame frame = null;
//      if (displayDevice != null) {
//        frame = new Frame(displayDevice.getDefaultConfiguration());
//      } else {
//        frame = new Frame();
//      }
//      */
//      //Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
//
//      // remove the grow box by default
//      // users who want it back can call frame.setResizable(true)
//      frame.setResizable(false);
//
//      // Set the trimmings around the image
//      Image image = Toolkit.getDefaultToolkit().createImage(ICON_IMAGE);
//      frame.setIconImage(image);
//      frame.setTitle(name);
//
////    Class c = Class.forName(name);
//      Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(name);
//      final PApplet applet = (PApplet) c.newInstance();
//
//      // these are needed before init/start
//      applet.frame = frame;
//      applet.sketchPath = folder;
//      applet.args = PApplet.subset(args, 1);
//      applet.external = external;
//
//      // Need to save the window bounds at full screen,
//      // because pack() will cause the bounds to go to zero.
//      // http://dev.processing.org/bugs/show_bug.cgi?id=923
//      Rectangle fullScreenRect = null;
//
//      // For 0149, moving this code (up to the pack() method) before init().
//      // For OpenGL (and perhaps other renderers in the future), a peer is
//      // needed before a GLDrawable can be created. So pack() needs to be
//      // called on the Frame before applet.init(), which itself calls size(),
//      // and launches the Thread that will kick off setup().
//      // http://dev.processing.org/bugs/show_bug.cgi?id=891
//      // http://dev.processing.org/bugs/show_bug.cgi?id=908
//      if (present) {
//        frame.setUndecorated(true);
//        frame.setBackground(backgroundColor);
//        if (exclusive) {
//          displayDevice.setFullScreenWindow(frame);
//          fullScreenRect = frame.getBounds();
//        } else {
//          DisplayMode mode = displayDevice.getDisplayMode();
//          fullScreenRect = new Rectangle(0, 0, mode.getWidth(), mode.getHeight());
//          frame.setBounds(fullScreenRect);
//          frame.setVisible(true);
//        }
//      }
//      frame.setLayout(null);
//      frame.add(applet);
//      if (present) {
//        frame.invalidate();
//      } else {
//        frame.pack();
//      }
//      // insufficient, places the 100x100 sketches offset strangely
//      //frame.validate();
//
//      applet.init();
//
//      // Wait until the applet has figured out its width.
//      // In a static mode app, this will be after setup() has completed,
//      // and the empty draw() has set "finished" to true.
//      // TODO make sure this won't hang if the applet has an exception.
//      while (applet.defaultSize && !applet.finished) {
//        //System.out.println("default size");
//        try {
//          Thread.sleep(5);
//
//        } catch (InterruptedException e) {
//          //System.out.println("interrupt");
//        }
//      }
//      //println("not default size " + applet.width + " " + applet.height);
//      //println("  (g width/height is " + applet.g.width + "x" + applet.g.height + ")");
//
//      if (present) {
//        // After the pack(), the screen bounds are gonna be 0s
//        frame.setBounds(fullScreenRect);
//        applet.setBounds((fullScreenRect.width - applet.width) / 2,
//                         (fullScreenRect.height - applet.height) / 2,
//                         applet.width, applet.height);
//
//        if (!hideStop) {
//          Label label = new Label("stop");
//          label.setForeground(stopColor);
//          label.addMouseListener(new MouseAdapter() {
//              public void mousePressed(MouseEvent e) {
//                System.exit(0);
//              }
//            });
//          frame.add(label);
//
//          Dimension labelSize = label.getPreferredSize();
//          // sometimes shows up truncated on mac
//          //System.out.println("label width is " + labelSize.width);
//          labelSize = new Dimension(100, labelSize.height);
//          label.setSize(labelSize);
//          label.setLocation(20, fullScreenRect.height - labelSize.height - 20);
//        }
//
//        // not always running externally when in present mode
//        if (external) {
//          applet.setupExternalMessages();
//        }
//
//      } else {  // if not presenting
//        // can't do pack earlier cuz present mode don't like it
//        // (can't go full screen with a frame after calling pack)
////        frame.pack();  // get insets. get more.
//        Insets insets = frame.getInsets();
//
//        int windowW = Math.max(applet.width, MIN_WINDOW_WIDTH) +
//          insets.left + insets.right;
//        int windowH = Math.max(applet.height, MIN_WINDOW_HEIGHT) +
//          insets.top + insets.bottom;
//
//        frame.setSize(windowW, windowH);
//
//        if (location != null) {
//          // a specific location was received from PdeRuntime
//          // (applet has been run more than once, user placed window)
//          frame.setLocation(location[0], location[1]);
//
//        } else if (external) {
//          int locationX = editorLocation[0] - 20;
//          int locationY = editorLocation[1];
//
//          if (locationX - windowW > 10) {
//            // if it fits to the left of the window
//            frame.setLocation(locationX - windowW, locationY);
//
//          } else {  // doesn't fit
//            // if it fits inside the editor window,
//            // offset slightly from upper lefthand corner
//            // so that it's plunked inside the text area
//            locationX = editorLocation[0] + 66;
//            locationY = editorLocation[1] + 66;
//
//            if ((locationX + windowW > applet.screen.width - 33) ||
//                (locationY + windowH > applet.screen.height - 33)) {
//              // otherwise center on screen
//              locationX = (applet.screen.width - windowW) / 2;
//              locationY = (applet.screen.height - windowH) / 2;
//            }
//            frame.setLocation(locationX, locationY);
//          }
//        } else {  // just center on screen
//          frame.setLocation((applet.screen.width - applet.width) / 2,
//                            (applet.screen.height - applet.height) / 2);
//        }
//
////        frame.setLayout(null);
////        frame.add(applet);
//
//        if (backgroundColor == Color.black) {  //BLACK) {
//          // this means no bg color unless specified
//          backgroundColor = SystemColor.control;
//        }
//        frame.setBackground(backgroundColor);
//
//        int usableWindowH = windowH - insets.top - insets.bottom;
//        applet.setBounds((windowW - applet.width)/2,
//                         insets.top + (usableWindowH - applet.height)/2,
//                         applet.width, applet.height);
//
//        if (external) {
//          applet.setupExternalMessages();
//
//        } else {  // !external
//          frame.addWindowListener(new WindowAdapter() {
//              public void windowClosing(WindowEvent e) {
//                System.exit(0);
//              }
//            });
//        }
//
//        // handle frame resizing events
//        applet.setupFrameResizeListener();
//
//        // all set for rockin
//        if (applet.displayable()) {
//          frame.setVisible(true);
//        }
//      }
//
//      applet.requestFocus(); // ask for keydowns
//      //System.out.println("exiting main()");
//
//    } catch (Exception e) {
//      e.printStackTrace();
//      System.exit(1);
//    }
//  }


  //////////////////////////////////////////////////////////////


  /**
   * Begin recording to a new renderer of the specified type, using the width
   * and height of the main drawing surface.
   */
//  public PGraphics beginRecord(String renderer, String filename) {
//    filename = insertFrame(filename);
//    PGraphics rec = createGraphics(width, height, renderer, filename);
//    beginRecord(rec);
//    return rec;
//  }


  /**
   * Begin recording (echoing) commands to the specified PGraphics object.
   */
//  public void beginRecord(PGraphics recorder) {
//    PGraphics.showMethodWarning("beginRecord");
//  }


//  public void endRecord() {
//    PGraphics.showMethodWarning("endRecord");
//  }


  /**
   * Begin recording raw shape data to a renderer of the specified type,
   * using the width and height of the main drawing surface.
   *
   * If hashmarks (###) are found in the filename, they'll be replaced
   * by the current frame number (frameCount).
   */
//  public PGraphics beginRaw(String renderer, String filename) {
//    filename = insertFrame(filename);
//    PGraphics rec = createGraphics(width, height, renderer, filename);
//    g.beginRaw(rec);
//    return rec;
//  }


  /**
   * Begin recording raw shape data to the specified renderer.
   *
   * This simply echoes to g.beginRaw(), but since is placed here (rather than
   * generated by preproc.pl) for clarity and so that it doesn't echo the
   * command should beginRecord() be in use.
   */
//  public void beginRaw(PGraphics rawGraphics) {
//    g.beginRaw(rawGraphics);
//  }


  /**
   * Stop recording raw shape data to the specified renderer.
   *
   * This simply echoes to g.beginRaw(), but since is placed here (rather than
   * generated by preproc.pl) for clarity and so that it doesn't echo the
   * command should beginRecord() be in use.
   */
//  public void endRaw() {
//    g.endRaw();
//  }


  //////////////////////////////////////////////////////////////


  /**
   * Override the g.pixels[] function to set the pixels[] array
   * that's part of the PApplet object. Allows the use of
   * pixels[] in the code, rather than g.pixels[].
   */
  public void loadPixels() {
    g.loadPixels();
    pixels = g.pixels;
  }


  public void updatePixels() {
    g.updatePixels();
  }


  public void updatePixels(int x1, int y1, int x2, int y2) {
    g.updatePixels(x1, y1, x2, y2);
  }


  private void tellPDE(final String message) {
    Log.i(getComponentName().getPackageName(), "PROCESSING " + message);
  }


  @Override
  protected void onStart() {
    tellPDE("onStart");
    super.onStart();
  }


  @Override
  protected void onStop() {
    tellPDE("onStop");
    super.onStop();
  }


  //////////////////////////////////////////////////////////////

  // everything below this line is automatically generated. no touch.
  // public functions for processing.core


  /**
   * Store data of some kind for the renderer that requires extra metadata of
   * some kind. Usually this is a renderer-specific representation of the
   * image data, for instance a BufferedImage with tint() settings applied for
   * PGraphicsJava2D, or resized image data and OpenGL texture indices for
   * PGraphicsOpenGL.
   * @param renderer The PGraphics renderer associated to the image
   * @param storage The metadata required by the renderer
   */
  public void setCache(PImage image, Object storage) {
    g.setCache(image, storage);
  }


  /**
   * Get cache storage data for the specified renderer. Because each renderer
   * will cache data in different formats, it's necessary to store cache data
   * keyed by the renderer object. Otherwise, attempting to draw the same
   * image to both a PGraphicsJava2D and a PGraphicsOpenGL will cause errors.
   * @param renderer The PGraphics renderer associated to the image
   * @return metadata stored for the specified renderer
   */
  public Object getCache(PImage image) {
    return g.getCache(image);
  }


  /**
   * Remove information associated with this renderer from the cache, if any.
   * @param renderer The PGraphics renderer whose cache data should be removed
   */
  public void removeCache(PImage image) {
    g.removeCache(image);
  }


  public void flush() {
    g.flush();
  }


  public PGL beginPGL() {
    return g.beginPGL();
  }


  public void endPGL() {
    g.endPGL();
  }


  /**
   * Enable a hint option.
   * <P>
   * For the most part, hints are temporary api quirks,
   * for which a proper api hasn't been properly worked out.
   * for instance SMOOTH_IMAGES existed because smooth()
   * wasn't yet implemented, but it will soon go away.
   * <P>
   * They also exist for obscure features in the graphics
   * engine, like enabling/disabling single pixel lines
   * that ignore the zbuffer, the way they do in alphabot.
   * <P>
   * Current hint options:
   * <UL>
   * <LI><TT>DISABLE_DEPTH_TEST</TT> -
   * turns off the z-buffer in the P3D or OPENGL renderers.
   * </UL>
   */
  public void hint(int which) {
    g.hint(which);
  }


  /**
   * Start a new shape of type POLYGON
   */
  public void beginShape() {
    g.beginShape();
  }


  /**
   * Start a new shape.
   * <P>
   * <B>Differences between beginShape() and line() and point() methods.</B>
   * <P>
   * beginShape() is intended to be more flexible at the expense of being
   * a little more complicated to use. it handles more complicated shapes
   * that can consist of many connected lines (so you get joins) or lines
   * mixed with curves.
   * <P>
   * The line() and point() command are for the far more common cases
   * (particularly for our audience) that simply need to draw a line
   * or a point on the screen.
   * <P>
   * From the code side of things, line() may or may not call beginShape()
   * to do the drawing. In the beta code, they do, but in the alpha code,
   * they did not. they might be implemented one way or the other depending
   * on tradeoffs of runtime efficiency vs. implementation efficiency &mdash
   * meaning the speed that things run at vs. the speed it takes me to write
   * the code and maintain it. for beta, the latter is most important so
   * that's how things are implemented.
   */
  public void beginShape(int kind) {
    g.beginShape(kind);
  }


  /**
   * Sets whether the upcoming vertex is part of an edge.
   * Equivalent to glEdgeFlag(), for people familiar with OpenGL.
   */
  public void edge(boolean edge) {
    g.edge(edge);
  }


  /**
   * Sets the current normal vector. Only applies with 3D rendering
   * and inside a beginShape/endShape block.
   * <P/>
   * This is for drawing three dimensional shapes and surfaces,
   * allowing you to specify a vector perpendicular to the surface
   * of the shape, which determines how lighting affects it.
   * <P/>
   * For people familiar with OpenGL, this function is basically
   * identical to glNormal3f().
   */
  public void normal(float nx, float ny, float nz) {
    g.normal(nx, ny, nz);
  }


  /**
   * Set texture mode to either to use coordinates based on the IMAGE
   * (more intuitive for new users) or NORMALIZED (better for advanced chaps)
   */
  public void textureMode(int mode) {
    g.textureMode(mode);
  }


  public void textureWrap(int wrap) {
    g.textureWrap(wrap);
  }


  /**
   * Set texture image for current shape.
   * Needs to be called between @see beginShape and @see endShape
   *
   * @param image reference to a PImage object
   */
  public void texture(PImage image) {
    g.texture(image);
  }


  /**
   * Removes texture image for current shape.
   * Needs to be called between @see beginShape and @see endShape
   *
   */
  public void noTexture() {
    g.noTexture();
  }


  public void vertex(float x, float y) {
    g.vertex(x, y);
  }


  public void vertex(float x, float y, float z) {
    g.vertex(x, y, z);
  }


  /**
   * Used by renderer subclasses or PShape to efficiently pass in already
   * formatted vertex information.
   * @param v vertex parameters, as a float array of length VERTEX_FIELD_COUNT
   */
  public void vertex(float[] v) {
    g.vertex(v);
  }


  public void vertex(float x, float y, float u, float v) {
    g.vertex(x, y, u, v);
  }


  public void vertex(float x, float y, float z, float u, float v) {
    g.vertex(x, y, z, u, v);
  }


  /** This feature is in testing, do not use or rely upon its implementation */
  public void breakShape() {
    g.breakShape();
  }


  public void beginContour() {
    g.beginContour();
  }


  public void endContour() {
    g.endContour();
  }


  public void endShape() {
    g.endShape();
  }


  public void endShape(int mode) {
    g.endShape(mode);
  }


  public void clip(float a, float b, float c, float d) {
    g.clip(a, b, c, d);
  }


  public void noClip() {
    g.noClip();
  }


  public void blendMode(int mode) {
    g.blendMode(mode);
  }


  public PShape loadShape(String filename) {
    return g.loadShape(filename);
  }


  public PShape createShape(PShape source) {
    return g.createShape(source);
  }


  public PShape createShape() {
    return g.createShape();
  }


  public PShape createShape(int type) {
    return g.createShape(type);
  }


  public PShape createShape(int kind, float... p) {
    return g.createShape(kind, p);
  }


  public PShader loadShader(String fragFilename) {
    return g.loadShader(fragFilename);
  }


  public PShader loadShader(String fragFilename, String vertFilename) {
    return g.loadShader(fragFilename, vertFilename);
  }


  public void shader(PShader shader) {
    g.shader(shader);
  }


  public void shader(PShader shader, int kind) {
    g.shader(shader, kind);
  }


  public void resetShader() {
    g.resetShader();
  }


  public void resetShader(int kind) {
    g.resetShader(kind);
  }


  public PShader getShader(int kind) {
    return g.getShader(kind);
  }


  public void filter(PShader shader) {
    g.filter(shader);
  }


  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
    g.bezierVertex(x2, y2, x3, y3, x4, y4);
  }


  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    g.bezierVertex(x2, y2, z2, x3, y3, z3, x4, y4, z4);
  }


  public void quadraticVertex(float cx, float cy,
                              float x3, float y3) {
    g.quadraticVertex(cx, cy, x3, y3);
  }


  public void quadraticVertex(float cx, float cy, float cz,
                              float x3, float y3, float z3) {
    g.quadraticVertex(cx, cy, cz, x3, y3, z3);
  }


  public void curveVertex(float x, float y) {
    g.curveVertex(x, y);
  }


  public void curveVertex(float x, float y, float z) {
    g.curveVertex(x, y, z);
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


  public void rect(float a, float b, float c, float d) {
    g.rect(a, b, c, d);
  }


  public void rect(float a, float b, float c, float d, float r) {
    g.rect(a, b, c, d, r);
  }


  public void rect(float a, float b, float c, float d,
                   float tl, float tr, float br, float bl) {
    g.rect(a, b, c, d, tl, tr, br, bl);
  }


  public void ellipseMode(int mode) {
    g.ellipseMode(mode);
  }


  public void ellipse(float a, float b, float c, float d) {
    g.ellipse(a, b, c, d);
  }


  /**
   * Identical parameters and placement to ellipse,
   * but draws only an arc of that ellipse.
   * <p/>
   * start and stop are always radians because angleMode() was goofy.
   * ellipseMode() sets the placement.
   * <p/>
   * also tries to be smart about start < stop.
   */
  public void arc(float a, float b, float c, float d,
                  float start, float stop) {
    g.arc(a, b, c, d, start, stop);
  }


  public void arc(float a, float b, float c, float d,
                  float start, float stop, int mode) {
    g.arc(a, b, c, d, start, stop, mode);
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


  /**
   * Set the detail level for approximating a sphere. The ures and vres params
   * control the horizontal and vertical resolution.
   *
   * Code for sphereDetail() submitted by toxi [031031].
   * Code for enhanced u/v version from davbol [080801].
   */
  public void sphereDetail(int ures, int vres) {
    g.sphereDetail(ures, vres);
  }


  /**
   * Draw a sphere with radius r centered at coordinate 0, 0, 0.
   * <P>
   * Implementation notes:
   * <P>
   * cache all the points of the sphere in a static array
   * top and bottom are just a bunch of triangles that land
   * in the center point
   * <P>
   * sphere is a series of concentric circles who radii vary
   * along the shape, based on, er.. cos or something
   * <PRE>
   * [toxi 031031] new sphere code. removed all multiplies with
   * radius, as scale() will take care of that anyway
   *
   * [toxi 031223] updated sphere code (removed modulos)
   * and introduced sphereAt(x,y,z,r)
   * to avoid additional translate()'s on the user/sketch side
   *
   * [davbol 080801] now using separate sphereDetailU/V
   * </PRE>
   */
  public void sphere(float r) {
    g.sphere(r);
  }


  /**
   * Evalutes quadratic bezier at point t for points a, b, c, d.
   * t varies between 0 and 1, and a and d are the on curve points,
   * b and c are the control points. this can be done once with the
   * x coordinates and a second time with the y coordinates to get
   * the location of a bezier curve at t.
   * <P>
   * For instance, to convert the following example:<PRE>
   * stroke(255, 102, 0);
   * line(85, 20, 10, 10);
   * line(90, 90, 15, 80);
   * stroke(0, 0, 0);
   * bezier(85, 20, 10, 10, 90, 90, 15, 80);
   *
   * // draw it in gray, using 10 steps instead of the default 20
   * // this is a slower way to do it, but useful if you need
   * // to do things with the coordinates at each step
   * stroke(128);
   * beginShape(LINE_STRIP);
   * for (int i = 0; i <= 10; i++) {
   *   float t = i / 10.0f;
   *   float x = bezierPoint(85, 10, 90, 15, t);
   *   float y = bezierPoint(20, 10, 90, 80, t);
   *   vertex(x, y);
   * }
   * endShape();</PRE>
   */
  public float bezierPoint(float a, float b, float c, float d, float t) {
    return g.bezierPoint(a, b, c, d, t);
  }


  /**
   * Provide the tangent at the given point on the bezier curve.
   * Fix from davbol for 0136.
   */
  public float bezierTangent(float a, float b, float c, float d, float t) {
    return g.bezierTangent(a, b, c, d, t);
  }


  public void bezierDetail(int detail) {
    g.bezierDetail(detail);
  }


  /**
   * Draw a cubic bezier curve. The first and last points are
   * the on-curve points. The middle two are the 'control' points,
   * or 'handles' in an application like Illustrator.
   * <P>
   * Identical to typing:
   * <PRE>beginShape();
   * vertex(x1, y1);
   * bezierVertex(x2, y2, x3, y3, x4, y4);
   * endShape();
   * </PRE>
   * In Postscript-speak, this would be:
   * <PRE>moveto(x1, y1);
   * curveto(x2, y2, x3, y3, x4, y4);</PRE>
   * If you were to try and continue that curve like so:
   * <PRE>curveto(x5, y5, x6, y6, x7, y7);</PRE>
   * This would be done in processing by adding these statements:
   * <PRE>bezierVertex(x5, y5, x6, y6, x7, y7)
   * </PRE>
   * To draw a quadratic (instead of cubic) curve,
   * use the control point twice by doubling it:
   * <PRE>bezier(x1, y1, cx, cy, cx, cy, x2, y2);</PRE>
   */
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


  /**
   * Get a location along a catmull-rom curve segment.
   *
   * @param t Value between zero and one for how far along the segment
   */
  public float curvePoint(float a, float b, float c, float d, float t) {
    return g.curvePoint(a, b, c, d, t);
  }


  /**
   * Calculate the tangent at a t value (0..1) on a Catmull-Rom curve.
   * Code thanks to Dave Bollinger (Bug #715)
   */
  public float curveTangent(float a, float b, float c, float d, float t) {
    return g.curveTangent(a, b, c, d, t);
  }


  public void curveDetail(int detail) {
    g.curveDetail(detail);
  }


  public void curveTightness(float tightness) {
    g.curveTightness(tightness);
  }


  /**
   * Draws a segment of Catmull-Rom curve.
   * <P>
   * As of 0070, this function no longer doubles the first and
   * last points. The curves are a bit more boring, but it's more
   * mathematically correct, and properly mirrored in curvePoint().
   * <P>
   * Identical to typing out:<PRE>
   * beginShape();
   * curveVertex(x1, y1);
   * curveVertex(x2, y2);
   * curveVertex(x3, y3);
   * curveVertex(x4, y4);
   * endShape();
   * </PRE>
   */
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


  /**
   * If true in PImage, use bilinear interpolation for copy()
   * operations. When inherited by PGraphics, also controls shapes.
   */
  public void smooth() {
    g.smooth();
  }


  public void smooth(int level) {
    g.smooth(level);
  }


  /**
   * Disable smoothing. See smooth().
   */
  public void noSmooth() {
    g.noSmooth();
  }


  /**
   * The mode can only be set to CORNERS, CORNER, and CENTER.
   * <p/>
   * Support for CENTER was added in release 0146.
   */
  public void imageMode(int mode) {
    g.imageMode(mode);
  }


  public void image(PImage image, float x, float y) {
    g.image(image, x, y);
  }


  public void image(PImage image, float x, float y, float c, float d) {
    g.image(image, x, y, c, d);
  }


  /**
   * Draw an image(), also specifying u/v coordinates.
   * In this method, the  u, v coordinates are always based on image space
   * location, regardless of the current textureMode().
   */
  public void image(PImage image,
                    float a, float b, float c, float d,
                    int u1, int v1, int u2, int v2) {
    g.image(image, a, b, c, d, u1, v1, u2, v2);
  }


  /**
   * Set the orientation for the shape() command (like imageMode() or rectMode()).
   * @param mode Either CORNER, CORNERS, or CENTER.
   */
  public void shapeMode(int mode) {
    g.shapeMode(mode);
  }


  public void shape(PShape shape) {
    g.shape(shape);
  }


  /**
   * Convenience method to draw at a particular location.
   */
  public void shape(PShape shape, float x, float y) {
    g.shape(shape, x, y);
  }


  public void shape(PShape shape, float x, float y, float c, float d) {
    g.shape(shape, x, y, c, d);
  }


  /**
   * Sets the alignment of the text to one of LEFT, CENTER, or RIGHT.
   * This will also reset the vertical text alignment to BASELINE.
   */
  public void textAlign(int align) {
    g.textAlign(align);
  }


  /**
   * Sets the horizontal and vertical alignment of the text. The horizontal
   * alignment can be one of LEFT, CENTER, or RIGHT. The vertical alignment
   * can be TOP, BOTTOM, CENTER, or the BASELINE (the default).
   */
  public void textAlign(int alignX, int alignY) {
    g.textAlign(alignX, alignY);
  }


  /**
   * Returns the ascent of the current font at the current size.
   * This is a method, rather than a variable inside the PGraphics object
   * because it requires calculation.
   */
  public float textAscent() {
    return g.textAscent();
  }


  /**
   * Returns the descent of the current font at the current size.
   * This is a method, rather than a variable inside the PGraphics object
   * because it requires calculation.
   */
  public float textDescent() {
    return g.textDescent();
  }


  /**
   * Sets the current font. The font's size will be the "natural"
   * size of this font (the size that was set when using "Create Font").
   * The leading will also be reset.
   */
  public void textFont(PFont which) {
    g.textFont(which);
  }


  /**
   * Useful function to set the font and size at the same time.
   */
  public void textFont(PFont which, float size) {
    g.textFont(which, size);
  }


  /**
   * Set the text leading to a specific value. If using a custom
   * value for the text leading, you'll have to call textLeading()
   * again after any calls to textSize().
   */
  public void textLeading(float leading) {
    g.textLeading(leading);
  }


  /**
   * Sets the text rendering/placement to be either SCREEN (direct
   * to the screen, exact coordinates, only use the font's original size)
   * or MODEL (the default, where text is manipulated by translate() and
   * can have a textSize). The text size cannot be set when using
   * textMode(SCREEN), because it uses the pixels directly from the font.
   */
  public void textMode(int mode) {
    g.textMode(mode);
  }


  /**
   * Sets the text size, also resets the value for the leading.
   */
  public void textSize(float size) {
    g.textSize(size);
  }


  public float textWidth(char c) {
    return g.textWidth(c);
  }


  /**
   * Return the width of a line of text. If the text has multiple
   * lines, this returns the length of the longest line.
   */
  public float textWidth(String str) {
    return g.textWidth(str);
  }


  /**
   * Draw a single character on screen.
   * Extremely slow when used with textMode(SCREEN) and Java 2D,
   * because loadPixels has to be called first and updatePixels last.
   */
  public void text(char c, float x, float y) {
    g.text(c, x, y);
  }


  /**
   * Draw a single character on screen (with a z coordinate)
   */
  public void text(char c, float x, float y, float z) {
    g.text(c, x, y, z);
  }


  /**
   * Draw a chunk of text.
   * Newlines that are \n (Unix newline or linefeed char, ascii 10)
   * are honored, but \r (carriage return, Windows and Mac OS) are
   * ignored.
   */
  public void text(String str, float x, float y) {
    g.text(str, x, y);
  }


  /**
   * Same as above but with a z coordinate.
   */
  public void text(String str, float x, float y, float z) {
    g.text(str, x, y, z);
  }


  /**
   * Draw text in a box that is constrained to a particular size.
   * The current rectMode() determines what the coordinates mean
   * (whether x1/y1/x2/y2 or x/y/w/h).
   * <P/>
   * Note that the x,y coords of the start of the box
   * will align with the *ascent* of the text, not the baseline,
   * as is the case for the other text() functions.
   * <P/>
   * Newlines that are \n (Unix newline or linefeed char, ascii 10)
   * are honored, and \r (carriage return, Windows and Mac OS) are
   * ignored.
   */
  public void text(String str, float x1, float y1, float x2, float y2) {
    g.text(str, x1, y1, x2, y2);
  }


  public void text(int num, float x, float y) {
    g.text(num, x, y);
  }


  public void text(int num, float x, float y, float z) {
    g.text(num, x, y, z);
  }


  /**
   * This does a basic number formatting, to avoid the
   * generally ugly appearance of printing floats.
   * Users who want more control should use their own nf() cmmand,
   * or if they want the long, ugly version of float,
   * use String.valueOf() to convert the float to a String first.
   */
  public void text(float num, float x, float y) {
    g.text(num, x, y);
  }


  public void text(float num, float x, float y, float z) {
    g.text(num, x, y, z);
  }


  /**
   * Push a copy of the current transformation matrix onto the stack.
   */
  public void pushMatrix() {
    g.pushMatrix();
  }


  /**
   * Replace the current transformation matrix with the top of the stack.
   */
  public void popMatrix() {
    g.popMatrix();
  }


  /**
   * Translate in X and Y.
   */
  public void translate(float tx, float ty) {
    g.translate(tx, ty);
  }


  /**
   * Translate in X, Y, and Z.
   */
  public void translate(float tx, float ty, float tz) {
    g.translate(tx, ty, tz);
  }


  /**
   * Two dimensional rotation.
   *
   * Same as rotateZ (this is identical to a 3D rotation along the z-axis)
   * but included for clarity. It'd be weird for people drawing 2D graphics
   * to be using rotateZ. And they might kick our a-- for the confusion.
   *
   * <A HREF="http://www.xkcd.com/c184.html">Additional background</A>.
   */
  public void rotate(float angle) {
    g.rotate(angle);
  }


  /**
   * Rotate around the X axis.
   */
  public void rotateX(float angle) {
    g.rotateX(angle);
  }


  /**
   * Rotate around the Y axis.
   */
  public void rotateY(float angle) {
    g.rotateY(angle);
  }


  /**
   * Rotate around the Z axis.
   *
   * The functions rotate() and rotateZ() are identical, it's just that it make
   * sense to have rotate() and then rotateX() and rotateY() when using 3D;
   * nor does it make sense to use a function called rotateZ() if you're only
   * doing things in 2D. so we just decided to have them both be the same.
   */
  public void rotateZ(float angle) {
    g.rotateZ(angle);
  }


  /**
   * Rotate about a vector in space. Same as the glRotatef() function.
   */
  public void rotate(float angle, float vx, float vy, float vz) {
    g.rotate(angle, vx, vy, vz);
  }


  /**
   * Scale in all dimensions.
   */
  public void scale(float s) {
    g.scale(s);
  }


  /**
   * Scale in X and Y. Equivalent to scale(sx, sy, 1).
   *
   * Not recommended for use in 3D, because the z-dimension is just
   * scaled by 1, since there's no way to know what else to scale it by.
   */
  public void scale(float sx, float sy) {
    g.scale(sx, sy);
  }


  /**
   * Scale in X, Y, and Z.
   */
  public void scale(float x, float y, float z) {
    g.scale(x, y, z);
  }


  /**
   * Shear along X axis
   */
  public void shearX(float angle) {
    g.shearX(angle);
  }


  /**
   * Skew along Y axis
   */
  public void shearY(float angle) {
    g.shearY(angle);
  }


  /**
   * Set the current transformation matrix to identity.
   */
  public void resetMatrix() {
    g.resetMatrix();
  }


  public void applyMatrix(PMatrix source) {
    g.applyMatrix(source);
  }


  public void applyMatrix(PMatrix2D source) {
    g.applyMatrix(source);
  }


  /**
   * Apply a 3x2 affine transformation matrix.
   */
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    g.applyMatrix(n00, n01, n02, n10, n11, n12);
  }


  public void applyMatrix(PMatrix3D source) {
    g.applyMatrix(source);
  }


  /**
   * Apply a 4x4 transformation matrix.
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    g.applyMatrix(n00, n01, n02, n03, n10, n11, n12, n13, n20, n21, n22, n23, n30, n31, n32, n33);
  }


  public PMatrix getMatrix() {
    return g.getMatrix();
  }


  /**
   * Copy the current transformation matrix into the specified target.
   * Pass in null to create a new matrix.
   */
  public PMatrix2D getMatrix(PMatrix2D target) {
    return g.getMatrix(target);
  }


  /**
   * Copy the current transformation matrix into the specified target.
   * Pass in null to create a new matrix.
   */
  public PMatrix3D getMatrix(PMatrix3D target) {
    return g.getMatrix(target);
  }


  /**
   * Set the current transformation matrix to the contents of another.
   */
  public void setMatrix(PMatrix source) {
    g.setMatrix(source);
  }


  /**
   * Set the current transformation to the contents of the specified source.
   */
  public void setMatrix(PMatrix2D source) {
    g.setMatrix(source);
  }


  /**
   * Set the current transformation to the contents of the specified source.
   */
  public void setMatrix(PMatrix3D source) {
    g.setMatrix(source);
  }


  /**
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {
    g.printMatrix();
  }


  public void beginCamera() {
    g.beginCamera();
  }


  public void endCamera() {
    g.endCamera();
  }


  public void camera() {
    g.camera();
  }


  public void camera(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
    g.camera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
  }


  public void printCamera() {
    g.printCamera();
  }


  public void ortho() {
    g.ortho();
  }


  public void ortho(float left, float right,
                    float bottom, float top) {
    g.ortho(left, right, bottom, top);
  }


  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    g.ortho(left, right, bottom, top, near, far);
  }


  public void perspective() {
    g.perspective();
  }


  public void perspective(float fovy, float aspect, float zNear, float zFar) {
    g.perspective(fovy, aspect, zNear, zFar);
  }


  public void frustum(float left, float right,
                      float bottom, float top,
                      float near, float far) {
    g.frustum(left, right, bottom, top, near, far);
  }


  public void printProjection() {
    g.printProjection();
  }


  /**
   * Given an x and y coordinate, returns the x position of where
   * that point would be placed on screen, once affected by translate(),
   * scale(), or any other transformations.
   */
  public float screenX(float x, float y) {
    return g.screenX(x, y);
  }


  /**
   * Given an x and y coordinate, returns the y position of where
   * that point would be placed on screen, once affected by translate(),
   * scale(), or any other transformations.
   */
  public float screenY(float x, float y) {
    return g.screenY(x, y);
  }


  /**
   * Maps a three dimensional point to its placement on-screen.
   * <P>
   * Given an (x, y, z) coordinate, returns the x position of where
   * that point would be placed on screen, once affected by translate(),
   * scale(), or any other transformations.
   */
  public float screenX(float x, float y, float z) {
    return g.screenX(x, y, z);
  }


  /**
   * Maps a three dimensional point to its placement on-screen.
   * <P>
   * Given an (x, y, z) coordinate, returns the y position of where
   * that point would be placed on screen, once affected by translate(),
   * scale(), or any other transformations.
   */
  public float screenY(float x, float y, float z) {
    return g.screenY(x, y, z);
  }


  /**
   * Maps a three dimensional point to its placement on-screen.
   * <P>
   * Given an (x, y, z) coordinate, returns its z value.
   * This value can be used to determine if an (x, y, z) coordinate
   * is in front or in back of another (x, y, z) coordinate.
   * The units are based on how the zbuffer is set up, and don't
   * relate to anything "real". They're only useful for in
   * comparison to another value obtained from screenZ(),
   * or directly out of the zbuffer[].
   */
  public float screenZ(float x, float y, float z) {
    return g.screenZ(x, y, z);
  }


  /**
   * Returns the model space x value for an x, y, z coordinate.
   * <P>
   * This will give you a coordinate after it has been transformed
   * by translate(), rotate(), and camera(), but not yet transformed
   * by the projection matrix. For instance, his can be useful for
   * figuring out how points in 3D space relate to the edge
   * coordinates of a shape.
   */
  public float modelX(float x, float y, float z) {
    return g.modelX(x, y, z);
  }


  /**
   * Returns the model space y value for an x, y, z coordinate.
   */
  public float modelY(float x, float y, float z) {
    return g.modelY(x, y, z);
  }


  /**
   * Returns the model space z value for an x, y, z coordinate.
   */
  public float modelZ(float x, float y, float z) {
    return g.modelZ(x, y, z);
  }


  public void pushStyle() {
    g.pushStyle();
  }


  public void popStyle() {
    g.popStyle();
  }


  public void style(PStyle s) {
    g.style(s);
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


  /**
   * Set the tint to either a grayscale or ARGB value.
   * See notes attached to the fill() function.
   */
  public void stroke(int rgb) {
    g.stroke(rgb);
  }


  public void stroke(int rgb, float alpha) {
    g.stroke(rgb, alpha);
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


  public void noTint() {
    g.noTint();
  }


  /**
   * Set the tint to either a grayscale or ARGB value.
   */
  public void tint(int rgb) {
    g.tint(rgb);
  }


  public void tint(int rgb, float alpha) {
    g.tint(rgb, alpha);
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


  /**
   * Set the fill to either a grayscale value or an ARGB int.
   */
  public void fill(int rgb) {
    g.fill(rgb);
  }


  public void fill(int rgb, float alpha) {
    g.fill(rgb, alpha);
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


  public void ambient(int rgb) {
    g.ambient(rgb);
  }


  public void ambient(float gray) {
    g.ambient(gray);
  }


  public void ambient(float x, float y, float z) {
    g.ambient(x, y, z);
  }


  public void specular(int rgb) {
    g.specular(rgb);
  }


  public void specular(float gray) {
    g.specular(gray);
  }


  public void specular(float x, float y, float z) {
    g.specular(x, y, z);
  }


  public void shininess(float shine) {
    g.shininess(shine);
  }


  public void emissive(int rgb) {
    g.emissive(rgb);
  }


  public void emissive(float gray) {
    g.emissive(gray);
  }


  public void emissive(float x, float y, float z) {
    g.emissive(x, y, z);
  }


  public void lights() {
    g.lights();
  }


  public void noLights() {
    g.noLights();
  }


  public void ambientLight(float red, float green, float blue) {
    g.ambientLight(red, green, blue);
  }


  public void ambientLight(float red, float green, float blue,
                           float x, float y, float z) {
    g.ambientLight(red, green, blue, x, y, z);
  }


  public void directionalLight(float red, float green, float blue,
                               float nx, float ny, float nz) {
    g.directionalLight(red, green, blue, nx, ny, nz);
  }


  public void pointLight(float red, float green, float blue,
                         float x, float y, float z) {
    g.pointLight(red, green, blue, x, y, z);
  }


  public void spotLight(float red, float green, float blue,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    g.spotLight(red, green, blue, x, y, z, nx, ny, nz, angle, concentration);
  }


  public void lightFalloff(float constant, float linear, float quadratic) {
    g.lightFalloff(constant, linear, quadratic);
  }


  public void lightSpecular(float x, float y, float z) {
    g.lightSpecular(x, y, z);
  }


  /**
   * Set the background to a gray or ARGB color.
   * <p>
   * For the main drawing surface, the alpha value will be ignored. However,
   * alpha can be used on PGraphics objects from createGraphics(). This is
   * the only way to set all the pixels partially transparent, for instance.
   * <p>
   * Note that background() should be called before any transformations occur,
   * because some implementations may require the current transformation matrix
   * to be identity before drawing.
   */
  public void background(int rgb) {
    g.background(rgb);
  }


  /**
   * See notes about alpha in background(x, y, z, a).
   */
  public void background(int rgb, float alpha) {
    g.background(rgb, alpha);
  }


  /**
   * Set the background to a grayscale value, based on the
   * current colorMode.
   */
  public void background(float gray) {
    g.background(gray);
  }


  /**
   * See notes about alpha in background(x, y, z, a).
   */
  public void background(float gray, float alpha) {
    g.background(gray, alpha);
  }


  /**
   * Set the background to an r, g, b or h, s, b value,
   * based on the current colorMode.
   */
  public void background(float x, float y, float z) {
    g.background(x, y, z);
  }


  /**
   * Clear the background with a color that includes an alpha value. This can
   * only be used with objects created by createGraphics(), because the main
   * drawing surface cannot be set transparent.
   * <p>
   * It might be tempting to use this function to partially clear the screen
   * on each frame, however that's not how this function works. When calling
   * background(), the pixels will be replaced with pixels that have that level
   * of transparency. To do a semi-transparent overlay, use fill() with alpha
   * and draw a rectangle.
   */
  public void background(float x, float y, float z, float a) {
    g.background(x, y, z, a);
  }


  public void clear() {
    g.clear();
  }


  /**
   * Takes an RGB or ARGB image and sets it as the background.
   * The width and height of the image must be the same size as the sketch.
   * Use image.resize(width, height) to make short work of such a task.
   * <P>
   * Note that even if the image is set as RGB, the high 8 bits of each pixel
   * should be set opaque (0xFF000000), because the image data will be copied
   * directly to the screen, and non-opaque background images may have strange
   * behavior. Using image.filter(OPAQUE) will handle this easily.
   * <P>
   * When using 3D, this will also clear the zbuffer (if it exists).
   */
  public void background(PImage image) {
    g.background(image);
  }


  public void colorMode(int mode) {
    g.colorMode(mode);
  }


  public void colorMode(int mode, float max) {
    g.colorMode(mode, max);
  }


  /**
   * Set the colorMode and the maximum values for (r, g, b)
   * or (h, s, b).
   * <P>
   * Note that this doesn't set the maximum for the alpha value,
   * which might be confusing if for instance you switched to
   * <PRE>colorMode(HSB, 360, 100, 100);</PRE>
   * because the alpha values were still between 0 and 255.
   */
  public void colorMode(int mode, float maxX, float maxY, float maxZ) {
    g.colorMode(mode, maxX, maxY, maxZ);
  }


  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ, float maxA) {
    g.colorMode(mode, maxX, maxY, maxZ, maxA);
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


  /**
   * Interpolate between two colors, using the current color mode.
   */
  public int lerpColor(int c1, int c2, float amt) {
    return g.lerpColor(c1, c2, amt);
  }


  /**
   * Interpolate between two colors. Like lerp(), but for the
   * individual color components of a color supplied as an int value.
   */
  static public int lerpColor(int c1, int c2, float amt, int mode) {
    return PGraphics.lerpColor(c1, c2, amt, mode);
  }


  /**
   * Display a warning that the specified method is only available with 3D.
   * @param method The method name (no parentheses)
   */
  static public void showDepthWarning(String method) {
    PGraphics.showDepthWarning(method);
  }


  /**
   * Display a warning that the specified method that takes x, y, z parameters
   * can only be used with x and y parameters in this renderer.
   * @param method The method name (no parentheses)
   */
  static public void showDepthWarningXYZ(String method) {
    PGraphics.showDepthWarningXYZ(method);
  }


  /**
   * Display a warning that the specified method is simply unavailable.
   */
  static public void showMethodWarning(String method) {
    PGraphics.showMethodWarning(method);
  }


  /**
   * Error that a particular variation of a method is unavailable (even though
   * other variations are). For instance, if vertex(x, y, u, v) is not
   * available, but vertex(x, y) is just fine.
   */
  static public void showVariationWarning(String str) {
    PGraphics.showVariationWarning(str);
  }


  /**
   * Display a warning that the specified method is not implemented, meaning
   * that it could be either a completely missing function, although other
   * variations of it may still work properly.
   */
  static public void showMissingWarning(String method) {
    PGraphics.showMissingWarning(method);
  }


  /**
   * Return true if this renderer should be drawn to the screen. Defaults to
   * returning true, since nearly all renderers are on-screen beasts. But can
   * be overridden for subclasses like PDF so that a window doesn't open up.
   * <br/> <br/>
   * A better name? showFrame, displayable, isVisible, visible, shouldDisplay,
   * what to call this?
   */
  public boolean displayable() {
    return g.displayable();
  }


  /**
   * Return true if this renderer does rendering through OpenGL. Defaults to false.
   */
  public boolean isGL() {
    return g.isGL();
  }


  /**
   * Returns the native Bitmap object for this PImage.
   */
  public Object getNative() {
    return g.getNative();
  }


  /**
   * Returns an ARGB "color" type (a packed 32 bit int with the color.
   * If the coordinate is outside the image, zero is returned
   * (black, but completely transparent).
   * <P>
   * If the image is in RGB format (i.e. on a PVideo object),
   * the value will get its high bits set, just to avoid cases where
   * they haven't been set already.
   * <P>
   * If the image is in ALPHA format, this returns a white with its
   * alpha value set.
   * <P>
   * This function is included primarily for beginners. It is quite
   * slow because it has to check to see if the x, y that was provided
   * is inside the bounds, and then has to check to see what image
   * type it is. If you want things to be more efficient, access the
   * pixels[] array directly.
   */
  public int get(int x, int y) {
    return g.get(x, y);
  }


  /**
   * Grab a subsection of a PImage, and copy it into a fresh PImage.
   * As of release 0149, no longer honors imageMode() for the coordinates.
   */
  /**
   * @param w width of pixel rectangle to get
   * @param h height of pixel rectangle to get
   */
  public PImage get(int x, int y, int w, int h) {
    return g.get(x, y, w, h);
  }


  /**
   * Returns a copy of this PImage. Equivalent to get(0, 0, width, height).
   */
  public PImage get() {
    return g.get();
  }


  /**
   * Set a single pixel to the specified color.
   */
  public void set(int x, int y, int c) {
    g.set(x, y, c);
  }


  /**
   * Efficient method of drawing an image's pixels directly to this surface.
   * No variations are employed, meaning that any scale, tint, or imageMode
   * settings will be ignored.
   */
  public void set(int x, int y, PImage img) {
    g.set(x, y, img);
  }


  /**
   * Set alpha channel for an image. Black colors in the source
   * image will make the destination image completely transparent,
   * and white will make things fully opaque. Gray values will
   * be in-between steps.
   * <P>
   * Strictly speaking the "blue" value from the source image is
   * used as the alpha color. For a fully grayscale image, this
   * is correct, but for a color image it's not 100% accurate.
   * For a more accurate conversion, first use filter(GRAY)
   * which will make the image into a "correct" grayscake by
   * performing a proper luminance-based conversion.
   */
  public void mask(int alpha[]) {
    g.mask(alpha);
  }


  /**
   * Set alpha channel for an image using another image as the source.
   */
  public void mask(PImage alpha) {
    g.mask(alpha);
  }


  /**
   * Method to apply a variety of basic filters to this image.
   * <P>
   * <UL>
   * <LI>filter(BLUR) provides a basic blur.
   * <LI>filter(GRAY) converts the image to grayscale based on luminance.
   * <LI>filter(INVERT) will invert the color components in the image.
   * <LI>filter(OPAQUE) set all the high bits in the image to opaque
   * <LI>filter(THRESHOLD) converts the image to black and white.
   * <LI>filter(DILATE) grow white/light areas
   * <LI>filter(ERODE) shrink white/light areas
   * </UL>
   * Luminance conversion code contributed by
   * <A HREF="http://www.toxi.co.uk">toxi</A>
   * <P/>
   * Gaussian blur code contributed by
   * <A HREF="http://incubator.quasimondo.com">Mario Klingemann</A>
   */
  public void filter(int kind) {
    g.filter(kind);
  }


  /**
   * Method to apply a variety of basic filters to this image.
   * These filters all take a parameter.
   * <P>
   * <UL>
   * <LI>filter(BLUR, int radius) performs a gaussian blur of the
   * specified radius.
   * <LI>filter(POSTERIZE, int levels) will posterize the image to
   * between 2 and 255 levels.
   * <LI>filter(THRESHOLD, float center) allows you to set the
   * center point for the threshold. It takes a value from 0 to 1.0.
   * </UL>
   * Gaussian blur code contributed by
   * <A HREF="http://incubator.quasimondo.com">Mario Klingemann</A>
   * and later updated by toxi for better speed.
   */
  public void filter(int kind, float param) {
    g.filter(kind, param);
  }


  /**
   * Copy things from one area of this image
   * to another area in the same image.
   */
  public void copy(int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    g.copy(sx, sy, sw, sh, dx, dy, dw, dh);
  }


  /**
   * Copies area of one image into another PImage object.
   */
  public void copy(PImage src,
                   int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    g.copy(src, sx, sy, sw, sh, dx, dy, dw, dh);
  }


  /**
   * Blends one area of this image to another area.
   * @see processing.core.PImage#blendColor(int,int,int)
   */
  public void blend(int sx, int sy, int sw, int sh,
                    int dx, int dy, int dw, int dh, int mode) {
    g.blend(sx, sy, sw, sh, dx, dy, dw, dh, mode);
  }


  /**
   * Copies area of one image into another PImage object.
   * @see processing.core.PImage#blendColor(int,int,int)
   */
  public void blend(PImage src,
                    int sx, int sy, int sw, int sh,
                    int dx, int dy, int dw, int dh, int mode) {
    g.blend(src, sx, sy, sw, sh, dx, dy, dw, dh, mode);
  }
}
