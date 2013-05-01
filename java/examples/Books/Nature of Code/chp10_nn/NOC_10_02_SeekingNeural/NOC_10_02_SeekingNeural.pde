// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A Vehicle controlled by a Perceptron

Vehicle v;

PVector desired;

ArrayList<PVector> targets;

void setup() {
  size(640, 360);
  // The Vehicle's desired location
  desired = new PVector(width/2,height/2);

  
  // Create a list of targets
  makeTargets();
  
  // Create the Vehicle (it has to know about the number of targets
  // in order to configure its brain)
  v = new Vehicle(targets.size(), random(width), random(height));
}

// Make a random ArrayList of targets to steer towards
void makeTargets() {
  targets = new ArrayList<PVector>();
  for (int i = 0; i < 8; i++) {
    targets.add(new PVector(random(width), random(height)));
  }
}

void draw() {
  background(255);

  // Draw a circle to show the Vehicle's goal
  stroke(0);
  strokeWeight(2);
  fill(0, 100);
  ellipse(desired.x, desired.y, 36, 36);

  // Draw the targets
  for (PVector target : targets) {
    noFill();
    stroke(0);
    strokeWeight(2);
    ellipse(target.x, target.y, 16, 16);
    line(target.x,target.y-16,target.x,target.y+16);
    line(target.x-16,target.y,target.x+16,target.y);
  }
  
  // Update the Vehicle
  v.steer(targets);
  v.update();
  v.display();
}

void mousePressed() {
  makeTargets();
}

