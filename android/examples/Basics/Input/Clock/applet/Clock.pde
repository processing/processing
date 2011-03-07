/**
 * Clock. 
 * 
 * The current time can be read with the second(), minute(), 
 * and hour() functions. In this example, sin() and cos() values
 * are used to set the position of the hands.
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
  float m = map(minute() + norm(second(), 0, 60), 0, 60, 0, TWO_PI) - HALF_PI; 
  float h = map(hour() + norm(minute(), 0, 60), 0, 24, 0, TWO_PI * 2) - HALF_PI;
  stroke(255);
  strokeWeight(1);
  line(100, 100, cos(s) * 72 + 100, sin(s) * 72 + 100);
  strokeWeight(2);
  line(100, 100, cos(m) * 60 + 100, sin(m) * 60 + 100);
  strokeWeight(4);
  line(100, 100, cos(h) * 50 + 100, sin(h) * 50 + 100);
  
  // Draw the minute ticks
  strokeWeight(2);
  for (int a = 0; a < 360; a+=6) {
    float x = 100 + ( cos(radians(a)) * 72 );
    float y = 100 + ( sin(radians(a)) * 72 );
    point(x, y);
  }
}
