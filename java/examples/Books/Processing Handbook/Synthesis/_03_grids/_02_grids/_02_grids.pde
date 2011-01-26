/** 
 * Synthesis 1: Form and Code
 * Wilson Grids by Casey Reas (www.processing.org)
 * p. 152
 * 
 * Step 2, textures inside the grid by adding a third for structure. 
*/


size(600, 600);
background(255);
int numSquares = 12;
int gap = 4;
int sqSize = (width-gap*2)/numSquares;
int halfSize = sqSize/2;
int offset = halfSize+gap;
int ydiv = sqSize/12;

for(int y = 0; y < numSquares; y++) {
  for(int x = 0; x < numSquares; x++) {
    float xp = offset + (x*sqSize);
    float yp = offset + (y*sqSize);
    for(int i=0; i<=sqSize-gap*2; i=i+ydiv) {
      float yy = i*halfSize+gap+i;
      line(xp-halfSize+gap, yp-halfSize+gap+i, xp+halfSize-gap, yp-halfSize+gap+i);
    }
  } 
  
}

