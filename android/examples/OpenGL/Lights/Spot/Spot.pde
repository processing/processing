/**
 * Spot. 
 * 
 * Move the mouse the change the position and concentation
 * of a blue spot light. 
 */
 
int concentration = 600; // Try values 1 -> 10000
PShape ball;

void setup() 
{
  size(640, 360, P3D);
  orientation(LANDSCAPE);
  noStroke();
  fill(204);
  sphereDetail(60);
  ball = createShape(SPHERE, 120);
}

void draw() 
{
  background(0); 
  
  // Light the bottom of the sphere
  directionalLight(51, 102, 126, 0, -1, 0);
  
  // Orange light on the upper-right of the sphere
  spotLight(204, 153, 0, 360, 160, 600, 0, 0, -1, PI/2, 600); 
  
  // Moving spotlight that follows the mouse
  spotLight(102, 153, 204, 360, mouseY, 600, 0, 0, -1, PI/2, 600); 
  
  translate(width/2, height/2, 0); 
  shape(ball); 
}

