// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Bin-Lattice Spatial Subdivision
// http://www.red3d.com/cwr/papers/2000/pip.pdf

// Example demonstrating optimized intersection test for large # of objects
// Each object registers its location in a virtual grid
// Only the objects in neighboring cells on the grid are tested against each other

int totalThings = 2000;

ArrayList<Thing> a;            // ArrayList for all "things"
ArrayList<Thing>[][] grid;     // Grid of ArrayLists for intersection test
int scl = 4;           // Size of each grid cell
int cols, rows;         // Total coluns and rows

void setup() {
  size(640,360);
  a = new ArrayList<Thing>();  // Create the list
  cols = width/scl;     // Calculate cols & rows
  rows = height/scl;    
  
  // Initialize grid as 2D array of empty ArrayLists
  grid = new ArrayList[cols][rows];
  for (int i = 0; i < cols; i++) {
    for (int j = 0; j < rows; j++) {
      grid[i][j] = new ArrayList<Thing>();
    }
  }
  
  // Put 2000 Things in the system
  for (int i = 0; i < totalThings; i++) {
    a.add(new Thing(random(width),random(height)));
  }

}

void draw() {
  background(0);

  // Every time through draw clear all the lists
  for (int i = 0; i < cols; i++) {
    for (int j = 0; j < rows; j++) {
      grid[i][j].clear();
    }
  }

  // Register every Thing object in the grid according to it's location
  for (Thing t : a) {
    t.highlight = false;
    int x = int(t.x) / scl; 
    int y = int (t.y) /scl;
    // It goes in 9 cells, i.e. every Thing is tested against other Things in its cell
    // as well as its 8 neighbors 
    for (int n = -1; n <= 1; n++) {
      for (int m = -1; m <= 1; m++) {
        if (x+n >= 0 && x+n < cols && y+m >= 0 && y+m< rows) grid[x+n][y+m].add(t);
      }
    }
  }

  // Run through the Grid
  stroke(255);
  for (int i = 0; i < cols; i++) {
    //line(i*scl,0,i*scl,height);
    for (int j = 0; j < rows; j++) {
      //line(0,j*scl,width,j*scl);
      
      // For every list in the grid
      ArrayList<Thing> temp = grid[i][j];
      // Check every Thing
      for (Thing t : temp) {
        // Against every other Thing
        for (Thing other : temp) {
          // As long as its not the same one
          if (other != t) {
            // Check to see if they are touching
            // (We could do many other things here besides just intersection tests, such
            // as apply forces, etc.)
            float d = dist(t.x,t.y,other.x,other.y);
            if (d < t.r/2 + other.r/2) {
              t.highlight = true;
            }
          }
        }
      }
    }
  }

  // Display and move all Things
  for (Thing t : a) {
    t.render();
    t.move();
  }
  
  fill(0);
  rect(0,height-20,width,20);
  fill(255);
  text("Framerate: " + int(frameRate),10,height-6);



}
