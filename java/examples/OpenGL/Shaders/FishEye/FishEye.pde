// Fish-eye shader, useful for dome projection

PGraphics canvas;
PShader fisheye;
PImage img;

void setup() {
  size(400, 400, P3D);  
  canvas = createGraphics(400, 400, P3D);

  fisheye = loadShader(PShader.TEXTURED, "FishEye.glsl");
  fisheye.set("aperture", 180.0);
  shader(fisheye);
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
  
  // The rendering of this image will be done through the fisheye shader, since
  // it was set as the PShader.TEXTURED shader of the main surface.
  image(canvas, 0, 0, width, height);
}
