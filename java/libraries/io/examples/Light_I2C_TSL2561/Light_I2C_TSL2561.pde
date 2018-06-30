import processing.io.*;
TSL2561 sensor;

// see setup.png in the sketch folder for wiring details

// this variable will contain the measured brightness
// Lux (lx) is the unit of illuminance
float lux;

void setup() {
  size(700, 100);
  textSize(72);
  //printArray(I2C.list());
  sensor = new TSL2561("i2c-1", 0x39);
}

void draw() {
  background(0);
  stroke(255);
  lux = sensor.lux();
  text(String.format("Light:  %.02f Lux", lux), 10, 75);
}

void dispose() {
  // turn the sensor off
  sensor.stop();
}
