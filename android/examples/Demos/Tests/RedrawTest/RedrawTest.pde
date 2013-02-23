void setup() {
  size(400, 400, P2D);  
  noLoop();
}

void draw() {
  background(255, 0, 0);
  ellipse(mouseX, mouseY, 100, 50);
  println("draw");  
}

void mousePressed() {
  redraw();
}

