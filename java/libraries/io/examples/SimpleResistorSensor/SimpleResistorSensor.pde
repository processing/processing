import processing.io.*;

// using a capacitor that gets charged and discharged, while
// measuring the time it takes, is an inexpensive way to
// read the value of an (analog) resistive sensor, such as
// a photocell
// kudos to ladyada for the original tutorial

// see setup.png in the sketch folder for wiring details

int max = 0;
int min = 9999;

void setup() {
}

void draw() {
  int val = sensorRead(4);
  println(val);

  // track largest and smallest reading, to get a sense
  // how we compare
  if (max < val) {
    max = val;
  }
  if (val < min) {
    min = val;
  }

  // convert current reading into a number between 0.0 and 1.0
  float frac = map(val, min, max, 0.0, 1.0);

  background(255 * frac);
}

int sensorRead(int pin) {
  // discharge the capacitor
  GPIO.pinMode(pin, GPIO.OUTPUT);
  GPIO.digitalWrite(pin, GPIO.LOW);
  delay(100);
  // now the capacitor should be empty

  // measure the time takes to fill it
  // up to ~ 1.4V again
  GPIO.pinMode(pin, GPIO.INPUT);
  int start = millis();
  while (GPIO.digitalRead(pin) == GPIO.LOW) {
    // wait
  }

  // return the time elapsed
  // this will vary based on the value of the
  // resistive sensor (lower resistance will
  // make the capacitor charge faster)
  return millis() - start;
}
