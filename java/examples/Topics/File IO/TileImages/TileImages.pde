/**
 * Tile Images
 *
 * Draws an image larger than the screen, and saves the image as six tiles.
 * The scaleValue variable sets amount of scaling: 1 is 100%, 2 is 200%, etc.
 */

int scaleValue = 3;  // Multiplication factor
int xoffset = 0;     // x-axis offset
int yoffset = 0;     // y-axis offset

void setup() {
  size(600, 600);
  stroke(0, 100);
}

void draw() {
  background(204);
  scale(scaleValue);
  translate(xoffset * (-width / scaleValue), yoffset * (-height / scaleValue));
  line(10, 150, 500, 50);
  line(0, 600, 600, 0);
  save("lines-" + yoffset + "-" + xoffset + ".png");
  setOffset();
}

void setOffset() {
  xoffset++;
  if (xoffset == scaleValue) {
    xoffset = 0;
    yoffset++;
    if (yoffset == scaleValue) {
      println("Tiles saved.");
      exit();
    }
  }
}