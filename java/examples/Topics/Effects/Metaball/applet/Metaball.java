import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class Metaball extends PApplet {

/**
 * Metaball Demo Effect
 * by luis2048. 
 * 
 * Organic-looking n-dimensional objects. The technique for rendering 
 * metaballs was invented by Jim Blinn in the early 1980s. Each metaball 
 * is defined as a function in n-dimensions. 
 */

int numBlobs = 3;

int[] blogPx = { 0, 90, 90 };
int[] blogPy = { 0, 120, 45 };

// Movement vector for each blob
int[] blogDx = { 1, 1, 1 };
int[] blogDy = { 1, 1, 1 };

PGraphics pg;
int[][] vy,vx; 

public void setup() {
  size(640, 360);
  pg = createGraphics(160, 90, P2D);    
  vy = new int[numBlobs][pg.height];
  vx = new int[numBlobs][pg.width];
}

public void draw() {
  for (int i=0; i<numBlobs; ++i) {
    blogPx[i]+=blogDx[i];
    blogPy[i]+=blogDy[i];

    // bounce across screen
    if (blogPx[i] < 0) {
      blogDx[i] = 1;
    }
    if (blogPx[i] > pg.width) {
      blogDx[i] = -1;
    }
    if (blogPy[i] < 0) {
      blogDy[i] = 1;
    }
    if (blogPy[i] > pg.height) {
      blogDy[i]=-1;
    }

    for (int x = 0; x < pg.width; x++) {
      vx[i][x] = PApplet.parseInt(sq(blogPx[i]-x));
    }

    for (int y = 0; y < pg.height; y++) {
      vy[i][y] = PApplet.parseInt(sq(blogPy[i]-y)); 
    }
  }

  // Output into a buffered image for reuse
  pg.beginDraw();
  pg.loadPixels();
  for (int y = 0; y < pg.height; y++) {
    for (int x = 0; x < pg.width; x++) {
      int m = 1;
      for (int i = 0; i < numBlobs; i++) {
        // Increase this number to make your blobs bigger
        m += 60000/(vy[i][y] + vx[i][x]+1);
      }
      pg.pixels[x+y*pg.width] = color(0, m+x, (x+m+y)/2);
    }
  }
  pg.updatePixels();
  pg.endDraw();

  // Display the results
  image(pg, 0, 0, width, height); 
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Metaball" });
  }
}
