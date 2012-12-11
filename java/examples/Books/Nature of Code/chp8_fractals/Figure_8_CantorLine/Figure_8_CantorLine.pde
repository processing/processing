// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

void setup() {
  size(800, 60);
  background(255);
}

void cantor(float x, float y, float len) {
  line(x, y, x+len, y);

  y += 20;
  line(x,y,x+len/3,y); //[bold]
  line(x+len*2/3,y,x+len,y); //[bold]
}

void draw() {
  cantor(10, 20, width-20);
  save("chapter08_12.png");
  noLoop();
}

