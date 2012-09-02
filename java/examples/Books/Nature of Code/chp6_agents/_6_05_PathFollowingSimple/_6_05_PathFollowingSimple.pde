// Path Following
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code

// Path is a just a straight line in this example
// Via Reynolds: // http://www.red3d.com/cwr/steer/PathFollow.html

// Using this variable to decide whether to draw all the stuff
boolean debug = true;

// A path object (series of connected points)
Path path;

// Two vehicles
Vehicle car1;
Vehicle car2;

void setup() {
  size(800, 200);
  smooth();

  path = new Path();

  // Each vehicle has different maxspeed and maxforce for demo purposes
  car1 = new Vehicle(new PVector(0, height/2), 3, 0.05);
  car2 = new Vehicle(new PVector(0, height/2), 5, 0.1);
}

void draw() {
  background(255);
  // Display the path
  path.display();
  // The boids follow the path
  car1.follow(path);
  car2.follow(path);
  // Call the generic run method (update, borders, display, etc.)
  car1.run();
  car2.run();
  
  // Instructions
  fill(0);
  text("Hit space bar to toggle debugging lines.", 10, height-30);
}

public void keyPressed() {
  if (key == ' ') {
    debug = !debug;
  }
}



