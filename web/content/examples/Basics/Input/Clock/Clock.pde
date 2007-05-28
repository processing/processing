/**
 * Clock. 
 * 
 * The current time can be read with the second(), minute(), 
 * and hour() functions. In this example, sin() and cos() values
 * are used to set the position of the hands.
 *
 * Created 27 May 2007
 */

void setup() {
  size(200, 200);
  stroke(255);
  smooth();
}
void draw() {
  background(0);
  fill(80);
  noStroke();
  // Angles for sin() and cos() start at 3 o'clock;
  // subtract HALF_PI to make them start at the top
  ellipse(100, 100, 160, 160);
  float s = map(second(), 0, 60, 0, TWO_PI) - HALF_PI;
  float m = map(minute(), 0, 60, 0, TWO_PI) - HALF_PI;
  float h = map(hour() % 12, 0, 12, 0, TWO_PI) - HALF_PI;
  stroke(255);
  strokeWeight(1);
  line(100, 100, cos(s) * 72 + 100, sin(s) * 72 + 100);
  strokeWeight(2);
  line(100, 100, cos(m) * 60 + 100, sin(m) * 60 + 100);
  strokeWeight(4);
  line(100, 100, cos(h) * 50 + 100, sin(h) * 50 + 100);
}
