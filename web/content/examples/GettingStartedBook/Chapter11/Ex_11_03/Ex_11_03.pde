import processing.opengl.*;

void setup() {
  size(420, 220, OPENGL);
  noStroke();
}

void draw() {
  lights();
  background(0);
  float camZ = (height/2.0) / tan(PI*60.0 / 360.0);
  camera(mouseX, mouseY, camZ,      // Camera location
         width/2.0, height/2.0, 0,  // Camera target
         0, 1, 0);                  // Camera orientation
  translate(width/2, height/2, -20);
  int dim = 18;
  for (int i = -height/2; i < height/2; i += dim*1.4) {
    for (int j = -height/2; j < height/2; j += dim*1.4) {
      pushMatrix();
      translate(i, j, -j);
      box(dim, dim, dim);
      popMatrix();
    }
  }
}

