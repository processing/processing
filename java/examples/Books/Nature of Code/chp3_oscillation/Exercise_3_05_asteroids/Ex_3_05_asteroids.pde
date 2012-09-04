// Nature of Code 2011
// Daniel Shiffman
// Chapter 3: Asteroids exercise
// http://www.shiffman.net

// Mover object
Spaceship ship;

void setup() {
  size(750, 200);
  smooth();
  ship = new Spaceship();
}

void draw() {
  background(255); 
  
  // Update location
  ship.update();
  // Wrape edges
  ship.wrapEdges();
  // Draw ship
  ship.display();
   

  fill(0);
  text("left right arrows to turn, z to thrust",10,height-5);

  // Turn or thrust the ship depending on what key is pressed
  if (keyPressed) {
    if (key == CODED && keyCode == LEFT) {
      ship.turn(-0.03);
    } else if (key == CODED && keyCode == RIGHT) {
      ship.turn(0.03);
    } else if (key == 'z' || key == 'Z') {
      ship.thrust(); 
    }
  }
}


