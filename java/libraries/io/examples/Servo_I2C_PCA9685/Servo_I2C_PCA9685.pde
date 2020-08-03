import processing.io.*;
PCA9685 servos;

// see setup.png in the sketch folder for wiring details

void setup() {
  size(400, 300);
  //printArray(I2C.list());
  servos = new PCA9685("i2c-1", 0x40);

  // different servo motors will vary in the pulse width they expect
  // the lines below set the pulse width for 0 degrees to 544 microseconds (μs)
  //                 and the pulse width for 180 degrees to 2400 microseconds
  // these values match the defaults of the Servo library on Arduino
  // but you might need to modify this for your particular servo still
  servos.attach(0, 544, 2400);
  servos.attach(1, 544, 2400);
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

void dispose() {
  servos.detach(0);
  servos.detach(1);
}
