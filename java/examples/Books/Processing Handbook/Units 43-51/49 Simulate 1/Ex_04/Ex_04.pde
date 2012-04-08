int SOUTH = 0; // Direction numbers with names
int EAST = 1; // so that the code self-documents
int NORTH = 2;
int WEST = 3;
int direction = NORTH; // Current direction of the ant
int x, y; // Ant's current position
color ON = color(255); // Color for an 'on' pixel
color OFF = color(0); // Color for an 'off' pixel

void setup() {
  size(100, 100);
  x = width / 2;
  y = height / 2;
  background(0);
}

void draw() {
  if (direction == SOUTH) {
    y++;
    if (y == height) {
      y = 0;
    }
  } else if (direction == EAST) {
    x++;
    if (x == width) {
      x = 0;
    }
  } else if (direction == NORTH) {
    if (y == 0) {
      y = height - 1;
    } else {
      y--;
    }
  } else if (direction == WEST) {
    if (x == 0) {
      x = width - 1;
    } else {
      x--;
    }
  }
  if (get(x, y) == ON) {
    set(x, y, OFF);
    if (direction == SOUTH) {
      direction = WEST;
    } else {
      direction--;
    }
  }
  else {
    set(x, y, ON);
    if (direction == WEST) {
      direction = SOUTH;
    } else {
      direction++; // Rotate direction
    }
  }
}
