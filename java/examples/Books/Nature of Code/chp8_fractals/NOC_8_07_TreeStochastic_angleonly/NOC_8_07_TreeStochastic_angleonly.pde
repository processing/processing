// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Stochastic Tree
// Renders a simple tree-like structure via recursion
// Angles and number of branches are random

void setup() {
  size(800, 200);
  newTree();
}

void draw() {
  noLoop();
}

void mousePressed() {
  pushMatrix();
  newTree();
  popMatrix();
  redraw();
}

void newTree() {
  background(255);
  fill(0);
  text("Click mouse to generate a new tree", 10, height-10);

  stroke(0);
  // Start the tree from the bottom of the screen
  translate(width/2, height);
  // Start the recursive branching!
  branch(60);
}



void branch(float h) {
  // thickness of the branch is mapped to its length
  float sw = map(h, 2, 120, 1, 5);
  strokeWeight(sw);
   float theta = random(0,PI/3);
 
  line(0, 0, 0, -h);
  translate(0, -h);
  h *= 0.66;
  if (h > 2) {
    pushMatrix();    
    rotate(theta);   
    branch(h);
    popMatrix();     
    pushMatrix();
    rotate(-theta);
    branch(h);
    popMatrix();
  }
}

