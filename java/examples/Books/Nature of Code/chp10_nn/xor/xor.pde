// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// XOR Multi-Layered Neural Network Example
// Neural network code is all in the "code" folder

import nn.*;

ArrayList inputs;  // List of training input values
Network nn;        // Neural Network Object
int count;         // Total training interations
Landscape land;    // Solution space
float theta = 0.0; // Angle of rotation
PFont f;           // Font


void setup() {

  size(400,400,P3D);

  // Create a landscape object
  land = new Landscape(20,300,300);

  f = createFont("Courier",12,true);

  nn = new Network(2,4);

  // Create a list of 4 training inputs
  inputs = new ArrayList();
  float[] input = new float[2];
  input[0] = 1; 
  input[1] = 0;  
  inputs.add((float []) input.clone());
  input[0] = 0; 
  input[1] = 1;  
  inputs.add((float []) input.clone());
  input[0] = 1; 
  input[1] = 1;  
  inputs.add((float []) input.clone());
  input[0] = 0; 
  input[1] = 0;  
  inputs.add((float []) input.clone());
}

void draw() {

  int trainingIterationsPerFrame = 5;

  for (int i = 0; i < trainingIterationsPerFrame; i++) {
    // Pick a random training input
    int pick = int(random(inputs.size()));
    // Grab that input
    float[] inp = (float[]) inputs.get(pick); 
    // Compute XOR
    float known = 1;
    if ((inp[0] == 1.0 && inp[1] == 1.0) || (inp[0] == 0 && inp[1] == 0)) known = 0;
    // Train that sucker!
    float result = nn.train(inp,known);
    count++;
  }

  // Ok, visualize the solution space
  background(175);
  pushMatrix();
  translate(width/2,height/2+20,-160);
  rotateX(PI/3);
  rotateZ(theta);

  // Put a little BOX on screen
  pushMatrix();
  stroke(50);
  noFill();
  translate(-10,-10,0);
  box(280);

  // Draw the landscape
  popMatrix();
  land.calculate(nn);
  land.render(); 
  theta += 0.0025;
  popMatrix();

  // Display overal neural net stats
  networkStatus();

}


void networkStatus() {
  float mse = 0.0;

  textFont(f);
  fill(0);
  text("Your friendly neighborhood neural network solving XOR.",10,20);
  text("Total iterations: " + count,10,40);

  for (int i = 0; i < inputs.size(); i++) {
    float[] inp = (float[]) inputs.get(i); 
    float known = 1;
    if ((inp[0] == 1.0 && inp[1] == 1.0) || (inp[0] == 0 && inp[1] == 0)) known = 0;
    float result = nn.feedForward(inp);
    //System.out.println("For: " + inp[0] + " " + inp[1] + ":  " + result);
    mse += (result - known)*(result - known);
  }

  float rmse = sqrt(mse/4.0);
  text("Root mean squared error: " + nf(rmse,1,5), 10,60);

}


