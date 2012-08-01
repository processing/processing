/**
 * Double Random 
 * by Ira Greenberg.  
 * 
 * Using two random() calls and the point() function 
 * to create an irregular sawtooth line.
 */

size(200, 200);
background(0);
int totalPts = 300;
float steps = totalPts + 1;
stroke(255);
float rand = 0;

for  (int i = 1; i < steps; i++){
  point( (width/steps) * i, (height/2) + random(-rand, rand) );
  rand += random(-5, 5);
}

