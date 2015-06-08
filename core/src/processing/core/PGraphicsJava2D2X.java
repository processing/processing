package processing.core;


public class PGraphicsJava2D2X extends PGraphicsJava2D {
//  PImage retina;
//  int retinaWidth;
//  int retinaHeight;


  //////////////////////////////////////////////////////////////

  // INTERNAL


  public PGraphicsJava2D2X() {
    pixelDensity = 2;
//    retina = new PImage();
//    retina.format = RGB;
  }


//  @Override
//  public void setParent(PApplet parent) {
//    super.setParent(parent);
//    retina.parent = parent;
//  }


//  @Override
//  protected void allocate() {
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
//  }


  //////////////////////////////////////////////////////////////

  // FRAME


//  @Override
//  public boolean canDraw() {
//    return parent.getGraphicsConfiguration() != null;
//  }


  @Override
  public void beginDraw() {
    super.beginDraw();
////    GraphicsConfiguration gc = parent.getGraphicsConfiguration();
////
////    if (image == null) {
////      retina.width = width * 2;
////      retina.height = height * 2;
////      image = gc.createCompatibleImage(retina.width, retina.height);
////    }
//    g2 = (Graphics2D) image.getGraphics();
//
//    vertexCount = 0;
//    checkSettings();
//    resetMatrix(); // reset model matrix

    // inserted here for retina
    g2.scale(2, 2);
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
    if ((retina.pixels == null) || (retina.pixels.length != retina.width * retina.height)) {
      retina.pixels = new int[retina.width * retina.height];
    }
    getRaster().getDataElements(0, 0, retina.width, retina.height, retina.pixels);

    if (hints[ENABLE_RETINA_PIXELS]) {
      pixels = retina.pixels;

    } else {
      if ((pixels == null) || (pixels.length != width * height)) {
        pixels = new int[width * height];
      }
      int offset = 0;
      int roffset = 0;
//      int offset01 = 1;
//      int offset10 = retinaWidth;
//      int offset11 = retinaWidth + 1;

      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int px00 = retina.pixels[roffset];
          int px01 = retina.pixels[roffset + 1];
          int px10 = retina.pixels[roffset + retina.width];
          int px11 = retina.pixels[roffset + retina.width + 1];

          int red = ((((px00 >> 16) & 0xff) +
                      ((px01 >> 16) & 0xff) +
                      ((px10 >> 16) & 0xff) +
                      ((px11 >> 16) & 0xff)) << 14) & 0xFF0000;

          int green = ((((px00 >> 8) & 0xff) +
                        ((px01 >> 8) & 0xff) +
                        ((px10 >> 8) & 0xff) +
                        ((px11 >> 8) & 0xff)) << 6) & 0xFF00;

          int blue = (((px00 & 0xff) +
                       (px01 & 0xff) +
                       (px10 & 0xff) +
                       (px11 & 0xff)) >> 2) & 0xFF;

          pixels[offset++] = 0xff000000 | red | green | blue;
          roffset += 2;
//          offset01 += 2;
//          offset10 += 2;
//          offset11 += 2;
        }
        roffset += retina.width;
//        offset01 += retinaWidth;
//        offset10 += retinaWidth;
//        offset11 += retinaWidth;
      }
    }
  }


  @Override
  public void updatePixels() {
    if (hints[ENABLE_RETINA_PIXELS]) {
      updatePixels(0, 0, retina.width, retina.height);
    } else {
      updatePixels(0, 0, width, height);
    }
  }


  @Override
  public void updatePixels(int ux, int uy, int uw, int uh) {
    int wide = hints[ENABLE_RETINA_PIXELS] ? retina.width : width;
    int high = hints[ENABLE_RETINA_PIXELS] ? retina.height : height;
    if ((ux != 0) || (uy != 0) || (uw != wide) || (uh != high)) {
      // Show a warning message, but continue anyway.
      showVariationWarning("updatePixels(x, y, w, h)");
    }
    // If not using retina pixels, will need to first downsample
    if (!hints[ENABLE_RETINA_PIXELS]) {
      int offset = 0;
      int roffset = 0;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int orig = pixels[offset++];
          retina.pixels[roffset] = orig;
          retina.pixels[roffset + 1] = orig;
          retina.pixels[roffset + retina.width] = orig;
          retina.pixels[roffset + retina.width + 1] = orig;
        }
        roffset += retina.width;
      }
    }
    getRaster().setDataElements(0, 0, retina.width, retina.height, retina.pixels);
    modified = true;
  }


  static int rgetset[] = new int[4];

  @Override
  public int get(int x, int y) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      if ((x < 0) || (y < 0) || (x >= retina.width) || (y >= retina.height)) {
        return 0;
      }
      getRaster().getDataElements(x, y, getset);
      return getset[0];

    } else {
      if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
        return 0;
      }
      getRaster().getDataElements(x, y, 2, 2, getset);

      int red = ((((rgetset[0] >> 16) & 0xff) +
                  ((rgetset[1] >> 16) & 0xff) +
                  ((rgetset[2] >> 16) & 0xff) +
                  ((rgetset[3] >> 16) & 0xff)) << 14) & 0xFF0000;

      int green = ((((rgetset[0] >> 8) & 0xff) +
                    ((rgetset[1] >> 8) & 0xff) +
                    ((rgetset[2] >> 8) & 0xff) +
                    ((rgetset[3] >> 8) & 0xff)) << 6) & 0xFF00;

      int blue = (((rgetset[0] & 0xff) +
                   (rgetset[1] & 0xff) +
                   (rgetset[2] & 0xff) +
                   (rgetset[3] & 0xff)) >> 2) & 0xFF;

      return 0xff000000 | red | green | blue;
    }
  }


  @Override
  public PImage get(int x, int y, int w, int h) {
    int targetX = 0;
    int targetY = 0;
    int targetWidth = w;
    int targetHeight = h;
    boolean cropped = false;

    if (x < 0) {
      w += x; // x is negative, removes the left edge from the width
      targetX = -x;
      cropped = true;
      x = 0;
    }
    if (y < 0) {
      h += y; // y is negative, clip the number of rows
      targetY = -y;
      cropped = true;
      y = 0;
    }

    if (hints[ENABLE_RETINA_PIXELS]) {
      if (x + w > retina.width) {
        w = retina.width - x;
        cropped = true;
      }
      if (y + h > retina.height) {
        h = retina.height - y;
        cropped = true;
      }
    } else {
      if (x + w > width) {
        w = width - x;
        cropped = true;
      }
      if (y + h > height) {
        h = height - y;
        cropped = true;
      }
    }

    if (w < 0) {
      w = 0;
    }
    if (h < 0) {
      h = 0;
    }

    int targetFormat = format;
    if (cropped && format == RGB) {
      targetFormat = ARGB;
    }

    PImage target = new PImage(targetWidth, targetHeight, targetFormat);
    target.parent = parent;  // parent may be null so can't use createImage()
    if (w > 0 && h > 0) {
      getImpl(x, y, w, h, target, targetX, targetY);
    }
    return target;
  }


  // Replaces version found in PImage
  @Override
  public PImage get() {
    loadPixels();
    PImage outgoing;
    if (hints[ENABLE_RETINA_PIXELS]) {
//      outgoing = new PImage(retinaWidth, retinaHeight, RGB);
//      outgoing.pixels = new int[retinaWidth * retinaHeight];
//      System.arraycopy(retinaPixels, 0, outgoing.pixels, 0, outgoing.pixels.length);
//      outgoing.parent = parent;
      outgoing = retina.get();
      outgoing.parent = parent;

    } else {
      outgoing = new PImage(width, height, RGB);
      outgoing.pixels = new int[width * height];
      System.arraycopy(pixels, 0, outgoing.pixels, 0, outgoing.pixels.length);
      outgoing.parent = parent;
    }
    return outgoing;
  }


  // Found in PGraphicsJava2D
  //protected void getImpl(int sourceX, int sourceY,
  //                       int sourceWidth, int sourceHeight,
  //                       PImage target, int targetX, int targetY)


  @Override
  public void set(int x, int y, int argb) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      if (x >= 0 && y >= 0 && x < retina.width && y < retina.height) {
        getset[0] = argb;
        getRaster().setDataElements(x, y, getset);
      }
    } else {
      if (x >= 0 && y >= 0 && x < width && y < height) {
        for (int i = 0; i < 4; i++) {
          rgetset[i] = argb;
        }
        getRaster().setDataElements(x, y, 2, 2, rgetset);
      }
    }
  }


  // Replaces version found in PImage
  @Override
  public void set(int x, int y, PImage img) {
    int sx = 0;
    int sy = 0;
    int sw = img.width;
    int sh = img.height;

    if (x < 0) {  // off left edge
      sx -= x;
      sw += x;
      x = 0;
    }
    if (y < 0) {  // off top edge
      sy -= y;
      sh += y;
      y = 0;
    }
    if (hints[ENABLE_RETINA_PIXELS]) {
      if (x + sw > retina.width) {  // off right edge
        sw = retina.width - x;
      }
      if (y + sh > retina.height) {  // off bottom edge
        sh = retina.height - y;
      }
    } else {
      if (x + sw > width) {  // off right edge
        sw = width - x;
      }
      if (y + sh > height) {  // off bottom edge
        sh = height - y;
      }
    }

    // this could be nonexistent
    if ((sw <= 0) || (sh <= 0)) return;

    setImpl(img, sx, sy, sw, sh, x, y);
  }


  // Handled by PGraphicsJava2D
  //protected void setImpl(PImage sourceImage,
  //                       int sourceX, int sourceY,
  //                       int sourceWidth, int sourceHeight,
  //                       int targetX, int targetY)


  //////////////////////////////////////////////////////////////

  // MASK


  @Override
  public void mask(int alpha[]) {
    showMethodWarning("mask");
  }


  @Override
  public void mask(PImage alpha) {
    showMethodWarning("mask");
  }



  //////////////////////////////////////////////////////////////

  // FILTER


  @Override
  public void filter(int kind) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      loadPixels();
      // Wrap the pixels as a fake image so that width/height
      // in the filter() commands work properly.
//      PImage temp = new PImage();
//      temp.width = retinaWidth;
//      temp.height = retinaHeight;
//      temp.format = RGB;
//      temp.pixels = retinaPixels;
//      temp.filter(kind);
      retina.filter(kind);
      updatePixels();

    } else {
      super.filter(kind);
    }
  }


  @Override
  public void filter(int kind, float param) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      loadPixels();
      retina.filter(kind, param);
      updatePixels();

    } else {
      super.filter(kind);
    }
  }



  //////////////////////////////////////////////////////////////

  // COPY


  @Override
  public void copy(int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      super.copy(sx, sy, sw, sh, dx, dy, dw, dh);

    } else {
      // Since copying into itself, double pixels for both src and dest
      if ((sw != dw) || (sh != dh)) {
        g2.drawImage(image,
                     dx*2, dy*2, (dx + dw)*2, (dy + dh)*2,
                     sx*2, sy*2, (sx + sw)*2, (sy + sh)*2, null);
      } else {
        dx = dx - sx;  // java2d's "dx" is the delta, not dest
        dy = dy - sy;
        g2.copyArea(sx*2, sy*2, sw*2, sh*2, dx*2, dy*2);
      }
    }
  }


  @Override
  public void copy(PImage src,
                   int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      super.copy(src, sx, sy, sw, sh, dx, dy, dw, dh);

    } else {
      // Double the pixels from the source image when drawing into dest
      g2.drawImage((Image) src.getNative(),
                   dx*2, dy*2, (dx + dw)*2, (dy + dh)*2,
                   sx, sy, sx + sw, sy + sh, null);
    }
  }



  //////////////////////////////////////////////////////////////

  // BLEND


  @Override
  public void blend(int sx, int sy, int sw, int sh,
                    int dx, int dy, int dw, int dh, int mode) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      retina.blend(sx, sy, sw, sh, dx, dy, dw, dh, mode);
    } else {
      super.blend(sx, sy, sw, sh, dx, dy, dw, dh, mode);
    }
  }


  @Override
  public void blend(PImage src,
                    int sx, int sy, int sw, int sh,
                    int dx, int dy, int dw, int dh, int mode) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      retina.blend(src, sx, sy, sw, sh, dx, dy, dw, dh, mode);
    } else {
      super.blend(src, sx, sy, sw, sh, dx, dy, dw, dh, mode);
    }
  }



  //////////////////////////////////////////////////////////////

  // SAVE


  @Override
  public boolean save(String filename) {
    if (hints[ENABLE_RETINA_PIXELS]) {
      return retina.save(filename);
    } else {
      return super.save(filename);
    }
  }
    */
}