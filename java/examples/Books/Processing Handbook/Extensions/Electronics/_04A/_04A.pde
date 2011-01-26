// Read data from the serial port and set the position of a servomotor
// according to the value
Servo myservo; // Create servo object to control a servo

int servoPin = 0; // Connect yellow servo wire to digital I/O pin 0
int val = 0; // Data received from the serial port

void setup() {
  myservo.attach(servoPin); // Attach the servo to the PWM pin
  Serial.begin(9600); // Start serial communication at 9600 bps
}

void loop() {
  if (Serial.available()) { // If data is available to read,
    val = Serial.read(); // read it and store it in val
  }
  myservo.write(val); // Set the servo position
  delay(15); // Wait for the servo to get there
}
