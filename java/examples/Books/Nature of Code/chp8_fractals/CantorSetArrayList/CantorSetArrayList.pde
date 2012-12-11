// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Cantor Set
// Renders a simple fractal, the Cantor Set
// Uses an ArrayList to store list of objects
// Generates when mouse is pressed

float h = 30;

// List of line objects
ArrayList<CantorLine> cantor;

void setup() {
  size(729, 200);

  // Start with one line
  cantor = new ArrayList<CantorLine>();
  cantor.add(new CantorLine(0, 100, width));
}

// Click the mouse to advance the sequence
void mousePressed() {
  generate();
}

void draw() {
  background(255);
  // Always show all the lines
  for (CantorLine cl : cantor) {
    cl.display();
  }
  
  fill(0);
  text("Click mouse to generate",10,height-20);
}

void generate() {
  // Generate the next set of lines
  ArrayList<CantorLine> next = new ArrayList<CantorLine>();
  for (CantorLine cl : cantor) {
    next.add(new CantorLine(cl.x,cl.y,cl.len/3));
    next.add(new CantorLine(cl.x+cl.len*2/3,cl.y,cl.len/3));
  }
  cantor = next;
}

