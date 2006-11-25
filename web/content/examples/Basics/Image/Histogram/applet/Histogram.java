import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Histogram extends PApplet {public void setup() {// Histogram
// By REAS <http://reas.com>

// Calculates the histogram of an image.
// A histogram is the frequency distribution 
// of the gray levels with the number of pure black values
// displayed on the left and number of pure white values on the right.

// Created 10 August 2002

size(200, 200);
colorMode(RGB, width);

int[] hist = new int[width];

// Load an image from the data directory
// Load a different image by modifying the comments
PImage a;
a = loadImage("cdi01_g.jpg");
image(a, 0, 0);

// Calculate the histogram
for (int i=0; i<width; i++) {
  for (int j=0; j<height; j++) {
    hist[PApplet.toInt(red(get(i, j)))]++; 
  }
} 

// Find the largest value in the histogram
float maxval = 0;
for (int i=0; i<width; i++) {
  if(hist[i] > maxval) {
    maxval = hist[i];
  }  
}

// Normalize the histogram to values between 0 and "height"
for (int i=0; i<width; i++) {
  hist[i] = PApplet.toInt(hist[i]/maxval * height);
}

// Draw half of the histogram (skip every second value)
stroke(width);
for (int i=0; i<width; i+=2) {
  line(i, height, i, height-hist[i]);
}

noLoop(); }}