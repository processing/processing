/** 
 * Synthesis 1: Form and Code
 * Wilson Grids by Casey Reas (www.processing.org)
 * p. 152
 * 
 * Step 1, basic embedded for structure. 
*/


size(600, 600);
background(255);
int sqNum = 12;
int sqSize = width/sqNum;
int halfSize = sqSize/2;

for (int y = halfSize; y < width; y = y+sqSize) {
  for (int x = halfSize; x < height; x = x+sqSize) {
    rect(x-halfSize+2, y-halfSize+2, sqSize-4, sqSize-4);
  } 
}
