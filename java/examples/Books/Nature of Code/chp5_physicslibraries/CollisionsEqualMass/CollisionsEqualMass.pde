// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Collisions -- Elastic, Equal Mass, Two objects only

// Based off of Chapter 9: Resolving Collisions
// Mathematics and Physics for Programmers by Danny Kodicek

// A Thing class for idealized collisions

Mover a;
Mover b;

boolean showVectors = true;

void setup() {
  size(200,200);
  a = new Mover(new PVector(random(5),random(-5,5)),new PVector(10,10));
  b = new Mover(new PVector(-2,1),new PVector(150,150));
}

void draw() {  
  background(255);
  a.go();
  b.go();  
  
  // Note this function will ONLY WORK with two objects
  // Needs to be revised in the case of an array of objects  
  a.collideEqualMass(b);
}



