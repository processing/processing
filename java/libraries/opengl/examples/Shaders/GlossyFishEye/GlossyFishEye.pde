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

  PGraphicsOpenGL pg = (PGraphicsOpenGL)g;
  fisheye = pg.loadShader("FishEye.glsl", PShader.TEXTURED);
  fisheye.setUniform("aperture", 180.0);
  pg.setShader(fisheye, PShader.TEXTURED);
  usingFishEye = true;
  
  glossy = pg.loadShader("GlossyVert.glsl", "GlossyFrag.glsl", PShader.LIT);  
  glossy.setUniform("AmbientColour", 0, 0, 0);
  glossy.setUniform("DiffuseColour", 0.9, 0.2, 0.2);
  glossy.setUniform("SpecularColour", 1.0, 1.0, 1.0);
  glossy.setUniform("AmbientIntensity", 1.0);
  glossy.setUniform("DiffuseIntensity", 1.0);
  glossy.setUniform("SpecularIntensity", 0.7);
  glossy.setUniform("Roughness", 0.7);
  glossy.setUniform("Sharpness", 0.0);
  ((PGraphicsOpenGL)canvas).setShader(glossy, PShader.LIT);
  
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
    ((PGraphicsOpenGL)g).defaultShader(PShader.TEXTURED);
    usingFishEye = false;
  } else {
    ((PGraphicsOpenGL)g).setShader(fisheye, PShader.TEXTURED);
    usingFishEye = true;
  }
}
