// Code for sensing a switch status and writing the value to the serial port
int switchPin = 0; // Switch connected to pin 0

void setup() {
  pinMode(switchPin, INPUT); // Set pin 0 as an input
  Serial.begin(9600); // Start serial communication at 9600 bps
}

void loop() {
  if (digitalRead(switchPin) == HIGH) { // If switch is ON,
    Serial.print(1, BYTE); // send 1 to Processing
  } else { // If the switch is not ON,
    Serial.print(0, BYTE); // send 0 to Processing
  }
  delay(100); // Wait 100 milliseconds
}
