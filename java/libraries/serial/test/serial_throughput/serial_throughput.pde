import processing.serial.*;
Serial serial;
int start;
byte[] in = new byte[32768];
long num_ok = 0;
long num_fail = 0;
long num_recv = 0;

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
      if (in[i] == '.') {
        num_ok++;
      } else {
        num_fail++;
        println("Received "+num_fail+" unexpected bytes");
      }
      num_recv++;
    }
  }
  fill(0);
  text(num_recv/((millis()-start)/1000.0), 0, height/2);
}
