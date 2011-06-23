// Trefoil, by Andres Colubri
// A parametric surface is textured procedurally
// using a particle system rendered to an offscreen
// surface.
// Click the screen to cycle between the textured surface,
// the same surface but without the texture, and the 
// offscreen texture with the particle system.

import processing.opengl.*;

PGraphics pg;
PShape trefoil;
PShape3D particles;
int mode = 0;

void setup() {
  size(280, 400, OPENGL);
  
  PFont font = createFont(PFont.list()[0], 18);
  textFont(font, 18);
  
  textureMode(NORMAL);
  noStroke();

  // Creating offscreen surface for 3D rendering.
  pg = createGraphics(32, 512, OPENGL);

  // Initializing particle system
  PShape3D.Parameters params = PShape3D.newParameters(POINT_SPRITES, STREAM);  
  particles = (PShape3D)createShape(1000, params);
  particles.loadVertices();
  for (int i = 0; i < particles.getVertexCount(); i++) {
    particles.set(i, random(0, 10), random(0, 10), 0);
  }
  particles.updateVertices();
  particles.loadColors();
  for (int i = 0; i < particles.getVertexCount(); i++) {
    particles.set(i, random(0, 1), random(0, 1), random(0, 1));
  }
  particles.updateColors(); 
  PImage sprite = loadImage("particle.png");
  particles.setTexture(sprite);
  particles.setSpriteSize(8);  

  // Saving trefoil surface into a PShape3D object
  trefoil = beginRecord();
  drawSurface(250, 60, 15, pg);
  endRecord();
}

void draw() {
  background(0);

  // Updating particle system
  particles.loadVertices();
  float[] v = particles.vertices;
  for (int i = 0; i < particles.getVertexCount(); i++) {    
    // Just random wandering
    v[3 * i + 0] += random(-1, 1);
    v[3 * i + 1] += random(-1, 1);
    // Particles wrap around the edges
    if (v[3 * i + 0] < 0) v[3 * i + 0] += pg.width; 
    if (v[3 * i + 1] < 0) v[3 * i + 1] += pg.height; 
    if (pg.width < v[3 * i + 0]) v[3 * i + 0] -= pg.width; 
    if (pg.height < v[3 * i + 1]) v[3 * i + 1] -= pg.height;
  }
  particles.updateVertices();
  
  // Drawing particles into offscreen surface, used to 
  // texture the trefoil shape.  
  pg.beginDraw();
  pg.hint(DISABLE_DEPTH_MASK);
  pg.shape(particles);
  pg.hint(ENABLE_DEPTH_MASK);
  pg.endDraw();

  if (mode < 2) {
    ambient(250, 250, 250);
    pointLight(255, 255, 255, 0, 0, 200);
     
    pushMatrix();
    translate(width/2, height/2, -200);
    rotateX(frameCount * PI / 500);
    rotateY(frameCount * PI / 500);  
  
    if (mode == 0) {
      trefoil.enableStyle();
      hint(DISABLE_DEPTH_MASK);
      screenBlend(ADD);
    } else {
      trefoil.disableStyle();
      screenBlend(BLEND);    
    }  
  
    shape(trefoil);
    hint(ENABLE_DEPTH_MASK); 
    popMatrix();
  } else {
    image(pg, 0, 0, width, height);
  }
  
  fill(255);
  text("fps: " + frameRate, 10, height - 20);
}

void mousePressed() {
  mode = (mode + 1) % 3;
}

