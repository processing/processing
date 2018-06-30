import processing.io.*;
BME280 bme280;

// see setup.png in the sketch folder for wiring details

void setup() {
  size(720, 320);
  textSize(72);

  //printArray(I2C.list());
  bme280 = new BME280("i2c-1", 0x77);
}

void draw() {
  background(0);
  stroke(255);

  bme280.update();
  float temp = bme280.temperature();
  float hum = bme280.humidity();
  float press = bme280.pressure();
  text(String.format("Temp:  %.02f\272C", temp), 10, 75);
  text(String.format("Hum:   %.02f %%", hum), 10, 150);
  text(String.format("Press: %.02f hPa", press / 100f), 10, 225);

  // pressure can be used to calculate the altitude like so
  float alt = bme280.altitude(press, temp);
  text(String.format("Alt: %.02f m", alt), 10, 300);
}
