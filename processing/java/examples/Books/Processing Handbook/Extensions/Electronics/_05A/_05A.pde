// Read data from the serial and turns a DC motor on or off according to the value
char val; // Data received from the serial port
int motorpin = 0; // Wiring: Connect L293D Pin En1 connected to Pin PWM 0
// int motorpin = 9; // Arduino: Connect L293D Pin En1 to Pin PWM 9

void setup() {
  Serial.begin(9600); // Start serial communication at 9600 bps
}

void loop() {
  if (Serial.available()) { // If data is available,
    val = Serial.read(); // read it and store it in val
  }
  if (val == 'H') { // If 'H' was received,
    analogWrite(motorpin, 125); // turn the motor on at medium speed
  } else { // If 'H' was not received
    analogWrite(motorpin, 0); // turn the motor off
  }
  delay(100); // Wait 100 milliseconds for next reading
}
