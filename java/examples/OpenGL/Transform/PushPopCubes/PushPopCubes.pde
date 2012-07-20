/**
 * PushPop Cubes  
 * by Ira Greenberg.  
 * 
 * Array of rotating cubes creates
 * dynamic field patterns. Color
 * controlled by light sources. Example 
 * of pushMatrix() and popMatrix().
 */

// Cube class required
float ang;
int rows = 21;
int cols = 21;
int cubeCount = rows*cols;
int colSpan, rowSpan;
float rotspd = 2.0;
Cube[] cubes = new Cube[cubeCount];
float[] angs = new float[cubeCount];
float[] rotvals = new float[cubeCount];

void setup(){
  size(640, 360, P3D);

  colSpan = width/(cols-1);
  rowSpan = height/(rows-1);
  noStroke(); 

  // instantiate cubes
  for (int i = 0; i < cubeCount; i++){
    cubes[i] = new Cube(12, 12, 6, 0, 0, 0);
    /* 3 different rotation options
       - 1st option: cubes each rotate uniformly
       - 2nd option: cubes each rotate randomly
       - 3rd option: cube columns rotate as waves
       To try the different rotations, leave one 
       of the rotVals[i] lines uncommented below
       and the other 2 commented out. */
    
    //rotvals[i] = rotspd;
    //rotvals[i] = random(-rotspd * 2, rotspd * 2);
    rotvals[i] = rotspd += .01;
  }
}

void draw(){
  int cubeCounter = 0;
  background(0);
  fill(200);
  
  // Set up some different colored lights
  pointLight(51, 102, 255, width/3, height/2, 100); 
  pointLight(200, 40, 60,  width/1.5, height/2, -150);

  // Raise overall light in scene 
  ambientLight(170, 170, 100); 

  // Translate, rotate and draw cubes
  for (int i = 0; i < cols; i++){
    for (int j = 0; j < rows; j++){
      pushMatrix();
      /* Translate each block.
         pushmatix and popmatrix add each cube
         translation to matrix, but restore
         original, so each cube rotates around its
         owns center */
      translate(i * colSpan, j * rowSpan, -20);
      //rotate each cube around y and x axes
      rotateY(radians(angs[cubeCounter]));
      rotateX(radians(angs[cubeCounter]));
      cubes[cubeCounter].drawCube();
      popMatrix();
      cubeCounter++;
    }
  }
  // Angs used in rotate function calls above
  for (int i = 0; i < cubeCount; i++){
    angs[i] += rotvals[i];
  }
}

// Simple Cube class, based on Quads
class Cube {

  // Properties
  int w, h, d;
  int shiftX, shiftY, shiftZ;

  // Constructor
  Cube(int w, int h, int d, int shiftX, int shiftY, int shiftZ){
    this.w = w;
    this.h = h;
    this.d = d;
    this.shiftX = shiftX;
    this.shiftY = shiftY;
    this.shiftZ = shiftZ;
  }

  /* Main cube drawing method, which looks 
     more confusing than it really is. It's 
     just a bunch of rectangles drawn for 
     each cube face */
  void drawCube(){
    
    // Front face
    beginShape(QUADS);
    normal(1, 0, 0);
    vertex(-w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
    vertex(w + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
    vertex(w + shiftX, h + shiftY, -d/2 + shiftZ); 
    vertex(-w/2 + shiftX, h + shiftY, -d/2 + shiftZ); 

    // Back face
    normal(-1, 0, 0);
    vertex(-w/2 + shiftX, -h/2 + shiftY, d + shiftZ); 
    vertex(w + shiftX, -h/2 + shiftY, d + shiftZ); 
    vertex(w + shiftX, h + shiftY, d + shiftZ); 
    vertex(-w/2 + shiftX, h + shiftY, d + shiftZ);

    // Left face
    normal(0, -1, 0);
    vertex(-w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
    vertex(-w/2 + shiftX, -h/2 + shiftY, d + shiftZ); 
    vertex(-w/2 + shiftX, h + shiftY, d + shiftZ); 
    vertex(-w/2 + shiftX, h + shiftY, -d/2 + shiftZ); 

    // Right face
    normal(0, 1, 0);
    vertex(w + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
    vertex(w + shiftX, -h/2 + shiftY, d + shiftZ); 
    vertex(w + shiftX, h + shiftY, d + shiftZ); 
    vertex(w + shiftX, h + shiftY, -d/2 + shiftZ); 

    // Top face
    normal(0, 0, 1);
    vertex(-w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
    vertex(w + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
    vertex(w + shiftX, -h/2 + shiftY, d + shiftZ); 
    vertex(-w/2 + shiftX, -h/2 + shiftY, d + shiftZ); 

    // Bottom face
    normal(0, 0, -1);
    vertex(-w/2 + shiftX, h + shiftY, -d/2 + shiftZ); 
    vertex(w + shiftX, h + shiftY, -d/2 + shiftZ); 
    vertex(w + shiftX, h + shiftY, d + shiftZ); 
    vertex(-w/2 + shiftX, h + shiftY, d + shiftZ); 
    endShape(); 
  }
}
