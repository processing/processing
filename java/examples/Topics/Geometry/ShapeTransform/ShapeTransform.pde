/**
 * Shape Transform
 * by Ira Greenberg.  
 * 
 * Illustrates the geometric relationship 
 * between Cube, Pyramid, Cone and 
 * Cylinder 3D primitives.
 * 
 * Instructions:<br />
 * Up Arrow - increases points<br />
 * Down Arrow - decreases points<br />
 * 'p' key toggles between cube/pyramid<br />
 */

int pts = 4; 
float angle = 0;
float radius = 99;
float cylinderLength = 95;

//vertices
PVector vertices[][];
boolean isPyramid = false;

float angleInc;

void setup(){
  size(640, 360, P3D);
  noStroke();
  angleInc = PI/300.0;
}

void draw(){
  background(170, 95, 95);
  lights();
  fill(255, 200, 200);
  translate(width/2, height/2);
  rotateX(frameCount * angleInc);
  rotateY(frameCount * angleInc);
  rotateZ(frameCount * angleInc);

  // initialize vertex arrays
  vertices = new PVector[2][pts+1];

  // fill arrays
  for (int i = 0; i < 2; i++){
    angle = 0;
    for(int j = 0; j <= pts; j++){
      vertices[i][j] = new PVector();
      if (isPyramid){
        if (i==1){
          vertices[i][j].x = 0;
          vertices[i][j].y = 0;
        }
        else {
          vertices[i][j].x = cos(radians(angle)) * radius;
          vertices[i][j].y = sin(radians(angle)) * radius;
        }
      } 
      else {
        vertices[i][j].x = cos(radians(angle)) * radius;
        vertices[i][j].y = sin(radians(angle)) * radius;
      }
      vertices[i][j].z = cylinderLength; 
      // the .0 after the 360 is critical
      angle += 360.0/pts;
    }
    cylinderLength *= -1;
  }

  // draw cylinder tube
  beginShape(QUAD_STRIP);
  for(int j = 0; j <= pts; j++){
    vertex(vertices[0][j].x, vertices[0][j].y, vertices[0][j].z);
    vertex(vertices[1][j].x, vertices[1][j].y, vertices[1][j].z);
  }
  endShape();

  //draw cylinder ends
  for (int i = 0; i < 2; i++){
    beginShape();
    for(int j = 0; j < pts; j++){
      vertex(vertices[i][j].x, vertices[i][j].y, vertices[i][j].z);
    }
    endShape(CLOSE);
  }
}


/*
 up/down arrow keys control
 polygon detail.
 */
void keyPressed(){
  if(key == CODED) { 
    // pts
    if (keyCode == UP) { 
      if (pts < 90){
        pts++;
      } 
    } 
    else if (keyCode == DOWN) { 
      if (pts > 4){
        pts--;
      }
    } 
  }
  if (key =='p'){
    if (isPyramid){
      isPyramid = false;
    } 
    else {
      isPyramid = true;
    }
  }
}

