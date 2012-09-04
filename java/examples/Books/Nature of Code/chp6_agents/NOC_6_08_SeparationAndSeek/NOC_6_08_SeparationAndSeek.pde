// Separation
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code, 2011

// Via Reynolds: http://www.red3d.com/cwr/steer/

// A list of vehicles
ArrayList<Vehicle> vehicles;

void setup() {
  size(800,200);
  smooth();

  // We are now making random vehicles and storing them in an ArrayList
  vehicles = new ArrayList<Vehicle>();
  for (int i = 0; i < 100; i++) {
    vehicles.add(new Vehicle(random(width),random(height)));
  }
}

void draw() {
  background(255);

  for (Vehicle v : vehicles) {
    // Path following and separation are worked on in this function
    v.applyBehaviors(vehicles);
    // Call the generic run method (update, borders, display, etc.)
    v.update();
    v.display();
  }

  // Instructions
  fill(0);
  text("Drag the mouse to generate new vehicles.",10,height-16);
}


void mouseDragged() {
  vehicles.add(new Vehicle(mouseX,mouseY));
}



