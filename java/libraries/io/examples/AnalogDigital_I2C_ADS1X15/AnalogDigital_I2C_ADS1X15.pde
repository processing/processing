import processing.io.*;
ADS1015 adc;
// or, alternatively:
// ADS1115 adc;

// see setup.png in the sketch folder for wiring details

void setup() {
  //printArray(I2C.list());

  adc = new ADS1015("i2c-1", 0x48);
  //adc = new ADS1115("i2c-1", 0x48);

  // this sets the measuring range to +/- 4.096 Volts
  // other ranges supported by this chip:
  // INTERNAL_6V144, INTERNAL_2V048, INTERNAL_1V024,
  // INTERNAL_0V512, INTERNAL_0V256
  adc.analogReference(ADS1X15.INTERNAL_4V096);

  // Important: do not attempt to measure voltages higher than
  // the supply voltage (VCC) + 0.3V, meaning that 3.6V is the
  // absolut maximum voltage on the Raspberry Pi. This is
  // irrespective of the analogReference() setting above.
}

void draw() {
  // this will return a number between 0 and 1
  // (as long as your voltage is positive)
  float measured = adc.analogRead(0);

  // multiply with the selected range to get the absolut voltage
  float volts = measured * 4.096;
  println("Analog Input 0 is " + volts + "V");

  background(255);
  fill(measured * 255);
  ellipse(width/2, height/2, width * 0.75, width * 0.75);
}
