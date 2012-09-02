// Daniel Shiffman, Nature of Code
// <http://www.shiffman.net>

// A basic implementation of John Conway's Game of Life CA

// Each cell is now an object!

GOL gol;

void setup() {
  size(400, 400);
  smooth();
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

