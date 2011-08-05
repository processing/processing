/**
 * Random. 
 * 
 * Random numbers create the basis of this image. 
 * Each time the program is loaded the result is different. 
 */

void setup() {
  size(640, 360);
  smooth();
  background(0);
  strokeWeight(20);
  frameRate(2);
}

void draw() {
  for (int i = 0; i < width; i++) {
    float r = random(255);
    float x = random(0, width);
    stroke(r, 100);
    line(i, 0, x, height);
  }
}

