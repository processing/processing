/**
 * Cubes Contained Within a Cube 
 * by Ira Greenberg.  
 * 
 * Collision detection against all
 * outer cube's surfaces. 
 */

// 20 little internal cubes
Cube[] cubies = new Cube[20]; 

// Size of outer cube
float bounds = 300;

void setup() {
  size(640, 360, P3D);

  for (int i = 0; i < cubies.length; i++) {
    // Cubies are randomly sized
    float cubieSize = random(5, 15);
    cubies[i] =  new Cube(cubieSize, cubieSize, cubieSize);
  }

}

void draw() {
  background(50);
  lights();

  // Center in display window
  translate(width/2, height/2, -130);

  // Rotate everything, including external large cube
  rotateX(frameCount * 0.001);
  rotateY(frameCount * 0.002);
  rotateZ(frameCount * 0.001);
  stroke(255);


  // Outer transparent cube, just using box() method
  noFill(); 
  box(bounds);

  // Move and rotate cubies
  for (Cube c : cubies) {
    c.update();
    c.display();
  }
}

