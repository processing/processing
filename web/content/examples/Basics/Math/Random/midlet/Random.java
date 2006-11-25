import processing.core.*; public class Random extends PMIDlet{public void setup() {// Random
// by REAS <http://reas.com>

// Random numbers create the basis of this image.
// Each time the program is loaded the result is different.

// Created 1 September 2002

size(200, 200);
noStroke();

for(int i=0; i<width; i++) {
  float r = random(0, 255);
  stroke(r);
  point(i, 0);
}

for(int i=1; i<width; i++) {
  for(int j=0; j<height; j++) {
    int p = get(j, i-1);
    stroke(red(p)-1);
    point(j, i);
  }
}
noLoop(); }}