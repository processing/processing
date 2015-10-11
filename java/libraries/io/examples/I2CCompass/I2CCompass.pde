import processing.io.*;
I2C i2c;

// HMC6352 is a digital compass module using I2C
// datasheet: https://www.sparkfun.com/datasheets/Components/HMC6352.pdf

void setup() {
  //printArray(I2C.list());
  i2c = new I2C(I2C.list()[0]);
  setHeadingMode();
}

void draw() {
  background(255);
  float deg = getHeading();
  println(deg + " degrees");
  line(width/2, height/2, width/2+sin(radians(deg))*width/2, height/2-cos(radians(deg))*height/2);
}

void setHeadingMode() {
  i2c.beginTransmission(0x21);
  // command byte for writing to EEPROM
  i2c.write((byte) 0x77);
  // address of the output data control byte
  i2c.write((byte) 0x4e);
  // give us the plain heading
  i2c.write((byte) 0x00);
  i2c.endTransmission();
}

float getHeading() {
  i2c.beginTransmission(0x21);
  // command byte for reading the data
  i2c.write((byte) 0x41);
  byte[] in = i2c.read(2);
  i2c.endTransmission();
  // put bytes together to tenth of degrees
  // & 0xff makes sure the byte is not interpreted as a negative value
  int deg = (in[0] & 0xff) << 8 | (in[1] & 0xff);
  // return degrees
  return deg / 10.0;
}
