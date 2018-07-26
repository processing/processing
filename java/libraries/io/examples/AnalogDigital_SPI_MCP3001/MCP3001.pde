import processing.io.SPI;

// MCP3001 is a Analog-to-Digital converter using SPI
// datasheet: http://ww1.microchip.com/downloads/en/DeviceDoc/21293C.pdf

class MCP3001 extends SPI {

  MCP3001(String dev) {
    super(dev);
    settings(500000, SPI.MSBFIRST, SPI.MODE0);
  }

  // returns a number between 0.0 and 1.0
  float analogRead() {
    // dummy write, actual values don't matter
    byte[] out = { 0, 0 };
    byte[] in = transfer(out);
    // some input bit shifting according to the datasheet p. 16
    int val = ((in[0] & 0x1f) << 5) | ((in[1] & 0xf8) >> 3);
    // val is between 0 and 1023
    return val/1023.0;
  }
}
