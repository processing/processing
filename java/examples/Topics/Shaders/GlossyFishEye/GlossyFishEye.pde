/**
 * Glossy Fish Eye
 * 
 * A fish-eye shader is used on the main surface and 
 * a glossy specular reflection shader is used on the
 * offscreen canvas. 
 */

PShader fisheye;
PShader glossy;
PGraphics canvas;
PImage img;
PShape ball;

boolean useFishEye = true;

void setup() {
  size(640, 640, P3D);  
  canvas = createGraphics(width, height, P3D);

  fisheye = loadShader("FishEye.glsl");
  fisheye.set("aperture", 180.0);
  
  glossy = loadShader("GlossyFrag.glsl", "GlossyVert.glsl");  
  glossy.set("AmbientColour", 0.0, 0.0, 0.0);
  glossy.set("DiffuseColour", 0.9, 0.2, 0.2);
  glossy.set("SpecularColour", 1.0, 1.0, 1.0);
  glossy.set("AmbientIntensity", 1.0);
  glossy.set("DiffuseIntensity", 1.0);
  glossy.set("SpecularIntensity", 0.7);
  glossy.set("Roughness", 0.7);
  glossy.set("Sharpness", 0.0);
  
  ball = createShape(SPHERE, 50);
  ball.setStroke(false);
}

void draw() {
  canvas.beginDraw();
  canvas.shader(glossy);
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
