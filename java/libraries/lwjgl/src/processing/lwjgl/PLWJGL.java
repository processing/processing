/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

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

package processing.lwjgl;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.nio.Buffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ARBES2Compatibility;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;
import org.lwjgl.opengl.PixelFormat;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

/**
 * Processing-OpenGL abstraction layer. LWJGL implementation.
 *
 * Some issues:
 * http://lwjgl.org/forum/index.php/topic,4711.0.html
 * http://www.java-gaming.org/topics/cannot-add-mouselistener-to-java-awt-canvas-with-lwjgl-on-windows/24650/view.html
 *
 */
public class PLWJGL extends PGL {
  // ........................................................

  // Public members to access the underlying GL objects and canvas
  
  /** GLU interface **/
  public static GLU glu;

  /** The canvas where OpenGL rendering takes place */
  public static Canvas canvas;
  
  // ........................................................
  
  // Event handling
  
  /** Poller threads to get the keyboard/mouse events from LWJGL */
  protected static KeyPoller keyPoller;
  protected static MousePoller mousePoller;

  // ........................................................
  
  // Utility buffers to copy projection/modelview matrices to GL  
  
  protected FloatBuffer projMatrix;
  protected FloatBuffer mvMatrix;
  
  // ........................................................
  
  // Static initialization for some parameters that need to be different for
  // LWJGL
  
  static {
    MIN_DIRECT_BUFFER_SIZE = 16;
    INDEX_TYPE             = GL11.GL_UNSIGNED_SHORT;
  }
  
  
  ///////////////////////////////////////////////////////////

  // Initialization, finalization
  
  
  public PLWJGL(PGraphicsOpenGL pg) {
    super(pg);
    if (glu == null) glu = new GLU();
  }

  
  public Canvas getCanvas() {
    return canvas;
  } 
  

  protected void setFps(float fps) {
    if (!setFps || targetFps != fps) {
      if (60 < fps) {
        // Disables v-sync
        Display.setVSyncEnabled(false);
        Display.sync((int)fps);
      } else  {
        Display.setVSyncEnabled(true);
      }
      targetFps = currentFps = fps; 
      setFps = true;      
    }
  }


  protected void initSurface(int antialias) {
    if (canvas != null) {
      keyPoller.requestStop();
      mousePoller.requestStop();

      try {
        Display.setParent(null);
      } catch (LWJGLException e) {
        e.printStackTrace();
      }
      Display.destroy();
      
      pg.parent.remove(canvas);
    }

    canvas = new Canvas();
    canvas.setFocusable(true);
    canvas.requestFocus();
    canvas.setBackground(new Color(pg.backgroundColor, true));   
    canvas.setBounds(0, 0, pg.parent.width, pg.parent.height);
    
    pg.parent.setLayout(new BorderLayout());
    pg.parent.add(canvas, BorderLayout.CENTER);     
    
    try {      
      DisplayMode[] modes = Display.getAvailableDisplayModes();
      int bpp = 0; 
      for (int i = 0; i < modes.length; i++) {
        bpp = PApplet.max(modes[i].getBitsPerPixel(), bpp);
      }
      
      PixelFormat format;
      if (USE_FBOLAYER_BY_DEFAULT) {
        format = new PixelFormat(bpp, REQUESTED_ALPHA_BITS,
                                      REQUESTED_DEPTH_BITS,
                                      REQUESTED_STENCIL_BITS, 1);   
        reqNumSamples = qualityToSamples(antialias);
        fboLayerRequested = true;
      } else {
        format = new PixelFormat(bpp, REQUESTED_ALPHA_BITS,
                                      REQUESTED_DEPTH_BITS,
                                      REQUESTED_STENCIL_BITS, antialias);  
        fboLayerRequested = false;  
      }
      
      Display.setDisplayMode(new DisplayMode(pg.parent.width, pg.parent.height));
      int argb = pg.backgroundColor;
      float r = ((argb >> 16) & 0xff) / 255.0f;
      float g = ((argb >> 8) & 0xff) / 255.0f;
      float b = ((argb) & 0xff) / 255.0f; 
      Display.setInitialBackground(r, g, b); 
      Display.setParent(canvas);      
      Display.create(format);

      // Might be useful later to specify the context attributes.
      // http://lwjgl.org/javadoc/org/lwjgl/opengl/ContextAttribs.html
//      ContextAttribs contextAtrributes = new ContextAttribs(4, 0);
//      contextAtrributes.withForwardCompatible(true);
//      contextAtrributes.withProfileCore(true);
//      Display.create(pixelFormat, contextAtrributes);      
    } catch (LWJGLException e) {
      e.printStackTrace();
    }
    
    glContext = Display.getDrawable().hashCode();

    registerListeners();
    
    fboLayerCreated = false;
    fboLayerInUse = false;
    firstFrame = true;
    setFps = false;
  }

    
  protected void reinitSurface() { }
  
  
  protected void registerListeners() {
    keyPoller = new KeyPoller(pg.parent);
    keyPoller.start();

    mousePoller = new MousePoller(pg.parent);
    mousePoller.start();
  }  
  

  ///////////////////////////////////////////////////////////

  // Frame rendering


  protected boolean canDraw() {
    return pg.initialized && pg.parent.isDisplayable();
  }
  
  
  protected void requestFocus() { }  
  

  protected void requestDraw() {
    if (pg.initialized) {
      glThread = Thread.currentThread();
      pg.parent.handleDraw();
      Display.update();
    }
  }

  
  protected void swapBuffers() {
    try {
      Display.swapBuffers();
    } catch (LWJGLException e) {
      e.printStackTrace();
    }  
  }


  @Override
  protected void beginGL() {
    if (projMatrix == null) {
      projMatrix = allocateFloatBuffer(16);
    }
    GL11.glMatrixMode(GL11.GL_PROJECTION);
    projMatrix.rewind();
    projMatrix.put(pg.projection.m00);
    projMatrix.put(pg.projection.m10);
    projMatrix.put(pg.projection.m20);
    projMatrix.put(pg.projection.m30);
    projMatrix.put(pg.projection.m01);
    projMatrix.put(pg.projection.m11);
    projMatrix.put(pg.projection.m21);
    projMatrix.put(pg.projection.m31);
    projMatrix.put(pg.projection.m02);
    projMatrix.put(pg.projection.m12);
    projMatrix.put(pg.projection.m22);
    projMatrix.put(pg.projection.m32);
    projMatrix.put(pg.projection.m03);
    projMatrix.put(pg.projection.m13);
    projMatrix.put(pg.projection.m23);
    projMatrix.put(pg.projection.m33);
    projMatrix.rewind();
    GL11.glLoadMatrix(projMatrix);

    if (mvMatrix == null) {
      mvMatrix = allocateFloatBuffer(16);
    }
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
    mvMatrix.rewind();
    mvMatrix.put(pg.modelview.m00);
    mvMatrix.put(pg.modelview.m10);
    mvMatrix.put(pg.modelview.m20);
    mvMatrix.put(pg.modelview.m30);
    mvMatrix.put(pg.modelview.m01);
    mvMatrix.put(pg.modelview.m11);
    mvMatrix.put(pg.modelview.m21);
    mvMatrix.put(pg.modelview.m31);
    mvMatrix.put(pg.modelview.m02);
    mvMatrix.put(pg.modelview.m12);
    mvMatrix.put(pg.modelview.m22);
    mvMatrix.put(pg.modelview.m32);
    mvMatrix.put(pg.modelview.m03);
    mvMatrix.put(pg.modelview.m13);
    mvMatrix.put(pg.modelview.m23);
    mvMatrix.put(pg.modelview.m33);
    mvMatrix.rewind();
    GL11.glLoadMatrix(mvMatrix);
  }
  
  
  ///////////////////////////////////////////////////////////

  // Utility functions

  
  protected static ByteBuffer allocateDirectByteBuffer(int size) {
    return BufferUtils.createByteBuffer(size);
  }


  protected static ShortBuffer allocateDirectShortBuffer(int size) {    
    return BufferUtils.createShortBuffer(size);
  }

  
  protected static IntBuffer allocateDirectIntBuffer(int size) {
    return BufferUtils.createIntBuffer(size);
  }

  
  protected static FloatBuffer allocateDirectFloatBuffer(int size) {
    return BufferUtils.createFloatBuffer(size);
  }
  
  
  @Override
  protected int getFontAscent(Object font) {
    FontMetrics metrics = pg.parent.getFontMetrics((Font)font);
    return metrics.getAscent();
  }


  @Override
  protected int getFontDescent(Object font) {
    FontMetrics metrics = pg.parent.getFontMetrics((Font)font);
    return metrics.getDescent();
  }


  @Override
  protected int getTextWidth(Object font, char buffer[], int start, int stop) {
    // maybe should use one of the newer/fancier functions for this?
    int length = stop - start;
    FontMetrics metrics = pg.parent.getFontMetrics((Font)font);
    return metrics.charsWidth(buffer, start, length);
  }
  
  
  @Override
  protected Object getDerivedFont(Object font, float size) {
    return ((Font)font).deriveFont(size);
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
          int y = parent.height - Mouse.getY();
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

  
  ///////////////////////////////////////////////////////////

  // Tessellator interface


  protected Tessellator createTessellator(TessellatorCallback callback) {
    return new Tessellator(callback);
  }


  protected class Tessellator implements PGL.Tessellator {
    protected GLUtessellator tess;
    protected TessellatorCallback callback;
    protected GLUCallback gluCallback;

    public Tessellator(TessellatorCallback callback) {
      this.callback = callback;
      tess = GLU.gluNewTess();
      gluCallback = new GLUCallback();

      tess.gluTessCallback(GLU.GLU_TESS_BEGIN, gluCallback);
      tess.gluTessCallback(GLU.GLU_TESS_END, gluCallback);
      tess.gluTessCallback(GLU.GLU_TESS_VERTEX, gluCallback);
      tess.gluTessCallback(GLU.GLU_TESS_COMBINE, gluCallback);
      tess.gluTessCallback(GLU.GLU_TESS_ERROR, gluCallback);
    }

    public void beginPolygon() {
      tess.gluTessBeginPolygon(null);
    }

    public void endPolygon() {
      tess.gluTessEndPolygon();
    }

    public void setWindingRule(int rule) {
      tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, rule);
    }

    public void beginContour() {
      tess.gluTessBeginContour();
    }

    public void endContour() {
      tess.gluTessEndContour();
    }

    public void addVertex(double[] v) {
      tess.gluTessVertex(v, 0, v);
    }

    protected class GLUCallback extends GLUtessellatorCallbackAdapter {
      @Override
      public void begin(int type) {
        callback.begin(type);
      }

      @Override
      public void end() {
        callback.end();
      }

      @Override
      public void vertex(Object data) {
        callback.vertex(data);
      }

      @Override
      public void combine(double[] coords, Object[] data,
                          float[] weight, Object[] outData) {
        callback.combine(coords, data, weight, outData);
      }

      @Override
      public void error(int errnum) {
        callback.error(errnum);
      }
    }
  }


  protected String tessError(int err) {
    return GLU.gluErrorString(err);
  }


  ///////////////////////////////////////////////////////////

  // Font outline


  static {
    SHAPE_TEXT_SUPPORTED = true;
    SEG_MOVETO  = PathIterator.SEG_MOVETO;
    SEG_LINETO  = PathIterator.SEG_LINETO;
    SEG_QUADTO  = PathIterator.SEG_QUADTO;
    SEG_CUBICTO = PathIterator.SEG_CUBICTO;
    SEG_CLOSE   = PathIterator.SEG_CLOSE;
  }


  protected FontOutline createFontOutline(char ch, Object font) {
    return new FontOutline(ch, font);
  }


  protected class FontOutline implements PGL.FontOutline {
    PathIterator iter;

    public FontOutline(char ch, Object font) {
      char textArray[] = new char[] { ch };
      Graphics2D graphics = (Graphics2D) pg.parent.getGraphics();
      FontRenderContext frc = graphics.getFontRenderContext();
      GlyphVector gv = ((Font)font).createGlyphVector(frc, textArray);
      Shape shp = gv.getOutline();
      iter = shp.getPathIterator(null);
    }

    public boolean isDone() {
      return iter.isDone();
    }

    public int currentSegment(float coords[]) {
      return iter.currentSegment(coords);
    }

    public void next() {
      iter.next();
    }
  }

  
  ///////////////////////////////////////////////////////////

  // Constants

  static {
    FALSE = GL11.GL_FALSE;
    TRUE  = GL11.GL_TRUE;

    INT            = GL11.GL_INT;
    BYTE           = GL11.GL_BYTE;
    SHORT          = GL11.GL_SHORT;
    FLOAT          = GL11.GL_FLOAT;
    BOOL           = GL20.GL_BOOL;
    UNSIGNED_INT   = GL11.GL_UNSIGNED_INT;
    UNSIGNED_BYTE  = GL11.GL_UNSIGNED_BYTE;
    UNSIGNED_SHORT = GL11.GL_UNSIGNED_SHORT;

    RGB             = GL11.GL_RGB;
    RGBA            = GL11.GL_RGBA;
    ALPHA           = GL11.GL_ALPHA;
    LUMINANCE       = GL11.GL_LUMINANCE;
    LUMINANCE_ALPHA = GL11.GL_LUMINANCE_ALPHA;

    UNSIGNED_SHORT_5_6_5   = GL12.GL_UNSIGNED_SHORT_5_6_5;
    UNSIGNED_SHORT_4_4_4_4 = GL12.GL_UNSIGNED_SHORT_4_4_4_4;
    UNSIGNED_SHORT_5_5_5_1 = GL12.GL_UNSIGNED_SHORT_5_5_5_1;

    RGBA4   = GL11.GL_RGBA4;
    RGB5_A1 = GL11.GL_RGB5_A1;
    RGB565  = ARBES2Compatibility.GL_RGB565;
    RGB8    = GL11.GL_RGB8;
    RGBA8   = GL11.GL_RGBA8;
    ALPHA8  = GL11.GL_ALPHA8;

    READ_ONLY  = GL15.GL_READ_ONLY;
    WRITE_ONLY = GL15.GL_WRITE_ONLY;
    READ_WRITE = GL15.GL_READ_WRITE;

    TESS_WINDING_NONZERO = GLU.GLU_TESS_WINDING_NONZERO;
    TESS_WINDING_ODD     = GLU.GLU_TESS_WINDING_ODD;

    GENERATE_MIPMAP_HINT = GL14.GL_GENERATE_MIPMAP_HINT;
    FASTEST              = GL11.GL_FASTEST;
    NICEST               = GL11.GL_NICEST;
    DONT_CARE            = GL11.GL_DONT_CARE;

    VENDOR                   = GL11.GL_VENDOR;
    RENDERER                 = GL11.GL_RENDERER;
    VERSION                  = GL11.GL_VERSION;
    EXTENSIONS               = GL11.GL_EXTENSIONS;
    SHADING_LANGUAGE_VERSION = GL20.GL_SHADING_LANGUAGE_VERSION;

    MAX_SAMPLES = GL30.GL_MAX_SAMPLES;
    SAMPLES     = GL13.GL_SAMPLES;

    ALIASED_LINE_WIDTH_RANGE = GL12.GL_ALIASED_LINE_WIDTH_RANGE;
    ALIASED_POINT_SIZE_RANGE = GL12.GL_ALIASED_POINT_SIZE_RANGE;

    DEPTH_BITS   = GL11.GL_DEPTH_BITS;
    STENCIL_BITS = GL11.GL_STENCIL_BITS;

    CCW = GL11.GL_CCW;
    CW  = GL11.GL_CW;

    VIEWPORT = GL11.GL_VIEWPORT;

    ARRAY_BUFFER         = GL15.GL_ARRAY_BUFFER;
    ELEMENT_ARRAY_BUFFER = GL15.GL_ELEMENT_ARRAY_BUFFER;

    MAX_VERTEX_ATTRIBS  = GL20.GL_MAX_VERTEX_ATTRIBS;

    STATIC_DRAW  = GL15.GL_STATIC_DRAW;
    DYNAMIC_DRAW = GL15.GL_DYNAMIC_DRAW;
    STREAM_DRAW  = GL15.GL_STREAM_DRAW;

    BUFFER_SIZE  = GL15.GL_BUFFER_SIZE;
    BUFFER_USAGE = GL15.GL_BUFFER_USAGE;

    POINTS         = GL11.GL_POINTS;
    LINE_STRIP     = GL11.GL_LINE_STRIP;
    LINE_LOOP      = GL11.GL_LINE_LOOP;
    LINES          = GL11.GL_LINES;
    TRIANGLE_FAN   = GL11.GL_TRIANGLE_FAN;
    TRIANGLE_STRIP = GL11.GL_TRIANGLE_STRIP;
    TRIANGLES      = GL11.GL_TRIANGLES;

    CULL_FACE      = GL11.GL_CULL_FACE;
    FRONT          = GL11.GL_FRONT;
    BACK           = GL11.GL_BACK;
    FRONT_AND_BACK = GL11.GL_FRONT_AND_BACK;

    POLYGON_OFFSET_FILL = GL11.GL_POLYGON_OFFSET_FILL;

    UNPACK_ALIGNMENT = GL11.GL_UNPACK_ALIGNMENT;
    PACK_ALIGNMENT   = GL11.GL_PACK_ALIGNMENT;

    TEXTURE_2D        = GL11.GL_TEXTURE_2D;
    TEXTURE_RECTANGLE = GL31.GL_TEXTURE_RECTANGLE;

    TEXTURE_BINDING_2D        = GL11.GL_TEXTURE_BINDING_2D;
    TEXTURE_BINDING_RECTANGLE = GL31.GL_TEXTURE_BINDING_RECTANGLE;

    MAX_TEXTURE_SIZE           = GL11.GL_MAX_TEXTURE_SIZE;
    TEXTURE_MAX_ANISOTROPY     = EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
    MAX_TEXTURE_MAX_ANISOTROPY = EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;

    MAX_VERTEX_TEXTURE_IMAGE_UNITS   = GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS;
    MAX_TEXTURE_IMAGE_UNITS          = GL20.GL_MAX_TEXTURE_IMAGE_UNITS;
    MAX_COMBINED_TEXTURE_IMAGE_UNITS = GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS;

    NUM_COMPRESSED_TEXTURE_FORMATS = GL13.GL_NUM_COMPRESSED_TEXTURE_FORMATS;
    COMPRESSED_TEXTURE_FORMATS     = GL13.GL_COMPRESSED_TEXTURE_FORMATS;

    NEAREST               = GL11.GL_NEAREST;
    LINEAR                = GL11.GL_LINEAR;
    LINEAR_MIPMAP_NEAREST = GL11.GL_LINEAR_MIPMAP_NEAREST;
    LINEAR_MIPMAP_LINEAR  = GL11.GL_LINEAR_MIPMAP_LINEAR;

    CLAMP_TO_EDGE = GL12.GL_CLAMP_TO_EDGE;
    REPEAT        = GL11.GL_REPEAT;

    TEXTURE0           = GL13.GL_TEXTURE0;
    TEXTURE1           = GL13.GL_TEXTURE1;
    TEXTURE2           = GL13.GL_TEXTURE2;
    TEXTURE3           = GL13.GL_TEXTURE3;
    TEXTURE_MIN_FILTER = GL11.GL_TEXTURE_MIN_FILTER;
    TEXTURE_MAG_FILTER = GL11.GL_TEXTURE_MAG_FILTER;
    TEXTURE_WRAP_S     = GL11.GL_TEXTURE_WRAP_S;
    TEXTURE_WRAP_T     = GL11.GL_TEXTURE_WRAP_T;
    TEXTURE_WRAP_R     = GL12.GL_TEXTURE_WRAP_R;

    TEXTURE_CUBE_MAP = GL13.GL_TEXTURE_CUBE_MAP;
    TEXTURE_CUBE_MAP_POSITIVE_X = GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
    TEXTURE_CUBE_MAP_POSITIVE_Y = GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
    TEXTURE_CUBE_MAP_POSITIVE_Z = GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
    TEXTURE_CUBE_MAP_NEGATIVE_X = GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
    TEXTURE_CUBE_MAP_NEGATIVE_Y = GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
    TEXTURE_CUBE_MAP_NEGATIVE_Z = GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;

    VERTEX_SHADER        = GL20.GL_VERTEX_SHADER;
    FRAGMENT_SHADER      = GL20.GL_FRAGMENT_SHADER;
    INFO_LOG_LENGTH      = GL20.GL_INFO_LOG_LENGTH;
    SHADER_SOURCE_LENGTH = GL20.GL_SHADER_SOURCE_LENGTH;
    COMPILE_STATUS       = GL20.GL_COMPILE_STATUS;
    LINK_STATUS          = GL20.GL_LINK_STATUS;
    VALIDATE_STATUS      = GL20.GL_VALIDATE_STATUS;
    SHADER_TYPE          = GL20.GL_SHADER_TYPE;
    DELETE_STATUS        = GL20.GL_DELETE_STATUS;

    FLOAT_VEC2   = GL20.GL_FLOAT_VEC2;
    FLOAT_VEC3   = GL20.GL_FLOAT_VEC3;
    FLOAT_VEC4   = GL20.GL_FLOAT_VEC4;
    FLOAT_MAT2   = GL20.GL_FLOAT_MAT2;
    FLOAT_MAT3   = GL20.GL_FLOAT_MAT3;
    FLOAT_MAT4   = GL20.GL_FLOAT_MAT4;
    INT_VEC2     = GL20.GL_INT_VEC2;
    INT_VEC3     = GL20.GL_INT_VEC3;
    INT_VEC4     = GL20.GL_INT_VEC4;
    BOOL_VEC2    = GL20.GL_BOOL_VEC2;
    BOOL_VEC3    = GL20.GL_BOOL_VEC3;
    BOOL_VEC4    = GL20.GL_BOOL_VEC4;
    SAMPLER_2D   = GL20.GL_SAMPLER_2D;
    SAMPLER_CUBE = GL20.GL_SAMPLER_CUBE;

    LOW_FLOAT    = ARBES2Compatibility.GL_LOW_FLOAT;
    MEDIUM_FLOAT = ARBES2Compatibility.GL_MEDIUM_FLOAT;
    HIGH_FLOAT   = ARBES2Compatibility.GL_HIGH_FLOAT;
    LOW_INT      = ARBES2Compatibility.GL_LOW_INT;
    MEDIUM_INT   = ARBES2Compatibility.GL_MEDIUM_INT;
    HIGH_INT     = ARBES2Compatibility.GL_HIGH_INT;

    CURRENT_VERTEX_ATTRIB = GL20.GL_CURRENT_VERTEX_ATTRIB;

    VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = GL15.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING;
    VERTEX_ATTRIB_ARRAY_ENABLED        = GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED;
    VERTEX_ATTRIB_ARRAY_SIZE           = GL20.GL_VERTEX_ATTRIB_ARRAY_SIZE;
    VERTEX_ATTRIB_ARRAY_STRIDE         = GL20.GL_VERTEX_ATTRIB_ARRAY_STRIDE;
    VERTEX_ATTRIB_ARRAY_TYPE           = GL20.GL_VERTEX_ATTRIB_ARRAY_TYPE;
    VERTEX_ATTRIB_ARRAY_NORMALIZED     = GL20.GL_VERTEX_ATTRIB_ARRAY_NORMALIZED;
    VERTEX_ATTRIB_ARRAY_POINTER        = GL20.GL_VERTEX_ATTRIB_ARRAY_POINTER;

    BLEND               = GL11.GL_BLEND;
    ONE                 = GL11.GL_ONE;
    ZERO                = GL11.GL_ZERO;
    SRC_ALPHA           = GL11.GL_SRC_ALPHA;
    DST_ALPHA           = GL11.GL_DST_ALPHA;
    ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;
    ONE_MINUS_DST_COLOR = GL11.GL_ONE_MINUS_DST_COLOR;
    ONE_MINUS_SRC_COLOR = GL11.GL_ONE_MINUS_SRC_COLOR;
    DST_COLOR           = GL11.GL_DST_COLOR;
    SRC_COLOR           = GL11.GL_SRC_COLOR;

    SAMPLE_ALPHA_TO_COVERAGE = GL13.GL_SAMPLE_ALPHA_TO_COVERAGE;
    SAMPLE_COVERAGE          = GL13.GL_SAMPLE_COVERAGE;

    KEEP      = GL11.GL_KEEP;
    REPLACE   = GL11.GL_REPLACE;
    INCR      = GL11.GL_INCR;
    DECR      = GL11.GL_DECR;
    INVERT    = GL11.GL_INVERT;
    INCR_WRAP = GL14.GL_INCR_WRAP;
    DECR_WRAP = GL14.GL_DECR_WRAP;
    NEVER     = GL11.GL_NEVER;
    ALWAYS    = GL11.GL_ALWAYS;

    EQUAL    = GL11.GL_EQUAL;
    LESS     = GL11.GL_LESS;
    LEQUAL   = GL11.GL_LEQUAL;
    GREATER  = GL11.GL_GREATER;
    GEQUAL   = GL11.GL_GEQUAL;
    NOTEQUAL = GL11.GL_NOTEQUAL;

    FUNC_ADD              = GL14.GL_FUNC_ADD;
    FUNC_MIN              = GL14.GL_MIN;
    FUNC_MAX              = GL14.GL_MAX;
    FUNC_REVERSE_SUBTRACT = GL14.GL_FUNC_REVERSE_SUBTRACT;
    FUNC_SUBTRACT         = GL14.GL_FUNC_SUBTRACT;

    DITHER = GL11.GL_DITHER;

    CONSTANT_COLOR           = GL11.GL_CONSTANT_COLOR;
    CONSTANT_ALPHA           = GL11.GL_CONSTANT_ALPHA;
    ONE_MINUS_CONSTANT_COLOR = GL11.GL_ONE_MINUS_CONSTANT_COLOR;
    ONE_MINUS_CONSTANT_ALPHA = GL11.GL_ONE_MINUS_CONSTANT_ALPHA;
    SRC_ALPHA_SATURATE       = GL11.GL_SRC_ALPHA_SATURATE;

    SCISSOR_TEST    = GL11.GL_SCISSOR_TEST;
    STENCIL_TEST    = GL11.GL_STENCIL_TEST;
    DEPTH_TEST      = GL11.GL_DEPTH_TEST;
    DEPTH_WRITEMASK = GL11.GL_DEPTH_WRITEMASK;
    ALPHA_TEST      = GL11.GL_ALPHA_TEST;

    COLOR_BUFFER_BIT   = GL11.GL_COLOR_BUFFER_BIT;
    DEPTH_BUFFER_BIT   = GL11.GL_DEPTH_BUFFER_BIT;
    STENCIL_BUFFER_BIT = GL11.GL_STENCIL_BUFFER_BIT;

    FRAMEBUFFER        = GL30.GL_FRAMEBUFFER;
    COLOR_ATTACHMENT0  = GL30.GL_COLOR_ATTACHMENT0;
    COLOR_ATTACHMENT1  = GL30.GL_COLOR_ATTACHMENT1;
    COLOR_ATTACHMENT2  = GL30.GL_COLOR_ATTACHMENT2;
    COLOR_ATTACHMENT3  = GL30.GL_COLOR_ATTACHMENT3;
    RENDERBUFFER       = GL30.GL_RENDERBUFFER;
    DEPTH_ATTACHMENT   = GL30.GL_DEPTH_ATTACHMENT;
    STENCIL_ATTACHMENT = GL30.GL_STENCIL_ATTACHMENT;
    READ_FRAMEBUFFER   = GL30.GL_READ_FRAMEBUFFER;
    DRAW_FRAMEBUFFER   = GL30.GL_DRAW_FRAMEBUFFER;

    DEPTH24_STENCIL8 = GL30.GL_DEPTH24_STENCIL8;

    DEPTH_COMPONENT   = GL11.GL_DEPTH_COMPONENT;
    DEPTH_COMPONENT16 = GL14.GL_DEPTH_COMPONENT16;
    DEPTH_COMPONENT24 = GL14.GL_DEPTH_COMPONENT24;
    DEPTH_COMPONENT32 = GL14.GL_DEPTH_COMPONENT32;

    STENCIL_INDEX  = GL11.GL_STENCIL_INDEX;
    STENCIL_INDEX1 = GL30.GL_STENCIL_INDEX1;
    STENCIL_INDEX4 = GL30.GL_STENCIL_INDEX4;
    STENCIL_INDEX8 = GL30.GL_STENCIL_INDEX8;

    DEPTH_STENCIL = GL30.GL_DEPTH_STENCIL;

    FRAMEBUFFER_COMPLETE                      = GL30.GL_FRAMEBUFFER_COMPLETE;
    FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
    FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
    FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT;
    FRAMEBUFFER_INCOMPLETE_FORMATS            = EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT;
    FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
    FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;
    FRAMEBUFFER_UNSUPPORTED                   = GL30.GL_FRAMEBUFFER_UNSUPPORTED;

    FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE           = GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE;
    FRAMEBUFFER_ATTACHMENT_OBJECT_NAME           = GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME;
    FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL         = GL30.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL;
    FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = GL30.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE;

    RENDERBUFFER_WIDTH           = GL30.GL_RENDERBUFFER_WIDTH;
    RENDERBUFFER_HEIGHT          = GL30.GL_RENDERBUFFER_HEIGHT;
    RENDERBUFFER_RED_SIZE        = GL30.GL_RENDERBUFFER_RED_SIZE;
    RENDERBUFFER_GREEN_SIZE      = GL30.GL_RENDERBUFFER_GREEN_SIZE;
    RENDERBUFFER_BLUE_SIZE       = GL30.GL_RENDERBUFFER_BLUE_SIZE;
    RENDERBUFFER_ALPHA_SIZE      = GL30.GL_RENDERBUFFER_ALPHA_SIZE;
    RENDERBUFFER_DEPTH_SIZE      = GL30.GL_RENDERBUFFER_DEPTH_SIZE;
    RENDERBUFFER_STENCIL_SIZE    = GL30.GL_RENDERBUFFER_STENCIL_SIZE;
    RENDERBUFFER_INTERNAL_FORMAT = GL30.GL_RENDERBUFFER_INTERNAL_FORMAT;

    MULTISAMPLE    = GL13.GL_MULTISAMPLE;
    POINT_SMOOTH   = GL11.GL_POINT_SMOOTH;
    LINE_SMOOTH    = GL11.GL_LINE_SMOOTH;
    POLYGON_SMOOTH = GL11.GL_POLYGON_SMOOTH;
  }
  
  ///////////////////////////////////////////////////////////

  // Special Functions

  public void flush() {
    GL11.glFlush();
  }

  public void finish() {
    GL11.glFinish();
  }

  public void hint(int target, int hint) {
    GL11.glHint(target, hint);
  }

  ///////////////////////////////////////////////////////////

  // State and State Requests

  public void enable(int value) {
    if (-1 < value) {
      GL11.glEnable(value);
    }
  }

  public void disable(int value) {
    if (-1 < value) {
      GL11.glDisable(value);
    }
  }

  public void getBooleanv(int value, IntBuffer data) {
    if (-1 < value) {
      if (byteBuffer.capacity() < data.capacity()) {
        byteBuffer = allocateDirectByteBuffer(data.capacity());
      }
      GL11.glGetBoolean(value, byteBuffer);
      for (int i = 0; i < data.capacity(); i++) {
        data.put(i, byteBuffer.get(i));
      }
    } else {
      fillIntBuffer(data, 0, data.capacity() - 1, 0);
    }
  }

  public void getIntegerv(int value, IntBuffer data) {
    if (-1 < value) {
      GL11.glGetInteger(value, data);
    } else {
      fillIntBuffer(data, 0, data.capacity() - 1, 0);
    }
  }

  public void getFloatv(int value, FloatBuffer data) {
    if (-1 < value) {
      GL11.glGetFloat(value, data);
    } else {
      fillFloatBuffer(data, 0, data.capacity() - 1, 0);
    }
  }

  public boolean isEnabled(int value) {
    return GL11.glIsEnabled(value);
  }

  public String getString(int name) {
    return GL11.glGetString(name);
  }

  ///////////////////////////////////////////////////////////

  // Error Handling

  public int getError() {
    return GL11.glGetError();
  }

  public String errorString(int err) {
    return GLU.gluErrorString(err);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Buffer Objects

  public void genBuffers(int n, IntBuffer buffers) {
    GL15.glGenBuffers(buffers);
  }

  public void deleteBuffers(int n, IntBuffer buffers) {
    GL15.glDeleteBuffers(buffers);
  }

  public void bindBuffer(int target, int buffer) {
    GL15.glBindBuffer(target, buffer);
  }

  public void bufferData(int target, int size, Buffer data, int usage) {
    if (data == null) {
      FloatBuffer empty = BufferUtils.createFloatBuffer(size);
      GL15.glBufferData(target, empty, usage);
    } else {
      if (data instanceof ByteBuffer) {
        GL15.glBufferData(target, (ByteBuffer)data, usage);
      } else if (data instanceof ShortBuffer) {
        GL15.glBufferData(target, (ShortBuffer)data, usage);
      } else if (data instanceof IntBuffer) {
        GL15.glBufferData(target, (IntBuffer)data, usage);
      } else if (data instanceof FloatBuffer) {
        GL15.glBufferData(target, (FloatBuffer)data, usage);
      }
    }
  }

  public void bufferSubData(int target, int offset, int size, Buffer data) {
    if (data instanceof ByteBuffer) {
      GL15.glBufferSubData(target, offset, (ByteBuffer)data);
    } else if (data instanceof ShortBuffer) {
      GL15.glBufferSubData(target, offset, (ShortBuffer)data);
    } else if (data instanceof IntBuffer) {
      GL15.glBufferSubData(target, offset, (IntBuffer)data);
    } else if (data instanceof FloatBuffer) {
      GL15.glBufferSubData(target, offset, (FloatBuffer)data);
    }
  }

  public void isBuffer(int buffer) {
    GL15.glIsBuffer(buffer);
  }

  public void getBufferParameteriv(int target, int value, IntBuffer data) {
    if (-1 < value) {
      int res = GL15.glGetBufferParameteri(target, value);
      data.put(0, res);
    } else {
      data.put(0, 0);
    }
  }

  public ByteBuffer mapBuffer(int target, int access) {
    return GL15.glMapBuffer(target, access, null);
  }

  public ByteBuffer mapBufferRange(int target, int offset, int length, int access) {
    return GL30.glMapBufferRange(target, offset, length, access, null);
  }

  public void unmapBuffer(int target) {
    GL15.glUnmapBuffer(target);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Viewport and Clipping

  public void depthRangef(float n, float f) {
    GL11.glDepthRange(n, f);
  }

  public void viewport(int x, int y, int w, int h) {
    GL11.glViewport(x, y, w, h);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Reading Pixels

  protected void readPixelsImpl(int x, int y, int width, int height, int format, int type, Buffer buffer) {
    GL11.glReadPixels(x, y, width, height, format, type, (IntBuffer)buffer);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Vertices

  public void vertexAttrib1f(int index, float value) {
    GL20.glVertexAttrib1f(index, value);
  }

  public void vertexAttrib2f(int index, float value0, float value1) {
    GL20.glVertexAttrib2f(index, value0, value1);
  }

  public void vertexAttrib3f(int index, float value0, float value1, float value2) {
    GL20.glVertexAttrib3f(index, value0, value1, value2);
  }

  public void vertexAttrib4f(int index, float value0, float value1, float value2, float value3) {
    GL20.glVertexAttrib4f(index, value0, value1, value2, value3);
  }

  public void vertexAttrib1fv(int index, FloatBuffer values) {
    GL20.glVertexAttrib1f(index, values.get());
  }

  public void vertexAttrib2fv(int index, FloatBuffer values) {
    GL20.glVertexAttrib2f(index, values.get(), values.get());
  }

  public void vertexAttrib3fv(int index, FloatBuffer values) {
    GL20.glVertexAttrib3f(index, values.get(), values.get(), values.get());
  }

  public void vertexAttri4fv(int index, FloatBuffer values) {
    GL20.glVertexAttrib4f(index, values.get(), values.get(), values.get(), values.get());
  }

  public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset) {
    GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset);
  }

  public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, Buffer data) {
    if (type == UNSIGNED_INT) {
      GL20.glVertexAttribPointer(index, size, true, normalized, stride, (IntBuffer)data);
    } else if (type == UNSIGNED_BYTE) {
      GL20.glVertexAttribPointer(index, size, true, normalized, stride, (ByteBuffer)data);
    } else if (type == UNSIGNED_SHORT) {
      GL20.glVertexAttribPointer(index, size, true, normalized, stride, (ShortBuffer)data);
    } else if (type == FLOAT) {
      GL20.glVertexAttribPointer(index, size, normalized, stride, (FloatBuffer)data);
    }
  }

  public void enableVertexAttribArray(int index) {
    GL20.glEnableVertexAttribArray(index);
  }

  public void disableVertexAttribArray(int index) {
    GL20.glDisableVertexAttribArray(index);
  }

  public void drawArrays(int mode, int first, int count) {
    GL11.glDrawArrays(mode, first, count);
  }

  public void drawElements(int mode, int count, int type, int offset) {
    GL11.glDrawElements(mode, count, type, offset);
  }

  public void drawElements(int mode, int count, int type, Buffer indices) {
    if (type == UNSIGNED_INT) {
      GL11.glDrawElements(mode, (IntBuffer)indices);
    } else if (type == UNSIGNED_BYTE) {
      GL11.glDrawElements(mode, (ByteBuffer)indices);
    } else if (type == UNSIGNED_SHORT) {
      GL11.glDrawElements(mode, (ShortBuffer)indices);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  // Rasterization

  public void lineWidth(float width) {
    GL11.glLineWidth(width);
  }

  public void frontFace(int dir) {
    GL11.glFrontFace(dir);
  }

  public void cullFace(int mode) {
    GL11.glCullFace(mode);
  }

  public void polygonOffset(float factor, float units) {
    GL11.glPolygonOffset(factor, units);
  }

  //////////////////////////////////////////////////////////////////////////////

  // Pixel Rectangles

  public void pixelStorei(int pname, int param) {
    GL11.glPixelStorei(pname, param);
  }

  ///////////////////////////////////////////////////////////

  // Texturing

  public void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data) {
    GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, (IntBuffer)data);
  }

  public void copyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) {
    GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border);
  }

  public void texSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, Buffer data) {
    GL11.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, (IntBuffer)data);
  }

  public void copyTexSubImage2D(int target, int level, int xOffset, int yOffset, int x, int y, int width, int height) {
    GL11.glCopyTexSubImage2D(target, level, x, y, xOffset, xOffset, width, height);
  }

  public void compressedTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int imageSize, Buffer data) {
    GL13.glCompressedTexImage2D(target, level, internalFormat, width, height, border, (ByteBuffer)data);
  }

  public void compressedTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int imageSize, Buffer data) {
    GL13.glCompressedTexSubImage2D(target, level, xOffset, yOffset, width, height, format, (ByteBuffer)data);
  }

  public void texParameteri(int target, int pname, int param) {
    GL11.glTexParameteri(target, pname, param);
  }

  public void texParameterf(int target, int pname, float param) {
    GL11.glTexParameterf(target, pname, param);
  }

  public void texParameteriv(int target, int pname, IntBuffer params) {
    GL11.glTexParameteri(target, pname, params.get());
  }

  public void texParameterfv(int target, int pname, FloatBuffer params) {
    GL11.glTexParameterf(target, pname, params.get());
  }

  public void generateMipmap(int target) {
    GL30.glGenerateMipmap(target);
  }

  public void genTextures(int n, IntBuffer textures) {
    GL11.glGenTextures(textures);
  }

  public void deleteTextures(int n, IntBuffer textures) {
    GL11.glDeleteTextures(textures);
  }

  public void getTexParameteriv(int target, int pname, IntBuffer params) {
    GL11.glGetTexParameter(target, pname, params);
  }

  public void getTexParameterfv(int target, int pname, FloatBuffer params) {
    GL11.glGetTexParameter(target, pname, params);
  }

  public boolean isTexture(int texture) {
    return GL11.glIsTexture(texture);
  }

  protected void activeTextureImpl(int texture) {
    GL13.glActiveTexture(texture);
  }
    
  protected void bindTextureImpl(int target, int texture) {
    GL11.glBindTexture(target, texture);
  }
    
  ///////////////////////////////////////////////////////////

  // Shaders and Programs

  public int createShader(int type) {
    return GL20.glCreateShader(type);
  }

  public void shaderSource(int shader, String source) {
    GL20.glShaderSource(shader, source);
  }

  public void compileShader(int shader) {
    GL20.glCompileShader(shader);
  }

  public void releaseShaderCompiler() {
    throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glReleaseShaderCompiler()"));
  }

  public void deleteShader(int shader) {
    GL20.glDeleteShader(shader);
  }

  public void shaderBinary(int count, IntBuffer shaders, int binaryFormat, Buffer binary, int length) {
    throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glShaderBinary()"));
  }

  public int createProgram() {
    return GL20.glCreateProgram();
  }

  public void attachShader(int program, int shader) {
    GL20.glAttachShader(program, shader);
  }

  public void detachShader(int program, int shader) {
    GL20.glDetachShader(program, shader);
  }

  public void linkProgram(int program) {
    GL20.glLinkProgram(program);
  }

  public void useProgram(int program) {
    GL20.glUseProgram(program);
  }

  public void deleteProgram(int program) {
    GL20.glDeleteProgram(program);
  }

  public String getActiveAttrib (int program, int index, IntBuffer size, IntBuffer type) {
    IntBuffer typeTmp = BufferUtils.createIntBuffer(2);
    String name = GL20.glGetActiveAttrib(program, index, 256, typeTmp);
    size.put(typeTmp.get(0));
    type.put(typeTmp.get(1));
    return name;    
  }

  public int getAttribLocation(int program, String name) {
    return GL20.glGetAttribLocation(program, name);
  }

  public void bindAttribLocation(int program, int index, String name) {
    GL20.glBindAttribLocation(program, index, name);
  }

  public int getUniformLocation(int program, String name) {
    return GL20.glGetUniformLocation(program, name);
  }

  public String getActiveUniform(int program, int index, IntBuffer size, IntBuffer type) {
    IntBuffer typeTmp = BufferUtils.createIntBuffer(2);
    String name = GL20.glGetActiveUniform(program, index, 256, typeTmp);
    type.put(typeTmp.get(0));
    return name;
  }

  public void uniform1i(int location, int value) {
    GL20.glUniform1i(location, value);
  }

  public void uniform2i(int location, int value0, int value1) {
    GL20.glUniform2i(location, value0, value1);
  }

  public void uniform3i(int location, int value0, int value1, int value2) {
    GL20.glUniform3i(location, value0, value1, value2);
  }

  public void uniform4i(int location, int value0, int value1, int value2, int value3) {
    GL20.glUniform4i(location, value0, value1, value2, value3);
  }

  public void uniform1f(int location, float value) {
    GL20.glUniform1f(location, value);
  }

  public void uniform2f(int location, float value0, float value1) {
    GL20.glUniform2f(location, value0, value1);
  }

  public void uniform3f(int location, float value0, float value1, float value2) {
    GL20.glUniform3f(location, value0, value1, value2);
  }

  public void uniform4f(int location, float value0, float value1, float value2, float value3) {
    GL20.glUniform4f(location, value0, value1, value2, value3);
  }

  public void uniform1iv(int location, int count, IntBuffer v) {
    v.limit(count);
    GL20.glUniform1(location, v);
    v.clear();
  }

  public void uniform2iv(int location, int count, IntBuffer v) {
    v.limit(count);
    GL20.glUniform2(location, v);
    v.clear();
  }

  public void uniform3iv(int location, int count, IntBuffer v) {
    v.limit(count);
    GL20.glUniform3(location, v);
    v.clear();
  }

  public void uniform4iv(int location, int count, IntBuffer v) {
    v.limit(count);
    GL20.glUniform4(location, v);
    v.clear();
  }

  public void uniform1fv(int location, int count, FloatBuffer v) {
    v.limit(count);
    GL20.glUniform1(location, v);
    v.clear();
  }

  public void uniform2fv(int location, int count, FloatBuffer v) {
    v.limit(count);
    GL20.glUniform2(location, v);
    v.clear();
  }

  public void uniform3fv(int location, int count, FloatBuffer v) {
    v.limit(count);
    GL20.glUniform3(location, v);
    v.clear();
  }

  public void uniform4fv(int location, int count, FloatBuffer v) {
    v.limit(count);
    GL20.glUniform4(location, v);
    v.clear();
  }  

  public void uniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer mat) {
    mat.limit(4);
    GL20.glUniformMatrix2(location, transpose, mat);
    mat.clear();
  }

  public void uniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer mat) {
    mat.limit(9);
    GL20.glUniformMatrix3(location, transpose, mat);
    mat.clear();
  }

  public void uniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer mat) {
    mat.limit(16);
    GL20.glUniformMatrix4(location, transpose, mat);
    mat.clear();
  }

  public void validateProgram(int program) {
    GL20.glValidateProgram(program);
  }

  public boolean isShader(int shader) {
    return GL20.glIsShader(shader);
  }

  public void getShaderiv(int shader, int pname, IntBuffer params) {
    GL20.glGetShader(shader, pname, params);
  }

  public void getAttachedShaders(int program, int maxCount, IntBuffer count, IntBuffer shaders) {
    GL20.glGetAttachedShaders(program, count, shaders);
  }

  public String getShaderInfoLog(int shader) {
    int len = GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH);
    return GL20.glGetShaderInfoLog(shader, len);
  }

  public String getShaderSource(int shader) {
    int len = GL20.glGetShaderi(shader, GL20.GL_SHADER_SOURCE_LENGTH);
    return GL20.glGetShaderSource(shader, len);
  }

  public void getShaderPrecisionFormat(int shaderType, int precisionType, IntBuffer range, IntBuffer precision) {
    throw new RuntimeException(String.format(MISSING_GLFUNC_ERROR, "glGetShaderPrecisionFormat()"));
  }

  public void getVertexAttribfv(int index, int pname, FloatBuffer params) {
    GL20.glGetVertexAttrib(index, pname, params);
  }

  public void getVertexAttribiv(int index, int pname, IntBuffer params) {
    GL20.glGetVertexAttrib(index, pname, params);
  }

  public void getVertexAttribPointerv(int index, int pname, ByteBuffer data) {
    int len = data.capacity();
    ByteBuffer res = GL20.glGetVertexAttribPointer(index, pname, len);
    data.put(res);
  }

  public void getUniformfv(int program, int location, FloatBuffer params) {
    GL20.glGetUniform(program, location, params);
  }

  public void getUniformiv(int program, int location, IntBuffer params) {
    GL20.glGetUniform(program, location, params);
  }

  public boolean isProgram(int program) {
    return GL20.glIsProgram(program);
  }

  public void getProgramiv(int program, int pname, IntBuffer params) {
    GL20.glGetProgram(program, pname, params);
  }

  public String getProgramInfoLog(int program) {
    int len = GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH);
    return GL20.glGetProgramInfoLog(program, len);
  }

  ///////////////////////////////////////////////////////////

  // Per-Fragment Operations

  public void scissor(int x, int y, int w, int h) {
    GL11.glScissor(x, y, w, h);
  }

  public void sampleCoverage(float value, boolean invert) {
    GL13.glSampleCoverage(value, invert);
  }

  public void stencilFunc(int func, int ref, int mask) {
    GL11.glStencilFunc(func, ref, mask);
  }

  public void stencilFuncSeparate(int face, int func, int ref, int mask) {
    GL20.glStencilFuncSeparate(face, func, ref, mask);
  }

  public void stencilOp(int sfail, int dpfail, int dppass) {
    GL11.glStencilOp(sfail, dpfail, dppass);
  }

  public void stencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
    GL20.glStencilOpSeparate(face, sfail, dpfail, dppass);
  }

  public void depthFunc(int func) {
    GL11.glDepthFunc(func);
  }

  public void blendEquation(int mode) {
    GL14.glBlendEquation(mode);
  }

  public void blendEquationSeparate(int modeRGB, int modeAlpha) {
    GL20.glBlendEquationSeparate(modeRGB, modeAlpha);
  }

  public void blendFunc(int src, int dst) {
    GL11.glBlendFunc(src, dst);
  }

  public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
    GL14.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
  }

  public void blendColor(float red, float green, float blue, float alpha) {
    GL14.glBlendColor(red, green, blue, alpha);
  }

  public void alphaFunc(int func, float ref) {
    GL11.glAlphaFunc(func, ref);
  }

  ///////////////////////////////////////////////////////////

  // Whole Framebuffer Operations

  public void colorMask(boolean r, boolean g, boolean b, boolean a) {
    GL11.glColorMask(r, g, b, a);
  }

  public void depthMask(boolean mask) {
    GL11.glDepthMask(mask);
  }

  public void stencilMask(int mask) {
    GL11.glStencilMask(mask);
  }

  public void stencilMaskSeparate(int face, int mask) {
    GL20.glStencilMaskSeparate(face, mask);
  }

  public void clear(int buf) {
    GL11.glClear(buf);
  }

  public void clearColor(float r, float g, float b, float a) {
    GL11.glClearColor(r, g, b, a);
  }

  public void clearDepth(float d) {
    GL11.glClearDepth(d);
  }

  public void clearStencil(int s) {
    GL11.glClearStencil(s);
  }

  ///////////////////////////////////////////////////////////

  // Framebuffers Objects

  public void bindFramebuffer(int target, int framebuffer) {
    GL30.glBindFramebuffer(target, framebuffer);
  }

  public void deleteFramebuffers(int n, IntBuffer framebuffers) {
    GL30.glDeleteFramebuffers(framebuffers);
  }

  public void genFramebuffers(int n, IntBuffer framebuffers) {
    GL30.glGenFramebuffers(framebuffers);
  }

  public void bindRenderbuffer(int target, int renderbuffer) {
    GL30.glBindRenderbuffer(target, renderbuffer);
  }

  public void deleteRenderbuffers(int n, IntBuffer renderbuffers) {
    GL30.glDeleteRenderbuffers(renderbuffers);
  }

  public void genRenderbuffers(int n, IntBuffer renderbuffers) {
    GL30.glGenRenderbuffers(renderbuffers);
  }

  public void renderbufferStorage(int target, int internalFormat, int width, int height) {
    GL30.glRenderbufferStorage(target, internalFormat, width, height);
  }

  public void framebufferRenderbuffer(int target, int attachment, int rendbuferfTarget, int renderbuffer) {
    GL30.glFramebufferRenderbuffer(target, attachment, rendbuferfTarget, renderbuffer);
  }

  public void framebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
    GL30.glFramebufferTexture2D(target, attachment, texTarget, texture, level);
  }

  public int checkFramebufferStatus(int target) {
    return GL30.glCheckFramebufferStatus(target);
  }

  public boolean isFramebuffer(int framebuffer) {
    return GL30.glIsFramebuffer(framebuffer);
  }

  public void getFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params) {
    GL30.glGetFramebufferAttachmentParameter(target, attachment, pname, params);
  }

  public boolean isRenderbuffer(int renderbuffer) {
    return GL30.glIsRenderbuffer(renderbuffer);
  }

  public void getRenderbufferParameteriv(int target, int pname, IntBuffer params) {
    GL30.glGetRenderbufferParameter(target, pname, params);
  }

  public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
    GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
  }

  public void renderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
    GL30.glRenderbufferStorageMultisample(target, samples, format, width, height);
  }

  public void readBuffer(int buf) {
    GL11.glReadBuffer(buf);
  }

  public void drawBuffer(int buf) {
    GL11.glDrawBuffer(buf);
  }


  @Override
  protected void getGL(PGL pgl) {
  }
}
