// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Sine Wave

// Two wave objects
Wave wave0;
Wave wave1;

void setup() {
  size(640,360);
  // Initialize a wave with starting point, width, amplitude, and period
  wave0 = new Wave(new PVector(200,75),100,20,500);
  wave1 = new Wave(new PVector(150,250),300,40,220);

}

void draw() {
  background(255);
  
  // Update and display waves
  wave0.calculate();
  wave0.display();
  
  wave1.calculate();
  wave1.display();


}


