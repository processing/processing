// Daniel Shiffman
// The Nature of Code
// http://natureofcode.com

// Simple Perceptron Example
// See: http://en.wikipedia.org/wiki/Perceptron

// Perceptron Class

class Perceptron {
  float[] weights;  // Array of weights for inputs
  float c;          // learning constant

  // Perceptron is created with n weights and learning constant
  Perceptron(int n, float c_) {
    weights = new float[n];
    // Start with random weights
    for (int i = 0; i < weights.length; i++) {
      weights[i] = random(-1,1); 
    }
    c = c_;
  }

  // Function to train the Perceptron
  // Weights are adjusted based on "desired" answer
  void train(float[] inputs, int desired) {
    // Guess the result
    int guess = feedforward(inputs);
    // Compute the factor for changing the weight based on the error
    // Error = desired output - guessed output
    // Note this can only be 0, -2, or 2
    // Multiply by learning constant
    float error = desired - guess;
    // Adjust weights based on weightChange * input
    for (int i = 0; i < weights.length; i++) {
      weights[i] += c * error * inputs[i];         
    }
  }

  // Guess -1 or 1 based on input values
  int feedforward(float[] inputs) {
    // Sum all values
    float sum = 0;
    for (int i = 0; i < weights.length; i++) {
      sum += inputs[i]*weights[i];
    }
    // Result is sign of the sum, -1 or 1
    return activate(sum);
  }
  
  int activate(float sum) {
    if (sum > 0) return 1;
    else return -1; 
  }
  
  // Return weights
  float[] getWeights() {
    return weights; 
  }
}
