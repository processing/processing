import processing.pdf.*;

void setup() {
  size(600, 800, PDF, "Ex-11-5.pdf");
  noFill();
  strokeCap(SQUARE);
}

void draw() {
  background(255);
  for (int y = 100; y < height - 300; y+=20) {
    float r = random(0, 102);
    strokeWeight(r / 10);
    beginShape();
    vertex(100, y);
    vertex(width/2, y + 200);
    vertex(width-100, y);
    endShape();
  }
  exit();
}

