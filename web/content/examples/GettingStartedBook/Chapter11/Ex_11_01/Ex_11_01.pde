import processing.opengl.*;

void setup() {
  size(440, 220, OPENGL);
  noStroke();
  fill(255, 190);
}

void draw() {
  background(0);
  translate(width/2, height/2, 0);
  rotateX(mouseX / 200.0);
  rotateY(mouseY / 100.0);
  int dim = 18;
  for (int i = -height/2; i < height/2; i += dim*1.2) {
    for (int j = -height/2; j < height/2; j += dim*1.2) {
      beginShape();
      vertex(i, j, 0);
      vertex(i+dim, j, 0);
      vertex(i+dim, j+dim, -dim);
      vertex(i, j+dim, -dim);
      endShape();
    }
  }
}

