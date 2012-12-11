// Recursive Tree
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

// Renders a simple tree-like structure via recursion
// Branching angle calculated as a function of horizontal mouse location

void setup() {
  size(800, 200);
  smooth();
}

void draw() {
  background(255);
  // Start the tree from the bottom of the screen
  translate(width/2, height);
  stroke(0);
  branch(60);
  noLoop();
}

void branch(float len) {
  strokeWeight(2);
      
  line(0, 0, 0, -len);
  // Move to the end of that line
  translate(0, -len);

  len *= 0.66;
  // All recursive functions must have an exit condition!!!!
  // Here, ours is when the length of the branch is 2 pixels or less
  if (len > 2) {
    pushMatrix();    // Save the current state of transformation (i.e. where are we now)
    rotate(PI/5);   // Rotate by theta
    branch(len);       // Ok, now call myself to draw two new branches!!
    popMatrix();     // Whenever we get back here, we "pop" in order to restore the previous matrix state

    // Repeat the same thing, only branch off to the "left" this time!
    pushMatrix();
    rotate(-PI/5);
    branch(len);
    popMatrix();
  }
}

