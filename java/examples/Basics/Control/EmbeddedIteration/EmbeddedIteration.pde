/**
 * Embedding Iteration. 
 * 
 * Embedding "for" structures allows repetition in two dimensions. 
 *
 */


size(640, 360); 
background(0); 
noStroke(); 

int gridSize = 40;

for (int x = gridSize; x <= width - gridSize; x += gridSize) {
  for (int y = gridSize; y <= height - gridSize; y += gridSize) {
    noStroke();
    fill(255);
    rect(x-1, y-1, 3, 3);
    stroke(255, 50);
    line(x, y, width/2, height/2);
  }
}

