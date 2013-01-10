package processing.core;

import java.awt.*;
import java.awt.image.*;


public class PGraphicsRetina2D extends PGraphicsJava2D {


  //////////////////////////////////////////////////////////////

  // INTERNAL


  public PGraphicsRetina2D() { }


  @Override
  protected void allocate() {
//    parent.setIgnoreRepaint(true);
//    g2 = (Graphics2D) parent.getGraphics();


    /*
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

        // Needs to be RGB otherwise there's a major performance hit [0204]
        // http://code.google.com/p/processing/issues/detail?id=729
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        GraphicsConfiguration gc = parent.getGraphicsConfiguration();
//        image = gc.createCompatibleImage(width, height);
        offscreen = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//        offscreen = gc.createCompatibleImage(width, height);
        g2 = (Graphics2D) offscreen.getGraphics();
      }
    } else {
      // Since this buffer's offscreen anyway, no need for the extra offscreen
      // buffer. However, unlike the primary surface, this feller needs to be
      // ARGB so that blending ("alpha" compositing) will work properly.
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      g2 = (Graphics2D) image.getGraphics();
    }
    */
  }


  //////////////////////////////////////////////////////////////

  // FRAME

  @Override
  public void beginDraw() {
//    g2 = (Graphics2D) parent.getGraphics();

    GraphicsConfiguration gc = parent.getGraphicsConfiguration();
    if (false) {
      if (image == null || ((VolatileImage) image).validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
        image = gc.createCompatibleVolatileImage(width*2, height*2);
      }
    } else {
      if (image == null) {
        image = gc.createCompatibleImage(width*2, height*2);
//        System.out.println("image type is " + image);
      }
    }
    g2 = (Graphics2D) image.getGraphics();
//    g2.scale(2, 2);

//  if (bimage == null ||
//      bimage.getWidth() != width ||
//      bimage.getHeight() != height) {
//    PApplet.debug("PGraphicsJava2D creating new image");
//    bimage = gc.createCompatibleImage(width, height);

    checkSettings();
    resetMatrix(); // reset model matrix

    // inserted here for retina
    g2.scale(2, 2);

    vertexCount = 0;
  }


  @Override
  public void endDraw() {
    g2.dispose();
    /*
    // hm, mark pixels as changed, because this will instantly do a full
    // copy of all the pixels to the surface.. so that's kind of a mess.
    //updatePixels();

    if (primarySurface) {
      //if (canvas != null) {
      if (useCanvas) {
        //System.out.println(canvas);

        // alternate version
        //canvas.repaint();  // ?? what to do for swapping buffers

        redraw();

      } else {
        // don't copy the pixels/data elements of the buffered image directly,
        // since it'll disable the nice speedy pipeline stuff, sending all drawing
        // into a world of suck that's rough 6 trillion times slower.
        synchronized (image) {
          //System.out.println("inside j2d sync");
          image.getGraphics().drawImage(offscreen, 0, 0, null);
        }
      }
    } else {
      // TODO this is probably overkill for most tasks...
      loadPixels();
    }

    // Marking as modified, and then calling updatePixels() in
    // the super class, which just sets the mx1, my1, mx2, my2
    // coordinates of the modified area. This avoids doing the
    // full copy of the pixels to the surface in this.updatePixels().
    setModified();
    super.updatePixels();
    */
  }



  //////////////////////////////////////////////////////////////

  // BACKGROUND


//  @Override
//  public void backgroundImpl() {
//    if (backgroundAlpha) {
//      clearPixels(backgroundColor);
//
//    } else {
//      Color bgColor = new Color(backgroundColor);
//      // seems to fire an additional event that causes flickering,
//      // like an extra background erase on OS X
////      if (canvas != null) {
////        canvas.setBackground(bgColor);
////      }
//      //new Exception().printStackTrace(System.out);
//      // in case people do transformations before background(),
//      // need to handle this with a push/reset/pop
//      Composite oldComposite = g2.getComposite();
//      g2.setComposite(defaultComposite);
//
//      pushMatrix();
//      resetMatrix();
//      g2.setColor(bgColor); //, backgroundAlpha));
//      g2.fillRect(0, 0, width, height);
//      popMatrix();
//
//      g2.setComposite(oldComposite);
//    }
//  }



  //////////////////////////////////////////////////////////////


  /*
  @Override
  public void loadPixels() {
    nope("loadPixels");
  }

  @Override
  public void updatePixels() {
    nope("updatePixels");
  }

  @Override
  public void updatePixels(int x, int y, int c, int d) {
    nope("updatePixels");
  }

  //

  @Override
  public int get(int x, int y) {
    nope("get");
    return 0;  // not reached
  }

  @Override
  public PImage get(int x, int y, int c, int d) {
    nope("get");
    return null;  // not reached
  }

  @Override
  public PImage get() {
    nope("get");
    return null;  // not reached
  }

  @Override
  public void set(int x, int y, int argb) {
    nope("set");
  }

  @Override
  public void set(int x, int y, PImage image) {
    nope("set");
  }

  //

  @Override
  public void mask(int alpha[]) {
    nope("mask");
  }

  @Override
  public void mask(PImage alpha) {
    nope("mask");
  }

  //

  @Override
  public void filter(int kind) {
    nope("filter");
  }

  @Override
  public void filter(int kind, float param) {
    nope("filter");
  }

  //

  @Override
  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    nope("copy");
  }

  @Override
  public void copy(PImage src,
                   int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    nope("copy");
  }

  //

  public void blend(int sx, int sy, int dx, int dy, int mode) {
    nope("blend");
  }

  public void blend(PImage src,
                    int sx, int sy, int dx, int dy, int mode) {
    nope("blend");
  }

  @Override
  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    nope("blend");
  }

  @Override
  public void blend(PImage src,
                    int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    nope("blend");
  }

  //

  @Override
  public boolean save(String filename) {
    nope("save");
    return false;
  }
  */


  //////////////////////////////////////////////////////////////


  protected void nope(String function) {
    throw new RuntimeException("No " + function + "() for PGraphicsRetina2D");
  }
}