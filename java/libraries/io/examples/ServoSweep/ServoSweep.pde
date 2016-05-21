import processing.io.*;

// see setup.png in the sketch folder for wiring details
// for more reliable operation it is recommended to power
// the servo from an external power source, see setup_better.png

SoftwareServo servo1;
SoftwareServo servo2;

void setup() {
  size(400, 300);
  servo1 = new SoftwareServo(this);
  servo1.attach(17);
  servo2 = new SoftwareServo(this);
  servo2.attach(4);
}

void draw() {
  background(0);
  stroke(255);
  strokeWeight(3);

  // we don't go right to the edge to prevent
  // making the servo unhappy
  float angle = 90 + sin(frameCount / 100.0)*85;
  servo1.write(angle);
  float y = map(angle, 0, 180, 0, height);
  line(0, y, width/2, y);

  angle = 90 + cos(frameCount / 100.0)*85;
  servo2.write(90 + cos(frameCount / 100.0)*85);
  y = map(angle, 0, 180, 0, height);
  line(width/2, y, width, y);
}
