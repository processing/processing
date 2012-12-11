// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

class GOL {

  float w = 20;
  float h = sin(radians(60))*w;
  int columns, rows;
  
  // Game of life board
  Cell[][] board;


  GOL() {
    // Initialize rows, columns and set-up arrays
    columns = width/int(w*3);
    rows = height/int(h);
    board = new Cell[columns][rows];
    init();
  }

  void init() {
    float h = sin(radians(60))*w;
    for (int i = 0; i < columns; i++) {
      for (int j = 0; j < rows; j++) {
        if (j % 2 == 0) board[i][j] = new Cell(i*w*3, j*h,w);
        else board[i][j] = new Cell(i*w*3+w+h/2, j*h, w);
      }
    }
  }



  // This is the easy part, just draw the cells, fill 255 for '1', fill 0 for '0'
  void display() {
    for ( int i = 0; i < columns;i++) {
      for ( int j = 0; j < rows;j++) {
        board[i][j].display();
      }
    }
  }
}

