import processing.io.I2C;

// MPR121 is a capacitive-touch sensor controller with 12 channels
// datasheet: https://www.nxp.com/docs/en/data-sheet/MPR121.pdf

class MPR121 extends I2C {
  int address;
  int touched;

  // registers used (there are more)
  static final int EFD0LB = 0x04; // ELE0 Electrode Filtered Data LSB
  static final int E0TTH = 0x41;  // ELE0 Touch Threshold
  static final int E0RTH = 0x42;  // ELE0 Release Threshold
  static final int E0BV = 0x1e;   // ELE0 Baseline Value
  static final int MHDR = 0x2b;   // MHD Rising
  static final int NHDR = 0x2c;   // NHD Amount Rising
  static final int NCLR = 0x2d;   // NCL Rising
  static final int MHDF = 0x2f;   // MHD Falling
  static final int NHDF = 0x30;   // NHD Amount Falling
  static final int NCLF = 0x31;   // NCL Falling
  static final int CDT = 0x5d;    // Filter/Global CDT Configuration
  static final int ECR = 0x5e;    // Electrode Configuration
  static final int SRST = 0x80;   // Soft Reset

  // there can be more than one device connected to the bus
  // as long as they have different addresses
  // possible addresses: 0x5a (default) - 0x5d
  MPR121(String dev, int address) {
    super(dev);
    this.address = address;
    reset();
  }

  void update() {
    beginTransmission(address);
    write(0x00);
    byte[] in = read(2);
    // & 0xff makes sure the byte is not interpreted as a negative value
    touched = (in[1] & 0xff) << 8 | (in[0] & 0xff);
  }

  boolean touched(int channel) {
    if (channel < 0 || 11 < channel) {
      return false;
    }
    if ((touched & (1 << channel)) != 0) {
      return true;
    } else {
      return false;
    }
  }

  void threshold(int touch, int release) {
    for (int i=0; i < 12; i++) {
      threshold(touch, release, i);
    }
  }

  void threshold(int touch, int release, int channel) {
    if (channel < 0 || 11 < channel) {
      return;
    }
    touch = constrain(touch, 0, 255);
    release = constrain(release, 0, 255);
    writeRegister(E0TTH + 2*channel, touch);
    writeRegister(E0RTH + 2*channel, release);
  }

  int analogRead(int channel) {
    if (channel < 0 || 11 < channel) {
      return 0;
    }
    beginTransmission(address);
    write(EFD0LB + 2*channel);
    byte[] in = read(2);
    return (in[1] & 0xff) << 8 | (in[0] & 0xff);
  }

  int analogReadBaseline(int channel) {
    if (channel < 0 || 11 < channel) {
      return 0;
    }
    beginTransmission(address);
    write(E0BV + channel);
    byte[] in = read(1);
    return (in[0] & 0xff) << 2;
  }

  void reset() {
    writeRegister(SRST, 0x63);
    delay(1);
    threshold(12, 6);
    // set baseline filtering control registers (see p. 12)
    writeRegister(MHDR, 0x01);
    writeRegister(NHDR, 0x01);
    writeRegister(NCLR, 0x0e);
    writeRegister(MHDF, 0x01);
    writeRegister(NHDF, 0x05);
    writeRegister(NCLF, 0x01);
    // change sample interval to 1ms period from default 16ms
    writeRegister(CDT, 0x20);
    // start sampling
    writeRegister(ECR, 0x8f);
  }

  void writeRegister(int register, int value) {
    beginTransmission(address);
    write(register);
    write(value);
    endTransmission();
  }
}
