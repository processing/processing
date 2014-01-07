void setup() {
  Serial.begin(115200);
}

void loop() {
  while (true) {
    Serial.write('.');
  }
}
