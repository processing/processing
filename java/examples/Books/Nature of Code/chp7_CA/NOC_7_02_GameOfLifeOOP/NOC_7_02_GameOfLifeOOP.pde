// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A basic implementation of John Conway's Game of Life CA

// Each cell is now an object!

GOL gol;

void setup() {
  size(640, 360);
  gol = new GOL();
}

void draw() {
  background(255);

  gol.generate();
  gol.display();
}

// reset board when mouse is pressed
void mousePressed() {
  gol.init();
}

