PImage sprite;  

int npartTotal = 50000;
float partSize = 20;

PVector positions[];

int fcount, lastm;
float frate;
int fint = 3;

void setup() {
  size(800, 600, P3D);
  frameRate(60);
  
  sprite = loadImage("sprite.png");

  initPositions();

  // Writing to the depth buffer is disabled to avoid rendering
  // artifacts due to the fact that the particles are semi-transparent
  // but not z-sorted.
  hint(DISABLE_DEPTH_MASK);
} 

void draw () {
  background(0);

  translate(width/2, height/2);
  rotateY(frameCount * 0.01);
 
  for (int n = 0; n < npartTotal; n++) {
    drawParticle(positions[n]);
  }
  
  fcount += 1;
  int m = millis();
  if (m - lastm > 1000 * fint) {
    frate = float(fcount) / fint;
    fcount = 0;
    lastm = m;
    println("fps: " + frate); 
  }  
}

void drawParticle(PVector center) {
  beginShape(QUAD);
  noStroke();
  tint(255);
  texture(sprite);
  normal(0, 0, 1);
  vertex(center.x - partSize/2, center.y - partSize/2, center.z, 0, 0);
  vertex(center.x + partSize/2, center.y - partSize/2, center.z, sprite.width, 0);
  vertex(center.x + partSize/2, center.y + partSize/2, center.z, sprite.width, sprite.height);
  vertex(center.x - partSize/2, center.y + partSize/2, center.z, 0, sprite.height);                
  endShape();  
}

void initPositions() {
  positions = new PVector[npartTotal];
  for (int n = 0; n < positions.length; n++) {
    positions[n] = new PVector(random(-500, +500), random(-500, +500), random(-500, +500));
  }  
}

