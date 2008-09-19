/**
 * Brick Tower
 * by Ira Greenberg. 
 *
 * 3D castle tower constructed out of individual bricks.
 * Uses the Point3D and Cube classes. 
 */

// Point3D and Cube classes required.
float bricksPerLayer = 16.0; // value must be even
float brickLayers = 18.0;
Cube brick;
float brickWidth = 60, brickHeight = 25, brickDepth = 25;
float radius = 175.0;
float angle = 0;

void setup(){
  size(200, 200, P3D);
  brick = new Cube(brickWidth, brickHeight, brickDepth);
}

void draw(){
  background(100, 125, 200);
  float tempX = 0, tempY = 0, tempZ = 0;
  fill(175, 87, 20);
  stroke(100, 50, 10);
  // add basic light setup
  lights();
  translate(width/2, height*1.4, -580);
  // tip tower to see inside
  rotateX(radians(-45));
  // slowly rotate tower
  rotateY(frameCount*PI/600);
  for (int i = 0; i < brickLayers; i++){
    // increment rows
    tempY-=brickHeight;
    // alternate brick seams
    angle = 360.0/bricksPerLayer*i/2;
    for (int j = 0; j < bricksPerLayer; j++){
      tempZ = cos(radians(angle))*radius;
      tempX = sin(radians(angle))*radius;
      pushMatrix();
      translate(tempX, tempY, tempZ);
      rotateY(radians(angle));
      // add crenelation
      if (i==brickLayers-1){
        if (j%2 == 0){
          brick.create();
        }
      }
      // create main tower
      else {
        brick.create();
      }
      popMatrix();
      angle += 360.0/bricksPerLayer;
    }
  }
}


class Point3D{
  float x, y, z;

  // constructors
  Point3D(){
  }

  Point3D(float x, float y, float z){
    this.x = x;
    this.y = y;
    this.z = z;
  }
}

class Cube{
  Point3D[] vertices = new Point3D[24];
  float w, h, d;

  // constructors
  // default constructor
  Cube(){
  }

  Cube(float w, float h, float d){
    this.w = w;
    this.h = h;
    this.d = d;

    // cube composed of 6 quads
    //front
    vertices[0] = new Point3D(-w/2,-h/2,d/2);
    vertices[1] = new Point3D(w/2,-h/2,d/2);
    vertices[2] = new Point3D(w/2,h/2,d/2);
    vertices[3] = new Point3D(-w/2,h/2,d/2);
    //left
    vertices[4] = new Point3D(-w/2,-h/2,d/2);
    vertices[5] = new Point3D(-w/2,-h/2,-d/2);
    vertices[6] = new Point3D(-w/2,h/2,-d/2);
    vertices[7] = new Point3D(-w/2,h/2,d/2);
    //right
    vertices[8] = new Point3D(w/2,-h/2,d/2);
    vertices[9] = new Point3D(w/2,-h/2,-d/2);
    vertices[10] = new Point3D(w/2,h/2,-d/2);
    vertices[11] = new Point3D(w/2,h/2,d/2);
    //back
    vertices[12] = new Point3D(-w/2,-h/2,-d/2);  
    vertices[13] = new Point3D(w/2,-h/2,-d/2);
    vertices[14] = new Point3D(w/2,h/2,-d/2);
    vertices[15] = new Point3D(-w/2,h/2,-d/2);
    //top
    vertices[16] = new Point3D(-w/2,-h/2,d/2);
    vertices[17] = new Point3D(-w/2,-h/2,-d/2);
    vertices[18] = new Point3D(w/2,-h/2,-d/2);
    vertices[19] = new Point3D(w/2,-h/2,d/2);
    //bottom
    vertices[20] = new Point3D(-w/2,h/2,d/2);
    vertices[21] = new Point3D(-w/2,h/2,-d/2);
    vertices[22] = new Point3D(w/2,h/2,-d/2);
    vertices[23] = new Point3D(w/2,h/2,d/2);
  }

  void create(){
    // draw cube
    for (int i=0; i<6; i++){
      beginShape(QUADS);
      for (int j=0; j<4; j++){
        vertex(vertices[j+4*i].x, vertices[j+4*i].y, vertices[j+4*i].z);
      }
      endShape();
    }
  }
} 
