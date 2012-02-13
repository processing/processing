// Example showing the use of a custom lighting shader in order  
// to apply a toon effect on the scene.
// Based on the glsl tutorial from lighthouse 3D:
// http://www.lighthouse3d.com/tutorials/glsl-tutorial/toon-shader-version-ii/

PShader shader;
PGraphicsAndroid3D pg;  
boolean usingShader;  

public void setup() {
  size(400, 400, P3D);
  noStroke();
  fill(204);

  pg = (PGraphicsAndroid3D)g;
  shader = pg.loadShader("ToonVert.glsl", "ToonFrag.glsl", FILL_SHADER_LIT);
  pg.setShader(shader, FILL_SHADER_LIT);
  usingShader = true;
}

void draw() {
  noStroke(); 
  background(0); 
  float dirY = (sin(PI * frameCount * 0.01) - 0.5) * 2;
  float dirX = (cos(PI * frameCount * 0.01) - 0.5) * 2;
  directionalLight(204, 204, 204, -dirX, -dirY, -1);
  translate(width/2, height/2); 
  sphere(80);
}  

void mousePressed() {
  if (usingShader) {
    pg.resetShader(FILL_SHADER_LIT);
    usingShader = false;
  } 
  else {
    pg.setShader(shader, FILL_SHADER_LIT);
    usingShader = true;
  }
}

