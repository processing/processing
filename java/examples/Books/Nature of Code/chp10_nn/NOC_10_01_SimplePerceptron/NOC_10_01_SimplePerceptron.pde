// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Simple Perceptron Example
// See: http://en.wikipedia.org/wiki/Perceptron

// Code based on text "Artificial Intelligence", George Luger

// A list of points we will use to "train" the perceptron
Trainer[] training = new Trainer[2000];
// A Perceptron object
Perceptron ptron;

// We will train the perceptron with one "Point" object at a time
int count = 0;

// Coordinate space
float xmin = -400;
float ymin = -100;
float xmax =  400;
float ymax =  100;

// The function to describe a line 
float f(float x) {
  return 0.4*x+1;
}

void setup() {
  size(640, 360);

  // The perceptron has 3 inputs -- x, y, and bias
  // Second value is "Learning Constant"
  ptron = new Perceptron(3, 0.00001);  // Learning Constant is low just b/c it's fun to watch, this is not necessarily optimal

  // Create a random set of training points and calculate the "known" answer
  for (int i = 0; i < training.length; i++) {
    float x = random(xmin, xmax);
    float y = random(ymin, ymax);
    int answer = 1;
    if (y < f(x)) answer = -1;
    training[i] = new Trainer(x, y, answer);
  }
  smooth();
}


void draw() {
  background(255);
  translate(width/2,height/2);

  // Draw the line
  strokeWeight(4);
  stroke(127);
  float x1 = xmin;
  float y1 = f(x1);
  float x2 = xmax;
  float y2 = f(x2);
  line(x1,y1,x2,y2);

  // Draw the line based on the current weights
  // Formula is weights[0]*x + weights[1]*y + weights[2] = 0
  stroke(0);
  strokeWeight(1);
  float[] weights = ptron.getWeights();
  x1 = xmin;
  y1 = (-weights[2] - weights[0]*x1)/weights[1];
  x2 = xmax;
  y2 = (-weights[2] - weights[0]*x2)/weights[1];
  line(x1,y1,x2,y2);



  // Train the Perceptron with one "training" point at a time
  ptron.train(training[count].inputs, training[count].answer);
  count = (count + 1) % training.length;

  // Draw all the points based on what the Perceptron would "guess"
  // Does not use the "known" correct answer
  for (int i = 0; i < count; i++) {
    stroke(0);
    strokeWeight(1);
    fill(0);
    int guess = ptron.feedforward(training[i].inputs);
    if (guess > 0) noFill();

    ellipse(training[i].inputs[0], training[i].inputs[1], 8, 8);
  }
}

