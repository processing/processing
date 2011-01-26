int neighbors(int x, int y) {
  int north = (y + height - 1) % height;
  int south = (y + 1) % height;
  int east = (x + 1) % width;
  int west = (x + width - 1) % width;
  return grid[x][north] + // North
         grid[east][north] + // Northeast
         grid[east][y] + // East
         grid[east][south] + // Southeast
         grid[x][south] + // South
         grid[west][south] + // Southwest
         grid[west][y] + // West
         grid[west][north]; // Northwest
}
