/**
 * Pie Chart  
 * By Ira Greenberg 
 * 
 * Uses the arc() function to generate a pie chart from the data
 * stored in an array. 
 */


float diameter;
int[] angles = { 30, 10, 45, 35, 60, 38, 75, 67 };
float lastAngle = 0;

void setup() {
  size(640, 360);
  background(100);
  smooth();
  noStroke();
  diameter = min(width, height) * 0.75;
  noLoop();  // Run once and stop
}


void draw() {
  for (int i = 0; i < angles.length; i++) {
    fill(angles[i] * 3.0);
    arc(width/2, height/2, diameter, diameter, lastAngle, lastAngle+radians(angles[i]));
    lastAngle += radians(angles[i]);
  }
}

