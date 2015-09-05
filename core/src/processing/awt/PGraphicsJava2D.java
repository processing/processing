/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
  Copyright (c) 2005-13 Ben Fry and Casey Reas

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

package processing.awt;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import processing.core.*;


/**
 * Subclass for PGraphics that implements the graphics API using Java2D.
 * <p>
 * To get access to the Java 2D "Graphics2D" object for the default
 * renderer, use:
 * <PRE>
 * Graphics2D g2 = (Graphics2D) g.getNative();
 * </PRE>
 * This will let you do Graphics2D calls directly, but is not supported
 * in any way shape or form. Which just means "have fun, but don't complain
 * if it breaks."
 * <p>
 * Advanced <a href="http://docs.oracle.com/javase/7/docs/webnotes/tsg/TSG-Desktop/html/java2d.html">debugging notes</a> for Java2D.
 */
public class PGraphicsJava2D extends PGraphics {
////  BufferStrategy strategy;
////  BufferedImage bimage;
////  VolatileImage vimage;
//  Canvas canvas;
////  boolean useCanvas = true;
//  boolean useCanvas = false;
////  boolean useRetina = true;
////  boolean useOffscreen = true;  // ~40fps
//  boolean useOffscreen = false;

  public Graphics2D g2;
//  protected BufferedImage offscreen;

  Composite defaultComposite;

  GeneralPath gpath;

  // path for contours so gpath can be closed
  GeneralPath auxPath;

  boolean openContour;

  /// break the shape at the next vertex (next vertex() call is a moveto())
  boolean breakShape;

  /// coordinates for internal curve calculation
  float[] curveCoordX;
  float[] curveCoordY;
  float[] curveDrawX;
  float[] curveDrawY;

  int transformCount;
  AffineTransform transformStack[] =
    new AffineTransform[MATRIX_STACK_DEPTH];
  double[] transform = new double[6];

  Line2D.Float line = new Line2D.Float();
  Ellipse2D.Float ellipse = new Ellipse2D.Float();
  Rectangle2D.Float rect = new Rectangle2D.Float();
  Arc2D.Float arc = new Arc2D.Float();

  protected Color tintColorObject;

  protected Color fillColorObject;
  public boolean fillGradient;
  public Paint fillGradientObject;

  protected Stroke strokeObject;
  protected Color strokeColorObject;
  public boolean strokeGradient;
  public Paint strokeGradientObject;

  Font fontObject;



  //////////////////////////////////////////////////////////////

  // INTERNAL


  public PGraphicsJava2D() { }


  //public void setParent(PApplet parent)


  //public void setPrimary(boolean primary)


  //public void setPath(String path)


//  /**
//   * Called in response to a resize event, handles setting the
//   * new width and height internally, as well as re-allocating
//   * the pixel buffer for the new size.
//   *
//   * Note that this will nuke any cameraMode() settings.
//   */
//  @Override
//  public void setSize(int iwidth, int iheight) {  // ignore
//    width = iwidth;
//    height = iheight;
//
//    allocate();
//    reapplySettings();
//  }


//  @Override
//  protected void allocate() {
//    //surface.initImage(this, width, height);
//    surface.initImage(this);
//  }


  /*
  @Override
  protected void allocate() {
    // Tried this with RGB instead of ARGB for the primarySurface version,
    // but didn't see any performance difference (OS X 10.6, Java 6u24).
    // For 0196, also attempted RGB instead of ARGB, but that causes
    // strange things to happen with blending.
//    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    if (primarySurface) {
      if (useCanvas) {
        if (canvas != null) {
          parent.removeListeners(canvas);
          parent.remove(canvas);
        }
        canvas = new Canvas();
        canvas.setIgnoreRepaint(true);

//        parent.setLayout(new BorderLayout());
//        parent.add(canvas, BorderLayout.CENTER);
        parent.add(canvas);
//        canvas.validate();
//        parent.doLayout();

        if (canvas.getWidth() != width || canvas.getHeight() != height) {
          PApplet.debug("PGraphicsJava2D comp size being set to " + width + "x" + height);
          canvas.setSize(width, height);
        } else {
          PApplet.debug("PGraphicsJava2D comp size already " + width + "x" + height);
        }

        parent.addListeners(canvas);
//        canvas.createBufferStrategy(1);
//        g2 = (Graphics2D) canvas.getGraphics();

      } else {
        parent.updateListeners(parent);  // in case they're already there

        // using a compatible image here doesn't seem to provide any performance boost

        if (useOffscreen) {
          // Needs to be RGB otherwise there's a major performance hit [0204]
          // http://code.google.com/p/processing/issues/detail?id=729
          image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        GraphicsConfiguration gc = parent.getGraphicsConfiguration();
//        image = gc.createCompatibleImage(width, height);
          offscreen = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//        offscreen = gc.createCompatibleImage(width, height);
          g2 = (Graphics2D) offscreen.getGraphics();

        } else {
//          System.out.println("hopefully faster " + width + " " + height);
//          new Exception().printStackTrace(System.out);

          GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
          // If not realized (off-screen, i.e the Color Selector Tool),
          // gc will be null.
          if (gc == null) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
          }

          image = gc.createCompatibleImage(width, height);
          g2 = (Graphics2D) image.getGraphics();
        }
      }
    } else {  // not the primary surface
      // Since this buffer's offscreen anyway, no need for the extra offscreen
      // buffer. However, unlike the primary surface, this feller needs to be
      // ARGB so that blending ("alpha" compositing) will work properly.
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      g2 = (Graphics2D) image.getGraphics();
    }
    */

    /*
    if (primarySurface) {
      Canvas canvas = ((PSurfaceAWT) surface).canvas;

      GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
      // If not realized (off-screen, i.e the Color Selector Tool),
      // gc will be null.
      if (gc == null) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
      }

      image = gc.createCompatibleImage(width, height);
      g2 = (Graphics2D) image.getGraphics();

    } else {

    }
    g2 = (Graphics2D) image.getGraphics();
  }
     */


  //public void dispose()


  @Override
  public PSurface createSurface() {
    return surface = new PSurfaceAWT(this);
  }


  /** Returns the java.awt.Graphics2D object used by this renderer. */
  @Override
  public Object getNative() {
    return g2;
  }


  //////////////////////////////////////////////////////////////

  // FRAME


//  @Override
//  public boolean canDraw() {
//    return true;
//  }


//  @Override
//  public void requestDraw() {
////    EventQueue.invokeLater(new Runnable() {
////      public void run() {
//    parent.handleDraw();
////      }
////    });
//  }


//  Graphics2D g2old;

  public Graphics2D checkImage() {
    if (image == null ||
      ((BufferedImage) image).getWidth() != width*pixelDensity ||
      ((BufferedImage) image).getHeight() != height*pixelDensity) {
//      ((VolatileImage) image).getWidth() != width ||
//      ((VolatileImage) image).getHeight() != height) {
//        image = new BufferedImage(width * pixelFactor, height * pixelFactor
//                                  format == RGB ?  BufferedImage.TYPE_INT_ARGB);

      GraphicsConfiguration gc = null;
      if (surface != null) {
        Component comp = null;  //surface.getComponent();
        if (comp == null) {
//          System.out.println("component null, but parent.frame is " + parent.frame);
          comp = parent.frame;
        }
        if (comp != null) {
          gc = comp.getGraphicsConfiguration();
        }
      }
      // If not realized (off-screen, i.e the Color Selector Tool), gc will be null.
      if (gc == null) {
        //System.err.println("GraphicsConfiguration null in initImage()");
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
      }

      // Formerly this was broken into separate versions based on offscreen or
      // not, but we may as well create a compatible image; it won't hurt, right?
      int wide = width * pixelDensity;
      int high = height * pixelDensity;
//      System.out.println("re-creating image");
      image = gc.createCompatibleImage(wide, high, Transparency.TRANSLUCENT);
//      image = gc.createCompatibleVolatileImage(wide, high);
      //image = surface.getComponent().createImage(width, height);
    }
    return (Graphics2D) image.getGraphics();
  }


  @Override
  public void beginDraw() {
    g2 = checkImage();

    // Calling getGraphics() seems to nuke several settings.
    // It seems to be re-creating a new Graphics2D object each time.
    // https://github.com/processing/processing/issues/3331
    if (strokeObject != null) {
      g2.setStroke(strokeObject);
    }
    // https://github.com/processing/processing/issues/2617
    if (fontObject != null) {
      g2.setFont(fontObject);
    }

    handleSmooth();

    /*
    // NOTE: Calling image.getGraphics() will create a new Graphics context,
    // even if it's for the same image that's already had a context created.
    // Seems like a speed/memory issue, and also requires that all smoothing,
    // stroke, font and other props be reset. Can't find a good answer about
    // whether getGraphics() and dispose() on each frame is 1) better practice
    // and 2) minimal overhead, however. Instinct suggests #1 may be true,
    // but #2 seems a problem.
    if (primarySurface && !useOffscreen) {
      GraphicsConfiguration gc = canvas.getGraphicsConfiguration();
      if (false) {
        if (image == null || ((VolatileImage) image).validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
          image = gc.createCompatibleVolatileImage(width, height);
          g2 = (Graphics2D) image.getGraphics();
          reapplySettings = true;
        }
      } else {
        if (image == null) {
          image = gc.createCompatibleImage(width, height);
          PApplet.debug("created new image, type is " + image);
          g2 = (Graphics2D) image.getGraphics();
          reapplySettings = true;
        }
      }
    }

    if (useCanvas && primarySurface) {
      if (parent.frameCount == 0) {
        canvas.createBufferStrategy(2);
        strategy = canvas.getBufferStrategy();
        PApplet.debug("PGraphicsJava2D.beginDraw() strategy is " + strategy);
        BufferCapabilities caps = strategy.getCapabilities();
        caps = strategy.getCapabilities();
        PApplet.debug("PGraphicsJava2D.beginDraw() caps are " +
                      " flipping: " + caps.isPageFlipping() +
                      " front/back accel: " + caps.getFrontBufferCapabilities().isAccelerated() + " " +
                      "/" + caps.getBackBufferCapabilities().isAccelerated());
      }
      GraphicsConfiguration gc = canvas.getGraphicsConfiguration();

      if (bimage == null ||
          bimage.getWidth() != width ||
          bimage.getHeight() != height) {
        PApplet.debug("PGraphicsJava2D creating new image");
        bimage = gc.createCompatibleImage(width, height);
//        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2 = bimage.createGraphics();
        defaultComposite = g2.getComposite();
        reapplySettings = true;
      }
    }
    */

    checkSettings();
    resetMatrix(); // reset model matrix
    vertexCount = 0;

    g2.scale(pixelDensity, pixelDensity);
  }


  /**
   * Smoothing for Java2D is 2 for bilinear, and 3 for bicubic (the default).
   * Internally, smooth(1) is the default, smooth(0) is noSmooth().
   */
  protected void handleSmooth() {
    if (smooth == 0) {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_OFF);
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                          RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

    } else {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);

      if (smooth == 1 || smooth == 3) {  // default is bicubic
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      } else if (smooth == 2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      }

      // http://docs.oracle.com/javase/tutorial/2d/text/renderinghints.html
      // Oracle Java text anti-aliasing on OS X looks like s*t compared to the
      // text rendering with Apple's old Java 6. Below, several attempts to fix:
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      // Turns out this is the one that actually makes things work.
      // Kerning is still screwed up, however.
      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                          RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
//                        RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
//    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
//                         RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

//    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
//                        RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }
  }


  @Override
  public void endDraw() {
    // hm, mark pixels as changed, because this will instantly do a full
    // copy of all the pixels to the surface.. so that's kind of a mess.
    //updatePixels();

    if (primaryGraphics) {
      /*
      //if (canvas != null) {
      if (useCanvas) {
        //System.out.println(canvas);

        // alternate version
        //canvas.repaint();  // ?? what to do for swapping buffers

//        System.out.println("endDraw() frameCount is " + parent.frameCount);
//        if (parent.frameCount != 0) {
        redraw();
//        }

      } else if (useOffscreen) {
        // don't copy the pixels/data elements of the buffered image directly,
        // since it'll disable the nice speedy pipeline stuff, sending all drawing
        // into a world of suck that's rough 6 trillion times slower.
        synchronized (image) {
          //System.out.println("inside j2d sync");
          image.getGraphics().drawImage(offscreen, 0, 0, null);
        }

      } else {
        // changed to not dispose and get on each frame,
        // otherwise a new Graphics context is used on each frame
//        g2.dispose();
//        System.out.println("not doing anything special in endDraw()");
      }
      */
    } else {
      // TODO this is probably overkill for most tasks...
      loadPixels();
    }

//    // Marking as modified, and then calling updatePixels() in
//    // the super class, which just sets the mx1, my1, mx2, my2
//    // coordinates of the modified area. This avoids doing the
//    // full copy of the pixels to the surface in this.updatePixels().
//    setModified();
//    super.updatePixels();

    // Marks pixels as modified so that the pixels will be updated.
    // Also sets mx1/y1/x2/y2 so that OpenGL will pick it up.
    setModified();

    g2.dispose();
  }


  /*
  private void redraw() {
    // only need this check if the validate() call will use redraw()
//    if (strategy == null) return;
    do {
      PApplet.debug("PGraphicsJava2D.redraw() top of outer do { } block");
      do {
        PApplet.debug("PGraphicsJava2D.redraw() top of inner do { } block");
        PApplet.debug("strategy is " + strategy);
        Graphics bsg = strategy.getDrawGraphics();
//        if (vimage != null) {
//          bsg.drawImage(vimage, 0, 0, null);
//        } else {
        bsg.drawImage(bimage, 0, 0, null);
//      if (parent.frameCount == 0) {
//        try {
//          ImageIO.write(image, "jpg", new java.io.File("/Users/fry/Desktop/buff.jpg"));
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//        }
        bsg.dispose();

        // the strategy version
//    g2.dispose();
//      if (!strategy.contentsLost()) {
//      if (parent.frameCount != 0) {
//      Toolkit.getDefaultToolkit().sync();
//      }
//      } else {
//        System.out.println("XXXXX strategy contents lost");
//      }
//    }
//    }
      } while (strategy.contentsRestored());

      PApplet.debug("PGraphicsJava2D.redraw() showing strategy");
      strategy.show();

    } while (strategy.contentsLost());
    PApplet.debug("PGraphicsJava2D.redraw() out of do { } block");
  }
  */



  //////////////////////////////////////////////////////////////

  // SETTINGS


  //protected void checkSettings()


  @Override
  protected void defaultSettings() {
//    if (!useCanvas) {
//      // Papered over another threading issue...
//      // See if this comes back now that the other issue is fixed.
////      while (g2 == null) {
////        try {
////          System.out.println("sleeping until g2 is available");
////          Thread.sleep(5);
////        } catch (InterruptedException e) { }
////      }
    defaultComposite = g2.getComposite();
//    }
    super.defaultSettings();
  }


  //protected void reapplySettings()



  //////////////////////////////////////////////////////////////

  // HINT


  @Override
  public void hint(int which) {
    // take care of setting the hint
    super.hint(which);

    // Avoid badness when drawing shorter strokes.
    // http://code.google.com/p/processing/issues/detail?id=1068
    // Unfortunately cannot always be enabled, because it makes the
    // stroke in many standard Processing examples really gross.
    if (which == ENABLE_STROKE_PURE) {
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                          RenderingHints.VALUE_STROKE_PURE);
    } else if (which == DISABLE_STROKE_PURE) {
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                          RenderingHints.VALUE_STROKE_DEFAULT);
    }
  }



  //////////////////////////////////////////////////////////////

  // SHAPE CREATION


  @Override
  protected PShape createShapeFamily(int type) {
    return new PShape(this, type);
  }


  @Override
  protected PShape createShapePrimitive(int kind, float... p) {
    return new PShape(this, kind, p);
  }


//  @Override
//  public PShape createShape(PShape source) {
//    return PShapeOpenGL.createShape2D(this, source);
//  }


  /*
  protected PShape createShapeImpl(PGraphicsJava2D pg, int type) {
    PShape shape = null;
    if (type == PConstants.GROUP) {
      shape = new PShape(pg, PConstants.GROUP);
    } else if (type == PShape.PATH) {
      shape = new PShape(pg, PShape.PATH);
    } else if (type == PShape.GEOMETRY) {
      shape = new PShape(pg, PShape.GEOMETRY);
    }
    // defaults to false, don't assign it and make complexity for overrides
    //shape.set3D(false);
    return shape;
  }
   */


  /*
  static protected PShape createShapeImpl(PGraphicsJava2D pg,
                                                int kind, float... p) {
    PShape shape = null;
    int len = p.length;

    if (kind == POINT) {
      if (len != 2) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape(pg, PShape.PRIMITIVE);
      shape.setKind(POINT);
    } else if (kind == LINE) {
      if (len != 4) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape(pg, PShape.PRIMITIVE);
      shape.setKind(LINE);
    } else if (kind == TRIANGLE) {
      if (len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape(pg, PShape.PRIMITIVE);
      shape.setKind(TRIANGLE);
    } else if (kind == QUAD) {
      if (len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape(pg, PShape.PRIMITIVE);
      shape.setKind(QUAD);
    } else if (kind == RECT) {
      if (len != 4 && len != 5 && len != 8 && len != 9) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape(pg, PShape.PRIMITIVE);
      shape.setKind(RECT);
    } else if (kind == ELLIPSE) {
      if (len != 4 && len != 5) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape(pg, PShape.PRIMITIVE);
      shape.setKind(ELLIPSE);
    } else if (kind == ARC) {
      if (len != 6 && len != 7) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape(pg, PShape.PRIMITIVE);
      shape.setKind(ARC);
    } else if (kind == BOX) {
      showWarning("Primitive not supported in 2D");
    } else if (kind == SPHERE) {
      showWarning("Primitive not supported in 2D");
    } else {
      showWarning("Unrecognized primitive type");
    }

    if (shape != null) {
      shape.setParams(p);
    }

    // defaults to false, don't assign it and make complexity for overrides
    //shape.set3D(false);

    return shape;
  }
  */



  //////////////////////////////////////////////////////////////

  // SHAPES


  @Override
  public void beginShape(int kind) {
    //super.beginShape(kind);
    shape = kind;
    vertexCount = 0;
    curveVertexCount = 0;

    // set gpath to null, because when mixing curves and straight
    // lines, vertexCount will be set back to zero, so vertexCount == 1
    // is no longer a good indicator of whether the shape is new.
    // this way, just check to see if gpath is null, and if it isn't
    // then just use it to continue the shape.
    gpath = null;
    auxPath = null;
  }


  //public boolean edge(boolean e)


  //public void normal(float nx, float ny, float nz) {


  //public void textureMode(int mode)


  @Override
  public void texture(PImage image) {
    showMethodWarning("texture");
  }


  @Override
  public void vertex(float x, float y) {
    curveVertexCount = 0;
    //float vertex[];

    if (vertexCount == vertices.length) {
      float temp[][] = new float[vertexCount<<1][VERTEX_FIELD_COUNT];
      System.arraycopy(vertices, 0, temp, 0, vertexCount);
      vertices = temp;
      //message(CHATTER, "allocating more vertices " + vertices.length);
    }
    // not everyone needs this, but just easier to store rather
    // than adding another moving part to the code...
    vertices[vertexCount][X] = x;
    vertices[vertexCount][Y] = y;
    vertexCount++;

    switch (shape) {

    case POINTS:
      point(x, y);
      break;

    case LINES:
      if ((vertexCount % 2) == 0) {
        line(vertices[vertexCount-2][X],
             vertices[vertexCount-2][Y], x, y);
      }
      break;

    case TRIANGLES:
      if ((vertexCount % 3) == 0) {
        triangle(vertices[vertexCount - 3][X],
                 vertices[vertexCount - 3][Y],
                 vertices[vertexCount - 2][X],
                 vertices[vertexCount - 2][Y],
                 x, y);
      }
      break;

    case TRIANGLE_STRIP:
      if (vertexCount >= 3) {
        triangle(vertices[vertexCount - 2][X],
                 vertices[vertexCount - 2][Y],
                 vertices[vertexCount - 1][X],
                 vertices[vertexCount - 1][Y],
                 vertices[vertexCount - 3][X],
                 vertices[vertexCount - 3][Y]);
      }
      break;

    case TRIANGLE_FAN:
      if (vertexCount >= 3) {
        // This is an unfortunate implementation because the stroke for an
        // adjacent triangle will be repeated. However, if the stroke is not
        // redrawn, it will replace the adjacent line (when it lines up
        // perfectly) or show a faint line (when off by a small amount).
        // The alternative would be to wait, then draw the shape as a
        // polygon fill, followed by a series of vertices. But that's a
        // poor method when used with PDF, DXF, or other recording objects,
        // since discrete triangles would likely be preferred.
        triangle(vertices[0][X],
                 vertices[0][Y],
                 vertices[vertexCount - 2][X],
                 vertices[vertexCount - 2][Y],
                 x, y);
      }
      break;

    case QUAD:
    case QUADS:
      if ((vertexCount % 4) == 0) {
        quad(vertices[vertexCount - 4][X],
             vertices[vertexCount - 4][Y],
             vertices[vertexCount - 3][X],
             vertices[vertexCount - 3][Y],
             vertices[vertexCount - 2][X],
             vertices[vertexCount - 2][Y],
             x, y);
      }
      break;

    case QUAD_STRIP:
      // 0---2---4
      // |   |   |
      // 1---3---5
      if ((vertexCount >= 4) && ((vertexCount % 2) == 0)) {
        quad(vertices[vertexCount - 4][X],
             vertices[vertexCount - 4][Y],
             vertices[vertexCount - 2][X],
             vertices[vertexCount - 2][Y],
             x, y,
             vertices[vertexCount - 3][X],
             vertices[vertexCount - 3][Y]);
      }
      break;

    case POLYGON:
      if (gpath == null) {
        gpath = new GeneralPath();
        gpath.moveTo(x, y);
      } else if (breakShape) {
        gpath.moveTo(x, y);
        breakShape = false;
      } else {
        gpath.lineTo(x, y);
      }
      break;
    }
  }


  @Override
  public void vertex(float x, float y, float z) {
    showDepthWarningXYZ("vertex");
  }

  @Override
  public void vertex(float[] v) {
    vertex(v[X], v[Y]);
  }


  @Override
  public void vertex(float x, float y, float u, float v) {
    showVariationWarning("vertex(x, y, u, v)");
  }


  @Override
  public void vertex(float x, float y, float z, float u, float v) {
    showDepthWarningXYZ("vertex");
  }


  @Override
  public void beginContour() {
    if (openContour) {
      PGraphics.showWarning("Already called beginContour()");
      return;
    }

    // draw contours to auxiliary path so main path can be closed later
    GeneralPath contourPath = auxPath;
    auxPath = gpath;
    gpath = contourPath;

    if (contourPath != null) {  // first contour does not break
      breakShape = true;
    }

    openContour = true;
  }


  @Override
  public void endContour() {
    if (!openContour) {
      PGraphics.showWarning("Need to call beginContour() first");
      return;
    }

    // close this contour
    if (gpath != null) gpath.closePath();

    // switch back to main path
    GeneralPath contourPath = gpath;
    gpath = auxPath;
    auxPath = contourPath;

    openContour = false;
  }


  @Override
  public void endShape(int mode) {
    if (openContour) { // correct automagically, notify user
      endContour();
      PGraphics.showWarning("Missing endContour() before endShape()");
    }
    if (gpath != null) {  // make sure something has been drawn
      if (shape == POLYGON) {
        if (mode == CLOSE) {
          gpath.closePath();
        }
        if (auxPath != null) {
          gpath.append(auxPath, false);
        }
        drawShape(gpath);
      }
    }
    shape = 0;
  }

  //////////////////////////////////////////////////////////////

  // CLIPPING


  @Override
  protected void clipImpl(float x1, float y1, float x2, float y2) {
    g2.setClip(new Rectangle2D.Float(x1, y1, x2 - x1, y2 - y1));
  }


  @Override
  public void noClip() {
    g2.setClip(null);
  }



  //////////////////////////////////////////////////////////////

  // BLEND

  /**
   * ( begin auto-generated from blendMode.xml )
   *
   * This is a new reference entry for Processing 2.0. It will be updated shortly.
   *
   * ( end auto-generated )
   *
   * @webref Rendering
   * @param mode the blending mode to use
   */
  @Override
  protected void blendModeImpl() {
    if (blendMode == BLEND) {
      g2.setComposite(defaultComposite);

    } else {
      g2.setComposite(new Composite() {

        @Override
        public CompositeContext createContext(ColorModel srcColorModel,
                                              ColorModel dstColorModel,
                                              RenderingHints hints) {
          return new BlendingContext(blendMode);
        }
      });
    }
  }


  // Blending implementation cribbed from portions of Romain Guy's
  // demo and terrific writeup on blending modes in Java 2D.
  // http://www.curious-creature.org/2006/09/20/new-blendings-modes-for-java2d/
  private static final class BlendingContext implements CompositeContext {
    private int mode;

    private BlendingContext(int mode) {
      this.mode = mode;
    }

    public void dispose() { }

    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
      // not sure if this is really necessary, since we control our buffers
      if (src.getSampleModel().getDataType() != DataBuffer.TYPE_INT ||
          dstIn.getSampleModel().getDataType() != DataBuffer.TYPE_INT ||
          dstOut.getSampleModel().getDataType() != DataBuffer.TYPE_INT) {
        throw new IllegalStateException("Source and destination must store pixels as INT.");
      }

      int width = Math.min(src.getWidth(), dstIn.getWidth());
      int height = Math.min(src.getHeight(), dstIn.getHeight());

      int[] srcPixels = new int[width];
      int[] dstPixels = new int[width];

      for (int y = 0; y < height; y++) {
        src.getDataElements(0, y, width, 1, srcPixels);
        dstIn.getDataElements(0, y, width, 1, dstPixels);
        for (int x = 0; x < width; x++) {
          dstPixels[x] = blendColor(dstPixels[x], srcPixels[x], mode);
        }
        dstOut.setDataElements(0, y, width, 1, dstPixels);
      }
    }
  }



  //////////////////////////////////////////////////////////////

  // BEZIER VERTICES


  @Override
  public void bezierVertex(float x1, float y1,
                           float x2, float y2,
                           float x3, float y3) {
    bezierVertexCheck();
    gpath.curveTo(x1, y1, x2, y2, x3, y3);
  }


  @Override
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    showDepthWarningXYZ("bezierVertex");
  }



  //////////////////////////////////////////////////////////////

  // QUADRATIC BEZIER VERTICES


  @Override
  public void quadraticVertex(float ctrlX, float ctrlY,
                         float endX, float endY) {
    bezierVertexCheck();
    Point2D cur = gpath.getCurrentPoint();

    float x1 = (float) cur.getX();
    float y1 = (float) cur.getY();

    bezierVertex(x1 + ((ctrlX-x1)*2/3.0f), y1 + ((ctrlY-y1)*2/3.0f),
                 endX + ((ctrlX-endX)*2/3.0f), endY + ((ctrlY-endY)*2/3.0f),
                 endX, endY);
  }


  @Override
  public void quadraticVertex(float x2, float y2, float z2,
                         float x4, float y4, float z4) {
    showDepthWarningXYZ("quadVertex");
  }



  //////////////////////////////////////////////////////////////

  // CURVE VERTICES


  @Override
  protected void curveVertexCheck() {
    super.curveVertexCheck();

    if (curveCoordX == null) {
      curveCoordX = new float[4];
      curveCoordY = new float[4];
      curveDrawX = new float[4];
      curveDrawY = new float[4];
    }
  }


  @Override
  protected void curveVertexSegment(float x1, float y1,
                                    float x2, float y2,
                                    float x3, float y3,
                                    float x4, float y4) {
    curveCoordX[0] = x1;
    curveCoordY[0] = y1;

    curveCoordX[1] = x2;
    curveCoordY[1] = y2;

    curveCoordX[2] = x3;
    curveCoordY[2] = y3;

    curveCoordX[3] = x4;
    curveCoordY[3] = y4;

    curveToBezierMatrix.mult(curveCoordX, curveDrawX);
    curveToBezierMatrix.mult(curveCoordY, curveDrawY);

    // since the paths are continuous,
    // only the first point needs the actual moveto
    if (gpath == null) {
      gpath = new GeneralPath();
      gpath.moveTo(curveDrawX[0], curveDrawY[0]);
    }

    gpath.curveTo(curveDrawX[1], curveDrawY[1],
                  curveDrawX[2], curveDrawY[2],
                  curveDrawX[3], curveDrawY[3]);
  }


  @Override
  public void curveVertex(float x, float y, float z) {
    showDepthWarningXYZ("curveVertex");
  }



  //////////////////////////////////////////////////////////////

  // RENDERER


  //public void flush()



  //////////////////////////////////////////////////////////////

  // POINT, LINE, TRIANGLE, QUAD


  @Override
  public void point(float x, float y) {
    if (stroke) {
//      if (strokeWeight > 1) {
      line(x, y, x + EPSILON, y + EPSILON);
//      } else {
//        set((int) screenX(x, y), (int) screenY(x, y), strokeColor);
//      }
    }
  }


  @Override
  public void line(float x1, float y1, float x2, float y2) {
    line.setLine(x1, y1, x2, y2);
    strokeShape(line);
  }


  @Override
  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    gpath = new GeneralPath();
    gpath.moveTo(x1, y1);
    gpath.lineTo(x2, y2);
    gpath.lineTo(x3, y3);
    gpath.closePath();
    drawShape(gpath);
  }


  @Override
  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    GeneralPath gp = new GeneralPath();
    gp.moveTo(x1, y1);
    gp.lineTo(x2, y2);
    gp.lineTo(x3, y3);
    gp.lineTo(x4, y4);
    gp.closePath();
    drawShape(gp);
  }



  //////////////////////////////////////////////////////////////

  // RECT


  //public void rectMode(int mode)


  //public void rect(float a, float b, float c, float d)


  @Override
  protected void rectImpl(float x1, float y1, float x2, float y2) {
    rect.setFrame(x1, y1, x2-x1, y2-y1);
    drawShape(rect);
  }



  //////////////////////////////////////////////////////////////

  // ELLIPSE


  //public void ellipseMode(int mode)


  //public void ellipse(float a, float b, float c, float d)


  @Override
  protected void ellipseImpl(float x, float y, float w, float h) {
    ellipse.setFrame(x, y, w, h);
    drawShape(ellipse);
  }



  //////////////////////////////////////////////////////////////

  // ARC


  //public void arc(float a, float b, float c, float d,
  //                float start, float stop)


  @Override
  protected void arcImpl(float x, float y, float w, float h,
                         float start, float stop, int mode) {
    // 0 to 90 in java would be 0 to -90 for p5 renderer
    // but that won't work, so -90 to 0?

    start = -start * RAD_TO_DEG;
    stop = -stop * RAD_TO_DEG;

    // ok to do this because already checked for NaN
//    while (start < 0) {
//      start += 360;
//      stop += 360;
//    }
//    if (start > stop) {
//      float temp = start;
//      start = stop;
//      stop = temp;
//    }
    float sweep = stop - start;

    // The defaults, before 2.0b7, were to stroke as Arc2D.OPEN, and then fill
    // using Arc2D.PIE. That's a little wonky, but it's here for compatability.
    int fillMode = Arc2D.PIE;
    int strokeMode = Arc2D.OPEN;

    if (mode == OPEN) {
      fillMode = Arc2D.OPEN;
      //strokeMode = Arc2D.OPEN;

    } else if (mode == PIE) {
      //fillMode = Arc2D.PIE;
      strokeMode = Arc2D.PIE;

    } else if (mode == CHORD) {
      fillMode = Arc2D.CHORD;
      strokeMode = Arc2D.CHORD;
    }

    if (fill) {
      //System.out.println("filla");
      arc.setArc(x, y, w, h, start, sweep, fillMode);
      fillShape(arc);
    }
    if (stroke) {
      //System.out.println("strokey");
      arc.setArc(x, y, w, h, start, sweep, strokeMode);
      strokeShape(arc);
    }
  }



  //////////////////////////////////////////////////////////////

  // JAVA2D SHAPE/PATH HANDLING


  protected void fillShape(Shape s) {
    if (fillGradient) {
      g2.setPaint(fillGradientObject);
      g2.fill(s);
    } else if (fill) {
      g2.setColor(fillColorObject);
      g2.fill(s);
    }
  }


  protected void strokeShape(Shape s) {
    if (strokeGradient) {
      g2.setPaint(strokeGradientObject);
      g2.draw(s);
    } else if (stroke) {
      g2.setColor(strokeColorObject);
      g2.draw(s);
    }
  }


  protected void drawShape(Shape s) {
    if (fillGradient) {
      g2.setPaint(fillGradientObject);
      g2.fill(s);
    } else if (fill) {
      g2.setColor(fillColorObject);
      g2.fill(s);
    }
    if (strokeGradient) {
      g2.setPaint(strokeGradientObject);
      g2.draw(s);
    } else if (stroke) {
      g2.setColor(strokeColorObject);
      g2.draw(s);
    }
  }



  //////////////////////////////////////////////////////////////

  // BOX


  //public void box(float size)


  @Override
  public void box(float w, float h, float d) {
    showMethodWarning("box");
  }



  //////////////////////////////////////////////////////////////

  // SPHERE


  //public void sphereDetail(int res)


  //public void sphereDetail(int ures, int vres)


  @Override
  public void sphere(float r) {
    showMethodWarning("sphere");
  }



  //////////////////////////////////////////////////////////////

  // BEZIER


  //public float bezierPoint(float a, float b, float c, float d, float t)


  //public float bezierTangent(float a, float b, float c, float d, float t)


  //protected void bezierInitCheck()


  //protected void bezierInit()


  /** Ignored (not needed) in Java 2D. */
  @Override
  public void bezierDetail(int detail) {
  }


  //public void bezier(float x1, float y1,
  //                   float x2, float y2,
  //                   float x3, float y3,
  //                   float x4, float y4)


  //public void bezier(float x1, float y1, float z1,
  //                   float x2, float y2, float z2,
  //                   float x3, float y3, float z3,
  //                   float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // CURVE


  //public float curvePoint(float a, float b, float c, float d, float t)


  //public float curveTangent(float a, float b, float c, float d, float t)


  /** Ignored (not needed) in Java 2D. */
  @Override
  public void curveDetail(int detail) {
  }

  //public void curveTightness(float tightness)


  //protected void curveInitCheck()


  //protected void curveInit()


  //public void curve(float x1, float y1,
  //                  float x2, float y2,
  //                  float x3, float y3,
  //                  float x4, float y4)


  //public void curve(float x1, float y1, float z1,
  //                  float x2, float y2, float z2,
  //                  float x3, float y3, float z3,
  //                  float x4, float y4, float z4)



//  //////////////////////////////////////////////////////////////
//
//  // SMOOTH
//
//
//  @Override
//  public void smooth() {
//    smooth = true;
//
//    if (quality == 0) {
//      quality = 4;  // change back to bicubic
//    }
//
//    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                        RenderingHints.VALUE_ANTIALIAS_ON);
//
//    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                        quality == 4 ?
//                        RenderingHints.VALUE_INTERPOLATION_BICUBIC :
//                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//
//    // http://docs.oracle.com/javase/tutorial/2d/text/renderinghints.html
//    // Oracle Java text anti-aliasing on OS X looks like s*t compared to the
//    // text rendering with Apple's old Java 6. Below, several attempts to fix:
//    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
//                         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//    // Turns out this is the one that actually makes things work.
//    // Kerning is still screwed up, however.
//    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
//                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
////    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
////                        RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
////    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
////                         RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
//
////    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
////                        RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
//
//  }
//
//
//  @Override
//  public void smooth(int quality) {
//    this.quality = quality;
//    if (quality == 0) {
//      noSmooth();
//    } else {
//      smooth();
//    }
//  }
//
//
//  @Override
//  public void noSmooth() {
//    smooth = false;
//    quality = 0;  // https://github.com/processing/processing/issues/3113
//    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                        RenderingHints.VALUE_ANTIALIAS_OFF);
//    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
//    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
//                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
//  }



  //////////////////////////////////////////////////////////////

  // IMAGE


  //public void imageMode(int mode)


  //public void image(PImage image, float x, float y)


  //public void image(PImage image, float x, float y, float c, float d)


  //public void image(PImage image,
  //                  float a, float b, float c, float d,
  //                  int u1, int v1, int u2, int v2)


  /**
   * Handle renderer-specific image drawing.
   */
  @Override
  protected void imageImpl(PImage who,
                           float x1, float y1, float x2, float y2,
                           int u1, int v1, int u2, int v2) {
    // Image not ready yet, or an error
    if (who.width <= 0 || who.height <= 0) return;

    ImageCache cash = (ImageCache) getCache(who);

    // Nuke the cache if the image was resized
    if (cash != null) {
      if (who.width != cash.image.getWidth() ||
          who.height != cash.image.getHeight()) {
        cash = null;
      }
    }

    if (cash == null) {
      //System.out.println("making new image cache");
      cash = new ImageCache(); //who);
      setCache(who, cash);
      who.updatePixels();  // mark the whole thing for update
      who.setModified();
    }

    // If image previously was tinted, or the color changed
    // or the image was tinted, and tint is now disabled
    if ((tint && !cash.tinted) ||
        (tint && (cash.tintedColor != tintColor)) ||
        (!tint && cash.tinted)) {
      // For tint change, mark all pixels as needing update.
      who.updatePixels();
    }

    if (who.isModified()) {
      if (who.pixels == null) {
        // This might be a PGraphics that hasn't been drawn to yet.
        // Can't just bail because the cache has been created above.
        // https://github.com/processing/processing/issues/2208
        who.pixels = new int[who.width * who.height];
      }
      cash.update(who, tint, tintColor);
      who.setModified(false);
    }

    g2.drawImage(((ImageCache) getCache(who)).image,
                 (int) x1, (int) y1, (int) x2, (int) y2,
                 u1, v1, u2, v2, null);

    // Every few years I think "nah, Java2D couldn't possibly be that f*king
    // slow, why are we doing this by hand?" then comes the affirmation:
//    Composite oldComp = null;
//    if (false && tint) {
//      oldComp = g2.getComposite();
//      int alpha = (tintColor >> 24) & 0xff;
//      System.out.println("using alpha composite");
//      Composite alphaComp =
//        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f);
//      g2.setComposite(alphaComp);
//    }
//
//    long t = System.currentTimeMillis();
//    g2.drawImage(who.getImage(),
//                 (int) x1, (int) y1, (int) x2, (int) y2,
//                 u1, v1, u2, v2, null);
//    System.out.println(System.currentTimeMillis() - t);
//
//    if (oldComp != null) {
//      g2.setComposite(oldComp);
//    }
  }


  static class ImageCache {
    boolean tinted;
    int tintedColor;
    int[] tintedTemp;  // one row of tinted pixels
    BufferedImage image;
//    BufferedImage compat;

//    public ImageCache(PImage source) {
////      this.source = source;
//      // even if RGB, set the image type to ARGB, because the
//      // image may have an alpha value for its tint().
////      int type = BufferedImage.TYPE_INT_ARGB;
//      //System.out.println("making new buffered image");
////      image = new BufferedImage(source.width, source.height, type);
//    }

    /**
     * Update the pixels of the cache image. Already determined that the tint
     * has changed, or the pixels have changed, so should just go through
     * with the update without further checks.
     */
    public void update(PImage source, boolean tint, int tintColor) {
      //int bufferType = BufferedImage.TYPE_INT_ARGB;
      int targetType = ARGB;
      boolean opaque = (tintColor & 0xFF000000) == 0xFF000000;
      if (source.format == RGB) {
        if (!tint || (tint && opaque)) {
          //bufferType = BufferedImage.TYPE_INT_RGB;
          targetType = RGB;
        }
      }
//      boolean wrongType = (image != null) && (image.getType() != bufferType);
//      if ((image == null) || wrongType) {
//        image = new BufferedImage(source.width, source.height, bufferType);
//      }
      // Must always use an ARGB image, otherwise will write zeros
      // in the alpha channel when drawn to the screen.
      // https://github.com/processing/processing/issues/2030
      if (image == null) {
        image = new BufferedImage(source.width, source.height,
                                  BufferedImage.TYPE_INT_ARGB);
      }

      WritableRaster wr = image.getRaster();
      if (tint) {
        if (tintedTemp == null || tintedTemp.length != source.width) {
          tintedTemp = new int[source.width];
        }
        int a2 = (tintColor >> 24) & 0xff;
//        System.out.println("tint color is " + a2);
//        System.out.println("source.pixels[0] alpha is " + (source.pixels[0] >>> 24));
        int r2 = (tintColor >> 16) & 0xff;
        int g2 = (tintColor >> 8) & 0xff;
        int b2 = (tintColor) & 0xff;

        //if (bufferType == BufferedImage.TYPE_INT_RGB) {
        if (targetType == RGB) {
          // The target image is opaque, meaning that the source image has no
          // alpha (is not ARGB), and the tint has no alpha.
          int index = 0;
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              int argb1 = source.pixels[index++];
              int r1 = (argb1 >> 16) & 0xff;
              int g1 = (argb1 >> 8) & 0xff;
              int b1 = (argb1) & 0xff;

              // Prior to 2.1, the alpha channel was commented out here,
              // but can't remember why (just thought unnecessary b/c of RGB?)
              // https://github.com/processing/processing/issues/2030
              tintedTemp[x] = 0xFF000000 |
                  (((r2 * r1) & 0xff00) << 8) |
                  ((g2 * g1) & 0xff00) |
                  (((b2 * b1) & 0xff00) >> 8);
            }
            wr.setDataElements(0, y, source.width, 1, tintedTemp);
          }
          // could this be any slower?
//          float[] scales = { tintR, tintG, tintB };
//          float[] offsets = new float[3];
//          RescaleOp op = new RescaleOp(scales, offsets, null);
//          op.filter(image, image);

        //} else if (bufferType == BufferedImage.TYPE_INT_ARGB) {
        } else if (targetType == ARGB) {
          if (source.format == RGB &&
              (tintColor & 0xffffff) == 0xffffff) {
            int hi = tintColor & 0xff000000;
            int index = 0;
            for (int y = 0; y < source.height; y++) {
              for (int x = 0; x < source.width; x++) {
                tintedTemp[x] = hi | (source.pixels[index++] & 0xFFFFFF);
              }
              wr.setDataElements(0, y, source.width, 1, tintedTemp);
            }
          } else {
            int index = 0;
            for (int y = 0; y < source.height; y++) {
              if (source.format == RGB) {
                int alpha = tintColor & 0xFF000000;
                for (int x = 0; x < source.width; x++) {
                  int argb1 = source.pixels[index++];
                  int r1 = (argb1 >> 16) & 0xff;
                  int g1 = (argb1 >> 8) & 0xff;
                  int b1 = (argb1) & 0xff;
                  tintedTemp[x] = alpha |
                      (((r2 * r1) & 0xff00) << 8) |
                      ((g2 * g1) & 0xff00) |
                      (((b2 * b1) & 0xff00) >> 8);
                }
              } else if (source.format == ARGB) {
                for (int x = 0; x < source.width; x++) {
                  int argb1 = source.pixels[index++];
                  int a1 = (argb1 >> 24) & 0xff;
                  int r1 = (argb1 >> 16) & 0xff;
                  int g1 = (argb1 >> 8) & 0xff;
                  int b1 = (argb1) & 0xff;
                  tintedTemp[x] =
                      (((a2 * a1) & 0xff00) << 16) |
                      (((r2 * r1) & 0xff00) << 8) |
                      ((g2 * g1) & 0xff00) |
                      (((b2 * b1) & 0xff00) >> 8);
                }
              } else if (source.format == ALPHA) {
                int lower = tintColor & 0xFFFFFF;
                for (int x = 0; x < source.width; x++) {
                  int a1 = source.pixels[index++];
                  tintedTemp[x] =
                      (((a2 * a1) & 0xff00) << 16) | lower;
                }
              }
              wr.setDataElements(0, y, source.width, 1, tintedTemp);
            }
          }
          // Not sure why ARGB images take the scales in this order...
//          float[] scales = { tintR, tintG, tintB, tintA };
//          float[] offsets = new float[4];
//          RescaleOp op = new RescaleOp(scales, offsets, null);
//          op.filter(image, image);
        }
      } else {  // !tint
        if (targetType == RGB && (source.pixels[0] >> 24 == 0)) {
          // If it's an RGB image and the high bits aren't set, need to set
          // the high bits to opaque because we're drawing ARGB images.
          source.filter(OPAQUE);
          // Opting to just manipulate the image here, since it shouldn't
          // affect anything else (and alpha(get(x, y)) should return 0xff).
          // Wel also make no guarantees about the values of the pixels array
          // in a PImage and how the high bits will be set.
        }
        // If no tint, just shove the pixels on in there verbatim
        wr.setDataElements(0, 0, source.width, source.height, source.pixels);
      }
      this.tinted = tint;
      this.tintedColor = tintColor;

//      GraphicsConfiguration gc = parent.getGraphicsConfiguration();
//      compat = gc.createCompatibleImage(image.getWidth(),
//                                        image.getHeight(),
//                                        Transparency.TRANSLUCENT);
//
//      Graphics2D g = compat.createGraphics();
//      g.drawImage(image, 0, 0, null);
//      g.dispose();
    }
  }



  //////////////////////////////////////////////////////////////

  // SHAPE


  //public void shapeMode(int mode)


  //public void shape(PShape shape)


  //public void shape(PShape shape, float x, float y)


  //public void shape(PShape shape, float x, float y, float c, float d)


  //////////////////////////////////////////////////////////////

  // SHAPE I/O


  //public PShape loadShape(String filename)


  @Override
  public PShape loadShape(String filename, String options) {
    String extension = PApplet.getExtension(filename);
    if (extension.equals("svg") || extension.equals("svgz")) {
      return new PShapeJava2D(parent.loadXML(filename));
    }
    PGraphics.showWarning("Unsupported format: " + filename);
    return null;
  }



  //////////////////////////////////////////////////////////////

  // TEXT ATTRIBTUES


  //public void textAlign(int align)


  //public void textAlign(int alignX, int alignY)


  @Override
  public float textAscent() {
    if (textFont == null) {
      defaultFontOrDeath("textAscent");
    }

    Font font = (Font) textFont.getNative();
    if (font != null) {
      //return getFontMetrics(font).getAscent();
      return g2.getFontMetrics(font).getAscent();
    }
    return super.textAscent();
  }


  @Override
  public float textDescent() {
    if (textFont == null) {
      defaultFontOrDeath("textDescent");
    }
    Font font = (Font) textFont.getNative();
    if (font != null) {
      //return getFontMetrics(font).getDescent();
      return g2.getFontMetrics(font).getDescent();
    }
    return super.textDescent();
  }


  //public void textFont(PFont which)


  //public void textFont(PFont which, float size)


  //public void textLeading(float leading)


  //public void textMode(int mode)


  @Override
  protected boolean textModeCheck(int mode) {
    return mode == MODEL;
  }


  /**
   * Same as parent, but override for native version of the font.
   * <p/>
   * Called from textFontImpl and textSizeImpl, so the metrics
   * will get recorded properly.
   */
  @Override
  protected void setTextSize(float size) {
    // if a native version available, derive this font
//    if (textFontNative != null) {
//      textFontNative = textFontNative.deriveFont(size);
//      g2.setFont(textFontNative);
//      textFontNativeMetrics = g2.getFontMetrics(textFontNative);
//    }
    Font font = (Font) textFont.getNative();
    //if (font != null && (textFont.isStream() || hints[ENABLE_NATIVE_FONTS])) {
    if (font != null) {
      Map<TextAttribute, Object> map =
        new HashMap<TextAttribute, Object>();
      map.put(TextAttribute.SIZE, size);
      map.put(TextAttribute.KERNING,
              TextAttribute.KERNING_ON);
//      map.put(TextAttribute.TRACKING,
//              TextAttribute.TRACKING_TIGHT);
      font = font.deriveFont(map);
      g2.setFont(font);
      textFont.setNative(font);
      fontObject = font;

//      Font dfont = font.deriveFont(size);
////      Map<TextAttribute, ?> attrs = dfont.getAttributes();
////      for (TextAttribute ta : attrs.keySet()) {
////        System.out.println(ta + " -> " + attrs.get(ta));
////      }
//      g2.setFont(dfont);
//      textFont.setNative(dfont);
    }

    // take care of setting the textSize and textLeading vars
    // this has to happen second, because it calls textAscent()
    // (which requires the native font metrics to be set)
    super.setTextSize(size);
  }


  //public float textWidth(char c)


  //public float textWidth(String str)


  @Override
  protected float textWidthImpl(char buffer[], int start, int stop) {
    if (textFont == null) {
      defaultFontOrDeath("textWidth");
    }

    Font font = (Font) textFont.getNative();
//    System.out.println(font);
    //if (font != null && (textFont.isStream() || hints[ENABLE_NATIVE_FONTS])) {
    if (font != null) {
//      System.out.println("using charswidth for " + new String(buffer, start, stop-start));
      // maybe should use one of the newer/fancier functions for this?
//      int length = stop - start;
//      FontMetrics metrics = getFontMetrics(font);
      FontMetrics metrics = g2.getFontMetrics(font);
      // Using fractional metrics makes the measurement worse, not better,
      // at least on OS X 10.6 (November, 2010).
      // TextLayout returns the same value as charsWidth().
//      System.err.println("using native");
//      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
//                          RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//      float m1 = metrics.charsWidth(buffer, start, length);
//      float m2 = (float) metrics.getStringBounds(buffer, start, stop, g2).getWidth();
//      TextLayout tl = new TextLayout(new String(buffer, start, length), font, g2.getFontRenderContext());
//      float m3 = (float) tl.getBounds().getWidth();
//      System.err.println(m1 + " " + m2 + " " + m3);
//////      return m1;
////      return m2;
////      return metrics.charsWidth(buffer, start, length);
//      return m2;
      return (float)
        metrics.getStringBounds(buffer, start, stop, g2).getWidth();
    }
//    System.err.println("not native");
    return super.textWidthImpl(buffer, start, stop);
  }


//  protected void beginTextScreenMode() {
//    loadPixels();
//  }


//  protected void endTextScreenMode() {
//    updatePixels();
//  }


  //////////////////////////////////////////////////////////////

  // TEXT

  // None of the variations of text() are overridden from PGraphics.



  //////////////////////////////////////////////////////////////

  // TEXT IMPL


  //protected void textLineAlignImpl(char buffer[], int start, int stop,
  //                                 float x, float y)


  @Override
  protected void textLineImpl(char buffer[], int start, int stop,
                              float x, float y) {
    Font font = (Font) textFont.getNative();
//    if (font != null && (textFont.isStream() || hints[ENABLE_NATIVE_FONTS])) {
    if (font != null) {
      /*
      // save the current setting for text smoothing. note that this is
      // different from the smooth() function, because the font smoothing
      // is controlled when the font is created, not now as it's drawn.
      // fixed a bug in 0116 that handled this incorrectly.
      Object textAntialias =
        g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);

      // override the current text smoothing setting based on the font
      // (don't change the global smoothing settings)
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          textFont.smooth ?
                          RenderingHints.VALUE_ANTIALIAS_ON :
                          RenderingHints.VALUE_ANTIALIAS_OFF);
      */
      Object antialias =
        g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
      if (antialias == null) {
        // if smooth() and noSmooth() not called, this will be null (0120)
        antialias = RenderingHints.VALUE_ANTIALIAS_DEFAULT;
      }

      // override the current smoothing setting based on the font
      // also changes global setting for antialiasing, but this is because it's
      // not possible to enable/disable them independently in some situations.
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          textFont.isSmooth() ?
                          RenderingHints.VALUE_ANTIALIAS_ON :
                          RenderingHints.VALUE_ANTIALIAS_OFF);

      g2.setColor(fillColorObject);

      int length = stop - start;
      if (length != 0) {
      g2.drawChars(buffer, start, length, (int) (x + 0.5f), (int) (y + 0.5f));
      // better to use round here? also, drawChars now just calls drawString
//      g2.drawString(new String(buffer, start, stop - start), Math.round(x), Math.round(y));

      // better to use drawString() with floats? (nope, draws the same)
      //g2.drawString(new String(buffer, start, length), x, y);

      // this didn't seem to help the scaling issue, and creates garbage
      // because of a fairly heavyweight new temporary object
//      java.awt.font.GlyphVector gv =
//        font.createGlyphVector(g2.getFontRenderContext(), new String(buffer, start, stop - start));
//      g2.drawGlyphVector(gv, x, y);
      }

      // return to previous smoothing state if it was changed
      //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntialias);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);

    } else {  // otherwise just do the default
      super.textLineImpl(buffer, start, stop, x, y);
    }
  }


//  /**
//   * Convenience method to get a legit FontMetrics object. Where possible,
//   * override this any renderer subclass so that you're not using what's
//   * returned by getDefaultToolkit() to get your metrics.
//   */
//  @SuppressWarnings("deprecation")
//  public FontMetrics getFontMetrics(Font font) {  // ignore
//    Frame frame = parent.frame;
//    if (frame != null) {
//      return frame.getToolkit().getFontMetrics(font);
//    }
//    return Toolkit.getDefaultToolkit().getFontMetrics(font);
//  }
//
//
//  /**
//   * Convenience method to jump through some Java2D hoops and get an FRC.
//   */
//  public FontRenderContext getFontRenderContext(Font font) {  // ignore
//    return getFontMetrics(font).getFontRenderContext();
//  }

  /*
  Toolkit toolkit;

  @SuppressWarnings("deprecation")
  protected FontMetrics getFontMetrics(Font font) {
    if (toolkit == null) {
      try {
        Canvas canvas = (Canvas) surface.getNative();
        toolkit = canvas.getToolkit();
      } catch (Exception e) {
        // May error out if it's a PSurfaceNone or similar
        toolkit = Toolkit.getDefaultToolkit();
      }
    }
    return toolkit.getFontMetrics(font);
    //return (g2 != null) ? g2.getFontMetrics(font) : super.getFontMetrics(font);
  }
  */


  //////////////////////////////////////////////////////////////

  // MATRIX STACK


  @Override
  public void pushMatrix() {
    if (transformCount == transformStack.length) {
      throw new RuntimeException("pushMatrix() cannot use push more than " +
                                 transformStack.length + " times");
    }
    transformStack[transformCount] = g2.getTransform();
    transformCount++;
  }


  @Override
  public void popMatrix() {
    if (transformCount == 0) {
      throw new RuntimeException("missing a pushMatrix() " +
                                 "to go with that popMatrix()");
    }
    transformCount--;
    g2.setTransform(transformStack[transformCount]);
  }



  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMS


  @Override
  public void translate(float tx, float ty) {
    g2.translate(tx, ty);
  }


  //public void translate(float tx, float ty, float tz)


  @Override
  public void rotate(float angle) {
    g2.rotate(angle);
  }


  @Override
  public void rotateX(float angle) {
    showDepthWarning("rotateX");
  }


  @Override
  public void rotateY(float angle) {
    showDepthWarning("rotateY");
  }


  @Override
  public void rotateZ(float angle) {
    showDepthWarning("rotateZ");
  }


  @Override
  public void rotate(float angle, float vx, float vy, float vz) {
    showVariationWarning("rotate");
  }


  @Override
  public void scale(float s) {
    g2.scale(s, s);
  }


  @Override
  public void scale(float sx, float sy) {
    g2.scale(sx, sy);
  }


  @Override
  public void scale(float sx, float sy, float sz) {
    showDepthWarningXYZ("scale");
  }


  @Override
  public void shearX(float angle) {
    g2.shear(Math.tan(angle), 0);
  }


  @Override
  public void shearY(float angle) {
    g2.shear(0, Math.tan(angle));
  }



  //////////////////////////////////////////////////////////////

  // MATRIX MORE


  @Override
  public void resetMatrix() {
    g2.setTransform(new AffineTransform());
  }


  //public void applyMatrix(PMatrix2D source)


  @Override
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    //System.out.println("PGraphicsJava2D.applyMatrix()");
    //System.out.println(new AffineTransform(n00, n10, n01, n11, n02, n12));
    g2.transform(new AffineTransform(n00, n10, n01, n11, n02, n12));
    //g2.transform(new AffineTransform(n00, n01, n02, n10, n11, n12));
  }


  //public void applyMatrix(PMatrix3D source)


  @Override
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    showVariationWarning("applyMatrix");
  }



  //////////////////////////////////////////////////////////////

  // MATRIX GET/SET


  @Override
  public PMatrix getMatrix() {
    return getMatrix((PMatrix2D) null);
  }


  @Override
  public PMatrix2D getMatrix(PMatrix2D target) {
    if (target == null) {
      target = new PMatrix2D();
    }
    g2.getTransform().getMatrix(transform);
    target.set((float) transform[0], (float) transform[2], (float) transform[4],
               (float) transform[1], (float) transform[3], (float) transform[5]);
    return target;
  }


  @Override
  public PMatrix3D getMatrix(PMatrix3D target) {
    showVariationWarning("getMatrix");
    return target;
  }


  //public void setMatrix(PMatrix source)


  @Override
  public void setMatrix(PMatrix2D source) {
    g2.setTransform(new AffineTransform(source.m00, source.m10,
                                        source.m01, source.m11,
                                        source.m02, source.m12));
  }


  @Override
  public void setMatrix(PMatrix3D source) {
    showVariationWarning("setMatrix");
  }


  @Override
  public void printMatrix() {
    getMatrix((PMatrix2D) null).print();
  }



  //////////////////////////////////////////////////////////////

  // CAMERA and PROJECTION

  // Inherit the plaintive warnings from PGraphics


  //public void beginCamera()
  //public void endCamera()
  //public void camera()
  //public void camera(float eyeX, float eyeY, float eyeZ,
  //                   float centerX, float centerY, float centerZ,
  //                   float upX, float upY, float upZ)
  //public void printCamera()

  //public void ortho()
  //public void ortho(float left, float right,
  //                  float bottom, float top,
  //                  float near, float far)
  //public void perspective()
  //public void perspective(float fov, float aspect, float near, float far)
  //public void frustum(float left, float right,
  //                    float bottom, float top,
  //                    float near, float far)
  //public void printProjection()



  //////////////////////////////////////////////////////////////

  // SCREEN and MODEL transforms


  @Override
  public float screenX(float x, float y) {
    g2.getTransform().getMatrix(transform);
    return (float)transform[0]*x + (float)transform[2]*y + (float)transform[4];
  }


  @Override
  public float screenY(float x, float y) {
    g2.getTransform().getMatrix(transform);
    return (float)transform[1]*x + (float)transform[3]*y + (float)transform[5];
  }


  @Override
  public float screenX(float x, float y, float z) {
    showDepthWarningXYZ("screenX");
    return 0;
  }


  @Override
  public float screenY(float x, float y, float z) {
    showDepthWarningXYZ("screenY");
    return 0;
  }


  @Override
  public float screenZ(float x, float y, float z) {
    showDepthWarningXYZ("screenZ");
    return 0;
  }


  //public float modelX(float x, float y, float z)


  //public float modelY(float x, float y, float z)


  //public float modelZ(float x, float y, float z)



  //////////////////////////////////////////////////////////////

  // STYLE

  // pushStyle(), popStyle(), style() and getStyle() inherited.



  //////////////////////////////////////////////////////////////

  // STROKE CAP/JOIN/WEIGHT


  @Override
  public void strokeCap(int cap) {
    super.strokeCap(cap);
    strokeImpl();
  }


  @Override
  public void strokeJoin(int join) {
    super.strokeJoin(join);
    strokeImpl();
  }


  @Override
  public void strokeWeight(float weight) {
    super.strokeWeight(weight);
    strokeImpl();
  }


  protected void strokeImpl() {
    int cap = BasicStroke.CAP_BUTT;
    if (strokeCap == ROUND) {
      cap = BasicStroke.CAP_ROUND;
    } else if (strokeCap == PROJECT) {
      cap = BasicStroke.CAP_SQUARE;
    }

    int join = BasicStroke.JOIN_BEVEL;
    if (strokeJoin == MITER) {
      join = BasicStroke.JOIN_MITER;
    } else if (strokeJoin == ROUND) {
      join = BasicStroke.JOIN_ROUND;
    }

    strokeObject = new BasicStroke(strokeWeight, cap, join);
    g2.setStroke(strokeObject);
  }



  //////////////////////////////////////////////////////////////

  // STROKE

  // noStroke() and stroke() inherited from PGraphics.


  @Override
  protected void strokeFromCalc() {
    super.strokeFromCalc();
    strokeColorObject = new Color(strokeColor, true);
    strokeGradient = false;
  }



  //////////////////////////////////////////////////////////////

  // TINT

  // noTint() and tint() inherited from PGraphics.


  @Override
  protected void tintFromCalc() {
    super.tintFromCalc();
    // TODO actually implement tinted images
    tintColorObject = new Color(tintColor, true);
  }



  //////////////////////////////////////////////////////////////

  // FILL

  // noFill() and fill() inherited from PGraphics.


  @Override
  protected void fillFromCalc() {
    super.fillFromCalc();
    fillColorObject = new Color(fillColor, true);
    fillGradient = false;
  }



  //////////////////////////////////////////////////////////////

  // MATERIAL PROPERTIES


  //public void ambient(int rgb)
  //public void ambient(float gray)
  //public void ambient(float x, float y, float z)
  //protected void ambientFromCalc()
  //public void specular(int rgb)
  //public void specular(float gray)
  //public void specular(float x, float y, float z)
  //protected void specularFromCalc()
  //public void shininess(float shine)
  //public void emissive(int rgb)
  //public void emissive(float gray)
  //public void emissive(float x, float y, float z )
  //protected void emissiveFromCalc()



  //////////////////////////////////////////////////////////////

  // LIGHTS


  //public void lights()
  //public void noLights()
  //public void ambientLight(float red, float green, float blue)
  //public void ambientLight(float red, float green, float blue,
  //                         float x, float y, float z)
  //public void directionalLight(float red, float green, float blue,
  //                             float nx, float ny, float nz)
  //public void pointLight(float red, float green, float blue,
  //                       float x, float y, float z)
  //public void spotLight(float red, float green, float blue,
  //                      float x, float y, float z,
  //                      float nx, float ny, float nz,
  //                      float angle, float concentration)
  //public void lightFalloff(float constant, float linear, float quadratic)
  //public void lightSpecular(float x, float y, float z)
  //protected void lightPosition(int num, float x, float y, float z)
  //protected void lightDirection(int num, float x, float y, float z)



  //////////////////////////////////////////////////////////////

  // BACKGROUND


  int[] clearPixels;

  protected void clearPixels(int color) {
    // On a hi-res display, image may be larger than width/height
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);

    // Create a small array that can be used to set the pixels several times.
    // Using a single-pixel line of length 'width' is a tradeoff between
    // speed (setting each pixel individually is too slow) and memory
    // (an array for width*height would waste lots of memory if it stayed
    // resident, and would terrify the gc if it were re-created on each trip
    // to background().
//    WritableRaster raster = ((BufferedImage) image).getRaster();
//    WritableRaster raster = image.getRaster();
    WritableRaster raster = getRaster();
    if ((clearPixels == null) || (clearPixels.length < imageWidth)) {
      clearPixels = new int[imageWidth];
    }
    Arrays.fill(clearPixels, 0, imageWidth, backgroundColor);
    for (int i = 0; i < imageHeight; i++) {
      raster.setDataElements(0, i, imageWidth, 1, clearPixels);
    }
  }

  // background() methods inherited from PGraphics, along with the
  // PImage version of backgroundImpl(), since it just calls set().


  //public void backgroundImpl(PImage image)


  @Override
  public void backgroundImpl() {
    if (backgroundAlpha) {
      clearPixels(backgroundColor);

    } else {
      Color bgColor = new Color(backgroundColor);
      // seems to fire an additional event that causes flickering,
      // like an extra background erase on OS X
//      if (canvas != null) {
//        canvas.setBackground(bgColor);
//      }
      //new Exception().printStackTrace(System.out);
      // in case people do transformations before background(),
      // need to handle this with a push/reset/pop
      Composite oldComposite = g2.getComposite();
      g2.setComposite(defaultComposite);

      pushMatrix();
      resetMatrix();
      g2.setColor(bgColor); //, backgroundAlpha));
//      g2.fillRect(0, 0, width, height);
      // On a hi-res display, image may be larger than width/height
      if (image != null) {
        // image will be null in subclasses (i.e. PDF)
        g2.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
      } else {
        // hope for the best if image is null
        g2.fillRect(0, 0, width, height);
      }
      popMatrix();

      g2.setComposite(oldComposite);
    }
  }



  //////////////////////////////////////////////////////////////

  // COLOR MODE

  // All colorMode() variations are inherited from PGraphics.



  //////////////////////////////////////////////////////////////

  // COLOR CALC

  // colorCalc() and colorCalcARGB() inherited from PGraphics.



  //////////////////////////////////////////////////////////////

  // COLOR DATATYPE STUFFING

  // final color() variations inherited.



  //////////////////////////////////////////////////////////////

  // COLOR DATATYPE EXTRACTION

  // final methods alpha, red, green, blue,
  // hue, saturation, and brightness all inherited.



  //////////////////////////////////////////////////////////////

  // COLOR DATATYPE INTERPOLATION

  // both lerpColor variants inherited.



  //////////////////////////////////////////////////////////////

  // BEGIN/END RAW


  @Override
  public void beginRaw(PGraphics recorderRaw) {
    showMethodWarning("beginRaw");
  }


  @Override
  public void endRaw() {
    showMethodWarning("endRaw");
  }



  //////////////////////////////////////////////////////////////

  // WARNINGS and EXCEPTIONS

  // showWarning and showException inherited.



  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES


  //public boolean displayable()  // true


  //public boolean is2D()  // true


  //public boolean is3D()  // false



  //////////////////////////////////////////////////////////////

  // PIMAGE METHODS


  // getImage, setCache, getCache, removeCache, isModified, setModified


  protected WritableRaster getRaster() {
    WritableRaster raster = null;
    if (primaryGraphics) {
      /*
      // 'offscreen' will probably be removed in the next release
      if (useOffscreen) {
        raster = offscreen.getRaster();
      } else*/ if (image instanceof VolatileImage) {
        // when possible, we'll try VolatileImage
        raster = ((VolatileImage) image).getSnapshot().getRaster();
      }
    }
    if (raster == null) {
      raster = ((BufferedImage) image).getRaster();
    }

    // On Raspberry Pi (and perhaps other platforms, the color buffer won't
    // necessarily be the int array that we'd like. Need to convert it here.
    // Not that this would probably mean getRaster() would need to work more
    // like loadRaster/updateRaster because the pixels will need to be
    // temporarily moved to (and later from) a buffer that's understood by
    // the rest of the Processing source.
    // https://github.com/processing/processing/issues/2010
    if (raster.getTransferType() != DataBuffer.TYPE_INT) {
      System.err.println("See https://github.com/processing/processing/issues/2010");
      throw new RuntimeException("Pixel operations are not supported on this device.");
    }
    return raster;
  }


  @Override
  public void loadPixels() {
    if (pixels == null || (pixels.length != pixelWidth*pixelHeight)) {
      pixels = new int[pixelWidth * pixelHeight];
    }

    WritableRaster raster = getRaster();
    raster.getDataElements(0, 0, pixelWidth, pixelHeight, pixels);
    if (raster.getNumBands() == 3) {
      // Java won't set the high bits when RGB, returns 0 for alpha
      // https://github.com/processing/processing/issues/2030
      for (int i = 0; i < pixels.length; i++) {
        pixels[i] = 0xff000000 | pixels[i];
      }
    }
      //((BufferedImage) image).getRGB(0, 0, width, height, pixels, 0, width);
//    WritableRaster raster = ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
//    WritableRaster raster = image.getRaster();
  }


//  /**
//   * Update the pixels[] buffer to the PGraphics image.
//   * <P>
//   * Unlike in PImage, where updatePixels() only requests that the
//   * update happens, in PGraphicsJava2D, this will happen immediately.
//   */
//  @Override
//  public void updatePixels() {
//    //updatePixels(0, 0, width, height);
////    WritableRaster raster = ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
////    WritableRaster raster = image.getRaster();
//    updatePixels(0, 0, width, height);
//  }


  /**
   * Update the pixels[] buffer to the PGraphics image.
   * <P>
   * Unlike in PImage, where updatePixels() only requests that the
   * update happens, in PGraphicsJava2D, this will happen immediately.
   */
  @Override
  public void updatePixels(int x, int y, int c, int d) {
    //if ((x == 0) && (y == 0) && (c == width) && (d == height)) {
//    System.err.format("%d %d %d %d .. w/h = %d %d .. pw/ph = %d %d %n", x, y, c, d, width, height, pixelWidth, pixelHeight);
    if ((x != 0) || (y != 0) || (c != pixelWidth) || (d != pixelHeight)) {
      // Show a warning message, but continue anyway.
      showVariationWarning("updatePixels(x, y, w, h)");
//      new Exception().printStackTrace(System.out);
    }
//    updatePixels();
    if (pixels != null) {
      getRaster().setDataElements(0, 0, pixelWidth, pixelHeight, pixels);
    }
    modified = true;
  }


//  @Override
//  protected void updatePixelsImpl(int x, int y, int w, int h) {
//    super.updatePixelsImpl(x, y, w, h);
//
//    if ((x != 0) || (y != 0) || (w != width) || (h != height)) {
//      // Show a warning message, but continue anyway.
//      showVariationWarning("updatePixels(x, y, w, h)");
//    }
//    getRaster().setDataElements(0, 0, width, height, pixels);
//  }



  //////////////////////////////////////////////////////////////

  // GET/SET


  static int getset[] = new int[1];


  @Override
  public int get(int x, int y) {
    if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) return 0;
    //return ((BufferedImage) image).getRGB(x, y);
//    WritableRaster raster = ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
    WritableRaster raster = getRaster();
    raster.getDataElements(x, y, getset);
    if (raster.getNumBands() == 3) {
      // https://github.com/processing/processing/issues/2030
      return getset[0] | 0xff000000;
    }
    return getset[0];
  }


  //public PImage get(int x, int y, int w, int h)


  @Override
  public PImage get() {
    return get(0, 0, width, height);
  }


  @Override
  protected void getImpl(int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         PImage target, int targetX, int targetY) {
    // last parameter to getRGB() is the scan size of the *target* buffer
    //((BufferedImage) image).getRGB(x, y, w, h, output.pixels, 0, w);
//    WritableRaster raster =
//      ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
    WritableRaster raster = getRaster();

    if (sourceWidth == target.width && sourceHeight == target.height) {
      raster.getDataElements(sourceX, sourceY, sourceWidth, sourceHeight, target.pixels);
      // https://github.com/processing/processing/issues/2030
      if (raster.getNumBands() == 3) {
        target.filter(OPAQUE);
      }

    } else {
      // TODO optimize, incredibly inefficient to reallocate this much memory
      int[] temp = new int[sourceWidth * sourceHeight];
      raster.getDataElements(sourceX, sourceY, sourceWidth, sourceHeight, temp);

      // Copy the temporary output pixels over to the outgoing image
      int sourceOffset = 0;
      int targetOffset = targetY*target.width + targetX;
      for (int y = 0; y < sourceHeight; y++) {
        if (raster.getNumBands() == 3) {
          for (int i = 0; i < sourceWidth; i++) {
            // Need to set the high bits for this feller
            // https://github.com/processing/processing/issues/2030
            target.pixels[targetOffset + i] = 0xFF000000 | temp[sourceOffset + i];
          }
        } else {
          System.arraycopy(temp, sourceOffset, target.pixels, targetOffset, sourceWidth);
        }
        sourceOffset += sourceWidth;
        targetOffset += target.width;
      }
    }
  }


  @Override
  public void set(int x, int y, int argb) {
    if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) return;
//    ((BufferedImage) image).setRGB(x, y, argb);
    getset[0] = argb;
//    WritableRaster raster = ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
//    WritableRaster raster = image.getRaster();
    getRaster().setDataElements(x, y, getset);
  }


  //public void set(int x, int y, PImage img)


  @Override
  protected void setImpl(PImage sourceImage,
                         int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         int targetX, int targetY) {
    WritableRaster raster = getRaster();
//      ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();

    if ((sourceX == 0) && (sourceY == 0) &&
        (sourceWidth == sourceImage.width) &&
        (sourceHeight == sourceImage.height)) {
//      System.out.format("%d %d  %dx%d  %d%n", targetX, targetY,
//                             sourceImage.width, sourceImage.height,
//                             sourceImage.pixels.length);
      raster.setDataElements(targetX, targetY,
                             sourceImage.width, sourceImage.height,
                             sourceImage.pixels);
    } else {
      // TODO optimize, incredibly inefficient to reallocate this much memory
      PImage temp = sourceImage.get(sourceX, sourceY, sourceWidth, sourceHeight);
      raster.setDataElements(targetX, targetY, temp.width, temp.height, temp.pixels);
    }
  }



  //////////////////////////////////////////////////////////////

  // MASK


  static final String MASK_WARNING =
    "mask() cannot be used on the main drawing surface";


  @Override
  @SuppressWarnings("deprecation")
  public void mask(int[] alpha) {
    if (primaryGraphics) {
      showWarning(MASK_WARNING);

    } else {
      super.mask(alpha);
    }
  }


  @Override
  public void mask(PImage alpha) {
    if (primaryGraphics) {
      showWarning(MASK_WARNING);

    } else {
      super.mask(alpha);
    }
  }



  //////////////////////////////////////////////////////////////

  // FILTER

  // Because the PImage versions call loadPixels() and
  // updatePixels(), no need to override anything here.


  //public void filter(int kind)


  //public void filter(int kind, float param)



  //////////////////////////////////////////////////////////////

  // COPY


  @Override
  public void copy(int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    if ((sw != dw) || (sh != dh)) {
      g2.drawImage(image, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

    } else {
      dx = dx - sx;  // java2d's "dx" is the delta, not dest
      dy = dy - sy;
      g2.copyArea(sx, sy, sw, sh, dx, dy);
    }
  }


  @Override
  public void copy(PImage src,
                   int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    g2.drawImage((Image) src.getNative(),
                 dx, dy, dx + dw, dy + dh,
                 sx, sy, sx + sw, sy + sh, null);
  }



  //////////////////////////////////////////////////////////////

  // BLEND


//  static public int blendColor(int c1, int c2, int mode)


//  public void blend(int sx, int sy, int sw, int sh,
//                    int dx, int dy, int dw, int dh, int mode)


//  public void blend(PImage src,
//                    int sx, int sy, int sw, int sh,
//                    int dx, int dy, int dw, int dh, int mode)



  //////////////////////////////////////////////////////////////

  // SAVE


//  public void save(String filename) {
//    loadPixels();
//    super.save(filename);
//  }
}
