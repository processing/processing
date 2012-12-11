// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Recursive Tree

// Renders a simple tree-like structure via recursion
// Branching angle calculated as a function of horizontal mouse location

float theta;   

void setup() {
  size(1800, 500);
  smooth();
}

void draw() {
  background(255);
  // Let's pick an angle 0 to 90 degrees based on the mouse position
  theta = PI/6;//map(mouseX,0,width,0,PI/2);

  // Start the tree from the bottom of the screen
  translate(width/2, height);
  stroke(0);
  branch(200,0);
  save("chapter08_exc06.png");
  noLoop();
}

void branch(float len, int level) {
  // Each branch will be 2/3rds the size of the previous one

  //float sw = map(len,2,120,1,10);
  //strokeWeight(sw);
  strokeWeight(2);
      
  line(0, 0, 0, -len);
  // Move to the end of that line
  translate(0, -len);

  len *= 0.66;
  level++;
  // All recursive functions must have an exit condition!!!!
  // Here, ours is when the length of the branch is 2 pixels or less
  if (level < 5) {
    pushMatrix();    // Save the current state of transformation (i.e. where are we now)
    rotate(theta);   // Rotate by theta
    branch(len,level);       // Ok, now call myself to draw two new branches!!
    popMatrix();     // Whenever we get back here, we "pop" in order to restore the previous matrix state

    // Repeat the same thing, only branch off to the "left" this time!
    pushMatrix();
    rotate(-theta);
    branch(len,level);
    popMatrix();
  }
}

