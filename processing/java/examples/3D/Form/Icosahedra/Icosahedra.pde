/**
 * I Like Icosahedra
 * by Ira Greenberg.
 * 
 * This example plots icosahedra. The Icosahdron is a regular
 * polyhedron composed of twenty equalateral triangles.
 */
 
Icosahedron ico1;
Icosahedron ico2;
Icosahedron ico3;

void setup(){
  size(640, 360, P3D);
  ico1 = new Icosahedron(75);
  ico2 = new Icosahedron(75);
  ico3 = new Icosahedron(75);
}

void draw(){
  background(0);
  lights();
  translate(width/2, height/2);

  pushMatrix();
  translate(-width/3.5, 0);
  rotateX(frameCount*PI/185);
  rotateY(frameCount*PI/-200);
  stroke(170, 0, 0);
  noFill();
  ico1.create();
  popMatrix();

  pushMatrix();
  rotateX(frameCount*PI/200);
  rotateY(frameCount*PI/300);
  stroke(150, 0, 180);
  fill(170, 170, 0);
  ico2.create();
  popMatrix();

  pushMatrix();
  translate(width/3.5, 0);
  rotateX(frameCount*PI/-200);
  rotateY(frameCount*PI/200);
  noStroke();
  fill(0, 0, 185);
  ico3.create();
  popMatrix();
}


