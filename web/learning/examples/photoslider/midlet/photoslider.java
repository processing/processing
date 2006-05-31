import processing.core.*; import processing.video.*; public class photoslider extends PMIDlet{// Photoslider
// by Francis Li
// http://www.francisli.com/
//
// Posted May 31, 2006
// 
// This example demonstrates the use of the Video library to capture images
// on a camera phone. An image is captured then cut up into a simple slider
// puzzle. Use the select/fire button on your phone to capture the image-
// you may get a security warning asking for your permission to use the
// camera. Then, use the d-pad/joystick on your phone to move the empty
// box around, swapping with an adjoining piece, until you've reassembled
// the image. When the image has been reassembled, the original image will
// be displayed in its entirety. If you can't reassemble the photo, simply
// press select/fire again to see the solved image. Pressing select/fire
// on the assembled image to start the process over again with camera capture.
//


final int STATE_CAPTURE = 0;
final int STATE_PLAY    = 1;
final int STATE_SOLVED  = 2;
final int STATE_ERROR   = 3;

final int BOARD_SIZE    = 3;
final int BOARD_TOTAL   = BOARD_SIZE * BOARD_SIZE;

Capture cap;
int state;
PFont font;
PImage img;
PImage[] pieces;
int startX, startY, pieceSize;
int[][] board;
int cursorRow, cursorCol;

public void setup() {
  //// set up camera capture from library
  cap = new Capture(this);  
  int x = (width - cap.viewWidth) / 2;
  int y = (height - cap.viewHeight) / 2; 
  //// set starting state based on success on starting camera 
  if (cap.show(x, y)) {
    state = STATE_CAPTURE;
  } 
  else {
    state = STATE_ERROR;
  }  
  //// load and set default phone font for display
  font = loadFont();
  textFont(font);
  //// no animation loop during capture
  noLoop();
}

public void destroy() {
  //// close the camera object on sketch exit
  cap.close();
}

public void draw() {
  //// set a black background
  background(0);
  if (state == STATE_PLAY) {
    //// set white outlines around pieces
    stroke(255);
    noFill();
    int index;
    //// draw the pieces of the board
    for (int row = 0; row < BOARD_SIZE; row++) {
      for (int col = 0; col < BOARD_SIZE; col++) {
        index = board[row][col];
        if (index >= 0) {
          image(pieces[index], startX + col * pieceSize, startY + row * pieceSize);
        }
        rect(startX + col * pieceSize, startY + row * pieceSize, pieceSize, pieceSize);
      }
    }
  } 
  else if (state == STATE_SOLVED) {
    //// once solved, show complete image centered in screen
    image(img, (width - img.width) / 2, (height - img.height) / 2);
  }
  else if (state == STATE_ERROR) {
    //// paint error message
    fill(255);
    text("Unable to initialize your camera.", 10, 10, width - 20, height - 20);
  }
}

public void keyPressed() {
  switch (state) {
  case STATE_CAPTURE:
    //// capture a frame and load it into a PImage
    byte[] data = cap.read();
    cap.hide();
    img = loadImage(data);

    //// based on captured image size, set up pieces first time
    if (pieces == null) {
      //// calculate size of pieces
      pieceSize = min(min(img.width, img.height) / BOARD_SIZE, min(width, height) / BOARD_SIZE);
      //// find starting coordinate so pieces are in center
      startX = (width - pieceSize * BOARD_SIZE) / 2;
      startY = (height - pieceSize * BOARD_SIZE) / 2;
      //// create pieces
      pieces = new PImage[BOARD_TOTAL];
      for (int i = 0; i < BOARD_TOTAL; i++) {
        pieces[i] = new PImage(pieceSize, pieceSize);
      }
      //// set up board
      board = new int[BOARD_SIZE][BOARD_SIZE];
    }

    //// cut image up
    int x = (img.width - pieceSize * BOARD_SIZE) / 2;
    int y = (img.height - pieceSize * BOARD_SIZE) / 2;
    int imgSize = min(img.width, img.height) / BOARD_SIZE;      
    for (int row = 0; row < BOARD_SIZE; row++) {
      for (int col = 0; col < BOARD_SIZE; col++) {
        pieces[row * BOARD_SIZE + col].copy(img, x + col * imgSize, y + row * imgSize, imgSize, imgSize, 0, 0, pieceSize, pieceSize);
      }
    }

    //// set up board
    shuffle();

    //// go to play state
    state = STATE_PLAY;
    redraw();
    break;
  case STATE_PLAY:
    int newRow = cursorRow, newCol = cursorCol;
    switch (keyCode) {
    case UP:
      if (cursorRow > 0) {
        newRow = cursorRow - 1;
      }
      break;
    case DOWN:
      if (cursorRow < (BOARD_SIZE - 1)) {
        newRow = cursorRow + 1;
      }
      break;
    case LEFT:
      if (cursorCol > 0) {
        newCol = cursorCol - 1;
      }
      break;
    case RIGHT:
      if (cursorCol < (BOARD_SIZE - 1)) {
        newCol = cursorCol + 1;
      }
      break;
    case FIRE:
      state = STATE_SOLVED;
      break;
    }
    boolean solved = swap(cursorRow, cursorCol, newRow, newCol, true);
    if (solved) {
      state = STATE_SOLVED;
    } 
    else {
      cursorRow = newRow;
      cursorCol = newCol;
    }  
    //// in case a move was made, redraw the screen
    redraw();
    break;
  case STATE_SOLVED:
    img = null;
    if (cap.show((width - cap.viewWidth) / 2, (height - cap.viewHeight) / 2)) {
      state = STATE_CAPTURE;
    } else {
      state = STATE_ERROR;
    }
    
    redraw();
    break;
  }
}

public boolean swap(int oldRow, int oldCol, int newRow, int newCol, boolean check) {
  if ((oldRow != newRow) || (oldCol != newCol)) {
    int swap = board[oldRow][oldCol];
    board[oldRow][oldCol] = board[newRow][newCol];
    board[newRow][newCol] = swap;
  }
  //// check if solved
  boolean solved = true;
  if (check) {
    int index;
    for (int i = 0; i < BOARD_TOTAL; i++) {
      index = board[i / BOARD_SIZE][i % BOARD_SIZE];
      if ((index >= 0) && (index != i)) {
        solved = false;
        break;
      }
    }
  }
  return solved;
}

public void shuffle() {
  //// first, initialze board to "solved"
  for (int row = 0; row < BOARD_SIZE; row++) {
    for (int col = 0; col < BOARD_SIZE; col++) {
      board[row][col] = row * BOARD_SIZE + col;
    }
  }
  //// randomly pick a square to be the blank
  int row = random(BOARD_SIZE - 1);
  int col = random(BOARD_SIZE - 1);
  //// negative number used to indicate blank;
  board[row][col] = -1;

  //// pick a random number of moves to shuffle
  int moves = random(6, 12);
  int dir, lastDir = -1, swap;
  for (int i = 0; i < moves; i++) {
    //// pick a random direction to swap
    dir = random(3);
    switch (dir) {
    case 0:
      //// up
      if ((lastDir == 1) || (col == 0)) {
        i--;
      } 
      else {
        swap(row, col, row, col - 1, false);
        col = col - 1;
        lastDir = dir;
      } 
      break;
    case 1:
      //// down
      if ((lastDir == 0) || (col == (BOARD_SIZE - 1))) {
        i--;
      } 
      else {
        swap(row, col, row, col + 1, false);
        col = col + 1;
        lastDir = dir;
      } 
      break;
    case 2:
      //// left
      if ((lastDir == 3) || (row == 0)) {
        i--;
      } 
      else {
        swap(row, col, row - 1, col, false);
        row = row - 1;
        lastDir = dir;
      } 
      break;
    case 3:
      //// right
      if ((lastDir == 2) || (row == (BOARD_SIZE - 1))) {
        i--;
      } 
      else {
        swap(row, col, row + 1, col, false);
        row = row + 1;
        lastDir = dir;
      } 
      break;
    }
  }
  //// save last position as cursor start position
  cursorRow = row;
  cursorCol = col;
}
}