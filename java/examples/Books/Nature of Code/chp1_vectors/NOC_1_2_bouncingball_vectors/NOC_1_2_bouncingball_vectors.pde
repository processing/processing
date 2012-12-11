// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Example 1-2: Bouncing Ball, with PVector!
PVector location;
PVector velocity;

void setup() {
  size(200,200);
  background(255);
  location = new PVector(100,100);
  velocity = new PVector(2.5,5);
}

void draw() {
  noStroke();
  fill(255,10);
  rect(0,0,width,height);
  
  // Add the current speed to the location.
  location.add(velocity);

  if ((location.x > width) || (location.x < 0)) {
    velocity.x = velocity.x * -1;
  }
  if ((location.y > height) || (location.y < 0)) {
    velocity.y = velocity.y * -1;
  }

  // Display circle at x location
  stroke(0);
  fill(175);
  ellipse(location.x,location.y,16,16);
}


