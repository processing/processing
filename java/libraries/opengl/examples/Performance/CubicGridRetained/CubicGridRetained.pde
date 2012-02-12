float boxSize = 20;
float margin = boxSize*2;
float depth = 400;
color boxFill;

PShape grid;

int fcount, lastm;
float frate;
int fint = 3;

void setup() {
  size(640, 360, P3D);
  frameRate(120);
  noStroke();
  
  grid = createShape(PShape.GROUP);
    
  // Build grid using multiple translations 
  for (float i =- depth/2+margin; i <= depth/2-margin; i += boxSize){
    for (float j =- height+margin; j <= height-margin; j += boxSize){
      for (float k =- width+margin; k <= width-margin; k += boxSize){
        // Base fill color on counter values, abs function 
        // ensures values stay within legal range
        boxFill = color(abs(i), abs(j), abs(k), 50);
        
        PShape cube = createShape(QUADS);
        cube.noStroke();
        cube.fill(boxFill);
        
        float w = boxSize;
        float h = boxSize;
        float d = boxSize; 
        float shiftX = k; 
        float shiftY = j; 
        float shiftZ = i;
        
        // Front face
        cube.normal(0, 0, 1);
        cube.vertex(-w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, +h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(-w/2 + shiftX, +h/2 + shiftY, -d/2 + shiftZ); 

        // Back face
        cube.normal(0, 0, -1);
        cube.vertex(-w/2 + shiftX, -h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, -h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, +h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(-w/2 + shiftX, +h/2 + shiftY, +d/2 + shiftZ);

        // Left face
        cube.normal(1, 0, 0);
        cube.vertex(-w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(-w/2 + shiftX, -h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(-w/2 + shiftX, +h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(-w/2 + shiftX, +h/2 + shiftY, -d/2 + shiftZ); 

        // Right face
        cube.normal(-1, 0, 0);
        cube.vertex(+w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, -h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, +h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, +h/2 + shiftY, -d/2 + shiftZ); 

        // Top face
        cube.normal(0, 1, 0);
        cube.vertex(-w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, -h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, -h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(-w/2 + shiftX, -h/2 + shiftY, +d/2 + shiftZ); 

        // Bottom face
        cube.normal(0, -1, 0);
        cube.vertex(-w/2 + shiftX, +h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, +h/2 + shiftY, -d/2 + shiftZ); 
        cube.vertex(+w/2 + shiftX, +h/2 + shiftY, +d/2 + shiftZ); 
        cube.vertex(-w/2 + shiftX, +h/2 + shiftY, +d/2 + shiftZ);  
            
        cube.end();
        grid.addChild(cube);
      }
    }
  }
}

void draw() {
  background(255);
  
  hint(DISABLE_DEPTH_TEST);
  
  // Center and spin grid
  pushMatrix();
  translate(width/2, height/2, -depth);
  rotateY(frameCount * 0.01);
  rotateX(frameCount * 0.01);

  shape(grid);
  popMatrix();
  
  fcount += 1;
  int m = millis();
  if (m - lastm > 1000 * fint) {
    frate = float(fcount) / fint;
    fcount = 0;
    lastm = m;
    println("fps: " + frate); 
  } 
  fill(0);
  text("fps: " + frate, 10, 20);
}
