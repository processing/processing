import processing.io.*;
PCA9685 servos;

// see setup.png in the sketch folder for wiring details

void setup() {
  size(400, 300);
  //printArray(I2C.list());
  servos = new PCA9685("i2c-1", 0x40);
  servos.attach(0);
  servos.attach(1);
}

void draw() {
  background(0);
  stroke(255);
  strokeWeight(3);

  // we don't go right to the edge to prevent
  // making the servo unhappy
  float angle = 90 + sin(frameCount / 100.0)*85;
  servos.write(0, angle);
  float y = map(angle, 0, 180, 0, height);
  line(0, y, width/2, y);

  angle = 90 + cos(frameCount / 100.0)*85;
  servos.write(1, 90 + cos(frameCount / 100.0)*85);
  y = map(angle, 0, 180, 0, height);
  line(width/2, y, width, y);
}
