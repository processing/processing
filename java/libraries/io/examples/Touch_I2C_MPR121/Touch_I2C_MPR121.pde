import processing.io.*;
MPR121 touch;

// see setup.png in the sketch folder for wiring details

void setup() {
  size(600, 200);
  //printArray(I2C.list());
  touch = new MPR121("i2c-1", 0x5a);
}

void draw() {
  background(204);
  noStroke();

  touch.update();

  for (int i=0; i < 12; i++) {
    if (touch.touched(i)) {
      fill(255, 0, 0);
    } else {
      fill(255, 255, 255);
    }
    ellipse((width/12) * (i+0.5), height/2, 20, 20);
  }
}
