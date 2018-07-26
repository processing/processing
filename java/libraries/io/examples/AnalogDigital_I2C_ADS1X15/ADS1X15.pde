import processing.io.I2C;

// ADS1015 and ADS1115 are Analog-to-Digital converters using I2C
// they have four channels and 12 and 16 bits of resolution respectively
// datasheets: http://www.ti.com/lit/ds/symlink/ads1015.pdf
//             http://www.ti.com/lit/ds/symlink/ads1115.pdf

class ADS1015 extends ADS1X15 {
  ADS1015(String dev, int address) {
    super(dev, address);
    bitShift = 4;
    conversionDelay = 1;
  }

  // returns a number between -1.0 and 1.0
  float analogRead(int channel) {
    return readSingleEnded(channel) / 2047.0;
  }
}

class ADS1115 extends ADS1X15 {
  ADS1115(String dev, int address) {
    super(dev, address);
    bitShift = 0;
    conversionDelay = 8;
  }

  // returns a number between -1.0 and 1.0
  float analogRead(int channel) {
    return readSingleEnded(channel) / 32767.0;
  }
}


class ADS1X15 extends I2C {
  int address;
  int bitShift;         // bits to shift the result to the right
  int conversionDelay;  // in ms
  int channel;          // last channel used
  int range;            // see below

  // possible voltage ranges
  static final int INTERNAL_6V144 = 0;  // +/- 6.144V
  static final int INTERNAL_4V096 = 1;  // +/- 4.096V (library default)
  static final int INTERNAL_2V048 = 2;  // +/- 2.048V
  static final int INTERNAL_1V024 = 3;  // +/- 1.024V
  static final int INTERNAL_0V512 = 4;  // +/- 0.512V
  static final int INTERNAL_0V256 = 5;  // +/- 0.256V

  ADS1X15(String dev, int address) {
    super(dev);
    this.address = address;
    this.channel = -1;
    this.range = INTERNAL_4V096;
  }

  // be careful not to make the input voltage exceed VCC + 0.3V
  // this is regardless of the selected input range
  void analogReference(int type) {
    if (type < 0 || 7 < type) {
      throw new RuntimeException("Invalid range setting");
    }
    range = type;
  }

  int readSingleEnded(int channel) {
    if (channel < 0 || 3 < channel) {
      System.err.println("The channel needs to be from 0 to 3");
      throw new IllegalArgumentException("Unexpected channel");
    }

    if (channel != this.channel) {
      int config = 0x0183;                    // start with the default value from datasheet
      config &= ~0x100;                       // enable continuous readings
      config |= (range << 9);                 // set selected range (gain)
      config |= (1 << 14) | (channel << 12);  // set single-ended and channel
      config |= (1 << 15);                    // start a single conversion
      writeRegister(0x01, config);            // write to the configuration register at 0x01

      // when the channel switched we need to wait for the upcoming
      // conversion to finish
      delay(conversionDelay);

      // save the channel so that we don't need to do the same for
      // subsequent reads from the same channel
      this.channel = channel;
    }

    return readS16(0x00) >> bitShift;  // read from the conversion register at 0x00
    // the ADS1015 will have its 12-bit result in the upper bits, shift those right by four
  }

  protected void writeRegister(int register, int value) {
    beginTransmission(address);
    write(register);
    write(value >> 8);
    write(value & 0xFF);
    endTransmission();
  }

  protected int readS16(int register) {
    beginTransmission(address);
    write(register);
    byte[] in = read(2);
    return (in[0] << 8) | in[1];
  }
}
