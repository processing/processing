// Wolfram Cellular Automata
// Daniel Shiffman <http://www.shiffman.net>

// A class to manage the CA

class CA {

  int generation;  // How many generations?
  int[] ruleset;   // An array to store the ruleset, for example {0,1,1,0,1,1,0,1}
  int w = 5;
  int[][] matrix;  // Store a history of generations in 2D array, not just one

  int cols;
  int rows;


  CA(int[] r) {
    ruleset = r;
    cols = width/w;
    rows = height/w;
    matrix = new int[cols][rows];
    restart();
  }

  // Make a random ruleset
  void randomize() {
    for (int i = 0; i < 8; i++) {
      ruleset[i] = int(random(2));
    }
  }

  // Reset to generation 0
  void restart() {
    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        matrix[i][j] = 0;
      }
    }
    matrix[cols/2][0] = 1;    // We arbitrarily start with just the middle cell having a state of "1"
    generation = 0;
  }


  // The process of creating the new generation
  void generate() {

    // For every spot, determine new state by examing current state, and neighbor states
    // Ignore edges that only have one neighor
    for (int i = 0; i < cols; i++) {
      int left  = matrix[(i+cols-1)%cols][generation%rows];   // Left neighbor state
      int me    = matrix[i][generation%rows];       // Current state
      int right = matrix[(i+1)%cols][generation%rows];  // Right neighbor state
      matrix[i][(generation+1)%rows] = rules(left, me, right); // Compute next generation state based on ruleset
    }
    generation++;
  }

  // This is the easy part, just draw the cells, fill 255 for '1', fill 0 for '0'
  void display() {
    int offset = generation%rows;

    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        int y = j - offset;
        if (y <= 0) y = rows + y;
        if (matrix[i][j] == 1) fill(0);
        else                   fill(255);
        noStroke();
        rect(i*w, (y-1)*w, w, w);
      }
    }
  }
  // Implementing the Wolfram rules
  // This is the concise conversion to binary way
  /*int rules (int a, int b, int c) {
   String s = "" + a + b + c;
   int index = Integer.parseInt(s, 2);
   return ruleset[index];
   }*/
  // For JavaScript Mode
  int rules (int a, int b, int c) {
    if (a == 1 && b == 1 && c == 1) return ruleset[7];
    if (a == 1 && b == 1 && c == 0) return ruleset[6];
    if (a == 1 && b == 0 && c == 1) return ruleset[5];
    if (a == 1 && b == 0 && c == 0) return ruleset[4];
    if (a == 0 && b == 1 && c == 1) return ruleset[3];
    if (a == 0 && b == 1 && c == 0) return ruleset[2];
    if (a == 0 && b == 0 && c == 1) return ruleset[1];
    if (a == 0 && b == 0 && c == 0) return ruleset[0];
    return 0;
  }

  // The CA is done if it reaches the bottom of the screen
  boolean finished() {
    if (generation > height/w) {
      return true;
    } 
    else {
      return false;
    }
  }
}

