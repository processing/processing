int[][] grid, futureGrid;

void setup() {
  size(540, 100);
  frameRate(8);
  grid = new int[width][height];
  futureGrid = new int[width][height];
  float density = 0.3 * width * height;
  for (int i = 0; i < density; i++) {
    grid[int(random(width))][int(random(height))] = 1;
  }
  background(0);
}

void draw() {
  for (int x = 1; x < width - 1; x++) {
    for (int y = 1; y < height - 1; y++) {
      // Check the number of neighbors (adjacent cells)
      int nb = neighbors(x, y);
      if ((grid[x][y] == 1) && (nb < 2)) {
        futureGrid[x][y] = 0; // Isolation death
        set(x, y, color(0));
      } else if ((grid[x][y] == 1) && (nb > 3)) {
        futureGrid[x][y] = 0; // Overpopulation death
        set(x, y, color(0));
      } else if ((grid[x][y] == 0) && (nb == 3)) {
        futureGrid[x][y] = 1; // Birth
        set(x, y, color(255));
      } else {
        futureGrid[x][y] = grid[x][y]; // Survive
      }
    }
  }

  // Swap current and future grids
  int[][] temp = grid;
  grid = futureGrid;
  futureGrid = temp;
}

// Count the number of adjacent cells 'on'
int neighbors(int x, int y) {
  return grid[x][y-1] + // North
         grid[x+1][y-1] + // Northeast
         grid[x+1][y] + // East
         grid[x+1][y+1] + // Souteast
         grid[x][y+1] + // South
         grid[x-1][y+1] + // Southwest
         grid[x-1][y] + // West
         grid[x-1][y-1]; // Northwest
}

