int x = 215;

void setup() {
  size(480, 120);
}

void draw() {
  if (keyPressed && (key == CODED)) { // If it's a coded key
    if (keyCode == LEFT) {            // If it's the left arrow
      x--;
    } 
    else if (keyCode == RIGHT) {      // If it's the right arrow
      x++;
    }
  }
  rect(x, 45, 50, 50);
}

