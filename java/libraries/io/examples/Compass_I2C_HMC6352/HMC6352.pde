import processing.io.I2C;

// HMC6352 is a digital compass using I2C
// datasheet: https://www.sparkfun.com/datasheets/Components/HMC6352.pdf

class HMC6352 extends I2C {
  int address;

  HMC6352(String dev, int address) {
    super(dev);
    this.address = address;
    setHeadingMode();
  }

  void setHeadingMode() {
    beginTransmission(address);
    // command byte for writing to EEPROM
    write(0x77);
    // address of the output data control byte
    write(0x4e);
    // give us the plain heading
    write(0x00);
    endTransmission();
  }

  float heading() {
    beginTransmission(address);
    // command byte for reading the data
    write(0x41);
    byte[] in = read(2);
    endTransmission();
    // put bytes together to tenth of degrees
    // & 0xff makes sure the byte is not interpreted as a negative value
    int deg = (in[0] & 0xff) << 8 | (in[1] & 0xff);
    // return degrees
    return deg / 10.0;
  }
}
