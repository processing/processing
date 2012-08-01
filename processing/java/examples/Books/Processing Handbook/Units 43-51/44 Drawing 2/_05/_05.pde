int numLines = 500;
MovingLine[] lines = new MovingLine[numLines];
int currentLine = 0;

void setup() {
  size(100, 100);
  smooth();
  frameRate(30);
  for (int i = 0; i < numLines; i++) {
    lines[i] = new MovingLine();
  }
}

void draw() {
  background(204);
  for (int i = 0; i < currentLine; i++) {
    lines[i].display();
  }
}

void mouseDragged() {
  lines[currentLine].setPosition(mouseX, mouseY,
                                 pmouseX, pmouseY);
  if (currentLine < numLines - 1) {
    currentLine++;
  }
}

class MovingLine {
  float x1, y1, x2, y2;

  void setPosition(int x, int y, int px, int py) {
    x1 = x;
    y1 = y;
    x2 = px;
    y2 = py;
  }

  void display() {
    x1 += random(-0.1, 0.1);
    y1 += random(-0.1, 0.1);
    x2 += random(-0.1, 0.1);
    y2 += random(-0.1, 0.1);
    line(x1, y1, x2, y2);
  }
}
