import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Mandelbrot extends PApplet {// The Mandelbrot Set
// Daniel Shiffman <http://www.shiffman.net>

// Simple rendering of the Mandelbrot set
// c = a + bi
// Iterate z = z^2 + c, i.e.
// z(0) = 0
// z(1) = 0*0 + c
// z(2) = c*c + c
// z(3) = (c*c + c) * (c*c + c) + c
// etc.
// c*c = (a+bi) * (a+bi) = a^2 - b^2 + 2abi

// Created 2 May 2005

// Establish a range of values on the complex plane
double xmin = -2.5f; double ymin = -2; double wh = 4;
// A different range will allow us to "zoom" in or out on the fractal
// double xmin = -1.5; double ymin = -.1; double wh = 0.15;

public void setup() {
  size(200,200,P2D);
}

public void draw() {

  loadPixels();
  
  // Maximum number of iterations for each point on the complex plane
  int maxiterations = 200;

  // x goes from xmin to xmax
  double xmax = xmin + wh;
  // y goes from ymin to ymax
  double ymax = ymin + wh;
  
  // Calculate amount we increment x,y for each pixel
  double dx = (xmax - xmin) / (width);
  double dy = (ymax - ymin) / (height);

  // Start y
  double y = ymin;
  for(int j = 0; j < height; j++) {
    // Start x
    double x = xmin;
    for(int i = 0;  i < width; i++) {
      
      // Now we test, as we iterate z = z^2 + cm does z tend towards infinity?
      double a = x;
      double b = y;
      int n = 0;
      while (n < maxiterations) {
        double aa = a * a;
        double bb = b * b;
        double twoab = 2.0f * a * b;
        a = aa - bb + x;
        b = twoab + y;
        // Infinty in our finite world is simple, let's just consider it 16
        if(aa + bb > 16.0f) {
          break;  // Bail
        }
        n++;
      }
      
      // We color each pixel based on how long it takes to get to infinity
      // If we never got there, let's pick the color black
      if (n == maxiterations) pixels[i+j*width] = 0;
      else pixels[i+j*width] = color(n*16 % 255);  // Gosh, we could make fancy colors here if we wanted
      x += dx;
    }
    y += dy;
  }
  updatePixels();
  noLoop();
}

}