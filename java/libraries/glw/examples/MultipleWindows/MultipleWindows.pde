import processing.glw.*;

PGraphics canvas1;
PGraphics canvas2;
  
void setup() {
  size(100, 100, GLW.RENDERER);
  canvas1 = createGraphics(320, 240, GLW.P2D);
  canvas2 = createGraphics(320, 240, GLW.P2D);
  GLW.createWindow(canvas1);
  GLW.createWindow(canvas2);
}

void draw() {
  canvas1.beginDraw();    
  canvas1.background(200);
  canvas1.ellipse(mouseX, mouseY, 100, 100);
  canvas1.endDraw();

  canvas2.beginDraw();
  canvas2.background(170);
  canvas2.ellipse(mouseX, mouseY, 50, 50);    
  canvas2.endDraw();      
}
  
void keyPressed() {
  GLW.getFocusedWindow().setVisible(false); 
}