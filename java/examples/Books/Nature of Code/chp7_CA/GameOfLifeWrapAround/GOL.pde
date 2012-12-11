// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class GOL {

  int w = 8;
  int columns, rows;

  // Game of life board
  int[][] board;


  GOL() {
    // Initialize rows, columns and set-up arrays
    columns = width/w;
    rows = height/w;
    board = new int[columns][rows];
    //next = new int[columns][rows];
    // Call function to fill array with random values 0 or 1
    init();
  }

  void init() {
    for (int i =1;i < columns-1;i++) {
      for (int j =1;j < rows-1;j++) {
        board[i][j] = int(random(2));
      }
    }
  }

  // The process of creating the new generation
  void generate() {

    int[][] next = new int[columns][rows];

    // Loop through every spot in our 2D array and check spots neighbors
    for (int x = 0; x < columns; x++) {
      for (int y = 0; y < rows; y++) {

        // Add up all the states in a 3x3 surrounding grid
        int neighbors = 0;
        for (int i = -1; i <= 1; i++) {
          for (int j = -1; j <= 1; j++) {
            neighbors += board[(x+i+columns)%columns][(y+j+rows)%rows];
          }
        }

        // A little trick to subtract the current cell's state since
        // we added it in the above loop
        neighbors -= board[x][y];

        // Rules of Life
        if      ((board[x][y] == 1) && (neighbors <  2)) next[x][y] = 0;           // Loneliness
        else if ((board[x][y] == 1) && (neighbors >  3)) next[x][y] = 0;           // Overpopulation
        else if ((board[x][y] == 0) && (neighbors == 3)) next[x][y] = 1;           // Reproduction
        else                                            next[x][y] = board[x][y];  // Stasis
      }
    }

    // Next is now our board
    board = next;
  }

  // This is the easy part, just draw the cells, fill 255 for '1', fill 0 for '0'
  void display() {
    for ( int i = 0; i < columns;i++) {
      for ( int j = 0; j < rows;j++) {
        if ((board[i][j] == 1)) fill(0);
        else fill(255); 
        stroke(0);
        rect(i*w, j*w, w, w);
      }
    }
  }
}

