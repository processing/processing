PShape cube;

void setup() {
  size(400, 400, P3D);

  cube = createShape(BOX, 100);
}

void draw() {
  background(120);

  lights();

  translate(mouseX, mouseY);
  rotateX(frameCount * 0.01f);
  rotateY(frameCount * 0.01f);

  shape(cube);
}

void keyPressed() {
  // Restarts the OpenGL surface. Automatically 
  // recreates all the current GL resources.
  ((PGraphicsOpenGL)g).restartPGL();
}

