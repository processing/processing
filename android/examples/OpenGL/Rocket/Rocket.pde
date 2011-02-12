// Rocket, by Andres Colubri.
// This example shows the OBJ load functionality and the use of
// the PShape3D class to create a particle system.
// Rocket model from http://keleb.free.fr/codecorner/models-en.htm

PShape3D rocket;
PShape3D particles;
int[] lifetimes;

int numParticlesTotal = 600; 
int numParticlesPerFrame = 10;
int particleLifetime;

float time;
float timeInc = 0.03;

PVector axis, pos, vel;

void setup() {
  size(480, 800, A3D);
  orientation(PORTRAIT);

  rocket = (PShape3D)loadShape("rocket.obj");
  // Adjusting the size, orientation and position of the object.
  // The bulk scale, rotate and translate operations done on the
  // shape are permanent.
  rocket.scaleVertices(0.2);
  rocket.rotateVerticesX(-PI);
  rocket.translateVertices(-5, 25, 0);  

  // The particle system is stored in a PShape3D object set to
  // POINT_SPRITES mode
  particles = (PShape3D)createShape(numParticlesTotal, PShape3D.newParameters(POINT_SPRITES, DYNAMIC));
  particleLifetime = numParticlesTotal / numParticlesPerFrame;
  lifetimes = new int[numParticlesTotal];
  
  // Loading and setting the sprite image. 
  PImage sprite = loadImage("smoke.png");
  particles.setTexture(sprite);
  // The default maximum sprite size is determined by the graphics 
  // hardware (it usually ranges between 32 and 128 pixels).  
  println("Maximum sprite size: " + particles.getMaxSpriteSize());
  // The maximum size can be set with setMaxSpriteSize(), but will be 
  // capped by the maximum size supported by the hardware.
  particles.setMaxSpriteSize(32);  
  // The actual sprite size depends on the distance d from the sprite
  // to the camera as follows:
  // s = smax / (1 + c * d * d) (quadratic dependence on d)
  // or
  // s = smax / (1 + c * d)     (linear dependence on d)
  // where smax is the maximum sprite size and c an adjustable constant.
  // In the next call, the constant is adjusted so that the actual sprite
  // size is 10 when the distance is 400. A quadratic dependence on d is used.
  particles.setSpriteSize(10, 400, QUADRATIC);
  // PShape3D objects automatically update their bounding boxes, but we don't
  // want this for the particle system object.
  particles.autoBounds(false);
  particles.setColor(color(0)); // Making sure that all particles start as black.
  
  // Initialzing particles with negative lifetimes so they are added
  // progresively into the scene during the first frames of the program
  int t = -1;
  for (int i = 0; i < numParticlesTotal; i++) {
    if (i % numParticlesPerFrame == 0) {
      t++;
    }
    lifetimes[i] = -t; 
  }
  
  pos = new PVector(0, 0, 0);
  vel = new PVector(0, 0, 0);
  
  // The rocket object is originally aligned to the Y axis. We
  // use this vector to calculate the rotation needed to make 
  // the rocket aligned to the velocity vector
  axis = new PVector(0, 1, 0);  
}

void draw() {    
  background(0);
  
  updatePosition();
  updateParticles();

  ambient(250, 250, 250);
  pointLight(255, 255, 255, 500, height/2, 400);

  pushMatrix(); 
  translate(pos.x, pos.y, pos.z);

  PVector rotAxis = axis.cross(vel);
  float angle = PVector.angleBetween(axis, vel);
  rotate(angle, rotAxis.x, rotAxis.y, rotAxis.z);
  
  rotateY(2 * time);
  
  shape(rocket);
  popMatrix();

  // The particles are not lit.
  noLights();
  
  // Writing to the depth mask is disabled to avoid rendering artifacts due 
  // to the fact that the particles are transparent but not depth sorted
  hint(DISABLE_DEPTH_MASK);
  shape(particles);
  hint(ENABLE_DEPTH_MASK);
  
  time += timeInc;
}

void updatePosition() {
  pos.x = width/2 + 150 * cos(time);
  pos.y = 50 + height/2 + 200 * cos(2 * time);
  pos.z = 150 + 200 * sin(time);
  
  vel.x = 150 * sin(time);
  vel.y = 400 * sin(2 * time);
  vel.z = -150 * cos(time);  
}  

void updateParticles() {
  // Respawn dead particles
  particles.loadVertices();
  for (int i = 0; i < numParticlesTotal; i++) {
    if (lifetimes[i] == 0) {
      particles.vertices[3 * i + 0] = random(pos.x - 8, pos.x + 8);
      particles.vertices[3 * i + 1] = random(pos.y - 8, pos.y + 8);     
      particles.vertices[3 * i + 2] = random(pos.z - 8, pos.z + 8);       
    }
  }
  particles.updateVertices();  
  
  // Update colors and lifetimes of particles
  particles.loadColors();
  for (int i = 0; i < numParticlesTotal; i++) {
    if (0 <= lifetimes[i]) {
      // Interpolating between alpha 1 to 0:
      float a = 1.0 - float(lifetimes[i]) / particleLifetime;
      
      // Interpolating from orange to white during the first 
      // quarter of the particle's life (the color shoud be specified
      // in normalized RGBA components)
      float f = min(float(lifetimes[i]) / (0.25 * particleLifetime), 1);      
      particles.colors[4 * i + 0] = (1 - f) * 0.98 + f;
      particles.colors[4 * i + 1] = (1 - f) * 0.75 + f;      
      particles.colors[4 * i + 2] = (1 - f) * 0.26 + f;
      particles.colors[4 * i + 3] = a;                  
      
    }
    lifetimes[i]++;
    if (lifetimes[i] == particleLifetime) {
      lifetimes[i] = 0;
    }
  }
  particles.updateColors();
}


