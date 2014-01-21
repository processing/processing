void setup() {
  Serial.begin(115200);
}

void loop() {
  while (true) {
    int in = Serial.read();
    if (in != -1) {
      Serial.write(in);
    }
  }
}
