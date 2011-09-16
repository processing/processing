/**
 * Words. 
 * 
 * The text() function is used for writing words to the screen. 
 */


PFont fontA;
  
void setup() {
  size(640, 360);

  // Create the font
  fontA = createFont("Mono", 24);
  textFont(fontA, 24);
}

void draw() {
  background(102);
  textAlign(RIGHT);
  drawType(width * 0.25);
  textAlign(CENTER);
  drawType(width * 0.5);
  textAlign(LEFT);
  drawType(width * 0.75);
}

void drawType(float x) {
  line(x, 0, x, 65);
  line(x, 220, x, height);
  fill(0);
  text("ichi", x, 95);
  fill(51);
  text("ni", x, 130);
  fill(204);
  text("san", x, 165);
  fill(255);
  text("shi", x, 210);
}
