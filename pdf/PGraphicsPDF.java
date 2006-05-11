package processing.pdf;

import java.io.*;
import java.util.*;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import processing.core.*;


public class PGraphicsPDF extends PGraphics2 {

  File temp;
  //int frameCount;
  File file;
  Document document;
  PdfWriter writer;
  PdfContentByte content;
  //PdfTemplate tp;
  DefaultFontMapper mapper;

  // BaseFont baseFont = mapper.awtToPdf(java.awt.Font awtFont)


  public PGraphicsPDF(int width, int height, PApplet applet) {
    this(width, height, applet, null);  // will throw an error
  }


  public PGraphicsPDF(int width, int height, PApplet applet, String path) {
    super(width, height, null);

    //System.out.println("trying " + path);

    if (path != null) {
      file = new File(path);
      if (!file.isAbsolute()) file = null;
    }
    if (file == null) {
      throw new RuntimeException("PGraphicsPDF requires an absolute path " +
                                 "for the location of the output file.");
    }

    //if (applet != null) {
    //  applet.registerDispose(this);
    //}

    //System.out.println("making " + path);

    //if (path == null) path = "output.pdf";
    //this.file = new File(path);

    // don't want to require PApplet as the way to do this.. but how?
    //if (applet != null) {
    //applet.registerDispose(this);
    //}

    /*
    mapper = new DefaultFontMapper();
    FontFactory.registerDirectories();

    // ummm.. problematic?
    //mapper.insertDirectory("c:\\winxp\\fonts");
    mapper.insertDirectory("/System/Library/Fonts");
    mapper.insertDirectory("/Library/Fonts");
    mapper.insertDirectory("/Users/fry/Library/Fonts");
    */

    // seems to only pick up ttf and otf fonts
    //FontFactory.registerDirectory("/System/Library/Fonts");
    //FontFactory.registerDirectory("/Library/Fonts");
    //FontFactory.registerDirectory("/Users/fry/Library/Fonts");

    /*
    Set registered = FontFactory.getRegisteredFonts();
    for (Iterator i = registered.iterator(); i.hasNext(); ) {
      System.out.println((String) i.next());
    }
    */
  }


  // create a temporary file and put the graphics crap there
  // don't start a fresh page if frameCount is zero (setup isn't its own page)

  /**
   * all the init stuff happens in here, in case someone calls size()
   * along the way and wants to hork things up.
   */
  protected void allocate() {
    // can't do anything here, because this will be called by the
    // superclass PGraphics, and the file/path object won't be set yet
    // (since super() called right at the beginning of the constructor)
  }


  /*
  public void defaults() {
    System.out.println("PGraphicsPDF.defaults()");
    super.defaults();
  }
  */


  // if the File object changes, then need to start a new file
  //
  /*
  public void record(int frameCount, File ifile) {
    this.frameCount = frameCount;
    if (ifile == file) {
      // same shit, different pile
      // start a new page on the file that's currently open
      return;

    } else {

    if (!file.getName().endsWith(".pdf")) {
      // yeaeaargh
    }
  }
  */

  public void beginDraw() {
    // temporary
    //file = new File(filename); //"test.pdf");
    //System.out.println("pdf beginDraw()");
    //document = new Document();

    if (document == null) {
      document = new Document(new Rectangle(width, height));
      try {
        writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();
        content = writer.getDirectContent();

      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Problem saving the PDF file.");
      }

      // how to call newPage() in here?
      /*
        System.out.println("beginDraw() " + width + ", " + height);
        tp = content.createTemplate(width, height);
        //g2 = tp.createGraphics(width, height, mapper);
        g2 = tp.createGraphicsShapes(width, height);
        //System.out.println("g2 is " + g2);
        tp.setWidth(width);
        tp.setHeight(height);
      */

      // what's a good way to switch between these?
      // the regular createGraphics doesn't seem to recognize fonts
      // how should the insertDirectory stuff be used properly?
      //g2 = content.createGraphics(width, height);
      g2 = content.createGraphicsShapes(width, height);
    }
    super.beginDraw();
  }


  /*
  public void rect(float x1, float y1, float x2, float y2) {
    System.out.println("calling rect " + x1 + " " + y1 + " " + x2 + " " + y2);
    super.rect(x1, y1, x2, y2);
  }
  */


  /**
   * Call to explicitly go to the next page from within a single draw().
   */
  public void nextPage() {
    g2.dispose();
    try {
      document.newPage();  // is this bad if no addl pages are made?
    } catch (Exception e) {
      e.printStackTrace();
    }
    g2 = content.createGraphicsShapes(width, height);

    // should there be a beginDraw/endDraw in here?
  }


  public void endDraw() {
    //System.out.println("endDraw()");

    /*
    String text1 = "This text has \u0634\u0627\u062f\u062c\u0645\u0647\u0648\u0631 123,456 \u0645\u0646 (Arabic)";
    java.awt.Font font = new java.awt.Font("arial", 0, 18);
    g2.setFont(font);
    g2.drawString(text1, 100, 100);
    */

    /*
    content.addTemplate(tp, 0, 0); //50, 400);
    */

    /*
    try {
      document.newPage();  // is this bad if no addl pages are made?
    } catch (Exception e) {
      e.printStackTrace();
    }
    */

    /*
      g2.dispose();
      document.close();  // can't be done in finalize, not always called
    */
  }


  public void dispose() {
    //System.out.println("calling dispose");
    if (document != null) {
      g2.dispose();
      document.close();  // can't be done in finalize, not always called
      document = null;
    }
    //new Exception().printStackTrace(System.out);
  }


  /**
   * Don't open a window for this renderer, it won't be used.
   */
  public boolean displayable() {
    return false;
  }

  /*
  protected void finalize() throws Throwable {
    System.out.println("calling finalize");
  //document.close();  // do this in dispose instead?
  }
  */


  //////////////////////////////////////////////////////////////


  /*
  public void endRecord() {
    super.endRecord();
    dispose();
  }


  public void endRaw() {
    System.out.println("ending raw");
    super.endRaw();
    System.out.println("disposing");
    dispose();
    System.out.println("done");
  }
  */


  //////////////////////////////////////////////////////////////


  /*
  protected void rectImpl(float x1, float y1, float x2, float y2) {
    //rect.setFrame(x1, y1, x2-x1, y2-y1);
    //draw_shape(rect);
    System.out.println("rect implements");
    g2.fillRect((int)x1, (int)y1, (int) (x2-x1), (int) (y2-y1));
  }
  *

  /*
  public void clear() {
    g2.setColor(Color.red);
    g2.fillRect(0, 0, width, height);
  }
  */


  //////////////////////////////////////////////////////////////


  /*
  protected void imageImplAWT(java.awt.Image awtImage,
                              float x1, float y1, float x2, float y2,
                              int u1, int v1, int u2, int v2) {
    pushMatrix();
    translate(x1, y1);
    int awtImageWidth = awtImage.getWidth(null);
    int awtImageHeight = awtImage.getHeight(null);
    scale((x2 - x1) / (float)awtImageWidth,
          (y2 - y1) / (float)awtImageHeight);
    g2.drawImage(awtImage,
                 0, 0, awtImageWidth, awtImageHeight,
                 u1, v1, u2, v2, null);
    popMatrix();
  }
  */


  //////////////////////////////////////////////////////////////


  public void loadPixels() {
    nope("loadPixels");
  }

  public void updatePixels() {
    nope("updatePixels");
  }

  public void updatePixels(int x, int y, int c, int d) {
    nope("updatePixels");
  }

  //

  public int get(int x, int y) {
    nope("get");
    return 0;  // not reached
  }

  public PImage get(int x, int y, int c, int d) {
    nope("get");
    return null;  // not reached
  }

  public PImage get() {
    nope("get");
    return null;  // not reached
  }

  public void set(int x, int y, int argb) {
    nope("set");
  }

  public void set(int x, int y, PImage image) {
    nope("set");
  }

  //

  public void mask(int alpha[]) {
    nope("mask");
  }

  public void mask(PImage alpha) {
    nope("mask");
  }

  //

  public void filter(int kind) {
    nope("filter");
  }

  public void filter(int kind, float param) {
    nope("filter");
  }

  //

  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    nope("copy");
  }

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

  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    nope("blend");
  }

  public void blend(PImage src,
                    int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    nope("blend");
  }

  //

  public void save(String filename) {
    nope("save");
  }


  //////////////////////////////////////////////////////////////


  protected void nope(String function) {
    throw new RuntimeException("No " + function + "() for PGraphicsPDF");
  }
}
