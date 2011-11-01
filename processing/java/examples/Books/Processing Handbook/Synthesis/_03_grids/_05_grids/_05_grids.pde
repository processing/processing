/** 
 * Synthesis 1: Form and Code
 * Wilson Grids by Casey Reas (www.processing.org)
 * p. 152
 * 
 * Step 5. 
*/


size(600, 600);
background(0);
stroke(255, 204);
float numSquares = 12.0;
int gap = 4;
float xUnit = (width-gap*2)/numSquares;
float yUnit = (height-gap*2)/numSquares;
float squareWidth = xUnit;
float squareHeight = yUnit;

for (float y = 0; y < numSquares; y = Y + 1) {
  for (float x = 0; x < numSquares; x = x+1) {
    float ydiv = squareHeight/12;
    float xoff = xUnit/2+gap;
    float yoff = yUnit/2+gap;
    float xp = xoff + (x*xUnit);
    float yp = yoff + (y*yUnit);
    for(float i=0; i<=squareHeight-gap*2; i=i+ydiv) {
      float yy = i*squareHeight/2+gap+i;
      line(xp-squareWidth/2+gap, yp-squareHeight/2+gap+i, xp+squareWidth/2-gap, yp-squareHeight/2+gap+i);
    }
    
    ydiv = random(1.0, squareHeight/4);
    
    for(float i=0; i<=squareHeight-gap*2; i=i+ydiv) {
      float yy = i*squareHeight/2+gap+i;
      line(xp-squareWidth/2+gap, yp-squareHeight/2+gap+i, xp+squareWidth/2-gap, yp-squareHeight/2+gap+i);
    }
  } 
  
}

// save("Synthesis-3--3-" + int(random(0, 100)) + ".tif");
