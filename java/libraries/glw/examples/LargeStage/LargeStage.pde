import processing.glw.*;

PGraphics stage;

void setup() {
  // The main window will be hidden, only GLW.RENDERER
  // can be used in size()
  size(100, 100, GLW.RENDERER);
  
  stage = createGraphics(2560, 1440, GLW.P2D);
  GLW.createWindow(stage);
  frameRate(180);
}

void draw() {
  // The draw() method is used to update the offscreen surfaces,
  // but not to draw directly to the screen.  
  stage.beginDraw();    
  stage.background(200);  
  stage.fill(255);
  stage.ellipse(mouseX, mouseY, 50, 50);  
  stage.fill(0);
  stage.text(frameRate, 100, 100);
  stage.endDraw();
}