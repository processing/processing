
// A simple Particle class, renders the particle as an image

class Particle {
  Vector3D loc;
  Vector3D vel;
  Vector3D acc;
  float timer;
  PImage img;

  // One constructor
  Particle(Vector3D a, Vector3D v, Vector3D l, PImage img_) {
    acc = a.copy();
    vel = v.copy();
    loc = l.copy();
    timer = 100.0;
    img = img_;
  }

  // Another constructor (the one we are using here)
  Particle(Vector3D l,PImage img_) {
    acc = new Vector3D(0.0,0.0,0.0);
    float x = (float) generator.nextGaussian()*0.3f;
    float y = (float) generator.nextGaussian()*0.3f - 1.0f;
    vel = new Vector3D(x,y,0);
    loc = l.copy();
    timer = 100.0;
    img = img_;
  }

  void run() {
    update();
    render();
  }
  
  // Method to apply a force vector to the Particle object
  // Note we are ignoring "mass" here
  void add_force(Vector3D f) {
    acc.add(f);
  }  

  // Method to update location
  void update() {
    vel.add(acc);
    loc.add(vel);
    timer -= 2.5;
    acc.setXY(0,0);
  }

  // Method to display
  void render() {
    imageMode(CORNER);
    tint(255,timer);
    image(img,loc.x-img.width/2,loc.y-img.height/2);
  }

  // Is the particle still useful?
  boolean dead() {
    if (timer <= 0.0) {
      return true;
    } else {
      return false;
    }
  }
}




