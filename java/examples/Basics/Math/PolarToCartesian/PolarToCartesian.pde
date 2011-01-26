/**
 * PolarToCartesian
 * by Daniel Shiffman.  
 * 
 * Convert a polar coordinate (r,theta) to cartesian (x,y):  
 * x = r * cos(theta)
 * y = r * sin(theta)
 */
 
float r;

// Angle and angular velocity, accleration
float theta;
float theta_vel;
float theta_acc;

void setup() {
  size(200, 200);
  frameRate(30);
  smooth();
  
  // Initialize all values
  r = 50;
  theta = 0;
  theta_vel = 0;
  theta_acc = 0.0001;
}

void draw() {
  background(0);
  // Translate the origin point to the center of the screen
  translate(width/2, height/2);
  
  // Convert polar to cartesian
  float x = r * cos(theta);
  float y = r * sin(theta);
  
  // Draw the ellipse at the cartesian coordinate
  ellipseMode(CENTER);
  noStroke();
  fill(200);
  ellipse(x, y, 16, 16);
  
  // Apply acceleration and velocity to angle (r remains static in this example)
  theta_vel += theta_acc;
  theta += theta_vel;

}




