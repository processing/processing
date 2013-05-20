
void setup() {
  size(600, 360);
}

void draw() {
  background(255);
  
  PVector a = new PVector(20,300);
  PVector b = new PVector(500,250);
  PVector mouse = new PVector(mouseX,mouseY);
  
  stroke(0);
  strokeWeight(2);
  line(a.x,a.y,b.x,b.y);
  line(a.x,a.y,mouse.x,mouse.y);
  fill(0);
  ellipse(a.x,a.y,8,8);
  ellipse(b.x,b.y,8,8);
  ellipse(mouse.x,mouse.y,8,8);
  
  PVector norm = scalarProjection(mouse,a,b);
  strokeWeight(1);
  stroke(50);
  line(mouse.x,mouse.y,norm.x,norm.y);

  noStroke();
  fill(255,0,0);
  ellipse(norm.x,norm.y,16,16);
}


PVector scalarProjection(PVector p, PVector a, PVector b) {
  PVector ap = PVector.sub(p, a);
  PVector ab = PVector.sub(b, a);
  ab.normalize(); // Normalize the line
  ab.mult(ap.dot(ab));
  PVector normalPoint = PVector.add(a, ab);
  return normalPoint;
}

