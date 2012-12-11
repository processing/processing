// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Wolfram Cellular Automata

// A class to manage the CA

class CA {

  int[] cells;     // An array of 0s and 1s 
  int generation;  // How many generations?

  int[] ruleset;     // An array to store the ruleset, for example {0,1,1,0,1,1,0,1}

  CA(int[] r) {
    ruleset = r;
    cells = new int[width/scl];
    restart();
  }

  CA() {
    scl = 1;
    cells = new int[width/scl];
    randomize();
    restart();
  }

  // Set the rules of the CA
  void setRules(int[] r) {
    ruleset = r;
  }

  // Make a random ruleset
  void randomize() {
    for (int i = 0; i < 8; i++) {
      ruleset[i] = int(random(2));
    }
  }

  // Reset to generation 0
  void restart() {
    for (int i = 0; i < cells.length; i++) {
      cells[i] = 0;
    }
    cells[cells.length/2] = 1;    // We arbitrarily start with just the middle cell having a state of "1"
    generation = 0;
  }

  // The process of creating the new generation
  void generate() {
    // First we create an empty array for the new values
    int[] nextgen = new int[cells.length];
    // For every spot, determine new state by examing current state, and neighbor states
    // Ignore edges that only have one neighor
    for (int i = 1; i < cells.length-1; i++) {
      int left = cells[i-1];   // Left neighbor state
      int me = cells[i];       // Current state
      int right = cells[i+1];  // Right neighbor state
      nextgen[i] = rules(left, me, right); // Compute next generation state based on ruleset
    }
    // The current generation is the new generation
    cells = nextgen;
    generation++;
  }

  // This is the easy part, just draw the cells, fill 255 for '1', fill 0 for '0'
  void render() {
    for (int i = 0; i < cells.length; i++) {
      if (cells[i] == 1) fill(0);
      else               fill(255);
      stroke(0);
      rect(i*scl, generation*scl, scl, scl);
    }
  }

  // Implementing the Wolfram rules
  // Could be improved and made more concise, but here we can explicitly see what is going on for each case
  int rules (int a, int b, int c) {
    String s = "" + a + b + c;
    int index = Integer.parseInt(s,2);
    return ruleset[index];
  }

  // The CA is done if it reaches the bottom of the screen
  boolean finished() {
    if (generation > height/scl) {
      return true;
    } 
    else {
      return false;
    }
  }
}

