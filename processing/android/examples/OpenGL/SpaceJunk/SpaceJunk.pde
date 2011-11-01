// Space Junk, by Ira Greenberg. 
// Zoom suggestion by Danny Greenberg.
// 
// Rotating cubes in space using a custom Cube class. 
// Color controlled by light sources. Swipe the finger left
// and right (in landscape mode) to zoom.

// Used for oveall rotation
float ang;

// Cube count-lower/raise to test performance
int limit = 200;

// Array for all cubes
Cube[]cubes = new Cube[limit];

void setup() {
  size(800, 480, P3D); 
  orientation(LANDSCAPE);
  
  background(0); 
  noStroke();

  // Instantiate cubes, passing in random vals for size and postion
  for (int i = 0; i< cubes.length; i++){
    cubes[i] = new Cube(int(random(-10, 10)), int(random(-10, 10)), 
    int(random(-10, 10)), int(random(-140, 140)), int(random(-140, 140)), 
    int(random(-140, 140)));
  }
  
  // Automatic normal calculation can be turned on/off.
  autoNormal(true);
}

void draw(){
  background(0); 
  fill(200);

  // Set up some different colored lights
  pointLight(51, 102, 255, 65, 60, 100); 
  pointLight(200, 40, 60, -65, -60, -150);

  // Raise overall light in scene 
  ambientLight(70, 70, 10); 

  // Center geometry in display windwow.
  // you can change 3rd argument ('0')
  // to move block group closer(+)/further(-)
  translate(width/2, height/2, -200 + mouseX * 0.65);

  // Rotate around y and x axes
  rotateY(radians(ang));
  rotateX(radians(ang));

  // Draw cubes
  for (int i = 0; i < cubes.length; i++){
    cubes[i].drawCube();
  }
  
  // Used in rotate function calls above
  ang++;
}


