/**
 * Spore 1 
 * by Mike Davis. 
 * 
 * A short program for alife experiments. Click in the window to restart.
 * Each cell is represented by a pixel on the display as well as an entry in
 * the array 'cells'. Each cell has a run() method, which performs actions
 * based on the cell's surroundings.  Cells run one at a time (to avoid conflicts
 * like wanting to move to the same space) and in random order.
 */

World w;
int numcells = 0;
int maxcells = 6700;
Cell[] cells = new Cell[maxcells];
color spore_color;
// set lower for smoother animation, higher for faster simulation
int runs_per_loop = 10000;
color black = color(0, 0, 0);
  
void setup() {
  size(640, 360);
  frameRate(24);
  reset();
}

void reset() {
  clearScreen();  
  w = new World();
  spore_color = color(172, 255, 128);
  seed();
}

void seed() {
  // Add cells at random places
  for (int i = 0; i < maxcells; i++)
  {
    int cX = (int)random(width);
    int cY = (int)random(height);
    if (w.getpix(cX, cY) == black) {
      w.setpix(cX, cY, spore_color);
      cells[numcells] = new Cell(cX, cY);
      numcells++;
    }
  }
}

void draw() {
  // Run cells in random order
  for (int i = 0; i < runs_per_loop; i++) {
    int selected = min((int)random(numcells), numcells - 1);
    cells[selected].run();
  }
}

void clearScreen() {
  background(0);
}

class Cell {
  int x, y;
  Cell(int xin, int yin) {
    x = xin;
    y = yin;
  }

    // Perform action based on surroundings
  void run() {
    // Fix cell coordinates
    while(x < 0) {
      x+=width;
    }
    while(x > width - 1) {
      x-=width;
    }
    while(y < 0) {
      y+=height;
    }
    while(y > height - 1) {
      y-=height;
    }
    
    // Cell instructions
    if (w.getpix(x + 1, y) == black) {
      move(0, 1);
    } else if (w.getpix(x, y - 1) != black && w.getpix(x, y + 1) != black) {
      move((int)random(9) - 4, (int)random(9) - 4);
    }
  }
  
  // Will move the cell (dx, dy) units if that space is empty
  void move(int dx, int dy) {
    if (w.getpix(x + dx, y + dy) == black) {
      w.setpix(x + dx, y + dy, w.getpix(x, y));
      w.setpix(x, y, color(0));
      x += dx;
      y += dy;
    }
  }
}

//  The World class simply provides two functions, get and set, which access the
//  display in the same way as getPixel and setPixel.  The only difference is that
//  the World class's get and set do screen wraparound ("toroidal coordinates").
class World {
  
  void setpix(int x, int y, int c) {
    while(x < 0) x+=width;
    while(x > width - 1) x-=width;
    while(y < 0) y+=height;
    while(y > height - 1) y-=height;
    set(x, y, c);
  }
  
  color getpix(int x, int y) {
    while(x < 0) x+=width;
    while(x > width - 1) x-=width;
    while(y < 0) y+=height;
    while(y > height - 1) y-=height;
    return get(x, y);
  }
}

void mousePressed() {
  numcells = 0;
  reset();
}

