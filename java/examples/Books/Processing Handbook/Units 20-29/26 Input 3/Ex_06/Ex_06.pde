boolean drawT = false;

void setup() {
  size(100, 100);
  noStroke();
}

void draw() {
  background(204);
  if (drawT == true) {
    rect(20, 20, 60, 20);
    rect(39, 40, 22, 45);
  }
}

void keyPressed() {
  if ((key == 'T') || (key == 't')) {
    drawT = true;
  }
}

void keyReleased() {
  drawT = false;
}