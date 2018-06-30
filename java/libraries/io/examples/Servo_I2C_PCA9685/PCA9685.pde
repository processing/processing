import processing.io.I2C;

// PCA9685 is a 16-channel servo/PWM driver
// datasheet: https://cdn-shop.adafruit.com/datasheets/PCA9685.pdf
// code contributed by @OlivierLD

public class PCA9685 extends I2C {
  public final static int PCA9685_ADDRESS = 0x40;

  // registers used
  public final static int MODE1 = 0x00;
  public final static int PRESCALE = 0xFE;
  public final static int LED0_ON_L = 0x06;
  public final static int LED0_ON_H = 0x07;
  public final static int LED0_OFF_L = 0x08;
  public final static int LED0_OFF_H = 0x09;

  private int address;
  private int freq = 200;              // 200 Hz default frequency (after power-up)
  private boolean hasFreqSet = false;  // whether a different frequency has been set
  private int minPulses[] = new int[16];
  private int maxPulses[] = new int[16];


  public PCA9685(String dev) {
    this(dev, PCA9685_ADDRESS);
  }
  public PCA9685(String dev, int address) {
    super(dev);
    this.address = address;
    // reset device
    command(MODE1, (byte) 0x00);
  }


  public void attach(int channel) {
    // same as on Arduino
    attach(channel, 544, 2400);
  }

  public void attach(int channel, int minPulse, int maxPulse) {
    if (channel < 0 || 15 < channel) {
      throw new IllegalArgumentException("Channel must be between 0 and 15");
    }
    minPulses[channel] = minPulse;
    maxPulses[channel] = maxPulse;

    // set the PWM frequency to be the same as on Arduino
    if (!hasFreqSet) {
      frequency(50);
    }
  }

  public void write(int channel, float angle) {
    if (channel < 0 || 15 < channel) {
      throw new IllegalArgumentException("Channel must be between 0 and 15");
    }
    if (angle < 0 || 180 < angle) {
      throw new IllegalArgumentException("Angle must be between 0 and 180");
    }
    int us = (int)(minPulses[channel] + (angle/180.0) * (maxPulses[channel]-minPulses[channel]));

    double pulseLength = 1000000; // 1s = 1,000,000 us per pulse
    pulseLength /= freq;          // 40..1000 Hz
    pulseLength /= 4096;          // 12 bits of resolution
    int pulse = us;
    pulse /= pulseLength;
    // println(pulseLength + " us per bit, pulse:" + pulse);
    pwm(channel, 0, pulse);
  }

  public boolean attached(int channel) {
    if (channel < 0 || 15 < channel) {
      return false;
    }
    return (maxPulses[channel] != 0) ? true : false;
  }

  public void detach(int channel) {
    pwm(channel, 0, 0);
    minPulses[channel] = 0;
    maxPulses[channel] = 0;
  }


  /**
   * @param freq 40..1000 Hz
   */
  public void frequency(int freq) {
    this.freq = freq;
    float preScaleVal = 25000000.0f; // 25MHz
    preScaleVal /= 4096.0;           // 4096: 12-bit
    preScaleVal /= freq;
    preScaleVal -= 1.0;
    // println("Setting PWM frequency to " + freq + " Hz");
    // println("Estimated pre-scale: " + preScaleVal);
    double preScale = Math.floor(preScaleVal + 0.5);
    // println("Final pre-scale: " + preScale);
    byte oldmode = (byte) readU8(MODE1);
    byte newmode = (byte) ((oldmode & 0x7F) | 0x10); // sleep
    command(MODE1, newmode);                         // go to sleep
    command(PRESCALE, (byte) (Math.floor(preScale)));
    command(MODE1, oldmode);
    delay(5);
    command(MODE1, (byte) (oldmode | 0x80));
    hasFreqSet = true;
  }

  /**
   * @param channel 0..15
   * @param on      cycle offset to turn output on (0..4095)
   * @param off     cycle offset to turn output off again (0..4095)
   */
  public void pwm(int channel, int on, int off) {
    if (channel < 0 || 15 < channel) {
      throw new IllegalArgumentException("Channel must be between 0 and 15");
    }
    if (on < 0 || 4095 < on) {
      throw new IllegalArgumentException("On must be between 0 and 4095");
    }
    if (off < 0 || 4095 < off) {
      throw new IllegalArgumentException("Off must be between 0 and 4095");
    }
    if (off < on) {
      throw new IllegalArgumentException("Off must be greater than On");
    }
    command(LED0_ON_L + 4 * channel, (byte) (on & 0xFF));
    command(LED0_ON_H + 4 * channel, (byte) (on >> 8));
    command(LED0_OFF_L + 4 * channel, (byte) (off & 0xFF));
    command(LED0_OFF_H + 4 * channel, (byte) (off >> 8));
  }


  private void command(int register, byte value) {
    beginTransmission(address);
    write(register);
    write(value);
    endTransmission();
  }

  private byte readU8(int register) {
    beginTransmission(address);
    write(register);
    byte[] ba = read(1);
    endTransmission();
    return (byte)(ba[0] & 0xFF);
  }
}
