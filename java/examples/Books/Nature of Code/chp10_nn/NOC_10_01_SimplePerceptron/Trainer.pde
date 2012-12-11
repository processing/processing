// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Simple Perceptron Example
// See: http://en.wikipedia.org/wiki/Perceptron

// A class to describe a training point
// Has an x and y, a "bias" (1) and known output
// Could also add a variable for "guess" but not required here

class Trainer {
  
  float[] inputs;
  int answer; 
  
  Trainer(float x, float y, int a) {
    inputs = new float[3];
    inputs[0] = x;
    inputs[1] = y;
    inputs[2] = 1;
    answer = a;
  }
}
