// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Landscape with height values according to Perlin noise

Landscape land;    
float theta = 0.0;

void setup() {

  size(800,200,P3D);

  // Create a landscape object
  land = new Landscape(20,800,400);
}

void draw() {

  // Ok, visualize the landscape space
  background(255);
  pushMatrix();
  translate(width/2,height/2+20,-160);
  rotateX(PI/3);
  rotateZ(theta);
  land.render(); 
  popMatrix();

  land.calculate();
  
  theta += 0.0025;
}
