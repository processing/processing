// Sine Wave
// Daniel Shiffman <http://www.shiffman.net>


// Two wave objects
Wave wave0;
Wave wave1;

void setup() {
  size(750,200);
  smooth();
  // Initialize a wave with starting point, width, amplitude, and period
  wave0 = new Wave(new PVector(50,75),100,20,500);
  wave1 = new Wave(new PVector(300,100),300,40,220);

}

void draw() {
  background(255);
  
  // Update and display waves
  wave0.calculate();
  wave0.display();
  
  wave1.calculate();
  wave1.display();


}


