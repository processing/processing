import processing.io.I2C;

// BME280 is an integrated environmental sensor
// It can measure temperature, pressure and humidity
// datasheet: https://cdn-shop.adafruit.com/datasheets/BST-BME280_DS001-10.pdf
// code contributed by @OlivierLD

public class BME280 extends I2C {

  public final static int BME280_I2CADDR = 0x77; // this is the default I2C address
  public final static int DEFAULT_ADDR = BME280_I2CADDR;

  // Operating Modes
  public final static int BME280_OSAMPLE_1  = 1;
  public final static int BME280_OSAMPLE_2  = 2;
  public final static int BME280_OSAMPLE_4  = 3;
  public final static int BME280_OSAMPLE_8  = 4;
  public final static int BME280_OSAMPLE_16 = 5;

  // BME280 Registers
  public final static int BME280_REGISTER_DIG_T1 = 0x88; // Trimming parameter registers
  public final static int BME280_REGISTER_DIG_T2 = 0x8A;
  public final static int BME280_REGISTER_DIG_T3 = 0x8C;

  public final static int BME280_REGISTER_DIG_P1 = 0x8E;
  public final static int BME280_REGISTER_DIG_P2 = 0x90;
  public final static int BME280_REGISTER_DIG_P3 = 0x92;
  public final static int BME280_REGISTER_DIG_P4 = 0x94;
  public final static int BME280_REGISTER_DIG_P5 = 0x96;
  public final static int BME280_REGISTER_DIG_P6 = 0x98;
  public final static int BME280_REGISTER_DIG_P7 = 0x9A;
  public final static int BME280_REGISTER_DIG_P8 = 0x9C;
  public final static int BME280_REGISTER_DIG_P9 = 0x9E;

  public final static int BME280_REGISTER_DIG_H1 = 0xA1;
  public final static int BME280_REGISTER_DIG_H2 = 0xE1;
  public final static int BME280_REGISTER_DIG_H3 = 0xE3;
  public final static int BME280_REGISTER_DIG_H4 = 0xE4;
  public final static int BME280_REGISTER_DIG_H5 = 0xE5;
  public final static int BME280_REGISTER_DIG_H6 = 0xE6;
  public final static int BME280_REGISTER_DIG_H7 = 0xE7;

  public final static int BME280_REGISTER_CHIPID = 0xD0;
  public final static int BME280_REGISTER_VERSION = 0xD1;
  public final static int BME280_REGISTER_SOFTRESET = 0xE0;

  public final static int BME280_REGISTER_CONTROL_HUM = 0xF2;
  public final static int BME280_REGISTER_CONTROL = 0xF4;
  public final static int BME280_REGISTER_CONFIG = 0xF5;
  public final static int BME280_REGISTER_PRESSURE_DATA = 0xF7;
  public final static int BME280_REGISTER_TEMP_DATA = 0xFA;
  public final static int BME280_REGISTER_HUMIDITY_DATA = 0xFD;

  private int dig_T1 = 0;
  private int dig_T2 = 0;
  private int dig_T3 = 0;

  private int dig_P1 = 0;
  private int dig_P2 = 0;
  private int dig_P3 = 0;
  private int dig_P4 = 0;
  private int dig_P5 = 0;
  private int dig_P6 = 0;
  private int dig_P7 = 0;
  private int dig_P8 = 0;
  private int dig_P9 = 0;

  private int dig_H1 = 0;
  private int dig_H2 = 0;
  private int dig_H3 = 0;
  private int dig_H4 = 0;
  private int dig_H5 = 0;
  private int dig_H6 = 0;

  private float tFine = 0.0f;

  private int address;
  private int mode = BME280_OSAMPLE_8;
  private float standardSeaLevelPressure = 101325.0f; // in Pa (1013.25 hPa)

  protected float temp = 0.0f; // most recent sensor readings, set by update()
  protected float press = 0.0f;
  protected float hum = 0.0f;


  public BME280(String dev) {
    this(dev, DEFAULT_ADDR);
  }

  public BME280(String dev, int address) {
    super(dev);
    this.address = address;

    // Soft reset
    command(BME280_REGISTER_SOFTRESET, (byte)0xB6);
    // Wait for the chip to wake up
    delay(300);

    try {
      readCalibrationData();
      // showCalibrationData();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    command(BME280_REGISTER_CONTROL, (byte)0x3F);
    tFine = 0.0f;
  }


  /**
   * Read and update all sensors values
   */
  public void update() {
    // The order used to read the data is important!
    // 1.temperature, 2.pressure (analog to altitude), 3.humidity.

    try {
      temp = readTemperature();
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      ex.printStackTrace();
    }

    try {
      press = readPressure();
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      ex.printStackTrace();
    }

    try {
      hum = readHumidity();
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      ex.printStackTrace();
    }
  }

  /**
   * Returns the temperature in degrees celsius
   */
  public float temperature() {
    return temp;
  }

  /**
   * Returns the pressure in Pa
   */
  public float pressure() {
    return press;
  }

  /**
   * Returns the altitude in meters
   * @param pressure as returned by pressure()
   */
  public float altitude(float pressure) {
    double altitude = 0.0;
    if (standardSeaLevelPressure != 0) {
      altitude = 44330.0 * (1.0 - Math.pow(pressure / standardSeaLevelPressure, 0.1903));
    }
    return (float)altitude;
  }

  /**
   * Returns the altitude in meters
   * @param pressure as returned by pressure() in Pa
   * @param temperature as returned by temperature() in Celcius
   */
  public float altitude(float pressure, float temperature) {
    double altitude = 0.0;
    if (standardSeaLevelPressure != 0) {
      altitude = ((Math.pow(standardSeaLevelPressure / pressure, 1 / 5.257)  - 1) * (temperature + 273.25)) / 0.0065;
    }
    return (float)altitude;
  }

  /**
   * Returns the humidity in percent
   */
  public float humidity() {
    return hum;
  }

  /**
   * Set the standard sea level pressure used for calculating altitude()
   * Defaults to 101325 Pa (1013.25 hPa)
   */
  public void setStandardSeaLevelPressure(float pressure) {
    standardSeaLevelPressure = pressure;
  }


  protected float readTemperature() {
    // Returns the compensated temperature in degrees celcius
    float UT = readRawTemp();
    float var1 = 0.0f;
    float var2 = 0.0f;
    float temp = 0.0f;

    // Read raw temp before aligning it with the calibration values
    var1 = (UT / 16384.0f - dig_T1 / 1024.0f) * (float) dig_T2;
    var2 = ((UT / 131072.0f - dig_T1 / 8192.0f) * (UT / 131072.0f - dig_T1 / 8192.0f)) * (float) dig_T3;
    tFine = (int) (var1 + var2);
    temp = (var1 + var2) / 5120.0f;
    // println("DBG: Calibrated temperature = " + temp + " C");
    return temp;
  }

  protected float readPressure() {
    // Returns the compensated pressure in Pascal
    int adc = readRawPressure();
    // println("ADC:" + adc + ", tFine:" + tFine);
    float var1 = (tFine / 2.0f) - 64000.0f;
    float var2 = var1 * var1 * (dig_P6 / 32768.0f);
    var2 = var2 + var1 * dig_P5 * 2.0f;
    var2 = (var2 / 4.0f) + (dig_P4 * 65536.0f);
    var1 = (dig_P3 * var1 * var1 / 524288.0f + dig_P2 * var1) / 524288.0f;
    var1 = (1.0f + var1 / 32768.0f) * dig_P1;
    if (var1 == 0f) {
      return 0.0f;
    }
    float p = 1048576.0f - adc;
    p = ((p - var2 / 4096.0f) * 6250.0f) / var1;
    var1 = dig_P9 * p * p / 2147483648.0f;
    var2 = p * dig_P8 / 32768.0f;
    p = p + (var1 + var2 + dig_P7) / 16.0f;
    // println("DBG: Pressure = " + p + " Pa");
    return p;
  }

  protected float readHumidity() {
    // Returns the compensated humidity in percent
    int adc = readRawHumidity();
    float h = tFine - 76800.0f;
    h = (adc - (dig_H4 * 64.0f + dig_H5 / 16384.8f * h)) *
        (dig_H2 / 65536.0f * (1.0f + dig_H6 / 67108864.0f * h * (1.0f + dig_H3 / 67108864.0f * h)));
    h = h * (1.0f - dig_H1 * h / 524288.0f);
    if (h > 100) {
      h = 100;
    } else if (h < 0) {
      h = 0;
    }
    // println("DBG: Humidity = " + h);
    return h;
  }


  private void readCalibrationData() {
    // Reads the calibration data from the IC
    dig_T1 = readU16LE(BME280_REGISTER_DIG_T1);
    dig_T2 = readS16LE(BME280_REGISTER_DIG_T2);
    dig_T3 = readS16LE(BME280_REGISTER_DIG_T3);

    dig_P1 = readU16LE(BME280_REGISTER_DIG_P1);
    dig_P2 = readS16LE(BME280_REGISTER_DIG_P2);
    dig_P3 = readS16LE(BME280_REGISTER_DIG_P3);
    dig_P4 = readS16LE(BME280_REGISTER_DIG_P4);
    dig_P5 = readS16LE(BME280_REGISTER_DIG_P5);
    dig_P6 = readS16LE(BME280_REGISTER_DIG_P6);
    dig_P7 = readS16LE(BME280_REGISTER_DIG_P7);
    dig_P8 = readS16LE(BME280_REGISTER_DIG_P8);
    dig_P9 = readS16LE(BME280_REGISTER_DIG_P9);

    dig_H1 = readU8(BME280_REGISTER_DIG_H1);
    dig_H2 = readS16LE(BME280_REGISTER_DIG_H2);
    dig_H3 = readU8(BME280_REGISTER_DIG_H3);
    dig_H6 = readS8(BME280_REGISTER_DIG_H7);

    int h4 = readS8(BME280_REGISTER_DIG_H4);
    h4 = (h4 << 24) >> 20;
    dig_H4 = h4 | (readU8(BME280_REGISTER_DIG_H5) & 0x0F);

    int h5 = readS8(BME280_REGISTER_DIG_H6);
    h5 = (h5 << 24) >> 20;
    dig_H5 = h5 | (readU8(BME280_REGISTER_DIG_H5) >> 4 & 0x0F);
  }

  private String displayRegister(int reg) {
    return String.format("0x%s (%d)", lpad(Integer.toHexString(reg & 0xFFFF).toUpperCase(), 4, "0"), reg);
  }

  private void showCalibrationData() {
    // Displays the calibration values for debugging purposes
    println("======================");
    println("DBG: T1 = " + displayRegister(dig_T1));
    println("DBG: T2 = " + displayRegister(dig_T2));
    println("DBG: T3 = " + displayRegister(dig_T3));
    println("----------------------");
    println("DBG: P1 = " + displayRegister(dig_P1));
    println("DBG: P2 = " + displayRegister(dig_P2));
    println("DBG: P3 = " + displayRegister(dig_P3));
    println("DBG: P4 = " + displayRegister(dig_P4));
    println("DBG: P5 = " + displayRegister(dig_P5));
    println("DBG: P6 = " + displayRegister(dig_P6));
    println("DBG: P7 = " + displayRegister(dig_P7));
    println("DBG: P8 = " + displayRegister(dig_P8));
    println("DBG: P9 = " + displayRegister(dig_P9));
    println("----------------------");
    println("DBG: H1 = " + displayRegister(dig_H1));
    println("DBG: H2 = " + displayRegister(dig_H2));
    println("DBG: H3 = " + displayRegister(dig_H3));
    println("DBG: H4 = " + displayRegister(dig_H4));
    println("DBG: H5 = " + displayRegister(dig_H5));
    println("DBG: H6 = " + displayRegister(dig_H6));
    println("======================");
  }

  private void command(int reg, byte val) {
    super.beginTransmission(address);
    super.write(reg);
    super.write(val);
    super.endTransmission();
  }

  private int readRawTemp() {
    // Returns the raw (uncompensated) temperature
    int meas = mode;
    // println(String.format("readRawTemp: 1 - meas=%d", meas));
    command(BME280_REGISTER_CONTROL_HUM, (byte) meas); // HUM ?
    meas = mode << 5 | mode << 2 | 1;
    // println(String.format("readRawTemp: 2 - meas=%d", meas));
    command(BME280_REGISTER_CONTROL, (byte) meas);

    double sleepTime = 0.00125 + 0.0023 * (1 << mode);
    sleepTime = sleepTime + 0.0023 * (1 << mode) + 0.000575;
    sleepTime = sleepTime + 0.0023 * (1 << mode) + 0.000575;
    delay((int)Math.round(sleepTime * 1000));
    int msb = readU8(BME280_REGISTER_TEMP_DATA);
    int lsb = readU8(BME280_REGISTER_TEMP_DATA + 1);
    int xlsb = readU8(BME280_REGISTER_TEMP_DATA + 2);
    int raw = ((msb << 16) | (lsb << 8) | xlsb) >> 4;
    // println("DBG: Raw Temp: " + (raw & 0xFFFF) + ", " + raw + String.format(", msb: 0x%04X lsb: 0x%04X xlsb: 0x%04X", msb, lsb, xlsb));
    return raw;
  }

  private int readRawPressure() {
    // Returns the raw (uncompensated) pressure
    int msb = readU8(BME280_REGISTER_PRESSURE_DATA);
    int lsb = readU8(BME280_REGISTER_PRESSURE_DATA + 1);
    int xlsb = readU8(BME280_REGISTER_PRESSURE_DATA + 2);
    int raw = ((msb << 16) | (lsb << 8) | xlsb) >> 4;
    // println("DBG: Raw Press: " + (raw & 0xFFFF) + ", " + raw + String.format(", msb: 0x%04X lsb: 0x%04X xlsb: 0x%04X", msb, lsb, xlsb));
    return raw;
  }

  private int readRawHumidity() {
    // Returns the raw (uncompensated) humidity
    int msb = readU8(BME280_REGISTER_HUMIDITY_DATA);
    int lsb = readU8(BME280_REGISTER_HUMIDITY_DATA + 1);
    int raw = (msb << 8) | lsb;
    return raw;
  }

  private int readU16LE(int register) {
    super.beginTransmission(address);
    super.write((byte)register);
    byte[] ba = super.read(2);
    super.endTransmission();
    return ((ba[1] & 0xFF) << 8) + (ba[0] & 0xFF); // Little Endian
  }

  private int readS16LE(int register) {
    super.beginTransmission(address);
    super.write((byte)register);
    byte[] ba = super.read(2);
    super.endTransmission();

    int lo = ba[0] & 0xFF;
    int hi = ba[1] & 0xFF;
    if (hi > 127)
      hi -= 256;
    return (hi << 8) + lo; // Little Endian
  }

  private int readU8(int register) {
    super.beginTransmission(address);
    super.write(register);
    byte[] ba = super.read(1);
    super.endTransmission();
    return (int)(ba[0] & 0xFF);
  }

  private int readS8(int register) {
    int val = readU8(register);
    if (val > 127)
      val -= 256;
    return val;
  }

  private String rpad(String s, int len, String pad) {
    String str = s;
    while (str.length() < len) {
      str += pad;
    }
    return str;
  }

  private String lpad(String s, int len, String pad) {
    String str = s;
    while (str.length() < len) {
      str = pad + str;
    }
    return str;
  }
}
