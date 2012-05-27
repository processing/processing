// This example uses a FxAA post-processing filter for fast 
// fullscreen antialiasing:
// http://www.kotaku.com.au/2011/12/what-is-fxaa/
//
// Press any key to enable/disable the shader.

PGraphics canvas;
PGraphicsOpenGL pg;
PShader shader;
boolean usingShader;
  
void setup() {
  size(1280, 800, P2D);
  noSmooth();
  
  canvas = createGraphics(width, height, P2D);
  canvas.noSmooth();
    
  pg = (PGraphicsOpenGL) g;
  shader = pg.loadShader("fxaa.glsl", POLY_SHADER_TEX);
  pg.setShader(shader, POLY_SHADER_TEX);
  println("FXAA shader is enabled");
  usingShader = true;
  
  canvas.beginDraw();
  canvas.background(255);  
  canvas.stroke(0);
  canvas.strokeWeight(15);
  canvas.strokeCap(ROUND);
  canvas.endDraw();
}

public void draw() {
  canvas.beginDraw();
  if (1 < dist(mouseX, mouseY, pmouseX, pmouseY)) {
    canvas.line(pmouseX, pmouseY, mouseX, mouseY);
  }
  canvas.endDraw();
  
  image(canvas, 0, 0);
}
  
public void keyPressed() {
  if (usingShader) {
    pg.defaultShader(POLY_SHADER_TEX);
    println("FXAA shader is disabled");
    usingShader = false;
  } else {
    pg.setShader(shader, POLY_SHADER_TEX);
    println("FXAA shader is enabled");
    usingShader = true;
  }
}

