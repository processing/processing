Mover mover;

void setup() {
  size(800,200);
  background(255);
  smooth();
  mover = new Mover(); 
}

void draw() {
  if (mousePressed) {
  //background(255);
  rectMode(CORNER);
  noStroke();
  fill(255,5);
  rect(0,0,width,height);
  
  mover.update();
  mover.checkEdges();
  mover.display(); 
  }
  
}


