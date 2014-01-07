// Arduino Duemilanove (168) on OS X 10.9
// with either 115200 or 38400 bps
// on Processing 2.0.3 (cu & tty): 24 ms avg, 35 ms max
// on Processing 2.1b1 (cu & tty): 18 ms avg, 35 ms max

import processing.serial.*;
Serial serial;
int start;
byte out = '@';
int last_send = 0;
byte[] in = new byte[32768];
long num_fail = 0;
long num_recv = 0;
int max_latency = 0;

void setup() {  
  println(serial.list());
  // change this accordingly
  serial = new Serial(this, serial.list()[0], 115200);
  start = millis();
}

void draw() {
  background(255);
  if (0 < serial.available()) {
    int recv = serial.readBytes(in);
    for (int i=0; i < recv; i++) {
      if (in[i] == out) {
        num_recv++;
        int now = millis();
        if (max_latency < now-last_send) {
          max_latency = now-last_send;
        }
        last_send = 0;
      }
    }
  }
  if (last_send != 0 && 1000 < millis()-last_send) {
    num_fail++;
    last_send = 0;
    println(num_fail+" bytes timed out");
  }
  if (last_send == 0) {
    if (out == 'Z') {
      out = '@';
    }
    serial.write(++out);
    last_send = millis();
  }
  fill(0);
  text(((millis()-start)/(float)num_recv+" ms avg"), 0, height/2);
  text(max_latency+" ms max", 0, height/2+20);
}
