import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class MultipleParticleSystems extends PApplet {

/**
 * Multiple Particle Systems
 * by Daniel Shiffman.  
 * 
 * Click the mouse to generate a burst of particles
 * at mouse location. 
 * 
 * Each burst is one instance of a particle system
 * with Particles and CrazyParticles (a subclass of Particle)
 * Note use of Inheritance and Polymorphism here. 
 */
 
ArrayList psystems;

public void setup() {
  size(640, 360);
  colorMode(RGB, 255, 255, 255, 100);
  psystems = new ArrayList();
  smooth();
}

public void draw() {
  background(0);

  // Cycle through all particle systems, run them and delete old ones
  for (int i = psystems.size()-1; i >= 0; i--) {
    ParticleSystem psys = (ParticleSystem) psystems.get(i);
    psys.run();
    if (psys.dead()) {
      psystems.remove(i);
    }
  }

}

// When the mouse is pressed, add a new particle system
public void mousePressed() {
  psystems.add(new ParticleSystem(PApplet.parseInt(random(5,25)),new Vector3D(mouseX,mouseY)));
}











// A subclass of Particle

class CrazyParticle extends Particle {

  // Just adding one new variable to a CrazyParticle
  // It inherits all other fields from "Particle", and we don't have to retype them!
  float theta;

  // The CrazyParticle constructor can call the parent class (super class) constructor
  CrazyParticle(Vector3D l) {
    // "super" means do everything from the constructor in Particle
    super(l);
    // One more line of code to deal with the new variable, theta
    theta = 0.0f;

  }

  // Notice we don't have the method run() here; it is inherited from Particle

  // This update() method overrides the parent class update() method
  public void update() {
    super.update();
    // Increment rotation based on horizontal velocity
    float theta_vel = (vel.x * vel.magnitude()) / 10.0f;
    theta += theta_vel;
  }

  // Override timer
  public void timer() {
    timer -= 0.5f;
  }
  
  // Method to display
  public void render() {
    // Render the ellipse just like in a regular particle
    super.render();

    // Then add a rotating line
    pushMatrix();
    translate(loc.x,loc.y);
    rotate(theta);
    stroke(255,timer);
    line(0,0,25,0);
    popMatrix();
  }
}

// A simple Particle class

class Particle {
  Vector3D loc;
  Vector3D vel;
  Vector3D acc;
  float r;
  float timer;

  // One constructor
  Particle(Vector3D a, Vector3D v, Vector3D l, float r_) {
    acc = a.copy();
    vel = v.copy();
    loc = l.copy();
    r = r_;
    timer = 100.0f;
  }
  
  // Another constructor (the one we are using here)
  Particle(Vector3D l) {
    acc = new Vector3D(0,0.05f,0);
    vel = new Vector3D(random(-1,1),random(-2,0),0);
    loc = l.copy();
    r = 10.0f;
    timer = 100.0f;
  }


  public void run() {
    update();
    render();
  }

  // Method to update location
  public void update() {
    vel.add(acc);
    loc.add(vel);
    timer -= 1.0f;
  }

  // Method to display
  public void render() {
    ellipseMode(CENTER);
    stroke(255,timer);
    fill(100,timer);
    ellipse(loc.x,loc.y,r,r);
  }
  
  // Is the particle still useful?
  public boolean dead() {
    if (timer <= 0.0f) {
      return true;
    } else {
      return false;
    }
  }
}
// An ArrayList is used to manage the list of Particles 

class ParticleSystem {

  ArrayList particles;    // An arraylist for all the particles
  Vector3D origin;        // An origin point for where particles are birthed

  ParticleSystem(int num, Vector3D v) {
    particles = new ArrayList();              // Initialize the arraylist
    origin = v.copy();                        // Store the origin point
    for (int i = 0; i < num; i++) {
      // We have a 50% chance of adding each kind of particle
      if (random(1) < 0.5f) {
        particles.add(new CrazyParticle(origin)); 
      } else {
        particles.add(new Particle(origin)); 
      }
    }
  }

  public void run() {
    // Cycle through the ArrayList backwards b/c we are deleting
    for (int i = particles.size()-1; i >= 0; i--) {
      Particle p = (Particle) particles.get(i);
      p.run();
      if (p.dead()) {
        particles.remove(i);
      }
    }
  }

  public void addParticle() {
    particles.add(new Particle(origin));
  }

  public void addParticle(Particle p) {
    particles.add(p);
  }

  // A method to test if the particle system still has particles
  public boolean dead() {
    if (particles.isEmpty()) {
      return true;
    } 
    else {
      return false;
    }
  }

}

// Simple Vector class

class Vector3D {
  float x;
  float y;
  float z;

  Vector3D(float x_, float y_, float z_) {
    x = x_; 
    y = y_; 
    z = z_;
  }

  Vector3D(float x_, float y_) {
    x = x_; 
    y = y_; 
    z = 0f;
  }

  Vector3D() {
    x = 0f; 
    y = 0f; 
    z = 0f;
  }

  public void setX(float x_) {
    x = x_;
  }

  public void setY(float y_) {
    y = y_;
  }

  public void setZ(float z_) {
    z = z_;
  }

  public void setXY(float x_, float y_) {
    x = x_;
    y = y_;
  }

  public void setXYZ(float x_, float y_, float z_) {
    x = x_;
    y = y_;
    z = z_;
  }

  public void setXYZ(Vector3D v) {
    x = v.x;
    y = v.y;
    z = v.z;
  }
  public float magnitude() {
    return (float) Math.sqrt(x*x + y*y + z*z);
  }

  public Vector3D copy() {
    return new Vector3D(x,y,z);
  }

  public Vector3D copy(Vector3D v) {
    return new Vector3D(v.x, v.y,v.z);
  }

  public void add(Vector3D v) {
    x += v.x;
    y += v.y;
    z += v.z;
  }

  public void sub(Vector3D v) {
    x -= v.x;
    y -= v.y;
    z -= v.z;
  }

  public void mult(float n) {
    x *= n;
    y *= n;
    z *= n;
  }

  public void div(float n) {
    x /= n;
    y /= n;
    z /= n;
  }

  /* float dot(Vector3D v) {
   //implement DOT product
   }*/

  /* Vector3D cross(Vector3D v) {
   //implement CROSS product
   }*/

  public void normalize() {
    float m = magnitude();
    if (m > 0) {
      div(m);
    }
  }

  public void limit(float max) {
    if (magnitude() > max) {
      normalize();
      mult(max);
    }
  }

  public float heading2D() {
    float angle = (float) Math.atan2(-y, x);
    return -1*angle;
  }

  public Vector3D add(Vector3D v1, Vector3D v2) {
    Vector3D v = new Vector3D(v1.x + v2.x,v1.y + v2.y, v1.z + v2.z);
    return v;
  }

  public Vector3D sub(Vector3D v1, Vector3D v2) {
    Vector3D v = new Vector3D(v1.x - v2.x,v1.y - v2.y,v1.z - v2.z);
    return v;
  }

  public Vector3D div(Vector3D v1, float n) {
    Vector3D v = new Vector3D(v1.x/n,v1.y/n,v1.z/n);
    return v;
  }

  public Vector3D mult(Vector3D v1, float n) {
    Vector3D v = new Vector3D(v1.x*n,v1.y*n,v1.z*n);
    return v;
  }

  public float distance (Vector3D v1, Vector3D v2) {
    float dx = v1.x - v2.x;
    float dy = v1.y - v2.y;
    float dz = v1.z - v2.z;
    return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
  }

  public void display(float x, float y, float scayl) {
    pushMatrix();
    float arrowsize = 4;
    // Translate to location to render vector
    translate(x,y);
    stroke(255);
    // Call vector heading function to get direction (note that pointing up is a heading of 0) and rotate
    rotate(heading2D());
    // Calculate length of vector & scale it to be bigger or smaller if necessary
    float len = magnitude()*scayl;
    // Draw three lines to make an arrow (draw pointing up since we've rotate to the proper direction)
    line(0,0,len,0);
    line(len,0,len-arrowsize,+arrowsize/2);
    line(len,0,len-arrowsize,-arrowsize/2);
    popMatrix();
  } 

}


  static public void main(String args[]) {
    PApplet.main(new String[] { "MultipleParticleSystems" });
  }
}
