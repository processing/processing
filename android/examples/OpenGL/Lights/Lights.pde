// Lights
// Modified from an example by Simon Greenwold. 
// Display a box with three different kinds of lights. 

void setup() {
  size(480,  800,  A3D);
  orientation(PORTRAIT);

  noStroke();
}

void draw() {
  defineLights();
  background(0);

  fill(150);

  for (int x = 0; x <= width + 100; x += 100) {
    for (int y = 0; y <= height + 100; y += 100) {
      pushMatrix();
      translate(x,  y);
      rotateY(map(mouseX,  0,  width,  0,  PI));
      rotateX(map(mouseY,  0,  height,  0,  PI));
      box(90);
      popMatrix();
    }
  }
}

void defineLights() {
  // Orange point light on the right
  pointLight(150,  100,  0,     // Color
  300,  -200,  0); // Position

  // Blue directional light from the left
  directionalLight(0,  102,  255,  // Color
  1,  0,  0);    // The x-, y-, z-axis direction

  // Yellow spotlight from the front
  spotLight(255,  255,  109,     // Color
  0,  40,  200,        // Position
  0,  -0.5f,  -0.5f,   // Direction
  PI / 2,  2);       // Angle, concentration
}  

