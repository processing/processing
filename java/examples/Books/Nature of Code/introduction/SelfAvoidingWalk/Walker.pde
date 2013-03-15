// Daniel Shiffman
// The Nature of Code
// http://www.shiffman.net/

// A random walker object!

class Walker {
  int x, y;

  boolean[][] grid;

  Walker() {
    x = width/2;
    y = height/2;
    grid = new boolean[width][height];
  }

  void render() {
    stroke(0);
    line(x,y,x,y);
  }

  // Randomly move up, down, left, right, or stay in one place
  void step() {

    boolean ok = false;

    int helpme = 0;

    while (!ok) {

      int choice = int(random(4));

      int saveX = x;
      int saveY = y;

      if (choice == 0) {
        x++;
      } 
      else if (choice == 1) {
        x--;
      } 
      else if (choice == 2) {
        y++;
      } 
      else {
        y--;
      }
      
      x = constrain(x, 0, width-1);
      y = constrain(y, 0, height-1);

      if (grid[x][y] == false) {
        ok = true;
        grid[x][y] = true;
      } 
      else {
        x = saveX;
        y = saveY;
      }


      helpme++;

      if (helpme > 1000) {
        println("STUCK");
        noLoop();
        ok = true;
      }
    }
  }
}

