import processing.glw.*;

void setup() {
  size(2560, 1440, GLW.P2D);
  frameRate(180);
}

void draw() {
  background(255, 0, 0);

  fill(255);
  text("FPS: " + frameRate, mouseX, mouseY);    
}
