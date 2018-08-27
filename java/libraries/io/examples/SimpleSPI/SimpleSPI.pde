import processing.io.*;
SPI spi;

// MCP3001 is a Analog-to-Digital converter using SPI
// datasheet: http://ww1.microchip.com/downloads/en/DeviceDoc/21293C.pdf
// see setup.png in the sketch folder for wiring details

// also see AnalogDigital_SPI_MCP3001 for how to write the
// same sketch in an object-oriented way

void setup() {
  //printArray(SPI.list());
  spi = new SPI(SPI.list()[0]);
  spi.settings(500000, SPI.MSBFIRST, SPI.MODE0);
}

void draw() {
  // dummy write, actual values don't matter
  byte[] out = { 0, 0 };
  byte[] in = spi.transfer(out);
  // some input bit shifting according to the datasheet p. 16
  int val = ((in[0] & 0x1f) << 5) | ((in[1] & 0xf8) >> 3);
  // val is between 0 and 1023
  background(map(val, 0, 1023, 0, 255));
}
