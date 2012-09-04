// Stochastic Tree
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

// Renders a simple tree-like structure via recursion
// Angles and number of branches are random

void setup() {
  size(600, 400);
  smooth();
  newTree();
}

void draw() {

}

void mousePressed() {
  newTree();
}

void newTree() {
  background(255);
  fill(0);
  text("Click mouse to generate a new tree", 10, height-20);

  stroke(0);
  // Start the tree from the bottom of the screen
  translate(width/2, height);
  // Start the recursive branching!
  branch(120);
}



void branch(float h) {
  // thickness of the branch is mapped to its length
  float sw = map(h, 2, 120, 1, 5);
  strokeWeight(sw);
  // Draw the actual branch
  line(0, 0, 0, -h);
  // Move along to end
  translate(0, -h);

  // Each branch will be 2/3rds the size of the previous one
  h *= 0.66f;

  // All recursive functions must have an exit condition!!!!
  // Here, ours is when the length of the branch is 2 pixels or less
  if (h > 2) {
    // A random number of branches
    int n = int(random(1, 4));
    for (int i = 0; i < n; i++) {
      // Picking a random angle
      float theta = random(-PI/2, PI/2);
      pushMatrix();      // Save the current state of transformation (i.e. where are we now)
      rotate(theta);     // Rotate by theta
      branch(h);         // Ok, now call myself to branch again
      popMatrix();       // Whenever we get back here, we "pop" in order to restore the previous matrix state
    }
  }
}

