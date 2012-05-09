/**
 * Cubes Contained Within a Cube 
 * by Ira Greenberg.  
 * 
 * Collision detection against all
 * outer cube's surfaces. 
 * Uses the Point3D and Cube classes. 
 */

Cube stage; // external large cube
int cubies = 20;
Cube[]c = new Cube[cubies]; // internal little cubes
color[][]quadBG = new color[cubies][6];

// Controls cubie's movement
float[]x = new float[cubies];
float[]y = new float[cubies];
float[]z = new float[cubies];
float[]xSpeed = new float[cubies];
float[]ySpeed = new float[cubies];
float[]zSpeed = new float[cubies];

// Controls cubie's rotation
float[]xRot = new float[cubies];
float[]yRot = new float[cubies];
float[]zRot = new float[cubies];

// Size of external cube
float bounds = 300;

void setup() {
  size(640, 360, P3D);
  orientation(LANDSCAPE);
  
  for (int i = 0; i < cubies; i++){
    // Each cube face has a random color component
    float colorShift = random(-75, 75);
    quadBG[i][0] = color(0);
    quadBG[i][1] = color(51);
    quadBG[i][2] = color(102);
    quadBG[i][3] = color(153);
    quadBG[i][4] = color(204);
    quadBG[i][5] = color(255);

    // Cubies are randomly sized
    float cubieSize = random(5, 15);
    c[i] =  new Cube(cubieSize, cubieSize, cubieSize);

    // Initialize cubie's position, speed and rotation
    x[i] = 0;
    y[i] = 0; 
    z[i] = 0;

    xSpeed[i] = random(-1, 1);
    ySpeed[i] = random(-1, 1); 
    zSpeed[i] = random(-1, 1); 

    xRot[i] = random(40, 100);
    yRot[i] = random(40, 100);
    zRot[i] = random(40, 100);
  }
  
  // Instantiate external large cube
  stage =  new Cube(bounds, bounds, bounds);
}

void draw(){
  background(50);
  lights();
  
  // Center in display window
  translate(width/2, height/2, -130);
  
  // Outer transparent cube
  noFill(); 
  
  // Rotate everything, including external large cube
  rotateX(frameCount * 0.001);
  rotateY(frameCount * 0.002);
  rotateZ(frameCount * 0.001);
  stroke(255);
  
  // Draw external large cube
  stage.create();

  // Move and rotate cubies
  for (int i = 0; i < cubies; i++){
    pushMatrix();
    translate(x[i], y[i], z[i]);
    rotateX(frameCount*PI/xRot[i]);
    rotateY(frameCount*PI/yRot[i]);
    rotateX(frameCount*PI/zRot[i]);
    noStroke();
    c[i].create(quadBG[i]);
    x[i] += xSpeed[i];
    y[i] += ySpeed[i];
    z[i] += zSpeed[i];
    popMatrix();

    // Draw lines connecting cubbies
    stroke(0);
    if (i < cubies-1){
      line(x[i], y[i], z[i], x[i+1], y[i+1], z[i+1]);
    }

    // Check wall collisions
    if (x[i] > bounds/2 || x[i] < -bounds/2){
      xSpeed[i]*=-1;
    }
    if (y[i] > bounds/2 || y[i] < -bounds/2){
      ySpeed[i]*=-1;
    }
    if (z[i] > bounds/2 || z[i] < -bounds/2){
      zSpeed[i]*=-1;
    }
  }
}

