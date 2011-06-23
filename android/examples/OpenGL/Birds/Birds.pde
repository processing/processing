// Crazy Flocking 3D Birds, by Ira Greenberg. 
// Android port by Andres Colubri
// Simulates a flock of birds using a Bird class and nested
// pushMatrix() / popMatrix() functions. 
// Trigonometry functions handle the flapping and sinuous movement.
//
// This example shows the shape recording functionality in A3D,
// where an entire geometry drawn with the regular API can be
// stored in a PShape3D object for later use. This greatly
// improves the performance of the application.
// Tap on the screen to enable/disable drawing with the recorded
// shape.

// Flock array
int birdCount = 200;
Bird[]birds = new Bird[birdCount];
float[]x = new float[birdCount];
float[]y = new float[birdCount];
float[]z = new float[birdCount];
float[]rx = new float[birdCount];
float[]ry = new float[birdCount];
float[]rz = new float[birdCount];
float[]spd = new float[birdCount];
float[]rot = new float[birdCount];
PShape body;
PShape wing;

boolean usingPShape = false;

void setup() {
  size(screenWidth, screenHeight, P3D);
  orientation(PORTRAIT);
  
  PFont font = createFont(PFont.list()[0], 24);
  textFont(font, 24);  
  
  noStroke();

  // Initialize arrays with random values
  for (int i = 0; i < birdCount; i++){
    birds[i] = new Bird(random(-300, 300), random(-300, 300), 
               random(-500, -2500), random(5, 30), random(5, 30)); 

    x[i] = random(20, 340);
    y[i] = random(30, 350);
    z[i] = random(1000, 4800);
    rx[i] = random(-160, 160);
    ry[i] = random(-55, 55);
    rz[i] = random(-20, 20);
    spd[i] = random(.1, 3.75);
    rot[i] = random(.025, .15);
  }
 
  // Saving box and rectangle used to draw birds
  // into PShape3D objects.
  body = beginRecord();
  box(1, 1, 1);
  endRecord();  

  wing = beginRecord();
  rect(0, 0, 1, 1);
  endRecord();
}

void draw() {
  background(0);
  lights();

  for (int i = 0; i < birdCount; i++){
    birds[i].setFlight(x[i], y[i], z[i], rx[i], ry[i], rz[i]);
    birds[i].setWingSpeed(spd[i]);
    birds[i].setRotSpeed(rot[i]);
    birds[i].fly();
  }

  fill(255);
  if (usingPShape) {
    text("With PShape3D. FPS: " + frameRate, 10, height - 30);  
  } else {
    text("Without PShape3D. FPS: " + frameRate, 10, height - 30);  
  } 
}

void mousePressed() {
  usingPShape = !usingPShape;
}
