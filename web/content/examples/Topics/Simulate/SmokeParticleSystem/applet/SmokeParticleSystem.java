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

public class SmokeParticleSystem extends PApplet {

/**
 * Smoke Particle System
 * by Daniel Shiffman.  
 * 
 * A basic smoke effect using a particle system. 
 * Each particle is rendered as an alpha masked image. 
 */

ParticleSystem ps;
Random generator;

public void setup() {

  size(640, 200);
  colorMode(RGB, 255, 255, 255, 100);

  // Using a Java random number generator for Gaussian random numbers
  generator = new Random();

  // Create an alpha masked image to be applied as the particle's texture
  PImage msk = loadImage("texture.gif");
  PImage img = new PImage(msk.width,msk.height);
  for (int i = 0; i < img.pixels.length; i++) img.pixels[i] = color(255);
  img.mask(msk);
  ps = new ParticleSystem(0,new Vector3D(width/2,height-20),img);

  smooth();
}

public void draw() {
  background(75);

  // Calculate a "wind" force based on mouse horizontal position
  float dx = (mouseX - width/2) / 1000.0f;
  Vector3D wind = new Vector3D(dx,0,0);
  wind.display(width/2,50,500);
  ps.add_force(wind);
  ps.run();
  for (int i = 0; i < 2; i++) {
    ps.addParticle();
  }
}











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
    timer = 100.0f;
    img = img_;
  }

  // Another constructor (the one we are using here)
  Particle(Vector3D l,PImage img_) {
    acc = new Vector3D(0.0f,0.0f,0.0f);
    float x = (float) generator.nextGaussian()*0.3f;
    float y = (float) generator.nextGaussian()*0.3f - 1.0f;
    vel = new Vector3D(x,y,0);
    loc = l.copy();
    timer = 100.0f;
    img = img_;
  }

  public void run() {
    update();
    render();
  }
  
  // Method to apply a force vector to the Particle object
  // Note we are ignoring "mass" here
  public void add_force(Vector3D f) {
    acc.add(f);
  }  

  // Method to update location
  public void update() {
    vel.add(acc);
    loc.add(vel);
    timer -= 2.5f;
    acc.setXY(0,0);
  }

  // Method to display
  public void render() {
    imageMode(CORNER);
    tint(255,timer);
    image(img,loc.x-img.width/2,loc.y-img.height/2);
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




// A class to describe a group of Particles
// An ArrayList is used to manage the list of Particles 

class ParticleSystem {

  ArrayList particles;    // An arraylist for all the particles
  Vector3D origin;        // An origin point for where particles are birthed
  PImage img;
  
  ParticleSystem(int num, Vector3D v, PImage img_) {
    particles = new ArrayList();              // Initialize the arraylist
    origin = v.copy();                        // Store the origin point
    img = img_;
    for (int i = 0; i < num; i++) {
      particles.add(new Particle(origin, img));    // Add "num" amount of particles to the arraylist
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
  
  // Method to add a force vector to all particles currently in the system
  public void add_force(Vector3D dir) {
    for (int i = particles.size()-1; i >= 0; i--) {
      Particle p = (Particle) particles.get(i);
      p.add_force(dir);
    }
  
  }  

  public void addParticle() {
    particles.add(new Particle(origin,img));
  }

  public void addParticle(Particle p) {
    particles.add(p);
  }

  // A method to test if the particle system still has particles
  public boolean dead() {
    if (particles.isEmpty()) {
      return true;
    } else {
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
    PApplet.main(new String[] { "SmokeParticleSystem" });
  }
}
