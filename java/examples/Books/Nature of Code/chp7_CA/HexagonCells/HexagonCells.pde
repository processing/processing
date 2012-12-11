// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Outline for game of life 
// This is just a grid of hexagons right now

GOL gol;

void setup() {
  size(600, 600);
  gol = new GOL();
}

void draw() {
  background(255);
  gol.display();
}

// reset board when mouse is pressed
void mousePressed() {
  gol.init();
}

