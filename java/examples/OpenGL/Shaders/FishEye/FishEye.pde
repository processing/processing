// Fish-eye shader, useful for dome projection

PGraphics canvas;
PShader fisheye;
PImage img;

void setup() {
  size(400, 400, P3D);  
  canvas = createGraphics(400, 400, P3D);

  fisheye = loadShader("FishEye.glsl");
  fisheye.set("aperture", 180.0);  
}

void draw() {
  canvas.beginDraw();
  canvas.background(0);
  canvas.stroke(255, 0, 0);
  for (int i = 0; i < width; i += 10) {
    canvas.line(i, 0, i, height);
  }
  for (int i = 0; i < height; i += 10) {
    canvas.line(0, i, width, i);
  }
  canvas.lights();
  canvas.noStroke();
  canvas.translate(mouseX, mouseY, 100);
  canvas.rotateX(frameCount * 0.01f);
  canvas.rotateY(frameCount * 0.01f);  
  canvas.box(50);  
  canvas.endDraw(); 
  
  shader(fisheye);
  image(canvas, 0, 0, width, height);
}