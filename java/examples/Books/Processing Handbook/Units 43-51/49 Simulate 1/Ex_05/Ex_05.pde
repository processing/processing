int[][] angles = { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 1,-1 },
                   { 0,-1 }, {-1,-1 }, {-1, 0 }, {-1, 1 } };
int numAngles = angles.length;
int x, y, nx, ny;
int dir = 0;
color black = color(0);
color white = color(255);

void setup() {
  size(100, 100);
  background(255);
  x = width / 2;
  nx = x;
  y = height / 2;
  ny = y;
  float woodDensity = width * height * 0.5;
  for (int i = 0; i < woodDensity; i++) {
    int rx = int(random(width));
    int ry = int(random(height));
    set(rx, ry, black);
  }
}

void draw() {
  int rand = int(abs(random(-1, 2)));
  dir = (dir + rand + numAngles) % numAngles;
  nx = (nx + angles[dir][0] + width) % width;
  ny = (ny + angles[dir][1] + height) % height;
  if ((get(x, y) == black) && (get(nx, ny) == white)) {
// Move the chip one space
    set(x, y, white);
    set(nx, ny, black);
    x = nx;
    y = ny;
  }
  else if ((get(x, y) == black) && (get(nx, ny) == black)) {
// Move in the opposite direction
    dir = (dir + (numAngles / 2)) % numAngles;
    x = (x + angles[dir][0] + width) % width;
    y = (y + angles[dir][1] + height) % height;
  }
  else {
// Not carrying
    x = nx;
    y = ny;
  }
  nx = x; // Save the current position
  ny = y;
}
