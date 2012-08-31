// Trefoil, by Andres Colubri
// A parametric surface is textured procedurally
// by drawing on an offscreen PGraphics surface.

PGraphics pg;
PShape trefoil;

void setup() {
  size(1024, 768, P3D);
  
  textureMode(NORMAL);
  noStroke();

  // Creating offscreen surface for 3D rendering.
  pg = createGraphics(32, 512, P3D);
  pg.beginDraw();
  pg.background(0, 0);
  pg.noStroke();
  pg.fill(255, 0, 0, 200);
  pg.endDraw(); 

  // Saving trefoil surface into a PShape3D object
  trefoil = createTrefoil(350, 60, 15, pg);
}

void draw() {
  background(0);
  
  pg.beginDraw();    
  pg.ellipse(random(pg.width), random(pg.height), 4, 4);
  pg.endDraw(); 

  ambient(250, 250, 250);
  pointLight(255, 255, 255, 0, 0, 200);
     
  pushMatrix();
  translate(width/2, height/2, -200);
  rotateX(frameCount * PI / 500);
  rotateY(frameCount * PI / 500);      
  shape(trefoil);
  popMatrix();
}
