/**
 * Random. 
 * 
 * Random numbers create the basis of this image. 
 * Each time the program is loaded the result is different. 
 */

void setup() {
  size(640, 360);
  background(0);
  strokeWeight(20);
  frameRate(2);
}

void draw() {
  for (int i = 0; i < width; i++) {
    float r = random(255);
    stroke(r);
    line(i, 0, i, height);
  }
}

