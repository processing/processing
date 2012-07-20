// Fish-eye shader on the main surface, glossy specular reflection shader
// on the offscreen canvas.

PGraphics canvas;
PShader fisheye;
PShader glossy;
PImage img;
PShape ball;

boolean usingFishEye;

void setup() {
  size(800, 800, P3D);  
  canvas = createGraphics(800, 800, P3D);

  fisheye = (PShader)loadShader("FishEye.glsl", PShader.TEXTURED);
  fisheye.set("aperture", 180.0);
  shader(fisheye, PShader.TEXTURED);
  usingFishEye = true;
  
  glossy = (PShader)loadShader("GlossyVert.glsl", "GlossyFrag.glsl", PShader.LIT);  
  glossy.set("AmbientColour", 0, 0, 0);
  glossy.set("DiffuseColour", 0.9, 0.2, 0.2);
  glossy.set("SpecularColour", 1.0, 1.0, 1.0);
  glossy.set("AmbientIntensity", 1.0);
  glossy.set("DiffuseIntensity", 1.0);
  glossy.set("SpecularIntensity", 0.7);
  glossy.set("Roughness", 0.7);
  glossy.set("Sharpness", 0.0);
  canvas.shader(glossy, PShader.LIT);
  
  ball = createShape(SPHERE, 50);
  //ball.fill(200, 50, 50);
  ball.noStroke();
}

void draw() {
  canvas.beginDraw();
  canvas.noStroke();
  canvas.background(0);
  canvas.pushMatrix();
  canvas.rotateY(frameCount * 0.01);
  canvas.pointLight(204, 204, 204, 1000, 1000, 1000);
  canvas.popMatrix();
  for (float x = 0; x < canvas.width + 100; x += 100) {
    for (float y = 0; y < canvas.height + 100; y += 100) {
      for (float z = 0; z < 400; z += 100) {
        canvas.pushMatrix();
        canvas.translate(x, y, -z);
        canvas.shape(ball);
        canvas.popMatrix();
      }
    }
  } 
  canvas.endDraw(); 
  
  image(canvas, 0, 0, width, height);
  
  println(frameRate);
}

public void mousePressed() {
  if (usingFishEye) {
    resetShader(PShader.TEXTURED);
    usingFishEye = false;
  } else {
    shader(fisheye, PShader.TEXTURED);
    usingFishEye = true;
  }
}