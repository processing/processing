/** 
 * Synthesis 1: Form and Code
 * Wilson Grids by Casey Reas (www.processing.org)
 * p. 152
 * 
 * Step 3, changing the size of each grid element. 
*/


size(600, 600);
background(0);
stroke(255, 204);
int numSquares = 6;
int gap = 8;
int sqSize = (width-gap*2)/numSquares;

for (float y=0; y<numSquares; y=y+1) {
  for (float x=0; x<numSquares; x=x+1) {
    int sqArea = int(random(10, 100));
    int halfArea = sqArea/2;
    int ydiv = sqArea/12;
    float offset = sqSize/2+gap;
    float xp = offset + (x*sqSize);
    float yp = offset + (y*sqSize);
    for (int i = 0; i <= sqArea-gap*2; i = i+ydiv) {
      float yy = i*halfArea+gap+i;
      line(xp-halfArea+gap, yp-halfArea+gap+i, xp+halfArea-gap, yp-halfArea+gap+i);
    }
  } 
}
