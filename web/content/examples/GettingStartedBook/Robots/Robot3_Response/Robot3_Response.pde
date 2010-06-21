// Robot 3: Response from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

float x = 60;          // X-coordinate
float y = 440;         // Y-coordinate
int radius = 45;       // Head Radius
int bodyHeight = 160;  // Body Height
int neckHeight = 70;   // Neck Height

float easing = 0.02;

void setup() {
  size(360, 480);
  smooth();
  strokeWeight(2);
  ellipseMode(RADIUS);
}

void draw() {
  
  int targetX = mouseX;
  x += (targetX - x) * easing;
    
  if (mousePressed) {
    neckHeight = 16;
    bodyHeight = 90; 
  } else {
    neckHeight = 70;
    bodyHeight = 160; 
  }
  
  float ny = y - bodyHeight - neckHeight - radius;
  
  background(204);
  
  // Neck
  stroke(102);
  line(x+12, y-bodyHeight, x+12, ny); 

  // Antennae
  line(x+12, ny, x-18, ny-43);
  line(x+12, ny, x+42, ny-99);
  line(x+12, ny, x+78, ny+15);

  // Body
  noStroke();
  fill(102);
  ellipse(x, y-33, 33, 33);
  fill(0);
  rect(x-45, y-bodyHeight, 90, bodyHeight-33);

  // Head
  fill(0);
  ellipse(x+12, ny, radius, radius); 
  fill(255);
  ellipse(x+24, ny-6, 14, 14);
  fill(0);
  ellipse(x+24, ny-6, 3, 3);
}
