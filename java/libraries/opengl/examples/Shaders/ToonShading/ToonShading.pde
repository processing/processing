// Example showing the use of a custom lighting shader in order  
// to apply a toon effect on the scene.
// Based on the glsl tutorial from lighthouse 3D:
// http://www.lighthouse3d.com/tutorials/glsl-tutorial/toon-shader-version-ii/

PShader toon;
boolean customShader;  

public void setup() {
  size(400, 400, P3D);
  noStroke();
  fill(204);

  toon = (PShader)loadShader("ToonVert.glsl", "ToonFrag.glsl", PShader.LIT);
  shader(toon, PShader.LIT);
  customShader = true;
}

public void draw() {
  noStroke(); 
  background(0); 
  float dirY = (mouseY / float(height) - 0.5) * 2;
  float dirX = (mouseX / float(width) - 0.5) * 2;
  directionalLight(204, 204, 204, -dirX, -dirY, -1);
  translate(width/2, height/2); 
  sphere(80);
}  

public void mousePressed() {
  if (dist(mouseX, mouseY, width/2, height/2) < 80) {
    if (customShader) {
      resetShader(PShader.LIT);
      customShader = false;
    } 
    else {
      shader(toon, PShader.LIT);
      customShader = true;
    }  
  }
}

