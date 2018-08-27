import processing.io.*;
I2C i2c;

// MCP4725 is a Digital-to-Analog converter using I2C
// datasheet: http://ww1.microchip.com/downloads/en/DeviceDoc/22039d.pdf

// also see DigitalAnalog_I2C_MCP4725 for how to write the
// same sketch in an object-oriented way

void setup() {
  //printArray(I2C.list());
  i2c = new I2C(I2C.list()[0]);
}

void draw() {
  background(map(mouseX, 0, width, 0, 255));
  setAnalog(map(mouseX, 0, width, 0.0, 1.0));
}

// outputs voltages from 0V to the supply voltage
// (works with 3.3V and 5V)
void setAnalog(float fac) {
  fac = constrain(fac, 0.0, 1.0);
  // convert to 12 bit value
  int val = int(4095 * fac);
  i2c.beginTransmission(0x60);
  i2c.write(val >> 8);
  i2c.write(val & 255);
  i2c.endTransmission();
}
