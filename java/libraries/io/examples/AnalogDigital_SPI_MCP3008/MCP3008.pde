import processing.io.SPI;

// MCP3008 is a Analog-to-Digital converter using SPI
// other than the MCP3001, this has 8 input channels
// datasheet: http://ww1.microchip.com/downloads/en/DeviceDoc/21295d.pdf

class MCP3008 extends SPI {

  MCP3008(String dev) {
    super(dev);
    settings(500000, SPI.MSBFIRST, SPI.MODE0);
  }

  // returns a number between 0.0 and 1.0
  float analogRead(int channel) {
    if (channel < 0 ||  7 < channel) {
      System.err.println("The channel needs to be from 0 to 7");
      throw new IllegalArgumentException("Unexpected channel");
    }
    byte[] out = { 0, 0, 0 };
    // encode the channel number in the first byte
    out[0] = (byte)(0x18 | channel);
    byte[] in = transfer(out);
    int val = ((in[1] & 0x03) << 8) | (in[2] & 0xff);
    // val is between 0 and 1023
    return val/1023.0;
  }
}
