// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Wolfram Cellular Automata

// Simple demonstration of a Wolfram 1-dimensional cellular automata
// When the system reaches bottom of the window, it restarts with a new ruleset
// Mouse click restarts as well

CA ca;   // An object to describe a Wolfram elementary Cellular Automata

int delay = 0;

void setup() {
  size(800, 200);
  background(255);
  int[] ruleset = {
    0, 1, 0, 1, 1, 0, 1, 0
  };    // An initial rule system
  ca = new CA(ruleset);                 // Initialize CA
  frameRate(30);
}

void draw() {
  ca.display();          // Draw the CA
  ca.generate();

  if (ca.finished()) {   // If we're done, clear the screen, pick a new ruleset and restart
    delay++;
    if (delay > 30) {
      background(255);
      ca.randomize();
      ca.restart();
      delay = 0;
    }
  }
}

void mousePressed() {
  background(255);
  ca.randomize();
  ca.restart();
}

