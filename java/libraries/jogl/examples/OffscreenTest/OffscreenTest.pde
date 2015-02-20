import processing.jogl.*;

PGraphics pg;

void setup() {
  size(400, 400, JOGL.P2D);

  pg = createGraphics(400, 400, JOGL.P2D);
  pg.smooth(4);
}

void draw() {
  background(0);

  pg.beginDraw();
  pg.background(255, 0, 0);
  pg.ellipse(mouseX, mouseY, 100, 100);
  pg.endDraw();

  image(pg, 0, 0, 400, 400);
}

void keyPressed() {
  if (key == '1') pg.smooth(1);
  else if (key == '2') pg.smooth(2);
  else if (key == '3') pg.smooth(4);
  else if (key == '4') pg.smooth(8);
  else if (key == '5') pg.smooth(16);
  else if (key == '6') pg.smooth(32); 
}

