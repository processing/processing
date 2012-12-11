// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// An array of objects
Oscillator[] oscillators = new Oscillator[10];

void setup()  {   
  size(800,200);  
  smooth();  
  // Initialize all objects
  for (int i = 0; i < oscillators.length; i++) {
    oscillators[i] = new Oscillator();
  }
  background(255);  
}   

void draw() {     
  background(255);  
  // Run all objects
  for (int i = 0; i < oscillators.length; i++) {
    oscillators[i].oscillate();
    oscillators[i].display();
  }
}   




