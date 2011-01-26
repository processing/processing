/** 
 * Synthesis 1: Form and Code
 * Wilson Grids by Casey Reas (www.processing.org)
 * p. 152
 * 
 * Step 4. 
*/

size(600, 600);
background(0);
stroke(255, 204);
float numSquares = 6.0;
int gap = 8;
float xUnit = (width-gap*2)/numSquares;
float yUnit = (height-gap*2)/numSquares;
float squareWidth = xUnit;
int squareHeight = int(yUnit);

for(float y=0; y<numSquares; y=y+1) {
  for(float x=0; x<numSquares; x=x+1) {
    squareWidth = int(random(10, 300));
    squareHeight = int(squareWidth);
    //squareHeight = int(random(10, 100));
    int ydiv = squareHeight/12;
    float xoff = xUnit/2+gap;
    float yoff = yUnit/2+gap;
    float xp = xoff + (x*xUnit);
    float yp = yoff + (y*yUnit);
    for(int i=0; i<=squareHeight-gap*2; i=i+ydiv) {
      float yy = i*squareHeight/2+gap+i;
      line(xp-squareWidth/2+gap, yp-squareHeight/2+gap+i, xp+squareWidth/2-gap, yp-squareHeight/2+gap+i);
    }
  } 
  
}

