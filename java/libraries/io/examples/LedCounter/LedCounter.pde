import processing.io.*;
LED leds[];

// the Raspberry Pi has two build-in LEDs we can control
// led0 (green) and led1 (red)

void setup() {
  String available[] = LED.list();
  print("Available: ");
  println(available);

  // create an object for each LED and store it in an array
  leds = new LED[available.length];
  for (int i=0; i < available.length; i++) {
    leds[i] = new LED(available[i]);
  }

  frameRate(1);
}

void draw() {
  // make the LEDs count in binary
  for (int i=0; i < leds.length; i++) {
    if ((frameCount & (1 << i)) != 0) {
      leds[i].brightness(1.0);
    } else {
      leds[i].brightness(0.0);
    }
  }
  println(frameCount);
}

void keyPressed() {
  // cleanup
  for (int i=0; i < leds.length; i++) {
    leds[i].close();
  }
  exit();
}
