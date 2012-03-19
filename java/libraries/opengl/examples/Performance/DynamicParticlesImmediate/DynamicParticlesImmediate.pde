PImage sprite;  

int npartTotal = 10000;
int npartPerFrame = 25;
float speed = 1.0;
float gravity = 0.05;
float partSize = 20;

int partLifetime;
PVector positions[];
PVector velocities[];
int lifetimes[];  

int fcount, lastm;
float frate;
int fint = 3;

void setup() {
  size(640, 480, P3D);
  frameRate(120);
  
  sprite = loadImage("sprite.png");

  partLifetime = npartTotal / npartPerFrame;
  initPositions();
  initVelocities();
  initLifetimes(); 

  // Writing to the depth buffer is disabled to avoid rendering
  // artifacts due to the fact that the particles are semi-transparent
  // but not z-sorted.
  hint(DISABLE_DEPTH_MASK);
  
  // Testing some hints
  //hint(DISABLE_TRANSFORM_CACHE);
  //hint(ENABLE_ACCURATE_2D);
} 

void draw () {
  background(0);

  for (int n = 0; n < npartTotal; n++) {
    lifetimes[n]++;
    if (lifetimes[n] == partLifetime) {
      lifetimes[n] = 0;
    }      

    if (0 <= lifetimes[n]) {      
      float opacity = 1.0 - float(lifetimes[n]) / partLifetime;
            
      if (lifetimes[n] == 0) {
        // Re-spawn dead particle
        positions[n].x = mouseX;
        positions[n].y = mouseY;
        
        float angle = random(0, TWO_PI);
        float s = random(0.5 * speed, 0.5 * speed);
        velocities[n].x = s * cos(angle);
        velocities[n].y = s * sin(angle);
      } else {
        positions[n].x += velocities[n].x;
        positions[n].y += velocities[n].y;
        
        velocities[n].y += gravity;
      }
      drawParticle(positions[n], opacity);
    }
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

void drawParticle(PVector center, float opacity) {
  beginShape(QUAD);
  noStroke();
  tint(255, opacity * 255);
  texture(sprite);
  normal(0, 0, 1);
  vertex(center.x - partSize/2, center.y - partSize/2, 0, 0);
  vertex(center.x + partSize/2, center.y - partSize/2, sprite.width, 0);
  vertex(center.x + partSize/2, center.y + partSize/2, sprite.width, sprite.height);
  vertex(center.x - partSize/2, center.y + partSize/2, 0, sprite.height);                
  endShape();  
}

void initPositions() {
  positions = new PVector[npartTotal];
  for (int n = 0; n < positions.length; n++) {
    positions[n] = new PVector();
  }  
}

void initVelocities() {
  velocities = new PVector[npartTotal];
  for (int n = 0; n < velocities.length; n++) {
    velocities[n] = new PVector();
  }
}

void initLifetimes() {
  // Initializing particles with negative lifetimes so they are added
  // progressively into the screen during the first frames of the sketch   
  lifetimes = new int[npartTotal];
  int t = -1;
  for (int n = 0; n < lifetimes.length; n++) {    
    if (n % npartPerFrame == 0) {
      t++;
    }
    lifetimes[n] = -t;
  }
} 
