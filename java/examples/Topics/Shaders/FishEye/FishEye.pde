/**
 * Fish Eye
 * 
 * This fish-eye shader is useful for dome projection
 */

PShader fisheye;
PGraphics canvas;
PImage img;

boolean useFishEye = true;

void setup() {
  size(640, 640, P3D);  
  canvas = createGraphics(width, height, P3D);

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
  canvas.rotateX(frameCount * 0.01);
  canvas.rotateY(frameCount * 0.01);  
  canvas.box(100);  
  canvas.endDraw(); 
  
   if (useFishEye == true) {
    shader(fisheye);
  } 
  image(canvas, 0, 0, width, height);
}

void mousePressed() {
  if (useFishEye) {
    useFishEye = false;
    resetShader();    
  } else {
    useFishEye = true;
  }
}
