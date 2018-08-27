import processing.io.*;
HMC6352 compass;

// see setup.png in the sketch folder for wiring details

void setup() {
  // the module's I2C address can be changed by modifying values in its EEPROM
  // 0x21 is however the default address

  //printArray(I2C.list());
  compass = new HMC6352("i2c-1", 0x21);
}

void draw() {
  background(255);
  float deg = compass.heading();
  println(deg + " degrees");
  line(width/2, height/2, width/2+sin(radians(deg))*width/2, height/2-cos(radians(deg))*height/2);
}
