// This example uses a FxAA post-processing filter for fast 
// fullscreen antialiasing:
// http://www.kotaku.com.au/2011/12/what-is-fxaa/
//
// Press any key to enable/disable the shader.

PGraphics canvas;
boolean drawing = false;
PGraphicsOpenGL pg;
PShader shader;
boolean usingShader;
String message;
float msgLen;
  
void setup() {
  size(1280, 800, P2D);
  noSmooth();
  
  canvas = createGraphics(width, height, P2D);
  canvas.noSmooth();
    
  pg = (PGraphicsOpenGL) g;
  shader = pg.loadShader("fxaa.glsl", PShader.TEXTURED);
  pg.setShader(shader, PShader.TEXTURED);
  usingShader = true;
  
  canvas.beginDraw();
  canvas.background(255);  
  canvas.stroke(0);
  canvas.strokeWeight(15);
  canvas.strokeCap(ROUND);
  canvas.endDraw();
  
  PFont font = createFont("Arial", 18);
  textFont(font);  
  updateMessage();
  
  drawing = false;
}

public void draw() {
  if (drawing) {
    canvas.beginDraw();
    if (1 < dist(mouseX, mouseY, pmouseX, pmouseY)) {
      canvas.line(pmouseX, pmouseY, mouseX, mouseY);
    }
    canvas.endDraw();
  }
  
  image(canvas, 0, 0);
  
  drawMessage();
}
  
public void mousePressed() {
  if (!drawing && width - msgLen < mouseX && height - 23 < mouseY) {
    if (usingShader) {
      pg.defaultShader(PShader.TEXTURED);
      usingShader = false;
    } else {
      pg.setShader(shader, PShader.TEXTURED);
      usingShader = true;
    }
    updateMessage();    
  } else {
    drawing = true;   
  }
}

void mouseReleased() {
  drawing = false;
}

void updateMessage() {
  if (usingShader) {
    message = "Anti-aliasing enabled";      
  } else {
    message = "Anti-aliasing disabled";
  }
  msgLen = textWidth(message);
}

void drawMessage() {
  if (usingShader) {
    // We need the default texture shader to 
    // render text.
    pg.defaultShader(PShader.TEXTURED);
  }
  fill(0);
  text(message, width - msgLen, height - 5);
  if (usingShader) {
    pg.setShader(shader, PShader.TEXTURED);
  }
}
