import processing.core.*; import processing.bluetooth.*; public class tictactoe extends PMIDlet{// Tic-Tac-Toe
// by Francis Li
// http://www.francisli.com/
//
// This example demonstrates the use of the Bluetooth library in implementing
// the simple turn-based game Tic-Tac-Toe. The game and its presentation are simple
// so as not to obscure the code related to Bluetooth networking. The code
// demonstrates how a phone can advertise itself with a service, find another
// phone running the service, connect, and communicate.
//


final int STATE_START = 0;
final int STATE_FIND  = 1;
final int STATE_HOST  = 2;
final int STATE_PLAY  = 3;
final int STATE_OVER  = 4;

final int PIECE_NONE  = 0;
final int PIECE_X     = 1;
final int PIECE_O     = 2;

final int RESULT_NONE = 0;
final int RESULT_X    = PIECE_X;
final int RESULT_O    = PIECE_O;
final int RESULT_DRAW = 3;

int state;
int result;

int[][] board;
int thirdX, thirdY;

boolean yourturn;
int yourpiece;
int selectionRow, selectionColumn;

//// bluetooth library
Bluetooth bt;
//// discovered services
Service[] services;
//// status message
String msg;
//// connection to other player
Client c;

PFont font;

public void setup() {
  //// set up font
  font = loadFont();
  textFont(font);

  //// initialize Bluetooth library
  bt = new Bluetooth(this);

  //// set up board and drawing
  board = new int[3][3];
  thirdX = width / 3;
  thirdY = (height - font.height) / 3;
  ellipseMode(CORNER);
}

public void destroy() {
  bt.stop();
}

public void draw() {
  background(255);

  if (state == STATE_START) {
    fill(0);
    textAlign(LEFT);
    text("Tic-tac-toe\n1. Start a new game\n2. Find a game", 2, 2, width - 4, height - 4);
  } 
  else if (state == STATE_FIND) {
    fill(0);
    textAlign(LEFT);
    if (services == null) {
      text("Looking for a game to join...\n\n" + msg, 2, 2, width - 4, height - 4);
    } 
    else {
      String list = "Games found:\n\n";
      for (int i = 0, length = length(services); i < length; i++) {
        list += i + ". " + services[i].device.name + "\n";
      }
      text(list, 2, 2, width - 4, height - 4);
    }
  } 
  else if (state == STATE_HOST) {
    fill(0);
    textAlign(LEFT);
    text("Waiting for another player...", 2, 2, width - 4, height - 4);
  }
  else {
    noFill();
    textAlign(CENTER);      
    if (state == STATE_PLAY) {
      if (yourturn) {
        text("YOUR TURN", 2, 2, width - 4, font.height);
      } 
      else {
        text("Waiting...", 2, 2, width - 4, font.height);
      }
    } 
    else {
      if (result == RESULT_DRAW) {
        text("Draw!", 2, 2, width - 4, font.height);
      } 
      else if (result == yourpiece) {
        text("You WIN!", 2, 2, width - 4, font.height);
      } 
      else {
        text("You LOSE...", 2, 2, width - 4, font.height);
      }
    }
    translate(0, font.height);
    stroke(0);
    line(0, thirdY, width, thirdY);
    line(0, 2 * thirdY, width, 2 * thirdY);
    line(thirdX, 0, thirdX, height);
    line(2 * thirdX, 0, 2 * thirdX, height);

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        switch(board[row][col]) {
        case 1:
          ellipse(col * thirdX, row * thirdY, thirdX, thirdY);
          break;
        case 2:
          line(col * thirdX, row * thirdY, (col + 1) * thirdX, (row + 1) * thirdY);
          line(col * thirdX, (row + 1) * thirdY, (col + 1) * thirdX, row * thirdY);          
          break;
        }
      }
    }

    if ((state == STATE_PLAY) && !yourturn) {
      if (c.available() > 0) {
        int theirpiece = c.readInt();
        int row = c.readInt();
        int col = c.readInt();
        board[row][col] = theirpiece;
        yourturn = true;

        result = checkResult();
        if (result != RESULT_NONE) {
          state = STATE_OVER;
          //// to confirm last message recieved, send result back
          c.writeInt(result);
          c.flush();
          //// close the connection
          c.stop();
          c = null;
        }
      }
    } 
    else {
      if (c != null) {
        //// if we were the one to find the result first, check for the confirmation
        try {
          if (c.available() > 0) {
            //// don't actually care about value, just read that last int
            c.readInt();
            //// close
            c.stop();
            c = null;
          }
        } 
        catch (PException pe) {
          //// an exception may occur if the other side closed before we read the confirmation
          //// which is fine, just close on our side
          c.stop();
          c = null;
        }
      }
    }
  }
}

public void libraryEvent(Object library, int event, Object data) {
  if (library == bt) {
    switch (event) {
    case Bluetooth.EVENT_DISCOVER_DEVICE:
      msg = "Found device at: " + ((Device) data).address + "...";
      break;
    case Bluetooth.EVENT_DISCOVER_DEVICE_COMPLETED:
      msg = "Found " + length((Device[]) data) + " devices, looking for game...";
      break;
    case Bluetooth.EVENT_DISCOVER_SERVICE:      
      msg = "Found game on " + ((Service[]) data)[0].device.address + "...";
      break;
    case Bluetooth.EVENT_DISCOVER_SERVICE_COMPLETED:
      services = (Service[]) data;
      msg = "Search complete.";
      break;
    case Bluetooth.EVENT_CLIENT_CONNECTED:
      c = (Client) data;
      yourturn = true;
      yourpiece = PIECE_X;
      state = STATE_PLAY;
      break;
    }
  }
}

public void keyPressed() {
  if (state == STATE_START) {
    switch (key) {
    case '1':
      bt.start("tictactoe");
      state = STATE_HOST;
      break;
    case '2':
      services = null;
      bt.find();
      state = STATE_FIND;
      msg = "Looking for devices...";
      break;
    }
  } 
  else if (state == STATE_FIND) {
    if (services != null) {
      if ((key >= '0') && (key <= '9')) {
        int i = key - '0';
        if (i < length(services)) {
          c = services[i].connect();
          yourturn = false;
          yourpiece = PIECE_O;
          state = STATE_PLAY;
        }
      }
    }
  }
  else if ((state == STATE_PLAY) && yourturn) {
    int row = -1, col = -1;
    switch (keyCode) {
    default:
      switch (key) {
      case '1':
        row = 0; 
        col = 0;
        break;
      case '2':
        row = 0; 
        col = 1;
        break;
      case '3':
        row = 0; 
        col = 2;
        break;
      case '4':
        row = 1; 
        col = 0;
        break;
      case '5':
        row = 1; 
        col = 1;
        break;
      case '6':
        row = 1; 
        col = 2;
        break;
      case '7':
        row = 2; 
        col = 0;
        break;
      case '8':
        row = 2; 
        col = 1;
        break;
      case '9':
        row = 2; 
        col = 2;
        break;
      }
    }
    if ((row >= 0) && (col >= 0)) {
      if (board[row][col] == PIECE_NONE) {
        board[row][col] = yourpiece;        
        yourturn = false;
        c.writeInt(yourpiece);
        c.writeInt(row);
        c.writeInt(col);
        c.flush();
        result = checkResult();
        if (result != RESULT_NONE) {
          state = STATE_OVER;
        }
      }
    }
  } 
  else if (state == STATE_OVER) {
    if (keyCode == FIRE) {
      state = STATE_START;
      //// reset board
      for (int row = 0; row < 3; row++) {
        for (int col = 0; col < 3; col++) {
          board[row][col] = PIECE_NONE;
        }
      }
    }
  }
}

public int checkResult() {
  int result = RESULT_NONE;
  //// check for a winner
  for (int piece = PIECE_X; piece <= PIECE_O; piece++) {
    //// check rows and columns simultaneously
    for (int i = 0; i < 3; i++) {
      //// check row
      if ((board[i][0] == piece) && (board[i][1] == piece) && (board[i][2] == piece)) {
        result = piece;
        break;
      }
      //// check column
      if ((board[0][i] == piece) && (board[1][i] == piece) && (board[2][i] == piece)) {
        result = piece;
        break;
      }
    }
    //// check diagonals
    if (result != RESULT_NONE) {
      break;
    }
    else if ((board[0][0] == piece) && (board[1][1] == piece) && (board[2][2] == piece)) {
      result = piece;
      break;
    } 
    else if ((board[0][2] == piece) && (board[1][1] == piece) && (board[2][0] == piece)) {
      result = piece;
      break;
    }
  }
  //// check for a draw
  if (result == RESULT_NONE) {
    result = RESULT_DRAW;
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        if (board[row][col] == PIECE_NONE) {
          //// still empty spaces left, so not a draw yet
          result = RESULT_NONE;
          break;
        }
      }
      if (result == RESULT_NONE) {
        break;
      }
    }
  }
  return result;
}
}