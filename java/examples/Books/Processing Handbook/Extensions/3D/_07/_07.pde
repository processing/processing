// Vary the specular reflection component of a material
// with vertical position of the mouse
void setup() {
  size(400, 400, P3D);
  noStroke();
  colorMode(RGB, 1);
  fill(0.4);
}

void draw() {
  background(0);
  translate(width / 2, height / 2);
  // Set the specular color of lights that follow
  lightSpecular(1, 1, 1);
  directionalLight(0.8, 0.8, 0.8, 0, 0, -1);
  float s = mouseX / float(width);
  specular(s, s, s);
  sphere(100);
}
