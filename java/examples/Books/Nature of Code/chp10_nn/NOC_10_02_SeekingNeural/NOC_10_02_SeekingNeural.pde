// A Vehicle controlled by a Perceptron
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Spring 2011

Vehicle v;

PVector desired;

ArrayList<PVector> targets;

void setup() {
  size(800, 200);
  smooth();
  
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

  // Draw a rectangle to show the Vehicle's goal
  rectMode(CENTER);
  stroke(0);
  strokeWeight(2);
  fill(0, 100);
  rect(desired.x, desired.y, 36, 36);

  // Draw the targets
  for (PVector target : targets) {
    fill(0, 100);
    stroke(0);
    strokeWeight(2);
    ellipse(target.x, target.y, 30, 30);
  }
  
  // Update the Vehicle
  v.steer(targets);
  v.update();
  v.display();
}

void mousePressed() {
  makeTargets();
}

