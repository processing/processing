// Export a DXF file when the R key is pressed
import processing.dxf.*;
boolean record = false;

void setup() {
  size(400, 400, P3D);
  noStroke();
  sphereDetail(12);
}

void draw() {
  if (record == true) {
    beginRaw(DXF, "output.dxf"); // Start recording to the file
  }
  lights();
  background(0);
  translate(width / 3, height / 3, -200);
  rotateZ(map(mouseY, 0, height, 0, PI));
  rotateY(map(mouseX, 0, width, 0, HALF_PI));
  for (int y = -2; y < 2; y++) {
    for (int x = -2; x < 2; x++) {
      for (int z = -2; z < 2; z++) {
        pushMatrix();
        translate(120*x, 120*y, -120*z);
        sphere(30);
        popMatrix();
      }
    }
  }
  if (record == true) {
    endRaw();
    record = false; // Stop recording to the file
  }
}

void keyPressed() {
  if (key == 'R' || key == 'r') { // Press R to save the file
    record = true;
  }
}

