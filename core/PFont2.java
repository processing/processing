/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

package processing.core;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;


public class PFont2 extends PFont {

  public PFont2(String name, int size) throws IOException {
    this(new Font(name, Font.PLAIN, size), true);
  }

  public PFont2(String name, int size, boolean smooth) throws IOException {
    this(new Font(name, Font.PLAIN, size), smooth);
  }

  public PFont2(Font font, boolean smooth) throws IOException {
    //int firstChar = 33;
    //int lastChar = 126;

    //this.charCount = lastChar - firstChar + 1;
    this.charCount = charset.length;
    this.mbox = font.getSize();

    // size for image/texture is next power of 2 over font size
    iwidth = iheight = (int) 
      Math.pow(2, Math.ceil(Math.log(mbox) / Math.log(2)));

    iwidthf = iheightf = (float) iwidth;

    /*
    iwidth = (int) 
      Math.pow(2, Math.ceil(Math.log(mbox) / Math.log(2)));
    iheight = (int) 
      Math.pow(2, Math.ceil(Math.log(mbox) / Math.log(2)));

    iwidthf = (float) iwidth;
    iheightf = (float) iheight;
    */

    images = new PImage[charCount];

    // allocate enough space for the character info
    value       = new int[charCount];
    height      = new int[charCount];
    width       = new int[charCount];
    setWidth    = new int[charCount];
    topExtent   = new int[charCount];
    leftExtent  = new int[charCount];

    int mbox3 = mbox * 3;

    BufferedImage playground = 
      new BufferedImage(mbox3, mbox3, BufferedImage.TYPE_INT_RGB);

    Graphics2D g = (Graphics2D) playground.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                       smooth ? 
                       RenderingHints.VALUE_ANTIALIAS_ON :
                       RenderingHints.VALUE_ANTIALIAS_OFF);

    g.setFont(font);
    FontMetrics metrics = g.getFontMetrics();

    int index = 0;
    for (int i = 0; i < charCount; i++) {
      char c = charset[i];
      if (!font.canDisplay(c)) {  // skip chars not in the font
        continue;
      }

      g.setColor(Color.white);
      g.fillRect(0, 0, mbox3, mbox3);
      g.setColor(Color.black);
      g.drawString(String.valueOf(c), mbox, mbox * 2);

      // grabs copy of the current data.. so no updates (do each time)
      Raster raster = playground.getData();

      //int w = metrics.charWidth(c);
      int minX = 1000, maxX = 0;
      int minY = 1000, maxY = 0;
      boolean pixelFound = false;

      for (int y = 0; y < mbox3; y++) {
        for (int x = 0; x < mbox3; x++) {
          int sample = raster.getSample(x, y, 0);  // maybe?
          // or int samples[] = raster.getPixel(x, y, null);

          //if (sample == 0) {  // or just not white? hmm
          if (sample != 255) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            pixelFound = true; 
            //System.out.println(x + " " + y + " = " + sample);
          }
        }
      }

      if (!pixelFound) {
        //System.out.println("no pixels found in char " + ((char)i));
        // this was dumb that it was set to 20 & 30, because for small
        // fonts, those guys don't exist
        minX = minY = 0; //20;
        maxX = maxY = 0; //30;

        // this will create a 1 pixel white (clear) character..
        // maybe better to set one to -1 so nothing is added?
      }

      value[index] = c;
      height[index] = (maxY - minY) + 1;
      width[index] = (maxX - minX) + 1;
      setWidth[index] = metrics.charWidth(c);

      // offset from vertical location of baseline
      // of where the char was drawn (mbox*2)
      topExtent[index] = mbox*2 - minY;

      // offset from left of where coord was drawn
      leftExtent[index] = minX - mbox;

      //System.out.println(height[index] + " " + width[index] + " " + 
      //                 setWidth[index] + " " + 
      //                 topExtent[index] + " " + leftExtent[index]);

      images[index] = new PImage(new int[width[index] * height[index]],
                                 width[index], height[index], ALPHA);

      for (int y = minY; y <= maxY; y++) {
        for (int x = minX; x <= maxX; x++) {
          //System.out.println("getting pixel " + x + " " + y);
          int value = 255 - raster.getSample(x, y, 0);
          int pindex = (y - minY) * width[index] + (x - minX);
          images[index].pixels[pindex] = value;
          //System.out.print(BApplet.nf(value, 3) + " ");
        }
        //System.out.println();
      }
      //System.out.println();
      index++;
    }
    charCount = index;
  }
}