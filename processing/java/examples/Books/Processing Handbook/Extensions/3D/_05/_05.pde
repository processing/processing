// Import and display an OBJ model
// requires OBJ loader from: http://users.design.ucla.edu/%7Etatsuyas/tools/objloader/index.htm
import saito.objloader.*;
OBJModel model;

void setup() {
  size(400, 400, P3D);
  model = new OBJModel(this);
  model.load("chair.obj"); // Model must be in the data directory
  model.drawMode(POLYGON);
  noStroke();
}

void draw() {
  background(0);
  lights();
  pushMatrix();
  translate(width / 2, height, -width);
  rotateY(map(mouseX, 0, width, -PI, PI));
  rotateX(PI / 4);
  scale(6.0);
  model.draw();
  popMatrix();
}
