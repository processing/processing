/**
 * Pie Chart  
 * By Ira Greenberg 
 * 
 * Uses the arc() function to generate a pie chart from the data
 * stored in an array. 
 */
 
size(200, 200);
background(100);
smooth();
noStroke();

int diameter = 150;
int[] angs = {30, 10, 45, 35, 60, 38, 75, 67};
float lastAng = 0;

for (int i = 0; i < angs.length; i++){
  fill(angs[i] * 3.0);
  arc(width/2, height/2, diameter, diameter, lastAng, lastAng+radians(angs[i]));
  lastAng += radians(angs[i]);  
}

