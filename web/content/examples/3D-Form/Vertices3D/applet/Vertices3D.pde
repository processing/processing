// Vertices 3D
// by REAS <http://reas.com>

// Scaling an object back in space makes it appear smaller.

// Updated 21 August 2002

float spin = PI/6;       // Rotation angle
int circle_points = 6;   // Resolution of arcs
int depth = 10;
float size = 120.0; 
float thick = 50.0;

void setup() 
{
  size(200, 200, P3D);
  framerate(30);
  background(0);
}

void draw() 
{
  noStroke();
  fill(0, 10);
  rect(0, 0, width, height);
  
  translate(width/2, height/2);
  
  for(int i=0; i<depth; i++) {
    stroke(255 - (i * 22.5));
    rotate(spin);
    drawSpokes(-i*20);
    pushMatrix();
    rotate(PI);
    drawSpokes(-i*20);
    popMatrix();
  }
  
  spin += 0.002;
  if(spin>PI) {
    spin = 0;
  }
}

void drawSpokes(float z_space) 
{
  beginShape(LINES);
  for (int i=0; i<circle_points; i++) {
    float angle = PI/2.0 * i/circle_points;
    vertex(size*cos(angle), size*sin(angle), z_space);
    vertex((size-thick)*cos(angle), (size-thick)*sin(angle), z_space);	
  }
  endShape();
}

